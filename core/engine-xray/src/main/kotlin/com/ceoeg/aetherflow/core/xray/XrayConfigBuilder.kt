package com.ceoeg.aetherflow.core.xray

import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.RoutingRule
import com.ceoeg.aetherflow.core.common.model.RuleMatchType
import com.ceoeg.aetherflow.core.common.model.SecurityMode
import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import com.ceoeg.aetherflow.core.common.model.TransportType
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.routing.DnsAndRoutingCompiler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates Xray-core v26+ JSON configurations including:
 * - Ephemeral authenticated local SOCKS5/HTTP inbounds (anti-port-scan hardening)
 * - VLESS (XTLS-RPRX-Vision, REALITY + Post-Quantum `mldsa65Verify`), VMess AEAD, Trojan, SS-2022
 * - XHTTP / SplitHTTP with `xmux` connection pool settings
 * - Unified `streamSettings.finalmask` TLS ClientHello Fragmentation & UDP Noise injection
 * - Multi-hop `sockopt.dialerProxy` outbound chaining (Entry -> Exit / Cloudflare WARP)
 * - Split DNS, FakeIP, and Geo/Custom routing rules
 */
@Singleton
class XrayConfigBuilder @Inject constructor(
    private val routingCompiler: DnsAndRoutingCompiler,
) {
    private val prettyJson = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun buildConfig(
        profile: TunnelProfile,
        settings: AppSettings,
        sessionAuth: SessionLoopbackAuth,
        customRules: List<RoutingRule> = emptyList(),
        detourExitProfile: TunnelProfile? = null,
    ): String {
        if (profile.protocol == ProtocolType.CUSTOM_JSON && profile.rawCustomConfig.isNotBlank()) {
            // Strip removed legacy `"allowInsecure": true` so imported raw Xray JSON configs run on Xray v26.6+
            return profile.rawCustomConfig.replace(
                Regex(""""allowInsecure"\s*:\s*true"""),
                """"allowInsecure": false""",
            )
        }

        val plan = routingCompiler.compile(settings, customRules)

        val root = buildJsonObject {
            putJsonObject("log") {
                put("loglevel", settings.logSeverity.wireLevel)
            }

            putJsonObject("stats") {}

            putJsonObject("policy") {
                putJsonObject("system") {
                    put("statsOutboundUplink", true)
                    put("statsOutboundDownlink", true)
                }
            }

            put("inbounds", buildInbounds(settings, sessionAuth))

            if (plan.dns.fakeIpEnabled) {
                putJsonArray("fakedns") {
                    add(
                        buildJsonObject {
                            put("ipPool", plan.dns.fakeIpv4Cidr)
                            put("poolSize", 65535)
                        }
                    )
                    if (!plan.dns.blockIpv6Leak) {
                        add(
                            buildJsonObject {
                                put("ipPool", plan.dns.fakeIpv6Cidr)
                                put("poolSize", 65535)
                            }
                        )
                    }
                }
            }

            putJsonObject("dns") {
                put("queryStrategy", if (plan.dns.blockIpv6Leak) "UseIPv4" else "UseIP")
                putJsonArray("servers") {
                    if (plan.dns.fakeIpEnabled) {
                        add(JsonPrimitive("fakedns"))
                    }
                    if (plan.dns.splitDnsEnabled && plan.dns.domesticCountryCode.isNotBlank()) {
                        add(
                            buildJsonObject {
                                put("address", plan.dns.directAddress)
                                putJsonArray("domains") {
                                    add(JsonPrimitive("geosite:${plan.dns.domesticCountryCode}"))
                                }
                                putJsonArray("expectIPs") {
                                    add(JsonPrimitive("geoip:${plan.dns.domesticCountryCode}"))
                                }
                                put("skipFallback", true)
                            }
                        )
                    }
                    add(JsonPrimitive(plan.dns.remoteAddress))
                }
            }

            put("outbounds", buildOutbounds(profile, settings, detourExitProfile, plan.dns.blockIpv6Leak))
            put("routing", buildRouting(plan))
        }

        return prettyJson.encodeToString(JsonObject.serializer(), root)
    }

    private fun buildInbounds(
        settings: AppSettings,
        sessionAuth: SessionLoopbackAuth,
    ): JsonArray = buildJsonArray {
        val destOverrides = buildJsonArray {
            add(JsonPrimitive("http"))
            add(JsonPrimitive("tls"))
            add(JsonPrimitive("quic"))
            if (settings.dnsSettings.fakeIpEnabled) {
                add(JsonPrimitive("fakedns"))
            }
        }

        // Primary ephemeral authenticated local SOCKS5 inbound
        add(
            buildJsonObject {
                put("tag", "socks-in")
                put("listen", "127.0.0.1")
                put("port", sessionAuth.socksPort)
                put("protocol", "socks")
                putJsonObject("settings") {
                    put("auth", "password")
                    putJsonArray("accounts") {
                        add(
                            buildJsonObject {
                                put("user", sessionAuth.username)
                                put("pass", sessionAuth.password)
                            }
                        )
                    }
                    put("udp", true)
                }
                putJsonObject("sniffing") {
                    put("enabled", true)
                    put("destOverride", destOverrides)
                    put("routeOnly", !settings.dnsSettings.fakeIpEnabled)
                }
            }
        )

        // Ephemeral authenticated local HTTP inbound
        add(
            buildJsonObject {
                put("tag", "http-in")
                put("listen", "127.0.0.1")
                put("port", sessionAuth.httpPort)
                put("protocol", "http")
                putJsonObject("settings") {
                    putJsonArray("accounts") {
                        add(
                            buildJsonObject {
                                put("user", sessionAuth.username)
                                put("pass", sessionAuth.password)
                            }
                        )
                    }
                }
            }
        )

        // Optional LAN / Hotspot Tethering inbound
        val lan = settings.lanTethering
        if (lan.enabled) {
            add(
                buildJsonObject {
                    put("tag", "lan-socks-in")
                    put("listen", lan.bindAddress)
                    put("port", lan.socksPort)
                    put("protocol", "socks")
                    putJsonObject("settings") {
                        if (lan.requireAuth && lan.password.isNotBlank()) {
                            put("auth", "password")
                            putJsonArray("accounts") {
                                add(
                                    buildJsonObject {
                                        put("user", lan.username)
                                        put("pass", lan.password)
                                    }
                                )
                            }
                        } else {
                            put("auth", "noauth")
                        }
                        put("udp", true)
                    }
                    putJsonObject("sniffing") {
                        put("enabled", true)
                        put("destOverride", destOverrides)
                    }
                }
            )
        }
    }

    private fun buildOutbounds(
        profile: TunnelProfile,
        settings: AppSettings,
        detourExitProfile: TunnelProfile?,
        blockIpv6Leak: Boolean,
    ): JsonArray = buildJsonArray {
        if (detourExitProfile != null) {
            // Entry Node -> Detour Exit Node chain:
            // "proxy" is the exit node dialed through "entry-hop"
            add(
                buildSingleProxyOutbound(
                    tag = "proxy",
                    profile = detourExitProfile,
                    settings = settings,
                    dialerProxyTag = "entry-hop",
                )
            )
            add(
                buildSingleProxyOutbound(
                    tag = "entry-hop",
                    profile = profile,
                    settings = settings,
                    dialerProxyTag = null,
                )
            )
        } else {
            add(
                buildSingleProxyOutbound(
                    tag = "proxy",
                    profile = profile,
                    settings = settings,
                    dialerProxyTag = null,
                )
            )
        }

        add(
            buildJsonObject {
                put("tag", "direct")
                put("protocol", "freedom")
                putJsonObject("settings") {
                    put("domainStrategy", if (blockIpv6Leak) "UseIPv4" else "AsIs")
                }
            }
        )

        add(
            buildJsonObject {
                put("tag", "block")
                put("protocol", "blackhole")
                putJsonObject("settings") {
                    putJsonObject("response") {
                        put("type", "none")
                    }
                }
            }
        )

        add(
            buildJsonObject {
                put("tag", "dns-out")
                put("protocol", "dns")
            }
        )
    }

    private fun buildSingleProxyOutbound(
        tag: String,
        profile: TunnelProfile,
        settings: AppSettings,
        dialerProxyTag: String?,
    ): JsonObject = buildJsonObject {
        put("tag", tag)
        when (profile.protocol) {
            ProtocolType.VLESS -> {
                put("protocol", "vless")
                putJsonObject("settings") {
                    putJsonArray("vnext") {
                        add(
                            buildJsonObject {
                                put("address", profile.serverAddress)
                                put("port", profile.serverPort)
                                putJsonArray("users") {
                                    add(
                                        buildJsonObject {
                                            put("id", profile.uuid)
                                            put(
                                                "encryption",
                                                profile.method.ifBlank { "none" }
                                                    .let { if (it == "auto") "none" else it }
                                            )
                                            if (profile.flow.isNotBlank()) {
                                                put("flow", profile.flow)
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
                put("streamSettings", buildStreamSettings(profile, settings, dialerProxyTag))
            }

            ProtocolType.VMESS -> {
                put("protocol", "vmess")
                putJsonObject("settings") {
                    putJsonArray("vnext") {
                        add(
                            buildJsonObject {
                                put("address", profile.serverAddress)
                                put("port", profile.serverPort)
                                putJsonArray("users") {
                                    add(
                                        buildJsonObject {
                                            put("id", profile.uuid)
                                            put("alterId", profile.alterId)
                                            put("security", profile.method.ifBlank { "auto" })
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
                put("streamSettings", buildStreamSettings(profile, settings, dialerProxyTag))
            }

            ProtocolType.TROJAN -> {
                put("protocol", "trojan")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        add(
                            buildJsonObject {
                                put("address", profile.serverAddress)
                                put("port", profile.serverPort)
                                put("password", profile.password)
                            }
                        )
                    }
                }
                put("streamSettings", buildStreamSettings(profile, settings, dialerProxyTag))
            }

            ProtocolType.SHADOWSOCKS -> {
                put("protocol", "shadowsocks")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        add(
                            buildJsonObject {
                                put("address", profile.serverAddress)
                                put("port", profile.serverPort)
                                put("method", profile.method.ifBlank { "2022-blake3-aes-128-gcm" })
                                put("password", profile.password)
                                put("uot", true)
                            }
                        )
                    }
                }
                put("streamSettings", buildStreamSettings(profile, settings, dialerProxyTag))
            }

            ProtocolType.WIREGUARD, ProtocolType.AMNEZIA_WG -> {
                val wg = profile.wireGuard
                put("protocol", "wireguard")
                putJsonObject("settings") {
                    put("secretKey", wg.privateKey)
                    putJsonArray("address") {
                        wg.localAddresses.forEach { add(JsonPrimitive(it)) }
                    }
                    put("mtu", wg.mtu)
                    if (wg.reservedBytes.size == 3) {
                        putJsonArray("reserved") {
                            wg.reservedBytes.forEach { add(JsonPrimitive(it)) }
                        }
                    }
                    putJsonArray("peers") {
                        add(
                            buildJsonObject {
                                put("publicKey", wg.peerPublicKey)
                                if (wg.presharedKey.isNotBlank()) put("preSharedKey", wg.presharedKey)
                                put("endpoint", "${profile.serverAddress}:${profile.serverPort}")
                                put("keepAlive", wg.persistentKeepalive)
                            }
                        )
                    }
                }
                if (dialerProxyTag != null) {
                    putJsonObject("streamSettings") {
                        putJsonObject("sockopt") {
                            put("dialerProxy", dialerProxyTag)
                        }
                    }
                }
            }

            ProtocolType.HTTP -> {
                put("protocol", "http")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        add(
                            buildJsonObject {
                                put("address", profile.serverAddress)
                                put("port", profile.serverPort)
                                if (profile.username.isNotBlank()) {
                                    putJsonArray("users") {
                                        add(
                                            buildJsonObject {
                                                put("user", profile.username)
                                                put("pass", profile.password)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                put("streamSettings", buildStreamSettings(profile, settings, dialerProxyTag))
            }

            else -> {
                put("protocol", "socks")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        add(
                            buildJsonObject {
                                put("address", profile.serverAddress)
                                put("port", profile.serverPort)
                                if (profile.username.isNotBlank()) {
                                    putJsonArray("users") {
                                        add(
                                            buildJsonObject {
                                                put("user", profile.username)
                                                put("pass", profile.password)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                if (dialerProxyTag != null) {
                    putJsonObject("streamSettings") {
                        putJsonObject("sockopt") {
                            put("dialerProxy", dialerProxyTag)
                        }
                    }
                }
            }
        }
    }

    private fun buildStreamSettings(
        profile: TunnelProfile,
        settings: AppSettings,
        dialerProxyTag: String?,
    ): JsonObject = buildJsonObject {
        val t = profile.transport
        val tls = profile.tlsReality
        put("network", t.type.wireValue)
        put("security", tls.security.wireValue)

        when (tls.security) {
            SecurityMode.TLS -> putJsonObject("tlsSettings") {
                put("serverName", tls.sni.ifBlank { profile.serverAddress })
                // Xray-core v26.6+ removed `allowInsecure` and migrated to `pinnedPeerCertSha256` (pcs)
                // and `verifyPeerCertByName` (vcn). Never emit `allowInsecure: true`.
                put("fingerprint", tls.fingerprint.wireValue)
                if (tls.alpn.isNotEmpty()) {
                    putJsonArray("alpn") { tls.alpn.forEach { add(JsonPrimitive(it)) } }
                }
                if (tls.echConfigList.isNotBlank()) {
                    put("echConfigList", tls.echConfigList)
                }
                if (tls.pinnedPeerCertSha256.isNotBlank()) {
                    put("pinnedPeerCertSha256", tls.pinnedPeerCertSha256)
                }
                val effectiveVerifyName = when {
                    tls.verifyPeerCertByName.isNotBlank() -> tls.verifyPeerCertByName
                    tls.allowInsecure &&
                        tls.pinnedPeerCertSha256.isBlank() &&
                        tls.sni.isNotBlank() &&
                        !tls.sni.equals(profile.serverAddress, ignoreCase = true) -> profile.serverAddress
                    else -> ""
                }
                if (effectiveVerifyName.isNotBlank()) {
                    put("verifyPeerCertByName", effectiveVerifyName)
                }
            }

            SecurityMode.REALITY -> putJsonObject("realitySettings") {
                put("serverName", tls.sni.ifBlank { profile.serverAddress })
                put("fingerprint", tls.fingerprint.wireValue)
                put("publicKey", tls.realityPublicKey)
                put("shortId", tls.realityShortId)
                put("spiderX", tls.realitySpiderX.ifBlank { "/" })
                if (tls.mldsa65Verify.isNotBlank()) {
                    put("mldsa65Verify", tls.mldsa65Verify)
                }
            }

            SecurityMode.NONE -> Unit
        }

        when (t.type) {
            TransportType.WS -> putJsonObject("wsSettings") {
                put("path", t.path.ifBlank { "/" })
                if (t.host.isNotBlank()) {
                    putJsonObject("headers") {
                        put("Host", t.host)
                    }
                }
            }

            TransportType.HTTP_UPGRADE -> putJsonObject("httpupgradeSettings") {
                put("path", t.path.ifBlank { "/" })
                if (t.host.isNotBlank()) put("host", t.host)
            }

            TransportType.XHTTP -> putJsonObject("xhttpSettings") {
                put("path", t.path.ifBlank { "/" })
                if (t.host.isNotBlank()) put("host", t.host)
                put("mode", t.xhttp.mode)
                put("xPaddingBytes", t.xhttp.xPaddingBytes)
                put("scMaxEachPostBytes", t.xhttp.scMaxEachPostBytes)
                put("scMinPostsIntervalMs", t.xhttp.scMinPostsIntervalMs)
                putJsonObject("xmux") {
                    put("maxConcurrency", t.xhttp.maxConcurrency)
                    put("maxConnections", t.xhttp.maxConnections)
                    put("cMaxReuseTimes", t.xhttp.cMaxReuseTimes)
                    put("hMaxRequestTimes", t.xhttp.hMaxRequestTimes)
                    put("hMaxReusableSecs", t.xhttp.hMaxReusableSecs)
                }
            }

            TransportType.GRPC -> putJsonObject("grpcSettings") {
                put("serviceName", t.serviceName)
                put("multiMode", t.grpcMode.equals("multi", ignoreCase = true))
                if (t.host.isNotBlank()) put("authority", t.host)
            }

            TransportType.H2 -> putJsonObject("httpSettings") {
                put("path", t.path.ifBlank { "/" })
                if (t.host.isNotBlank()) {
                    putJsonArray("host") { add(JsonPrimitive(t.host)) }
                }
            }

            TransportType.MKCP -> putJsonObject("kcpSettings") {
                if (t.kcpSeed.isNotBlank()) put("seed", t.kcpSeed)
                putJsonObject("header") { put("type", t.kcpHeaderType.ifBlank { "none" }) }
            }

            TransportType.QUIC -> putJsonObject("quicSettings") {
                put("security", "none")
                putJsonObject("header") { put("type", t.kcpHeaderType.ifBlank { "none" }) }
            }

            TransportType.RAW_TCP -> Unit
        }

        // Xray v26.3+ Unified FinalMask Anti-DPI (TLS ClientHello Fragmentation + UDP Noise)
        val useFragment = profile.antiDpi.tlsFragmentEnabled || settings.globalAntiDpi.forceTlsFragment
        val useNoise = profile.antiDpi.udpNoiseEnabled || settings.globalAntiDpi.forceUdpNoise
        if (useFragment || useNoise) {
            putJsonObject("finalmask") {
                if (useFragment) {
                    val length = if (profile.antiDpi.tlsFragmentEnabled) {
                        profile.antiDpi.fragmentLength
                    } else {
                        settings.globalAntiDpi.defaultFragmentLength
                    }
                    val delay = if (profile.antiDpi.tlsFragmentEnabled) {
                        profile.antiDpi.fragmentIntervalMs
                    } else {
                        settings.globalAntiDpi.defaultFragmentDelayMs
                    }
                    putJsonArray("tcp") {
                        add(
                            buildJsonObject {
                                put("type", "fragment")
                                putJsonObject("settings") {
                                    put("packets", profile.antiDpi.fragmentPackets.ifBlank { "tlshello" })
                                    put("length", length)
                                    put("delay", delay)
                                    put("maxSplit", profile.antiDpi.fragmentMaxSplit.ifBlank { "0" })
                                }
                            }
                        )
                    }
                }
                if (useNoise) {
                    val noisePacket = if (profile.antiDpi.udpNoiseEnabled) {
                        profile.antiDpi.udpNoisePacket
                    } else {
                        settings.globalAntiDpi.defaultUdpNoisePacket
                    }
                    val noiseDelay = if (profile.antiDpi.udpNoiseEnabled) {
                        profile.antiDpi.udpNoiseDelayMs
                    } else {
                        settings.globalAntiDpi.defaultUdpNoiseDelayMs
                    }
                    putJsonArray("udp") {
                        add(
                            buildJsonObject {
                                put("type", "noise")
                                putJsonObject("settings") {
                                    put("reset", profile.antiDpi.udpNoiseReset.ifBlank { "0" })
                                    putJsonArray("noise") {
                                        add(
                                            buildJsonObject {
                                                put("type", profile.antiDpi.udpNoiseType.ifBlank { "rand" })
                                                put("packet", noisePacket)
                                                put("delay", noiseDelay)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (dialerProxyTag != null) {
            putJsonObject("sockopt") {
                put("dialerProxy", dialerProxyTag)
            }
        }
    }

    private fun buildRouting(plan: com.ceoeg.aetherflow.core.routing.CompiledRoutingPlan): JsonObject =
        buildJsonObject {
            put("domainStrategy", "IPIfNonMatch")
            putJsonArray("rules") {
                // DNS Hijack rule
                add(
                    buildJsonObject {
                        put("type", "field")
                        put("port", 53)
                        put("outboundTag", "dns-out")
                    }
                )

                // IPv6 Leak shield
                if (plan.dns.blockIpv6Leak) {
                    add(
                        buildJsonObject {
                            put("type", "field")
                            putJsonArray("ip") { add(JsonPrimitive("::/0")) }
                            put("outboundTag", "block")
                        }
                    )
                }

                // Compiled ordered rules (WebRTC block, QUIC block, Adblock, Custom, Geo-Bypass)
                plan.orderedRules.forEach { rule ->
                    val items = rule.value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    if (items.isNotEmpty()) {
                        add(
                            buildJsonObject {
                                put("type", "field")
                                put("outboundTag", rule.action.outboundTag)
                                when (rule.matchType) {
                                    RuleMatchType.DOMAIN_SUFFIX -> putJsonArray("domain") {
                                        items.forEach { add(JsonPrimitive("domain:$it")) }
                                    }
                                    RuleMatchType.DOMAIN_KEYWORD -> putJsonArray("domain") {
                                        items.forEach { add(JsonPrimitive("keyword:$it")) }
                                    }
                                    RuleMatchType.DOMAIN_FULL -> putJsonArray("domain") {
                                        items.forEach { add(JsonPrimitive("full:$it")) }
                                    }
                                    RuleMatchType.GEOSITE -> putJsonArray("domain") {
                                        items.forEach { add(JsonPrimitive("geosite:$it")) }
                                    }
                                    RuleMatchType.GEOIP -> putJsonArray("ip") {
                                        items.forEach { add(JsonPrimitive("geoip:$it")) }
                                    }
                                    RuleMatchType.CIDR -> putJsonArray("ip") {
                                        items.forEach { add(JsonPrimitive(it)) }
                                    }
                                    RuleMatchType.PORT -> put("port", items.joinToString(","))
                                    RuleMatchType.PROTOCOL -> putJsonArray("protocol") {
                                        items.forEach { add(JsonPrimitive(it)) }
                                    }
                                    RuleMatchType.PROCESS_PACKAGE -> putJsonArray("domain") {
                                        // Per-app package filtering is enforced primarily at VpnService TUN layer
                                        items.forEach { add(JsonPrimitive("keyword:$it")) }
                                    }
                                }
                            }
                        )
                    }
                }

                // Private LAN CIDRs always direct
                add(
                    buildJsonObject {
                        put("type", "field")
                        putJsonArray("ip") {
                            plan.bypassPrivateCidrs.forEach { add(JsonPrimitive(it)) }
                        }
                        put("outboundTag", "direct")
                    }
                )
            }
        }
}
