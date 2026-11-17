package com.ceoeg.aetherflow.core.common.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class EngineBackend(val displayName: String) {
    AUTO("Smart Auto"),
    XRAY("Xray-core"),
    SING_BOX("Sing-box"),
    AMNEZIA_WG("AmneziaWG"),
    OPENVPN("OpenVPN"),
}

@Serializable
enum class ProtocolType(
    val displayName: String,
    val badgeLabel: String,
    val defaultEngine: EngineBackend,
) {
    VLESS("VLESS", "VLESS", EngineBackend.XRAY),
    VMESS("VMess AEAD", "VMESS", EngineBackend.XRAY),
    TROJAN("Trojan", "TROJAN", EngineBackend.XRAY),
    SHADOWSOCKS("Shadowsocks / SS-2022", "SS", EngineBackend.XRAY),
    HYSTERIA2("Hysteria 2 (QUIC)", "HY2", EngineBackend.SING_BOX),
    HYSTERIA("Hysteria v1", "HY1", EngineBackend.SING_BOX),
    TUIC_V5("TUIC v5 (QUIC)", "TUIC", EngineBackend.SING_BOX),
    ANYTLS_V2("AnyTLS v2", "ANYTLS", EngineBackend.SING_BOX),
    SHADOWTLS_V3("ShadowTLS v3", "STLS3", EngineBackend.SING_BOX),
    NAIVE_PROXY("NaiveProxy", "NAIVE", EngineBackend.SING_BOX),
    JUICITY("Juicity", "JUICITY", EngineBackend.SING_BOX),
    MIERU("Mieru", "MIERU", EngineBackend.SING_BOX),
    SSH("SSH Tunnel", "SSH", EngineBackend.SING_BOX),
    SOCKS("SOCKS4/4a/5", "SOCKS", EngineBackend.XRAY),
    HTTP("HTTP / HTTPS Proxy", "HTTP", EngineBackend.XRAY),
    WIREGUARD("WireGuard", "WG", EngineBackend.AMNEZIA_WG),
    AMNEZIA_WG("AmneziaWG (DPI-Resistant)", "AWG", EngineBackend.AMNEZIA_WG),
    OPENVPN("OpenVPN", "OVPN", EngineBackend.OPENVPN),
    CUSTOM_JSON("Raw Core Config", "RAW", EngineBackend.XRAY),
}

@Serializable
enum class TransportType(val wireValue: String, val label: String) {
    RAW_TCP("raw", "TCP / Raw"),
    WS("ws", "WebSocket"),
    HTTP_UPGRADE("httpupgrade", "HTTPUpgrade"),
    XHTTP("xhttp", "XHTTP (SplitHTTP)"),
    GRPC("grpc", "gRPC"),
    H2("h2", "HTTP/2"),
    MKCP("kcp", "mKCP"),
    QUIC("quic", "QUIC"),
}

@Serializable
enum class SecurityMode(val wireValue: String) {
    NONE("none"),
    TLS("tls"),
    REALITY("reality"),
}

@Serializable
enum class UtlsFingerprint(val wireValue: String, val label: String) {
    CHROME("chrome", "Chrome"),
    FIREFOX("firefox", "Firefox"),
    SAFARI("safari", "Safari"),
    IOS("ios", "iOS"),
    ANDROID("android", "Android"),
    EDGE("edge", "Edge"),
    QQ("qq", "QQ Browser"),
    RANDOM("random", "Random"),
    RANDOMIZED("randomized", "Randomized"),
}

@Serializable
data class XhttpXmuxSettings(
    val mode: String = "auto",
    val xPaddingBytes: String = "100-1000",
    val scMaxEachPostBytes: Int = 1_000_000,
    val scMinPostsIntervalMs: String = "30-50",
    val maxConcurrency: String = "16-32",
    val maxConnections: Int = 0,
    val cMaxReuseTimes: Int = 0,
    val hMaxRequestTimes: String = "600-900",
    val hMaxReusableSecs: String = "1800-3000",
    val downloadAddress: String = "",
    val downloadPort: Int = 0,
)

