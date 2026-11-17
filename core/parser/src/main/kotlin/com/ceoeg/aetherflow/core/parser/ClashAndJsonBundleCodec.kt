package com.ceoeg.aetherflow.core.parser

import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.QuicProtocolSettings
import com.ceoeg.aetherflow.core.common.model.SecurityMode
import com.ceoeg.aetherflow.core.common.model.TlsRealityConfig
import com.ceoeg.aetherflow.core.common.model.TransportSettings
import com.ceoeg.aetherflow.core.common.model.TransportType
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.model.UtlsFingerprint
import com.ceoeg.aetherflow.core.common.model.WireGuardSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
 * Bidirectional parser & serializer for Clash/Mihomo YAML, Sing-box JSON,
 * Xray Custom JSON, SIP008 JSON, and Subscription-Userinfo quota headers.
 */
object ClashAndJsonBundleCodec {

    private val prettyJson = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseClashYaml(yamlText: String, subscriptionId: String? = null): List<TunnelProfile> {
        val loaderOpts = LoaderOptions().apply {
            codePointLimit = 8 * 1024 * 1024
            maxAliasesForCollections = 64
        }
        val yaml = Yaml(SafeConstructor(loaderOpts))
        val root = runCatching { yaml.load<Any>(yamlText) }.getOrNull() as? Map<*, *> ?: return emptyList()
        val proxies = root["proxies"] as? List<*> ?: return emptyList()
        return proxies.mapNotNull { entry ->
            (entry as? Map<*, *>)?.let { parseClashProxyNode(it, subscriptionId) }
        }
    }

