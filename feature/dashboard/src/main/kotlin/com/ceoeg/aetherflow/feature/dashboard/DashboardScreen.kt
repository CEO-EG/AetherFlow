package com.ceoeg.aetherflow.feature.dashboard

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ceoeg.aetherflow.core.common.database.TunnelRepository
import com.ceoeg.aetherflow.core.common.design.AetherActionChip
import com.ceoeg.aetherflow.core.common.design.AetherMonoTypography
import com.ceoeg.aetherflow.core.common.design.AetherPalette
import com.ceoeg.aetherflow.core.common.design.ProtocolBadge
import com.ceoeg.aetherflow.core.common.design.formatBitrateHuman
import com.ceoeg.aetherflow.core.common.design.formatBytesHuman
import com.ceoeg.aetherflow.core.common.design.formatDurationHms
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.ExitNetworkStatus
import com.ceoeg.aetherflow.core.common.model.RoutingMode
import com.ceoeg.aetherflow.core.common.model.TrafficSnapshot
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.model.VpnConnectionState
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import com.ceoeg.aetherflow.core.vpn.UnifiedEngineOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val vpnState: VpnConnectionState = VpnConnectionState.Disconnected,
    val settings: AppSettings = AppSettings(),
    val selectedProfile: TunnelProfile? = null,
    val latestTraffic: TrafficSnapshot = TrafficSnapshot(),
    val trafficHistory: List<TrafficSnapshot> = emptyList(),
    val exitStatus: ExitNetworkStatus? = null,
    val isVerifyingExit: Boolean = false,
    val profileCount: Int = 0,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val orchestrator: UnifiedEngineOrchestrator,
    private val repository: TunnelRepository,
    private val settingsStore: EncryptedSettingsStore,
) : ViewModel() {

    private val verifyingExit = MutableStateFlow(false)
    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        orchestrator.connectionState,
        settingsStore.settingsFlow,
        repository.profilesFlow,
        orchestrator.trafficHistory,
        combine(orchestrator.exitStatus, verifyingExit) { exit, loading -> exit to loading },
    ) { vpnState, settings, profiles, history, (exit, loading) ->
        val selected = settings.activeProfileId?.let { id -> profiles.firstOrNull { it.id == id } }
            ?: profiles.firstOrNull()
        DashboardUiState(
            vpnState = vpnState,
            settings = settings,
            selectedProfile = selected,
            latestTraffic = history.lastOrNull() ?: TrafficSnapshot(),
            trafficHistory = history,
            exitStatus = exit,
            isVerifyingExit = loading,
            profileCount = profiles.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun prepareVpnConsent() = orchestrator.prepareVpnIntent()

    fun connect() {
        orchestrator.requestConnectFromUi()
    }

    fun disconnect() {
        orchestrator.requestDisconnectFromUi()
    }

    fun setRoutingMode(mode: RoutingMode) {
        viewModelScope.launch {
            settingsStore.update { it.copy(routingMode = mode) }
        }
    }

    fun refreshLatency() {
        viewModelScope.launch {
            val ms = orchestrator.triggerLiveLatencyCheck()
            _statusBanner.value = if (ms > 0L) "HTTP-204 RTT: $ms ms" else "Latency probe timed out"
        }
    }

    fun verifyExitNetwork() {
        viewModelScope.launch {
            verifyingExit.value = true
            val res = orchestrator.refreshExitVerification()
            verifyingExit.value = false
            res.onFailure {
                _statusBanner.value = "Exit check failed: ${it.message}"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToProfiles: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.connect()
        }
    }

    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.vpnState) {
        if (state.vpnState is VpnConnectionState.Connected) {
            while (isActive) {
                nowEpochMs = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PrimaryConnectionCard(
            state = state,
            nowEpochMs = nowEpochMs,
            onToggleConnection = {
                when (state.vpnState) {
                    is VpnConnectionState.Connected,
                    is VpnConnectionState.Connecting,
                    is VpnConnectionState.Reconnecting -> viewModel.disconnect()
                    else -> {
                        val consentIntent = viewModel.prepareVpnConsent()
                        if (consentIntent != null && state.settings.routingMode != RoutingMode.LOCAL_PROXY_ONLY) {
                            vpnPermissionLauncher.launch(consentIntent)
                        } else {
                            viewModel.connect()
                        }
                    }
                }
            },
            onSelectEndpointClick = onNavigateToProfiles,
            onPingRefresh = viewModel::refreshLatency,
        )

        // Segmented Routing Mode Selector (compact, single-line fit with icons across all phone widths)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "ROUTING TOPOLOGY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RoutingMode.entries.forEachIndexed { index, mode ->
                        val isSelected = state.settings.routingMode == mode
                        val modeIcon = when (mode) {
                            RoutingMode.FULL_TUNNEL -> Icons.Outlined.Shield
                            RoutingMode.SMART_GEO_BYPASS -> Icons.Outlined.Public
                            RoutingMode.LOCAL_PROXY_ONLY -> Icons.Outlined.Hub
                        }
                        SegmentedButton(
                            selected = isSelected,
                            onClick = { viewModel.setRoutingMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = RoutingMode.entries.size,
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.primary,
                                activeBorderColor = MaterialTheme.colorScheme.primary,
                                inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                                inactiveBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            icon = {
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = mode.shortLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.5.sp,
                                        letterSpacing = 0.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }

        // High-Precision Live Bitrate & Cumulative Telemetry Card
        LiveTelemetryChartCard(
            latest = state.latestTraffic,
            history = state.trafficHistory,
        )

        // 1-Tap Exit Network Verification Card
        ExitNetworkVerificationCard(
            exitStatus = state.exitStatus,
            isLoading = state.isVerifyingExit,
            onVerifyClick = viewModel::verifyExitNetwork,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrimaryConnectionCard(
    state: DashboardUiState,
    nowEpochMs: Long,
    onToggleConnection: () -> Unit,
    onSelectEndpointClick: () -> Unit,
    onPingRefresh: () -> Unit,
) {
    val vpn = state.vpnState
    val isConnected = vpn is VpnConnectionState.Connected
    val isBusy = vpn is VpnConnectionState.Connecting || vpn is VpnConnectionState.Reconnecting

    val statusColor by animateColorAsState(
        targetValue = when (vpn) {
            is VpnConnectionState.Connected -> AetherPalette.EmeraldSignal
            is VpnConnectionState.Connecting, is VpnConnectionState.Reconnecting -> AetherPalette.WarningAmber
            is VpnConnectionState.Error -> AetherPalette.AlertCrimson
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 280),
        label = "statusAccent",
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (vpn is VpnConnectionState.Disconnected) {
                MaterialTheme.colorScheme.outline
            } else {
                statusColor.copy(alpha = if (isConnected) 0.72f else 0.45f)
            },
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(11.dp),
                        shape = CircleShape,
                        color = statusColor,
                    ) {}
                    Text(
                        text = vpn.statusLabel.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(8.dp))

                if (vpn is VpnConnectionState.Connected) {
                    val elapsedSec = ((nowEpochMs - vpn.connectedSinceEpochMs) / 1000L).coerceAtLeast(0L)
                    Text(
                        text = formatDurationHms(elapsedSec),
                        style = AetherMonoTypography.metricMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                    )
                } else {
                    ProtocolBadge(
                        text = state.settings.tunSettings.backend.label,
                        accentColor = AetherPalette.AetherCyan,
                    )
                }
            }

            // Active or staged endpoint details
            val activeName = when (vpn) {
                is VpnConnectionState.Connected -> vpn.profileName
                is VpnConnectionState.Connecting -> vpn.profileName
                is VpnConnectionState.Reconnecting -> vpn.targetProfileName
                else -> if (state.settings.smartAutoSelectEnabled) {
                    "Smart Auto-Select (${state.profileCount} endpoints)"
                } else {
                    state.selectedProfile?.name ?: "No Endpoint Configured"
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = activeName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val protoBadge = when (vpn) {
                        is VpnConnectionState.Connected -> "${vpn.protocol.badgeLabel} • ${vpn.transportLabel}"
                        else -> state.selectedProfile?.let {
                            "${it.protocol.badgeLabel} • ${it.transport.type.label}"
                        } ?: "MULTI-CORE READY"
                    }
                    val engineBadge = when (vpn) {
                        is VpnConnectionState.Connected -> vpn.engine.displayName
                        is VpnConnectionState.Connecting -> vpn.engine.displayName
                        else -> state.selectedProfile?.resolvedEngine()?.displayName ?: "Xray / Sing-box / AWG"
                    }
                    ProtocolBadge(text = protoBadge, accentColor = AetherPalette.EmeraldSignal)
                    ProtocolBadge(text = engineBadge, accentColor = AetherPalette.AetherCyan)

                    if (vpn is VpnConnectionState.Connected && vpn.chainedExitName != null) {
                        ProtocolBadge(
                            text = "CHAIN → ${vpn.chainedExitName}",
                            accentColor = AetherPalette.WarningAmber,
                        )
                    }
                }

                if (vpn is VpnConnectionState.Error) {
                    Text(
                        text = vpn.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // HTTP-204 Latency + Endpoint Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val rtt = when (vpn) {
                    is VpnConnectionState.Connected -> vpn.latestHttpDelayMs
                    else -> state.selectedProfile?.urlTestDelayMs ?: -1L
                }
                AetherActionChip(
                    onClick = onPingRefresh,
                    label = if (rtt > 0L) "HTTP-204: $rtt ms" else "Test Latency",
                    icon = Icons.Outlined.Speed,
                    iconTint = AetherPalette.EmeraldSignal,
                )

                Spacer(Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onSelectEndpointClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Switch Node",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            // Authoritative Enterprise Connect / Disconnect Action Button (TV D-pad & Touch friendly)
            val isDangerAction = isConnected || isBusy
            Button(
                onClick = onToggleConnection,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .focusable()
                    .semantics {
                        contentDescription = if (isConnected) "Disconnect VPN tunnel" else "Connect VPN tunnel"
                    },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDangerAction) {
                        AetherPalette.AlertCrimson
                    } else {
                        AetherPalette.EmeraldSignal
                    },
                    contentColor = if (isDangerAction) {
                        Color.White
                    } else {
                        Color(0xFF022C22)
                    },
                ),
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Outlined.Security else Icons.Outlined.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        isConnected -> "DISCONNECT TUNNEL"
                        isBusy -> "CANCEL CONNECTION"
                        else -> "ACTIVATE SECURE TUNNEL"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LiveTelemetryChartCard(
    latest: TrafficSnapshot,
    history: List<TrafficSnapshot>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "LIVE THROUGHPUT TELEMETRY (60S)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = AetherPalette.EmeraldSignal,
                    modifier = Modifier.size(18.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DOWNLOAD",
                        style = MaterialTheme.typography.labelMedium,
                        color = AetherPalette.EmeraldSignal,
                    )
                    Text(
                        text = formatBitrateHuman(latest.downlinkBps),
                        style = AetherMonoTypography.metricLarge.copy(fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Session: ${formatBytesHuman(latest.totalDownlinkBytes)}",
                        style = AetherMonoTypography.codeSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = "UPLOAD",
                        style = MaterialTheme.typography.labelMedium,
                        color = AetherPalette.AetherCyan,
                    )
                    Text(
                        text = formatBitrateHuman(latest.uplinkBps),
                        style = AetherMonoTypography.metricLarge.copy(fontSize = 20.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Session: ${formatBytesHuman(latest.totalUplinkBytes)}",
                        style = AetherMonoTypography.codeSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    for (row in 1..3) {
                        val y = h * (row / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                        )
                    }

                    val samples = history.ifEmpty { listOf(TrafficSnapshot()) }
                    val peak = samples.maxOf { maxOf(it.downlinkBps, it.uplinkBps) }
                        .coerceAtLeast(64 * 1024L)
                        .toFloat()

                    fun buildSeriesPath(selector: (TrafficSnapshot) -> Long): Path {
                        val path = Path()
                        val count = samples.size.coerceAtLeast(2)
                        samples.forEachIndexed { idx, snap ->
                            val x = (idx.toFloat() / (count - 1).coerceAtLeast(1)) * w
                            val ratio = (selector(snap).toFloat() / peak).coerceIn(0f, 1f)
                            val y = h - (ratio * (h * 0.88f)) - 4f
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        return path
                    }

                    val downPath = buildSeriesPath { it.downlinkBps }
                    val upPath = buildSeriesPath { it.uplinkBps }

                    val fillPath = Path().apply {
                        addPath(downPath)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AetherPalette.EmeraldSignal.copy(alpha = 0.25f),
                                AetherPalette.EmeraldSignal.copy(alpha = 0.0f),
                            )
                        ),
                    )
                    drawPath(
                        path = downPath,
                        color = AetherPalette.EmeraldSignal,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = upPath,
                        color = AetherPalette.AetherCyan,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExitNetworkVerificationCard(
    exitStatus: ExitNetworkStatus?,
    isLoading: Boolean,
    onVerifyClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        tint = AetherPalette.AetherCyan,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "EXIT NETWORK & LEAK AUDIT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onVerifyClick, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Verify tunnel exit IP and DNS leak status",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (exitStatus == null) {
                Text(
                    text = "Tap refresh to audit active tunnel exit IP, ASN/ISP, geolocation, and DNS leak protection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = exitStatus.ip,
                            style = AetherMonoTypography.metricMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${exitStatus.country} (${exitStatus.countryCode}) • ${exitStatus.asn} ${exitStatus.isp}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = exitStatus.resolverSummary,
                            style = AetherMonoTypography.codeSmall,
                            color = if (exitStatus.dnsLeakDetected) {
                                AetherPalette.WarningAmber
                            } else {
                                AetherPalette.EmeraldSignal
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ProtocolBadge(
                        text = if (exitStatus.dnsLeakDetected) "UNTUNNELED" else "LEAK-FREE",
                        accentColor = if (exitStatus.dnsLeakDetected) {
                            AetherPalette.WarningAmber
                        } else {
                            AetherPalette.EmeraldSignal
                        },
                    )
                }
            }
        }
    }
}
