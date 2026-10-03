package com.galaxy.weatherwatch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * 顶部位置条：显示当前位置名称，点击进入城市选择。
 * 在成功页 / 失败页共用，保证交互一致。
 *
 * @param locationName 显示的位置名称（可能为空白，由调用方决定兜底文案）
 * @param onClick 点击位置条回调（通常为打开城市搜索）
 * @param modifier 修饰符
 */
@Composable
fun LocationBar(
    locationName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LocationPinIcon(sizeDp = 14)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = locationName,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            letterSpacing = 0.6.sp,
            maxLines = 1
        )
    }
}
