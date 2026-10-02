package com.galaxy.weatherwatch.ui.components

import com.galaxy.weatherwatch.data.model.WeatherIcon
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 回归测试：矢量图标映射必须覆盖 WeatherIcon 全部枚举值。
 * 原 emoji 实现依赖系统字形渲染（手表上触发字体回退扫描导致卡顿），
 * 现改为 Canvas 矢量绘制，此测试锁定枚举 → 图元的纯映射关系。
 */
class WeatherIconGlyphTest {

    @Test
    fun `every weather icon maps to a glyph`() {
        val mapped = WeatherIcon.entries.map { it.toGlyph() }
        assertEquals(WeatherIcon.entries.size, mapped.size)
        // 映射必须覆盖所有图元类型，保证 Canvas 分支完整
        assertEquals(WeatherGlyph.entries.toSet(), mapped.toSet())
    }

    @Test
    fun `clear day maps to sun`() {
        assertEquals(WeatherGlyph.SUN, WeatherIcon.CLEAR_DAY.toGlyph())
    }

    @Test
    fun `clear night maps to moon`() {
        assertEquals(WeatherGlyph.MOON, WeatherIcon.CLEAR_NIGHT.toGlyph())
    }

    @Test
    fun `cloudy variants map to cloud glyphs`() {
        assertEquals(WeatherGlyph.CLOUD_SUN, WeatherIcon.FEW_CLOUDS_DAY.toGlyph())
        assertEquals(WeatherGlyph.CLOUD, WeatherIcon.FEW_CLOUDS_NIGHT.toGlyph())
        assertEquals(WeatherGlyph.CLOUD, WeatherIcon.SCATTERED_CLOUDS_DAY.toGlyph())
        assertEquals(WeatherGlyph.CLOUD, WeatherIcon.BROKEN_CLOUDS_NIGHT.toGlyph())
    }

    @Test
    fun `rain variants map to rain glyph`() {
        assertEquals(WeatherGlyph.RAIN, WeatherIcon.SHOWER_RAIN_DAY.toGlyph())
        assertEquals(WeatherGlyph.RAIN, WeatherIcon.RAIN_NIGHT.toGlyph())
    }

    @Test
    fun `thunder snow and mist map to their glyphs`() {
        assertEquals(WeatherGlyph.THUNDER, WeatherIcon.THUNDERSTORM_DAY.toGlyph())
        assertEquals(WeatherGlyph.THUNDER, WeatherIcon.THUNDERSTORM_NIGHT.toGlyph())
        assertEquals(WeatherGlyph.SNOW, WeatherIcon.SNOW_DAY.toGlyph())
        assertEquals(WeatherGlyph.SNOW, WeatherIcon.SNOW_NIGHT.toGlyph())
        assertEquals(WeatherGlyph.MIST, WeatherIcon.MIST_DAY.toGlyph())
        assertEquals(WeatherGlyph.MIST, WeatherIcon.MIST_NIGHT.toGlyph())
    }
}
