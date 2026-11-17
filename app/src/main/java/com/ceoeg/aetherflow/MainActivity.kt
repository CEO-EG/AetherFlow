package com.ceoeg.aetherflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ceoeg.aetherflow.core.common.database.TunnelRepository
import com.ceoeg.aetherflow.core.common.design.AetherFlowEnterpriseTheme
import com.ceoeg.aetherflow.core.common.design.AetherPalette
import com.ceoeg.aetherflow.core.common.design.ProtocolBadge
import com.ceoeg.aetherflow.core.common.model.AppSettings
import com.ceoeg.aetherflow.core.common.model.VpnConnectionState
import com.ceoeg.aetherflow.core.common.security.EncryptedSettingsStore
import com.ceoeg.aetherflow.core.parser.DeepLinkImportResult
import com.ceoeg.aetherflow.core.parser.UniversalConfigParser
import com.ceoeg.aetherflow.core.vpn.SubscriptionSyncManager
import com.ceoeg.aetherflow.core.vpn.UnifiedEngineOrchestrator
import com.ceoeg.aetherflow.feature.dashboard.DashboardScreen
import com.ceoeg.aetherflow.feature.diagnostics.DiagnosticsScreen
import com.ceoeg.aetherflow.feature.profiles.ProfilesScreen
import com.ceoeg.aetherflow.feature.routing.RoutingScreen
import com.ceoeg.aetherflow.feature.settings.SettingsScreen
import com.ceoeg.aetherflow.feature.subscriptions.SubscriptionsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AetherDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    DASHBOARD("Shield", "AetherFlow Telemetry", Icons.Outlined.Shield),
    PROFILES("Nodes", "Endpoint Profiles", Icons.Outlined.Dns),
    SUBSCRIPTIONS("Feeds", "Subscription & WARP Hub", Icons.Outlined.CloudSync),
    ROUTING("Routing", "Split-Tunnel & Encrypted DNS", Icons.Outlined.Route),
    DIAGNOSTICS("Inspect", "Socket & Log Diagnostics", Icons.Outlined.Analytics),
    SETTINGS("Config", "Core & Anti-DPI Engine", Icons.Outlined.Settings),
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsStore: EncryptedSettingsStore
    @Inject lateinit var orchestrator: UnifiedEngineOrchestrator
    @Inject lateinit var parser: UniversalConfigParser
    @Inject lateinit var repository: TunnelRepository
    @Inject lateinit var syncManager: SubscriptionSyncManager

    private val snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Optional telemetry notification consent */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPostNotificationsIfNeeded()
        handleIncomingIntent(intent)

        setContent {
            val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
            val vpnState by orchestrator.connectionState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                snackbarEvents.collect { msg ->
                    snackbarHostState.showSnackbar(msg)
                }
            }

            AetherFlowEnterpriseTheme(themeMode = settings.themeMode) {
                AetherAdaptiveRootScaffold(
                    vpnState = vpnState,
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleIncomingIntent(incoming: Intent?) {
        val dataUri = incoming?.data ?: return
        lifecycleScope.launch {
            val scheme = dataUri.scheme?.lowercase().orEmpty()
            if (scheme == "content" || scheme == "file") {
                val fileText = runCatching {
                    contentResolver.openInputStream(dataUri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (!fileText.isNullOrBlank()) {
                    val profiles = parser.importAny(fileText, hintName = dataUri.lastPathSegment)
                    if (profiles.isNotEmpty()) {
                        repository.upsertProfiles(profiles)
                        settingsStore.update { s ->
                            if (s.activeProfileId == null) s.copy(activeProfileId = profiles.first().id) else s
                        }
                        snackbarEvents.tryEmit("Imported ${profiles.size} profile(s) from file.")
                    }
                }
                return@launch
            }

            when (val result = parser.parseClientDeepLink(dataUri.toString())) {
                is DeepLinkImportResult.ProfilesImported -> {
                    repository.upsertProfiles(result.profiles)
                    settingsStore.update { s ->
                        if (s.activeProfileId == null) s.copy(activeProfileId = result.profiles.first().id) else s
                    }
                    snackbarEvents.tryEmit("Imported ${result.profiles.size} endpoint(s) via deep-link.")
                }
                is DeepLinkImportResult.SubscriptionDiscovered -> {
                    repository.upsertSubscription(result.feed)
                    val synced = syncManager.syncSubscription(result.feed)
                    synced.onSuccess { updated ->
                        snackbarEvents.tryEmit("Synced '${updated.name}' (${updated.profileCount} endpoints).")
                    }.onFailure {
                        snackbarEvents.tryEmit("Saved subscription '${result.feed.name}'.")
                    }
                }
                is DeepLinkImportResult.Unsupported -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AetherAdaptiveRootScaffold(
    vpnState: VpnConnectionState,
    snackbarHostState: SnackbarHostState,
) {
    var currentDestination by remember { mutableStateOf(AetherDestination.DASHBOARD) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useWidescreenRail = maxWidth >= 720.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentDestination.subtitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        val badgeColor = when (vpnState) {
                            is VpnConnectionState.Connected -> AetherPalette.EmeraldSignal
                            is VpnConnectionState.Connecting, is VpnConnectionState.Reconnecting -> AetherPalette.WarningAmber
                            is VpnConnectionState.Error -> AetherPalette.AlertCrimson
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        ProtocolBadge(
                            text = vpnState.statusLabel.uppercase(),
                            accentColor = badgeColor,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useWidescreenRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        AetherDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                alwaysShowLabel = true,
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.title,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 10.5.sp,
                                            letterSpacing = 0.sp,
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
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            if (useWidescreenRail) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight(),
                    ) {
                        AetherDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title) },
                                modifier = Modifier.focusable(),
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DestinationHostContent(
                            destination = currentDestination,
                            onNavigate = { currentDestination = it },
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    DestinationHostContent(
                        destination = currentDestination,
                        onNavigate = { currentDestination = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationHostContent(
    destination: AetherDestination,
    onNavigate: (AetherDestination) -> Unit,
) {
    when (destination) {
        AetherDestination.DASHBOARD -> DashboardScreen(
            onNavigateToProfiles = { onNavigate(AetherDestination.PROFILES) },
        )
        AetherDestination.PROFILES -> ProfilesScreen()
        AetherDestination.SUBSCRIPTIONS -> SubscriptionsScreen()
        AetherDestination.ROUTING -> RoutingScreen()
        AetherDestination.DIAGNOSTICS -> DiagnosticsScreen()
        AetherDestination.SETTINGS -> SettingsScreen()
    }
}
