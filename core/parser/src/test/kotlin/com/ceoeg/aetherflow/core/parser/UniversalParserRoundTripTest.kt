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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UniversalParserRoundTripTest {

    private val parser = UniversalConfigParser()

    @Test
    fun `round-trip VLESS REALITY with post-quantum mldsa65Verify and XTLS-Vision`() {
        val original = TunnelProfile(
            name = "Frankfurt REALITY PQ",
            protocol = ProtocolType.VLESS,
            serverAddress = "de-edge.aether.example",
            serverPort = 443,
            uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            flow = "xtls-rprx-vision",
            transport = TransportSettings(type = TransportType.RAW_TCP),
            tlsReality = TlsRealityConfig(
                security = SecurityMode.REALITY,
                sni = "www.microsoft.com",
                fingerprint = UtlsFingerprint.CHROME,
                realityPublicKey = "j8_yPq2LmN9vK3xR7tW5zQ1aB4cD6eF8gH0iJ2kL4mM",
                realityShortId = "6ba85179e30d4fc2",
                realitySpiderX = "/telemetry/v1",
                mldsa65Verify = "MLDSA65_PQ_VERIFY_KEY_BASE64URL_SAMPLE_998877",
            ),
        )

        val uri = ShareUriCodec.serializeToUri(original)
        assertTrue(uri.startsWith("vless://"))
        assertTrue(uri.contains("pqv=") || uri.contains("mldsa65Verify="))

        val reparsed = ShareUriCodec.parseUri(uri)
        assertNotNull(reparsed)
        reparsed!!
        assertEquals(ProtocolType.VLESS, reparsed.protocol)
        assertEquals(original.serverAddress, reparsed.serverAddress)
        assertEquals(original.uuid, reparsed.uuid)
        assertEquals("xtls-rprx-vision", reparsed.flow)
        assertEquals(SecurityMode.REALITY, reparsed.tlsReality.security)
        assertEquals(original.tlsReality.realityPublicKey, reparsed.tlsReality.realityPublicKey)
        assertEquals(original.tlsReality.realityShortId, reparsed.tlsReality.realityShortId)
        assertEquals(original.tlsReality.mldsa65Verify, reparsed.tlsReality.mldsa65Verify)
    }

    @Test
    fun `round-trip VMess AEAD Trojan Shadowsocks-2022 Hysteria2 TUICv5 and AnyTLS`() {
        val profiles = listOf(
            TunnelProfile(
                name = "VMess WS TLS",
                protocol = ProtocolType.VMESS,
                serverAddress = "jp1.aether.example",
                serverPort = 8443,
                uuid = "12345678-1234-1234-1234-123456789abc",
                method = "aes-128-gcm",
                transport = TransportSettings(
                    type = TransportType.WS,
                    host = "cdn.jp1.aether.example",
                    path = "/ws-stream",
                ),
                tlsReality = TlsRealityConfig(
                    security = SecurityMode.TLS,
                    sni = "cdn.jp1.aether.example",
                ),
            ),
            TunnelProfile(
                name = "Trojan gRPC",
                protocol = ProtocolType.TROJAN,
                serverAddress = "sg1.aether.example",
                serverPort = 443,
                password = "trojan_secret_key_2026",
                transport = TransportSettings(
                    type = TransportType.GRPC,
                    serviceName = "aether.TrojanGun",
                ),
                tlsReality = TlsRealityConfig(
                    security = SecurityMode.TLS,
                    sni = "sg1.aether.example",
                ),
            ),
            TunnelProfile(
                name = "SS-2022 Blake3",
                protocol = ProtocolType.SHADOWSOCKS,
                serverAddress = "nl1.aether.example",
                serverPort = 9443,
                method = "2022-blake3-aes-256-gcm",
                password = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY=",
            ),
            TunnelProfile(
                name = "Hysteria2 PortHopping",
                protocol = ProtocolType.HYSTERIA2,
                serverAddress = "us-quic.aether.example",
                serverPort = 443,
                password = "hy2_auth_token",
                tlsReality = TlsRealityConfig(
                    security = SecurityMode.TLS,
                    sni = "us-quic.aether.example",
                ),
                quicExtra = QuicProtocolSettings(
                    salamanderPassword = "salamander_obfs_secret",
                    portHoppingRange = "20000-40000",
                ),
            ),
            TunnelProfile(
                name = "TUIC v5 ZeroRTT",
                protocol = ProtocolType.TUIC_V5,
                serverAddress = "uk-tuic.aether.example",
                serverPort = 443,
                uuid = "99999999-8888-7777-6666-555555555555",
                password = "tuic_secret_password",
                tlsReality = TlsRealityConfig(
                    security = SecurityMode.TLS,
                    sni = "uk-tuic.aether.example",
                    alpn = listOf("h3"),
                ),
                quicExtra = QuicProtocolSettings(
                    tuicCongestionControl = "bbr",
                    tuicUdpRelayMode = "native",
                ),
            ),
            TunnelProfile(
                name = "AnyTLS v2 Stealth",
                protocol = ProtocolType.ANYTLS_V2,
                serverAddress = "ch-anytls.aether.example",
                serverPort = 443,
                password = "anytls_v2_token",
                tlsReality = TlsRealityConfig(
                    security = SecurityMode.TLS,
                    sni = "ch-anytls.aether.example",
                ),
            ),
        )

        val base64Feed = parser.exportToBase64Subscription(profiles)
        val decodedProfiles = parser.importAny(base64Feed)

        assertEquals(profiles.size, decodedProfiles.size)
        assertEquals(ProtocolType.VMESS, decodedProfiles[0].protocol)
        assertEquals("/ws-stream", decodedProfiles[0].transport.path)

        assertEquals(ProtocolType.TROJAN, decodedProfiles[1].protocol)
        assertEquals("aether.TrojanGun", decodedProfiles[1].transport.serviceName)

        assertEquals(ProtocolType.SHADOWSOCKS, decodedProfiles[2].protocol)
        assertEquals("2022-blake3-aes-256-gcm", decodedProfiles[2].method)

        assertEquals(ProtocolType.HYSTERIA2, decodedProfiles[3].protocol)
        assertEquals("salamander_obfs_secret", decodedProfiles[3].quicExtra.salamanderPassword)
        assertEquals("20000-40000", decodedProfiles[3].quicExtra.portHoppingRange)

        assertEquals(ProtocolType.TUIC_V5, decodedProfiles[4].protocol)
        assertEquals("bbr", decodedProfiles[4].quicExtra.tuicCongestionControl)

        assertEquals(ProtocolType.ANYTLS_V2, decodedProfiles[5].protocol)
        assertEquals("anytls_v2_token", decodedProfiles[5].password)
    }

    @Test
    fun `round-trip AmneziaWG conf INI with Jc Jmin Jmax S1 S2 H1-H4 and Reserved bytes`() {
        val awgConf = """
            [Interface]
            PrivateKey = cHJpdmF0ZUtleUJhc2U2NFNhbXBsZTEyMzQ1Njc4OTA=
            Address = 172.16.0.2/32, 2606:4700:110::2/128
            DNS = 1.1.1.1, 1.0.0.1
            MTU = 1380
            Reserved = 141, 98, 44
            Jc = 5
            Jmin = 50
            Jmax = 250
            S1 = 48
            S2 = 92
            H1 = 120491821
            H2 = 220491822
            H3 = 320491823
            H4 = 420491824

            [Peer]
            PublicKey = bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo=
            AllowedIPs = 0.0.0.0/0, ::/0
            Endpoint = engage.cloudflareclient.com:2408
            PersistentKeepalive = 25
        """.trimIndent()

        val parsed = parser.importAny(awgConf, hintName = "AWG WARP").single()
        assertEquals(ProtocolType.AMNEZIA_WG, parsed.protocol)
        assertEquals("engage.cloudflareclient.com", parsed.serverAddress)
        assertEquals(2408, parsed.serverPort)
        assertEquals(5, parsed.wireGuard.jc)
        assertEquals(50, parsed.wireGuard.jmin)
        assertEquals(250, parsed.wireGuard.jmax)
        assertEquals(48, parsed.wireGuard.s1)
        assertEquals(92, parsed.wireGuard.s2)
        assertEquals("120491821", parsed.wireGuard.h1)
        assertEquals(listOf(141, 98, 44), parsed.wireGuard.reservedBytes)

        val reserialized = WireGuardAndOpenVpnCodec.serializeWireGuardConf(parsed)
        val reparsed = WireGuardAndOpenVpnCodec.parseWireGuardConf(reserialized)!!
        assertEquals(parsed.wireGuard.jc, reparsed.wireGuard.jc)
        assertEquals(parsed.wireGuard.h4, reparsed.wireGuard.h4)
        assertEquals(parsed.wireGuard.reservedBytes, reparsed.wireGuard.reservedBytes)
    }

    @Test
    fun `parses Clash Mihomo YAML Subscription-Userinfo headers and client deep-links`() {
        val clashYaml = """
            proxies:
              - name: "Mihomo-VLESS-REALITY"
                type: vless
                server: se1.aether.example
                port: 443
                uuid: 11111111-2222-3333-4444-555555555555
                tls: true
                servername: www.apple.com
                flow: xtls-rprx-vision
                client-fingerprint: chrome
                reality-opts:
                  public-key: pubKeySampleXYZ
                  short-id: 1a2b3c4d
              - name: "Mihomo-HY2"
                type: hysteria2
                server: fi1.aether.example
                port: 8443
                password: hy2_yaml_password
                sni: fi1.aether.example
                obfs: salamander
                obfs-password: obfs_yaml_secret
                ports: 30000-35000
        """.trimIndent()

        val imported = parser.importAny(clashYaml)
        assertEquals(2, imported.size)
        assertEquals(ProtocolType.VLESS, imported[0].protocol)
        assertEquals(SecurityMode.REALITY, imported[0].tlsReality.security)
        assertEquals("pubKeySampleXYZ", imported[0].tlsReality.realityPublicKey)
        assertEquals(ProtocolType.HYSTERIA2, imported[1].protocol)
        assertEquals("obfs_yaml_secret", imported[1].quicExtra.salamanderPassword)
        assertEquals("30000-35000", imported[1].quicExtra.portHoppingRange)

        val quota = ClashAndJsonBundleCodec.parseSubscriptionUserInfoHeader(
            "upload=1073741824; download=4294967296; total=107374182400; expire=1893456000"
        )
        assertEquals(1073741824L, quota.uploadBytes)
        assertEquals(4294967296L, quota.downloadBytes)
        assertEquals(107374182400L, quota.totalBytes)
        assertEquals(1893456000L, quota.expireEpochSec)

        val deepLink = parser.parseClientDeepLink(
            "sing-box://import-remote-profile?url=https%3A%2F%2Fsub.aether.example%2Fapi%2Fv1#EnterpriseFeed"
        )
        assertTrue(deepLink is DeepLinkImportResult.SubscriptionDiscovered)
        val discovered = (deepLink as DeepLinkImportResult.SubscriptionDiscovered).feed
        assertEquals("EnterpriseFeed", discovered.name)
        assertEquals("https://sub.aether.example/api/v1", discovered.url)
    }
}
