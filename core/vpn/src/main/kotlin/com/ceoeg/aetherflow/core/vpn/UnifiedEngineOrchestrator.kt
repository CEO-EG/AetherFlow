package com.ceoeg.aetherflow.core.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.ceoeg.aetherflow.core.common.database.TunnelRepository
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.ActiveSocketConnection
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.EngineBackend
import com.ceoeg.aetherflow.core.common.model.ExitNetworkStatus
import com.ceoeg.aetherflow.core.common.model.RoutingMode
import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import com.ceoeg.aetherflow.core.common.model.TrafficSnapshot
import com.ceoeg.aetherflow.core.common.model.TunBackendMode
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.model.VpnConnectionState
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import com.ceoeg.aetherflow.core.common.security.EphemeralLoopbackAuthGenerator
import com.ceoeg.aetherflow.core.common.security.TlsCertificatePinResolver
import com.ceoeg.aetherflow.core.singbox.ClashApiInspectorClient
import com.ceoeg.aetherflow.core.singbox.SingBoxConfigBuilder
import com.ceoeg.aetherflow.core.singbox.SingBoxEngineController
import com.ceoeg.aetherflow.core.tun.TunEngineController
import com.ceoeg.aetherflow.core.wireguard.WireGuardEngineController
import com.ceoeg.aetherflow.core.xray.XrayConfigBuilder
import com.ceoeg.aetherflow.core.xray.XrayEngineController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central multi-core VPN & proxy orchestrator.
 *
 * Responsibilities:
 * 1. Selects optimal backend (`Xray-core`, `sing-box`, or `AmneziaWG/WireGuard`) or chains
 *    Entry -> Exit / WARP detours seamlessly.
 * 2. Generates per-session ephemeral loopback ports & 256-bit SOCKS5 credentials, synchronizing
 *    them atomically between the core engine and `hev-socks5-tunnel`.
 * 3. Streams 1s high-precision Upload/Download bitrate history and Clash-API active connections.
 * 4. Runs zero-drop Smart Auto-Failover health checks when packet loss/timeouts exceed thresholds.
 */
