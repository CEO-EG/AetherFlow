package com.ceoeg.aetherflow.core.xray

import com.ceoeg.aetherflow.core.common.model.AntiDpiSettings
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.DnsProtocol
import com.ceoeg.aetherflow.core.common.model.DnsSettings
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.RoutingMode
import com.ceoeg.aetherflow.core.common.model.RoutingRule
import com.ceoeg.aetherflow.core.common.model.RuleAction
import com.ceoeg.aetherflow.core.common.model.RuleMatchType
import com.ceoeg.aetherflow.core.common.model.SecurityMode
import com.ceoeg.aetherflow.core.common.model.TlsRealityConfig
import com.ceoeg.aetherflow.core.common.model.TransportSettings
import com.ceoeg.aetherflow.core.common.model.TransportType
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.model.XhttpXmuxSettings
import com.ceoeg.aetherflow.core.common.security.EphemeralLoopbackAuthGenerator
import com.ceoeg.aetherflow.core.routing.DnsAndRoutingCompiler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class XrayConfigBuilderTest {

    private val builder = XrayConfigBuilder(DnsAndRoutingCompiler())
    private val authGenerator = EphemeralLoopbackAuthGenerator()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `generates Xray v26 JSON with REALITY mldsa65Verify XHTTP xmux finalmask Fragment Noise FakeIP SplitDNS and ephemeral SOCKS5 auth`() {
        val sessionAuth = authGenerator.generateSessionAuth()

        val entryProfile = TunnelProfile(
            name = "Entry XHTTP REALITY",
            protocol = ProtocolType.VLESS,
            serverAddress = "entry.aether.example",
            serverPort = 443,
            uuid = "11111111-2222-3333-4444-555555555555",
            transport = TransportSettings(
                type = TransportType.XHTTP,
                host = "cdn.aether.example",
                path = "/xhttp-split",
                xhttp = XhttpXmuxSettings(
                    mode = "packet-up",
                    xPaddingBytes = "120-800",
                    maxConcurrency = "16-32",
                ),
            ),
            tlsReality = TlsRealityConfig(
                security = SecurityMode.REALITY,
                sni = "cdn.aether.example",
                realityPublicKey = "realityPubKeyTest123",
                realityShortId = "deadbeef",
                mldsa65Verify = "pqVerifyKeyTest456",
            ),
            antiDpi = AntiDpiSettings(
                tlsFragmentEnabled = true,
                fragmentLength = "80-160",
                fragmentIntervalMs = "12-24",
                udpNoiseEnabled = true,
                udpNoisePacket = "20-40",
                udpNoiseDelayMs = "10-15",
            ),
        )

        val exitDetour = TunnelProfile(
            name = "Exit Trojan Hop",
            protocol = ProtocolType.TROJAN,
            serverAddress = "exit.aether.example",
            serverPort = 8443,
            password = "exitHopPassword",
            tlsReality = TlsRealityConfig(
                security = SecurityMode.TLS,
                sni = "exit.aether.example",
            ),
        )

        val settings = AppSettings(
            routingMode = RoutingMode.SMART_GEO_BYPASS,
            regionalBypassCode = "ir",
            dnsSettings = DnsSettings(
                remoteProtocol = DnsProtocol.DOH,
                remoteDnsServer = "https://1.1.1.1/dns-query",
                directDnsServer = "https://223.5.5.5/dns-query",
                splitDnsEnabled = true,
                fakeIpEnabled = true,
                blockIpv6Leak = true,
                blockWebRtcStun = true,
            ),
        )

        val customRules = listOf(
            RoutingRule(
                name = "Route GitHub via Proxy",
                matchType = RuleMatchType.DOMAIN_SUFFIX,
                value = "github.com",
                action = RuleAction.PROXY,
                priority = 10,
            )
        )

        val rawConfig = builder.buildConfig(
            profile = entryProfile,
            settings = settings,
            sessionAuth = sessionAuth,
            customRules = customRules,
            detourExitProfile = exitDetour,
        )

        val root = json.parseToJsonElement(rawConfig).jsonObject

        // 1. Verify ephemeral authenticated local SOCKS5 inbound (never static unauthenticated 10808)
        val socksInbound = root["inbounds"]!!.jsonArray
            .map { it.jsonObject }
            .first { it["tag"]?.jsonPrimitive?.content == "socks-in" }
        assertEquals("127.0.0.1", socksInbound["listen"]?.jsonPrimitive?.content)
        assertEquals(sessionAuth.socksPort, socksInbound["port"]?.jsonPrimitive?.content?.toInt())
        val inboundSettings = socksInbound["settings"]!!.jsonObject
        assertEquals("password", inboundSettings["auth"]?.jsonPrimitive?.content)
        val account = inboundSettings["accounts"]!!.jsonArray.first().jsonObject
        assertEquals(sessionAuth.username, account["user"]?.jsonPrimitive?.content)
        assertEquals(sessionAuth.password, account["pass"]?.jsonPrimitive?.content)

        // 2. Verify FakeIP pool & Split DNS
        assertNotNull(root["fakedns"])
        val dnsObj = root["dns"]!!.jsonObject
        assertEquals("UseIPv4", dnsObj["queryStrategy"]?.jsonPrimitive?.content)
        assertTrue(rawConfig.contains("geosite:ir"))

        // 3. Verify Entry -> Exit chaining ("proxy" dials through "entry-hop") + REALITY mldsa65Verify, XHTTP xmux, and finalmask
        val outbounds = root["outbounds"]!!.jsonArray.map { it.jsonObject }
        val exitProxyOutbound = outbounds.first { it["tag"]?.jsonPrimitive?.content == "proxy" }
        val entryHopOutbound = outbounds.firstOrNull { it["tag"]?.jsonPrimitive?.content == "entry-hop" }
        assertNotNull(entryHopOutbound)
        entryHopOutbound!!

        assertEquals(
            "entry-hop",
            exitProxyOutbound["streamSettings"]!!.jsonObject["sockopt"]!!.jsonObject["dialerProxy"]?.jsonPrimitive?.content,
        )

        val streamSettings = entryHopOutbound["streamSettings"]!!.jsonObject
        assertEquals("xhttp", streamSettings["network"]?.jsonPrimitive?.content)
        assertTrue(streamSettings["xhttpSettings"].toString().contains("xmux"))
        assertEquals(
            "pqVerifyKeyTest456",
            streamSettings["realitySettings"]!!.jsonObject["mldsa65Verify"]?.jsonPrimitive?.content,
        )
        val finalmaskStr = streamSettings["finalmask"].toString()
        assertTrue(finalmaskStr.contains("fragment"))
        assertTrue(finalmaskStr.contains("80-160"))
        assertTrue(finalmaskStr.contains("noise"))

        // 4. Verify Routing rules (WebRTC STUN block + custom rule + regional bypass)
        val routingStr = root["routing"].toString()
        assertTrue(routingStr.contains("3478"))
        assertTrue(routingStr.contains("github.com"))
    }

    @Test
    fun `migrates legacy allowInsecure with custom SNI to Xray v26 pcs and vcn without emitting allowInsecure`() {
        val sessionAuth = authGenerator.generateSessionAuth()
        val settings = AppSettings()

        // Unpinned custom SNI + allowInsecure=1 fallback (e.g., offline before TLS pin probe)
        val unpinnedCustomSniProfile = TunnelProfile(
            name = "vless-tcp-saleh",
            protocol = ProtocolType.VLESS,
            serverAddress = "vless-eg.duckdns.org",
            serverPort = 443,
            uuid = "eb82c67b-227e-4f53-a941-485f2cd2caa1",
            method = "none",
            transport = TransportSettings(type = TransportType.RAW_TCP),
            tlsReality = TlsRealityConfig(
                security = SecurityMode.TLS,
                sni = "partners.playstation.net",
                alpn = listOf("h2", "http/1.1"),
                allowInsecure = true,
            ),
        )

        val unpinnedJson = builder.buildConfig(unpinnedCustomSniProfile, settings, sessionAuth)
        org.junit.jupiter.api.Assertions.assertFalse(
            unpinnedJson.contains("allowInsecure"),
            "Xray v26.6+ rejects allowInsecure; it must never be emitted in tlsSettings",
        )
        val unpinnedRoot = json.parseToJsonElement(unpinnedJson).jsonObject
        val unpinnedTls = unpinnedRoot["outbounds"]!!.jsonArray
            .first().jsonObject["streamSettings"]!!.jsonObject["tlsSettings"]!!.jsonObject
        assertEquals("partners.playstation.net", unpinnedTls["serverName"]?.jsonPrimitive?.content)
        assertEquals("vless-eg.duckdns.org", unpinnedTls["verifyPeerCertByName"]?.jsonPrimitive?.content)

        // Auto-pinned leaf SHA-256 (`pcs`) mode
        val sampleSha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val pinnedProfile = unpinnedCustomSniProfile.copy(
            tlsReality = unpinnedCustomSniProfile.tlsReality.copy(
                pinnedPeerCertSha256 = sampleSha256,
            )
        )
        val pinnedJson = builder.buildConfig(pinnedProfile, settings, sessionAuth)
        org.junit.jupiter.api.Assertions.assertFalse(pinnedJson.contains("allowInsecure"))
        val pinnedTls = json.parseToJsonElement(pinnedJson).jsonObject["outbounds"]!!.jsonArray
            .first().jsonObject["streamSettings"]!!.jsonObject["tlsSettings"]!!.jsonObject
        assertEquals("partners.playstation.net", pinnedTls["serverName"]?.jsonPrimitive?.content)
        assertEquals(sampleSha256, pinnedTls["pinnedPeerCertSha256"]?.jsonPrimitive?.content)
    }
}
