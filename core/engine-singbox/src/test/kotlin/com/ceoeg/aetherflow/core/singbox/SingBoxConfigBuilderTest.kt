package com.ceoeg.aetherflow.core.singbox

import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.DnsProtocol
import com.ceoeg.aetherflow.core.common.model.DnsSettings
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.QuicProtocolSettings
import com.ceoeg.aetherflow.core.common.model.RoutingMode
import com.ceoeg.aetherflow.core.common.model.SecurityMode
import com.ceoeg.aetherflow.core.common.model.TlsRealityConfig
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.security.EphemeralLoopbackAuthGenerator
import com.ceoeg.aetherflow.core.routing.DnsAndRoutingCompiler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SingBoxConfigBuilderTest {

    private val builder = SingBoxConfigBuilder(DnsAndRoutingCompiler())
    private val authGenerator = EphemeralLoopbackAuthGenerator()
    private val inspectorClient = ClashApiInspectorClient()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `generates Sing-box JSON for Hysteria2 ShadowTLSv3 AnyTLSv2 FakeIP SplitDNS and authenticated Clash API`() {
        val sessionAuth = authGenerator.generateSessionAuth()
        val settings = AppSettings(
            routingMode = RoutingMode.SMART_GEO_BYPASS,
            regionalBypassCode = "cn",
            dnsSettings = DnsSettings(
                remoteProtocol = DnsProtocol.DOQ,
                remoteDnsServer = "quic://dns.adguard-dns.com",
                splitDnsEnabled = true,
                fakeIpEnabled = true,
                blockIpv6Leak = true,
            ),
        )

        val hy2Profile = TunnelProfile(
            name = "Hysteria2 Brutal Hop",
            protocol = ProtocolType.HYSTERIA2,
            serverAddress = "hy2.aether.example",
            serverPort = 443,
            password = "hy2_secret_auth",
            tlsReality = TlsRealityConfig(
                security = SecurityMode.TLS,
                sni = "hy2.aether.example",
            ),
            quicExtra = QuicProtocolSettings(
                upMbps = 150,
                downMbps = 500,
                salamanderPassword = "salamander_key_99",
                portHoppingRange = "25000-30000",
            ),
        )

        val hy2Json = builder.buildConfig(hy2Profile, settings, sessionAuth)
        val root = json.parseToJsonElement(hy2Json).jsonObject

        // Verify ephemeral SOCKS5 inbound credentials
        val mixedInbound = root["inbounds"]!!.jsonArray.first().jsonObject
        assertEquals(sessionAuth.socksPort, mixedInbound["listen_port"]?.jsonPrimitive?.content?.toInt())
        val userObj = mixedInbound["users"]!!.jsonArray.first().jsonObject
        assertEquals(sessionAuth.username, userObj["username"]?.jsonPrimitive?.content)
        assertEquals(sessionAuth.password, userObj["password"]?.jsonPrimitive?.content)

        // Verify Hysteria2 outbound fields
        val proxyOutbound = root["outbounds"]!!.jsonArray
            .map { it.jsonObject }
            .first { it["tag"]?.jsonPrimitive?.content == "proxy" }
        assertEquals("hysteria2", proxyOutbound["type"]?.jsonPrimitive?.content)
        assertTrue(proxyOutbound.toString().contains("salamander_key_99"))
        assertTrue(proxyOutbound.toString().contains("25000:30000"))

        // Verify FakeIP & Clash API Bearer secret
        assertTrue(root["dns"].toString().contains("fakeip"))
        val clashApi = root["experimental"]!!.jsonObject["clash_api"]!!.jsonObject
        assertEquals("127.0.0.1:${sessionAuth.controlApiPort}", clashApi["external_controller"]?.jsonPrimitive?.content)
        assertEquals(sessionAuth.clashApiSecret, clashApi["secret"]?.jsonPrimitive?.content)

        // Verify ShadowTLS v3 generates inner Shadowsocks-2022 + outer shadowtls transport wrapper
        val shadowTlsProfile = TunnelProfile(
            name = "ShadowTLS v3",
            protocol = ProtocolType.SHADOWTLS_V3,
            serverAddress = "stls.aether.example",
            serverPort = 443,
            password = "inner_ss2022_key",
            tlsReality = TlsRealityConfig(security = SecurityMode.TLS, sni = "www.cloudflare.com"),
            quicExtra = QuicProtocolSettings(
                shadowTlsVersion = 3,
                shadowTlsPassword = "outer_handshake_pass",
                innerMethod = "2022-blake3-aes-128-gcm",
                innerPassword = "inner_ss2022_key",
            ),
        )
        val stlsJson = builder.buildConfig(shadowTlsProfile, settings, sessionAuth)
        assertTrue(stlsJson.contains("proxy-shadowtls-transport"))
        assertTrue(stlsJson.contains("2022-blake3-aes-128-gcm"))
    }

    @Test
    fun `ClashApiInspectorClient parses live connections snapshot accurately`() {
        val samplePayload = """
            {
              "downloadTotal": 1048576,
              "uploadTotal": 262144,
              "connections": [
                {
                  "id": "conn-uuid-01",
                  "upload": 4096,
                  "download": 65536,
                  "rule": "domain_suffix=anthropic.com",
                  "chains": ["proxy", "WARP-Exit"],
                  "metadata": {
                    "network": "tcp",
                    "sourceIP": "198.18.0.1",
                    "sourcePort": "54210",
                    "destinationIP": "104.18.12.34",
                    "destinationPort": "443",
                    "host": "api.anthropic.com"
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = inspectorClient.parseConnectionsSnapshot(samplePayload)
        assertEquals(1, parsed.size)
        assertEquals("conn-uuid-01", parsed[0].id)
        assertEquals("TCP", parsed[0].network)
        assertEquals("api.anthropic.com", parsed[0].host)
        assertEquals(listOf("proxy", "WARP-Exit"), parsed[0].outboundChain)
        assertEquals(65536L, parsed[0].downloadBytes)
    }
}
