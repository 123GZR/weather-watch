package com.galaxy.weatherwatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.galaxy.weatherwatch.data.model.WeatherIcon

/**
 * 占位图标：用单字 emoji 表示天气。
 * TODO(里程碑3): 替换为设计稿矢量资源，避免依赖系统 emoji 字形。
 */
@Composable
fun WeatherIcon(
    icon: WeatherIcon,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon.emoji(),
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colors.onSurface
        )
    }
}

private fun WeatherIcon.emoji(): String = when (this) {
    WeatherIcon.CLEAR_DAY -> "☀"
    WeatherIcon.CLEAR_NIGHT -> "🌙"
    WeatherIcon.FEW_CLOUDS_DAY -> "🌤"
    WeatherIcon.FEW_CLOUDS_NIGHT -> "☁"
    WeatherIcon.SCATTERED_CLOUDS_DAY, WeatherIcon.SCATTERED_CLOUDS_NIGHT -> "☁"
    WeatherIcon.BROKEN_CLOUDS_DAY, WeatherIcon.BROKEN_CLOUDS_NIGHT -> "☁"
    WeatherIcon.SHOWER_RAIN_DAY, WeatherIcon.SHOWER_RAIN_NIGHT -> "🌧"
    WeatherIcon.RAIN_DAY, WeatherIcon.RAIN_NIGHT -> "🌧"
    WeatherIcon.THUNDERSTORM_DAY, WeatherIcon.THUNDERSTORM_NIGHT -> "⛈"
    WeatherIcon.SNOW_DAY, WeatherIcon.SNOW_NIGHT -> "❄"
    WeatherIcon.MIST_DAY, WeatherIcon.MIST_NIGHT -> "🌫"
}

@Suppress("unused")
private val UnusedColor = Color.Transparent
