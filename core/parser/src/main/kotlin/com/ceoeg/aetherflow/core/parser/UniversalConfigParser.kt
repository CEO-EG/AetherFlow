package com.ceoeg.aetherflow.core.parser

import com.ceoeg.aetherflow.core.common.model.SubscriptionFeed
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeepLinkImportResult {
    data class ProfilesImported(val profiles: List<TunnelProfile>) : DeepLinkImportResult
    data class SubscriptionDiscovered(val feed: SubscriptionFeed) : DeepLinkImportResult
    data class Unsupported(val reason: String) : DeepLinkImportResult
}

data class ConfigValidationResult(
    val isValid: Boolean,
    val detectedFormat: String,
    val summaryMessage: String,
)

/**
 * Master facade for importing/exporting all tunnel formats, deep-links
 * (`vpnclient://`, `v2rayng://`, `sing-box://`, `clash://`, `hiddify://`),
 * and real-time JSON/YAML/.conf validation.
 */
@Singleton
class UniversalConfigParser @Inject constructor() {

    private val strictJson = Json { ignoreUnknownKeys = true }

    fun importAny(
        rawContent: String,
        subscriptionId: String? = null,
        hintName: String? = null,
    ): List<TunnelProfile> {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 1. WireGuard / AmneziaWG .conf INI
        if (trimmed.contains("[Interface]", ignoreCase = true) &&
            trimmed.contains("[Peer]", ignoreCase = true)
        ) {
            return listOfNotNull(
                WireGuardAndOpenVpnCodec.parseWireGuardConf(
                    confContent = trimmed,
                    profileName = hintName ?: "WireGuard Endpoint",
                    subscriptionId = subscriptionId,
                )
            )
        }

        // 2. OpenVPN .ovpn
        if (trimmed.contains("<ca>", ignoreCase = true) ||
            (trimmed.contains("client") && trimmed.contains("dev tun"))
        ) {
            return listOfNotNull(
                WireGuardAndOpenVpnCodec.parseOpenVpn(
                    ovpnContent = trimmed,
                    profileName = hintName ?: "OpenVPN Endpoint",
                    subscriptionId = subscriptionId,
                )
            )
        }

        // 3. JSON bundle (Sing-box, Xray, SIP008)
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            val jsonProfiles = ClashAndJsonBundleCodec.parseJsonBundle(trimmed, subscriptionId)
            if (jsonProfiles.isNotEmpty()) return jsonProfiles
        }

        // 4. Clash / Mihomo YAML
        if (trimmed.contains("proxies:")) {
            val clashProfiles = ClashAndJsonBundleCodec.parseClashYaml(trimmed, subscriptionId)
            if (clashProfiles.isNotEmpty()) return clashProfiles
        }

        // 5. Direct multi-line share URIs
        val directUris = trimmed.lineSequence()
            .map { it.trim() }
            .filter { it.contains("://") }
            .mapNotNull { ShareUriCodec.parseUri(it, subscriptionId) }
            .toList()
        if (directUris.isNotEmpty()) return directUris

        // 6. Base64-encoded subscription feed (containing URIs, YAML, or JSON)
        val decodedFeed = Base64Codec.decodeFlexible(trimmed)
        if (!decodedFeed.isNullOrBlank() && decodedFeed != trimmed) {
            return importAny(decodedFeed, subscriptionId, hintName)
        }

