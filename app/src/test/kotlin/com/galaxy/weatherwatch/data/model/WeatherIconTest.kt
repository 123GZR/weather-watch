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

    @Test
    fun `fromQWeatherCode maps clear and cloudy codes with day night variants`() {
        assertEquals(WeatherIcon.CLEAR_DAY, WeatherIcon.fromQWeatherCode("100"))
        assertEquals(WeatherIcon.SCATTERED_CLOUDS_DAY, WeatherIcon.fromQWeatherCode("101"))
        assertEquals(WeatherIcon.FEW_CLOUDS_DAY, WeatherIcon.fromQWeatherCode("102"))
        assertEquals(WeatherIcon.FEW_CLOUDS_DAY, WeatherIcon.fromQWeatherCode("103"))
        assertEquals(WeatherIcon.BROKEN_CLOUDS_DAY, WeatherIcon.fromQWeatherCode("104"))
        assertEquals(WeatherIcon.CLEAR_NIGHT, WeatherIcon.fromQWeatherCode("150"))
        assertEquals(WeatherIcon.SCATTERED_CLOUDS_NIGHT, WeatherIcon.fromQWeatherCode("151"))
        assertEquals(WeatherIcon.FEW_CLOUDS_NIGHT, WeatherIcon.fromQWeatherCode("152"))
        assertEquals(WeatherIcon.FEW_CLOUDS_NIGHT, WeatherIcon.fromQWeatherCode("153"))
        assertEquals(WeatherIcon.BROKEN_CLOUDS_NIGHT, WeatherIcon.fromQWeatherCode("154"))
    }

    @Test
    fun `fromQWeatherCode maps rain snow thunder and mist codes`() {
        assertEquals(WeatherIcon.SHOWER_RAIN_DAY, WeatherIcon.fromQWeatherCode("300"))
        assertEquals(WeatherIcon.SHOWER_RAIN_DAY, WeatherIcon.fromQWeatherCode("301"))
        assertEquals(WeatherIcon.THUNDERSTORM_DAY, WeatherIcon.fromQWeatherCode("302"))
        assertEquals(WeatherIcon.THUNDERSTORM_DAY, WeatherIcon.fromQWeatherCode("303"))
        assertEquals(WeatherIcon.RAIN_DAY, WeatherIcon.fromQWeatherCode("305"))
        assertEquals(WeatherIcon.RAIN_DAY, WeatherIcon.fromQWeatherCode("313"))
        assertEquals(WeatherIcon.RAIN_DAY, WeatherIcon.fromQWeatherCode("399"))
        assertEquals(WeatherIcon.SNOW_DAY, WeatherIcon.fromQWeatherCode("400"))
        assertEquals(WeatherIcon.SNOW_DAY, WeatherIcon.fromQWeatherCode("405"))
        assertEquals(WeatherIcon.SNOW_DAY, WeatherIcon.fromQWeatherCode("499"))
        assertEquals(WeatherIcon.MIST_DAY, WeatherIcon.fromQWeatherCode("500"))
        assertEquals(WeatherIcon.MIST_DAY, WeatherIcon.fromQWeatherCode("502"))
        assertEquals(WeatherIcon.MIST_DAY, WeatherIcon.fromQWeatherCode("515"))
    }

    @Test
    fun `fromQWeatherCode falls back to CLEAR_DAY for unknown code`() {
        assertEquals(WeatherIcon.CLEAR_DAY, WeatherIcon.fromQWeatherCode("999"))
        assertEquals(WeatherIcon.CLEAR_DAY, WeatherIcon.fromQWeatherCode("abc"))
        assertEquals(WeatherIcon.CLEAR_DAY, WeatherIcon.fromQWeatherCode(null))
    }
}
