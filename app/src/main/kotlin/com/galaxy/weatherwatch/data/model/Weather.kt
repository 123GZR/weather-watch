package com.galaxy.weatherwatch.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 性能说明：这些数据类加 [Immutable] 后，Compose 编译器会将它们视为稳定类型，
 * 刷新时若内容未变则跳过对应子树的重组，避免手表上全列表无谓重组。
 */
@Immutable
@Serializable
data class WeatherSnapshot(
    val current: CurrentWeather,
    val today: DailySummary,
    val hourly: List<HourlyForecast>
)

@Immutable
@Serializable
data class CurrentWeather(
    val temp: Double,
    val feelsLike: Double,
    val icon: WeatherIcon,
    val description: String,
    val updatedAt: Long
)

@Immutable
@Serializable
data class HourlyForecast(
    val epochSecond: Long,
    val temp: Double,
    val icon: WeatherIcon,
    @SerialName("pop")
    val precipProbability: Double
)

@Immutable
@Serializable
data class DailySummary(
    val minTemp: Double,
    val maxTemp: Double,
    val precipProbability: Double
)
