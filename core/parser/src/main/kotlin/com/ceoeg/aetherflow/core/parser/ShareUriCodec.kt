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
import com.ceoeg.aetherflow.core.common.model.XhttpXmuxSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Bidirectional Share-URI parser and serializer covering Xray, Sing-box, QUIC,
 * SSH, WireGuard, and AmneziaWG URI formats.
 */
object ShareUriCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseUri(rawUri: String, subscriptionId: String? = null): TunnelProfile? {
        val line = rawUri.trim()
        if (line.isEmpty() || !line.contains("://")) return null
        val scheme = line.substringBefore("://").lowercase()
        return when (scheme) {
            "vless" -> parseVless(line, subscriptionId)
            "vmess" -> parseVmess(line, subscriptionId)
            "trojan" -> parseTrojan(line, subscriptionId)
            "ss" -> parseShadowsocks(line, subscriptionId)
            "hysteria2", "hy2" -> parseHysteria2(line, subscriptionId)
            "hysteria" -> parseHysteria1(line, subscriptionId)
            "tuic" -> parseTuicV5(line, subscriptionId)
            "anytls" -> parseAnyTls(line, subscriptionId)
            "shadowtls" -> parseShadowTls(line, subscriptionId)
            "naive+https", "naive+quic" -> parseNaive(line, subscriptionId)
            "juicity" -> parseJuicity(line, subscriptionId)
            "mieru" -> parseMieru(line, subscriptionId)
            "ssh" -> parseSsh(line, subscriptionId)
            "socks", "socks5", "socks4", "socks4a" -> parseSocks(line, subscriptionId)
            "http", "https" -> parseHttpProxy(line, subscriptionId)
            "wg", "wireguard" -> parseWireGuardUri(line, isAmnezia = false, subscriptionId = subscriptionId)
            "awg", "amneziawg" -> parseWireGuardUri(line, isAmnezia = true, subscriptionId = subscriptionId)
            else -> null
        }
    }

    fun serializeToUri(profile: TunnelProfile): String {
        return when (profile.protocol) {
            ProtocolType.VLESS -> serializeVless(profile)
            ProtocolType.VMESS -> serializeVmess(profile)
            ProtocolType.TROJAN -> serializeTrojan(profile)
            ProtocolType.SHADOWSOCKS -> serializeShadowsocks(profile)
            ProtocolType.HYSTERIA2 -> serializeHysteria2(profile)
            ProtocolType.HYSTERIA -> serializeHysteria1(profile)
            ProtocolType.TUIC_V5 -> serializeTuicV5(profile)
            ProtocolType.ANYTLS_V2 -> serializeAnyTls(profile)
            ProtocolType.SHADOWTLS_V3 -> serializeShadowTls(profile)
            ProtocolType.NAIVE_PROXY -> serializeNaive(profile)
            ProtocolType.JUICITY -> serializeJuicity(profile)
            ProtocolType.MIERU -> serializeMieru(profile)
            ProtocolType.SSH -> serializeSsh(profile)
            ProtocolType.SOCKS -> serializeSocks(profile)
            ProtocolType.HTTP -> serializeHttpProxy(profile)
            ProtocolType.WIREGUARD -> serializeWireGuardUri(profile, isAmnezia = false)
            ProtocolType.AMNEZIA_WG -> serializeWireGuardUri(profile, isAmnezia = true)
            ProtocolType.OPENVPN -> profile.openVpn.rawOvpnContent
            ProtocolType.CUSTOM_JSON -> profile.rawCustomConfig
        }
    }

    private data class ParsedStandardUri(
        val scheme: String,
        val userInfo: String,
        val host: String,
        val port: Int,
        val query: Map<String, String>,
        val fragment: String,
    )

    private fun splitStandardUri(raw: String, defaultPort: Int = 443): ParsedStandardUri? {
        val scheme = raw.substringBefore("://").lowercase()
        val afterScheme = raw.substringAfter("://")
        val fragment = if (afterScheme.contains('#')) {
            Base64Codec.urlDecode(afterScheme.substringAfterLast('#'))
        } else {
            ""
        }
        val beforeFragment = afterScheme.substringBeforeLast('#')
        val queryPart = if (beforeFragment.contains('?')) beforeFragment.substringAfter('?') else ""
        val authorityAndPath = beforeFragment.substringBefore('?').trimEnd('/')

        val userInfo: String
        val hostPort: String
        if (authorityAndPath.contains('@')) {
            userInfo = authorityAndPath.substringBeforeLast('@')
            hostPort = authorityAndPath.substringAfterLast('@')
        } else {
            userInfo = ""
            hostPort = authorityAndPath
        }

        val host: String
        val port: Int
        if (hostPort.startsWith("[")) {
            host = hostPort.substringAfter('[').substringBefore(']')
            val portStr = hostPort.substringAfter("]:", "")
            port = portStr.substringBefore('/').toIntOrNull() ?: defaultPort
        } else {
            val cleanHostPort = hostPort.substringBefore('/')
            if (cleanHostPort.contains(':')) {
                host = cleanHostPort.substringBeforeLast(':')
                port = cleanHostPort.substringAfterLast(':').toIntOrNull() ?: defaultPort
            } else {
                host = cleanHostPort
                port = defaultPort
            }
        }
        if (host.isBlank()) return null

        val queryMap = LinkedHashMap<String, String>()
        if (queryPart.isNotEmpty()) {
            for (pair in queryPart.split('&')) {
                if (pair.isBlank()) continue
                val key = Base64Codec.urlDecode(pair.substringBefore('='))
                val value = if (pair.contains('=')) Base64Codec.urlDecode(pair.substringAfter('=')) else ""
                queryMap[key] = value
            }
        }

        return ParsedStandardUri(
            scheme = scheme,
            userInfo = userInfo,
            host = host,
            port = port,
            query = queryMap,
            fragment = fragment,
        )
    }

    private fun parseTransportFromQuery(q: Map<String, String>): TransportSettings {
        val net = (q["type"] ?: q["network"] ?: "tcp").lowercase()
        val transportType = when (net) {
            "ws", "websocket" -> TransportType.WS
            "httpupgrade" -> TransportType.HTTP_UPGRADE
            "xhttp", "splithttp" -> TransportType.XHTTP
            "grpc" -> TransportType.GRPC
            "h2", "http" -> TransportType.H2
            "kcp", "mkcp" -> TransportType.MKCP
            "quic" -> TransportType.QUIC
            else -> TransportType.RAW_TCP
        }

        var xhttp = XhttpXmuxSettings(
            mode = q["mode"] ?: "auto",
        )
        val extraJson = q["extra"]
        if (!extraJson.isNullOrBlank()) {
            runCatching {
                val obj = json.parseToJsonElement(extraJson).jsonObject
                val padding = obj["xPaddingBytes"]?.jsonPrimitive?.contentOrNull
                val xmuxObj = obj["xmux"] as? JsonObject
                xhttp = xhttp.copy(
                    xPaddingBytes = padding ?: xhttp.xPaddingBytes,
                    maxConcurrency = xmuxObj?.get("maxConcurrency")?.jsonPrimitive?.contentOrNull
                        ?: xhttp.maxConcurrency,
                    hMaxRequestTimes = xmuxObj?.get("hMaxRequestTimes")?.jsonPrimitive?.contentOrNull
                        ?: xhttp.hMaxRequestTimes,
                    hMaxReusableSecs = xmuxObj?.get("hMaxReusableSecs")?.jsonPrimitive?.contentOrNull
                        ?: xhttp.hMaxReusableSecs,
                )
            }
        }

        return TransportSettings(
            type = transportType,
            host = q["host"] ?: q["authority"] ?: "",
            path = q["path"] ?: "",
            serviceName = q["serviceName"] ?: q["path"]?.trimStart('/') ?: "",
            grpcMode = q["mode"]?.takeIf { transportType == TransportType.GRPC } ?: "gun",
            kcpSeed = q["seed"] ?: "",
            kcpHeaderType = q["headerType"] ?: "none",
            xhttp = xhttp,
        )
    }

    private fun parseTlsRealityFromQuery(
        q: Map<String, String>,
        defaultSecurity: SecurityMode = SecurityMode.NONE,
    ): TlsRealityConfig {
        val sec = when (q["security"]?.lowercase()) {
            "reality" -> SecurityMode.REALITY
            "tls", "xtls" -> SecurityMode.TLS
            "none", "" -> if (!q["pbk"].isNullOrBlank()) SecurityMode.REALITY else defaultSecurity
            null -> if (!q["pbk"].isNullOrBlank()) SecurityMode.REALITY else defaultSecurity
            else -> defaultSecurity
        }
        val fpStr = (q["fp"] ?: q["fingerprint"] ?: "chrome").lowercase()
        val fp = UtlsFingerprint.entries.firstOrNull { it.wireValue == fpStr } ?: UtlsFingerprint.CHROME
        val alpn = (q["alpn"] ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val insecure = q["allowInsecure"] == "1" || q["allowInsecure"] == "true" ||
            q["insecure"] == "1" || q["insecure"] == "true"

        return TlsRealityConfig(
            security = sec,
            sni = q["sni"] ?: q["peer"] ?: q["serverName"] ?: "",
            alpn = alpn,
            allowInsecure = insecure,
            fingerprint = fp,
            realityPublicKey = q["pbk"] ?: q["publicKey"] ?: "",
            realityShortId = q["sid"] ?: q["shortId"] ?: "",
            realitySpiderX = q["spx"] ?: q["spiderX"] ?: "/",
            mldsa65Verify = q["pqv"] ?: q["mldsa65Verify"] ?: "",
            echConfigList = q["ech"] ?: "",
            pinnedPeerCertSha256 = q["pcs"] ?: q["pinned_certchain_sha256"] ?: "",
        )
    }

    private fun parseVless(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        val uuid = Base64Codec.urlDecode(u.userInfo)
        if (uuid.isBlank()) return null
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.VLESS,
            serverAddress = u.host,
            serverPort = u.port,
            uuid = uuid,
            flow = u.query["flow"] ?: "",
            method = u.query["encryption"] ?: "none",
            transport = parseTransportFromQuery(u.query),
            tlsReality = parseTlsRealityFromQuery(u.query),
        )
    }

    private fun serializeVless(p: TunnelProfile): String {
        val params = LinkedHashMap<String, String>()
        params["encryption"] = p.method.ifBlank { "none" }.let { if (it == "auto") "none" else it }
        if (p.flow.isNotBlank()) params["flow"] = p.flow
        params["security"] = p.tlsReality.security.wireValue
        if (p.tlsReality.sni.isNotBlank()) params["sni"] = p.tlsReality.sni
        if (p.tlsReality.alpn.isNotEmpty()) params["alpn"] = p.tlsReality.alpn.joinToString(",")
        params["fp"] = p.tlsReality.fingerprint.wireValue
        if (p.tlsReality.allowInsecure) params["allowInsecure"] = "1"
        if (p.tlsReality.security == SecurityMode.REALITY) {
            if (p.tlsReality.realityPublicKey.isNotBlank()) params["pbk"] = p.tlsReality.realityPublicKey
            if (p.tlsReality.realityShortId.isNotBlank()) params["sid"] = p.tlsReality.realityShortId
            if (p.tlsReality.realitySpiderX.isNotBlank()) params["spx"] = p.tlsReality.realitySpiderX
            if (p.tlsReality.mldsa65Verify.isNotBlank()) params["pqv"] = p.tlsReality.mldsa65Verify
        }
        if (p.tlsReality.echConfigList.isNotBlank()) params["ech"] = p.tlsReality.echConfigList
        params["type"] = p.transport.type.wireValue
        if (p.transport.host.isNotBlank()) params["host"] = p.transport.host
        if (p.transport.path.isNotBlank()) params["path"] = p.transport.path
        if (p.transport.serviceName.isNotBlank() && p.transport.type == TransportType.GRPC) {
            params["serviceName"] = p.transport.serviceName
            params["mode"] = p.transport.grpcMode
        }
        if (p.transport.type == TransportType.XHTTP) {
            params["mode"] = p.transport.xhttp.mode
            val extraObj = buildJsonObject {
                put("xPaddingBytes", p.transport.xhttp.xPaddingBytes)
                putJsonObject("xmux") {
                    put("maxConcurrency", p.transport.xhttp.maxConcurrency)
                    put("hMaxRequestTimes", p.transport.xhttp.hMaxRequestTimes)
                    put("hMaxReusableSecs", p.transport.xhttp.hMaxReusableSecs)
                }
            }
            params["extra"] = extraObj.toString()
        }
        return buildStandardUri("vless", Base64Codec.urlEncode(p.uuid), p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseVmess(raw: String, subscriptionId: String?): TunnelProfile? {
        val body = raw.substringAfter("://")
        // Support both v2 Base64 JSON and AEAD vmess://uuid@host:port URI
        if (!body.contains('@')) {
            val decoded = Base64Codec.decodeFlexible(body) ?: return null
            val obj = runCatching { json.parseToJsonElement(decoded).jsonObject }.getOrNull() ?: return null
            val host = obj["add"]?.jsonPrimitive?.contentOrNull ?: return null
            val port = obj["port"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 443
            val uuid = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
            val net = (obj["net"]?.jsonPrimitive?.contentOrNull ?: "tcp").lowercase()
            val transportType = when (net) {
                "ws" -> TransportType.WS
                "httpupgrade" -> TransportType.HTTP_UPGRADE
                "xhttp", "splithttp" -> TransportType.XHTTP
                "grpc" -> TransportType.GRPC
                "h2", "http" -> TransportType.H2
                "kcp" -> TransportType.MKCP
                "quic" -> TransportType.QUIC
                else -> TransportType.RAW_TCP
            }
            val tlsVal = (obj["tls"]?.jsonPrimitive?.contentOrNull ?: "").lowercase()
            val security = if (tlsVal == "tls") SecurityMode.TLS else SecurityMode.NONE
            val fpStr = (obj["fp"]?.jsonPrimitive?.contentOrNull ?: "chrome").lowercase()
            val fp = UtlsFingerprint.entries.firstOrNull { it.wireValue == fpStr } ?: UtlsFingerprint.CHROME
            val alpn = (obj["alpn"]?.jsonPrimitive?.contentOrNull ?: "")
                .split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val path = obj["path"]?.jsonPrimitive?.contentOrNull ?: ""

            return TunnelProfile(
                subscriptionId = subscriptionId,
                name = obj["ps"]?.jsonPrimitive?.contentOrNull?.ifBlank { "$host:$port" } ?: "$host:$port",
                protocol = ProtocolType.VMESS,
                serverAddress = host,
                serverPort = port,
                uuid = uuid,
                alterId = obj["aid"]?.jsonPrimitive?.intOrNull ?: 0,
                method = obj["scy"]?.jsonPrimitive?.contentOrNull?.ifBlank { "auto" } ?: "auto",
                transport = TransportSettings(
                    type = transportType,
                    host = obj["host"]?.jsonPrimitive?.contentOrNull ?: "",
                    path = path,
                    serviceName = if (transportType == TransportType.GRPC) path else "",
                    kcpHeaderType = obj["type"]?.jsonPrimitive?.contentOrNull ?: "none",
                ),
                tlsReality = TlsRealityConfig(
                    security = security,
                    sni = obj["sni"]?.jsonPrimitive?.contentOrNull ?: "",
                    alpn = alpn,
                    fingerprint = fp,
                ),
            )
        } else {
            val u = splitStandardUri(raw, 443) ?: return null
            return TunnelProfile(
                subscriptionId = subscriptionId,
                name = u.fragment.ifBlank { "${u.host}:${u.port}" },
                protocol = ProtocolType.VMESS,
                serverAddress = u.host,
                serverPort = u.port,
                uuid = Base64Codec.urlDecode(u.userInfo),
                method = u.query["encryption"] ?: "auto",
                transport = parseTransportFromQuery(u.query),
                tlsReality = parseTlsRealityFromQuery(u.query),
            )
        }
    }

    private fun serializeVmess(p: TunnelProfile): String {
        val net = when (p.transport.type) {
            TransportType.RAW_TCP -> "tcp"
            else -> p.transport.type.wireValue
        }
        val payload = buildJsonObject {
            put("v", "2")
            put("ps", p.name)
            put("add", p.serverAddress)
            put("port", p.serverPort.toString())
            put("id", p.uuid)
            put("aid", p.alterId.toString())
            put("scy", p.method.ifBlank { "auto" })
            put("net", net)
            put("type", p.transport.kcpHeaderType.ifBlank { "none" })
            put("host", p.transport.host)
            put(
                "path",
                if (p.transport.type == TransportType.GRPC) p.transport.serviceName else p.transport.path,
            )
            put("tls", if (p.tlsReality.security == SecurityMode.TLS) "tls" else "")
            put("sni", p.tlsReality.sni)
            put("alpn", p.tlsReality.alpn.joinToString(","))
            put("fp", p.tlsReality.fingerprint.wireValue)
        }
        return "vmess://${Base64Codec.encodeStandard(payload.toString())}"
    }

    private fun parseTrojan(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.TROJAN,
            serverAddress = u.host,
            serverPort = u.port,
            password = Base64Codec.urlDecode(u.userInfo),
            transport = parseTransportFromQuery(u.query),
            tlsReality = parseTlsRealityFromQuery(u.query, defaultSecurity = SecurityMode.TLS),
        )
    }

    private fun serializeTrojan(p: TunnelProfile): String {
        val params = LinkedHashMap<String, String>()
        params["security"] = p.tlsReality.security.wireValue
        if (p.tlsReality.sni.isNotBlank()) params["sni"] = p.tlsReality.sni
        if (p.tlsReality.alpn.isNotEmpty()) params["alpn"] = p.tlsReality.alpn.joinToString(",")
        params["fp"] = p.tlsReality.fingerprint.wireValue
        if (p.tlsReality.allowInsecure) params["allowInsecure"] = "1"
        if (p.tlsReality.security == SecurityMode.REALITY) {
            if (p.tlsReality.realityPublicKey.isNotBlank()) params["pbk"] = p.tlsReality.realityPublicKey
            if (p.tlsReality.realityShortId.isNotBlank()) params["sid"] = p.tlsReality.realityShortId
            if (p.tlsReality.mldsa65Verify.isNotBlank()) params["pqv"] = p.tlsReality.mldsa65Verify
        }
        params["type"] = p.transport.type.wireValue
        if (p.transport.host.isNotBlank()) params["host"] = p.transport.host
        if (p.transport.path.isNotBlank()) params["path"] = p.transport.path
        if (p.transport.serviceName.isNotBlank()) params["serviceName"] = p.transport.serviceName
        return buildStandardUri("trojan", Base64Codec.urlEncode(p.password), p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseShadowsocks(raw: String, subscriptionId: String?): TunnelProfile? {
        val afterScheme = raw.substringAfter("ss://")
        val fragment = if (afterScheme.contains('#')) {
            Base64Codec.urlDecode(afterScheme.substringAfterLast('#'))
        } else {
            ""
        }
        val mainPart = afterScheme.substringBeforeLast('#')

        // Legacy whole-URI base64: ss://BASE64(method:password@host:port)#tag
        if (!mainPart.contains('@')) {
            val decoded = Base64Codec.decodeFlexible(mainPart) ?: return null
            return parseShadowsocks("ss://$decoded#${Base64Codec.urlEncode(fragment)}", subscriptionId)
        }

        val u = splitStandardUri(raw, 8388) ?: return null
        val decodedUserInfo = if (u.userInfo.contains(':')) {
            Base64Codec.urlDecode(u.userInfo)
        } else {
            Base64Codec.decodeFlexible(u.userInfo) ?: Base64Codec.urlDecode(u.userInfo)
        }
        if (!decodedUserInfo.contains(':')) return null
        val method = decodedUserInfo.substringBefore(':')
        val password = decodedUserInfo.substringAfter(':')

        val pluginRaw = u.query["plugin"] ?: ""
        val pluginName = pluginRaw.substringBefore(';')
        val pluginOpts = if (pluginRaw.contains(';')) pluginRaw.substringAfter(';') else ""

        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.SHADOWSOCKS,
            serverAddress = u.host,
            serverPort = u.port,
            method = method,
            password = password,
            plugin = pluginName,
            pluginOpts = pluginOpts,
        )
    }

    private fun serializeShadowsocks(p: TunnelProfile): String {
        val userInfoEncoded = Base64Codec.encodeUrlSafeNoPad("${p.method}:${p.password}")
        val params = LinkedHashMap<String, String>()
        if (p.plugin.isNotBlank()) {
            params["plugin"] = if (p.pluginOpts.isNotBlank()) "${p.plugin};${p.pluginOpts}" else p.plugin
        }
        return buildStandardUri("ss", userInfoEncoded, p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseHysteria2(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        val password = Base64Codec.urlDecode(u.userInfo).ifBlank { u.query["auth"] ?: "" }
        val tls = parseTlsRealityFromQuery(u.query, defaultSecurity = SecurityMode.TLS).let {
            if (it.alpn.isEmpty()) it.copy(alpn = listOf("h3")) else it
        }
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.HYSTERIA2,
            serverAddress = u.host,
            serverPort = u.port,
            password = password,
            tlsReality = tls,
            quicExtra = QuicProtocolSettings(
                upMbps = u.query["upmbps"]?.toIntOrNull() ?: 100,
                downMbps = u.query["downmbps"]?.toIntOrNull() ?: 250,
                salamanderPassword = if (u.query["obfs"] == "salamander") u.query["obfs-password"] ?: "" else "",
                portHoppingRange = u.query["mport"] ?: "",
                hopIntervalSec = u.query["hop_interval"]?.removeSuffix("s")?.toIntOrNull() ?: 30,
            ),
        )
    }

    private fun serializeHysteria2(p: TunnelProfile): String {
        val params = LinkedHashMap<String, String>()
        if (p.tlsReality.sni.isNotBlank()) params["sni"] = p.tlsReality.sni
        if (p.tlsReality.allowInsecure) params["insecure"] = "1"
        if (p.tlsReality.alpn.isNotEmpty()) params["alpn"] = p.tlsReality.alpn.joinToString(",")
        if (p.quicExtra.salamanderPassword.isNotBlank()) {
            params["obfs"] = "salamander"
            params["obfs-password"] = p.quicExtra.salamanderPassword
        }
        if (p.quicExtra.portHoppingRange.isNotBlank()) {
            params["mport"] = p.quicExtra.portHoppingRange
            params["hop_interval"] = "${p.quicExtra.hopIntervalSec}s"
        }
        params["upmbps"] = p.quicExtra.upMbps.toString()
        params["downmbps"] = p.quicExtra.downMbps.toString()
        return buildStandardUri("hysteria2", Base64Codec.urlEncode(p.password), p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseHysteria1(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.HYSTERIA,
            serverAddress = u.host,
            serverPort = u.port,
            password = u.query["auth"] ?: Base64Codec.urlDecode(u.userInfo),
            tlsReality = parseTlsRealityFromQuery(u.query, defaultSecurity = SecurityMode.TLS),
            quicExtra = QuicProtocolSettings(
                upMbps = u.query["upmbps"]?.toIntOrNull() ?: 50,
                downMbps = u.query["downmbps"]?.toIntOrNull() ?: 200,
                salamanderPassword = u.query["obfsParam"] ?: u.query["obfs"] ?: "",
                portHoppingRange = u.query["mport"] ?: "",
            ),
        )
    }

    private fun serializeHysteria1(p: TunnelProfile): String {
        val params = LinkedHashMap<String, String>()
        params["auth"] = p.password
        params["upmbps"] = p.quicExtra.upMbps.toString()
        params["downmbps"] = p.quicExtra.downMbps.toString()
        if (p.tlsReality.sni.isNotBlank()) params["peer"] = p.tlsReality.sni
        if (p.quicExtra.salamanderPassword.isNotBlank()) params["obfsParam"] = p.quicExtra.salamanderPassword
        return buildStandardUri("hysteria", "", p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseTuicV5(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        val decodedUser = Base64Codec.urlDecode(u.userInfo)
        val uuid = decodedUser.substringBefore(':')
        val password = decodedUser.substringAfter(':', "")
        val tls = parseTlsRealityFromQuery(u.query, defaultSecurity = SecurityMode.TLS).let {
            if (it.alpn.isEmpty()) it.copy(alpn = listOf("h3")) else it
        }
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.TUIC_V5,
            serverAddress = u.host,
            serverPort = u.port,
            uuid = uuid,
            password = password,
            tlsReality = tls,
            quicExtra = QuicProtocolSettings(
                tuicCongestionControl = u.query["congestion_control"] ?: "bbr",
                tuicUdpRelayMode = u.query["udp_relay_mode"] ?: "native",
                tuicZeroRtt = u.query["zero_rtt"] != "0" && u.query["zero_rtt"] != "false",
            ),
        )
    }

    private fun serializeTuicV5(p: TunnelProfile): String {
        val params = LinkedHashMap<String, String>()
        params["congestion_control"] = p.quicExtra.tuicCongestionControl
        params["udp_relay_mode"] = p.quicExtra.tuicUdpRelayMode
        params["zero_rtt"] = if (p.quicExtra.tuicZeroRtt) "1" else "0"
        if (p.tlsReality.sni.isNotBlank()) params["sni"] = p.tlsReality.sni
        if (p.tlsReality.alpn.isNotEmpty()) params["alpn"] = p.tlsReality.alpn.joinToString(",")
        if (p.tlsReality.allowInsecure) params["allow_insecure"] = "1"
        val userInfo = "${Base64Codec.urlEncode(p.uuid)}:${Base64Codec.urlEncode(p.password)}"
        return buildStandardUri("tuic", userInfo, p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseAnyTls(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.ANYTLS_V2,
            serverAddress = u.host,
            serverPort = u.port,
            password = Base64Codec.urlDecode(u.userInfo),
            tlsReality = parseTlsRealityFromQuery(u.query, defaultSecurity = SecurityMode.TLS),
            quicExtra = QuicProtocolSettings(
                anyTlsMinIdleSession = u.query["min_idle_session"]?.toIntOrNull() ?: 5,
                anyTlsIdleCheckIntervalSec = u.query["idle_session_check_interval"]
                    ?.removeSuffix("s")?.toIntOrNull() ?: 30,
                anyTlsIdleTimeoutSec = u.query["idle_session_timeout"]
                    ?.removeSuffix("s")?.toIntOrNull() ?: 30,
            ),
        )
    }

    private fun serializeAnyTls(p: TunnelProfile): String {
        val params = LinkedHashMap<String, String>()
        if (p.tlsReality.sni.isNotBlank()) params["sni"] = p.tlsReality.sni
        params["fp"] = p.tlsReality.fingerprint.wireValue
        if (p.tlsReality.allowInsecure) params["insecure"] = "1"
        params["min_idle_session"] = p.quicExtra.anyTlsMinIdleSession.toString()
        params["idle_session_check_interval"] = "${p.quicExtra.anyTlsIdleCheckIntervalSec}s"
        params["idle_session_timeout"] = "${p.quicExtra.anyTlsIdleTimeoutSec}s"
        return buildStandardUri("anytls", Base64Codec.urlEncode(p.password), p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseShadowTls(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        val stlsPassword = Base64Codec.urlDecode(u.userInfo)
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.SHADOWTLS_V3,
            serverAddress = u.host,
            serverPort = u.port,
            password = stlsPassword,
            tlsReality = parseTlsRealityFromQuery(u.query, defaultSecurity = SecurityMode.TLS),
            quicExtra = QuicProtocolSettings(
                shadowTlsVersion = u.query["version"]?.toIntOrNull() ?: 3,
                shadowTlsPassword = stlsPassword,
                innerMethod = u.query["inner_method"] ?: "2022-blake3-aes-128-gcm",
                innerPassword = u.query["inner_password"] ?: "",
            ),
        )
    }

    private fun serializeShadowTls(p: TunnelProfile): String {
        val params = LinkedHashMap<String, String>()
        params["version"] = p.quicExtra.shadowTlsVersion.toString()
        if (p.tlsReality.sni.isNotBlank()) params["sni"] = p.tlsReality.sni
        params["fp"] = p.tlsReality.fingerprint.wireValue
        if (p.quicExtra.innerMethod.isNotBlank()) params["inner_method"] = p.quicExtra.innerMethod
        if (p.quicExtra.innerPassword.isNotBlank()) params["inner_password"] = p.quicExtra.innerPassword
        val pass = p.password.ifBlank { p.quicExtra.shadowTlsPassword }
        return buildStandardUri("shadowtls", Base64Codec.urlEncode(pass), p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseNaive(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        val decoded = Base64Codec.urlDecode(u.userInfo)
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.NAIVE_PROXY,
            serverAddress = u.host,
            serverPort = u.port,
            username = decoded.substringBefore(':'),
            password = decoded.substringAfter(':', ""),
            tlsReality = parseTlsRealityFromQuery(u.query, defaultSecurity = SecurityMode.TLS).let {
                if (it.sni.isBlank()) it.copy(sni = u.host) else it
            },
            quicExtra = QuicProtocolSettings(
                naiveQuic = u.scheme == "naive+quic",
                naiveConcurrency = u.query["insecure-concurrency"]?.toIntOrNull() ?: 1,
            ),
        )
    }

    private fun serializeNaive(p: TunnelProfile): String {
        val scheme = if (p.quicExtra.naiveQuic) "naive+quic" else "naive+https"
        val userInfo = "${Base64Codec.urlEncode(p.username)}:${Base64Codec.urlEncode(p.password)}"
        val params = LinkedHashMap<String, String>()
        if (p.tlsReality.sni.isNotBlank()) params["sni"] = p.tlsReality.sni
        params["insecure-concurrency"] = p.quicExtra.naiveConcurrency.toString()
        return buildStandardUri(scheme, userInfo, p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseJuicity(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        val decoded = Base64Codec.urlDecode(u.userInfo)
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.JUICITY,
            serverAddress = u.host,
            serverPort = u.port,
            uuid = decoded.substringBefore(':'),
            password = decoded.substringAfter(':', ""),
            tlsReality = parseTlsRealityFromQuery(u.query, defaultSecurity = SecurityMode.TLS),
            quicExtra = QuicProtocolSettings(
                juicityCongestionControl = u.query["congestion_control"] ?: "bbr",
            ),
        )
    }

    private fun serializeJuicity(p: TunnelProfile): String {
        val userInfo = "${Base64Codec.urlEncode(p.uuid)}:${Base64Codec.urlEncode(p.password)}"
        val params = LinkedHashMap<String, String>()
        params["congestion_control"] = p.quicExtra.juicityCongestionControl
        if (p.tlsReality.sni.isNotBlank()) params["sni"] = p.tlsReality.sni
        return buildStandardUri("juicity", userInfo, p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseMieru(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 443) ?: return null
        val decoded = Base64Codec.urlDecode(u.userInfo)
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.MIERU,
            serverAddress = u.host,
            serverPort = u.port,
            username = decoded.substringBefore(':'),
            password = decoded.substringAfter(':', ""),
            quicExtra = QuicProtocolSettings(
                mieruTransport = u.query["transport"] ?: "TCP",
                mieruMultiplexing = u.query["multiplexing"] ?: "MULTIPLEXING_HIGH",
            ),
        )
    }

    private fun serializeMieru(p: TunnelProfile): String {
        val userInfo = "${Base64Codec.urlEncode(p.username)}:${Base64Codec.urlEncode(p.password)}"
        val params = linkedMapOf(
            "transport" to p.quicExtra.mieruTransport,
            "multiplexing" to p.quicExtra.mieruMultiplexing,
        )
        return buildStandardUri("mieru", userInfo, p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseSsh(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 22) ?: return null
        val decoded = Base64Codec.urlDecode(u.userInfo)
        val user = decoded.substringBefore(':').ifBlank { "root" }
        val pass = if (decoded.contains(':')) decoded.substringAfter(':') else ""
        val pemDecoded = u.query["private_key"]?.let { Base64Codec.decodeFlexible(it) ?: it } ?: ""
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.SSH,
            serverAddress = u.host,
            serverPort = u.port,
            username = user,
            password = pass,
            quicExtra = QuicProtocolSettings(
                sshUser = user,
                sshPrivateKeyPem = pemDecoded,
                sshPrivateKeyPassphrase = u.query["passphrase"] ?: "",
                sshHostKey = u.query["host_key"] ?: "",
                sshClientVersion = u.query["client_version"] ?: "SSH-2.0-OpenSSH_9.6",
            ),
        )
    }

    private fun serializeSsh(p: TunnelProfile): String {
        val user = p.username.ifBlank { p.quicExtra.sshUser.ifBlank { "root" } }
        val userInfo = if (p.password.isNotBlank()) {
            "${Base64Codec.urlEncode(user)}:${Base64Codec.urlEncode(p.password)}"
        } else {
            Base64Codec.urlEncode(user)
        }
        val params = LinkedHashMap<String, String>()
        if (p.quicExtra.sshPrivateKeyPem.isNotBlank()) {
            params["private_key"] = Base64Codec.encodeUrlSafeNoPad(p.quicExtra.sshPrivateKeyPem)
        }
        if (p.quicExtra.sshHostKey.isNotBlank()) params["host_key"] = p.quicExtra.sshHostKey
        if (p.quicExtra.sshClientVersion.isNotBlank()) params["client_version"] = p.quicExtra.sshClientVersion
        return buildStandardUri("ssh", userInfo, p.serverAddress, p.serverPort, params, p.name)
    }

    private fun parseSocks(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, 1080) ?: return null
        val decoded = if (u.userInfo.contains(':')) {
            Base64Codec.urlDecode(u.userInfo)
        } else {
            Base64Codec.decodeFlexible(u.userInfo) ?: Base64Codec.urlDecode(u.userInfo)
        }
        val version = when (u.scheme) {
            "socks4" -> "4"
            "socks4a" -> "4a"
            else -> "5"
        }
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.SOCKS,
            serverAddress = u.host,
            serverPort = u.port,
            username = decoded.substringBefore(':', ""),
            password = decoded.substringAfter(':', ""),
            quicExtra = QuicProtocolSettings(socksVersion = version),
        )
    }

    private fun serializeSocks(p: TunnelProfile): String {
        val scheme = when (p.quicExtra.socksVersion) {
            "4" -> "socks4"
            "4a" -> "socks4a"
            else -> "socks5"
        }
        val userInfo = if (p.username.isNotBlank() || p.password.isNotBlank()) {
            "${Base64Codec.urlEncode(p.username)}:${Base64Codec.urlEncode(p.password)}"
        } else {
            ""
        }
        return buildStandardUri(scheme, userInfo, p.serverAddress, p.serverPort, emptyMap(), p.name)
    }

    private fun parseHttpProxy(raw: String, subscriptionId: String?): TunnelProfile? {
        val u = splitStandardUri(raw, if (raw.startsWith("https://")) 443 else 8080) ?: return null
        val decoded = Base64Codec.urlDecode(u.userInfo)
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = ProtocolType.HTTP,
            serverAddress = u.host,
            serverPort = u.port,
            username = decoded.substringBefore(':', ""),
            password = decoded.substringAfter(':', ""),
            tlsReality = TlsRealityConfig(
                security = if (u.scheme == "https") SecurityMode.TLS else SecurityMode.NONE,
                sni = u.query["sni"] ?: u.host,
            ),
        )
    }

    private fun serializeHttpProxy(p: TunnelProfile): String {
        val scheme = if (p.tlsReality.security == SecurityMode.TLS) "https" else "http"
        val userInfo = if (p.username.isNotBlank() || p.password.isNotBlank()) {
            "${Base64Codec.urlEncode(p.username)}:${Base64Codec.urlEncode(p.password)}"
        } else {
            ""
        }
        return buildStandardUri(scheme, userInfo, p.serverAddress, p.serverPort, emptyMap(), p.name)
    }

    private fun parseWireGuardUri(
        raw: String,
        isAmnezia: Boolean,
        subscriptionId: String?,
    ): TunnelProfile? {
        val u = splitStandardUri(raw, 51820) ?: return null
        val privateKey = Base64Codec.urlDecode(u.userInfo).ifBlank { u.query["privateKey"] ?: "" }
        val reserved = (u.query["reserved"] ?: "")
            .split(',').mapNotNull { it.trim().toIntOrNull() }
        val wg = WireGuardSettings(
            privateKey = privateKey,
            peerPublicKey = u.query["publicKey"] ?: "",
            presharedKey = u.query["presharedKey"] ?: u.query["psk"] ?: "",
            localAddresses = (u.query["address"] ?: "172.16.0.2/32")
                .split(',').map { it.trim() }.filter { it.isNotEmpty() },
            dnsServers = (u.query["dns"] ?: "1.1.1.1")
                .split(',').map { it.trim() }.filter { it.isNotEmpty() },
            mtu = u.query["mtu"]?.toIntOrNull() ?: 1400,
            persistentKeepalive = u.query["keepalive"]?.toIntOrNull() ?: 25,
            reservedBytes = reserved,
            jc = u.query["jc"]?.toIntOrNull() ?: 0,
            jmin = u.query["jmin"]?.toIntOrNull() ?: 0,
            jmax = u.query["jmax"]?.toIntOrNull() ?: 0,
            s1 = u.query["s1"]?.toIntOrNull() ?: 0,
            s2 = u.query["s2"]?.toIntOrNull() ?: 0,
            h1 = u.query["h1"] ?: "",
            h2 = u.query["h2"] ?: "",
            h3 = u.query["h3"] ?: "",
            h4 = u.query["h4"] ?: "",
        )
        val proto = if (isAmnezia || wg.hasAmneziaObfuscation) {
            ProtocolType.AMNEZIA_WG
        } else {
            ProtocolType.WIREGUARD
        }
        return TunnelProfile(
            subscriptionId = subscriptionId,
            name = u.fragment.ifBlank { "${u.host}:${u.port}" },
            protocol = proto,
            serverAddress = u.host,
            serverPort = u.port,
            wireGuard = wg,
        )
    }

    private fun serializeWireGuardUri(p: TunnelProfile, isAmnezia: Boolean): String {
        val wg = p.wireGuard
        val scheme = if (isAmnezia || wg.hasAmneziaObfuscation) "awg" else "wg"
        val params = LinkedHashMap<String, String>()
        params["publicKey"] = wg.peerPublicKey
        if (wg.presharedKey.isNotBlank()) params["presharedKey"] = wg.presharedKey
        params["address"] = wg.localAddresses.joinToString(",")
        params["dns"] = wg.dnsServers.joinToString(",")
        params["mtu"] = wg.mtu.toString()
        if (wg.reservedBytes.isNotEmpty()) params["reserved"] = wg.reservedBytes.joinToString(",")
        if (wg.jc > 0) {
            params["jc"] = wg.jc.toString()
            params["jmin"] = wg.jmin.toString()
            params["jmax"] = wg.jmax.toString()
        }
        if (wg.s1 > 0) params["s1"] = wg.s1.toString()
        if (wg.s2 > 0) params["s2"] = wg.s2.toString()
        if (wg.h1.isNotBlank()) params["h1"] = wg.h1
        if (wg.h2.isNotBlank()) params["h2"] = wg.h2
        if (wg.h3.isNotBlank()) params["h3"] = wg.h3
        if (wg.h4.isNotBlank()) params["h4"] = wg.h4
        return buildStandardUri(
            scheme,
            Base64Codec.urlEncode(wg.privateKey),
            p.serverAddress,
            p.serverPort,
            params,
            p.name,
        )
    }

    private fun buildStandardUri(
        scheme: String,
        encodedUserInfo: String,
        host: String,
        port: Int,
        query: Map<String, String>,
        name: String,
    ): String {
        val formattedHost = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
        val userPart = if (encodedUserInfo.isNotEmpty()) "$encodedUserInfo@" else ""
        val queryStr = if (query.isNotEmpty()) {
            "?" + query.entries.joinToString("&") { (k, v) ->
                "${Base64Codec.urlEncode(k)}=${Base64Codec.urlEncode(v)}"
            }
        } else {
            ""
        }
        val frag = if (name.isNotBlank()) "#${Base64Codec.urlEncode(name)}" else ""
        return "$scheme://$userPart$formattedHost:$port$queryStr$frag"
    }
}
