package com.ceoeg.aetherflow.core.vpn

import android.content.Context
import com.ceoeg.aetherflow.core.common.database.ProfileDao
import com.ceoeg.aetherflow.core.common.database.ProfileEntity
import com.ceoeg.aetherflow.core.common.database.RoutingRuleDao
import com.ceoeg.aetherflow.core.common.database.RoutingRuleEntity
import com.ceoeg.aetherflow.core.common.database.SubscriptionDao
import com.ceoeg.aetherflow.core.common.database.SubscriptionEntity
import com.ceoeg.aetherflow.core.common.database.TunnelRepository
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.EngineBackend
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import com.ceoeg.aetherflow.core.common.model.TunBackendMode
import com.ceoeg.aetherflow.core.common.model.TunSettings
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.model.VpnConnectionState
import com.ceoeg.aetherflow.core.common.security.CredentialCipher
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import com.ceoeg.aetherflow.core.common.security.EphemeralLoopbackAuthGenerator
import com.ceoeg.aetherflow.core.routing.DnsAndRoutingCompiler
import com.ceoeg.aetherflow.core.singbox.ClashApiInspectorClient
import com.ceoeg.aetherflow.core.singbox.SingBoxConfigBuilder
import com.ceoeg.aetherflow.core.singbox.SingBoxEngineController
import com.ceoeg.aetherflow.core.singbox.SingBoxNativeBridge
import com.ceoeg.aetherflow.core.tun.BadVpnNativeBridge
import com.ceoeg.aetherflow.core.tun.HevTunConfigBuilder
import com.ceoeg.aetherflow.core.tun.HevTunNativeBridge
import com.ceoeg.aetherflow.core.tun.TunEngineController
import com.ceoeg.aetherflow.core.wireguard.AmneziaWireGuardConfigBuilder
import com.ceoeg.aetherflow.core.wireguard.WireGuardEngineController
import com.ceoeg.aetherflow.core.wireguard.WireGuardNativeBridge
import com.ceoeg.aetherflow.core.xray.XrayConfigBuilder
import com.ceoeg.aetherflow.core.xray.XrayEngineController
import com.ceoeg.aetherflow.core.xray.XrayNativeBridge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class VpnStateMachineAndFailoverTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `state machine enforces deterministic lifecycle transitions and guards invalid jumps`() = runTest {
        val sm = VpnStateMachine()
        assertEquals(VpnConnectionState.Disconnected, sm.state.value)

        // Cannot jump directly from Disconnected -> Connected without Connecting first
        sm.onConnected(
            profileId = "p1",
            profileName = "Node-1",
            protocol = ProtocolType.VLESS,
            transportLabel = "TCP",
            engine = EngineBackend.XRAY,
        )
        assertEquals(VpnConnectionState.Disconnected, sm.state.value)

        // Valid transition sequence: Disconnected -> Connecting -> Connected -> Reconnecting -> Connected -> Disconnecting -> Disconnected
        sm.onStartConnecting("Node-1", EngineBackend.XRAY)
        assertTrue(sm.state.value is VpnConnectionState.Connecting)

        sm.onConnected(
            profileId = "p1",
            profileName = "Node-1",
            protocol = ProtocolType.VLESS,
            transportLabel = "TCP",
            engine = EngineBackend.XRAY,
            latestHttpDelayMs = 42L,
        )
        val connected1 = sm.state.value as VpnConnectionState.Connected
        assertEquals("Node-1", connected1.profileName)
        assertEquals(42L, connected1.latestHttpDelayMs)

        sm.onStartReconnecting("Timeout threshold exceeded", "Node-2", EngineBackend.SING_BOX)
        assertTrue(sm.state.value is VpnConnectionState.Reconnecting)

        sm.onConnected(
            profileId = "p2",
            profileName = "Node-2",
            protocol = ProtocolType.HYSTERIA2,
            transportLabel = "QUIC",
            engine = EngineBackend.SING_BOX,
            latestHttpDelayMs = 28L,
        )
        val connected2 = sm.state.value as VpnConnectionState.Connected
        assertEquals("Node-2", connected2.profileName)
        assertEquals(EngineBackend.SING_BOX, connected2.engine)

        sm.onStartDisconnecting()
        assertEquals(VpnConnectionState.Disconnecting, sm.state.value)

        sm.onDisconnected()
        assertEquals(VpnConnectionState.Disconnected, sm.state.value)
    }

    @Test
    fun `orchestrator synchronizes ephemeral SOCKS5 auth with HevTUN and performs automatic node failover`() = runTest {
        val fakeContext = createFakeContext(tempDir)
        val cipher = CredentialCipher()
        val settingsStore = EncryptedSettingsStore(fakeContext, cipher)
        val repository = TunnelRepository(
            profileDao = InMemoryProfileDao(),
            subscriptionDao = InMemorySubscriptionDao(),
            routingRuleDao = InMemoryRoutingRuleDao(),
            cipher = cipher,
        )

        val primaryNode = TunnelProfile(
            id = "node-primary",
            name = "Primary-Xray-Node",
            protocol = ProtocolType.VLESS,
            serverAddress = "primary.aether.example",
            serverPort = 443,
            uuid = "11111111-2222-3333-4444-555555555555",
        )
        val backupFastNode = TunnelProfile(
            id = "node-backup-fast",
            name = "Backup-HY2-Fast",
            protocol = ProtocolType.HYSTERIA2,
            serverAddress = "fast.aether.example",
            serverPort = 443,
            password = "hy2_password",
        )
        val backupSlowNode = TunnelProfile(
            id = "node-backup-slow",
            name = "Backup-Trojan-Slow",
            protocol = ProtocolType.TROJAN,
            serverAddress = "slow.aether.example",
            serverPort = 443,
            password = "trojan_password",
        )
        repository.upsertProfiles(listOf(primaryNode, backupFastNode, backupSlowNode))

        settingsStore.update {
            it.copy(
                activeProfileId = primaryNode.id,
                smartAutoSelectEnabled = true,
                tunSettings = TunSettings(backend = TunBackendMode.HEV_SOCKS5_TUNNEL, mtu = 1420),
            )
        }

        var capturedXrayJson = ""
        var capturedSingBoxJson = ""
        var capturedHevYaml = ""

        val fakeXrayBridge = object : XrayNativeBridge {
            override val isNativeAvailable: Boolean = true
            override fun initEnv(assetDir: String) = Unit
            override fun startLoop(configJson: String, tunFd: Int) {
                capturedXrayJson = configJson
            }
            override fun stopLoop() = Unit
            override fun measureActiveDelay(url: String): Long = 45L
            override fun measureStandaloneConfigDelay(configJson: String, url: String): Long = 45L
            override fun queryOutboundTrafficDelta(): Pair<Long, Long> = 1024L to 4096L
            override fun coreVersion(): String = "Xray-test"
        }

        val fakeSingBoxBridge = object : SingBoxNativeBridge {
            override val isNativeAvailable: Boolean = true
            override fun startService(configJson: String) {
                capturedSingBoxJson = configJson
            }
            override fun stopService() = Unit
            override fun version(): String = "sing-box-test"
        }

        val fakeHevBridge = object : HevTunNativeBridge {
            override val isNativeLibraryAvailable: Boolean = true
            override fun startService(configFilePath: String, tunFd: Int) {
                capturedHevYaml = File(configFilePath).readText()
            }
            override fun stopService() = Unit
            override fun getStats(): LongArray = longArrayOf(10L, 2048L, 20L, 8192L)
        }

        val fakeBadVpnBridge = object : BadVpnNativeBridge {
            override val isBinaryAvailable: Boolean = false
            override fun startTun2Socks(
                tunFd: Int,
                mtu: Int,
                socksPort: Int,
                socksUser: String,
                socksPass: String,
                enableIpv6: Boolean,
            ) = Unit
            override fun stopTun2Socks() = Unit
        }

        val fakeWgBridge = object : WireGuardNativeBridge {
            override val isNativeAvailable: Boolean = true
            override fun startTunnel(tunnelName: String, wgQuickConfig: String) = Unit
            override fun stopTunnel() = Unit
        }

        val fakeProber = object : EndpointProber {
            override suspend fun measureTcpHandshake(host: String, port: Int, timeoutMs: Int): Long = 30L
            override suspend fun measureProfileDelay(profile: TunnelProfile, settings: AppSettings): NodeProbeResult {
                return when (profile.id) {
                    "node-primary" -> NodeProbeResult(profile.id, tcpPingMs = 20L, httpDelayMs = 25L)
                    "node-backup-fast" -> NodeProbeResult(profile.id, tcpPingMs = 30L, httpDelayMs = 38L)
                    else -> NodeProbeResult(profile.id, tcpPingMs = 180L, httpDelayMs = 240L)
                }
            }
            override suspend fun measureActiveTunnelHttpDelay(testUrl: String, sessionAuth: SessionLoopbackAuth?): Long = 35L
            override suspend fun measureActiveDownloadSpeedMbps(speedUrl: String, sessionAuth: SessionLoopbackAuth?): Double = 250.0
        }

        val logger = DiagnosticLogger()
        val compiler = DnsAndRoutingCompiler()
        val orchestrator = UnifiedEngineOrchestrator(
            appContext = fakeContext,
            stateMachine = VpnStateMachine(),
            repository = repository,
            settingsStore = settingsStore,
            authGenerator = EphemeralLoopbackAuthGenerator(),
            xrayConfigBuilder = XrayConfigBuilder(compiler),
            xrayController = XrayEngineController(fakeContext, fakeXrayBridge),
            singBoxConfigBuilder = SingBoxConfigBuilder(compiler),
            singBoxController = SingBoxEngineController(fakeSingBoxBridge),
            clashInspector = ClashApiInspectorClient(),
            wireGuardController = WireGuardEngineController(AmneziaWireGuardConfigBuilder(), fakeWgBridge),
            tunController = TunEngineController(
                context = fakeContext,
                hevConfigBuilder = HevTunConfigBuilder(),
                hevBridge = fakeHevBridge,
                badVpnBridge = fakeBadVpnBridge,
                logger = logger,
            ),
            smartNodeSelector = SmartNodeSelector(fakeProber),
            prober = fakeProber,
            exitVerifier = ExitNetworkVerifier(),
            logger = logger,
        )

        // 1. Start session on primary node with simulated TUN fd = 42
        val startResult = orchestrator.startSessionInternal(tunFd = 42, explicitProfileId = primaryNode.id)
        assertTrue(startResult.isSuccess)

        val sessionAuth1 = orchestrator.activeSessionAuth.value
        assertNotNull(sessionAuth1)
        sessionAuth1!!

        // Verify Xray inbound & HevTUN YAML share the exact ephemeral port and credentials
        assertTrue(capturedXrayJson.contains("\"port\": ${sessionAuth1.socksPort}"))
        assertTrue(capturedXrayJson.contains(sessionAuth1.username))
        assertTrue(capturedXrayJson.contains(sessionAuth1.password))
        assertTrue(capturedHevYaml.contains("port: ${sessionAuth1.socksPort}"))
        assertTrue(capturedHevYaml.contains("username: '${sessionAuth1.username}'"))
        assertTrue(capturedHevYaml.contains("password: '${sessionAuth1.password}'"))
        assertTrue(capturedHevYaml.contains("mtu: 1420"))

        // 2. Simulate primary node timeout and trigger automatic hot failover
        orchestrator.performHotFailoverOrRebind(
            reason = "Primary node packet loss",
            currentProfile = primaryNode,
            currentEngine = EngineBackend.XRAY,
            tunFd = 42,
        )

        val postFailoverState = orchestrator.connectionState.value as VpnConnectionState.Connected
        assertEquals("Backup-HY2-Fast", postFailoverState.profileName)
        assertEquals(EngineBackend.SING_BOX, postFailoverState.engine)
        assertTrue(capturedSingBoxJson.contains("hysteria2"))

        // Clean shutdown
        orchestrator.stopSessionInternal()
        assertEquals(VpnConnectionState.Disconnected, orchestrator.connectionState.value)
    }

    @Test
    fun `single configured endpoint rebinds in place on network handoff instead of failing with auto-failover exhausted`() = runTest {
        val fakeContext = createFakeContext(tempDir)
        val cipher = CredentialCipher()
        val settingsStore = EncryptedSettingsStore(fakeContext, cipher)
        val repository = TunnelRepository(
            profileDao = InMemoryProfileDao(),
            subscriptionDao = InMemorySubscriptionDao(),
            routingRuleDao = InMemoryRoutingRuleDao(),
            cipher = cipher,
        )

        val singleNode = TunnelProfile(
            id = "vless-tcp-ahmed",
            name = "vless-tcp-ahmed",
            protocol = ProtocolType.VLESS,
            serverAddress = "edge.aether.example",
            serverPort = 443,
            uuid = "11111111-2222-3333-4444-555555555555",
        )
        repository.upsertProfile(singleNode)

        settingsStore.update {
            it.copy(
                activeProfileId = singleNode.id,
                smartAutoSelectEnabled = false,
                tunSettings = TunSettings(backend = TunBackendMode.HEV_SOCKS5_TUNNEL, mtu = 1400),
            )
        }

        var startCount = 0
        val fakeXrayBridge = object : XrayNativeBridge {
            override val isNativeAvailable: Boolean = true
            override fun initEnv(assetDir: String) = Unit
            override fun startLoop(configJson: String, tunFd: Int) {
                startCount++
            }
            override fun stopLoop() = Unit
            override fun measureActiveDelay(url: String): Long = 52L
            override fun measureStandaloneConfigDelay(configJson: String, url: String): Long = 52L
            override fun queryOutboundTrafficDelta(): Pair<Long, Long> = 0L to 0L
            override fun coreVersion(): String = "Xray-test"
        }

        val fakeSingBoxBridge = object : SingBoxNativeBridge {
            override val isNativeAvailable: Boolean = false
            override fun startService(configJson: String) = Unit
            override fun stopService() = Unit
            override fun version(): String = "none"
        }

        val fakeHevBridge = object : HevTunNativeBridge {
            override val isNativeLibraryAvailable: Boolean = true
            override fun startService(configFilePath: String, tunFd: Int) = Unit
            override fun stopService() = Unit
            override fun getStats(): LongArray = longArrayOf(0L, 0L, 0L, 0L)
        }

        val fakeBadVpnBridge = object : BadVpnNativeBridge {
            override val isBinaryAvailable: Boolean = false
            override fun startTun2Socks(
                tunFd: Int,
                mtu: Int,
                socksPort: Int,
                socksUser: String,
                socksPass: String,
                enableIpv6: Boolean,
            ) = Unit
            override fun stopTun2Socks() = Unit
        }

        val fakeWgBridge = object : WireGuardNativeBridge {
            override val isNativeAvailable: Boolean = false
            override fun startTunnel(tunnelName: String, wgQuickConfig: String) = Unit
            override fun stopTunnel() = Unit
        }

        var simulateDroppedSocket = false
        val fakeProber = object : EndpointProber {
            override suspend fun measureTcpHandshake(host: String, port: Int, timeoutMs: Int): Long = 40L
            override suspend fun measureProfileDelay(profile: TunnelProfile, settings: AppSettings): NodeProbeResult =
                NodeProbeResult(profile.id, tcpPingMs = 40L, httpDelayMs = 52L)
            override suspend fun measureActiveTunnelHttpDelay(testUrl: String, sessionAuth: SessionLoopbackAuth?): Long =
                if (simulateDroppedSocket) -1L else 52L
            override suspend fun measureActiveDownloadSpeedMbps(speedUrl: String, sessionAuth: SessionLoopbackAuth?): Double = 100.0
        }

        val logger = DiagnosticLogger()
        val compiler = DnsAndRoutingCompiler()
        val orchestrator = UnifiedEngineOrchestrator(
            appContext = fakeContext,
            stateMachine = VpnStateMachine(),
            repository = repository,
            settingsStore = settingsStore,
            authGenerator = EphemeralLoopbackAuthGenerator(),
            xrayConfigBuilder = XrayConfigBuilder(compiler),
            xrayController = XrayEngineController(fakeContext, fakeXrayBridge),
            singBoxConfigBuilder = SingBoxConfigBuilder(compiler),
            singBoxController = SingBoxEngineController(fakeSingBoxBridge),
            clashInspector = ClashApiInspectorClient(),
            wireGuardController = WireGuardEngineController(AmneziaWireGuardConfigBuilder(), fakeWgBridge),
            tunController = TunEngineController(
                context = fakeContext,
                hevConfigBuilder = HevTunConfigBuilder(),
                hevBridge = fakeHevBridge,
                badVpnBridge = fakeBadVpnBridge,
                logger = logger,
            ),
            smartNodeSelector = SmartNodeSelector(fakeProber),
            prober = fakeProber,
            exitVerifier = ExitNetworkVerifier(),
            logger = logger,
        )

        assertTrue(orchestrator.startSessionInternal(tunFd = 42, explicitProfileId = singleNode.id).isSuccess)
        assertEquals(1, startCount)
        assertTrue(orchestrator.connectionState.value is VpnConnectionState.Connected)

        // Simulate cellular/Wi-Fi handoff where the old socket died (-1L) when only 1 node is configured
        simulateDroppedSocket = true
        orchestrator.onNetworkHandoffDetected(PhysicalTransportType.CELLULAR, currentTunFd = 42)
        simulateDroppedSocket = false

        val afterHandoff = orchestrator.connectionState.value
        assertTrue(afterHandoff is VpnConnectionState.Connected, "Expected Connected after handoff, got $afterHandoff")
        assertEquals("vless-tcp-ahmed", (afterHandoff as VpnConnectionState.Connected).profileName)
        assertEquals(2, startCount)

        // Even if performHotFailoverOrRebind is invoked directly with only 1 node, it rebinds cleanly in place
        orchestrator.performHotFailoverOrRebind(
            reason = "Transient carrier timeout",
            currentProfile = singleNode,
            currentEngine = EngineBackend.XRAY,
            tunFd = 42,
        )
        val afterDirectRebind = orchestrator.connectionState.value
        assertTrue(afterDirectRebind is VpnConnectionState.Connected)
        assertEquals(3, startCount)

        orchestrator.stopSessionInternal()
    }

    private fun createFakeContext(rootDir: File): Context {
        val files = File(rootDir, "files").apply { mkdirs() }
        val cache = File(rootDir, "cache").apply { mkdirs() }
        return object : android.content.ContextWrapper(null) {
            override fun getFilesDir(): File = files
            override fun getCacheDir(): File = cache
            override fun getApplicationContext(): Context = this
            override fun getPackageName(): String = "com.ceoeg.aetherflow.test"
        }
    }
}

