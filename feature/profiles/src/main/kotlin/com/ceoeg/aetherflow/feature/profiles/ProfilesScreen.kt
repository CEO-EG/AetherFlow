package com.ceoeg.aetherflow.feature.profiles

import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ceoeg.aetherflow.core.common.database.TunnelRepository
import com.ceoeg.aetherflow.core.common.design.AetherActionChip
import com.ceoeg.aetherflow.core.common.design.AetherFilterChip
import com.ceoeg.aetherflow.core.common.design.AetherMonoTypography
import com.ceoeg.aetherflow.core.common.design.AetherOptionSwitchRow
import com.ceoeg.aetherflow.core.common.design.AetherPalette
import com.ceoeg.aetherflow.core.common.design.ProtocolBadge
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.ProtocolType
import com.ceoeg.aetherflow.core.common.model.SecurityMode
import com.ceoeg.aetherflow.core.common.model.SubscriptionFeed
import com.ceoeg.aetherflow.core.common.model.TransportType
import com.ceoeg.aetherflow.core.common.model.TunnelProfile
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import com.ceoeg.aetherflow.core.common.security.TlsCertificatePinResolver
import com.ceoeg.aetherflow.core.parser.ClashAndJsonBundleCodec
import com.ceoeg.aetherflow.core.parser.ConfigValidationResult
import com.ceoeg.aetherflow.core.parser.ShareUriCodec
import com.ceoeg.aetherflow.core.parser.UniversalConfigParser
import com.ceoeg.aetherflow.core.parser.WireGuardAndOpenVpnCodec
import com.ceoeg.aetherflow.core.vpn.SmartNodeSelector
import com.ceoeg.aetherflow.core.vpn.UnifiedEngineOrchestrator
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfilesUiState(
    val profiles: List<TunnelProfile> = emptyList(),
    val subscriptions: List<SubscriptionFeed> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val selectedSubscriptionFilter: String? = null,
    val protocolFilter: ProtocolType? = null,
    val searchQuery: String = "",
    val isTestingLatency: Boolean = false,
    val statusMessage: String? = null,
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repository: TunnelRepository,
    private val settingsStore: EncryptedSettingsStore,
    private val parser: UniversalConfigParser,
    private val smartSelector: SmartNodeSelector,
    private val orchestrator: UnifiedEngineOrchestrator,
) : ViewModel() {

    private val subFilter = MutableStateFlow<String?>(null)
    private val protoFilter = MutableStateFlow<ProtocolType?>(null)
    private val query = MutableStateFlow("")
    private val testing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfilesUiState> = combine(
        repository.profilesFlow,
        repository.subscriptionsFlow,
        settingsStore.settingsFlow,
        combine(subFilter, protoFilter, query) { s, p, q -> Triple(s, p, q) },
        combine(testing, message) { t, m -> t to m },
    ) { allProfiles, subs, settings, (subId, proto, q), (isTesting, msg) ->
        val filtered = allProfiles.filter { prof ->
            (subId == null || prof.subscriptionId == subId) &&
                (proto == null || prof.protocol == proto) &&
                (q.isBlank() ||
                    prof.name.contains(q, ignoreCase = true) ||
                    prof.serverAddress.contains(q, ignoreCase = true) ||
                    prof.protocol.displayName.contains(q, ignoreCase = true))
        }
        ProfilesUiState(
            profiles = filtered,
            subscriptions = subs,
            settings = settings,
            selectedSubscriptionFilter = subId,
            protocolFilter = proto,
            searchQuery = q,
            isTestingLatency = isTesting,
            statusMessage = msg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    fun selectSubscriptionTab(subscriptionId: String?) {
        subFilter.value = subscriptionId
    }

    fun selectProtocolFilter(protocol: ProtocolType?) {
        protoFilter.value = if (protoFilter.value == protocol) null else protocol
    }

    fun updateSearch(text: String) {
        query.value = text
    }

    fun setSmartAutoSelect(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.update {
                it.copy(
                    smartAutoSelectEnabled = enabled,
                    activeSubscriptionGroupId = subFilter.value,
                )
            }
        }
    }

    fun activateProfile(profile: TunnelProfile, connectImmediately: Boolean = false) {
        viewModelScope.launch {
            settingsStore.update {
                it.copy(activeProfileId = profile.id, smartAutoSelectEnabled = false)
            }
            if (connectImmediately) {
                orchestrator.requestConnectFromUi(profile.id)
            }
        }
    }

    fun saveProfile(profile: TunnelProfile) {
        viewModelScope.launch {
            repository.upsertProfile(profile)
            if (uiState.value.settings.activeProfileId == null) {
                settingsStore.update { it.copy(activeProfileId = profile.id) }
            }
            message.value = "Saved endpoint '${profile.name}'"
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            repository.deleteProfile(id)
        }
    }

    fun importRawPayload(raw: String, sourceLabel: String) {
        viewModelScope.launch {
            val imported = parser.importAny(raw, subscriptionId = subFilter.value)
            if (imported.isNotEmpty()) {
                repository.upsertProfiles(imported)
                if (uiState.value.settings.activeProfileId == null) {
                    settingsStore.update { it.copy(activeProfileId = imported.first().id) }
                }
                message.value = "Imported ${imported.size} endpoint(s) from $sourceLabel"
            } else {
                message.value = "No valid endpoints recognized in $sourceLabel"
            }
        }
    }

    fun testAllVisibleLatencies() {
        viewModelScope.launch {
            val targets = uiState.value.profiles
            if (targets.isEmpty() || testing.value) return@launch
            testing.value = true
            val probed = smartSelector.probeAllConcurrently(targets, uiState.value.settings)
            repository.upsertProfiles(probed)
            testing.value = false
            val reachable = probed.count { it.urlTestDelayMs > 0L || it.tcpPingMs > 0L }
            message.value = "Probed ${probed.size} nodes ($reachable reachable)"
        }
    }

    fun deduplicateEndpoints() {
        viewModelScope.launch {
            val removed = repository.deduplicateProfiles()
            message.value = "Removed $removed duplicate endpoint(s)"
        }
    }

    fun purgeUnreachable() {
        viewModelScope.launch {
            val purged = repository.purgeUnreachableProfiles()
            message.value = "Purged $purged unreachable endpoint(s)"
        }
    }

    fun validateRawConfig(content: String): ConfigValidationResult = parser.validateRawConfig(content)

    fun exportAllAsClashYaml(): String = ClashAndJsonBundleCodec.exportToClashYaml(uiState.value.profiles)

    fun exportProfileText(profile: TunnelProfile): String {
        return when (profile.protocol) {
            ProtocolType.WIREGUARD, ProtocolType.AMNEZIA_WG ->
                WireGuardAndOpenVpnCodec.serializeWireGuardConf(profile)
            ProtocolType.OPENVPN -> WireGuardAndOpenVpnCodec.serializeOpenVpn(profile)
            else -> ShareUriCodec.serializeToUri(profile)
        }
    }
}

@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editingProfile by remember { mutableStateOf<TunnelProfile?>(null) }
    var showRawImportDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (!text.isNullOrBlank()) {
                viewModel.importRawPayload(text, "Local File")
            }
        }
    }

    val qrImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val decodedQr = decodeQrFromUri(context, uri)
            if (!decodedQr.isNullOrBlank()) {
                viewModel.importRawPayload(decodedQr, "QR Image")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Subscription Group Tabs with Icons
        val tabs = listOf<Pair<String?, String>>(null to "All Nodes") +
            state.subscriptions.map { it.id to it.name }
        val selectedIndex = tabs.indexOfFirst { it.first == state.selectedSubscriptionFilter }
            .coerceAtLeast(0)

        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { idx, (subId, title) ->
                val isTabSelected = idx == selectedIndex
                Tab(
                    selected = isTabSelected,
                    onClick = { viewModel.selectSubscriptionTab(subId) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = if (subId == null) Icons.Outlined.Dns else Icons.Outlined.CloudQueue,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = title,
                                fontWeight = if (isTabSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Full-width Search Box
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearch,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                placeholder = {
                    Text(
                        text = "Search host, tag, or protocol...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )

            // Quick Import & Creation Toolbar (All with Unified High-Contrast Icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AetherActionChip(
                    onClick = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clipText = cb?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                        if (!clipText.isNullOrBlank()) {
                            viewModel.importRawPayload(clipText, "Clipboard")
                        } else {
                            showRawImportDialog = true
                        }
                    },
                    label = "Paste URI",
                    icon = Icons.Outlined.ContentPaste,
                    iconTint = AetherPalette.AetherCyan,
                )
                AetherActionChip(
                    onClick = { qrImageLauncher.launch("image/*") },
                    label = "Scan QR",
                    icon = Icons.Outlined.QrCodeScanner,
                    iconTint = AetherPalette.EmeraldSignal,
                )
                AetherActionChip(
                    onClick = { filePickerLauncher.launch("*/*") },
                    label = "Import File",
                    icon = Icons.Outlined.FileOpen,
                    iconTint = AetherPalette.TelemetryCyan,
                )
                AetherActionChip(
                    onClick = { editingProfile = TunnelProfile(name = "New Endpoint") },
                    label = "New Node",
                    icon = Icons.Outlined.Add,
                    iconTint = AetherPalette.EmeraldBright,
                )
            }

            // Smart Auto-Select + Batch Maintenance Row (Every chip has a crisp icon & cohesive border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AetherFilterChip(
                    selected = state.settings.smartAutoSelectEnabled,
                    onClick = { viewModel.setSmartAutoSelect(!state.settings.smartAutoSelectEnabled) },
                    label = "Smart Auto-Select",
                    icon = Icons.Outlined.AutoAwesome,
                    accentColor = AetherPalette.WarningAmber,
                )
                AetherActionChip(
                    onClick = viewModel::testAllVisibleLatencies,
                    label = if (state.isTestingLatency) "Probing..." else "Test Latencies",
                    icon = Icons.Outlined.Speed,
                    iconTint = AetherPalette.EmeraldSignal,
                )
                AetherActionChip(
                    onClick = viewModel::deduplicateEndpoints,
                    label = "Deduplicate",
                    icon = Icons.Outlined.FilterNone,
                    iconTint = AetherPalette.TelemetryCyan,
                )
                AetherActionChip(
                    onClick = viewModel::purgeUnreachable,
                    label = "Purge Dead",
                    icon = Icons.Outlined.DeleteSweep,
                    iconTint = AetherPalette.WarningAmber,
                )
                AetherActionChip(
                    onClick = {
                        val yaml = viewModel.exportAllAsClashYaml()
                        copyToClipboard(context, "Clash YAML", yaml)
                    },
                    label = "Copy Clash YAML",
                    icon = Icons.Outlined.Code,
                    iconTint = AetherPalette.AetherCyan,
                )
            }

            // Protocol Quick Filters with Distinct Protocol Icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    ProtocolType.VLESS to Icons.Outlined.Shield,
                    ProtocolType.HYSTERIA2 to Icons.Outlined.Bolt,
                    ProtocolType.TUIC_V5 to Icons.Outlined.Speed,
                    ProtocolType.ANYTLS_V2 to Icons.Outlined.Lock,
                    ProtocolType.AMNEZIA_WG to Icons.Outlined.VpnKey,
                    ProtocolType.SHADOWSOCKS to Icons.Outlined.EnhancedEncryption,
                    ProtocolType.TROJAN to Icons.Outlined.Security,
                    ProtocolType.SSH to Icons.Outlined.Terminal,
                ).forEach { (proto, protoIcon) ->
                    AetherFilterChip(
                        selected = state.protocolFilter == proto,
                        onClick = { viewModel.selectProtocolFilter(proto) },
                        label = proto.badgeLabel,
                        icon = protoIcon,
                    )
                }
            }

            state.statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = AetherMonoTypography.codeSmall,
                    color = AetherPalette.EmeraldSignal,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.profiles, key = { it.id }) { profile ->
                    val isSelected = state.settings.activeProfileId == profile.id &&
                        !state.settings.smartAutoSelectEnabled
                    ProfileItemCard(
                        profile = profile,
                        isSelected = isSelected,
                        allProfiles = state.profiles,
                        onSelect = { viewModel.activateProfile(profile, connectImmediately = false) },
                        onEdit = { editingProfile = profile },
                        onCopy = {
                            copyToClipboard(context, profile.name, viewModel.exportProfileText(profile))
                        },
                        onDelete = { viewModel.deleteProfile(profile.id) },
                    )
                }
            }
        }
    }

    if (showRawImportDialog) {
        var rawText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRawImportDialog = false },
            title = { Text("Import URI / YAML / JSON / .conf") },
            text = {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    textStyle = AetherMonoTypography.codeSmall,
                    placeholder = { Text("vless://... | hysteria2://... | proxies: ... | [Interface]") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importRawPayload(rawText, "Manual Paste")
                    showRawImportDialog = false
                }) {
                    Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRawImportDialog = false }) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel")
                }
            },
        )
    }

    editingProfile?.let { current ->
        ProfileDualModeEditorDialog(
            initialProfile = current,
            availableDetourProfiles = state.profiles.filter { it.id != current.id },
            onValidateRaw = viewModel::validateRawConfig,
            onDismiss = { editingProfile = null },
            onSave = { updated ->
                viewModel.saveProfile(updated)
                editingProfile = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileItemCard(
    profile: TunnelProfile,
    isSelected: Boolean,
    allProfiles: List<TunnelProfile>,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val detourName = profile.detourProfileId?.let { id -> allProfiles.firstOrNull { it.id == id }?.name }
    val rtt = if (profile.urlTestDelayMs > 0L) profile.urlTestDelayMs else profile.tcpPingMs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            width = if (isSelected) 1.8.dp else 1.dp,
            color = if (isSelected) AetherPalette.EmeraldSignal else MaterialTheme.colorScheme.outline,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (rtt > 0L) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$rtt ms",
                        style = AetherMonoTypography.metricMedium,
                        color = if (rtt < 180L) AetherPalette.EmeraldSignal else AetherPalette.WarningAmber,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Text(
                text = "${profile.serverAddress}:${profile.serverPort}",
                style = AetherMonoTypography.codeSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ProtocolBadge(profile.protocol.badgeLabel, accentColor = AetherPalette.EmeraldSignal)
                    ProtocolBadge(profile.resolvedEngine().displayName, accentColor = AetherPalette.AetherCyan)
                    if (profile.tlsReality.mldsa65Verify.isNotBlank()) {
                        ProtocolBadge("PQ-MLDSA65", accentColor = AetherPalette.WarningAmber)
                    }
                    if (profile.tlsReality.security == SecurityMode.TLS &&
                        (profile.tlsReality.allowInsecure || profile.tlsReality.pinnedPeerCertSha256.isNotBlank())
                    ) {
                        ProtocolBadge("SNI-PIN", accentColor = AetherPalette.WarningAmber)
                    }
                    if (detourName != null) {
                        ProtocolBadge("→ $detourName", accentColor = AetherPalette.WarningAmber)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Export URI/Config",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Endpoint",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Endpoint",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDualModeEditorDialog(
    initialProfile: TunnelProfile,
    availableDetourProfiles: List<TunnelProfile>,
    onValidateRaw: (String) -> ConfigValidationResult,
    onDismiss: () -> Unit,
    onSave: (TunnelProfile) -> Unit,
) {
    var editorTab by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(initialProfile.name) }
    var protocol by remember { mutableStateOf(initialProfile.protocol) }
    var host by remember { mutableStateOf(initialProfile.serverAddress) }
    var port by remember { mutableStateOf(initialProfile.serverPort.toString()) }
    var uuidOrPass by remember {
        mutableStateOf(
            when (initialProfile.protocol) {
                ProtocolType.VLESS, ProtocolType.VMESS, ProtocolType.TUIC_V5, ProtocolType.JUICITY ->
                    initialProfile.uuid
                ProtocolType.WIREGUARD, ProtocolType.AMNEZIA_WG ->
                    initialProfile.wireGuard.privateKey
                else -> initialProfile.password
            }
        )
    }
    var sni by remember { mutableStateOf(initialProfile.tlsReality.sni) }
    var security by remember { mutableStateOf(initialProfile.tlsReality.security) }
    var allowInsecure by remember { mutableStateOf(initialProfile.tlsReality.allowInsecure) }
    var pinnedCertSha256 by remember { mutableStateOf(initialProfile.tlsReality.pinnedPeerCertSha256) }
    var verifyCertByName by remember { mutableStateOf(initialProfile.tlsReality.verifyPeerCertByName) }
    var isFetchingPin by remember { mutableStateOf(false) }
    var pinStatusMsg by remember { mutableStateOf<String?>(null) }
    val dialogScope = rememberCoroutineScope()
    var transport by remember { mutableStateOf(initialProfile.transport.type) }
    var path by remember { mutableStateOf(initialProfile.transport.path) }
    var realityPubKey by remember { mutableStateOf(initialProfile.tlsReality.realityPublicKey) }
    var mldsa65Verify by remember { mutableStateOf(initialProfile.tlsReality.mldsa65Verify) }
    var awgJc by remember { mutableStateOf(initialProfile.wireGuard.jc.toString()) }
    var wgPeerPubKey by remember { mutableStateOf(initialProfile.wireGuard.peerPublicKey) }
    var tlsFragment by remember { mutableStateOf(initialProfile.antiDpi.tlsFragmentEnabled) }
    var detourId by remember { mutableStateOf(initialProfile.detourProfileId) }

    var rawConfigText by remember {
        mutableStateOf(
            initialProfile.rawCustomConfig.ifBlank {
                ShareUriCodec.serializeToUri(initialProfile)
            }
        )
    }
    val validation = remember(rawConfigText) { onValidateRaw(rawConfigText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Endpoint Studio", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AetherFilterChip(
                        selected = editorTab == 0,
                        onClick = { editorTab = 0 },
                        label = "Structured Form",
                        icon = Icons.Outlined.Tune,
                    )
                    AetherFilterChip(
                        selected = editorTab == 1,
                        onClick = { editorTab = 1 },
                        label = "Raw JSON / YAML",
                        icon = Icons.Outlined.Code,
                    )
                }
            }
        },
        text = {
            if (editorTab == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Protocol Selector Dropdown
                    var protoMenuOpen by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { protoMenuOpen = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.FilterList, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Protocol: ${protocol.displayName}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        DropdownMenu(expanded = protoMenuOpen, onDismissRequest = { protoMenuOpen = false }) {
                            ProtocolType.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.displayName) },
                                    onClick = {
                                        protocol = item
                                        protoMenuOpen = false
                                    },
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Server Host / IP") },
                            singleLine = true,
                            modifier = Modifier.weight(2f),
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    OutlinedTextField(
                        value = uuidOrPass,
                        onValueChange = { uuidOrPass = it },
                        label = {
                            Text(
                                when (protocol) {
                                    ProtocolType.VLESS, ProtocolType.VMESS, ProtocolType.TUIC_V5 -> "UUID"
                                    ProtocolType.WIREGUARD, ProtocolType.AMNEZIA_WG -> "Client PrivateKey"
                                    else -> "Password / Key"
                                }
                            )
                        },
                        singleLine = true,
                        textStyle = AetherMonoTypography.codeSmall,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (protocol == ProtocolType.WIREGUARD || protocol == ProtocolType.AMNEZIA_WG) {
                        OutlinedTextField(
                            value = wgPeerPubKey,
                            onValueChange = { wgPeerPubKey = it },
                            label = { Text("Peer PublicKey") },
                            singleLine = true,
                            textStyle = AetherMonoTypography.codeSmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = awgJc,
                            onValueChange = { awgJc = it },
                            label = { Text("AmneziaWG Junk Packet Count (Jc)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        OutlinedTextField(
                            value = sni,
                            onValueChange = { sni = it },
                            label = { Text("SNI / ServerName") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SecurityMode.entries.forEach { sec ->
                                val secIcon = when (sec) {
                                    SecurityMode.NONE -> Icons.Outlined.LockOpen
                                    SecurityMode.TLS -> Icons.Outlined.Lock
                                    SecurityMode.REALITY -> Icons.Outlined.VerifiedUser
                                }
                                AetherFilterChip(
                                    selected = security == sec,
                                    onClick = { security = sec },
                                    label = sec.wireValue.uppercase(),
                                    icon = secIcon,
                                )
                            }
                        }

                        if (security == SecurityMode.TLS) {
                            AetherOptionSwitchRow(
                                title = "Allow Custom SNI / Self-Signed Cert",
                                subtitle = "Auto-pins server SHA-256 (pcs) & vcn on Xray v26+",
                                checked = allowInsecure,
                                onCheckedChange = { allowInsecure = it },
                                icon = Icons.Outlined.VerifiedUser,
                                iconTint = AetherPalette.WarningAmber,
                            )

                            OutlinedTextField(
                                value = verifyCertByName,
                                onValueChange = { verifyCertByName = it },
                                label = { Text("Verify Cert Hostname (vcn)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text(host.ifBlank { "e.g. vless-eg.duckdns.org" }, maxLines = 1) },
                                singleLine = true,
                                textStyle = AetherMonoTypography.codeSmall,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            OutlinedTextField(
                                value = pinnedCertSha256,
                                onValueChange = { pinnedCertSha256 = it },
                                label = { Text("Pinned Server Cert SHA-256 (pcs)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                placeholder = { Text("Auto-resolved on connect or tap Fetch below", maxLines = 1) },
                                singleLine = true,
                                textStyle = AetherMonoTypography.codeSmall,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            OutlinedButton(
                                enabled = !isFetchingPin && host.isNotBlank(),
                                onClick = {
                                    isFetchingPin = true
                                    pinStatusMsg = null
                                    dialogScope.launch {
                                        val res = TlsCertificatePinResolver.fetchServerCertPin(
                                            host = host,
                                            port = port.toIntOrNull() ?: 443,
                                            sni = sni,
                                        )
                                        isFetchingPin = false
                                        res.onSuccess { info ->
                                            pinnedCertSha256 = info.sha256Hex
                                            if (info.isCaCert && verifyCertByName.isBlank() && info.certNames.isNotEmpty()) {
                                                verifyCertByName = info.certNames.joinToString(",")
                                            }
                                            pinStatusMsg = "Pinned SHA-256: ${info.sha256Hex.take(16)}..."
                                        }.onFailure { err ->
                                            pinStatusMsg = "Probe failed: ${err.message}"
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Outlined.Fingerprint, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isFetchingPin) "Probing TLS Handshake..." else "Fetch Server Cert SHA-256",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            pinStatusMsg?.let { status ->
                                Text(
                                    text = status,
                                    style = AetherMonoTypography.codeSmall,
                                    color = if (status.startsWith("Pinned")) {
                                        AetherPalette.EmeraldSignal
                                    } else {
                                        AetherPalette.AlertCrimson
                                    },
                                )
                            }
                        }

                        if (security == SecurityMode.REALITY) {
                            OutlinedTextField(
                                value = realityPubKey,
                                onValueChange = { realityPubKey = it },
                                label = { Text("REALITY X25519 PublicKey (pbk)") },
                                singleLine = true,
                                textStyle = AetherMonoTypography.codeSmall,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = mldsa65Verify,
                                onValueChange = { mldsa65Verify = it },
                                label = { Text("Post-Quantum mldsa65Verify (pqv)") },
                                singleLine = true,
                                textStyle = AetherMonoTypography.codeSmall,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            TransportType.entries.forEach { tr ->
                                AetherFilterChip(
                                    selected = transport == tr,
                                    onClick = { transport = tr },
                                    label = tr.label,
                                    icon = Icons.Outlined.SwapHoriz,
                                )
                            }
                        }

                        if (transport != TransportType.RAW_TCP) {
                            OutlinedTextField(
                                value = path,
                                onValueChange = { path = it },
                                label = { Text("Path / gRPC ServiceName") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    AetherOptionSwitchRow(
                        title = "TLS ClientHello Fragmentation",
                        subtitle = "Splits ClientHello records to defeat SNI DPI",
                        checked = tlsFragment,
                        onCheckedChange = { tlsFragment = it },
                        icon = Icons.Outlined.ContentCut,
                        iconTint = AetherPalette.EmeraldSignal,
                    )

                    // Visual Outbound Proxy Chaining Selector (Entry Node -> Exit / WARP Detour)
                    var chainMenuOpen by remember { mutableStateOf(false) }
                    val selectedDetourName = detourId?.let { id ->
                        availableDetourProfiles.firstOrNull { it.id == id }?.name
                    } ?: "Direct Exit (Single-Hop)"
                    OutlinedButton(
                        onClick = { chainMenuOpen = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Exit Chain: $selectedDetourName",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        DropdownMenu(expanded = chainMenuOpen, onDismissRequest = { chainMenuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Direct Exit (Single-Hop)") },
                                onClick = {
                                    detourId = null
                                    chainMenuOpen = false
                                },
                            )
                            availableDetourProfiles.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text("Chain via → ${candidate.name}") },
                                    onClick = {
                                        detourId = candidate.id
                                        chainMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = rawConfigText,
                        onValueChange = { rawConfigText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp),
                        textStyle = AetherMonoTypography.codeSmall,
                        visualTransformation = ConfigSyntaxHighlightTransformation(),
                    )
                    Text(
                        text = "${validation.detectedFormat}: ${validation.summaryMessage}",
                        style = AetherMonoTypography.codeSmall,
                        color = if (validation.isValid) AetherPalette.EmeraldSignal else AetherPalette.AlertCrimson,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = editorTab == 0 || validation.isValid,
                onClick = {
                    if (editorTab == 1) {
                        onSave(
                            initialProfile.copy(
                                name = name,
                                protocol = ProtocolType.CUSTOM_JSON,
                                rawCustomConfig = rawConfigText,
                            )
                        )
                    } else {
                        val isUuidProto = protocol in setOf(
                            ProtocolType.VLESS,
                            ProtocolType.VMESS,
                            ProtocolType.TUIC_V5,
                            ProtocolType.JUICITY,
                        )
                        val isWg = protocol == ProtocolType.WIREGUARD || protocol == ProtocolType.AMNEZIA_WG
                        onSave(
                            initialProfile.copy(
                                name = name.ifBlank { "$host:$port" },
                                protocol = protocol,
                                serverAddress = host.trim(),
                                serverPort = port.toIntOrNull() ?: 443,
                                uuid = if (isUuidProto) uuidOrPass.trim() else initialProfile.uuid,
                                password = if (!isUuidProto && !isWg) uuidOrPass.trim() else initialProfile.password,
                                transport = initialProfile.transport.copy(
                                    type = transport,
                                    path = path,
                                    serviceName = if (transport == TransportType.GRPC) path else initialProfile.transport.serviceName,
                                ),
                                tlsReality = initialProfile.tlsReality.copy(
                                    security = security,
                                    sni = sni.trim(),
                                    allowInsecure = allowInsecure,
                                    realityPublicKey = realityPubKey.trim(),
                                    mldsa65Verify = mldsa65Verify.trim(),
                                    pinnedPeerCertSha256 = pinnedCertSha256.trim(),
                                    verifyPeerCertByName = verifyCertByName.trim(),
                                ),
                                wireGuard = initialProfile.wireGuard.copy(
                                    privateKey = if (isWg) uuidOrPass.trim() else initialProfile.wireGuard.privateKey,
                                    peerPublicKey = wgPeerPubKey.trim(),
                                    jc = awgJc.toIntOrNull() ?: 0,
                                    jmin = if ((awgJc.toIntOrNull() ?: 0) > 0) 40 else 0,
                                    jmax = if ((awgJc.toIntOrNull() ?: 0) > 0) 700 else 0,
                                ),
                                antiDpi = initialProfile.antiDpi.copy(tlsFragmentEnabled = tlsFragment),
                                detourProfileId = detourId,
                            )
                        )
                    }
                },
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Endpoint")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancel")
            }
        },
    )
}

/** Syntax highlighter for JSON / YAML / WireGuard `.conf` keys, strings, and numbers. */
private class ConfigSyntaxHighlightTransformation : VisualTransformation {
    private val keyRegex = Regex(""""([^"]+)"\s*:|^[\s-]*([A-Za-z0-9_-]+)\s*[:=]""", RegexOption.MULTILINE)
    private val boolNumRegex = Regex("""\b(true|false|null|\d+)\b""")

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val styled = buildAnnotatedString {
            append(raw)
            boolNumRegex.findAll(raw).forEach { m ->
                addStyle(SpanStyle(color = Color(0xFFF59E0B)), m.range.first, m.range.last + 1)
            }
            keyRegex.findAll(raw).forEach { m ->
                addStyle(
                    SpanStyle(color = Color(0xFF22D3EE), fontWeight = FontWeight.SemiBold),
                    m.range.first,
                    m.range.last + 1,
                )
            }
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

private fun copyToClipboard(context: Context, label: String, content: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cb?.setPrimaryClip(android.content.ClipData.newPlainText(label, content))
}

private fun decodeQrFromUri(context: Context, uri: android.net.Uri): String? {
    return runCatching {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val source = RGBLuminanceSource(w, h, pixels)
        val binary = BinaryBitmap(HybridBinarizer(source))
        MultiFormatReader().decode(binary).text
    }.getOrNull()
}
