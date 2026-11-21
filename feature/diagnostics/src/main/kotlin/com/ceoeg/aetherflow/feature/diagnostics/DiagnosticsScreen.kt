package com.ceoeg.aetherflow.feature.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ceoeg.aetherflow.core.common.design.AetherActionChip
import com.ceoeg.aetherflow.core.common.design.AetherFilterChip
import com.ceoeg.aetherflow.core.common.design.AetherMonoTypography
import com.ceoeg.aetherflow.core.common.design.AetherOptionSwitchRow
import com.ceoeg.aetherflow.core.common.design.AetherPalette
import com.ceoeg.aetherflow.core.common.design.ProtocolBadge
import com.ceoeg.aetherflow.core.common.design.formatBytesHuman
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.ActiveSocketConnection
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.DiagnosticLogEntry
import com.ceoeg.aetherflow.core.common.model.LanTetheringSettings
import com.ceoeg.aetherflow.core.common.model.LogSeverity
import com.ceoeg.aetherflow.core.common.model.SessionLoopbackAuth
import com.ceoeg.aetherflow.core.common.model.VpnConnectionState
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import com.ceoeg.aetherflow.core.singbox.ClashApiInspectorClient
import com.ceoeg.aetherflow.core.vpn.UnifiedEngineOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class LanInterfaceEndpoint(
    val ip: String,
    val interfaceName: String,
    val kindLabel: String,
    val priority: Int,
)

data class LanListenerProbeStatus(
    val mixedPortListening: Boolean = false,
    val httpPortListening: Boolean = false,
)

