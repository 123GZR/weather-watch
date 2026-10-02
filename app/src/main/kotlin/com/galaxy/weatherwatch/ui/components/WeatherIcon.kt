package com.galaxy.weatherwatch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import com.galaxy.weatherwatch.data.model.WeatherIcon
import kotlin.math.cos
import kotlin.math.sin

/**
 * 天气图标：Canvas 矢量绘制，替代原 emoji 实现。
 *
 * 性能说明：手表上渲染彩色 emoji 会触发字体回退扫描，滚动列表时反复出现卡顿；
 * 矢量绘制没有字体加载与图片解码开销，且月牙路径通过 [remember] 缓存，
 * 背景色只在主题主色变化时重建。
 *
 * 坐标系：图标在 48x48 设计空间内绘制，再等比缩放到 40.dp 画布。
 */
@Composable
fun WeatherIcon(
    icon: WeatherIcon,
    modifier: Modifier = Modifier
) {
    val glyphColor = MaterialTheme.colors.onSurface
    // MaterialTheme.colors 是 @Composable getter，先在组合作用域取值，
    // 再放入 remember 的 key 与 lambda，避免非组合上下文调用报错
    val primaryColor = MaterialTheme.colors.primary
    val backgroundColor = remember(primaryColor) {
        primaryColor.copy(alpha = 0.18f)
    }
    // PathOperation.Difference 求差集开销较大，组合期缓存一次，绘制期直接复用
    val moonPath = remember { buildMoonCrescentPath() }

    Canvas(modifier = modifier.size(40.dp)) {
        drawCircle(color = backgroundColor, radius = size.minDimension / 2f)
        val unit = size.minDimension / 48f
        val offsetX = (size.width - size.minDimension) / 2f
        val offsetY = (size.height - size.minDimension) / 2f
        withTransform(
            transformBlock = {
                translate(offsetX, offsetY)
                scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero)
            },
            drawBlock = {
                when (icon.toGlyph()) {
                    WeatherGlyph.SUN -> drawSun(glyphColor)
                    WeatherGlyph.MOON -> drawPath(moonPath, glyphColor)
                    WeatherGlyph.CLOUD -> drawCloud(glyphColor)
                    WeatherGlyph.CLOUD_SUN -> {
                        drawSunCore(glyphColor, center = Offset(31.5f, 16f), radius = 6.5f)
                        drawCloud(glyphColor)
                    }
                    WeatherGlyph.RAIN -> {
                        drawCloud(glyphColor)
                        drawRainDrops(glyphColor)
                    }
                    WeatherGlyph.THUNDER -> {
                        drawCloud(glyphColor)
                        drawLightning(glyphColor)
                    }
                    WeatherGlyph.SNOW -> {
                        drawCloud(glyphColor)
                        drawSnowDots(glyphColor)
                    }
                    WeatherGlyph.MIST -> drawMist(glyphColor)
                }
            }
        )
    }
}

/** 图标到绘制图元的纯映射，便于单元测试覆盖全部 18 种天气枚举。 */
internal enum class WeatherGlyph { SUN, MOON, CLOUD, CLOUD_SUN, RAIN, THUNDER, SNOW, MIST }

internal fun WeatherIcon.toGlyph(): WeatherGlyph = when (this) {
    WeatherIcon.CLEAR_DAY -> WeatherGlyph.SUN
    WeatherIcon.CLEAR_NIGHT -> WeatherGlyph.MOON
    WeatherIcon.FEW_CLOUDS_DAY -> WeatherGlyph.CLOUD_SUN
    WeatherIcon.FEW_CLOUDS_NIGHT,
    WeatherIcon.SCATTERED_CLOUDS_DAY,
    WeatherIcon.SCATTERED_CLOUDS_NIGHT,
    WeatherIcon.BROKEN_CLOUDS_DAY,
    WeatherIcon.BROKEN_CLOUDS_NIGHT -> WeatherGlyph.CLOUD
    WeatherIcon.SHOWER_RAIN_DAY,
    WeatherIcon.SHOWER_RAIN_NIGHT,
    WeatherIcon.RAIN_DAY,
    WeatherIcon.RAIN_NIGHT -> WeatherGlyph.RAIN
    WeatherIcon.THUNDERSTORM_DAY,
    WeatherIcon.THUNDERSTORM_NIGHT -> WeatherGlyph.THUNDER
    WeatherIcon.SNOW_DAY,
    WeatherIcon.SNOW_NIGHT -> WeatherGlyph.SNOW
    WeatherIcon.MIST_DAY,
    WeatherIcon.MIST_NIGHT -> WeatherGlyph.MIST
}

private fun DrawScope.drawSun(color: Color) {
    val center = Offset(24f, 24f)
    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        drawLine(
            color = color,
            start = Offset(center.x + 16.5f * dx, center.y + 16.5f * dy),
            end = Offset(center.x + 21.5f * dx, center.y + 21.5f * dy),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }
    drawSunCore(color, center = center, radius = 9.5f)
}

private fun DrawScope.drawSunCore(color: Color, center: Offset, radius: Float) {
    drawCircle(color = color, radius = radius, center = center)
}

private fun DrawScope.drawCloud(color: Color) {
    drawCircle(color = color, radius = 6.5f, center = Offset(17f, 27f))
    drawCircle(color = color, radius = 8.5f, center = Offset(26f, 23.5f))
    drawCircle(color = color, radius = 7f, center = Offset(35f, 27.5f))
    drawRoundRect(
        color = color,
        topLeft = Offset(10f, 26.5f),
        size = Size(32f, 11f),
        cornerRadius = CornerRadius(7f, 7f)
    )
}

private fun DrawScope.drawRainDrops(color: Color) {
    val stroke = 2.2f
    drawLine(color, Offset(16f, 42f), Offset(14.5f, 45.5f), stroke, StrokeCap.Round)
    drawLine(color, Offset(25f, 42f), Offset(23.5f, 45.5f), stroke, StrokeCap.Round)
    drawLine(color, Offset(34f, 42f), Offset(32.5f, 45.5f), stroke, StrokeCap.Round)
}

private fun DrawScope.drawLightning(color: Color) {
    val bolt = Path().apply {
        moveTo(28f, 31f)
        lineTo(18f, 43f)
        lineTo(25f, 43f)
        lineTo(21f, 47.5f)
        lineTo(31f, 39f)
        lineTo(24f, 39f)
        close()
    }
    drawPath(bolt, color)
}

private fun DrawScope.drawSnowDots(color: Color) {
    drawCircle(color = color, radius = 1.9f, center = Offset(17f, 43.5f))
    drawCircle(color = color, radius = 1.9f, center = Offset(26f, 45f))
    drawCircle(color = color, radius = 1.9f, center = Offset(35f, 43.5f))
}

private fun DrawScope.drawMist(color: Color) {
    val stroke = 2.5f
    drawLine(color, Offset(11f, 19f), Offset(37f, 19f), stroke, StrokeCap.Round)
    drawLine(color, Offset(15f, 27f), Offset(33f, 27f), stroke, StrokeCap.Round)
    drawLine(color, Offset(11f, 35f), Offset(37f, 35f), stroke, StrokeCap.Round)
}

/** 大圆减去偏移小圆得到月牙，避免使用半透明背景色做擦除导致的混色。 */
private fun buildMoonCrescentPath(): Path {
    val full = Path().apply { addOval(Rect(offset = Offset(20f, 9f), size = Size(28f, 30f))) }
    val cut = Path().apply { addOval(Rect(offset = Offset(26.5f, 7.5f), size = Size(28f, 30f))) }
    return Path.combine(PathOperation.Difference, full, cut)
}
