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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val repository: WeatherRepository = MockWeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            // 已有数据时保留旧快照、只翻转刷新标记，避免整屏闪回 Loading；
            // 状态合并为单一 StateFlow，一次刷新只推送一次状态、只触发一轮重组。
            _uiState.update { previous ->
                when (previous) {
                    is WeatherUiState.Success -> previous.copy(isRefreshing = true)
                    else -> WeatherUiState.Loading
                }
            }
            // TODO(里程碑2): 用 FusedLocationProviderClient 取真实 GPS
            val lat = 39.9042
            val lon = 116.4074
            val result = repository.fetchWeather(lat, lon)
            _uiState.value = result.fold(
                onSuccess = { WeatherUiState.Success(it) },
                onFailure = { WeatherUiState.Error(it.toErrorType()) }
            )
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
    data class Success(
        val snapshot: WeatherSnapshot,
        val isRefreshing: Boolean = false
    ) : WeatherUiState

    data class Error(
        val type: ErrorType,
        val isRefreshing: Boolean = false
    ) : WeatherUiState

    enum class ErrorType {
        PERMISSION_DENIED,
        NO_NETWORK,
        TIMEOUT,
        API_ERROR,
        GPS_UNAVAILABLE
    }
}
