package com.ceoeg.aetherflow.core.common.design

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceoeg.aetherflow.core.common.model.ThemeMode
import java.util.Locale

object AetherPalette {
    // Brand Accents harmonized with the AetherFlow Logo (Electric Cyan + Emerald Signal)
    val AetherCyan = Color(0xFF22D3EE)
    val TelemetryCyan = Color(0xFF38BDF8)
    val EmeraldSignal = Color(0xFF10B981)
    val EmeraldBright = Color(0xFF34D399)
    val WarningAmber = Color(0xFFF59E0B)
    val AlertCrimson = Color(0xFFEF4444)

    // Slate Dark (Harmonized with AetherFlow obsidian-slate emblem background)
    val SlateBackground = Color(0xFF090F19)
    val SlateSurfaceLow = Color(0xFF0E1624)
    val SlateSurface = Color(0xFF121C2D)
    val SlateSurfaceElevated = Color(0xFF19273D)
    val SlateSurfaceHigh = Color(0xFF22334F)
    val SlateOutline = Color(0xFF384E6E)
    val SlateOutlineSubtle = Color(0xFF23334B)

    // True Pitch-Black AMOLED
    val AmoledBackground = Color(0xFF000000)
    val AmoledSurfaceLow = Color(0xFF070B11)
    val AmoledSurface = Color(0xFF0B111A)
    val AmoledSurfaceElevated = Color(0xFF131D2B)
    val AmoledSurfaceHigh = Color(0xFF1C2A3E)
    val AmoledOutline = Color(0xFF2F4360)
    val AmoledOutlineSubtle = Color(0xFF1B2738)

    // Crisp Light
    val CrispBackground = Color(0xFFF1F5F9)
    val CrispSurface = Color(0xFFFFFFFF)
    val CrispSurfaceLow = Color(0xFFF8FAFC)
    val CrispSurfaceElevated = Color(0xFFE2E8F0)
    val CrispPrimary = Color(0xFF0284C7)
    val CrispOutline = Color(0xFF94A3B8)
    val CrispOutlineSubtle = Color(0xFFCBD5E1)
}

private val SlateDarkScheme: ColorScheme = darkColorScheme(
    primary = AetherPalette.AetherCyan,
    onPrimary = Color(0xFF032530),
    primaryContainer = Color(0xFF0C3B4E),
    onPrimaryContainer = Color(0xFFCFFAFE),
    secondary = AetherPalette.EmeraldSignal,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = AetherPalette.WarningAmber,
    onTertiary = Color(0xFF3B1D00),
    tertiaryContainer = Color(0xFF573005),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = AetherPalette.SlateBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = AetherPalette.SlateSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = AetherPalette.SlateSurfaceElevated,
    onSurfaceVariant = Color(0xFFA8B8CE),
    surfaceTint = AetherPalette.AetherCyan,
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = Color(0xFF0369A1),
    outline = AetherPalette.SlateOutline,
    outlineVariant = AetherPalette.SlateOutlineSubtle,
    error = AetherPalette.AlertCrimson,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF5C1519),
    onErrorContainer = Color(0xFFFEE2E2),
    surfaceDim = Color(0xFF070C14),
    surfaceBright = Color(0xFF263856),
    surfaceContainerLowest = Color(0xFF060A11),
    surfaceContainerLow = AetherPalette.SlateSurfaceLow,
    surfaceContainer = AetherPalette.SlateSurface,
    surfaceContainerHigh = AetherPalette.SlateSurfaceElevated,
    surfaceContainerHighest = AetherPalette.SlateSurfaceHigh,
)

private val PitchBlackAmoledScheme: ColorScheme = SlateDarkScheme.copy(
    background = AetherPalette.AmoledBackground,
    surface = AetherPalette.AmoledSurface,
    surfaceVariant = AetherPalette.AmoledSurfaceElevated,
    outline = AetherPalette.AmoledOutline,
    outlineVariant = AetherPalette.AmoledOutlineSubtle,
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF1F2F46),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = AetherPalette.AmoledSurfaceLow,
    surfaceContainer = AetherPalette.AmoledSurface,
    surfaceContainerHigh = AetherPalette.AmoledSurfaceElevated,
    surfaceContainerHighest = AetherPalette.AmoledSurfaceHigh,
)