    private fun parseClashProxyNode(node: Map<*, *>, subscriptionId: String?): TunnelProfile? {
        fun str(key: String, default: String = ""): String = node[key]?.toString()?.trim() ?: default
        fun intVal(key: String, default: Int): Int = str(key).toIntOrNull() ?: default
        fun boolVal(key: String, default: Boolean = false): Boolean =
            node[key]?.toString()?.lowercase()?.toBooleanStrictOrNull() ?: default

        val type = str("type").lowercase()
        val name = str("name", "Clash Endpoint")
        val server = str("server")
        val port = intVal("port", 443)
        if (server.isBlank()) return null

        val networkStr = str("network", "tcp").lowercase()
        val transportType = when (networkStr) {
            "ws" -> TransportType.WS
            "grpc" -> TransportType.GRPC
            "h2", "http" -> TransportType.H2
            else -> TransportType.RAW_TCP
        }
        val wsOpts = node["ws-opts"] as? Map<*, *>
        val grpcOpts = node["grpc-opts"] as? Map<*, *>
        val wsHeaders = (wsOpts?.get("headers") as? Map<*, *>)
        val transport = TransportSettings(
            type = transportType,
            host = wsHeaders?.get("Host")?.toString() ?: wsHeaders?.get("host")?.toString() ?: "",
            path = wsOpts?.get("path")?.toString() ?: "",
            serviceName = grpcOpts?.get("grpc-service-name")?.toString() ?: "",
        )

        val realityOpts = node["reality-opts"] as? Map<*, *>
        val isReality = realityOpts != null
        val isTls = boolVal("tls", type in setOf("trojan", "hysteria2", "tuic", "anytls"))
        val fpStr = str("client-fingerprint", "chrome").lowercase()
        val fp = UtlsFingerprint.entries.firstOrNull { it.wireValue == fpStr } ?: UtlsFingerprint.CHROME
        val alpnList = (node["alpn"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

        val tlsReality = TlsRealityConfig(
            security = when {
                isReality -> SecurityMode.REALITY
                isTls -> SecurityMode.TLS
                else -> SecurityMode.NONE
            },
            sni = str("servername").ifBlank { str("sni") },
            alpn = alpnList,
            allowInsecure = boolVal("skip-cert-verify", false),
            fingerprint = fp,
            realityPublicKey = realityOpts?.get("public-key")?.toString() ?: "",
            realityShortId = realityOpts?.get("short-id")?.toString() ?: "",
            mldsa65Verify = realityOpts?.get("mldsa65-verify")?.toString()
                ?: realityOpts?.get("support-x25519mlkem768")?.toString()?.takeIf { it.length > 16 } ?: "",
        )

        return when (type) {
            "vless" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.VLESS,
                serverAddress = server,
                serverPort = port,
                uuid = str("uuid"),
                flow = str("flow"),
                transport = transport,
                tlsReality = tlsReality,
            )
            "vmess" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.VMESS,
                serverAddress = server,
                serverPort = port,
                uuid = str("uuid"),
                alterId = intVal("alterId", 0),
                method = str("cipher", "auto"),
                transport = transport,
                tlsReality = tlsReality,
            )
            "trojan" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.TROJAN,
                serverAddress = server,
                serverPort = port,
                password = str("password"),
                transport = transport,
                tlsReality = tlsReality,
            )
            "ss" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.SHADOWSOCKS,
                serverAddress = server,
                serverPort = port,
                method = str("cipher", "2022-blake3-aes-128-gcm"),
                password = str("password"),
                plugin = str("plugin"),
            )
            "hysteria2" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.HYSTERIA2,
                serverAddress = server,
                serverPort = port,
                password = str("password"),
                tlsReality = tlsReality.let { if (it.alpn.isEmpty()) it.copy(alpn = listOf("h3")) else it },
                quicExtra = QuicProtocolSettings(
                    upMbps = str("up").filter { it.isDigit() }.toIntOrNull() ?: 100,
                    downMbps = str("down").filter { it.isDigit() }.toIntOrNull() ?: 250,
                    salamanderPassword = if (str("obfs") == "salamander") str("obfs-password") else "",
                    portHoppingRange = str("ports"),
                ),
            )
            "tuic" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.TUIC_V5,
                serverAddress = server,
                serverPort = port,
                uuid = str("uuid"),
                password = str("password"),
                tlsReality = tlsReality,
                quicExtra = QuicProtocolSettings(
                    tuicCongestionControl = str("congestion-controller", "bbr"),
                    tuicUdpRelayMode = str("udp-relay-mode", "native"),
                ),
            )
            "anytls" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.ANYTLS_V2,
                serverAddress = server,
                serverPort = port,
                password = str("password"),
                tlsReality = tlsReality,
                quicExtra = QuicProtocolSettings(
                    anyTlsMinIdleSession = intVal("min-idle-session", 5),
                ),
            )
            "wireguard" -> {
                val awgOpts = node["amnezia-wg-option"] as? Map<*, *>
                val wg = WireGuardSettings(
                    privateKey = str("private-key"),
                    peerPublicKey = str("public-key"),
                    presharedKey = str("pre-shared-key"),
                    localAddresses = listOfNotNull(
                        str("ip").takeIf { it.isNotBlank() },
                        str("ipv6").takeIf { it.isNotBlank() },
                    ).ifEmpty { listOf("172.16.0.2/32") },
                    mtu = intVal("mtu", 1400),
                    reservedBytes = (node["reserved"] as? List<*>)
                        ?.mapNotNull { it?.toString()?.toIntOrNull() } ?: emptyList(),
                    jc = awgOpts?.get("jc")?.toString()?.toIntOrNull() ?: 0,
                    jmin = awgOpts?.get("jmin")?.toString()?.toIntOrNull() ?: 0,
                    jmax = awgOpts?.get("jmax")?.toString()?.toIntOrNull() ?: 0,
                    s1 = awgOpts?.get("s1")?.toString()?.toIntOrNull() ?: 0,
                    s2 = awgOpts?.get("s2")?.toString()?.toIntOrNull() ?: 0,
                    h1 = awgOpts?.get("h1")?.toString() ?: "",
                    h2 = awgOpts?.get("h2")?.toString() ?: "",
                    h3 = awgOpts?.get("h3")?.toString() ?: "",
                    h4 = awgOpts?.get("h4")?.toString() ?: "",
                )
                TunnelProfile(
                    subscriptionId = subscriptionId,
                    name = name,
                    protocol = if (wg.hasAmneziaObfuscation) ProtocolType.AMNEZIA_WG else ProtocolType.WIREGUARD,
                    serverAddress = server,
                    serverPort = port,
                    wireGuard = wg,
                )
            }
            "ssh" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.SSH,
                serverAddress = server,
                serverPort = port,
                username = str("username", "root"),
                password = str("password"),
                quicExtra = QuicProtocolSettings(
                    sshUser = str("username", "root"),
                    sshPrivateKeyPem = str("private-key"),
                ),
            )
            "socks5" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.SOCKS,
                serverAddress = server,
                serverPort = port,
                username = str("username"),
                password = str("password"),
            )
            "http" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = name,
                protocol = ProtocolType.HTTP,
                serverAddress = server,
                serverPort = port,
                username = str("username"),
                password = str("password"),
                tlsReality = tlsReality,
            )
            else -> null
        }
    }

    fun exportToClashYaml(profiles: List<TunnelProfile>): String {
        val proxyMaps = profiles.map { p ->
            val m = LinkedHashMap<String, Any>()
            m["name"] = p.name
            m["server"] = p.serverAddress
            m["port"] = p.serverPort
            m["udp"] = true
            when (p.protocol) {
                ProtocolType.VLESS -> {
                    m["type"] = "vless"
                    m["uuid"] = p.uuid
                    if (p.flow.isNotBlank()) m["flow"] = p.flow
                    applyClashTlsAndTransport(m, p)
                }
                ProtocolType.VMESS -> {
                    m["type"] = "vmess"
                    m["uuid"] = p.uuid
                    m["alterId"] = p.alterId
                    m["cipher"] = p.method.ifBlank { "auto" }
                    applyClashTlsAndTransport(m, p)
                }
                ProtocolType.TROJAN -> {
                    m["type"] = "trojan"
                    m["password"] = p.password
                    applyClashTlsAndTransport(m, p)
                }
                ProtocolType.SHADOWSOCKS -> {
                    m["type"] = "ss"
                    m["cipher"] = p.method
                    m["password"] = p.password
                }
                ProtocolType.HYSTERIA2 -> {
                    m["type"] = "hysteria2"
                    m["password"] = p.password
                    if (p.tlsReality.sni.isNotBlank()) m["sni"] = p.tlsReality.sni
                    m["skip-cert-verify"] = p.tlsReality.allowInsecure
                    if (p.quicExtra.salamanderPassword.isNotBlank()) {
                        m["obfs"] = "salamander"
                        m["obfs-password"] = p.quicExtra.salamanderPassword
                    }
                    if (p.quicExtra.portHoppingRange.isNotBlank()) {
                        m["ports"] = p.quicExtra.portHoppingRange
                    }
                }
                ProtocolType.TUIC_V5 -> {
                    m["type"] = "tuic"
                    m["uuid"] = p.uuid
                    m["password"] = p.password
                    m["congestion-controller"] = p.quicExtra.tuicCongestionControl
                    m["udp-relay-mode"] = p.quicExtra.tuicUdpRelayMode
                    if (p.tlsReality.sni.isNotBlank()) m["sni"] = p.tlsReality.sni
                }
                ProtocolType.ANYTLS_V2 -> {
                    m["type"] = "anytls"
                    m["password"] = p.password
                    if (p.tlsReality.sni.isNotBlank()) m["sni"] = p.tlsReality.sni
                    m["client-fingerprint"] = p.tlsReality.fingerprint.wireValue
                    m["min-idle-session"] = p.quicExtra.anyTlsMinIdleSession
                }
                ProtocolType.WIREGUARD, ProtocolType.AMNEZIA_WG -> {
                    m["type"] = "wireguard"
                    m["ip"] = p.wireGuard.localAddresses.firstOrNull()?.substringBefore('/') ?: "172.16.0.2"
                    m["private-key"] = p.wireGuard.privateKey
                    m["public-key"] = p.wireGuard.peerPublicKey
                    if (p.wireGuard.presharedKey.isNotBlank()) m["pre-shared-key"] = p.wireGuard.presharedKey
                    m["mtu"] = p.wireGuard.mtu
                    if (p.wireGuard.hasAmneziaObfuscation) {
                        m["amnezia-wg-option"] = linkedMapOf(
                            "jc" to p.wireGuard.jc,
                            "jmin" to p.wireGuard.jmin,
                            "jmax" to p.wireGuard.jmax,
                            "s1" to p.wireGuard.s1,
                            "s2" to p.wireGuard.s2,
                            "h1" to p.wireGuard.h1,
                            "h2" to p.wireGuard.h2,
                            "h3" to p.wireGuard.h3,
                            "h4" to p.wireGuard.h4,
                        )
                    }
                }
                ProtocolType.SSH -> {
                    m["type"] = "ssh"
                    m["username"] = p.username.ifBlank { p.quicExtra.sshUser }
                    if (p.password.isNotBlank()) m["password"] = p.password
                    if (p.quicExtra.sshPrivateKeyPem.isNotBlank()) m["private-key"] = p.quicExtra.sshPrivateKeyPem
                }
                else -> {
                    m["type"] = "socks5"
                    if (p.username.isNotBlank()) m["username"] = p.username
                    if (p.password.isNotBlank()) m["password"] = p.password
                }
            }
            m
        }

        val root = linkedMapOf<String, Any>(
            "mixed-port" to 7890,
            "allow-lan" to false,
            "mode" to "rule",
            "proxies" to proxyMaps,
            "proxy-groups" to listOf(
                linkedMapOf(
                    "name" to "AetherFlow-Auto",
                    "type" to "url-test",
                    "url" to "https://cp.cloudflare.com/generate_204",
                    "interval" to 300,
                    "proxies" to profiles.map { it.name },
                )
            ),
            "rules" to listOf("MATCH,AetherFlow-Auto"),
        )
        val dumper = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
        }
        return Yaml(dumper).dump(root)
    }

    private fun applyClashTlsAndTransport(m: MutableMap<String, Any>, p: TunnelProfile) {
        val tls = p.tlsReality
        if (tls.security != SecurityMode.NONE) {
            m["tls"] = true
            if (tls.sni.isNotBlank()) m["servername"] = tls.sni
            m["client-fingerprint"] = tls.fingerprint.wireValue
            m["skip-cert-verify"] = tls.allowInsecure
            if (tls.alpn.isNotEmpty()) m["alpn"] = tls.alpn
            if (tls.security == SecurityMode.REALITY) {
                val realityMap = linkedMapOf<String, Any>(
                    "public-key" to tls.realityPublicKey,
                    "short-id" to tls.realityShortId,
                )
                if (tls.mldsa65Verify.isNotBlank()) {
                    realityMap["mldsa65-verify"] = tls.mldsa65Verify
                }
                m["reality-opts"] = realityMap
            }
        }
        when (p.transport.type) {
            TransportType.WS -> {
                m["network"] = "ws"
                val ws = linkedMapOf<String, Any>("path" to p.transport.path.ifBlank { "/" })
                if (p.transport.host.isNotBlank()) ws["headers"] = mapOf("Host" to p.transport.host)
                m["ws-opts"] = ws
            }
            TransportType.GRPC -> {
                m["network"] = "grpc"
                m["grpc-opts"] = mapOf("grpc-service-name" to p.transport.serviceName)
            }
            else -> m["network"] = "tcp"
        }
    }

    /** Parses SIP008 JSON, Sing-box JSON, or Xray JSON configurations. */
    fun parseJsonBundle(jsonText: String, subscriptionId: String? = null): List<TunnelProfile> {
        val element = runCatching { prettyJson.parseToJsonElement(jsonText) }.getOrNull() ?: return emptyList()
        val obj = element as? JsonObject ?: return emptyList()

        // 1. SIP008 Shadowsocks JSON: { "version": 1, "servers": [ ... ] }
        val sipServers = obj["servers"] as? JsonArray
        if (sipServers != null) {
            return sipServers.mapNotNull { item ->
                val s = item as? JsonObject ?: return@mapNotNull null
                val host = s["server"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val port = s["server_port"]?.jsonPrimitive?.intOrNull ?: 8388
                TunnelProfile(
                    subscriptionId = subscriptionId,
                    name = s["remarks"]?.jsonPrimitive?.contentOrNull?.ifBlank { "$host:$port" } ?: "$host:$port",
                    protocol = ProtocolType.SHADOWSOCKS,
                    serverAddress = host,
                    serverPort = port,
                    method = s["method"]?.jsonPrimitive?.contentOrNull ?: "2022-blake3-aes-128-gcm",
                    password = s["password"]?.jsonPrimitive?.contentOrNull ?: "",
                    plugin = s["plugin"]?.jsonPrimitive?.contentOrNull ?: "",
                    pluginOpts = s["plugin_opts"]?.jsonPrimitive?.contentOrNull ?: "",
                )
            }
        }

        // 2. Sing-box JSON outbounds (identified by server_port or $schema)
        val outbounds = obj["outbounds"] as? JsonArray
        if (outbounds != null) {
            val parsedSingBox = outbounds.mapNotNull { item ->
                parseSingBoxOutboundJson(item as? JsonObject ?: return@mapNotNull null, subscriptionId)
            }
            if (parsedSingBox.isNotEmpty()) return parsedSingBox

            // 3. Xray JSON outbounds (identified by settings.vnext or settings.servers)
            val parsedXray = outbounds.mapNotNull { item ->
                parseXrayOutboundJson(item as? JsonObject ?: return@mapNotNull null, subscriptionId)
            }
            if (parsedXray.isNotEmpty()) return parsedXray
        }

        // Fallback: preserve as raw Custom JSON profile
        return listOf(
            TunnelProfile(
                subscriptionId = subscriptionId,
                name = obj["remarks"]?.jsonPrimitive?.contentOrNull ?: "Custom Core Config",
                protocol = ProtocolType.CUSTOM_JSON,
                rawCustomConfig = jsonText,
            )
        )
    }

    private fun parseSingBoxOutboundJson(o: JsonObject, subscriptionId: String?): TunnelProfile? {
        val type = o["type"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: return null
        if (type in setOf("direct", "block", "dns", "selector", "urltest")) return null
        val server = o["server"]?.jsonPrimitive?.contentOrNull ?: return null
        val port = o["server_port"]?.jsonPrimitive?.intOrNull ?: 443
        val tag = o["tag"]?.jsonPrimitive?.contentOrNull ?: "$server:$port"

        val tlsObj = o["tls"] as? JsonObject
        val realityObj = tlsObj?.get("reality") as? JsonObject
        val utlsObj = tlsObj?.get("utls") as? JsonObject
        val tlsEnabled = tlsObj?.get("enabled")?.jsonPrimitive?.booleanOrNull == true
        val realityEnabled = realityObj?.get("enabled")?.jsonPrimitive?.booleanOrNull == true
        val fpStr = utlsObj?.get("fingerprint")?.jsonPrimitive?.contentOrNull ?: "chrome"
        val fp = UtlsFingerprint.entries.firstOrNull { it.wireValue == fpStr } ?: UtlsFingerprint.CHROME

        val tlsConfig = TlsRealityConfig(
            security = when {
                realityEnabled -> SecurityMode.REALITY
                tlsEnabled -> SecurityMode.TLS
                else -> SecurityMode.NONE
            },
            sni = tlsObj?.get("server_name")?.jsonPrimitive?.contentOrNull ?: "",
            allowInsecure = tlsObj?.get("insecure")?.jsonPrimitive?.booleanOrNull == true,
            fingerprint = fp,
            realityPublicKey = realityObj?.get("public_key")?.jsonPrimitive?.contentOrNull ?: "",
            realityShortId = realityObj?.get("short_id")?.jsonPrimitive?.contentOrNull ?: "",
        )

        return when (type) {
            "hysteria2" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = tag,
                protocol = ProtocolType.HYSTERIA2,
                serverAddress = server,
                serverPort = port,
                password = o["password"]?.jsonPrimitive?.contentOrNull ?: "",
                tlsReality = tlsConfig,
                quicExtra = QuicProtocolSettings(
                    upMbps = o["up_mbps"]?.jsonPrimitive?.intOrNull ?: 100,
                    downMbps = o["down_mbps"]?.jsonPrimitive?.intOrNull ?: 250,
                    salamanderPassword = (o["obfs"] as? JsonObject)
                        ?.get("password")?.jsonPrimitive?.contentOrNull ?: "",
                    portHoppingRange = (o["server_ports"] as? JsonArray)
                        ?.firstOrNull()?.jsonPrimitive?.contentOrNull ?: "",
                ),
            )
            "tuic" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = tag,
                protocol = ProtocolType.TUIC_V5,
                serverAddress = server,
                serverPort = port,
                uuid = o["uuid"]?.jsonPrimitive?.contentOrNull ?: "",
                password = o["password"]?.jsonPrimitive?.contentOrNull ?: "",
                tlsReality = tlsConfig,
                quicExtra = QuicProtocolSettings(
                    tuicCongestionControl = o["congestion_control"]?.jsonPrimitive?.contentOrNull ?: "bbr",
                    tuicUdpRelayMode = o["udp_relay_mode"]?.jsonPrimitive?.contentOrNull ?: "native",
                ),
            )
            "anytls" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = tag,
                protocol = ProtocolType.ANYTLS_V2,
                serverAddress = server,
                serverPort = port,
                password = o["password"]?.jsonPrimitive?.contentOrNull ?: "",
                tlsReality = tlsConfig,
                quicExtra = QuicProtocolSettings(
                    anyTlsMinIdleSession = o["min_idle_session"]?.jsonPrimitive?.intOrNull ?: 5,
                ),
            )
            "vless" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = tag,
                protocol = ProtocolType.VLESS,
                serverAddress = server,
                serverPort = port,
                uuid = o["uuid"]?.jsonPrimitive?.contentOrNull ?: "",
                flow = o["flow"]?.jsonPrimitive?.contentOrNull ?: "",
                tlsReality = tlsConfig,
            )
            "shadowsocks" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = tag,
                protocol = ProtocolType.SHADOWSOCKS,
                serverAddress = server,
                serverPort = port,
                method = o["method"]?.jsonPrimitive?.contentOrNull ?: "2022-blake3-aes-128-gcm",
                password = o["password"]?.jsonPrimitive?.contentOrNull ?: "",
            )
            "ssh" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = tag,
                protocol = ProtocolType.SSH,
                serverAddress = server,
                serverPort = port,
                username = o["user"]?.jsonPrimitive?.contentOrNull ?: "root",
                password = o["password"]?.jsonPrimitive?.contentOrNull ?: "",
            )
            else -> null
        }
    }

    private fun parseXrayOutboundJson(o: JsonObject, subscriptionId: String?): TunnelProfile? {
        val protocol = o["protocol"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: return null
        if (protocol in setOf("freedom", "blackhole", "dns")) return null
        val settings = o["settings"] as? JsonObject ?: return null
        val tag = o["tag"]?.jsonPrimitive?.contentOrNull ?: "Xray Node"

        val stream = o["streamSettings"] as? JsonObject
        val secStr = stream?.get("security")?.jsonPrimitive?.contentOrNull?.lowercase() ?: "none"
        val reality = stream?.get("realitySettings") as? JsonObject
        val tls = stream?.get("tlsSettings") as? JsonObject
        val tlsConfig = TlsRealityConfig(
            security = when (secStr) {
                "reality" -> SecurityMode.REALITY
                "tls" -> SecurityMode.TLS
                else -> SecurityMode.NONE
            },
            sni = reality?.get("serverName")?.jsonPrimitive?.contentOrNull
                ?: tls?.get("serverName")?.jsonPrimitive?.contentOrNull ?: "",
            realityPublicKey = reality?.get("publicKey")?.jsonPrimitive?.contentOrNull ?: "",
            realityShortId = reality?.get("shortId")?.jsonPrimitive?.contentOrNull ?: "",
            mldsa65Verify = reality?.get("mldsa65Verify")?.jsonPrimitive?.contentOrNull ?: "",
        )

        val vnext = (settings["vnext"] as? JsonArray)?.firstOrNull() as? JsonObject
        if (vnext != null) {
            val host = vnext["address"]?.jsonPrimitive?.contentOrNull ?: return null
            val port = vnext["port"]?.jsonPrimitive?.intOrNull ?: 443
            val user = (vnext["users"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
            return TunnelProfile(
                subscriptionId = subscriptionId,
                name = tag,
                protocol = if (protocol == "vmess") ProtocolType.VMESS else ProtocolType.VLESS,
                serverAddress = host,
                serverPort = port,
                uuid = user["id"]?.jsonPrimitive?.contentOrNull ?: "",
                flow = user["flow"]?.jsonPrimitive?.contentOrNull ?: "",
                tlsReality = tlsConfig,
            )
        }

        val serverObj = (settings["servers"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
        val host = serverObj["address"]?.jsonPrimitive?.contentOrNull ?: return null
        val port = serverObj["port"]?.jsonPrimitive?.intOrNull ?: 443
        return when (protocol) {
            "trojan" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = tag,
                protocol = ProtocolType.TROJAN,
                serverAddress = host,
                serverPort = port,
                password = serverObj["password"]?.jsonPrimitive?.contentOrNull ?: "",
                tlsReality = tlsConfig,
            )
            "shadowsocks" -> TunnelProfile(
                subscriptionId = subscriptionId,
                name = tag,
                protocol = ProtocolType.SHADOWSOCKS,
                serverAddress = host,
                serverPort = port,
                method = serverObj["method"]?.jsonPrimitive?.contentOrNull ?: "2022-blake3-aes-128-gcm",
                password = serverObj["password"]?.jsonPrimitive?.contentOrNull ?: "",
            )
            else -> null
        }
    }

    fun exportToSip008Json(profiles: List<TunnelProfile>): String {
        val ssNodes = profiles.filter { it.protocol == ProtocolType.SHADOWSOCKS }
        val root = buildJsonObject {
            put("version", 1)
            putJsonArray("servers") {
                ssNodes.forEach { p ->
                    add(
                        buildJsonObject {
                            put("id", p.id)
                            put("remarks", p.name)
                            put("server", p.serverAddress)
                            put("server_port", p.serverPort)
                            put("method", p.method)
                            put("password", p.password)
                            if (p.plugin.isNotBlank()) put("plugin", p.plugin)
                            if (p.pluginOpts.isNotBlank()) put("plugin_opts", p.pluginOpts)
                        }
                    )
                }
            }
        }
        return prettyJson.encodeToString(JsonObject.serializer(), root)
    }

    data class ParsedSubscriptionUserInfo(
        val uploadBytes: Long = 0L,
        val downloadBytes: Long = 0L,
        val totalBytes: Long = 0L,
        val expireEpochSec: Long = 0L,
    )

    /** Parses `Subscription-Userinfo: upload=...; download=...; total=...; expire=...` headers. */
    fun parseSubscriptionUserInfoHeader(headerValue: String?): ParsedSubscriptionUserInfo {
        if (headerValue.isNullOrBlank()) return ParsedSubscriptionUserInfo()
        val values = HashMap<String, Long>()
        for (part in headerValue.split(';')) {
            val kv = part.trim()
            if (!kv.contains('=')) continue
            val k = kv.substringBefore('=').trim().lowercase()
            val v = kv.substringAfter('=').trim().toLongOrNull() ?: continue
            values[k] = v
        }
        return ParsedSubscriptionUserInfo(
            uploadBytes = values["upload"] ?: 0L,
            downloadBytes = values["download"] ?: 0L,
            totalBytes = values["total"] ?: 0L,
            expireEpochSec = values["expire"] ?: 0L,
        )
    }
}