private class InMemoryProfileDao : ProfileDao {
    private val items = LinkedHashMap<String, ProfileEntity>()
    private val flow = MutableStateFlow<List<ProfileEntity>>(emptyList())

    override fun observeAll(): Flow<List<ProfileEntity>> = flow
    override suspend fun getAll(): List<ProfileEntity> = items.values.toList()
    override suspend fun getBySubscription(subscriptionId: String): List<ProfileEntity> =
        items.values.filter { it.subscriptionId == subscriptionId }
    override suspend fun getById(id: String): ProfileEntity? = items[id]
    override suspend fun upsert(entity: ProfileEntity) {
        items[entity.id] = entity
        flow.value = items.values.toList()
    }
    override suspend fun upsertAll(entities: List<ProfileEntity>) {
        entities.forEach { items[it.id] = it }
        flow.value = items.values.toList()
    }
    override suspend fun deleteById(id: String) {
        items.remove(id)
        flow.value = items.values.toList()
    }
    override suspend fun deleteByIds(ids: List<String>) {
        ids.forEach { items.remove(it) }
        flow.value = items.values.toList()
    }
    override suspend fun deleteBySubscription(subscriptionId: String) {
        items.entries.removeIf { it.value.subscriptionId == subscriptionId }
        flow.value = items.values.toList()
    }
    override suspend fun deleteAll() {
        items.clear()
        flow.value = emptyList()
    }
}

