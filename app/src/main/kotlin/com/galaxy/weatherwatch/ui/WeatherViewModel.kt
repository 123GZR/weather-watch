package com.galaxy.weatherwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxy.weatherwatch.data.MockWeatherRepository
import com.galaxy.weatherwatch.data.NoNetworkException
import com.galaxy.weatherwatch.data.TimeoutException
import com.galaxy.weatherwatch.data.WeatherRepository
import com.galaxy.weatherwatch.data.model.WeatherSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository = MockWeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _isRefreshing.value = true
            if (_uiState.value !is WeatherUiState.Success) {
                _uiState.value = WeatherUiState.Loading
            }
            // TODO(里程碑2): 用 FusedLocationProviderClient 取真实 GPS
            val lat = 39.9042
            val lon = 116.4074
            val result = repository.fetchWeather(lat, lon)
            result.fold(
                onSuccess = { _uiState.value = WeatherUiState.Success(it) },
                onFailure = { _uiState.value = WeatherUiState.Error(it.toErrorType()) }
            )
            _isRefreshing.value = false
        }
    }
}

private fun Throwable.toErrorType(): WeatherUiState.ErrorType = when (this) {
    is NoNetworkException -> WeatherUiState.ErrorType.NO_NETWORK
    is TimeoutException -> WeatherUiState.ErrorType.TIMEOUT
    is com.galaxy.weatherwatch.data.ApiException -> WeatherUiState.ErrorType.API_ERROR
    is com.galaxy.weatherwatch.data.GpsUnavailableException -> WeatherUiState.ErrorType.GPS_UNAVAILABLE
    else -> WeatherUiState.ErrorType.API_ERROR
}

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val snapshot: WeatherSnapshot) : WeatherUiState
    data class Error(val type: ErrorType) : WeatherUiState

    enum class ErrorType {
        PERMISSION_DENIED,
        NO_NETWORK,
        TIMEOUT,
        API_ERROR,
        GPS_UNAVAILABLE
    }
}
