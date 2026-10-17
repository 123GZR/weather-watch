package com.galaxy.weatherwatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme

/**
 * 品牌色兜底 ColorScheme（Wear M3 动态配色不可用/不支持时使用）。
 *
 * 色值沿用原 M2.5 主题的品牌色；Wear M3 ColorScheme 没有 `surface` 槽，
 * 原 `surface=#111318` 映射到 `surfaceContainer` 系列（与 `colors.xml` 的
 * `surface_dark=#111318` 协调，启动窗口仍为 surface_dark）。
 * 其余色槽使用 M3 默认值。
 */
internal val DefaultColorScheme = ColorScheme(
    primary = Color(0xFFAECBFA),
    onPrimary = Color(0xFF002B57),
    secondary = Color(0xFFFDE293),
    tertiary = Color(0xFFFDD663),
    background = Color(0xFF0B0C10),
    surfaceContainerLow = Color(0xFF111318),
    surfaceContainer = Color(0xFF111318),
    surfaceContainerHigh = Color(0xFF111318),
    onSurface = Color(0xFFE6F0FF),
    onSurfaceVariant = Color(0xFFC5D5FF),
    error = Color(0xFFF28B82),
    onError = Color(0xFF680003),
    outline = Color(0xFF8AA0C2)
)

@Composable
fun GalaxyWeatherWatchTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // Wear M3 动态配色：旧设备/不支持时返回 null → 品牌色兜底
    val scheme = dynamicColorScheme(context) ?: DefaultColorScheme
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
