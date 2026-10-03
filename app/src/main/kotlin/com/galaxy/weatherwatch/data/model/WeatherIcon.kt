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

        /**
         * 和风天气 icon 码 → 天气枚举。
         *
         * 与 OWM 不同，和风的码是纯数字（如 `"100"`），晴/云类区分昼夜：
         * 100~104 为白天（晴/多云/少云/晴间多云/阴），150~154 为对应夜间变体；
         * 雨雪雷雾类昼夜共用同一码，而组件昼夜绘制相同（见 WeatherGlyph），
         * 因此统一映射到 DAY 变体。未知码回退 [CLEAR_DAY]。
         */
        fun fromQWeatherCode(code: String?): WeatherIcon {
            val numeric = code?.toIntOrNull() ?: return CLEAR_DAY
            return when (numeric) {
                100 -> CLEAR_DAY
                101 -> SCATTERED_CLOUDS_DAY
                102, 103 -> FEW_CLOUDS_DAY
                104 -> BROKEN_CLOUDS_DAY
                150 -> CLEAR_NIGHT
                151 -> SCATTERED_CLOUDS_NIGHT
                152, 153 -> FEW_CLOUDS_NIGHT
                154 -> BROKEN_CLOUDS_NIGHT
                300, 301 -> SHOWER_RAIN_DAY
                302, 303, 304 -> THUNDERSTORM_DAY
                in 305..318, 399 -> RAIN_DAY
                in 400..408, 499 -> SNOW_DAY
                in 500..515 -> MIST_DAY
                else -> CLEAR_DAY
            }
        }
    }
}
