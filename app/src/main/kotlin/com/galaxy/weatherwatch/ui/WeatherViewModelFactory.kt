package com.galaxy.weatherwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.galaxy.weatherwatch.data.CityRepository
import com.galaxy.weatherwatch.data.CitySelectionRepository
import com.galaxy.weatherwatch.data.GeocodingRepository
import com.galaxy.weatherwatch.data.LocationRepository
import com.galaxy.weatherwatch.data.WeatherRepository

/**
 * WeatherViewModel 的简单 Factory，用于构造函数注入依赖。
 * 未来接入 Hilt 等 DI 框架后可移除。
 */
class WeatherViewModelFactory(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository?,
    private val geocodingRepository: GeocodingRepository?,
    private val cityRepository: CityRepository? = null,
    private val citySelectionRepository: CitySelectionRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(
                weatherRepository = weatherRepository,
                locationRepository = locationRepository,
                geocodingRepository = geocodingRepository,
                cityRepository = cityRepository,
                citySelectionRepository = citySelectionRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
