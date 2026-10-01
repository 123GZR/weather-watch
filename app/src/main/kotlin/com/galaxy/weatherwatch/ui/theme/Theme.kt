package com.galaxy.weatherwatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme

private val DefaultWearColors = Colors(
    primary = Color(0xFFAECBFA),
    primaryVariant = Color(0xFF8AB4F8),
    secondary = Color(0xFFFDE293),
    secondaryVariant = Color(0xFFFDD663),
    error = Color(0xFFF28B82),
    onPrimary = Color(0xFF002B57),
    onSecondary = Color(0xFF4B3A00),
    onError = Color(0xFF680003),
    on = Color(0xFFE6F0FF),
    onVariant = Color(0xFFC5D5FF),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE6F0FF)
)

@Composable
fun GalaxyWeatherWatchTheme(
    colors: Colors = DefaultWearColors,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = colors,
        content = content
    )
}
