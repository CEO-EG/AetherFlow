package com.ceoeg.aetherflow.system

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ceoeg.aetherflow.MainActivity
import com.ceoeg.aetherflow.R
import com.ceoeg.aetherflow.core.common.model.VpnConnectionState
import com.ceoeg.aetherflow.core.vpn.UnifiedEngineOrchestrator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetOrchestratorEntryPoint {
    fun orchestrator(): UnifiedEngineOrchestrator
}

class AetherGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetOrchestratorEntryPoint::class.java,
        )
        val orchestrator = entryPoint.orchestrator()

        provideContent {
            val vpnState by orchestrator.connectionState.collectAsState()
            AetherWidgetContent(vpnState = vpnState)
        }
    }
}

@Composable
private fun AetherWidgetContent(vpnState: VpnConnectionState) {
    val isConnected = vpnState is VpnConnectionState.Connected

    val cardBg = ColorProvider(day = Color(0xFF090F19), night = Color(0xFF090F19))
    val accentCyan = ColorProvider(day = Color(0xFF22D3EE), night = Color(0xFF22D3EE))
    val textPrimary = ColorProvider(day = Color(0xFFF1F5F9), night = Color(0xFFF1F5F9))

    GlanceTheme {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(cardBg)
                .cornerRadius(16.dp)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.aetherflow_logo),
                contentDescription = "AetherFlow",
                modifier = GlanceModifier
                    .size(36.dp)
                    .cornerRadius(9.dp),
            )
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "AETHERFLOW TUNNEL",
                    style = TextStyle(
                        color = accentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = when (val s = vpnState) {
                        is VpnConnectionState.Connected -> "${s.profileName} (${s.engine.displayName})"
                        is VpnConnectionState.Connecting -> "Connecting to ${s.profileName}..."
                        is VpnConnectionState.Reconnecting -> "Failover: ${s.targetProfileName}..."
                        else -> "Tunnel Disconnected"
                    },
                    style = TextStyle(
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(GlanceModifier.width(8.dp))
            Button(
                text = if (isConnected) "Stop" else "Connect",
                onClick = actionRunCallback<ToggleTunnelWidgetAction>(),
            )
        }
    }
}

class ToggleTunnelWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetOrchestratorEntryPoint::class.java,
        )
        val orchestrator = entryPoint.orchestrator()
        val state = orchestrator.connectionState.value

        if (state is VpnConnectionState.Connected || state is VpnConnectionState.Connecting) {
            orchestrator.requestDisconnectFromUi()
        } else if (orchestrator.prepareVpnIntent() != null) {
            context.startActivity(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            orchestrator.requestConnectFromUi()
        }
        AetherGlanceWidget().updateAll(context)
    }
}

class AetherGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AetherGlanceWidget()
}
