package com.ceoeg.aetherflow

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ceoeg.aetherflow.core.common.database.TunnelRepository
import com.ceoeg.aetherflow.core.common.design.AetherBrandInfo
import com.ceoeg.aetherflow.core.common.design.AetherFlowEnterpriseTheme
import com.ceoeg.aetherflow.core.common.design.AetherMonoTypography
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

enum class AetherDestination(
    val title: String,
    val subtitle: String,
    val outlinedIcon: ImageVector,
    val selectedIcon: ImageVector,
) {
    DASHBOARD("Shield", "AetherFlow Telemetry", Icons.Outlined.Shield, Icons.Filled.Shield),
    PROFILES("Nodes", "Endpoint Profiles", Icons.Outlined.Dns, Icons.Filled.Dns),
    SUBSCRIPTIONS("Feeds", "Subscription & WARP Hub", Icons.Outlined.CloudSync, Icons.Filled.CloudSync),
    ROUTING("Routing", "Split-Tunnel & Encrypted DNS", Icons.Outlined.Route, Icons.Filled.Route),
    DIAGNOSTICS("Inspect", "Socket & Log Diagnostics", Icons.Outlined.Analytics, Icons.Filled.Analytics),
    SETTINGS("Config", "Core & Anti-DPI Engine", Icons.Outlined.Settings, Icons.Filled.Settings),
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
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPostNotificationsIfNeeded()
        handleIncomingIntent(intent)

        setContent {
            val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
            val vpnState by orchestrator.connectionState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            var showBrandSplash by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                snackbarEvents.collect { msg ->
                    snackbarHostState.showSnackbar(msg)
                }
            }

            LaunchedEffect(showBrandSplash) {
                if (showBrandSplash) {
                    delay(1_450.milliseconds)
                    showBrandSplash = false
                }
            }

            AetherFlowEnterpriseTheme(themeMode = settings.themeMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AetherAdaptiveRootScaffold(
                        vpnState = vpnState,
                        snackbarHostState = snackbarHostState,
                        onNotify = { msg -> snackbarEvents.tryEmit(msg) },
                    )

                    AnimatedVisibility(
                        visible = showBrandSplash,
                        exit = fadeOut(animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)),
                    ) {
                        AetherBrandSplashScreen()
                    }
                }
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