@Serializable
data class TransportSettings(
    val type: TransportType = TransportType.RAW_TCP,
    val host: String = "",
    val path: String = "",
    val serviceName: String = "",
    val grpcMode: String = "gun",
    val headers: Map<String, String> = emptyMap(),
    val kcpSeed: String = "",
    val kcpHeaderType: String = "none",
    val xhttp: XhttpXmuxSettings = XhttpXmuxSettings(),
)

@Serializable
data class TlsRealityConfig(
    val security: SecurityMode = SecurityMode.NONE,
    val sni: String = "",
    val alpn: List<String> = emptyList(),
    val allowInsecure: Boolean = false,
    val fingerprint: UtlsFingerprint = UtlsFingerprint.CHROME,
    val realityPublicKey: String = "",
    val realityShortId: String = "",
    val realitySpiderX: String = "/",
    /** Post-quantum ML-DSA-65 signature verification key (Xray REALITY mldsa65Verify / pqv). */
    val mldsa65Verify: String = "",
    /** Encrypted Client Hello (ECH) config list (Base64 or DNS record). */
    val echConfigList: String = "",
    /** Peer X.509 certificate SHA-256 fingerprint(s) (`pcs` / `pinnedPeerCertSha256` in Xray v26+). */
    val pinnedPeerCertSha256: String = "",
    /** Comma-separated certificate SAN/CN verification override (`vcn` / `verifyPeerCertByName` in Xray v26+). */
    val verifyPeerCertByName: String = "",
)

@Serializable
data class AntiDpiSettings(
    val tlsFragmentEnabled: Boolean = false,
    val fragmentPackets: String = "tlshello",
    val fragmentLength: String = "100-200",
    val fragmentIntervalMs: String = "10-20",
    val fragmentMaxSplit: String = "0",
    val udpNoiseEnabled: Boolean = false,
    val udpNoiseType: String = "rand",
    val udpNoisePacket: String = "10-20",
    val udpNoiseDelayMs: String = "10-16",
    val udpNoiseReset: String = "0",
)

@Serializable
data class QuicProtocolSettings(
    val upMbps: Int = 100,
    val downMbps: Int = 250,
    val salamanderPassword: String = "",
    val portHoppingRange: String = "",
    val hopIntervalSec: Int = 30,
    val tuicCongestionControl: String = "bbr",
    val tuicUdpRelayMode: String = "native",
    val tuicZeroRtt: Boolean = true,
    val tuicHeartbeatSec: Int = 10,
    val anyTlsMinIdleSession: Int = 5,
    val anyTlsIdleCheckIntervalSec: Int = 30,
    val anyTlsIdleTimeoutSec: Int = 30,
    val anyTlsPaddingScheme: List<String> = emptyList(),
    val shadowTlsVersion: Int = 3,
    val shadowTlsPassword: String = "",
    val innerProtocol: String = "shadowsocks",
    val innerMethod: String = "2022-blake3-aes-128-gcm",
    val innerPassword: String = "",
    val naiveQuic: Boolean = false,
    val naiveConcurrency: Int = 1,
    val juicityCongestionControl: String = "bbr",
    val mieruTransport: String = "TCP",
    val mieruMultiplexing: String = "MULTIPLEXING_HIGH",
    val sshUser: String = "root",
    val sshPrivateKeyPem: String = "",
    val sshPrivateKeyPassphrase: String = "",
    val sshHostKey: String = "",
    val sshClientVersion: String = "SSH-2.0-OpenSSH_9.6",
    val socksVersion: String = "5",
)

@Serializable
data class WireGuardSettings(
    val privateKey: String = "",
    val peerPublicKey: String = "",
    val presharedKey: String = "",
    val localAddresses: List<String> = listOf("172.16.0.2/32"),
    val dnsServers: List<String> = listOf("1.1.1.1", "1.0.0.1"),
    val allowedIps: List<String> = listOf("0.0.0.0/0", "::/0"),
    val mtu: Int = 1400,
    val persistentKeepalive: Int = 25,
    val reservedBytes: List<Int> = emptyList(),
    /** AmneziaWG DPI-resistant junk packet & header obfuscation parameters. */
    val jc: Int = 0,
    val jmin: Int = 0,
    val jmax: Int = 0,
    val s1: Int = 0,
    val s2: Int = 0,
    val h1: String = "",
    val h2: String = "",
    val h3: String = "",
    val h4: String = "",
    val isCloudflareWarp: Boolean = false,
    val warpDeviceId: String = "",
) {
    val hasAmneziaObfuscation: Boolean
        get() = jc > 0 || s1 > 0 || s2 > 0 || h1.isNotBlank() || h2.isNotBlank() || h3.isNotBlank() || h4.isNotBlank()
}

