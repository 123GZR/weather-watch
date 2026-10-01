package com.galaxy.weatherwatch.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherSnapshot(
    val current: CurrentWeather,
    val today: DailySummary,
    val hourly: List<HourlyForecast>
)

@Serializable
data class CurrentWeather(
    val temp: Double,
    val feelsLike: Double,
    val icon: WeatherIcon,
    val description: String,
    val updatedAt: Long
)

@Serializable
data class HourlyForecast(
    val epochSecond: Long,
    val temp: Double,
    val icon: WeatherIcon,
    @SerialName("pop")
    val precipProbability: Double
)

@Serializable
data class DailySummary(
    val minTemp: Double,
    val maxTemp: Double,
    val precipProbability: Double
)