@Composable
private fun AetherBrandSplashScreen() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val logoScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.86f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "splash_logo_scale",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "splash_content_alpha",
    )
    val pulseTransition = rememberInfiniteTransition(label = "splash_ring_pulse")
    val ringPulse by pulseTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash_ring_radius",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070C14),
                        AetherPalette.SlateBackground,
                        Color(0xFF060A11),
                    )
                )
            )
            .clickable(enabled = false) {},
    ) {
        // Subtle radial cyber-telemetry backdrop rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            val baseRadius = size.minDimension * 0.28f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AetherPalette.AetherCyan.copy(alpha = 0.16f),
                        AetherPalette.EmeraldSignal.copy(alpha = 0.05f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = baseRadius * 1.65f,
                ),
                radius = baseRadius * 1.65f,
                center = center,
            )
            drawCircle(
                color = AetherPalette.AetherCyan.copy(alpha = 0.22f),
                radius = baseRadius * ringPulse,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }

        // Center Brand Identity
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .alpha(contentAlpha)
                .scale(logoScale),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF101927),
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AetherPalette.AetherCyan.copy(alpha = 0.80f),
                            AetherPalette.EmeraldSignal.copy(alpha = 0.65f),
                        )
                    ),
                ),
                shadowElevation = 18.dp,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.aetherflow_logo),
                    contentDescription = AetherBrandInfo.APP_NAME,
                    modifier = Modifier
                        .size(112.dp)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(22.dp)),
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = AetherBrandInfo.APP_NAME,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                ),
                color = Color(0xFFF8FAFC),
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "MULTI-CORE ENTERPRISE VPN & TUNNEL",
                style = AetherMonoTypography.codeSmall.copy(
                    fontSize = 11.5.sp,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = AetherPalette.AetherCyan,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(14.dp))

            ProtocolBadge(
                text = "v${AetherBrandInfo.APP_VERSION} • XRAY • SING-BOX • AMNEZIAWG",
                accentColor = AetherPalette.EmeraldSignal,
            )

            Spacer(Modifier.height(26.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(156.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = AetherPalette.AetherCyan,
                trackColor = AetherPalette.SlateOutlineSubtle,
                strokeCap = StrokeCap.Round,
            )
        }

        // Bottom Author Signature & Repository Pill
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF101927).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, AetherPalette.AetherCyan.copy(alpha = 0.38f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            tint = AetherPalette.AetherCyan,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = AetherBrandInfo.DEVELOPER_SIGNATURE,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = Color(0xFFF8FAFC),
                        )
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = AetherPalette.AetherCyan,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = AetherBrandInfo.REPO_SHORT,
                        style = AetherMonoTypography.codeSmall.copy(fontSize = 11.sp),
                        color = AetherPalette.AetherCyan,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AetherAdaptiveRootScaffold(
    vpnState: VpnConnectionState,
    snackbarHostState: SnackbarHostState,
    onNotify: (String) -> Unit,
) {
    var currentDestination by remember { mutableStateOf(AetherDestination.DASHBOARD) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AetherAboutSignatureDialog(
            onDismiss = { showAboutDialog = false },
            onCopied = {
                showAboutDialog = false
                onNotify("Copied repository link: ${AetherBrandInfo.REPO_URL}")
            },
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useWidescreenRail = maxWidth >= 720.dp

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.aetherflow_logo),
                                    contentDescription = "AetherFlow Logo",
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                Text(
                                    text = currentDestination.subtitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
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
                            )
                            IconButton(
                                onClick = { showAboutDialog = true },
                                modifier = Modifier.padding(end = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "About AetherFlow & Developer",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useWidescreenRail) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                            AetherDestination.entries.forEach { dest ->
                                val isSelected = currentDestination == dest
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentDestination = dest },
                                    alwaysShowLabel = true,
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) dest.selectedIcon else dest.outlinedIcon,
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
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
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
                        header = {
                            Spacer(Modifier.height(8.dp))
                            Image(
                                painter = painterResource(id = R.drawable.aetherflow_logo),
                                contentDescription = "AetherFlow",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showAboutDialog = true },
                            )
                            Spacer(Modifier.height(4.dp))
                        },
                    ) {
                        AetherDestination.entries.forEach { dest ->
                            val isSelected = currentDestination == dest
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { currentDestination = dest },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) dest.selectedIcon else dest.outlinedIcon,
                                        contentDescription = dest.title,
                                    )
                                },
                                label = { Text(dest.title) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
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
private fun AetherAboutSignatureDialog(
    onDismiss: () -> Unit,
    onCopied: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Image(
                painter = painterResource(id = R.drawable.aetherflow_logo),
                contentDescription = AetherBrandInfo.APP_NAME,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${AetherBrandInfo.APP_NAME} v${AetherBrandInfo.APP_VERSION}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = AetherBrandInfo.DEVELOPER_SIGNATURE,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AetherPalette.AetherCyan,
                    )
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = AetherPalette.AetherCyan,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Enterprise Multi-Core Android VPN & Universal Network Tunnel Client powered by Xray-core, Sing-box, AmneziaWG, and hev-socks5-tunnel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = AetherBrandInfo.REPO_URL,
                        style = AetherMonoTypography.codeSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, AetherBrandInfo.REPO_URL.toUri()).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "GitHub Repo",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText("AetherFlow Repository", AetherBrandInfo.REPO_URL)
                            )
                            onCopied()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Copy Link",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.SemiBold)
            }
        },
    )
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
