package com.ceoeg.aetherflow.system

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import com.ceoeg.aetherflow.MainActivity
import com.ceoeg.aetherflow.core.common.model.VpnConnectionState
import com.ceoeg.aetherflow.core.vpn.UnifiedEngineOrchestrator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android System Quick Settings Tile allowing 1-tap encrypted VPN toggle
 * directly from the notification shade.
 */
@AndroidEntryPoint
class AetherQuickSettingsTileService : TileService() {

    @Inject
    lateinit var orchestrator: UnifiedEngineOrchestrator

    private val tileScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        stateJob?.cancel()
        stateJob = tileScope.launch {
            orchestrator.connectionState.collect { vpnState ->
                syncTile(vpnState)
            }
        }
    }

    override fun onStopListening() {
        stateJob?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val current = orchestrator.connectionState.value
        if (current is VpnConnectionState.Connected || current is VpnConnectionState.Connecting) {
            orchestrator.requestDisconnectFromUi()
            return
        }

        // If OS VPN consent hasn't been granted yet, launch MainActivity to prompt
        if (orchestrator.prepareVpnIntent() != null) {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val wrapper = PendingIntentActivityWrapper(
                this,
                201,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT,
                false,
            )
            TileServiceCompat.startActivityAndCollapse(this, wrapper)
        } else {
            orchestrator.requestConnectFromUi()
        }
    }

    override fun onDestroy() {
        tileScope.cancel()
        super.onDestroy()
    }

    private fun syncTile(state: VpnConnectionState) {
        val tile = qsTile ?: return
        when (state) {
            is VpnConnectionState.Connected -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "AetherFlow"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = state.profileName
                }
            }
            is VpnConnectionState.Connecting, is VpnConnectionState.Reconnecting -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "Connecting..."
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = state.statusLabel
                }
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "AetherFlow"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = "Disconnected"
                }
            }
        }
        tile.updateTile()
    }
}
