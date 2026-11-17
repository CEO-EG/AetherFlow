package com.ceoeg.aetherflow.core.routing

import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.DnsProtocol
import com.ceoeg.aetherflow.core.common.model.RoutingMode
import com.ceoeg.aetherflow.core.common.model.RoutingRule
import com.ceoeg.aetherflow.core.common.model.RuleAction
import com.ceoeg.aetherflow.core.common.model.RuleMatchType
import javax.inject.Inject
import javax.inject.Singleton

data class CompiledDnsPlan(
    val remoteAddress: String,
    val directAddress: String,
    val splitDnsEnabled: Boolean,
    val fakeIpEnabled: Boolean,
    val fakeIpv4Cidr: String = "198.18.0.0/15",
    val fakeIpv6Cidr: String = "fc00::/18",
    val blockIpv6Leak: Boolean,
    val blockWebRtcStun: Boolean,
    val blockQuicUdp443: Boolean,
    val domesticCountryCode: String,
)

data class CompiledRoutingPlan(
    val dns: CompiledDnsPlan,
    val orderedRules: List<RoutingRule>,
    val bypassPrivateCidrs: List<String>,
    val finalOutboundTag: String,
)

/**
 * Compiles high-level AppSettings (DoH/DoT/DoQ/DNSCrypt, FakeIP, Split DNS, Leak Shields,
 * Geo-Bypass, Adblock) + user custom RoutingRules into a deterministic plan shared across
 * Xray-core and Sing-box config builders.
 */
@Singleton
class DnsAndRoutingCompiler @Inject constructor() {

    fun compile(
        settings: AppSettings,
        customRules: List<RoutingRule>,
    ): CompiledRoutingPlan {
        val dns = settings.dnsSettings
        val normalizedRemote = normalizeDnsEndpoint(dns.remoteProtocol, dns.remoteDnsServer)
        val normalizedDirect = dns.directDnsServer.ifBlank { "https://223.5.5.5/dns-query" }
        val region = settings.regionalBypassCode.trim().lowercase()

        val synthesized = mutableListOf<RoutingRule>()

        // 1. Strict Leak Shield: Block WebRTC STUN ports so local/ISP IPs cannot leak
        if (dns.blockWebRtcStun) {
            synthesized += RoutingRule(
                id = "builtin-webrtc-stun-block",
                name = "Block WebRTC STUN Leak Ports",
                matchType = RuleMatchType.PORT,
                value = "3478,5349,19302,19305",
                action = RuleAction.BLOCK,
                priority = 1,
                isBuiltInPreset = true,
            )
        }

        // 2. Optional QUIC UDP/443 block to force TLS over TCP when TLS Fragmentation is active
        if (dns.blockQuicBypass) {
            synthesized += RoutingRule(
                id = "builtin-quic-443-block",
                name = "Block UDP/443 QUIC Fallback",
                matchType = RuleMatchType.PROTOCOL,
                value = "quic",
                action = RuleAction.BLOCK,
                priority = 2,
                isBuiltInPreset = true,
            )
        }

        // 3. Ad / Tracker / Malware domain blocking
        if (settings.blockAdsAndMalware) {
            synthesized += RoutingRule(
                id = "builtin-ads-malware-block",
                name = "Block Ads, Trackers & Telemetry",
                matchType = RuleMatchType.DOMAIN_SUFFIX,
                value = "doubleclick.net,googlesyndication.com,googleadservices.com,adsystem.com,app-measurement.com",
                action = RuleAction.BLOCK,
                priority = 5,
                isBuiltInPreset = true,
            )
        }

        // 4. User custom rules sorted by priority
        synthesized += customRules.filter { it.enabled }.sortedBy { it.priority }

        // 5. Smart Geo-Bypass / Regional Domestic Direct rules
        if (settings.routingMode == RoutingMode.SMART_GEO_BYPASS && region.isNotEmpty()) {
            synthesized += RoutingRule(
                id = "builtin-geosite-$region",
                name = "Domestic GeoSite Direct ($region)",
                matchType = RuleMatchType.GEOSITE,
                value = region,
                action = RuleAction.DIRECT,
                priority = 900,
                isBuiltInPreset = true,
            )
            synthesized += RoutingRule(
                id = "builtin-geoip-$region",
                name = "Domestic GeoIP Direct ($region)",
                matchType = RuleMatchType.GEOIP,
                value = region,
                action = RuleAction.DIRECT,
                priority = 901,
                isBuiltInPreset = true,
            )
        }

        val bypassPrivateCidrs = listOf(
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "127.0.0.0/8",
            "100.64.0.0/10",
            "169.254.0.0/16",
            "fc00::/7",
            "fe80::/10",
        )

        return CompiledRoutingPlan(
            dns = CompiledDnsPlan(
                remoteAddress = normalizedRemote,
                directAddress = normalizedDirect,
                splitDnsEnabled = dns.splitDnsEnabled,
                fakeIpEnabled = dns.fakeIpEnabled,
                blockIpv6Leak = dns.blockIpv6Leak || !settings.tunSettings.enableIpv6,
                blockWebRtcStun = dns.blockWebRtcStun,
                blockQuicUdp443 = dns.blockQuicBypass,
                domesticCountryCode = region,
            ),
            orderedRules = synthesized,
            bypassPrivateCidrs = bypassPrivateCidrs,
            finalOutboundTag = "proxy",
        )
    }

    private fun normalizeDnsEndpoint(protocol: DnsProtocol, raw: String): String {
        val clean = raw.trim().ifBlank { "1.1.1.1" }
        if (clean.contains("://")) return clean
        return when (protocol) {
            DnsProtocol.DOH -> "https://$clean/dns-query"
            DnsProtocol.DOT -> "tls://$clean"
            DnsProtocol.DOQ -> "quic://$clean"
            DnsProtocol.DNSCRYPT -> if (clean.startsWith("sdns://")) clean else "https://1.1.1.1/dns-query"
            DnsProtocol.UDP_PLAIN -> clean
        }
    }
}
