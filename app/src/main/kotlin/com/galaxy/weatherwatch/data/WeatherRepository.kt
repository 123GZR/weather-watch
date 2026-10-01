package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.model.WeatherSnapshot

interface WeatherRepository {
    suspend fun fetchWeather(lat: Double, lon: Double): Result<WeatherSnapshot>
}
