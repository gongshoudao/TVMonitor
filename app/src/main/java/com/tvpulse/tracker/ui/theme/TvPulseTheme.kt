package com.tvpulse.tracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object TvColors {
    val Background = Color(0xFF0F131C)
    val Surface = Color(0xFF0F131C)
    val SurfaceContainerLowest = Color(0xFF0A0E17)
    val SurfaceContainerLow = Color(0xFF181B25)
    val SurfaceContainer = Color(0xFF1C1F29)
    val SurfaceContainerHigh = Color(0xFF262A34)
    val SurfaceContainerHighest = Color(0xFF31353F)
    val SurfaceBright = Color(0xFF353943)

    val Primary = Color(0xFFC3F5FF)
    val PrimaryContainer = Color(0xFF00E5FF)
    val PrimaryFixedDim = Color(0xFF00DAF3)
    val OnPrimary = Color(0xFF00363D)
    val OnPrimaryContainer = Color(0xFF00363D)

    val Secondary = Color(0xFF7DFFA2)
    val SecondaryContainer = Color(0xFF05E777)
    val SecondaryFixedDim = Color(0xFF00E475)
    val OnSecondary = Color(0xFF003918)
    val OnSecondaryContainer = Color(0xFF003918)

    val Tertiary = Color(0xFFFFE9CD)
    val TertiaryContainer = Color(0xFFFFC769)
    val TertiaryFixedDim = Color(0xFFFFBA38)
    val OnTertiary = Color(0xFF432C00)
    val OnTertiaryContainer = Color(0xFF432C00)

    val Error = Color(0xFFFFB4AB)
    val ErrorContainer = Color(0xFF93000A)
    val OnError = Color(0xFF690005)
    val OnErrorContainer = Color(0xFFFFDAD6)

    val OnSurface = Color(0xFFDFE2EF)
    val OnSurfaceVariant = Color(0xFFBAC9CC)
    val Outline = Color(0xFF849396)
    val OutlineVariant = Color(0xFF3B494C)
}

private val TvDarkColorScheme = darkColorScheme(
    primary = TvColors.Primary,
    onPrimary = TvColors.OnPrimary,
    primaryContainer = TvColors.PrimaryContainer,
    onPrimaryContainer = TvColors.OnPrimaryContainer,
    secondary = TvColors.Secondary,
    onSecondary = TvColors.OnSecondary,
    secondaryContainer = TvColors.SecondaryContainer,
    onSecondaryContainer = TvColors.OnSecondaryContainer,
    tertiary = TvColors.Tertiary,
    onTertiary = TvColors.OnTertiary,
    tertiaryContainer = TvColors.TertiaryContainer,
    onTertiaryContainer = TvColors.OnTertiaryContainer,
    error = TvColors.Error,
    onError = TvColors.OnError,
    errorContainer = TvColors.ErrorContainer,
    onErrorContainer = TvColors.OnErrorContainer,
    background = TvColors.Background,
    onBackground = TvColors.OnSurface,
    surface = TvColors.Surface,
    onSurface = TvColors.OnSurface,
    surfaceVariant = TvColors.SurfaceContainerHighest,
    onSurfaceVariant = TvColors.OnSurfaceVariant,
    outline = TvColors.Outline,
    outlineVariant = TvColors.OutlineVariant
)

@Composable
fun TvPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        content = content
    )
}
