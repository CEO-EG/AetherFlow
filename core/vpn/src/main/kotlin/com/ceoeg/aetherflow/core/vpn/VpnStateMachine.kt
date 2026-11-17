package com.ceoeg.aetherflow.core.vpn

import com.ceoeg.aetherflow.core.common.model.EngineBackend
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.VpnConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic, coroutine-safe connection state machine enforcing valid transitions:
 * `Disconnected` <-> `Connecting` -> `Connected` <-> `Reconnecting` -> `Disconnecting` -> `Disconnected`
 * and `Error`.
 */
@Singleton
class VpnStateMachine @Inject constructor() {

    private val mutex = Mutex()
    private val _state = MutableStateFlow<VpnConnectionState>(VpnConnectionState.Disconnected)
    val state: StateFlow<VpnConnectionState> = _state.asStateFlow()

    suspend fun onStartConnecting(
        profileName: String,
        engine: EngineBackend,
        attempt: Int = 1,
    ): VpnConnectionState = mutex.withLock {
        val next = VpnConnectionState.Connecting(
            profileName = profileName,
            engine = engine,
            attempt = attempt,
        )
        _state.value = next
        next
    }

    suspend fun onConnected(
        profileId: String,
        profileName: String,
        protocol: ProtocolType,
        transportLabel: String,
        engine: EngineBackend,
        connectedSinceEpochMs: Long = System.currentTimeMillis(),
        latestHttpDelayMs: Long = -1L,
        chainedExitName: String? = null,
    ): VpnConnectionState = mutex.withLock {
        val cur = _state.value
        if (cur !is VpnConnectionState.Connecting &&
            cur !is VpnConnectionState.Reconnecting &&
            cur !is VpnConnectionState.Connected
        ) {
            return@withLock cur
        }
        val next = VpnConnectionState.Connected(
            profileId = profileId,
            profileName = profileName,
            protocol = protocol,
            transportLabel = transportLabel,
            engine = engine,
            connectedSinceEpochMs = connectedSinceEpochMs,
            latestHttpDelayMs = latestHttpDelayMs,
            chainedExitName = chainedExitName,
        )
        _state.value = next
        next
    }

    suspend fun updateConnectedLatency(delayMs: Long): VpnConnectionState = mutex.withLock {
        val cur = _state.value
        if (cur is VpnConnectionState.Connected) {
            val next = cur.copy(latestHttpDelayMs = delayMs)
            _state.value = next
            next
        } else {
            cur
        }
    }

    suspend fun onStartReconnecting(
        reason: String,
        targetProfileName: String,
        engine: EngineBackend,
    ): VpnConnectionState = mutex.withLock {
        val cur = _state.value
        if (cur !is VpnConnectionState.Connected && cur !is VpnConnectionState.Reconnecting) {
            return@withLock cur
        }
        val next = VpnConnectionState.Reconnecting(
            reason = reason,
            targetProfileName = targetProfileName,
            engine = engine,
        )
        _state.value = next
        next
    }

    suspend fun onStartDisconnecting(): VpnConnectionState = mutex.withLock {
        if (_state.value is VpnConnectionState.Disconnected) {
            return@withLock VpnConnectionState.Disconnected
        }
        _state.value = VpnConnectionState.Disconnecting
        VpnConnectionState.Disconnecting
    }

    suspend fun onDisconnected(): VpnConnectionState = mutex.withLock {
        _state.value = VpnConnectionState.Disconnected
        VpnConnectionState.Disconnected
    }

    suspend fun onError(reason: String, recoverable: Boolean = true): VpnConnectionState =
        mutex.withLock {
            val next = VpnConnectionState.Error(reason = reason, recoverable = recoverable)
            _state.value = next
            next
        }
}
