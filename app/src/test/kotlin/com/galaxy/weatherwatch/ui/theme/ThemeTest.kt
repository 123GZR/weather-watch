package com.galaxy.weatherwatch.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 回归测试：Wear Material 的 [androidx.wear.compose.material.Colors] 并没有 `on` /
 * `onVariant` 参数（早期代码误用了它们，导致 compileDebugKotlin 失败），
 * 正确名称是 `onSurface` / `onSurfaceVariant`。这里锁定主题实际用到的取值。
 */
class ThemeTest {

    @Test
    fun `default wear colors expose dark theme text colors`() {
        assertEquals(Color(0xFFE6F0FF), DefaultWearColors.onSurface)
        assertEquals(Color(0xFFC5D5FF), DefaultWearColors.onSurfaceVariant)
        assertEquals(Color(0xFFE6F0FF), DefaultWearColors.onBackground)
        assertEquals(Color(0xFF111318), DefaultWearColors.surface)
        assertEquals(Color.Black, DefaultWearColors.background)
    }

    @Test
    fun `default wear colors keep brand accent colors`() {
        assertEquals(Color(0xFFAECBFA), DefaultWearColors.primary)
        assertEquals(Color(0xFFFDE293), DefaultWearColors.secondary)
        assertEquals(Color(0xFFF28B82), DefaultWearColors.error)
    }
}
