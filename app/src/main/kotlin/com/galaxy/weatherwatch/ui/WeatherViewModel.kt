package com.galaxy.weatherwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxy.weatherwatch.data.CityRepository
import com.galaxy.weatherwatch.data.CitySelectionRepository
import com.galaxy.weatherwatch.data.GeocodingRepository
import com.galaxy.weatherwatch.data.GpsUnavailableException
import com.galaxy.weatherwatch.data.LocationRepository
import com.galaxy.weatherwatch.data.LocationUnavailableException
import com.galaxy.weatherwatch.data.NoNetworkException
import com.galaxy.weatherwatch.data.TimeoutException
import com.galaxy.weatherwatch.data.WeatherRepository
import com.galaxy.weatherwatch.data.model.City
import com.galaxy.weatherwatch.data.model.WeatherSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val locationRepository: LocationRepository,
    private val geocodingRepository: GeocodingRepository? = null,
    private val cityRepository: CityRepository? = null,
    private val citySelectionRepository: CitySelectionRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    /** 是否显示城市搜索页 */
    private val _showCitySearch = MutableStateFlow(false)
    val showCitySearch: StateFlow<Boolean> = _showCitySearch.asStateFlow()

    /** 城市搜索结果列表 */
    private val _citySearchResults = MutableStateFlow<List<City>>(emptyList())
    val citySearchResults: StateFlow<List<City>> = _citySearchResults.asStateFlow()

    private var refreshJob: Job? = null

    /** 最近一次成功获取的位置名称，供失败页展示兜底（失败时仍可看到当前位置） */
    private var lastLocationName: String = ""

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
                isRefreshing = false,
                locationName = lastLocationName
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
            locationRepository.getCurrentLocation().fold(
                onSuccess = { location ->
                    loadWeatherByGps(location.latitude, location.longitude)
                },
                onFailure = { throwable ->
                    // 定位失败：若存在记忆城市，回退到最近一次手动选择的城市取天气（成功态），
                    // 避免手表室内无 GPS 信号时直接进入错误页；无记忆城市时维持错误态。
                    val rememberedCity = citySelectionRepository?.getSelectedCity()
                    if (rememberedCity != null) {
                        applyWeatherResult(
                            lat = rememberedCity.latitude,
                            lon = rememberedCity.longitude,
                            locationName = rememberedCity.name
                        )
                    } else {
                        _uiState.value = WeatherUiState.Error(
                            type = throwable.toErrorType(),
                            locationName = lastLocationName
                        )
                    }
                }
            )
        }
    }

    /**
     * GPS/坐标路径：并行解析位置名称 + 获取天气，位置名称解析失败时降级为经纬度。
     */
    private suspend fun loadWeatherByGps(lat: Double, lon: Double) {
        // coroutineScope 保证内部 async 与外层协程同生命周期，名称解析与天气请求并行执行
        val (weatherResult, locationName) = coroutineScope {
            val locationNameDeferred = async { resolveLocationName(lat, lon) }
            val result = weatherRepository.fetchWeather(lat, lon)
            result to locationNameDeferred.await()
        }
        lastLocationName = locationName
        _uiState.value = weatherResult.fold(
            onSuccess = { snapshot ->
                WeatherUiState.Success(
                    snapshot.copy(locationName = locationName)
                )
            },
            onFailure = {
                WeatherUiState.Error(
                    type = it.toErrorType(),
                    isRefreshing = false,
                    locationName = locationName
                )
            }
        )
    }

    /**
     * 使用已知位置名称获取天气并更新状态。
     * 供「手动选择城市」与「定位失败回退记忆城市」两条路径共用。
     */
    private suspend fun applyWeatherResult(lat: Double, lon: Double, locationName: String) {
        val weatherResult = weatherRepository.fetchWeather(lat, lon)
        lastLocationName = locationName

        _uiState.value = weatherResult.fold(
            onSuccess = { snapshot ->
                WeatherUiState.Success(
                    snapshot.copy(locationName = locationName)
                )
            },
            onFailure = {
                WeatherUiState.Error(
                    type = it.toErrorType(),
                    isRefreshing = false,
                    locationName = locationName
                )
            }
        )
    }

    // ---- 城市搜索相关 ----

    /** 打开城市搜索页，初始化搜索结果为全部城市 */
    fun openCitySearch() {
        cityRepository?.let { repo ->
            _citySearchResults.value = repo.getAllCities()
        }
        _showCitySearch.value = true
    }

    /** 关闭城市搜索页 */
    fun closeCitySearch() {
        _showCitySearch.value = false
    }

    /**
     * 根据关键字搜索城市。
     * 支持中文名、全拼、拼音首字母缩写匹配。
     */
    fun searchCity(query: String) {
        cityRepository?.let { repo ->
            _citySearchResults.value = repo.searchCities(query)
        }
    }

    /**
     * 选择指定城市并获取该城市天气，然后关闭搜索页。
     * 同时把该城市记忆到本地，供后续定位失败时回退使用。
     */
    fun selectCity(city: City) {
        _showCitySearch.value = false
        citySelectionRepository?.saveSelectedCity(city)
        refreshWithCoordinates(city.latitude, city.longitude, city.name)
    }

    /**
     * 使用 GPS 定位获取天气，然后关闭搜索页。
     * 若定位不可用则走正常 refresh 流程（含错误处理）。
     */
    fun useGpsLocation() {
        _showCitySearch.value = false
        refresh()
    }

    /**
     * 使用指定坐标和位置名称刷新天气，跳过定位步骤。
     * 用于手动选择城市时的天气获取。
     */
    private fun refreshWithCoordinates(lat: Double, lon: Double, locationName: String) {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update { previous ->
                when (previous) {
                    is WeatherUiState.Success -> previous.copy(isRefreshing = true)
                    else -> WeatherUiState.Loading
                }
            }
            applyWeatherResult(lat, lon, locationName)
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
        val isRefreshing: Boolean = false,
        /** 失败页展示的位置名称（最后一次成功定位/手动选择的位置，可能为空） */
        val locationName: String = ""
    ) : WeatherUiState

    enum class ErrorType {
        PERMISSION_DENIED,
        NO_NETWORK,
        TIMEOUT,
        API_ERROR,
        GPS_UNAVAILABLE
    }
}
