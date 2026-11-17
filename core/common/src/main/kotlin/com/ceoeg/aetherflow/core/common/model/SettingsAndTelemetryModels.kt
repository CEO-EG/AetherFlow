package com.ceoeg.aetherflow.core.common.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class RoutingMode(val label: String, val shortLabel: String) {
    FULL_TUNNEL("Full Tunnel", "Full Tunnel"),
    SMART_GEO_BYPASS("Smart Geo-Bypass", "Geo-Bypass"),
    LOCAL_PROXY_ONLY("Local Proxy Only", "Proxy Only"),
}

@Serializable
enum class TunBackendMode(val label: String, val description: String) {
    HEV_SOCKS5_TUNNEL(
        "hev-socks5-tunnel",
        "Ultra-low-latency native C lwIP stack (Default)",
    ),
    BADVPN_TUN2SOCKS(
        "badvpn-tun2socks",
        "Classic compatibility C tun2socks fallback",
    ),
    CORE_NATIVE_TUN(
        "Native Core TUN",
        "Direct TUN file-descriptor handoff to Xray / Sing-box",
    ),
    PROXY_ONLY(
        "Proxy Only (No VPN)",
        "Exposes local SOCKS5/HTTP listeners without a TUN interface",
    ),
}

@Serializable
enum class ThemeMode(val label: String) {
    PITCH_BLACK_AMOLED("Pitch-Black AMOLED"),
    SLATE_DARK("Slate Dark"),
    CRISP_LIGHT("Crisp Light"),
    DYNAMIC_MONOCHROME("Material You Dynamic"),
}

@Serializable
enum class SplitTunnelMode(val label: String) {
    DISABLED("All Apps Tunneled"),
    TUNNEL_SELECTED_APPS("Tunnel Selected Apps Only"),
    BYPASS_SELECTED_APPS("Bypass Selected Apps"),
}

@Serializable
enum class DnsProtocol(val label: String, val prefix: String) {
    DOH("DNS-over-HTTPS (DoH)", "https://"),
    DOT("DNS-over-TLS (DoT)", "tls://"),
    DOQ("DNS-over-QUIC (DoQ)", "quic://"),
    DNSCRYPT("DNSCrypt", "sdns://"),
    UDP_PLAIN("Direct UDP", "udp://"),
}

@Serializable
enum class UserAgentPreset(val label: String, val headerValue: String) {
    V2RAY_NG("v2rayNG", "v2rayNG/1.10.19"),
    CLASH_META("ClashMeta (Mihomo)", "clash.meta/v1.19.0"),
    SING_BOX("sing-box", "SFA/1.14.0 (Android 16; sing-box 1.14.0)"),
    SHADOWROCKET("Shadowrocket", "Shadowrocket/2490 CFNetwork/1568.200.51 Darwin/24.1.0"),
    AETHERFLOW("AetherFlow Enterprise", "AetherFlow/1.0.0 (Android 16)"),
}

@Serializable
enum class LogSeverity(val wireLevel: String, val rank: Int) {
    NONE("none", 0),
    ERROR("error", 1),
    WARNING("warning", 2),
    INFO("info", 3),
    DEBUG("debug", 4),
}

@Serializable
enum class RuleMatchType(val label: String) {
    DOMAIN_SUFFIX("Domain Suffix"),
    DOMAIN_KEYWORD("Domain Keyword"),
    DOMAIN_FULL("Exact Domain"),
    GEOSITE("GeoSite Category"),
    GEOIP("GeoIP Country"),
    CIDR("IP CIDR"),
    PORT("Destination Port"),
    PROTOCOL("Sniffed Protocol"),
    PROCESS_PACKAGE("Android Package"),
}

@Serializable
enum class RuleAction(val label: String, val outboundTag: String) {
    PROXY("Tunnel via Proxy", "proxy"),
    DIRECT("Direct Bypass", "direct"),
    BLOCK("Reject / Block", "block"),
}

@Serializable
data class TunSettings(
    val backend: TunBackendMode = TunBackendMode.HEV_SOCKS5_TUNNEL,
    val mtu: Int = 1400,
    val enableIpv6: Boolean = false,
    val strictRoute: Boolean = true,
    val killSwitchBlocking: Boolean = true,
    val hevUdpInTcp: Boolean = false,
    val hevMultiQueue: Boolean = false,
    val hevTaskStackSize: Int = 86016,
    val hevTcpBufferSize: Int = 65536,
    val hevMapDnsEnabled: Boolean = true,
)

@Serializable
data class DnsSettings(
    val remoteProtocol: DnsProtocol = DnsProtocol.DOH,
    val remoteDnsServer: String = "https://1.1.1.1/dns-query",
    val directDnsServer: String = "https://223.5.5.5/dns-query",
    val splitDnsEnabled: Boolean = true,
    val fakeIpEnabled: Boolean = false,
    val blockIpv6Leak: Boolean = true,
    val blockWebRtcStun: Boolean = true,
    val blockQuicBypass: Boolean = false,
)

@Serializable
data class LanTetheringSettings(
    val enabled: Boolean = false,
    val bindAddress: String = "0.0.0.0",
    val socksPort: Int = 10810,
    val httpPort: Int = 10811,
    val requireAuth: Boolean = true,
    val username: String = "aether",
    val password: String = "",
)

