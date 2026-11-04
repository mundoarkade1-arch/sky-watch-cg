package com.skywatch.cg.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SkyWatchColors.ArrivalAccent,
    onPrimary = SkyWatchColors.ArrivalAccentContainer,
    primaryContainer = SkyWatchColors.SurfaceContainer,
    onPrimaryContainer = SkyWatchColors.OnBackground,
    secondary = SkyWatchColors.DepartureAccent,
    onSecondary = SkyWatchColors.OnBackground,
    background = SkyWatchColors.Background,
    onBackground = SkyWatchColors.OnBackground,
    surface = SkyWatchColors.Surface,
    onSurface = SkyWatchColors.OnSurface,
    surfaceVariant = SkyWatchColors.SurfaceContainer,
    onSurfaceVariant = SkyWatchColors.OnSurfaceVariant,
    outline = SkyWatchColors.Border,
    error = SkyWatchColors.Error,
    onError = SkyWatchColors.OnBackground
)

@Composable
fun SkyWatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SkyWatchTypography,
        content = content
    )
}