package com.ceoeg.aetherflow.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ceoeg.aetherflow.core.common.database.TunnelRepository
import com.ceoeg.aetherflow.core.common.design.AetherMonoTypography
import com.ceoeg.aetherflow.core.common.design.AetherPalette
import com.ceoeg.aetherflow.core.common.design.ProtocolBadge
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.GlobalAntiDpiDefaults
import com.ceoeg.aetherflow.core.common.model.ThemeMode
import com.ceoeg.aetherflow.core.common.model.TunBackendMode
import com.ceoeg.aetherflow.core.common.model.TunSettings
import com.ceoeg.aetherflow.core.common.security.BackupArchiveManager
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import com.ceoeg.aetherflow.core.common.security.VpnBackupSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val statusMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: EncryptedSettingsStore,
    private val repository: TunnelRepository,
    private val backupManager: BackupArchiveManager,
    private val logger: DiagnosticLogger,
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsStore.settingsFlow,
        messageFlow,
    ) { settings, msg ->
        SettingsUiState(settings = settings, statusMessage = msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsStore.update { it.copy(themeMode = mode) }
        }
    }

    fun updateTunSettings(transform: (TunSettings) -> TunSettings) {
        viewModelScope.launch {
            settingsStore.update { current ->
                current.copy(tunSettings = transform(current.tunSettings))
            }
        }
    }

    fun updateAntiDpiDefaults(transform: (GlobalAntiDpiDefaults) -> GlobalAntiDpiDefaults) {
        viewModelScope.launch {
            settingsStore.update { current ->
                current.copy(globalAntiDpi = transform(current.globalAntiDpi))
            }
        }
    }

    fun updateFailoverAndProbing(
        smartAutoSelect: Boolean? = null,
        timeoutThresholdMs: Long? = null,
        latencyUrl: String? = null,
    ) {
        viewModelScope.launch {
            settingsStore.update { current ->
                current.copy(
                    smartAutoSelectEnabled = smartAutoSelect ?: current.smartAutoSelectEnabled,
                    autoFailoverTimeoutThresholdMs = timeoutThresholdMs ?: current.autoFailoverTimeoutThresholdMs,
                    latencyTestUrl = latencyUrl ?: current.latencyTestUrl,
                )
            }
        }
    }

    fun exportBackupBytes(password: String, onReady: (ByteArray) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val snapshot = VpnBackupSnapshot(
                    settings = settingsStore.current(),
                    profiles = repository.getAllProfiles(),
                    subscriptions = repository.getAllSubscriptions(),
                    routingRules = repository.getAllRoutingRules(),
                )
                backupManager.exportEncryptedArchive(snapshot, password)
            }.onSuccess { bytes ->
                onReady(bytes)
                messageFlow.value = "Encrypted AES-256-GCM .vpnbak archive generated (${bytes.size} bytes)."
                logger.info("Backup", "Exported encrypted .vpnbak snapshot")
            }.onFailure { err ->
                messageFlow.value = "Backup export failed: ${err.message}"
            }
        }
    }

    fun importBackupBytes(bytes: ByteArray, password: String) {
        viewModelScope.launch {
            runCatching {
                val snapshot = backupManager.importEncryptedArchive(bytes, password)
                repository.restoreFullSnapshot(
                    profiles = snapshot.profiles,
                    subscriptions = snapshot.subscriptions,
                    rules = snapshot.routingRules,
                )
                settingsStore.update { snapshot.settings }
                snapshot
            }.onSuccess { snap ->
                messageFlow.value =
                    "Restored ${snap.profiles.size} profiles, ${snap.subscriptions.size} feeds, and ${snap.routingRules.size} rules."
                logger.info("Backup", "Restored .vpnbak archive successfully")
            }.onFailure { err ->
                messageFlow.value = "Restore failed (check password or file integrity): ${err.message}"
            }
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settings = state.settings
    val tun = settings.tunSettings
    val antiDpi = settings.globalAntiDpi

    var backupPassword by remember { mutableStateOf("") }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val bytes = pendingExportBytes
        if (uri != null && bytes != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            }
        }
        pendingExportBytes = null
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes != null) {
                viewModel.importBackupBytes(bytes, backupPassword)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        state.statusMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearMessage() },
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        // 1. Enterprise Appearance & Theme
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Enterprise Interface Theme", style = MaterialTheme.typography.titleMedium)
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }
        }

        // 2. Dual Native TUN Engine & Kernel Tuning
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
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Memory, contentDescription = null, tint = AetherPalette.TelemetryCyan)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Native TUN Stack & MTU Tuning",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ProtocolBadge(text = "MTU ${tun.mtu}", accentColor = AetherPalette.EmeraldSignal)
                }

                TunBackendMode.entries.forEach { backend ->
                    val selected = tun.backend == backend
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateTunSettings { it.copy(backend = backend) } },
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = backend.label, style = AetherMonoTypography.metricMedium)
                            Text(
                                text = backend.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Interface MTU (1280 – 9000)", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(1280, 1360, 1400, 1420, 1500, 9000).forEach { preset ->
                            AssistChip(
                                onClick = { viewModel.updateTunSettings { it.copy(mtu = preset) } },
                                label = { Text(preset.toString(), maxLines = 1) },
                            )
                        }
                    }
                    Slider(
                        value = tun.mtu.toFloat(),
                        onValueChange = { v ->
                            val snapped = ((v / 20f).roundToInt() * 20).coerceIn(1280, 9000)
                            viewModel.updateTunSettings { it.copy(mtu = snapped) }
                        },
                        valueRange = 1280f..9000f,
                    )
                }

                HorizontalDivider()

                SettingsSwitchRow(
                    title = "Hev UDP-in-TCP Encapsulation",
                    subtitle = "Wraps UDP datagrams inside SOCKS5 TCP streams when UDP relay is throttled",
                    checked = tun.hevUdpInTcp,
                    onCheckedChange = { v -> viewModel.updateTunSettings { it.copy(hevUdpInTcp = v) } },
                )
                SettingsSwitchRow(
                    title = "Kernel Multi-Queue TUN",
                    subtitle = "Enables multi-queue packet dispatch on supported Linux/Android kernels",
                    checked = tun.hevMultiQueue,
                    onCheckedChange = { v -> viewModel.updateTunSettings { it.copy(hevMultiQueue = v) } },
                )
                SettingsSwitchRow(
                    title = "Strict Kill-Switch Blocking",
                    subtitle = "Blocks non-VPN socket leaks during tunnel startup and handoff",
                    checked = tun.killSwitchBlocking,
                    onCheckedChange = { v -> viewModel.updateTunSettings { it.copy(killSwitchBlocking = v) } },
                )

                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_VPN_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "System Always-On VPN Settings",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // 3. Smart Auto-Select & Zero-Drop Failover
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bolt, contentDescription = null, tint = AetherPalette.WarningAmber)
                    Spacer(Modifier.width(8.dp))
                    Text("Intelligent Node Selection & Auto-Failover", style = MaterialTheme.typography.titleMedium)
                }

                SettingsSwitchRow(
                    title = "Smart Auto-Select Lowest Latency Endpoint",
                    subtitle = "Automatically connects to the fastest verified node and hot-swaps on timeout",
                    checked = settings.smartAutoSelectEnabled,
                    onCheckedChange = { v -> viewModel.updateFailoverAndProbing(smartAutoSelect = v) },
                )

                OutlinedTextField(
                    value = settings.latencyTestUrl,
                    onValueChange = { url -> viewModel.updateFailoverAndProbing(latencyUrl = url) },
                    label = { Text("HTTP-204 URL-Test Probe Endpoint") },
                    textStyle = AetherMonoTypography.codeSmall,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 4. Global Active Anti-DPI & Stealth Defaults
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = AetherPalette.EmeraldSignal)
                    Spacer(Modifier.width(8.dp))
                    Text("Active Anti-DPI & Stealth Arsenal", style = MaterialTheme.typography.titleMedium)
                }

                SettingsSwitchRow(
                    title = "Global TLS ClientHello Fragmentation",
                    subtitle = "Splits outbound TLS ClientHello records into randomized byte chunks",
                    checked = antiDpi.forceTlsFragment,
                    onCheckedChange = { v -> viewModel.updateAntiDpiDefaults { it.copy(forceTlsFragment = v) } },
                )
                if (antiDpi.forceTlsFragment) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = antiDpi.defaultFragmentLength,
                            onValueChange = { s ->
                                viewModel.updateAntiDpiDefaults { it.copy(defaultFragmentLength = s) }
                            },
                            label = { Text("Fragment Bytes") },
                            textStyle = AetherMonoTypography.metricMedium,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = antiDpi.defaultFragmentDelayMs,
                            onValueChange = { s ->
                                viewModel.updateAntiDpiDefaults { it.copy(defaultFragmentDelayMs = s) }
                            },
                            label = { Text("Split Interval (ms)") },
                            textStyle = AetherMonoTypography.metricMedium,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                HorizontalDivider()

                SettingsSwitchRow(
                    title = "Global UDP Noise Injection",
                    subtitle = "Injects randomized decoy UDP packets ahead of QUIC / WireGuard handshakes",
                    checked = antiDpi.forceUdpNoise,
                    onCheckedChange = { v -> viewModel.updateAntiDpiDefaults { it.copy(forceUdpNoise = v) } },
                )
                if (antiDpi.forceUdpNoise) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = antiDpi.defaultUdpNoisePacket,
                            onValueChange = { s ->
                                viewModel.updateAntiDpiDefaults { it.copy(defaultUdpNoisePacket = s) }
                            },
                            label = { Text("Noise Packet Size") },
                            textStyle = AetherMonoTypography.metricMedium,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = antiDpi.defaultUdpNoiseDelayMs,
                            onValueChange = { s ->
                                viewModel.updateAntiDpiDefaults { it.copy(defaultUdpNoiseDelayMs = s) }
                            },
                            label = { Text("Noise Delay (ms)") },
                            textStyle = AetherMonoTypography.metricMedium,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                HorizontalDivider()

                SettingsSwitchRow(
                    title = "Enable ECH (Encrypted Client Hello) by Default",
                    subtitle = "Encrypts outer SNI where supported by endpoint DNS HTTPS records",
                    checked = antiDpi.enableEchByDefault,
                    onCheckedChange = { v -> viewModel.updateAntiDpiDefaults { it.copy(enableEchByDefault = v) } },
                )
            }
        }

        // 5. Encrypted .vpnbak Backup & Restore
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AetherPalette.TelemetryCyan.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = AetherPalette.TelemetryCyan)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AES-256-GCM Encrypted Backup (.vpnbak)",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "PBKDF2-HMAC-SHA256 (210,000 iterations) portable snapshot of all profiles, subscriptions, rules, and settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OutlinedTextField(
                    value = backupPassword,
                    onValueChange = { backupPassword = it },
                    label = { Text("Archive Passphrase (min 6 characters)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            viewModel.exportBackupBytes(backupPassword) { bytes ->
                                pendingExportBytes = bytes
                                createBackupLauncher.launch("aetherflow-backup.vpnbak")
                            }
                        },
                        enabled = backupPassword.length >= 6,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Export .vpnbak",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = { openBackupLauncher.launch(arrayOf("*/*")) },
                        enabled = backupPassword.length >= 6,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Restore .vpnbak",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
