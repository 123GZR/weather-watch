package com.galaxy.weatherwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxy.weatherwatch.data.GeocodingRepository
import com.galaxy.weatherwatch.data.GpsUnavailableException
import com.galaxy.weatherwatch.data.LocationRepository
import com.galaxy.weatherwatch.data.LocationUnavailableException
import com.galaxy.weatherwatch.data.MockWeatherRepository
import com.galaxy.weatherwatch.data.NoNetworkException
import com.galaxy.weatherwatch.data.TimeoutException
import com.galaxy.weatherwatch.data.WeatherRepository
import com.galaxy.weatherwatch.data.model.WeatherSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val weatherRepository: WeatherRepository = MockWeatherRepository(),
    private val locationRepository: LocationRepository? = null,
    private val geocodingRepository: GeocodingRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    /**
     * 检查定位权限并启动刷新流程。
     * 由 Activity/Compose 层调用，因为权限状态只能在宿主层获取。
     *
     * @param hasPermission 是否已授予定位权限
     */
    fun refreshWithPermissionCheck(hasPermission: Boolean) {
        if (!hasPermission) {
            _uiState.value = WeatherUiState.Error(
                type = WeatherUiState.ErrorType.PERMISSION_DENIED,
                isRefreshing = false
            )
            return
        }
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

            // 第一步：获取位置
            val locationResult = locationRepository?.getCurrentLocation()
            val (lat, lon) = if (locationResult != null) {
                locationResult.fold(
                    onSuccess = { it.latitude to it.longitude },
                    onFailure = {
                        _uiState.value = WeatherUiState.Error(it.toErrorType())
                        return@launch
                    }
                )
            } else {
                // locationRepository 为 null 时回退到硬编码坐标（兼容测试与未注入场景）
                FALLBACK_LAT to FALLBACK_LON
            }

            // 第二步：并行获取位置名称 + 天气数据
            val locationNameDeferred = async {
                resolveLocationName(lat, lon)
            }
            val weatherResult = weatherRepository.fetchWeather(lat, lon)
            val locationName = locationNameDeferred.await()

            _uiState.value = weatherResult.fold(
                onSuccess = { snapshot ->
                    WeatherUiState.Success(
                        snapshot.copy(locationName = locationName)
                    )
                },
                onFailure = { WeatherUiState.Error(it.toErrorType(), isRefreshing = false) }
            )
        }
    }

    /**
     * 解析位置名称：优先反向地理编码，失败时降级为经纬度格式。
     */
    private suspend fun resolveLocationName(lat: Double, lon: Double): String {
        geocodingRepository?.let { repo ->
            val name = repo.getLocationName(lat, lon)
            if (!name.isNullOrBlank()) return name
        }
        // 降级：显示经纬度
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        return "%.2f°%s, %.2f°%s".format(
            kotlin.math.abs(lat), latDir,
            kotlin.math.abs(lon), lonDir
        )
    }

    companion object {
        private const val FALLBACK_LAT = 39.9042
        private const val FALLBACK_LON = 116.4074
    }
}

private fun Throwable.toErrorType(): WeatherUiState.ErrorType = when (this) {
    is NoNetworkException -> WeatherUiState.ErrorType.NO_NETWORK
    is TimeoutException -> WeatherUiState.ErrorType.TIMEOUT
    is com.galaxy.weatherwatch.data.ApiException -> WeatherUiState.ErrorType.API_ERROR
    is GpsUnavailableException -> WeatherUiState.ErrorType.GPS_UNAVAILABLE
    is LocationUnavailableException -> WeatherUiState.ErrorType.GPS_UNAVAILABLE
    is com.galaxy.weatherwatch.data.LocationPermissionDeniedException -> WeatherUiState.ErrorType.PERMISSION_DENIED
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