data class DiagnosticsUiState(
    val activeConnections: List<ActiveSocketConnection> = emptyList(),
    val sessionAuth: SessionLoopbackAuth? = null,
    val logEntries: List<DiagnosticLogEntry> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val isTunnelConnected: Boolean = false,
    val detectedLanEndpoints: List<LanInterfaceEndpoint> = emptyList(),
    val listenerProbe: LanListenerProbeStatus = LanListenerProbeStatus(),
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val orchestrator: UnifiedEngineOrchestrator,
    private val clashInspector: ClashApiInspectorClient,
    private val logger: DiagnosticLogger,
    private val settingsStore: EncryptedSettingsStore,
) : ViewModel() {

    private val detectedEndpointsFlow = MutableStateFlow(discoverLocalLanInterfaces())
    private val listenerProbeFlow = MutableStateFlow(LanListenerProbeStatus())

    private val baseFlow = combine(
        orchestrator.activeConnections,
        orchestrator.activeSessionAuth,
        logger.entries,
        settingsStore.settingsFlow,
        orchestrator.connectionState,
    ) { connections, auth, logs, settings, vpnState ->
        DiagnosticsUiState(
            activeConnections = connections,
            sessionAuth = auth,
            logEntries = logs,
            settings = settings,
            isTunnelConnected = vpnState is VpnConnectionState.Connected,
        )
    }

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        baseFlow,
        detectedEndpointsFlow,
        listenerProbeFlow,
    ) { base, endpoints, probe ->
        base.copy(
            detectedLanEndpoints = endpoints,
            listenerProbe = probe,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiagnosticsUiState(),
    )

    fun terminateSocket(connectionId: String) {
        val auth = uiState.value.sessionAuth ?: return
        viewModelScope.launch {
            val closed = clashInspector.closeConnection(auth, connectionId)
            if (closed) {
                logger.info("Inspector", "Terminated socket connection id=$connectionId")
            }
        }
    }

    fun terminateAllSockets() {
        val auth = uiState.value.sessionAuth ?: return
        val ids = uiState.value.activeConnections.map { it.id }
        viewModelScope.launch {
            ids.forEach { id -> clashInspector.closeConnection(auth, id) }
            logger.info("Inspector", "Requested termination of ${ids.size} active sockets")
        }
    }

    fun clearLogs() = logger.clear()

    fun buildExportLogPayload(
        minSeverity: LogSeverity,
        keyword: String,
        redact: Boolean,
    ): String {
        return logger.exportFormattedLogs(
            minSeverity = minSeverity,
            keyword = keyword,
            redactSensitive = redact,
        )
    }

    fun updateLogPreferences(severity: LogSeverity? = null, redactByDefault: Boolean? = null) {
        viewModelScope.launch {
            settingsStore.update { current ->
                current.copy(
                    logSeverity = severity ?: current.logSeverity,
                    redactLogsByDefault = redactByDefault ?: current.redactLogsByDefault,
                )
            }
        }
    }

    fun updateLanTethering(
        reloadIfConnected: Boolean = false,
        transform: (LanTetheringSettings) -> LanTetheringSettings,
    ) {
        viewModelScope.launch {
            settingsStore.update { current ->
                current.copy(lanTethering = transform(current.lanTethering))
            }
            if (reloadIfConnected) {
                orchestrator.reloadActiveSessionIfConnected("LAN/Hotspot Tethering updated")
                delay(450.milliseconds)
            }
            refreshLanNetworkAndListeners()
        }
    }

    fun applyAndReloadLanTethering() {
        viewModelScope.launch {
            orchestrator.reloadActiveSessionIfConnected("LAN/Hotspot Tethering applied")
            delay(500.milliseconds)
            refreshLanNetworkAndListeners()
        }
    }

    fun generateRandomLanPassword() {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        val rng = SecureRandom()
        val pwd = (1..16).map { alphabet[rng.nextInt(alphabet.length)] }.joinToString("")
        updateLanTethering(reloadIfConnected = true) { it.copy(password = pwd, requireAuth = true) }
    }

    fun refreshLanNetworkAndListeners() {
        viewModelScope.launch {
            val lan = settingsStore.current().lanTethering
            val (endpoints, probe) = withContext(Dispatchers.IO) {
                val ifaces = discoverLocalLanInterfaces()
                val mixedOpen = if (lan.enabled) isLocalTcpPortOpen(lan.socksPort) else false
                val httpOpen = if (lan.enabled) {
                    if (lan.httpPort == lan.socksPort) mixedOpen else isLocalTcpPortOpen(lan.httpPort)
                } else {
                    false
                }
                ifaces to LanListenerProbeStatus(
                    mixedPortListening = mixedOpen,
                    httpPortListening = httpOpen,
                )
            }
            detectedEndpointsFlow.value = endpoints
            listenerProbeFlow.value = probe
        }
    }

    private fun isLocalTcpPortOpen(port: Int): Boolean {
        if (port !in 1..65535) return false
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", port), 180)
                true
            }
        }.getOrDefault(false)
    }

    private fun discoverLocalLanInterfaces(): List<LanInterfaceEndpoint> {
        return runCatching {
            val found = mutableListOf<LanInterfaceEndpoint>()
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (nif in Collections.list(interfaces)) {
                if (!nif.isUp || nif.isLoopback) continue
                val name = nif.name.lowercase(Locale.US)
                if (name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("rmnet") ||
                    name.startsWith("ccmni") || name.startsWith("v4-rmnet") || name.startsWith("dummy")
                ) {
                    continue
                }
                for (addr in Collections.list(nif.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                        val ip = addr.hostAddress ?: continue
                        val (kind, priority) = classifyInterface(name, ip)
                        found += LanInterfaceEndpoint(
                            ip = ip,
                            interfaceName = nif.name,
                            kindLabel = kind,
                            priority = priority,
                        )
                    }
                }
            }
            found.sortedWith(compareBy<LanInterfaceEndpoint> { it.priority }.thenBy { it.interfaceName })
        }.getOrDefault(emptyList())
    }

    private fun classifyInterface(lowerName: String, ip: String): Pair<String, Int> {
        return when {
            lowerName.startsWith("ap") || lowerName.startsWith("swlan") ||
                lowerName.startsWith("softap") || lowerName == "wlan1" || lowerName == "wlan2" ||
                ip.startsWith("192.168.43.") || ip.startsWith("192.168.137.") ->
                "Wi-Fi Hotspot" to 0
            lowerName.startsWith("rndis") || lowerName.startsWith("usb") || lowerName.startsWith("ncm") ||
                ip.startsWith("192.168.42.") ->
                "USB Tethering" to 1
            lowerName.startsWith("bt-pan") || lowerName.startsWith("bnep") ||
                ip.startsWith("192.168.44.") ->
                "Bluetooth Tethering" to 2
            lowerName.startsWith("eth") ->
                "Ethernet LAN" to 3
            lowerName.startsWith("wlan") ->
                "Wi-Fi LAN" to 4
            else ->
                "Local Interface" to 5
        }
    }
}

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Connection Inspector" to Icons.Outlined.Cable,
        "Log Console" to Icons.Outlined.Terminal,
        "LAN Tethering" to Icons.Outlined.WifiTethering,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
        ) {
            tabs.forEachIndexed { index, (title, tabIcon) ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = title,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    },
                )
            }
        }

        when (selectedTab) {
            0 -> ConnectionInspectorTab(state = state, viewModel = viewModel)
            1 -> StructuredLogConsoleTab(state = state, viewModel = viewModel)
            2 -> LanTetheringTab(state = state, viewModel = viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConnectionInspectorTab(
    state: DiagnosticsUiState,
    viewModel: DiagnosticsViewModel,
) {
    var filterQuery by remember { mutableStateOf("") }
    val filtered = remember(state.activeConnections, filterQuery) {
        state.activeConnections.filter { conn ->
            filterQuery.isBlank() ||
                conn.host.contains(filterQuery, ignoreCase = true) ||
                conn.destination.contains(filterQuery, ignoreCase = true) ||
                conn.matchedRule.contains(filterQuery, ignoreCase = true) ||
                conn.outboundChain.any { it.contains(filterQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
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
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cable,
                            contentDescription = null,
                            tint = AetherPalette.AetherCyan,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clash-API Socket Inspector",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val endpointLabel = state.sessionAuth?.let {
                                "127.0.0.1:${it.controlApiPort} • Bearer Protected"
                            } ?: "Tunnel Idle • Loopback Unbound"
                            Text(
                                text = endpointLabel,
                                style = AetherMonoTypography.codeSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    ProtocolBadge(
                        text = "${state.activeConnections.size} SOCKETS",
                        accentColor = AetherPalette.EmeraldSignal,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Filter host, IP, rule, or outbound...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    )
                    if (state.activeConnections.isNotEmpty()) {
                        OutlinedButton(
                            onClick = viewModel::terminateAllSockets,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Close All", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (state.sessionAuth == null) {
                        "Connect a tunnel session to inspect live TCP/UDP sockets, matched GeoIP/GeoSite rules, and per-stream byte counters in real time."
                    } else {
                        "No active sockets match the current filter."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.id }) { conn ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    ProtocolBadge(
                                        text = conn.network,
                                        accentColor = if (conn.network == "UDP") AetherPalette.WarningAmber else AetherPalette.AetherCyan,
                                    )
                                    ProtocolBadge(
                                        text = conn.outboundChain.joinToString(" → "),
                                        accentColor = AetherPalette.EmeraldSignal,
                                    )
                                    Text(
                                        text = conn.matchedRule,
                                        style = AetherMonoTypography.codeSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = conn.host.ifBlank { conn.destination },
                                    style = AetherMonoTypography.metricMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${conn.source} → ${conn.destination}  •  ↑ ${formatBytesHuman(conn.uploadBytes)}  ↓ ${formatBytesHuman(conn.downloadBytes)}",
                                    style = AetherMonoTypography.codeSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { viewModel.terminateSocket(conn.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Terminate socket",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StructuredLogConsoleTab(
    state: DiagnosticsUiState,
    viewModel: DiagnosticsViewModel,
) {
    val context = LocalContext.current
    var keyword by remember { mutableStateOf("") }
    var severityFilter by remember { mutableStateOf(LogSeverity.DEBUG) }
    var redactSensitive by remember(state.settings.redactLogsByDefault) {
        mutableStateOf(state.settings.redactLogsByDefault)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            val payload = viewModel.buildExportLogPayload(severityFilter, keyword, redactSensitive)
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
            }
        }
    }

    val visibleEntries = remember(state.logEntries, keyword, severityFilter) {
        state.logEntries
            .asReversed()
            .filter { it.severity.rank <= severityFilter.rank && it.severity != LogSeverity.NONE }
            .filter {
                keyword.isBlank() ||
                    it.message.contains(keyword, ignoreCase = true) ||
                    it.source.contains(keyword, ignoreCase = true)
            }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search telemetry logs...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Outlined.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AetherActionChip(
                onClick = {
                    val text = viewModel.buildExportLogPayload(severityFilter, keyword, redactSensitive)
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("AetherFlow Logs", text))
                },
                label = "Copy",
                icon = Icons.Outlined.ContentCopy,
                iconTint = AetherPalette.AetherCyan,
            )
            AetherActionChip(
                onClick = { exportLauncher.launch("aetherflow-diagnostics.log") },
                label = "Export",
                icon = Icons.Outlined.SaveAlt,
                iconTint = AetherPalette.EmeraldSignal,
            )
            AetherActionChip(
                onClick = viewModel::clearLogs,
                label = "Clear",
                icon = Icons.Outlined.DeleteSweep,
                iconTint = AetherPalette.WarningAmber,
            )
            AetherFilterChip(
                selected = redactSensitive,
                onClick = {
                    val next = !redactSensitive
                    redactSensitive = next
                    viewModel.updateLogPreferences(redactByDefault = next)
                },
                label = if (redactSensitive) "Redaction ON" else "Unredacted",
                icon = Icons.Outlined.Shield,
                accentColor = AetherPalette.EmeraldSignal,
            )
            listOf(
                Triple(LogSeverity.ERROR, Icons.Outlined.ErrorOutline, AetherPalette.AlertCrimson),
                Triple(LogSeverity.WARNING, Icons.Outlined.WarningAmber, AetherPalette.WarningAmber),
                Triple(LogSeverity.INFO, Icons.Outlined.Info, AetherPalette.EmeraldSignal),
                Triple(LogSeverity.DEBUG, Icons.Outlined.BugReport, AetherPalette.AetherCyan),
            ).forEach { (sev, sevIcon, sevAccent) ->
                AetherFilterChip(
                    selected = severityFilter == sev,
                    onClick = {
                        severityFilter = sev
                        viewModel.updateLogPreferences(severity = sev)
                    },
                    label = sev.wireLevel.uppercase(),
                    icon = sevIcon,
                    accentColor = sevAccent,
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (visibleEntries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Diagnostic ring buffer is clean.",
                        style = AetherMonoTypography.metricMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(visibleEntries, key = { it.id }) { entry ->
                        val sevColor = when (entry.severity) {
                            LogSeverity.ERROR -> AetherPalette.AlertCrimson
                            LogSeverity.WARNING -> AetherPalette.WarningAmber
                            LogSeverity.INFO -> AetherPalette.EmeraldSignal
                            LogSeverity.DEBUG, LogSeverity.NONE -> AetherPalette.AetherCyan
                        }
                        val displayMsg = if (redactSensitive) {
                            DiagnosticLogger.redactSensitiveData(entry.message)
                        } else {
                            entry.message
                        }
                        Text(
                            text = "${timeFormat.format(Date(entry.timestampMs))} [${entry.severity.wireLevel.uppercase()}] [${entry.source}] $displayMsg",
                            style = AetherMonoTypography.codeSmall,
                            color = sevColor,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanTetheringTab(
    state: DiagnosticsUiState,
    viewModel: DiagnosticsViewModel,
) {
    val context = LocalContext.current
    val lan = state.settings.lanTethering
    var socksPortText by remember(lan.socksPort) { mutableStateOf(lan.socksPort.toString()) }
    var httpPortText by remember(lan.httpPort) { mutableStateOf(lan.httpPort.toString()) }
    var copyFeedback by remember { mutableStateOf<String?>(null) }

    // Auto-refresh Hotspot/USB/Wi-Fi IPs & live port probes every 3s while on this tab
    LaunchedEffect(lan.enabled, lan.socksPort, lan.httpPort, state.isTunnelConnected) {
        while (isActive) {
            viewModel.refreshLanNetworkAndListeners()
            delay(3.seconds)
        }
    }

    val primaryEndpoint = state.detectedLanEndpoints.firstOrNull()
    val primaryIp = primaryEndpoint?.ip ?: "192.168.43.1"
    val useAuth = lan.requireAuth && lan.password.isNotBlank()
    val portsLive = state.listenerProbe.mixedPortListening || state.listenerProbe.httpPortListening

    val statusAccent = when {
        !lan.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        portsLive -> AetherPalette.EmeraldSignal
        state.isTunnelConnected -> AetherPalette.WarningAmber
        else -> AetherPalette.WarningAmber
    }
    val statusBadgeText = when {
        !lan.enabled -> "DISABLED"
        state.listenerProbe.mixedPortListening && state.listenerProbe.httpPortListening -> "LIVE • 0.0.0.0"
        portsLive -> "PARTIAL LIVE"
        state.isTunnelConnected -> "RELOAD NEEDED"
        else -> "START VPN FIRST"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Live Hotspot / LAN Share Status & Quick Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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
                        Icon(
                            imageVector = if (portsLive) Icons.Outlined.CheckCircle else Icons.Outlined.WifiTethering,
                            contentDescription = null,
                            tint = statusAccent,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "Hotspot & LAN Share",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ProtocolBadge(
                        text = statusBadgeText,
                        accentColor = statusAccent,
                    )
                }

                AetherOptionSwitchRow(
                    title = "Allow Connections from LAN / Hotspot",
                    subtitle = "Binds Mixed (SOCKS5+HTTP) & HTTP proxy on 0.0.0.0 and bypasses LAN/hotspot subnets from TUN (Neko-ray mode)",
                    checked = lan.enabled,
                    onCheckedChange = { enabled ->
                        viewModel.updateLanTethering(reloadIfConnected = true) {
                            it.copy(enabled = enabled, bindAddress = "0.0.0.0")
                        }
                    },
                    icon = Icons.Outlined.WifiTethering,
                    iconTint = AetherPalette.AetherCyan,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = socksPortText,
                        onValueChange = { raw ->
                            socksPortText = raw.filter(Char::isDigit)
                            socksPortText.toIntOrNull()?.takeIf { it in 1024..65535 }?.let { port ->
                                viewModel.updateLanTethering { it.copy(socksPort = port) }
                            }
                        },
                        label = { Text("Mixed / SOCKS5 Port", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingText = {
                            val label = if (state.listenerProbe.mixedPortListening) "Listening" else "SOCKS5 + HTTP"
                            Text(label, maxLines = 1)
                        },
                        textStyle = AetherMonoTypography.metricMedium,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = httpPortText,
                        onValueChange = { raw ->
                            httpPortText = raw.filter(Char::isDigit)
                            httpPortText.toIntOrNull()?.takeIf { it in 1024..65535 }?.let { port ->
                                viewModel.updateLanTethering { it.copy(httpPort = port) }
                            }
                        },
                        label = { Text("HTTP Proxy Port", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingText = {
                            val label = if (state.listenerProbe.httpPortListening) "Listening" else "Wi-Fi Manual Proxy"
                            Text(label, maxLines = 1)
                        },
                        textStyle = AetherMonoTypography.metricMedium,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                AetherOptionSwitchRow(
                    title = "Append HTTP Proxy to VPN (Android 10+)",
                    subtitle = "Advertises the local HTTP proxy to system/tethered apps on Android 10–16 (requires Auth OFF)",
                    checked = lan.appendHttpProxyToVpn,
                    onCheckedChange = { append ->
                        viewModel.updateLanTethering(reloadIfConnected = true) {
                            it.copy(appendHttpProxyToVpn = append)
                        }
                    },
                    icon = Icons.Outlined.Hub,
                    iconTint = AetherPalette.TelemetryCyan,
                )

                AetherOptionSwitchRow(
                    title = "Require LAN Client Credentials",
                    subtitle = "Keep OFF for Android/iOS/PlayStation Wi-Fi Manual Proxy; turn ON for shared public Wi-Fi",
                    checked = lan.requireAuth,
                    onCheckedChange = { req ->
                        viewModel.updateLanTethering(reloadIfConnected = true) { it.copy(requireAuth = req) }
                    },
                    icon = Icons.Outlined.Lock,
                    iconTint = AetherPalette.EmeraldSignal,
                )

                if (lan.requireAuth) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = lan.username,
                            onValueChange = { u -> viewModel.updateLanTethering { it.copy(username = u) } },
                            label = { Text("Username") },
                            textStyle = AetherMonoTypography.metricMedium,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = lan.password,
                            onValueChange = { p -> viewModel.updateLanTethering { it.copy(password = p) } },
                            label = { Text("Password") },
                            textStyle = AetherMonoTypography.metricMedium,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    AetherActionChip(
                        onClick = viewModel::generateRandomLanPassword,
                        label = "Generate Strong 16-Char Password",
                        icon = Icons.Outlined.Key,
                        iconTint = AetherPalette.WarningAmber,
                    )
                }

                // Quick actions row: Open Android Hotspot Settings + Apply & Reload Tunnel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { openAndroidTetheringSettings(context) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Hotspot Settings",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (state.isTunnelConnected) {
                        Button(
                            onClick = viewModel::applyAndReloadLanTethering,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Apply & Reload",
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // 2. Detected Hotspot / LAN Addresses & Neko-ray Style Client Setup
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AetherPalette.AetherCyan.copy(alpha = 0.50f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val summaryBlock = buildString {
                appendLine("Hotspot / LAN Proxy Endpoint:")
                appendLine("  HTTP Proxy (Phones / PC / Consoles): $primaryIp:${lan.httpPort}")
                appendLine("  Mixed SOCKS5 + HTTP Port           : $primaryIp:${lan.socksPort}")
                if (useAuth) {
                    appendLine("  Auth Credentials                   : ${lan.username} / ${lan.password}")
                } else {
                    appendLine("  Auth                               : None (Open to Hotspot/LAN)")
                }
                appendLine()
                appendLine("Detected Local Interfaces:")
                if (state.detectedLanEndpoints.isEmpty()) {
                    appendLine("  • 192.168.43.1 (Default Android Hotspot — turn ON Hotspot then tap Refresh)")
                } else {
                    state.detectedLanEndpoints.forEach { ep ->
                        appendLine("  • ${ep.ip}  [${ep.kindLabel} • ${ep.interfaceName}]")
                    }
                }
            }.trimEnd()

            Column(
                modifier = Modifier.padding(16.dp),
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
                            imageVector = Icons.Outlined.SettingsEthernet,
                            contentDescription = null,
                            tint = AetherPalette.AetherCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Client Proxy Setup",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = viewModel::refreshLanNetworkAndListeners) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh Hotspot IPs",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Highlighted Primary Host & Port Readout
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "PROXY HOSTNAME (${primaryEndpoint?.kindLabel ?: "Hotspot Default"})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = primaryIp,
                            style = AetherMonoTypography.metricMedium,
                            color = AetherPalette.AetherCyan,
                        )
                        Text(
                            text = "HTTP Port: ${lan.httpPort}   •   Mixed/SOCKS5 Port: ${lan.socksPort}",
                            style = AetherMonoTypography.codeSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AetherActionChip(
                        onClick = {
                            copyLanText(context, "HTTP Proxy", "$primaryIp:${lan.httpPort}")
                            copyFeedback = "Copied HTTP Proxy: $primaryIp:${lan.httpPort}"
                        },
                        label = "Copy HTTP ($primaryIp:${lan.httpPort})",
                        icon = Icons.Outlined.ContentCopy,
                        iconTint = AetherPalette.EmeraldSignal,
                    )
                    AetherActionChip(
                        onClick = {
                            copyLanText(context, "Mixed SOCKS5 Proxy", "$primaryIp:${lan.socksPort}")
                            copyFeedback = "Copied Mixed/SOCKS5: $primaryIp:${lan.socksPort}"
                        },
                        label = "Copy Mixed/SOCKS5 (${lan.socksPort})",
                        icon = Icons.Outlined.ContentCopy,
                        iconTint = AetherPalette.AetherCyan,
                    )
                    AetherActionChip(
                        onClick = {
                            copyLanText(context, "LAN Proxy Config", summaryBlock)
                            copyFeedback = "Copied full hotspot setup details."
                        },
                        label = "Copy All Details",
                        icon = Icons.Outlined.ContentCopy,
                        iconTint = AetherPalette.TelemetryCyan,
                    )
                }

                if (copyFeedback != null) {
                    Text(
                        text = copyFeedback.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = AetherPalette.EmeraldSignal,
                    )
                }

                Text(
                    text = summaryBlock,
                    style = AetherMonoTypography.codeSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "How to connect (Neko-ray / v2rayNG method):\n" +
                        "1. Turn ON Mobile Hotspot (or USB Tethering) on this phone.\n" +
                        "2. Enable 'Allow Connections from LAN / Hotspot' above and connect AetherFlow VPN.\n" +
                        "3. On the other phone/laptop, open Wi-Fi settings -> Proxy -> Manual -> enter Host = $primaryIp and Port = ${lan.httpPort} (or ${lan.socksPort} for SOCKS5/HTTP).\n" +
                        "Tip (Android 13–16): If your device Hotspot settings show 'Allow clients to use VPNs', turn it ON too.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun copyLanText(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun openAndroidTetheringSettings(context: Context) {
    val intents = listOf(
        Intent().setComponent(
            ComponentName("com.android.settings", "com.android.settings.TetherSettings")
        ),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
        Intent(Settings.ACTION_WIFI_SETTINGS),
    )
    for (intent in intents) {
        val opened = runCatching {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (opened) break
    }
}
