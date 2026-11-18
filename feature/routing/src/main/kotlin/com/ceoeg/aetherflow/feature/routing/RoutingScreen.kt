package com.ceoeg.aetherflow.feature.routing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.FilterCenterFocus
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Https
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material.icons.outlined.VpnLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
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
import com.ceoeg.aetherflow.core.common.design.AetherSwitch
import com.ceoeg.aetherflow.core.common.design.ProtocolBadge
import com.ceoeg.aetherflow.core.common.design.formatBytesHuman
import com.ceoeg.aetherflow.core.common.logging.DiagnosticLogger
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.DnsProtocol
import com.ceoeg.aetherflow.core.common.model.DnsSettings
import com.ceoeg.aetherflow.core.common.model.RoutingRule
import com.ceoeg.aetherflow.core.common.model.RuleAction
import com.ceoeg.aetherflow.core.common.model.RuleMatchType
import com.ceoeg.aetherflow.core.common.model.SplitTunnelMode
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import com.ceoeg.aetherflow.core.routing.AppCategory
import com.ceoeg.aetherflow.core.routing.GeoAssetStatus
import com.ceoeg.aetherflow.core.routing.GeoRuleSetManager
import com.ceoeg.aetherflow.core.routing.InstalledAppEntry
import com.ceoeg.aetherflow.core.routing.SplitTunnelAppManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutingUiState(
    val settings: AppSettings = AppSettings(),
    val rules: List<RoutingRule> = emptyList(),
    val installedApps: List<InstalledAppEntry> = emptyList(),
    val geoAssets: List<GeoAssetStatus> = emptyList(),
    val isUpdatingGeo: Boolean = false,
    val isLoadingApps: Boolean = false,
    val statusBanner: String? = null,
)

@HiltViewModel
class RoutingViewModel @Inject constructor(
    private val settingsStore: EncryptedSettingsStore,
    private val repository: TunnelRepository,
    private val appManager: SplitTunnelAppManager,
    private val geoManager: GeoRuleSetManager,
    private val logger: DiagnosticLogger,
) : ViewModel() {

    private val installedAppsFlow = MutableStateFlow<List<InstalledAppEntry>>(emptyList())
    private val updatingGeoFlow = MutableStateFlow(false)
    private val loadingAppsFlow = MutableStateFlow(false)
    private val bannerFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RoutingUiState> = combine(
        settingsStore.settingsFlow,
        repository.routingRulesFlow,
        installedAppsFlow,
        geoManager.assets,
        combine(updatingGeoFlow, loadingAppsFlow, bannerFlow) { g, a, b -> Triple(g, a, b) },
    ) { settings, rules, apps, geo, statusTriple ->
        RoutingUiState(
            settings = settings,
            rules = rules,
            installedApps = apps,
            geoAssets = geo,
            isUpdatingGeo = statusTriple.first,
            isLoadingApps = statusTriple.second,
            statusBanner = statusTriple.third,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoutingUiState(),
    )

    init {
        refreshApps(force = false)
        viewModelScope.launch {
            geoManager.refreshStatus()
        }
    }

    fun refreshApps(force: Boolean) {
        viewModelScope.launch {
            loadingAppsFlow.value = true
            installedAppsFlow.value = appManager.getInstalledApps(forceRefresh = force)
            loadingAppsFlow.value = false
        }
    }

    suspend fun loadIcon(packageName: String): ImageBitmap? = appManager.loadAppIcon(packageName)

    fun setSplitTunnelMode(mode: SplitTunnelMode) {
        viewModelScope.launch {
            settingsStore.update { it.copy(splitTunnelMode = mode) }
        }
    }

    fun togglePackageSelection(packageName: String) {
        viewModelScope.launch {
            settingsStore.update { current ->
                val updated = if (packageName in current.splitTunnelPackages) {
                    current.splitTunnelPackages - packageName
                } else {
                    current.splitTunnelPackages + packageName
                }
                current.copy(splitTunnelPackages = updated)
            }
        }
    }

    fun applyQuickBypassBanking() {
        viewModelScope.launch {
            val financePkgs = installedAppsFlow.value
                .filter { it.category == AppCategory.BANKING_FINANCE }
                .map { it.packageName }
                .toSet()
            settingsStore.update { current ->
                current.copy(
                    splitTunnelMode = SplitTunnelMode.BYPASS_SELECTED_APPS,
                    splitTunnelPackages = current.splitTunnelPackages + financePkgs,
                )
            }
            bannerFlow.value = if (financePkgs.isEmpty()) {
                "Split-tunnel set to Bypass Mode (0 banking apps detected on device)."
            } else {
                "Bypassed ${financePkgs.size} banking & financial apps."
            }
        }
    }

    fun clearSelectedPackages() {
        viewModelScope.launch {
            settingsStore.update { it.copy(splitTunnelPackages = emptySet()) }
        }
    }

    fun setBlockAdsAndMalware(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.update { it.copy(blockAdsAndMalware = enabled) }
        }
    }

    fun setRegionalBypass(code: String) {
        viewModelScope.launch {
            settingsStore.update { it.copy(regionalBypassCode = code) }
        }
    }

    fun updateGeoDatabases() {
        viewModelScope.launch {
            updatingGeoFlow.value = true
            bannerFlow.value = "Syncing GeoIP / GeoSite (.dat & .srs) databases..."
            val result = geoManager.updateAllAssets()
            updatingGeoFlow.value = false
            result.onSuccess { count ->
                bannerFlow.value = "Updated $count GeoIP/GeoSite assets successfully."
                logger.info("GeoRouter", "Updated $count GeoIP/GeoSite databases")
            }.onFailure { err ->
                bannerFlow.value = "Geo update failed: ${err.message}"
                logger.error("GeoRouter", "Geo update failed: ${err.message}")
            }
        }
    }

    fun addRoutingRule(
        name: String,
        matchType: RuleMatchType,
        value: String,
        action: RuleAction,
        priority: Int,
    ) {
        if (value.isBlank()) return
        viewModelScope.launch {
            repository.upsertRoutingRule(
                RoutingRule(
                    name = name.ifBlank { "${matchType.label}: ${value.trim()}" },
                    enabled = true,
                    matchType = matchType,
                    value = value.trim(),
                    action = action,
                    priority = priority,
                )
            )
        }
    }

    fun toggleRuleEnabled(rule: RoutingRule) {
        viewModelScope.launch {
            repository.upsertRoutingRule(rule.copy(enabled = !rule.enabled))
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteRoutingRule(ruleId)
        }
    }

    fun updateDnsSettings(transform: (DnsSettings) -> DnsSettings) {
        viewModelScope.launch {
            settingsStore.update { current ->
                current.copy(dnsSettings = transform(current.dnsSettings))
            }
        }
    }

    fun dismissBanner() {
        bannerFlow.value = null
    }
}

