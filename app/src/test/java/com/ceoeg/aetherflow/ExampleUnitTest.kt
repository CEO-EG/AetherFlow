package com.ceoeg.aetherflow

import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.RoutingRule
import com.ceoeg.aetherflow.core.common.model.RuleAction
import com.ceoeg.aetherflow.core.common.model.RuleMatchType
import com.ceoeg.aetherflow.core.common.model.SubscriptionFeed
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.security.BackupArchiveManager
import com.ceoeg.aetherflow.core.common.security.VpnBackupSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExampleUnitTest {

    @Test
    fun `encrypted vpnbak archive round-trips profiles feeds rules and settings`() {
        val manager = BackupArchiveManager()
        val snapshot = VpnBackupSnapshot(
            settings = AppSettings(blockAdsAndMalware = true, regionalBypassCode = "ir"),
            profiles = listOf(
                TunnelProfile(
                    name = "Tokyo Edge",
                    protocol = ProtocolType.VLESS,
                    serverAddress = "edge.tokyo.example",
                    serverPort = 443,
                    uuid = "11111111-2222-3333-4444-555555555555",
                )
            ),
            subscriptions = listOf(
                SubscriptionFeed(name = "Corp Feed", url = "https://sub.example.com/feed")
            ),
            routingRules = listOf(
                RoutingRule(
                    name = "Bypass Intranet",
                    matchType = RuleMatchType.CIDR,
                    value = "10.0.0.0/8",
                    action = RuleAction.DIRECT,
                )
            ),
        )

        val encryptedBytes = manager.exportEncryptedArchive(snapshot, "Str0ngPassphrase!")
        val restored = manager.importEncryptedArchive(encryptedBytes, "Str0ngPassphrase!")

        assertEquals(1, restored.profiles.size)
        assertEquals("Tokyo Edge", restored.profiles.first().name)
        assertEquals("ir", restored.settings.regionalBypassCode)
        assertEquals("10.0.0.0/8", restored.routingRules.first().value)

        assertThrows(Exception::class.java) {
            manager.importEncryptedArchive(encryptedBytes, "WrongPassword123")
        }
    }

    @Test
    fun `diagnostic logger redacts public IPs UUIDs and secrets while preserving loopback`() {
        val raw = "Dialing 203.0.113.42:443 uuid=11111111-2222-3333-4444-555555555555 password=superSecret99 via 127.0.0.1:41920"
        val redacted = DiagnosticLogger.redactSensitiveData(raw)

        assertFalse(redacted.contains("203.0.113.42"))
        assertFalse(redacted.contains("11111111-2222-3333-4444-555555555555"))
        assertFalse(redacted.contains("superSecret99"))
        assertTrue(redacted.contains("127.0.0.1"))
    }
}