@Serializable
data class GlobalAntiDpiDefaults(
    val forceTlsFragment: Boolean = false,
    val defaultFragmentLength: String = "100-200",
    val defaultFragmentDelayMs: String = "10-20",
    val forceUdpNoise: Boolean = false,
    val defaultUdpNoisePacket: String = "16-32",
    val defaultUdpNoiseDelayMs: String = "10-20",
    val enableEchByDefault: Boolean = false,
    val customDefaultAlpn: String = "h2,http/1.1",
)

@Serializable
data class AppSettings(
    val activeProfileId: String? = null,
    val activeSubscriptionGroupId: String? = null,
    val smartAutoSelectEnabled: Boolean = false,
    val autoFailoverTimeoutThresholdMs: Long = 3500L,
    val autoFailoverMaxConsecutiveFailures: Int = 2,
    val routingMode: RoutingMode = RoutingMode.FULL_TUNNEL,
    val themeMode: ThemeMode = ThemeMode.SLATE_DARK,
    val tunSettings: TunSettings = TunSettings(),
    val dnsSettings: DnsSettings = DnsSettings(),
    val splitTunnelMode: SplitTunnelMode = SplitTunnelMode.DISABLED,
    val splitTunnelPackages: Set<String> = emptySet(),
    val blockAdsAndMalware: Boolean = true,
    val regionalBypassCode: String = "",
    val lanTethering: LanTetheringSettings = LanTetheringSettings(),
    val globalAntiDpi: GlobalAntiDpiDefaults = GlobalAntiDpiDefaults(),
    val logSeverity: LogSeverity = LogSeverity.INFO,
    val redactLogsByDefault: Boolean = true,
    val latencyTestUrl: String = "https://cp.cloudflare.com/generate_204",
    val speedTestUrl: String = "https://speed.cloudflare.com/__down?bytes=5000000",
)

@Serializable
data class SubscriptionFeed(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val userAgent: UserAgentPreset = UserAgentPreset.SING_BOX,
    val autoUpdateEnabled: Boolean = true,
    val updateIntervalHours: Int = 12,
    val smartAutoSelect: Boolean = true,
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val totalQuotaBytes: Long = 0L,
    val expireEpochSec: Long = 0L,
    val supportUrl: String = "",
    val profileCount: Int = 0,
    val lastSyncedEpochMs: Long = 0L,
) {
    val usedBytes: Long
        get() = (uploadBytes + downloadBytes).coerceAtLeast(0L)

    val remainingBytes: Long
        get() = if (totalQuotaBytes > 0L) (totalQuotaBytes - usedBytes).coerceAtLeast(0L) else 0L

    val quotaProgressFraction: Float
        get() = if (totalQuotaBytes > 0L) {
            (usedBytes.toDouble() / totalQuotaBytes.toDouble()).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
}

@Serializable
data class RoutingRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val matchType: RuleMatchType = RuleMatchType.DOMAIN_SUFFIX,
    val value: String,
    val action: RuleAction = RuleAction.PROXY,
    val priority: Int = 100,
    val isBuiltInPreset: Boolean = false,
)

@Serializable
data class SessionLoopbackAuth(
    val socksPort: Int,
    val httpPort: Int,
    val controlApiPort: Int,
    val username: String,
    val password: String,
    val clashApiSecret: String,
)

sealed interface VpnConnectionState {
    val statusLabel: String

    data object Disconnected : VpnConnectionState {
        override val statusLabel: String = "Disconnected"
    }

    data class Connecting(
        val profileName: String,
        val engine: EngineBackend,
        val attempt: Int = 1,
    ) : VpnConnectionState {
        override val statusLabel: String = "Connecting"
    }

    data class Connected(
        val profileId: String,
        val profileName: String,
        val protocol: ProtocolType,
        val transportLabel: String,
        val engine: EngineBackend,
        val connectedSinceEpochMs: Long,
        val latestHttpDelayMs: Long = -1L,
        val chainedExitName: String? = null,
    ) : VpnConnectionState {
        override val statusLabel: String = "Connected"
    }

    data class Reconnecting(
        val reason: String,
        val targetProfileName: String,
        val engine: EngineBackend,
    ) : VpnConnectionState {
        override val statusLabel: String = "Reconnecting"
    }

    data object Disconnecting : VpnConnectionState {
        override val statusLabel: String = "Disconnecting"
    }

    data class Error(
        val reason: String,
        val recoverable: Boolean = true,
    ) : VpnConnectionState {
        override val statusLabel: String = "Error"
    }
}

@Serializable
data class TrafficSnapshot(
    val uplinkBps: Long = 0L,
    val downlinkBps: Long = 0L,
    val totalUplinkBytes: Long = 0L,
    val totalDownlinkBytes: Long = 0L,
    val timestampMs: Long = System.currentTimeMillis(),
)

@Serializable
data class ActiveSocketConnection(
    val id: String,
    val network: String,
    val source: String,
    val destination: String,
    val host: String,
    val matchedRule: String,
    val outboundChain: List<String>,
    val uploadBytes: Long,
    val downloadBytes: Long,
    val uploadBps: Long = 0L,
    val downloadBps: Long = 0L,
    val startEpochMs: Long = System.currentTimeMillis(),
)

@Serializable
data class ExitNetworkStatus(
    val ip: String,
    val country: String,
    val countryCode: String,
    val asn: String,
    val isp: String,
    val dnsLeakDetected: Boolean,
    val resolverSummary: String,
    val checkedAtEpochMs: Long = System.currentTimeMillis(),
)

@Serializable
data class DiagnosticLogEntry(
    val id: Long,
    val timestampMs: Long,
    val severity: LogSeverity,
    val source: String,
    val message: String,
)
