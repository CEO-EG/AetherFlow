package com.ceoeg.aetherflow.feature.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ceoeg.aetherflow.core.common.design.AetherMonoTypography
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
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import com.ceoeg.aetherflow.core.singbox.ClashApiInspectorClient
import com.ceoeg.aetherflow.core.vpn.UnifiedEngineOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DiagnosticsUiState(
    val activeConnections: List<ActiveSocketConnection> = emptyList(),
    val sessionAuth: SessionLoopbackAuth? = null,
    val logEntries: List<DiagnosticLogEntry> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val detectedLanIps: List<String> = emptyList(),
)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val orchestrator: UnifiedEngineOrchestrator,
    private val clashInspector: ClashApiInspectorClient,
    private val logger: DiagnosticLogger,
    private val settingsStore: EncryptedSettingsStore,
) : ViewModel() {

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        orchestrator.activeConnections,
        orchestrator.activeSessionAuth,
        logger.entries,
        settingsStore.settingsFlow,
    ) { connections, auth, logs, settings ->
        DiagnosticsUiState(
            activeConnections = connections,
            sessionAuth = auth,
            logEntries = logs,
            settings = settings,
            detectedLanIps = discoverLocalLanIpv4Addresses(),
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

    fun updateLanTethering(transform: (LanTetheringSettings) -> LanTetheringSettings) {
        viewModelScope.launch {
            settingsStore.update { current ->
                current.copy(lanTethering = transform(current.lanTethering))
            }
        }
    }

    fun generateRandomLanPassword() {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        val rng = SecureRandom()
        val pwd = (1..16).map { alphabet[rng.nextInt(alphabet.length)] }.joinToString("")
        updateLanTethering { it.copy(password = pwd, requireAuth = true) }
    }

    private fun discoverLocalLanIpv4Addresses(): List<String> {
        return runCatching {
            val found = mutableListOf<String>()
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
            for (nif in Collections.list(interfaces)) {
                if (!nif.isUp || nif.isLoopback) continue
                if (nif.name.startsWith("tun") || nif.name.startsWith("ppp")) continue
                for (addr in Collections.list(nif.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                        found += "${addr.hostAddress} (${nif.name})"
                    }
                }
            }
            found
        }.getOrDefault(emptyList())
    }
}

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Connection Inspector", "Log Console", "LAN Tethering")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        )
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
                            tint = AetherPalette.TelemetryCyan,
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
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    )
                    if (state.activeConnections.isNotEmpty()) {
                        OutlinedButton(
                            onClick = viewModel::terminateAllSockets,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
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
                                        accentColor = if (conn.network == "UDP") AetherPalette.WarningAmber else AetherPalette.TelemetryCyan,
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
            leadingIcon = { Icon(Icons.Outlined.Terminal, contentDescription = null) },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AssistChip(
                onClick = {
                    val text = viewModel.buildExportLogPayload(severityFilter, keyword, redactSensitive)
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("AetherFlow Logs", text))
                },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, Modifier.size(16.dp)) },
                label = { Text("Copy") },
            )
            AssistChip(
                onClick = { exportLauncher.launch("aetherflow-diagnostics.log") },
                leadingIcon = { Icon(Icons.Outlined.SaveAlt, null, Modifier.size(16.dp)) },
                label = { Text("Export") },
            )
            AssistChip(
                onClick = viewModel::clearLogs,
                leadingIcon = { Icon(Icons.Outlined.DeleteSweep, null, Modifier.size(16.dp)) },
                label = { Text("Clear") },
            )
            FilterChip(
                selected = redactSensitive,
                onClick = {
                    val next = !redactSensitive
                    redactSensitive = next
                    viewModel.updateLogPreferences(redactByDefault = next)
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text(if (redactSensitive) "Redaction ON" else "Unredacted") },
            )
            listOf(LogSeverity.ERROR, LogSeverity.WARNING, LogSeverity.INFO, LogSeverity.DEBUG).forEach { sev ->
                FilterChip(
                    selected = severityFilter == sev,
                    onClick = {
                        severityFilter = sev
                        viewModel.updateLogPreferences(severity = sev)
                    },
                    label = { Text(sev.wireLevel.uppercase()) },
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
                            LogSeverity.DEBUG, LogSeverity.NONE -> AetherPalette.TelemetryCyan
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

@Composable
private fun LanTetheringTab(
    state: DiagnosticsUiState,
    viewModel: DiagnosticsViewModel,
) {
    val context = LocalContext.current
    val lan = state.settings.lanTethering
    var socksPortText by remember(lan.socksPort) { mutableStateOf(lan.socksPort.toString()) }
    var httpPortText by remember(lan.httpPort) { mutableStateOf(lan.httpPort.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Outlined.WifiTethering,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("LAN / Hotspot Proxy Share", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Share the encrypted VPN tunnel with laptops, consoles, or secondary devices over Wi-Fi Hotspot / USB Tethering",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = lan.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateLanTethering { it.copy(enabled = enabled) }
                        },
                    )
                }

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = socksPortText,
                        onValueChange = { raw ->
                            socksPortText = raw.filter(Char::isDigit)
                            socksPortText.toIntOrNull()?.takeIf { it in 1024..65535 }?.let { port ->
                                viewModel.updateLanTethering { it.copy(socksPort = port) }
                            }
                        },
                        label = { Text("SOCKS5 Port", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                        label = { Text("HTTP Port", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        textStyle = AetherMonoTypography.metricMedium,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Require LAN Client Credentials", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Prevents unauthorized peers on shared Wi-Fi networks from relaying traffic",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = lan.requireAuth,
                        onCheckedChange = { req -> viewModel.updateLanTethering { it.copy(requireAuth = req) } },
                    )
                }

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
                    AssistChip(
                        onClick = viewModel::generateRandomLanPassword,
                        leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = {
                            Text(
                                text = "Generate Strong 16-Char Password",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AetherPalette.TelemetryCyan.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val primaryIp = state.detectedLanIps.firstOrNull()?.substringBefore(" ") ?: "192.168.43.1"
            val summaryBlock = buildString {
                appendLine("Detected Device LAN Interfaces:")
                if (state.detectedLanIps.isEmpty()) {
                    appendLine("  • 192.168.43.1 (Hotspot Default)")
                } else {
                    state.detectedLanIps.forEach { appendLine("  • $it") }
                }
                appendLine()
                appendLine("SOCKS5 Proxy : $primaryIp:${lan.socksPort}")
                appendLine("HTTP Proxy   : $primaryIp:${lan.httpPort}")
                if (lan.requireAuth && lan.password.isNotBlank()) {
                    appendLine("Credentials  : ${lan.username} / ${lan.password}")
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
                    Text(
                        text = "Client Setup Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("LAN Proxy Config", summaryBlock))
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy", maxLines = 1, softWrap = false)
                    }
                }
                Text(
                    text = summaryBlock,
                    style = AetherMonoTypography.codeSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
