package com.galaxy.weatherwatch.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherIconTest {

    @Test
    fun `fromCode returns matching icon for known codes`() {
        assertEquals(WeatherIcon.CLEAR_DAY, WeatherIcon.fromCode("01d"))
        assertEquals(WeatherIcon.CLEAR_NIGHT, WeatherIcon.fromCode("01n"))
        assertEquals(WeatherIcon.RAIN_DAY, WeatherIcon.fromCode("10d"))
        assertEquals(WeatherIcon.THUNDERSTORM_NIGHT, WeatherIcon.fromCode("11n"))
        assertEquals(WeatherIcon.SNOW_DAY, WeatherIcon.fromCode("13d"))
        assertEquals(WeatherIcon.MIST_NIGHT, WeatherIcon.fromCode("50n"))
    }

    @Test
    fun `fromCode falls back to CLEAR_DAY for unknown code`() {
        assertEquals(WeatherIcon.CLEAR_DAY, WeatherIcon.fromCode("xx"))
        assertEquals(WeatherIcon.CLEAR_DAY, WeatherIcon.fromCode(null))
    }
}
