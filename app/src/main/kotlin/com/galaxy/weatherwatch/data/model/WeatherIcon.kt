package com.galaxy.weatherwatch.data.model

enum class WeatherIcon(val code: String) {
    CLEAR_DAY("01d"),
    CLEAR_NIGHT("01n"),
    FEW_CLOUDS_DAY("02d"),
    FEW_CLOUDS_NIGHT("02n"),
    SCATTERED_CLOUDS_DAY("03d"),
    SCATTERED_CLOUDS_NIGHT("03n"),
    BROKEN_CLOUDS_DAY("04d"),
    BROKEN_CLOUDS_NIGHT("04n"),
    SHOWER_RAIN_DAY("09d"),
    SHOWER_RAIN_NIGHT("09n"),
    RAIN_DAY("10d"),
    RAIN_NIGHT("10n"),
    THUNDERSTORM_DAY("11d"),
    THUNDERSTORM_NIGHT("11n"),
    SNOW_DAY("13d"),
    SNOW_NIGHT("13n"),
    MIST_DAY("50d"),
    MIST_NIGHT("50n");

    companion object {
        fun fromCode(code: String?): WeatherIcon =
            entries.firstOrNull { it.code == code } ?: CLEAR_DAY
    }
}
