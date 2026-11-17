package com.ceoeg.aetherflow.core.common.design

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val EmeraldSignal = Color(0xFF10B981)
    val EmeraldBright = Color(0xFF34D399)
    val TelemetryCyan = Color(0xFF38BDF8)
    val WarningAmber = Color(0xFFF59E0B)
    val AlertCrimson = Color(0xFFEF4444)

    // Slate Dark (Mullvad / Cloudflare Enterprise inspired)
    val SlateBackground = Color(0xFF0B111B)
    val SlateSurface = Color(0xFF111A28)
    val SlateSurfaceElevated = Color(0xFF182436)
    val SlateOutline = Color(0xFF24344D)

    // True Pitch-Black AMOLED
    val AmoledBackground = Color(0xFF000000)
    val AmoledSurface = Color(0xFF090D12)
    val AmoledSurfaceElevated = Color(0xFF101720)
    val AmoledOutline = Color(0xFF1E293B)

    // Crisp Light
    val CrispBackground = Color(0xFFF4F7FA)
    val CrispSurface = Color(0xFFFFFFFF)
    val CrispSurfaceElevated = Color(0xFFEBF1F6)
    val CrispPrimary = Color(0xFF0284C7)
    val CrispOutline = Color(0xFFCBD5E1)
}

private val SlateDarkScheme: ColorScheme = darkColorScheme(
    primary = AetherPalette.EmeraldSignal,
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = AetherPalette.TelemetryCyan,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = AetherPalette.WarningAmber,
    background = AetherPalette.SlateBackground,
    onBackground = Color(0xFFE2E8F0),
    surface = AetherPalette.SlateSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = AetherPalette.SlateSurfaceElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = AetherPalette.SlateOutline,
    error = AetherPalette.AlertCrimson,
)

private val PitchBlackAmoledScheme: ColorScheme = SlateDarkScheme.copy(
    background = AetherPalette.AmoledBackground,
    surface = AetherPalette.AmoledSurface,
    surfaceVariant = AetherPalette.AmoledSurfaceElevated,
    outline = AetherPalette.AmoledOutline,
)

private val CrispLightScheme: ColorScheme = lightColorScheme(
    primary = AetherPalette.CrispPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706),
    background = AetherPalette.CrispBackground,
    onBackground = Color(0xFF0F172A),
    surface = AetherPalette.CrispSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = AetherPalette.CrispSurfaceElevated,
    onSurfaceVariant = Color(0xFF475569),
    outline = AetherPalette.CrispOutline,
    error = Color(0xFFDC2626),
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
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
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
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
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

@Composable
fun ProtocolBadge(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
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