@Singleton
class UnifiedEngineOrchestrator @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val stateMachine: VpnStateMachine,
    private val repository: TunnelRepository,
    private val settingsStore: EncryptedSettingsStore,
    private val authGenerator: EphemeralLoopbackAuthGenerator,
    private val xrayConfigBuilder: XrayConfigBuilder,
    private val xrayController: XrayEngineController,
    private val singBoxConfigBuilder: SingBoxConfigBuilder,
    private val singBoxController: SingBoxEngineController,
    private val clashInspector: ClashApiInspectorClient,
    private val wireGuardController: WireGuardEngineController,
    private val tunController: TunEngineController,
    private val smartNodeSelector: SmartNodeSelector,
    private val prober: EndpointProber,
    private val exitVerifier: ExitNetworkVerifier,
    private val logger: DiagnosticLogger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val connectionState: StateFlow<VpnConnectionState> = stateMachine.state

    private val _trafficHistory = MutableStateFlow<List<TrafficSnapshot>>(
        List(60) { TrafficSnapshot() }
    )
    val trafficHistory: StateFlow<List<TrafficSnapshot>> = _trafficHistory.asStateFlow()

    private val _latestTraffic = MutableStateFlow(TrafficSnapshot())
    val latestTraffic: StateFlow<TrafficSnapshot> = _latestTraffic.asStateFlow()

    private val _activeConnections = MutableStateFlow<List<ActiveSocketConnection>>(emptyList())
    val activeConnections: StateFlow<List<ActiveSocketConnection>> = _activeConnections.asStateFlow()

    private val _exitStatus = MutableStateFlow<ExitNetworkStatus?>(null)
    val exitStatus: StateFlow<ExitNetworkStatus?> = _exitStatus.asStateFlow()

    private val _activeSessionAuth = MutableStateFlow<SessionLoopbackAuth?>(null)
    val activeSessionAuth: StateFlow<SessionLoopbackAuth?> = _activeSessionAuth.asStateFlow()

    private var telemetryJob: Job? = null
    private var healthWatchdogJob: Job? = null
    private var activeEngine: EngineBackend? = null
    private var activeProfile: TunnelProfile? = null
    private var activeTunFd: Int = 0
    private val failedProfileIdsInSession = HashSet<String>()

    /** Checks whether `VpnService.prepare(context)` user consent is required before connecting. */
    fun prepareVpnIntent(): Intent? {
        return runCatching { VpnService.prepare(appContext) }.getOrNull()
    }

    fun requestConnectFromUi(profileIdOverride: String? = null) {
        scope.launch {
            val settings = settingsStore.current()
            if (profileIdOverride != null) {
                settingsStore.update { it.copy(activeProfileId = profileIdOverride) }
            }
            if (settings.routingMode == RoutingMode.LOCAL_PROXY_ONLY ||
                settings.tunSettings.backend == TunBackendMode.PROXY_ONLY
            ) {
                startSessionInternal(tunFd = 0, explicitProfileId = profileIdOverride)
            } else {
                val intent = Intent(appContext, AetherVpnService::class.java).apply {
                    action = AetherVpnService.ACTION_CONNECT
                    if (profileIdOverride != null) {
                        putExtra(AetherVpnService.EXTRA_PROFILE_ID, profileIdOverride)
                    }
                }
                ContextCompat.startForegroundService(appContext, intent)
            }
        }
    }

    fun requestDisconnectFromUi() {
        scope.launch {
            val intent = Intent(appContext, AetherVpnService::class.java).apply {
                action = AetherVpnService.ACTION_DISCONNECT
            }
            runCatching { appContext.startService(intent) }
            stopSessionInternal()
        }
    }

    /**
     * Hot-reloads the running tunnel when LAN/Hotspot Tethering or routing settings change
     * so `0.0.0.0` proxy ports and TUN LAN-bypass routes take effect immediately without manual reconnect.
     */
    fun reloadActiveSessionIfConnected(reason: String) {
        val currentState = connectionState.value as? VpnConnectionState.Connected ?: return
        val profileId = currentState.profileId
        scope.launch {
            logger.info("Orchestrator", "Reloading active tunnel ($reason)...")
            val settings = settingsStore.current()
            if (settings.routingMode == RoutingMode.LOCAL_PROXY_ONLY ||
                settings.tunSettings.backend == TunBackendMode.PROXY_ONLY ||
                activeTunFd <= 0
            ) {
                startSessionInternal(tunFd = activeTunFd, explicitProfileId = profileId)
            } else {
                val intent = Intent(appContext, AetherVpnService::class.java).apply {
                    action = AetherVpnService.ACTION_CONNECT
                    putExtra(AetherVpnService.EXTRA_PROFILE_ID, profileId)
                }
                ContextCompat.startForegroundService(appContext, intent)
            }
        }
    }

    /**
     * Core session startup invoked either by `AetherVpnService` (with an established `tunFd`)
     * or directly in `Proxy-Only` mode / unit tests (`tunFd = 0`).
     */
    suspend fun startSessionInternal(
        tunFd: Int,
        explicitProfileId: String? = null,
    ): Result<TunnelProfile> {
        return runCatching {
            val settings = settingsStore.current()
            val allProfiles = repository.getAllProfiles()
            require(allProfiles.isNotEmpty()) {
                "No tunnel profiles configured. Import a URI, subscription, or enroll Cloudflare WARP first."
            }

            failedProfileIdsInSession.clear()

            val chosenProfile = resolveTargetProfile(
                explicitProfileId = explicitProfileId,
                settings = settings,
                allProfiles = allProfiles,
            ) ?: error("No reachable tunnel endpoint found in the selected group.")

            val detourExit = chosenProfile.detourProfileId?.let { detourId ->
                allProfiles.firstOrNull { it.id == detourId }
            }

            val engine = resolveExecutableEngine(chosenProfile, detourExit)

            stateMachine.onStartConnecting(chosenProfile.name, engine)
            launchEngineAndTun(
                profile = chosenProfile,
                detourExit = detourExit,
                engine = engine,
                settings = settings,
                tunFd = tunFd,
            )

            stateMachine.onConnected(
                profileId = chosenProfile.id,
                profileName = chosenProfile.name,
                protocol = chosenProfile.protocol,
                transportLabel = chosenProfile.transport.type.label,
                engine = engine,
                latestHttpDelayMs = chosenProfile.urlTestDelayMs.takeIf { it > 0L } ?: -1L,
                chainedExitName = detourExit?.name,
            )

            // Non-blocking post-connect HTTP-204 RTT verification so tunnel establishment is instant
            scope.launch {
                delay(500L)
                if (connectionState.value is VpnConnectionState.Connected && activeProfile?.id == chosenProfile.id) {
                    val liveDelay = prober.measureActiveTunnelHttpDelay(
                        settings.latencyTestUrl,
                        _activeSessionAuth.value,
                    )
                    if (liveDelay > 0L) {
                        stateMachine.updateConnectedLatency(liveDelay)
                    }
                }
            }

            startTelemetryAndWatchdogLoops(tunFd)
            chosenProfile
        }.onFailure { err ->
            logger.error("Orchestrator", "Tunnel startup failed: ${err.message}")
            stopEnginesOnly()
            stateMachine.onError(err.message ?: "Unknown connection failure")
        }
    }

    suspend fun stopSessionInternal() {
        stateMachine.onStartDisconnecting()
        telemetryJob?.cancel()
        healthWatchdogJob?.cancel()
        stopEnginesOnly()
        _activeSessionAuth.value = null
        _activeConnections.value = emptyList()
        _latestTraffic.value = TrafficSnapshot()
        activeProfile = null
        activeEngine = null
        activeTunFd = 0
        stateMachine.onDisconnected()
        logger.info("Orchestrator", "Tunnel disconnected cleanly.")
    }

    suspend fun onNetworkHandoffDetected(transport: PhysicalTransportType, currentTunFd: Int) {
        if (transport == PhysicalTransportType.NONE) return
        val currentState = connectionState.value as? VpnConnectionState.Connected ?: return
        val profile = activeProfile ?: return
        logger.info("Orchestrator", "Seamless network handoff detected (-> $transport); verifying active socket...")

        // Allow physical interface route table a brief window to settle
        delay(600L)
        if (connectionState.value !is VpnConnectionState.Connected) return

        val settings = settingsStore.current()
        val delayMs = prober.measureActiveTunnelHttpDelay(settings.latencyTestUrl, _activeSessionAuth.value)
        if (delayMs > 0L) {
            stateMachine.updateConnectedLatency(delayMs)
            return
        }

        val all = repository.getAllProfiles()
        val candidates = profile.subscriptionId?.let { subId ->
            all.filter { it.subscriptionId == subId }.ifEmpty { all }
        } ?: all

        if (settings.smartAutoSelectEnabled && candidates.size > 1) {
            performHotFailoverOrRebind(
                reason = "Network handoff to $transport",
                currentProfile = profile,
                currentEngine = currentState.engine,
                tunFd = currentTunFd,
            )
        } else {
            // Single-profile or manual selection mode: rebind the current profile in-place cleanly
            logger.info("Orchestrator", "Rebinding '${profile.name}' sockets after handoff to $transport")
            val detour = profile.detourProfileId?.let { id -> all.firstOrNull { it.id == id } }
            runCatching {
                launchEngineAndTun(profile, detour, currentState.engine, settings, currentTunFd)
            }.onFailure { err ->
                logger.warn("Orchestrator", "Socket rebind notice on '${profile.name}': ${err.message}")
            }
        }
    }

    suspend fun refreshExitVerification(): Result<ExitNetworkStatus> {
        val res = exitVerifier.verifyExitNetwork(_activeSessionAuth.value)
        res.onSuccess { _exitStatus.value = it }
        return res
    }

    suspend fun triggerLiveLatencyCheck(): Long {
        val settings = settingsStore.current()
        val rtt = prober.measureActiveTunnelHttpDelay(settings.latencyTestUrl, _activeSessionAuth.value)
        if (rtt > 0L) {
            stateMachine.updateConnectedLatency(rtt)
        }
        return rtt
    }

    private fun resolveExecutableEngine(
        profile: TunnelProfile,
        detourExit: TunnelProfile?,
    ): EngineBackend {
        val preferred = profile.resolvedEngine()
        if (detourExit != null && preferred != detourExit.resolvedEngine()) {
            return if (singBoxController.isNativeAvailable) {
                EngineBackend.SING_BOX
            } else {
                EngineBackend.XRAY
            }
        }
        // When AmneziaWG's standalone AAR isn't built locally, Xray-core v26+ natively executes
        // WireGuard & Cloudflare WARP (including 3-byte client_id Reserved headers + UDP Noise)!
        if (preferred == EngineBackend.AMNEZIA_WG &&
            !wireGuardController.isNativeAvailable &&
            xrayController.isNativeAvailable
        ) {
            return EngineBackend.XRAY
        }
        return preferred
    }

    private suspend fun resolveTargetProfile(
        explicitProfileId: String?,
        settings: AppSettings,
        allProfiles: List<TunnelProfile>,
    ): TunnelProfile? {
        if (explicitProfileId != null) {
            return allProfiles.firstOrNull { it.id == explicitProfileId }
        }
        if (settings.smartAutoSelectEnabled) {
            val groupFiltered = settings.activeSubscriptionGroupId?.let { subId ->
                allProfiles.filter { it.subscriptionId == subId }.ifEmpty { allProfiles }
            } ?: allProfiles
            return smartNodeSelector.selectBestEndpoint(groupFiltered, settings)
                ?: groupFiltered.firstOrNull()
        }
        return settings.activeProfileId?.let { id -> allProfiles.firstOrNull { it.id == id } }
            ?: allProfiles.firstOrNull()
    }

    private suspend fun launchEngineAndTun(
        profile: TunnelProfile,
        detourExit: TunnelProfile?,
        engine: EngineBackend,
        settings: AppSettings,
        tunFd: Int,
    ) {
        stopEnginesOnly()

        val customRules = repository.getAllRoutingRules()
        val sessionAuth = authGenerator.generateSessionAuth()
        _activeSessionAuth.value = sessionAuth

        logger.info(
            "Orchestrator",
            "Allocated ephemeral loopback SOCKS5=127.0.0.1:${sessionAuth.socksPort} for engine=${engine.displayName}"
        )
        val lan = settings.lanTethering
        if (lan.enabled) {
            val listenAddr = lan.bindAddress.ifBlank { "0.0.0.0" }
            val authNote = if (lan.requireAuth && lan.password.isNotBlank()) "password auth" else "noauth"
            logger.info(
                "LanTethering",
                "Hotspot/LAN proxy active on $listenAddr:${lan.socksPort} (Mixed SOCKS5+HTTP) & $listenAddr:${lan.httpPort} (HTTP) [$authNote]"
            )
        }

        val effectiveTunMode = if (settings.routingMode == RoutingMode.LOCAL_PROXY_ONLY || tunFd <= 0) {
            TunBackendMode.PROXY_ONLY
        } else {
            settings.tunSettings.backend
        }

        when (engine) {
            EngineBackend.XRAY -> {
                val resolvedProfile = TlsCertificatePinResolver.resolveProfileIfNeeded(profile)
                val resolvedDetour = detourExit?.let { TlsCertificatePinResolver.resolveProfileIfNeeded(it) }
                if (resolvedProfile != profile) {
                    logger.info(
                        "Orchestrator",
                        "Auto-pinned peer TLS SHA-256 (pcs) for '${resolvedProfile.name}' (Xray v26+ SNI/cert mode)"
                    )
                    scope.launch { repository.upsertProfile(resolvedProfile) }
                }
                if (resolvedDetour != null && resolvedDetour != detourExit) {
                    scope.launch { repository.upsertProfile(resolvedDetour) }
                }

                val cfg = xrayConfigBuilder.buildConfig(
                    profile = resolvedProfile,
                    settings = settings,
                    sessionAuth = sessionAuth,
                    customRules = customRules,
                    detourExitProfile = resolvedDetour,
                )
                val coreFd = if (effectiveTunMode == TunBackendMode.CORE_NATIVE_TUN) tunFd else 0
                xrayController.start(cfg, coreFd)
                if (effectiveTunMode != TunBackendMode.CORE_NATIVE_TUN && effectiveTunMode != TunBackendMode.PROXY_ONLY) {
                    tunController.startTun(tunFd, settings.tunSettings, sessionAuth)
                }
            }

            EngineBackend.SING_BOX -> {
                if (!singBoxController.isNativeAvailable && tunFd > 0) {
                    error(
                        "Sing-box native library (libbox.aar) is not built in this workspace yet. " +
                            "Run 'make build-singbox' (with Android NDK) or select an Xray-core / WireGuard / WARP node."
                    )
                }
                val cfg = singBoxConfigBuilder.buildConfig(
                    profile = profile,
                    settings = settings,
                    sessionAuth = sessionAuth,
                    customRules = customRules,
                    detourExitProfile = detourExit,
                )
                singBoxController.start(cfg)
                if (effectiveTunMode != TunBackendMode.CORE_NATIVE_TUN && effectiveTunMode != TunBackendMode.PROXY_ONLY) {
                    tunController.startTun(tunFd, settings.tunSettings, sessionAuth)
                }
            }

            EngineBackend.AMNEZIA_WG, EngineBackend.OPENVPN, EngineBackend.AUTO -> {
                if (!wireGuardController.isNativeAvailable && tunFd > 0) {
                    error("Native WireGuard/AmneziaWG engine unavailable. Run 'make fetch-cores' and rebuild.")
                }
                wireGuardController.start(profile, settings)
            }
        }

        activeEngine = engine
        activeProfile = profile
        activeTunFd = tunFd
    }

    private fun stopEnginesOnly() {
        runCatching { tunController.stopTun() }
        runCatching { xrayController.stop() }
        runCatching { singBoxController.stop() }
        runCatching { wireGuardController.stop() }
    }

    private fun startTelemetryAndWatchdogLoops(tunFd: Int) {
        telemetryJob?.cancel()
        healthWatchdogJob?.cancel()

        telemetryJob = scope.launch {
            var cumulativeUp = 0L
            var cumulativeDown = 0L
            var prevTunStats = 0L to 0L

            while (isActive) {
                delay(1000L)
                val (coreUpDelta, coreDownDelta) = when (activeEngine) {
                    EngineBackend.XRAY -> xrayController.pollTrafficDelta()
                    else -> 0L to 0L
                }
                val (tunTx, tunRx) = tunController.readTunBytesStats()
                val tunUpDelta = (tunTx - prevTunStats.first).coerceAtLeast(0L)
                val tunDownDelta = (tunRx - prevTunStats.second).coerceAtLeast(0L)
                prevTunStats = tunTx to tunRx

                val upBps = maxOf(coreUpDelta, tunUpDelta)
                val downBps = maxOf(coreDownDelta, tunDownDelta)
                cumulativeUp += upBps
                cumulativeDown += downBps

                val snap = TrafficSnapshot(
                    uplinkBps = upBps,
                    downlinkBps = downBps,
                    totalUplinkBytes = cumulativeUp,
                    totalDownlinkBytes = cumulativeDown,
                    timestampMs = System.currentTimeMillis(),
                )
                _latestTraffic.value = snap
                _trafficHistory.value = (_trafficHistory.value.drop(1) + snap)

                val auth = _activeSessionAuth.value
                if (auth != null && activeEngine == EngineBackend.SING_BOX) {
                    _activeConnections.value = clashInspector.fetchActiveConnections(auth)
                }
            }
        }

        healthWatchdogJob = scope.launch {
            var consecutiveFailures = 0
            while (isActive) {
                delay(15_000L)
                val settings = settingsStore.current()
                if (!settings.smartAutoSelectEnabled) continue
                val current = activeProfile ?: continue
                val engine = activeEngine ?: continue

                val delayMs = prober.measureActiveTunnelHttpDelay(
                    settings.latencyTestUrl,
                    _activeSessionAuth.value,
                )
                if (delayMs in 1..settings.autoFailoverTimeoutThresholdMs) {
                    consecutiveFailures = 0
                    stateMachine.updateConnectedLatency(delayMs)
                } else {
                    consecutiveFailures++
                    logger.warn(
                        "AutoFailover",
                        "Health probe degraded on '${current.name}' (delay=${delayMs}ms, strike=$consecutiveFailures)"
                    )
                    if (consecutiveFailures >= settings.autoFailoverMaxConsecutiveFailures) {
                        consecutiveFailures = 0
                        val all = repository.getAllProfiles()
                        if (all.size > 1) {
                            failedProfileIdsInSession += current.id
                            performHotFailoverOrRebind(
                                reason = "Timeout threshold exceeded",
                                currentProfile = current,
                                currentEngine = engine,
                                tunFd = tunFd,
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Publicly testable failover routine: transitions `Connected -> Reconnecting -> Connected`
     * (or rebinds in-place when only a single endpoint is configured).
     */
    suspend fun performHotFailoverOrRebind(
        reason: String,
        currentProfile: TunnelProfile,
        currentEngine: EngineBackend,
        tunFd: Int,
    ) {
        val settings = settingsStore.current()
        val all = repository.getAllProfiles()
        val candidates = currentProfile.subscriptionId?.let { subId ->
            all.filter { it.subscriptionId == subId }.ifEmpty { all }
        } ?: all

        if (candidates.size <= 1) {
            // Only one node exists: rebind in place rather than failing with standby exhaustion
            val detour = currentProfile.detourProfileId?.let { id -> all.firstOrNull { it.id == id } }
            stateMachine.onStartReconnecting(
                reason = reason,
                targetProfileName = currentProfile.name,
                engine = currentEngine,
            )
            runCatching {
                launchEngineAndTun(currentProfile, detour, currentEngine, settings, tunFd)
                val newRtt = prober.measureActiveTunnelHttpDelay(settings.latencyTestUrl, _activeSessionAuth.value)
                stateMachine.onConnected(
                    profileId = currentProfile.id,
                    profileName = currentProfile.name,
                    protocol = currentProfile.protocol,
                    transportLabel = currentProfile.transport.type.label,
                    engine = currentEngine,
                    latestHttpDelayMs = newRtt,
                    chainedExitName = detour?.name,
                )
            }.onFailure { err ->
                stateMachine.onError("Rebind failed on '${currentProfile.name}': ${err.message}")
            }
            return
        }

        val fallback = smartNodeSelector.selectBestEndpoint(
            candidates = candidates,
            settings = settings,
            excludedIds = failedProfileIdsInSession + currentProfile.id,
        )

        if (fallback == null) {
            stateMachine.onError("Auto-failover exhausted: all standby endpoints unreachable.")
            return
        }

        val detour = fallback.detourProfileId?.let { id -> all.firstOrNull { it.id == id } }
        val nextEngine = resolveExecutableEngine(fallback, detour)
        stateMachine.onStartReconnecting(
            reason = reason,
            targetProfileName = fallback.name,
            engine = nextEngine,
        )
        logger.info("AutoFailover", "Hot-swapping tunnel from '${currentProfile.name}' -> '${fallback.name}'")

        runCatching {
            launchEngineAndTun(fallback, detour, nextEngine, settings, tunFd)
            val newRtt = prober.measureActiveTunnelHttpDelay(settings.latencyTestUrl, _activeSessionAuth.value)
            stateMachine.onConnected(
                profileId = fallback.id,
                profileName = fallback.name,
                protocol = fallback.protocol,
                transportLabel = fallback.transport.type.label,
                engine = nextEngine,
                latestHttpDelayMs = newRtt,
                chainedExitName = detour?.name,
            )
        }.onFailure { err ->
            stateMachine.onError("Failover to '${fallback.name}' failed: ${err.message}")
        }
    }
}