@Serializable
data class OpenVpnSettings(
    val proto: String = "udp",
    val cipher: String = "AES-256-GCM",
    val auth: String = "SHA256",
    val caCert: String = "",
    val clientCert: String = "",
    val clientKey: String = "",
    val tlsAuthOrCryptKey: String = "",
    val tlsMode: String = "tls-crypt",
    val rawOvpnContent: String = "",
)

@Serializable
data class TunnelProfile(
    val id: String = UUID.randomUUID().toString(),
    val subscriptionId: String? = null,
    val name: String = "Untitled Endpoint",
    val protocol: ProtocolType = ProtocolType.VLESS,
    val enginePreference: EngineBackend = EngineBackend.AUTO,
    val serverAddress: String = "",
    val serverPort: Int = 443,
    val uuid: String = "",
    val password: String = "",
    val username: String = "",
    val method: String = "auto",
    val flow: String = "",
    val alterId: Int = 0,
    val plugin: String = "",
    val pluginOpts: String = "",
    val transport: TransportSettings = TransportSettings(),
    val tlsReality: TlsRealityConfig = TlsRealityConfig(),
    val antiDpi: AntiDpiSettings = AntiDpiSettings(),
    val quicExtra: QuicProtocolSettings = QuicProtocolSettings(),
    val wireGuard: WireGuardSettings = WireGuardSettings(),
    val openVpn: OpenVpnSettings = OpenVpnSettings(),
    val rawCustomConfig: String = "",
    val rawConfigFormat: String = "json",
    /** Optional outbound detour profile ID (Entry Node -> Exit Node / WARP chain). */
    val detourProfileId: String? = null,
    val regionCode: String = "",
    val tcpPingMs: Long = -1L,
    val urlTestDelayMs: Long = -1L,
    val downloadSpeedMbps: Double = 0.0,
    val lastTestedEpochMs: Long = 0L,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
) {
    fun resolvedEngine(): EngineBackend {
        if (enginePreference != EngineBackend.AUTO) return enginePreference
        return when (protocol) {
            ProtocolType.WIREGUARD, ProtocolType.AMNEZIA_WG -> EngineBackend.AMNEZIA_WG
            ProtocolType.OPENVPN -> EngineBackend.OPENVPN
            ProtocolType.HYSTERIA2,
            ProtocolType.HYSTERIA,
            ProtocolType.TUIC_V5,
            ProtocolType.ANYTLS_V2,
            ProtocolType.SHADOWTLS_V3,
            ProtocolType.NAIVE_PROXY,
            ProtocolType.JUICITY,
            ProtocolType.MIERU,
            ProtocolType.SSH,
            -> EngineBackend.SING_BOX
            ProtocolType.CUSTOM_JSON -> {
                if (rawCustomConfig.contains("\"outbounds\"") && rawCustomConfig.contains("server_port")) {
                    EngineBackend.SING_BOX
                } else {
                    EngineBackend.XRAY
                }
            }
            else -> EngineBackend.XRAY
        }
    }

    /** Canonical fingerprint key used to deduplicate identical endpoints across feeds. */
    fun deduplicationKey(): String = buildString {
        append(protocol.name)
        append('|')
        append(serverAddress.lowercase().trim())
        append(':')
        append(serverPort)
        append('|')
        append(uuid)
        append('|')
        append(password)
        append('|')
        append(transport.type.name)
        append('|')
        append(transport.path)
        append('|')
        append(tlsReality.sni)
        append('|')
        append(tlsReality.realityPublicKey)
        append('|')
        append(wireGuard.peerPublicKey)
    }
}