private class InMemorySubscriptionDao : SubscriptionDao {
    private val items = LinkedHashMap<String, SubscriptionEntity>()
    private val flow = MutableStateFlow<List<SubscriptionEntity>>(emptyList())

    override fun observeAll(): Flow<List<SubscriptionEntity>> = flow
    override suspend fun getAll(): List<SubscriptionEntity> = items.values.toList()
    override suspend fun getById(id: String): SubscriptionEntity? = items[id]
    override suspend fun upsert(entity: SubscriptionEntity) {
        items[entity.id] = entity
        flow.value = items.values.toList()
    }
    override suspend fun upsertAll(entities: List<SubscriptionEntity>) {
        entities.forEach { items[it.id] = it }
        flow.value = items.values.toList()
    }
    override suspend fun deleteById(id: String) {
        items.remove(id)
        flow.value = items.values.toList()
    }
    override suspend fun deleteAll() {
        items.clear()
        flow.value = emptyList()
    }
}

private class InMemoryRoutingRuleDao : RoutingRuleDao {
    private val items = LinkedHashMap<String, RoutingRuleEntity>()
    private val flow = MutableStateFlow<List<RoutingRuleEntity>>(emptyList())

    override fun observeAll(): Flow<List<RoutingRuleEntity>> = flow
    override suspend fun getAll(): List<RoutingRuleEntity> = items.values.toList()
    override suspend fun upsert(entity: RoutingRuleEntity) {
        items[entity.id] = entity
        flow.value = items.values.toList()
    }
    override suspend fun upsertAll(entities: List<RoutingRuleEntity>) {
        entities.forEach { items[it.id] = it }
        flow.value = items.values.toList()
    }
    override suspend fun deleteById(id: String) {
        items.remove(id)
        flow.value = items.values.toList()
    }
    override suspend fun deleteAll() {
        items.clear()
        flow.value = emptyList()
    }
}
