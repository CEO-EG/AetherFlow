package com.ceoeg.aetherflow.feature.subscriptions

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.ceoeg.aetherflow.core.common.design.formatBytesHuman
import com.ceoeg.aetherflow.core.common.model.SubscriptionFeed
import com.ceoeg.aetherflow.core.common.model.UserAgentPreset
import com.ceoeg.aetherflow.core.vpn.SubscriptionSyncManager
import com.ceoeg.aetherflow.core.wireguard.CloudflareWarpProvisioner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SubscriptionsUiState(
    val feeds: List<SubscriptionFeed> = emptyList(),
    val isBusy: Boolean = false,
    val statusNotice: String? = null,
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val repository: TunnelRepository,
    private val syncManager: SubscriptionSyncManager,
    private val warpProvisioner: CloudflareWarpProvisioner,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val notice = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        repository.subscriptionsFlow,
        busy,
        notice,
    ) { feeds, isBusy, msg ->
        SubscriptionsUiState(feeds = feeds, isBusy = isBusy, statusNotice = msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionsUiState())

    fun addAndSyncSubscription(
        name: String,
        url: String,
        userAgent: UserAgentPreset,
        intervalHours: Int,
        autoUpdate: Boolean,
    ) {
        viewModelScope.launch {
            busy.value = true
            val feed = SubscriptionFeed(
                name = name.ifBlank { "Enterprise Feed" },
                url = url.trim(),
                userAgent = userAgent,
                autoUpdateEnabled = autoUpdate,
                updateIntervalHours = intervalHours,
            )
            repository.upsertSubscription(feed)
            if (autoUpdate) syncManager.schedulePeriodicSync(intervalHours)
            val result = syncManager.syncSubscription(feed)
            busy.value = false
            notice.value = result.fold(
                onSuccess = { "Synced '${it.name}' (${it.profileCount} endpoints)" },
                onFailure = { "Saved '${feed.name}', initial sync failed: ${it.message}" },
            )
        }
    }

    fun syncSingle(feed: SubscriptionFeed) {
        viewModelScope.launch {
            busy.value = true
            val result = syncManager.syncSubscription(feed)
            busy.value = false
            notice.value = result.fold(
                onSuccess = { "Updated '${it.name}' (${it.profileCount} endpoints)" },
                onFailure = { "Sync error: ${it.message}" },
            )
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            busy.value = true
            val count = syncManager.syncAllAutoEnabled()
            busy.value = false
            notice.value = "Refreshed $count subscription feed(s)"
        }
    }

    fun deleteSubscription(id: String) {
        viewModelScope.launch {
            repository.deleteSubscriptionAndProfiles(id)
            notice.value = "Deleted subscription group and associated endpoints"
        }
    }

    fun enrollCloudflareWarp(twoHopWarpInWarp: Boolean) {
        viewModelScope.launch {
            busy.value = true
            if (twoHopWarpInWarp) {
                val (entry, exit) = warpProvisioner.enrollWarpInWarpPair()
                repository.upsertProfiles(listOf(entry, exit))
                notice.value = "Provisioned WARP-in-WARP 2-hop chain ('${exit.name}')"
            } else {
                val profile = warpProvisioner.enrollWarpProfile()
                repository.upsertProfile(profile)
                notice.value = "Provisioned '${profile.name}'"
            }
            busy.value = false
        }
    }
}

@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1-Tap Cloudflare WARP & WARP-in-WARP Provisioner Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AetherPalette.TelemetryCyan.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Layers, null, tint = AetherPalette.TelemetryCyan)
                    Text(
                        text = "CLOUDFLARE WARP & DETOUR ENROLLMENT",
                        style = MaterialTheme.typography.labelMedium,
                        color = AetherPalette.TelemetryCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "Generate fresh Curve25519 keys with AmneziaWG junk-packet DPI stealth—standalone or chained as a clean WARP-in-WARP exit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { viewModel.enrollCloudflareWarp(twoHopWarpInWarp = false) },
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "1-Tap WARP",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.enrollCloudflareWarp(twoHopWarpInWarp = true) },
                        enabled = !state.isBusy,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    ) {
                        Text(
                            text = "WARP-in-WARP",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Subscription Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FEEDS (${state.feeds.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = viewModel::syncAll,
                    enabled = !state.isBusy,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.CloudSync, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Sync All", maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Feed", maxLines = 1, softWrap = false)
                }
            }
        }

        state.statusNotice?.let {
            Text(it, style = AetherMonoTypography.codeSmall, color = AetherPalette.EmeraldSignal)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.feeds, key = { it.id }) { feed ->
                SubscriptionQuotaCard(
                    feed = feed,
                    onSync = { viewModel.syncSingle(feed) },
                    onDelete = { viewModel.deleteSubscription(feed.id) },
                )
            }
        }
    }

    if (showAddDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url, ua, hours, auto ->
                viewModel.addAndSyncSubscription(name, url, ua, hours, auto)
                showAddDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionQuotaCard(
    feed: SubscriptionFeed,
    onSync: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = feed.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ProtocolBadge("${feed.profileCount} NODES", accentColor = AetherPalette.EmeraldSignal)
                        ProtocolBadge("UA: ${feed.userAgent.label}", accentColor = AetherPalette.TelemetryCyan)
                    }
                }
                Row {
                    IconButton(onClick = onSync, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Sync subscription", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete subscription", modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (feed.totalQuotaBytes > 0L) {
                LinearProgressIndicator(
                    progress = { feed.quotaProgressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    color = if (feed.quotaProgressFraction > 0.85f) {
                        AetherPalette.WarningAmber
                    } else {
                        AetherPalette.EmeraldSignal
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Used: ${formatBytesHuman(feed.usedBytes)} / ${formatBytesHuman(feed.totalQuotaBytes)}",
                        style = AetherMonoTypography.codeSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Left: ${formatBytesHuman(feed.remainingBytes)}",
                        style = AetherMonoTypography.codeSmall,
                        color = AetherPalette.EmeraldSignal,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            if (feed.expireEpochSec > 0L) {
                Text(
                    text = "Expires: ${dateFmt.format(Date(feed.expireEpochSec * 1000L))}",
                    style = AetherMonoTypography.codeSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, UserAgentPreset, Int, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var ua by remember { mutableStateOf(UserAgentPreset.SING_BOX) }
    var hours by remember { mutableStateOf("12") }
    var autoSync by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Remote Subscription", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Subscription HTTPS URL") },
                    singleLine = true,
                    textStyle = AetherMonoTypography.codeSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("HTTP User-Agent Preset", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    UserAgentPreset.entries.forEach { preset ->
                        FilterChip(
                            selected = ua == preset,
                            onClick = { ua = preset },
                            label = { Text(preset.label, maxLines = 1) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Background WorkManager Sync",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = autoSync, onCheckedChange = { autoSync = it })
                }
                if (autoSync) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("Sync Interval (Hours)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = url.startsWith("http"),
                onClick = {
                    onConfirm(name, url, ua, hours.toIntOrNull() ?: 12, autoSync)
                },
            ) {
                Text("Save & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
