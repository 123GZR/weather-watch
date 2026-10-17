package com.galaxy.weatherwatch.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

/**
 * 定位 Pin 图标：Canvas 矢量绘制。
 *
 * 设计空间：24x24，描边样式（水滴形外轮廓 + 中心空心圆）。
 * 参照 demo 中的 loc-pin SVG 设计。
 *
 * 性能说明：使用 [drawWithCache] 缓存缩放换算，路径在组合期缓存，
 * 帧绘制直接复用，避免滚动时重复构建贝塞尔路径。
 */
@Composable
fun LocationPinIcon(
    modifier: Modifier = Modifier,
    sizeDp: Int = 16
) {
    val color = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background
    // 贝塞尔路径组合期构建一次，绘制期直接复用
    val pinPath = remember { buildPinPath() }
    val innerPath = remember { buildInnerPinPath() }

    Spacer(
        modifier = modifier
            .size(sizeDp.dp)
            .drawWithCache {
                val unit = size.minDimension / 24f
                onDrawBehind {
                    withTransform(
                        transformBlock = {
                            scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero)
                        },
                        drawBlock = {
                            drawLocationPin(color, bgColor, pinPath, innerPath)
                        }
                    )
                }
            }
    )
}

private fun buildPinPath(): Path = Path().apply {
    moveTo(12f, 3f)
    cubicTo(16.3f, 3f, 18.5f, 5.3f, 18.5f, 9f)
    cubicTo(18.5f, 12.2f, 15.8f, 15.6f, 12f, 21.5f)
    cubicTo(8.2f, 15.6f, 5.5f, 12.2f, 5.5f, 9f)
    cubicTo(5.5f, 5.3f, 7.7f, 3f, 12f, 3f)
    close()
}

private fun buildInnerPinPath(): Path = Path().apply {
    moveTo(12f, 4.8f)
    cubicTo(14.8f, 4.8f, 16.5f, 6.5f, 16.5f, 9f)
    cubicTo(16.5f, 11.4f, 14.4f, 14.2f, 12f, 19f)
    cubicTo(9.6f, 14.2f, 7.5f, 11.4f, 7.5f, 9f)
    cubicTo(7.5f, 6.5f, 9.2f, 4.8f, 12f, 4.8f)
    close()
}

private fun DrawScope.drawLocationPin(color: Color, bgColor: Color, pinPath: Path, innerPath: Path) {
    val centerX = 12f

    // 水滴形实心 pin
    drawPath(pinPath, color)

    // 内部挖空：用背景色填充一个小水滴形，形成描边效果
    drawPath(innerPath, bgColor)

    // 中心小圆（描边）
    drawCircle(
        color = color,
        radius = 2.6f,
        center = Offset(centerX, 9.5f)
    )
    // 中心小圆内部挖空
    drawCircle(
        color = bgColor,
        radius = 1.0f,
        center = Offset(centerX, 9.5f)
    )
}