private val CrispLightScheme: ColorScheme = lightColorScheme(
    primary = AetherPalette.CrispPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF073B5A),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = AetherPalette.CrispBackground,
    onBackground = Color(0xFF0F172A),
    surface = AetherPalette.CrispSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = AetherPalette.CrispSurfaceElevated,
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = AetherPalette.CrispPrimary,
    outline = AetherPalette.CrispOutline,
    outlineVariant = AetherPalette.CrispOutlineSubtle,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    surfaceDim = Color(0xFFE2E8F0),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = AetherPalette.CrispSurfaceLow,
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE8EEF5),
    surfaceContainerHighest = AetherPalette.CrispSurfaceElevated,
)

val AetherTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp,
    ),
)

object AetherMonoTypography {
    val metricLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum",
    )
    val metricMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum",
    )
    val codeSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontFeatureSettings = "tnum",
    )
}

@Composable
fun AetherFlowEnterpriseTheme(
    themeMode: ThemeMode = ThemeMode.SLATE_DARK,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when (themeMode) {
        ThemeMode.PITCH_BLACK_AMOLED -> PitchBlackAmoledScheme
        ThemeMode.SLATE_DARK -> SlateDarkScheme
        ThemeMode.CRISP_LIGHT -> CrispLightScheme
        ThemeMode.DYNAMIC_MONOCHROME -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context)
            } else {
                SlateDarkScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AetherTypography,
        content = content,
    )
}

/**
 * High-contrast enterprise toggle switch with crisp ON (`✓`) and OFF (`×`) state indicators,
 * eliminating low-contrast dark-on-dark unchecked thumbs across Slate Dark and AMOLED themes.
 */
@Composable
fun AetherSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val uncheckedThumb = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
    val uncheckedTrack = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        Color(0xFFE2E8F0)
    }
    val uncheckedBorder = if (isDark) Color(0xFF4B6385) else Color(0xFF94A3B8)

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            Icon(
                imageVector = if (checked) Icons.Outlined.Check else Icons.Outlined.Close,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = MaterialTheme.colorScheme.primary,
            checkedIconColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = uncheckedThumb,
            uncheckedTrackColor = uncheckedTrack,
            uncheckedBorderColor = uncheckedBorder,
            uncheckedIconColor = if (isDark) Color(0xFF1E293B) else Color.White,
        ),
    )
}

/**
 * Standardized option toggle row with an accented leading icon badge, crisp title/subtitle,
 * and high-visibility [AetherSwitch].
 */
@Composable
fun AetherOptionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color = iconTint.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, iconTint.copy(alpha = 0.30f)),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        AetherSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * Unified high-contrast FilterChip with a leading icon and cohesive border/container styling
 * matching [AetherActionChip].
 */
@Composable
fun AetherFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        leadingIcon = {
            Icon(
                imageVector = icon ?: Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = accentColor,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.4.dp,
        ),
    )
}

/**
 * Unified high-contrast AssistChip with a leading accented icon and crisp slate surface styling.
 */
@Composable
fun AetherActionChip(
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
) {
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
        },
        label = {
            Text(
                text = label,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = iconTint,
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = true,
            borderColor = MaterialTheme.colorScheme.outline,
            borderWidth = 1.dp,
        ),
    )
}

@Composable
fun ProtocolBadge(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.42f)),
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(horizontal = 7.dp, vertical = 3.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = AetherMonoTypography.codeSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = accentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

fun formatBytesHuman(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
    val gb = mb / 1024.0
    if (gb < 1024.0) return String.format(Locale.US, "%.2f GB", gb)
    val tb = gb / 1024.0
    return String.format(Locale.US, "%.2f TB", tb)
}

fun formatBitrateHuman(bytesPerSec: Long): String {
    return "${formatBytesHuman(bytesPerSec)}/s"
}

fun formatDurationHms(elapsedSeconds: Long): String {
    val safe = elapsedSeconds.coerceAtLeast(0L)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val s = safe % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}
