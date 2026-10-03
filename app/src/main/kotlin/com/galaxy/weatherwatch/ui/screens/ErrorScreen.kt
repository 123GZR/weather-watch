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
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.galaxy.weatherwatch.ui.WeatherUiState
import com.galaxy.weatherwatch.ui.components.LocationBar

/**
 * 错误 / 失败页面。
 *
 * 与成功页保持一致的「位置条 + 恢复操作」体验：
 * - 顶部位置条：展示最后一次已知位置（可能为空 → 显示「未获取位置」），点击可进入城市选择
 * - 主按钮：重试（权限拒绝时为「去设置」）
 * - 次按钮：「手动选择城市」——通过手动指定位置重新获取天气
 *
 * @param type 错误类型
 * @param locationName 失败页展示的位置名称（最后一次成功定位/手动选择的位置）
 * @param isRefreshing 是否正在刷新
 * @param onRetry 重试回调
 * @param onOpenAppSettings 权限拒绝时打开应用设置回调
 * @param onLocationClick 点击顶部位置条回调（打开城市选择）
 * @param onPickCity 手动选择城市回调（打开城市选择）
 * @param modifier 修饰符
 */
@Composable
fun ErrorScreen(
    type: WeatherUiState.ErrorType,
    locationName: String,
    isRefreshing: Boolean,
    onRetry: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onLocationClick: () -> Unit,
    onPickCity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPermissionDenied = type == WeatherUiState.ErrorType.PERMISSION_DENIED
    val buttonAction = if (isPermissionDenied) onOpenAppSettings else onRetry

    Box(modifier = modifier.fillMaxSize()) {
        // 顶部位置条：失败时仍可点击，进入城市选择后按所选位置获取天气
        LocationBar(
            locationName = locationName.ifBlank { "未获取位置" },
            onClick = onLocationClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
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
                color = MaterialTheme.colors.onSurface
            )
            Button(
                onClick = buttonAction,
                enabled = !isRefreshing,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator()
                } else {
                    Text(text = if (isPermissionDenied) "去设置" else "重试")
                }
            }
            // 手动选择城市：按用户指定的位置获取天气，不依赖 GPS/网络自动定位
            Button(
                onClick = onPickCity,
                enabled = !isRefreshing,
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Text(text = "手动选择城市")
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
