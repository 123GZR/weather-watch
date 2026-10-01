package com.galaxy.weatherwatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.galaxy.weatherwatch.ui.WeatherUiState

@Composable
fun ErrorScreen(
    type: WeatherUiState.ErrorType,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = type.title(),
                style = MaterialTheme.typography.title2,
                color = MaterialTheme.colors.error
            )
            Text(
                text = type.hint(),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.on
            )
            Button(onClick = onRetry) {
                Text(text = if (type == WeatherUiState.ErrorType.PERMISSION_DENIED) "去设置" else "重试")
            }
        }
    }
}

private fun WeatherUiState.ErrorType.title(): String = when (this) {
    WeatherUiState.ErrorType.PERMISSION_DENIED -> "缺少位置权限"
    WeatherUiState.ErrorType.NO_NETWORK -> "网络不可用"
    WeatherUiState.ErrorType.TIMEOUT -> "请求超时"
    WeatherUiState.ErrorType.API_ERROR -> "天气服务暂不可用"
    WeatherUiState.ErrorType.GPS_UNAVAILABLE -> "无法获取位置"
}

private fun WeatherUiState.ErrorType.hint(): String = when (this) {
    WeatherUiState.ErrorType.PERMISSION_DENIED -> "需要位置权限才能获取本地天气"
    WeatherUiState.ErrorType.NO_NETWORK -> "请检查网络后重试"
    WeatherUiState.ErrorType.TIMEOUT -> "请到开阔处重试"
    WeatherUiState.ErrorType.API_ERROR -> "请稍后重试"
    WeatherUiState.ErrorType.GPS_UNAVAILABLE -> "请到开阔处重试"
}
