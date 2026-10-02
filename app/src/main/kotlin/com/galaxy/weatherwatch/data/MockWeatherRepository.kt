package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.model.CurrentWeather
import com.galaxy.weatherwatch.data.model.DailySummary
import com.galaxy.weatherwatch.data.model.HourlyForecast
import com.galaxy.weatherwatch.data.model.WeatherIcon
import com.galaxy.weatherwatch.data.model.WeatherSnapshot
import kotlinx.coroutines.delay

/**
 * MVP 阶段用于打通 UI 与交互的本地假数据源。
 * 不发起任何网络请求，固定返回一份示例快照，刷新时仅推进时间戳。
 */
class MockWeatherRepository(
    private val simulateDelayMs: Long = 800L,
    private val failureMode: FailureMode = FailureMode.NONE
) : WeatherRepository {

    enum class FailureMode { NONE, NETWORK, TIMEOUT, API, GPS }

    override suspend fun fetchWeather(lat: Double, lon: Double): Result<WeatherSnapshot> {
        delay(simulateDelayMs)
        return when (failureMode) {
            FailureMode.NONE -> Result.success(buildSnapshot())
            FailureMode.NETWORK -> Result.failure(NoNetworkException())
            FailureMode.TIMEOUT -> Result.failure(TimeoutException())
            FailureMode.API -> Result.failure(ApiException(500))
            FailureMode.GPS -> Result.failure(GpsUnavailableException())
        }
    }

    private fun buildSnapshot(): WeatherSnapshot {
        val now = System.currentTimeMillis()
        val current = CurrentWeather(
            temp = 18.5,
            feelsLike = 17.2,
            icon = WeatherIcon.FEW_CLOUDS_DAY,
            description = "少云",
            updatedAt = now
        )
        val today = DailySummary(minTemp = 12.0, maxTemp = 22.0, precipProbability = 0.2)
        val hourly = (0 until 12).map { hoursAhead ->
            HourlyForecast(
                epochSecond = (now / 1000) + hoursAhead * 3600,
                temp = 16.0 + (hoursAhead % 6) - (hoursAhead / 6) * 0.5,
                icon = pickIcon(hoursAhead),
                precipProbability = if (hoursAhead in 4..7) 0.4 else 0.0
            )
        }
        return WeatherSnapshot(current = current, today = today, hourly = hourly)
    }

    private fun pickIcon(hour: Int): WeatherIcon =
        when (hour % 4) {
            0 -> WeatherIcon.CLEAR_DAY
            1 -> WeatherIcon.FEW_CLOUDS_DAY
            2 -> WeatherIcon.RAIN_DAY
            else -> WeatherIcon.BROKEN_CLOUDS_DAY
        }
}

class NoNetworkException : RuntimeException("网络不可用")
class TimeoutException : RuntimeException("请求超时")
class ApiException(val httpCode: Int) : RuntimeException("天气服务暂不可用 ($httpCode)")
class GpsUnavailableException : RuntimeException("无法获取位置")