@Composable
fun RoutingScreen(
    viewModel: RoutingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Split Tunneling" to Icons.Outlined.Apps,
        "Geo & Rules" to Icons.Outlined.Public,
        "DNS & Leak Shield" to Icons.Outlined.Dns,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp,
        ) {
            tabs.forEachIndexed { index, (label, tabIcon) ->
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
                                text = label,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    },
                )
            }
        }

        state.statusBanner?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.dismissBanner() },
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        when (selectedTab) {
            0 -> SplitTunnelTab(state = state, viewModel = viewModel)
            1 -> GeoAndCustomRulesTab(state = state, viewModel = viewModel)
            2 -> DnsStudioTab(dns = state.settings.dnsSettings, onUpdate = viewModel::updateDnsSettings)
        }
    }
}

@Composable
private fun SplitTunnelTab(
    state: RoutingUiState,
    viewModel: RoutingViewModel,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AppCategory.ALL) }
    var showSystemApps by remember { mutableStateOf(false) }

    val selectedPackages = state.settings.splitTunnelPackages
    val filteredApps = remember(state.installedApps, searchQuery, selectedCategory, showSystemApps, selectedPackages) {
        state.installedApps
            .asSequence()
            .filter { showSystemApps || !it.isSystemApp || it.packageName in selectedPackages }
            .filter { selectedCategory == AppCategory.ALL || it.category == selectedCategory }
            .filter {
                searchQuery.isBlank() ||
                    it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<InstalledAppEntry> { it.packageName in selectedPackages }
                    .thenBy { it.label.lowercase() }
            )
            .toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
                            imageVector = Icons.Outlined.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Per-App Routing Policy",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    ProtocolBadge(
                        text = "${selectedPackages.size} APPS",
                        accentColor = AetherPalette.AetherCyan,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SplitTunnelMode.entries.forEach { mode ->
                        val modeIcon = when (mode) {
                            SplitTunnelMode.DISABLED -> Icons.Outlined.Public
                            SplitTunnelMode.TUNNEL_SELECTED_APPS -> Icons.Outlined.FilterCenterFocus
                            SplitTunnelMode.BYPASS_SELECTED_APPS -> Icons.AutoMirrored.Outlined.CallSplit
                        }
                        AetherFilterChip(
                            selected = state.settings.splitTunnelMode == mode,
                            onClick = { viewModel.setSplitTunnelMode(mode) },
                            label = mode.label,
                            icon = modeIcon,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = viewModel::applyQuickBypassBanking,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Bypass Banking", maxLines = 1, softWrap = false)
                    }
                    if (selectedPackages.isNotEmpty()) {
                        TextButton(onClick = viewModel::clearSelectedPackages) {
                            Icon(Icons.Outlined.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear (${selectedPackages.size})", maxLines = 1, softWrap = false)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.refreshApps(force = true) }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh installed apps",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search app name or package ID...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("System", style = MaterialTheme.typography.labelMedium)
                AetherSwitch(
                    checked = showSystemApps,
                    onCheckedChange = { showSystemApps = it },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppCategory.entries.forEach { category ->
                val catIcon = when (category) {
                    AppCategory.ALL -> Icons.Outlined.Apps
                    AppCategory.BROWSER -> Icons.Outlined.Language
                    AppCategory.MESSAGING -> Icons.Outlined.Forum
                    AppCategory.MEDIA -> Icons.Outlined.PlayCircle
                    AppCategory.BANKING_FINANCE -> Icons.Outlined.AccountBalance
                    AppCategory.SYSTEM_UTILITY -> Icons.Outlined.SettingsSuggest
                }
                AetherFilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = category.label,
                    icon = catIcon,
                )
            }
        }

        if (state.isLoadingApps && state.installedApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isSelected = app.packageName in selectedPackages
                    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, app.packageName) {
                        value = viewModel.loadIcon(app.packageName)
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.togglePackageSelection(app.packageName) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val bmp = iconBitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = app.label.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = app.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = app.packageName,
                                    style = AetherMonoTypography.codeSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                ProtocolBadge(text = app.category.label)
                            }
                            Spacer(Modifier.width(4.dp))
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.togglePackageSelection(app.packageName) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeoAndCustomRulesTab(
    state: RoutingUiState,
    viewModel: RoutingViewModel,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var regionalMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
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
                            Icon(
                                imageVector = Icons.Outlined.Public,
                                contentDescription = null,
                                tint = AetherPalette.AetherCyan,
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GeoIP & GeoSite Rule Sets",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Dual-format Xray (.dat) & Sing-box (.srs) routing assets",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = viewModel::updateGeoDatabases,
                            enabled = !state.isUpdatingGeo,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            if (state.isUpdatingGeo) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Sync", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }

                    state.geoAssets.forEach { asset ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = asset.fileName,
                                    style = AetherMonoTypography.metricMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${asset.engineFormat} • SHA256: ${asset.sha256Prefix}",
                                    style = AetherMonoTypography.codeSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            ProtocolBadge(
                                text = if (asset.installed) formatBytesHuman(asset.sizeBytes) else "FALLBACK",
                                accentColor = if (asset.installed) AetherPalette.EmeraldSignal else AetherPalette.WarningAmber,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    AetherOptionSwitchRow(
                        title = "Block Ads, Trackers & Malware",
                        subtitle = "Rejects geosite:category-ads-all at the core router layer",
                        checked = state.settings.blockAdsAndMalware,
                        onCheckedChange = viewModel::setBlockAdsAndMalware,
                        icon = Icons.Outlined.GppBad,
                        iconTint = AetherPalette.AlertCrimson,
                    )

                    val activeRegionLabel = GeoRuleSetManager.REGIONAL_PRESETS
                        .firstOrNull { it.first == state.settings.regionalBypassCode }?.second
                        ?: "Disabled (Global Routing)"

                    Box {
                        OutlinedButton(
                            onClick = { regionalMenuExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Regional Bypass: $activeRegionLabel",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        DropdownMenu(
                            expanded = regionalMenuExpanded,
                            onDismissRequest = { regionalMenuExpanded = false },
                        ) {
                            GeoRuleSetManager.REGIONAL_PRESETS.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setRegionalBypass(code)
                                        regionalMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Custom Routing Rules (${state.rules.size})",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Rule", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                }
            }
        }

        if (state.rules.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "No custom rules configured. Add Domain, IP CIDR, GeoSite, Port, or Package rules to steer traffic between Proxy, Direct Bypass, or Block.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(state.rules, key = { it.id }) { rule ->
                val actionColor = when (rule.action) {
                    RuleAction.PROXY -> AetherPalette.EmeraldSignal
                    RuleAction.DIRECT -> AetherPalette.AetherCyan
                    RuleAction.BLOCK -> AetherPalette.AlertCrimson
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                ProtocolBadge(text = rule.action.outboundTag.uppercase(), accentColor = actionColor)
                                ProtocolBadge(text = rule.matchType.label)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = rule.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = rule.value,
                                style = AetherMonoTypography.codeSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        AetherSwitch(
                            checked = rule.enabled,
                            onCheckedChange = { viewModel.toggleRuleEnabled(rule) },
                        )
                        IconButton(onClick = { viewModel.deleteRule(rule.id) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete rule",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRoutingRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, matchType, value, action, priority ->
                viewModel.addRoutingRule(name, matchType, value, action, priority)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddRoutingRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, RuleMatchType, String, RuleAction, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var matchType by remember { mutableStateOf(RuleMatchType.DOMAIN_SUFFIX) }
    var value by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(RuleAction.DIRECT) }
    var priorityText by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Routing Rule") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Label (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Match Condition", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RuleMatchType.entries.forEach { type ->
                        val typeIcon = when (type) {
                            RuleMatchType.DOMAIN_SUFFIX,
                            RuleMatchType.DOMAIN_KEYWORD,
                            RuleMatchType.DOMAIN_FULL -> Icons.Outlined.Language
                            RuleMatchType.GEOSITE,
                            RuleMatchType.GEOIP -> Icons.Outlined.Public
                            RuleMatchType.CIDR -> Icons.Outlined.Lan
                            RuleMatchType.PORT -> Icons.Outlined.Pin
                            RuleMatchType.PROTOCOL -> Icons.Outlined.Bolt
                            RuleMatchType.PROCESS_PACKAGE -> Icons.Outlined.Apps
                        }
                        AetherFilterChip(
                            selected = matchType == type,
                            onClick = { matchType = type },
                            label = type.label,
                            icon = typeIcon,
                        )
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Target Expression (e.g. openai.com, 10.0.0.0/8, 443)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Outbound Action", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RuleAction.entries.forEach { act ->
                        val actIcon = when (act) {
                            RuleAction.PROXY -> Icons.Outlined.Shield
                            RuleAction.DIRECT -> Icons.AutoMirrored.Outlined.CallSplit
                            RuleAction.BLOCK -> Icons.Outlined.Block
                        }
                        AetherFilterChip(
                            selected = action == act,
                            onClick = { action = act },
                            label = act.label,
                            icon = actIcon,
                        )
                    }
                }
                OutlinedTextField(
                    value = priorityText,
                    onValueChange = { priorityText = it.filter(Char::isDigit) },
                    label = { Text("Priority (lower runs first)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        matchType,
                        value,
                        action,
                        priorityText.toIntOrNull() ?: 100,
                    )
                },
                enabled = value.isNotBlank(),
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Rule")
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

@Composable
private fun DnsStudioTab(
    dns: DnsSettings,
    onUpdate: ((DnsSettings) -> DnsSettings) -> Unit,
) {
    val presets = listOf(
        "Cloudflare DoH" to (DnsProtocol.DOH to "https://1.1.1.1/dns-query"),
        "Quad9 DoH" to (DnsProtocol.DOH to "https://dns.quad9.net/dns-query"),
        "AdGuard DoQ" to (DnsProtocol.DOQ to "quic://dns.adguard-dns.com"),
        "Mullvad DoT" to (DnsProtocol.DOT to "tls://dns.mullvad.net"),
        "Google DoH" to (DnsProtocol.DOH to "https://dns.google/dns-query"),
    )

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Dns,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Encrypted Resolver Architecture", style = MaterialTheme.typography.titleMedium)
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { (label, pair) ->
                        AetherActionChip(
                            onClick = {
                                onUpdate {
                                    it.copy(remoteProtocol = pair.first, remoteDnsServer = pair.second)
                                }
                            },
                            label = label,
                            icon = Icons.Outlined.VerifiedUser,
                            iconTint = AetherPalette.EmeraldSignal,
                        )
                    }
                }

                Text("Remote Tunnel DNS Transport", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DnsProtocol.entries.forEach { proto ->
                        val protoIcon = when (proto) {
                            DnsProtocol.DOH -> Icons.Outlined.Https
                            DnsProtocol.DOT -> Icons.Outlined.Lock
                            DnsProtocol.DOQ -> Icons.Outlined.Bolt
                            DnsProtocol.DNSCRYPT -> Icons.Outlined.EnhancedEncryption
                            DnsProtocol.UDP_PLAIN -> Icons.Outlined.Dns
                        }
                        AetherFilterChip(
                            selected = dns.remoteProtocol == proto,
                            onClick = { onUpdate { it.copy(remoteProtocol = proto) } },
                            label = proto.label,
                            icon = protoIcon,
                        )
                    }
                }

                OutlinedTextField(
                    value = dns.remoteDnsServer,
                    onValueChange = { v -> onUpdate { it.copy(remoteDnsServer = v) } },
                    label = { Text("Remote Encrypted DNS Endpoint (Tunneled)") },
                    textStyle = AetherMonoTypography.metricMedium,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                AetherOptionSwitchRow(
                    title = "Split-DNS Domestic Resolution",
                    subtitle = "Resolves bypassed domestic domains directly without proxy latency",
                    checked = dns.splitDnsEnabled,
                    onCheckedChange = { enabled -> onUpdate { it.copy(splitDnsEnabled = enabled) } },
                    icon = Icons.AutoMirrored.Outlined.AltRoute,
                    iconTint = AetherPalette.AetherCyan,
                )

                if (dns.splitDnsEnabled) {
                    OutlinedTextField(
                        value = dns.directDnsServer,
                        onValueChange = { v -> onUpdate { it.copy(directDnsServer = v) } },
                        label = { Text("Direct Bypass DNS Endpoint") },
                        textStyle = AetherMonoTypography.metricMedium,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = AetherPalette.EmeraldSignal,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Zero-Leak Shield & FakeIP", style = MaterialTheme.typography.titleMedium)
                }

                AetherOptionSwitchRow(
                    title = "FakeIP Synthetic Pool (198.18.0.0/15)",
                    subtitle = "Eliminates pre-connection DNS RTT by mapping domains synthetically in-core",
                    checked = dns.fakeIpEnabled,
                    onCheckedChange = { v -> onUpdate { it.copy(fakeIpEnabled = v) } },
                    icon = Icons.Outlined.Hub,
                    iconTint = AetherPalette.AetherCyan,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AetherOptionSwitchRow(
                    title = "Strict IPv6 Leak Prevention",
                    subtitle = "Suppresses un-tunneled AAAA resolution and blocks raw IPv6 egress leaks",
                    checked = dns.blockIpv6Leak,
                    onCheckedChange = { v -> onUpdate { it.copy(blockIpv6Leak = v) } },
                    icon = Icons.Outlined.VpnLock,
                    iconTint = AetherPalette.EmeraldSignal,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AetherOptionSwitchRow(
                    title = "WebRTC STUN / TURN Leak Blocker",
                    subtitle = "Blocks UDP STUN discovery ports (3478, 5349, 19302) used to fingerprint real ISP IPs",
                    checked = dns.blockWebRtcStun,
                    onCheckedChange = { v -> onUpdate { it.copy(blockWebRtcStun = v) } },
                    icon = Icons.Outlined.VideocamOff,
                    iconTint = AetherPalette.WarningAmber,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AetherOptionSwitchRow(
                    title = "Block Direct QUIC (UDP/443) Bypass",
                    subtitle = "Forces browsers onto TLS/TCP when inspecting or fragmenting ClientHello",
                    checked = dns.blockQuicBypass,
                    onCheckedChange = { v -> onUpdate { it.copy(blockQuicBypass = v) } },
                    icon = Icons.Outlined.Speed,
                    iconTint = AetherPalette.TelemetryCyan,
                )
            }
        }
    }
}
