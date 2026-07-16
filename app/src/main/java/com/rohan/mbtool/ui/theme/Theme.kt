package com.rohan.mbtool.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    background          = Bg,
    surface             = Surf,
    surfaceVariant      = SurfVar,
    surfaceContainerHigh = SurfCont,
    primary             = Primary,
    onPrimary           = OnSurf,
    onBackground        = OnSurf,
    onSurface           = OnSurf,
    onSurfaceVariant    = Muted,
    outline             = Border,
    error               = Danger,
)

private val LightColors = lightColorScheme(
    background          = LBg,
    surface             = LSurf,
    surfaceVariant      = LSurfVar,
    surfaceContainerHigh = LSurfCont,
    primary             = Primary,
    onPrimary           = OnSurf,
    onBackground        = LOnSurf,
    onSurface           = LOnSurf,
    onSurfaceVariant    = LMuted,
    outline             = LBorder,
    error               = Danger,
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun MBToolTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography  = Typography,
        content     = content,
    )
}
