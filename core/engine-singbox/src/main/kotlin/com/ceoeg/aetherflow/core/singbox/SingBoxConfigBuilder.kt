package com.ceoeg.aetherflow.core.singbox

import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.RoutingRule
import com.ceoeg.aetherflow.core.common.model.RuleMatchType
import com.ceoeg.aetherflow.core.common.model.SecurityMode
import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import com.ceoeg.aetherflow.core.common.model.TransportType
import com.ceoeg.aetherflow.core.common.model.TunBackendMode
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
 * Generates Sing-box v1.14+ (`experimental/libbox`) JSON configurations supporting:
 * - Hysteria & Hysteria 2 (Brutal CC, Salamander obfs, UDP port hopping)
 * - TUIC v5 (0-RTT, BBR/Cubic, native/QUIC UDP relay)
 * - AnyTLS v2 (TLS-in-TLS padding & idle session multiplexing)
 * - ShadowTLS v3 (`detour` transport wrapper paired with inner Shadowsocks-2022)
 * - NaiveProxy, Juicity, Mieru, SSH (PEM/Ed25519 key + host key pinning)
 * - Split DNS, FakeIP, `.srs` RuleSets, and authenticated loopback Clash API controller
 */
@Singleton
class SingBoxConfigBuilder @Inject constructor(
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
            return profile.rawCustomConfig
        }

        val plan = routingCompiler.compile(settings, customRules)

        val root = buildJsonObject {
            put("\$schema", "https://sing-box.sagernet.org/schema/config.json")

            putJsonObject("log") {
                val sbLevel = when (settings.logSeverity.wireLevel) {
                    "none" -> "panic"
                    "warning" -> "warn"
                    else -> settings.logSeverity.wireLevel
                }
                put("level", sbLevel)
                put("timestamp", true)
            }

            put("dns", buildDnsBlock(plan))
            put("inbounds", buildInbounds(settings, sessionAuth))
            put("outbounds", buildOutbounds(profile, detourExitProfile))
            put("route", buildRouteBlock(plan, settings))

            putJsonObject("experimental") {
                putJsonObject("cache_file") {
                    put("enabled", true)
                    put("store_fakeip", plan.dns.fakeIpEnabled)
                }
                putJsonObject("clash_api") {
                    put("external_controller", "127.0.0.1:${sessionAuth.controlApiPort}")
                    put("secret", sessionAuth.clashApiSecret)
                }
            }
        }

        return prettyJson.encodeToString(JsonObject.serializer(), root)
    }

    private fun buildDnsBlock(plan: com.ceoeg.aetherflow.core.routing.CompiledRoutingPlan): JsonObject =
        buildJsonObject {
            put("strategy", if (plan.dns.blockIpv6Leak) "ipv4_only" else "prefer_ipv4")
            putJsonArray("servers") {
                add(
                    buildJsonObject {
                        put("tag", "dns-remote")
                        put("address", plan.dns.remoteAddress)
                        put("detour", "proxy")
                    }
                )
                add(
                    buildJsonObject {
                        put("tag", "dns-direct")
                        put("address", plan.dns.directAddress)
                        put("detour", "direct")
                    }
                )
                if (plan.dns.fakeIpEnabled) {
                    add(
                        buildJsonObject {
                            put("tag", "dns-fakeip")
                            put("address", "fakeip")
                        }
                    )
                }
            }
            putJsonArray("rules") {
                add(
                    buildJsonObject {
                        put("outbound", "any")
                        put("server", "dns-direct")
                    }
                )
                if (plan.dns.splitDnsEnabled && plan.dns.domesticCountryCode.isNotBlank()) {
                    add(
                        buildJsonObject {
                            putJsonArray("geosite") { add(JsonPrimitive(plan.dns.domesticCountryCode)) }
                            put("server", "dns-direct")
                        }
                    )
                }
                if (plan.dns.fakeIpEnabled) {
                    add(
                        buildJsonObject {
                            putJsonArray("query_type") {
                                add(JsonPrimitive("A"))
                                if (!plan.dns.blockIpv6Leak) add(JsonPrimitive("AAAA"))
                            }
                            put("server", "dns-fakeip")
                        }
                    )
                }
            }
            if (plan.dns.fakeIpEnabled) {
                putJsonObject("fakeip") {
                    put("enabled", true)
                    put("inet4_range", plan.dns.fakeIpv4Cidr)
                    if (!plan.dns.blockIpv6Leak) {
                        put("inet6_range", plan.dns.fakeIpv6Cidr)
                    }
                }
            }
        }

    private fun buildInbounds(
        settings: AppSettings,
        sessionAuth: SessionLoopbackAuth,
    ): JsonArray = buildJsonArray {
        if (settings.tunSettings.backend == TunBackendMode.CORE_NATIVE_TUN) {
            add(
                buildJsonObject {
                    put("type", "tun")
                    put("tag", "tun-in")
                    putJsonArray("address") {
                        add(JsonPrimitive("172.19.0.1/30"))
                        if (settings.tunSettings.enableIpv6) {
                            add(JsonPrimitive("fdfe:dcba:9876::1/126"))
                        }
                    }
                    put("mtu", settings.tunSettings.mtu.coerceIn(1280, 9000))
                    put("auto_route", true)
                    put("strict_route", settings.tunSettings.strictRoute)
                    put("stack", "system")
                }
            )
        }

        // Synchronized ephemeral authenticated SOCKS5 inbound
        add(
            buildJsonObject {
                put("type", "socks")
                put("tag", "socks-in")
                put("listen", "127.0.0.1")
                put("listen_port", sessionAuth.socksPort)
                putJsonArray("users") {
                    add(
                        buildJsonObject {
                            put("username", sessionAuth.username)
                            put("password", sessionAuth.password)
                        }
                    )
                }
            }
        )

        add(
            buildJsonObject {
                put("type", "http")
                put("tag", "http-in")
                put("listen", "127.0.0.1")
                put("listen_port", sessionAuth.httpPort)
                putJsonArray("users") {
                    add(
                        buildJsonObject {
                            put("username", sessionAuth.username)
                            put("password", sessionAuth.password)
                        }
                    )
                }
            }
        )

        val lan = settings.lanTethering
        if (lan.enabled) {
            val listenAddr = lan.bindAddress.ifBlank { "0.0.0.0" }
            val useAuth = lan.requireAuth && lan.password.isNotBlank()
            add(
                buildJsonObject {
                    put("type", "mixed")
                    put("tag", "lan-mixed-in")
                    put("listen", listenAddr)
                    put("listen_port", lan.socksPort)
                    put("sniff", true)
                    put("sniff_override_destination", true)
                    if (useAuth) {
                        putJsonArray("users") {
                            add(
                                buildJsonObject {
                                    put("username", lan.username)
                                    put("password", lan.password)
                                }
                            )
                        }
                    }
                }
            )
            if (lan.httpPort != lan.socksPort) {
                add(
                    buildJsonObject {
                        put("type", "http")
                        put("tag", "lan-http-in")
                        put("listen", listenAddr)
                        put("listen_port", lan.httpPort)
                        put("sniff", true)
                        put("sniff_override_destination", true)
                        if (useAuth) {
                            putJsonArray("users") {
                                add(
                                    buildJsonObject {
                                        put("username", lan.username)
                                        put("password", lan.password)
                                    }
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    private fun buildOutbounds(
        profile: TunnelProfile,
        detourExitProfile: TunnelProfile?,
    ): JsonArray = buildJsonArray {
        if (detourExitProfile != null) {
            // Entry Node ("entry-hop") -> Detour Exit ("proxy")
            emitProfileOutbounds(
                targetArray = this,
                primaryTag = "proxy",
                profile = detourExitProfile,
                detourTag = "entry-hop",
            )
            emitProfileOutbounds(
                targetArray = this,
                primaryTag = "entry-hop",
                profile = profile,
                detourTag = null,
            )
        } else {
            emitProfileOutbounds(
                targetArray = this,
                primaryTag = "proxy",
                profile = profile,
                detourTag = null,
            )
        }

        add(
            buildJsonObject {
                put("type", "direct")
                put("tag", "direct")
            }
        )
        add(
            buildJsonObject {
                put("type", "block")
                put("tag", "block")
            }
        )
    }

    private fun emitProfileOutbounds(
        targetArray: kotlinx.serialization.json.JsonArrayBuilder,
        primaryTag: String,
        profile: TunnelProfile,
        detourTag: String?,
    ) {
        // ShadowTLS v3 requires two chained outbounds: inner proxy -> shadowtls transport
        if (profile.protocol == ProtocolType.SHADOWTLS_V3) {
            val stlsTag = "$primaryTag-shadowtls-transport"
            targetArray.add(
                buildJsonObject {
                    put("type", "shadowsocks")
                    put("tag", primaryTag)
                    put("method", profile.quicExtra.innerMethod.ifBlank { "2022-blake3-aes-128-gcm" })
                    put("password", profile.quicExtra.innerPassword.ifBlank { profile.password })
                    put("detour", stlsTag)
                }
            )
            targetArray.add(
                buildJsonObject {
                    put("type", "shadowtls")
                    put("tag", stlsTag)
                    put("server", profile.serverAddress)
                    put("server_port", profile.serverPort)
                    put("version", profile.quicExtra.shadowTlsVersion.coerceIn(1, 3))
                    put("password", profile.password.ifBlank { profile.quicExtra.shadowTlsPassword })
                    put("tls", buildSingBoxTls(profile, forceEnabled = true))
                    if (detourTag != null) put("detour", detourTag)
                }
            )
            return
        }

        targetArray.add(
            buildJsonObject {
                put("tag", primaryTag)
                if (detourTag != null) put("detour", detourTag)

                when (profile.protocol) {
                    ProtocolType.HYSTERIA2 -> {
                        put("type", "hysteria2")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        if (profile.quicExtra.portHoppingRange.isNotBlank()) {
                            putJsonArray("server_ports") {
                                profile.quicExtra.portHoppingRange
                                    .split(',')
                                    .map { it.trim().replace('-', ':') }
                                    .filter { it.isNotEmpty() }
                                    .forEach { add(JsonPrimitive(it)) }
                            }
                            put("hop_interval", "${profile.quicExtra.hopIntervalSec}s")
                        }
                        put("up_mbps", profile.quicExtra.upMbps)
                        put("down_mbps", profile.quicExtra.downMbps)
                        put("password", profile.password)
                        if (profile.quicExtra.salamanderPassword.isNotBlank()) {
                            putJsonObject("obfs") {
                                put("type", "salamander")
                                put("password", profile.quicExtra.salamanderPassword)
                            }
                        }
                        put("tls", buildSingBoxTls(profile, forceEnabled = true, defaultAlpn = listOf("h3")))
                    }

                    ProtocolType.HYSTERIA -> {
                        put("type", "hysteria")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("up_mbps", profile.quicExtra.upMbps)
                        put("down_mbps", profile.quicExtra.downMbps)
                        put("auth_str", profile.password)
                        if (profile.quicExtra.salamanderPassword.isNotBlank()) {
                            put("obfs", profile.quicExtra.salamanderPassword)
                        }
                        put("tls", buildSingBoxTls(profile, forceEnabled = true))
                    }

                    ProtocolType.TUIC_V5 -> {
                        put("type", "tuic")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("uuid", profile.uuid)
                        put("password", profile.password)
                        put("congestion_control", profile.quicExtra.tuicCongestionControl)
                        put("udp_relay_mode", profile.quicExtra.tuicUdpRelayMode)
                        put("zero_rtt_handshake", profile.quicExtra.tuicZeroRtt)
                        put("heartbeat", "${profile.quicExtra.tuicHeartbeatSec}s")
                        put("tls", buildSingBoxTls(profile, forceEnabled = true, defaultAlpn = listOf("h3")))
                    }

                    ProtocolType.ANYTLS_V2 -> {
                        put("type", "anytls")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("password", profile.password)
                        put("min_idle_session", profile.quicExtra.anyTlsMinIdleSession)
                        put("idle_session_check_interval", "${profile.quicExtra.anyTlsIdleCheckIntervalSec}s")
                        put("idle_session_timeout", "${profile.quicExtra.anyTlsIdleTimeoutSec}s")
                        put("tls", buildSingBoxTls(profile, forceEnabled = true))
                    }

                    ProtocolType.NAIVE_PROXY -> {
                        put("type", "naive")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("username", profile.username)
                        put("password", profile.password)
                        put("quic", profile.quicExtra.naiveQuic)
                        put("insecure_concurrency", profile.quicExtra.naiveConcurrency)
                        putJsonObject("tls") {
                            put("enabled", true)
                            put("server_name", profile.tlsReality.sni.ifBlank { profile.serverAddress })
                        }
                    }

                    ProtocolType.JUICITY -> {
                        put("type", "juicity")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("uuid", profile.uuid)
                        put("password", profile.password)
                        put("tls", buildSingBoxTls(profile, forceEnabled = true))
                    }

                    ProtocolType.MIERU -> {
                        put("type", "mieru")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("username", profile.username)
                        put("password", profile.password)
                        put("transport", profile.quicExtra.mieruTransport)
                        put("multiplexing", profile.quicExtra.mieruMultiplexing)
                    }

                    ProtocolType.SSH -> {
                        put("type", "ssh")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("user", profile.username.ifBlank { profile.quicExtra.sshUser.ifBlank { "root" } })
                        if (profile.password.isNotBlank()) put("password", profile.password)
                        if (profile.quicExtra.sshPrivateKeyPem.isNotBlank()) {
                            putJsonArray("private_key") {
                                add(JsonPrimitive(profile.quicExtra.sshPrivateKeyPem))
                            }
                        }
                        if (profile.quicExtra.sshPrivateKeyPassphrase.isNotBlank()) {
                            put("private_key_passphrase", profile.quicExtra.sshPrivateKeyPassphrase)
                        }
                        if (profile.quicExtra.sshHostKey.isNotBlank()) {
                            putJsonArray("host_key") {
                                add(JsonPrimitive(profile.quicExtra.sshHostKey))
                            }
                        }
                        if (profile.quicExtra.sshClientVersion.isNotBlank()) {
                            put("client_version", profile.quicExtra.sshClientVersion)
                        }
                    }

                    ProtocolType.VLESS -> {
                        put("type", "vless")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("uuid", profile.uuid)
                        if (profile.flow.isNotBlank()) put("flow", profile.flow)
                        if (profile.tlsReality.security != SecurityMode.NONE) {
                            put("tls", buildSingBoxTls(profile, forceEnabled = true))
                        }
                        buildSingBoxTransport(profile)?.let { put("transport", it) }
                    }

                    ProtocolType.VMESS -> {
                        put("type", "vmess")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("uuid", profile.uuid)
                        put("alter_id", profile.alterId)
                        put("security", profile.method.ifBlank { "auto" })
                        if (profile.tlsReality.security != SecurityMode.NONE) {
                            put("tls", buildSingBoxTls(profile, forceEnabled = true))
                        }
                        buildSingBoxTransport(profile)?.let { put("transport", it) }
                    }

                    ProtocolType.TROJAN -> {
                        put("type", "trojan")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("password", profile.password)
                        put("tls", buildSingBoxTls(profile, forceEnabled = true))
                        buildSingBoxTransport(profile)?.let { put("transport", it) }
                    }

                    ProtocolType.SHADOWSOCKS -> {
                        put("type", "shadowsocks")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        put("method", profile.method.ifBlank { "2022-blake3-aes-128-gcm" })
                        put("password", profile.password)
                    }

                    ProtocolType.WIREGUARD, ProtocolType.AMNEZIA_WG -> {
                        val wg = profile.wireGuard
                        put("type", "wireguard")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        putJsonArray("local_address") {
                            wg.localAddresses.forEach { add(JsonPrimitive(it)) }
                        }
                        put("private_key", wg.privateKey)
                        put("peer_public_key", wg.peerPublicKey)
                        if (wg.presharedKey.isNotBlank()) put("pre_shared_key", wg.presharedKey)
                        put("mtu", wg.mtu)
                        if (wg.reservedBytes.size == 3) {
                            putJsonArray("reserved") {
                                wg.reservedBytes.forEach { add(JsonPrimitive(it)) }
                            }
                        }
                    }

                    else -> {
                        put("type", "socks")
                        put("server", profile.serverAddress)
                        put("server_port", profile.serverPort)
                        if (profile.username.isNotBlank()) put("username", profile.username)
                        if (profile.password.isNotBlank()) put("password", profile.password)
                    }
                }
            }
        )
    }

    private fun buildSingBoxTls(
        profile: TunnelProfile,
        forceEnabled: Boolean,
        defaultAlpn: List<String> = emptyList(),
    ): JsonObject = buildJsonObject {
        val tls = profile.tlsReality
        put("enabled", forceEnabled || tls.security != SecurityMode.NONE)
        put("server_name", tls.sni.ifBlank { profile.serverAddress })
        put("insecure", tls.allowInsecure)
        val effectiveAlpn = tls.alpn.ifEmpty { defaultAlpn }
        if (effectiveAlpn.isNotEmpty()) {
            putJsonArray("alpn") { effectiveAlpn.forEach { add(JsonPrimitive(it)) } }
        }
        putJsonObject("utls") {
            put("enabled", true)
            put("fingerprint", tls.fingerprint.wireValue)
        }
        if (tls.security == SecurityMode.REALITY) {
            putJsonObject("reality") {
                put("enabled", true)
                put("public_key", tls.realityPublicKey)
                put("short_id", tls.realityShortId)
            }
        }
        if (tls.echConfigList.isNotBlank()) {
            putJsonObject("ech") {
                put("enabled", true)
                putJsonArray("config") { add(JsonPrimitive(tls.echConfigList)) }
            }
        }
    }

    private fun buildSingBoxTransport(profile: TunnelProfile): JsonObject? {
        val t = profile.transport
        return when (t.type) {
            TransportType.WS -> buildJsonObject {
                put("type", "ws")
                put("path", t.path.ifBlank { "/" })
                if (t.host.isNotBlank()) {
                    putJsonObject("headers") { put("Host", t.host) }
                }
            }
            TransportType.HTTP_UPGRADE -> buildJsonObject {
                put("type", "httpupgrade")
                put("path", t.path.ifBlank { "/" })
                if (t.host.isNotBlank()) put("host", t.host)
            }
            TransportType.GRPC -> buildJsonObject {
                put("type", "grpc")
                put("service_name", t.serviceName)
            }
            TransportType.H2 -> buildJsonObject {
                put("type", "http")
                put("path", t.path.ifBlank { "/" })
                if (t.host.isNotBlank()) {
                    putJsonArray("host") { add(JsonPrimitive(t.host)) }
                }
            }
            else -> null
        }
    }

    private fun buildRouteBlock(
        plan: com.ceoeg.aetherflow.core.routing.CompiledRoutingPlan,
        settings: AppSettings,
    ): JsonObject = buildJsonObject {
        put("auto_detect_interface", true)
        put("final", plan.finalOutboundTag)

        putJsonArray("rules") {
            add(
                buildJsonObject {
                    put("protocol", "dns")
                    put("action", "hijack-dns")
                }
            )
            add(
                buildJsonObject {
                    put("ip_is_private", true)
                    put("outbound", "direct")
                }
            )
            if (settings.dnsSettings.blockIpv6Leak) {
                add(
                    buildJsonObject {
                        putJsonArray("ip_cidr") { add(JsonPrimitive("::/0")) }
                        put("outbound", "block")
                    }
                )
            }
            plan.orderedRules.forEach { rule ->
                val items = rule.value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                if (items.isNotEmpty()) {
                    add(
                        buildJsonObject {
                            put("outbound", rule.action.outboundTag)
                            when (rule.matchType) {
                                RuleMatchType.DOMAIN_SUFFIX -> putJsonArray("domain_suffix") {
                                    items.forEach { add(JsonPrimitive(it)) }
                                }
                                RuleMatchType.DOMAIN_KEYWORD -> putJsonArray("domain_keyword") {
                                    items.forEach { add(JsonPrimitive(it)) }
                                }
                                RuleMatchType.DOMAIN_FULL -> putJsonArray("domain") {
                                    items.forEach { add(JsonPrimitive(it)) }
                                }
                                RuleMatchType.GEOSITE -> putJsonArray("geosite") {
                                    items.forEach { add(JsonPrimitive(it)) }
                                }
                                RuleMatchType.GEOIP -> putJsonArray("geoip") {
                                    items.forEach { add(JsonPrimitive(it)) }
                                }
                                RuleMatchType.CIDR -> putJsonArray("ip_cidr") {
                                    items.forEach { add(JsonPrimitive(it)) }
                                }
                                RuleMatchType.PORT -> putJsonArray("port") {
                                    items.mapNotNull { it.toIntOrNull() }.forEach { add(JsonPrimitive(it)) }
                                }
                                RuleMatchType.PROTOCOL -> putJsonArray("protocol") {
                                    items.forEach { add(JsonPrimitive(it)) }
                                }
                                RuleMatchType.PROCESS_PACKAGE -> putJsonArray("package_name") {
                                    items.forEach { add(JsonPrimitive(it)) }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
