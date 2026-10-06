package com.galaxy.weatherwatch.data

import android.content.SharedPreferences
import com.galaxy.weatherwatch.data.model.City
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedPreferencesCitySelectionRepositoryTest {

    private val city = City(
        name = "佛山市 顺德区",
        pinyin = "foshanshishundequ",
        abbreviation = "fsssdq",
        latitude = 22.84,
        longitude = 113.28
    )

    @Test
    fun `saveSelectedCity 将城市各字段写入 SharedPreferences`() {
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } just Runs

        SharedPreferencesCitySelectionRepository(prefs).saveSelectedCity(city)

        verify {
            editor.putString(SharedPreferencesCitySelectionRepository.KEY_NAME, city.name)
            editor.putString(SharedPreferencesCitySelectionRepository.KEY_PINYIN, city.pinyin)
            editor.putString(SharedPreferencesCitySelectionRepository.KEY_ABBREVIATION, city.abbreviation)
            editor.putString(SharedPreferencesCitySelectionRepository.KEY_LATITUDE, city.latitude.toString())
            editor.putString(SharedPreferencesCitySelectionRepository.KEY_LONGITUDE, city.longitude.toString())
            editor.apply()
        }
    }

    @Test
    fun `getSelectedCity - 已保存时返回完整城市`() {
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_NAME, null) } returns city.name
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_PINYIN, null) } returns city.pinyin
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_ABBREVIATION, null) } returns city.abbreviation
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_LATITUDE, null) } returns city.latitude.toString()
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_LONGITUDE, null) } returns city.longitude.toString()

        val result = SharedPreferencesCitySelectionRepository(prefs).getSelectedCity()

        assertEquals("经纬度应无精度损失", city, result)
    }

    @Test
    fun `getSelectedCity - 未保存时返回 null`() {
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString(any(), any()) } returns null

        val result = SharedPreferencesCitySelectionRepository(prefs).getSelectedCity()

        assertNull(result)
    }

    @Test
    fun `getSelectedCity - 经纬度数据损坏时返回 null 而非抛异常`() {
        val prefs = mockk<SharedPreferences>()
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_NAME, null) } returns city.name
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_PINYIN, null) } returns city.pinyin
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_ABBREVIATION, null) } returns city.abbreviation
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_LATITUDE, null) } returns "not-a-number"
        every { prefs.getString(SharedPreferencesCitySelectionRepository.KEY_LONGITUDE, null) } returns "113.28"

        val result = SharedPreferencesCitySelectionRepository(prefs).getSelectedCity()

        assertNull("损坏数据应视为未记忆，安全降级", result)
    }

    @Test
    fun `clearSelectedCity 清空全部记忆键`() {
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()
        every { prefs.edit() } returns editor
        every { editor.clear() } returns editor
        every { editor.apply() } just Runs

        SharedPreferencesCitySelectionRepository(prefs).clearSelectedCity()

        verify {
            editor.clear()
            editor.apply()
        }
    }
}