        return emptyList()
    }

    fun exportToBase64Subscription(profiles: List<TunnelProfile>): String {
        val lines = profiles.joinToString("\n") { ShareUriCodec.serializeToUri(it) }
        return Base64Codec.encodeStandard(lines)
    }

    /**
     * Parses external client deep-links:
     * - `vpnclient://import?url=...` / `vpnclient://...`
     * - `v2rayng://install-sub?url=...&name=...` / `v2rayng://install-config?url=...`
     * - `sing-box://import-remote-profile?url=...#name`
     * - `clash://install-config?url=...&name=...`
     * - `hiddify://import/<subUrl>#name`
     */
    fun parseClientDeepLink(deepLink: String): DeepLinkImportResult {
        val clean = deepLink.trim()
        val directProfile = ShareUriCodec.parseUri(clean)
        if (directProfile != null) {
            return DeepLinkImportResult.ProfilesImported(listOf(directProfile))
        }

        val scheme = clean.substringBefore("://", "").lowercase()
        if (scheme !in setOf("vpnclient", "v2rayng", "sing-box", "clash", "hiddify")) {
            return DeepLinkImportResult.Unsupported("Unsupported scheme: $scheme")
        }

        val afterScheme = clean.substringAfter("://")
        val fragmentName = if (afterScheme.contains('#')) {
            Base64Codec.urlDecode(afterScheme.substringAfterLast('#'))
        } else {
            ""
        }
        val beforeFragment = afterScheme.substringBeforeLast('#')

        // hiddify://import/https://example.com/sub
        if (scheme == "hiddify" && beforeFragment.startsWith("import/")) {
            val embeddedUrl = beforeFragment.removePrefix("import/")
            if (embeddedUrl.startsWith("http://") || embeddedUrl.startsWith("https://")) {
                return DeepLinkImportResult.SubscriptionDiscovered(
                    SubscriptionFeed(
                        name = fragmentName.ifBlank { "Imported Subscription" },
                        url = embeddedUrl,
                    )
                )
            }
        }

        val queryPart = beforeFragment.substringAfter('?', "")
        val queryMap = queryPart.split('&').mapNotNull { pair ->
            if (!pair.contains('=')) null
            else Base64Codec.urlDecode(pair.substringBefore('=')) to Base64Codec.urlDecode(pair.substringAfter('='))
        }.toMap()

        val remoteUrl = queryMap["url"]
        if (!remoteUrl.isNullOrBlank()) {
            val name = queryMap["name"]?.takeIf { it.isNotBlank() }
                ?: fragmentName.ifBlank { "Remote Subscription" }
            return DeepLinkImportResult.SubscriptionDiscovered(
                SubscriptionFeed(name = name, url = remoteUrl)
            )
        }

        val inlineData = queryMap["config"] ?: queryMap["data"]
        if (!inlineData.isNullOrBlank()) {
            val imported = importAny(inlineData)
            if (imported.isNotEmpty()) return DeepLinkImportResult.ProfilesImported(imported)
        }

        return DeepLinkImportResult.Unsupported("No valid subscription URL or profile payload in deep-link")
    }

    /** Real-time syntax & schema validator for the Raw Configuration Editor. */
    fun validateRawConfig(content: String): ConfigValidationResult {
        val text = content.trim()
        if (text.isEmpty()) {
            return ConfigValidationResult(false, "Empty", "Configuration cannot be empty.")
        }

        if (text.startsWith("{")) {
            return runCatching {
                val obj = strictJson.parseToJsonElement(text) as? JsonObject
                    ?: return ConfigValidationResult(false, "JSON", "Root element must be a JSON object.")
                val hasOutbounds = obj.containsKey("outbounds")
                val hasServers = obj.containsKey("servers")
                if (!hasOutbounds && !hasServers) {
                    ConfigValidationResult(
                        false,
                        "JSON",
                        "Missing required 'outbounds' (Xray / Sing-box) or 'servers' (SIP008) array.",
                    )
                } else {
                    val flavor = when {
                        obj.containsKey("\$schema") || text.contains("server_port") -> "Sing-box JSON"
                        hasServers -> "SIP008 JSON"
                        else -> "Xray-core JSON"
                    }
                    ConfigValidationResult(true, flavor, "Valid $flavor schema.")
                }
            }.getOrElse { err ->
                ConfigValidationResult(false, "JSON", "JSON syntax error: ${err.message?.lineSequence()?.firstOrNull()}")
            }
        }

        if (text.contains("[Interface]", ignoreCase = true)) {
            val parsed = WireGuardAndOpenVpnCodec.parseWireGuardConf(text)
            return if (parsed != null) {
                val flavor = if (parsed.wireGuard.hasAmneziaObfuscation) "AmneziaWG .conf" else "WireGuard .conf"
                ConfigValidationResult(true, flavor, "Valid $flavor (${parsed.serverAddress}:${parsed.serverPort}).")
            } else {
                ConfigValidationResult(
                    false,
                    "WireGuard .conf",
                    "Missing required PrivateKey, Peer PublicKey, or Endpoint.",
                )
            }
        }

        if (text.contains("proxies:")) {
            return runCatching {
                val yaml = Yaml(SafeConstructor(LoaderOptions()))
                val map = yaml.load<Any>(text) as? Map<*, *>
                val count = (map?.get("proxies") as? List<*>)?.size ?: 0
                if (count > 0) {
                    ConfigValidationResult(true, "Clash / Mihomo YAML", "Valid YAML ($count proxy nodes parsed).")
                } else {
                    ConfigValidationResult(false, "Clash YAML", "No valid entries inside 'proxies:' list.")
                }
            }.getOrElse { err ->
                ConfigValidationResult(false, "YAML", "YAML syntax error: ${err.message?.lineSequence()?.firstOrNull()}")
            }
        }

        val parsedAny = importAny(text)
        return if (parsedAny.isNotEmpty()) {
            ConfigValidationResult(true, "Share URI / Feed", "Valid configuration (${parsedAny.size} endpoint(s)).")
        } else {
            ConfigValidationResult(false, "Unknown", "Unrecognized configuration syntax.")
        }
    }
}
