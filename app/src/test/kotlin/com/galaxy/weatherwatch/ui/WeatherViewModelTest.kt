package com.galaxy.weatherwatch.ui

import app.cash.turbine.test
import com.galaxy.weatherwatch.data.CityRepository
import com.galaxy.weatherwatch.data.GeocodingRepository
import com.galaxy.weatherwatch.data.LocationRepository
import com.galaxy.weatherwatch.data.LocationUnavailableException
import com.galaxy.weatherwatch.data.LocalCityRepository
import com.galaxy.weatherwatch.data.MockWeatherRepository
import com.galaxy.weatherwatch.data.model.City
import com.galaxy.weatherwatch.data.model.LocationResult
import com.galaxy.weatherwatch.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WeatherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is Loading then Success after refresh when repository returns data`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = MockWeatherRepository(simulateDelayMs = 0L)
            val viewModel = WeatherViewModel(weatherRepository = repo)

            viewModel.uiState.test {
                val first = awaitItem()
                assertTrue("initial state should be Loading", first is WeatherUiState.Loading)

                viewModel.refresh()

                val second = awaitItem()
                assertTrue("next state should be Success", second is WeatherUiState.Success)
                assertFalse((second as WeatherUiState.Success).isRefreshing)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh keeps old snapshot and flips isRefreshing in a single state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = MockWeatherRepository(simulateDelayMs = 0L)
            val viewModel = WeatherViewModel(weatherRepository = repo)

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                // 首次刷新
                viewModel.refresh()
                val firstSuccess = awaitItem() as WeatherUiState.Success
                assertFalse(firstSuccess.isRefreshing)

                // 触发手动刷新：不应闪回 Loading，而是保留旧快照并置位 isRefreshing
                viewModel.refresh()

                val refreshing = awaitItem() as WeatherUiState.Success
                assertTrue(refreshing.isRefreshing)
                assertEquals(firstSuccess.snapshot, refreshing.snapshot)

                val settled = awaitItem() as WeatherUiState.Success
                assertFalse(settled.isRefreshing)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `error state surfaces when repository fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = MockWeatherRepository(
                simulateDelayMs = 0L,
                failureMode = MockWeatherRepository.FailureMode.NETWORK
            )
            val viewModel = WeatherViewModel(weatherRepository = repo)

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refresh()

                val terminal = awaitItem()
                assertTrue("terminal state should be Error", terminal is WeatherUiState.Error)
                assertEquals(
                    WeatherUiState.ErrorType.NO_NETWORK,
                    (terminal as WeatherUiState.Error).type
                )
                assertFalse(terminal.isRefreshing)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `retry from error goes back to Loading then Success`() =
        runTest(mainDispatcherRule.testDispatcher) {
            var fail = true
            val repo = object : com.galaxy.weatherwatch.data.WeatherRepository {
                override suspend fun fetchWeather(lat: Double, lon: Double) =
                    if (fail) {
                        Result.failure(com.galaxy.weatherwatch.data.NoNetworkException())
                    } else {
                        MockWeatherRepository(simulateDelayMs = 0L).fetchWeather(lat, lon)
                    }
            }
            val viewModel = WeatherViewModel(weatherRepository = repo)

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refresh()
                assertTrue(awaitItem() is WeatherUiState.Error)

                fail = false
                viewModel.refresh()

                assertTrue("retry should show Loading", awaitItem() is WeatherUiState.Loading)
                assertTrue("then Success", awaitItem() is WeatherUiState.Success)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- 定位相关测试 ----

    @Test
    fun `refreshWithPermissionCheck - 无权限时直接返回 PERMISSION_DENIED`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = WeatherViewModel(
                weatherRepository = MockWeatherRepository(simulateDelayMs = 0L)
            )

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refreshWithPermissionCheck(hasPermission = false)

                val error = awaitItem() as WeatherUiState.Error
                assertEquals(WeatherUiState.ErrorType.PERMISSION_DENIED, error.type)
                assertFalse(error.isRefreshing)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshWithPermissionCheck - 有权限时调用 refresh`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = MockWeatherRepository(simulateDelayMs = 0L)
            val viewModel = WeatherViewModel(weatherRepository = repo)

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refreshWithPermissionCheck(hasPermission = true)

                assertTrue(awaitItem() is WeatherUiState.Success)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh - 有 LocationRepository 时先定位再取天气`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepo = MockWeatherRepository(simulateDelayMs = 0L)
            val locationRepo = object : LocationRepository {
                override suspend fun getCurrentLocation(): Result<LocationResult> =
                    Result.success(
                        LocationResult(
                            latitude = 31.2304,
                            longitude = 121.4737,
                            accuracy = 10f,
                            epochSecond = 1_000_000L
                        )
                    )
            }
            val viewModel = WeatherViewModel(
                weatherRepository = weatherRepo,
                locationRepository = locationRepo
            )

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refresh()

                val success = awaitItem()
                assertTrue("should be Success after location + weather", success is WeatherUiState.Success)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh - 定位失败时状态为 Error GPS_UNAVAILABLE`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepo = MockWeatherRepository(simulateDelayMs = 0L)
            val locationRepo = object : LocationRepository {
                override suspend fun getCurrentLocation(): Result<LocationResult> =
                    Result.failure(LocationUnavailableException())
            }
            val viewModel = WeatherViewModel(
                weatherRepository = weatherRepo,
                locationRepository = locationRepo
            )

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refresh()

                val error = awaitItem() as WeatherUiState.Error
                assertEquals(WeatherUiState.ErrorType.GPS_UNAVAILABLE, error.type)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh - locationRepository 为 null 时回退到硬编码坐标`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepo = MockWeatherRepository(simulateDelayMs = 0L)
            // locationRepository 不传，默认为 null
            val viewModel = WeatherViewModel(weatherRepository = weatherRepo)

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refresh()

                // 应该成功返回数据，使用回退坐标
                assertTrue(awaitItem() is WeatherUiState.Success)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- 地理编码相关测试 ----

    @Test
    fun `refresh - 地理编码成功时使用真实位置名称`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepo = MockWeatherRepository(simulateDelayMs = 0L)
            val locationRepo = object : LocationRepository {
                override suspend fun getCurrentLocation(): Result<LocationResult> =
                    Result.success(
                        LocationResult(31.2304, 121.4737, 10f, 1_000_000L)
                    )
            }
            val geocodingRepo = object : GeocodingRepository {
                override suspend fun getLocationName(lat: Double, lon: Double): String? =
                    "上海市 黄浦区"
            }
            val viewModel = WeatherViewModel(
                weatherRepository = weatherRepo,
                locationRepository = locationRepo,
                geocodingRepository = geocodingRepo
            )

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refresh()

                val success = awaitItem() as WeatherUiState.Success
                assertEquals("上海市 黄浦区", success.snapshot.locationName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh - 地理编码失败时降级显示经纬度`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepo = MockWeatherRepository(simulateDelayMs = 0L)
            val locationRepo = object : LocationRepository {
                override suspend fun getCurrentLocation(): Result<LocationResult> =
                    Result.success(
                        LocationResult(31.2304, 121.4737, 10f, 1_000_000L)
                    )
            }
            val geocodingRepo = object : GeocodingRepository {
                override suspend fun getLocationName(lat: Double, lon: Double): String? = null
            }
            val viewModel = WeatherViewModel(
                weatherRepository = weatherRepo,
                locationRepository = locationRepo,
                geocodingRepository = geocodingRepo
            )

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refresh()

                val success = awaitItem() as WeatherUiState.Success
                // 降级为经纬度格式
                assertTrue(
                    "locationName should be lat/lon format",
                    success.snapshot.locationName.contains("°N") && success.snapshot.locationName.contains("°E")
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh - geocodingRepository 为 null 时降级显示经纬度`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepo = MockWeatherRepository(simulateDelayMs = 0L)
            val locationRepo = object : LocationRepository {
                override suspend fun getCurrentLocation(): Result<LocationResult> =
                    Result.success(
                        LocationResult(-23.5505, -46.6333, 10f, 1_000_000L)
                    )
            }
            // geocodingRepository 不传，默认为 null
            val viewModel = WeatherViewModel(
                weatherRepository = weatherRepo,
                locationRepository = locationRepo
            )

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)

                viewModel.refresh()

                val success = awaitItem() as WeatherUiState.Success
                // 南纬西经格式
                assertTrue(
                    "locationName should show S and W",
                    success.snapshot.locationName.contains("°S") && success.snapshot.locationName.contains("°W")
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- 城市搜索相关测试 ----

    @Test
    fun `openCitySearch 显示搜索页并加载全部城市`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = WeatherViewModel(
                weatherRepository = MockWeatherRepository(simulateDelayMs = 0L),
                cityRepository = LocalCityRepository()
            )

            viewModel.showCitySearch.test {
                assertFalse("初始应不显示城市搜索", awaitItem())

                viewModel.openCitySearch()

                assertTrue("openCitySearch 后应显示城市搜索", awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            viewModel.citySearchResults.test {
                // 打开搜索后第一个结果就是全部城市
                val cities = expectMostRecentItem()
                assertEquals(5, cities.size)
            }
        }

    @Test
    fun `closeCitySearch 关闭搜索页`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = WeatherViewModel(
                weatherRepository = MockWeatherRepository(simulateDelayMs = 0L),
                cityRepository = LocalCityRepository()
            )

            viewModel.showCitySearch.test {
                assertFalse(awaitItem())

                viewModel.openCitySearch()
                assertTrue(awaitItem())

                viewModel.closeCitySearch()
                assertFalse("closeCitySearch 后应关闭", awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `searchCity 根据关键字筛选城市`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = WeatherViewModel(
                weatherRepository = MockWeatherRepository(simulateDelayMs = 0L),
                cityRepository = LocalCityRepository()
            )

            viewModel.openCitySearch()

            viewModel.citySearchResults.test {
                // 初始全部城市
                val allCities = expectMostRecentItem()
                assertEquals(5, allCities.size)

                // 搜索「禅城」
                viewModel.searchCity("禅城")
                val result1 = expectMostRecentItem()
                assertEquals(1, result1.size)
                assertEquals("佛山市 禅城区", result1[0].name)

                // 搜索拼音 nanhai
                viewModel.searchCity("nanhai")
                val result2 = expectMostRecentItem()
                assertEquals(1, result2.size)
                assertEquals("佛山市 南海区", result2[0].name)

                // 搜索缩写 fs
                viewModel.searchCity("fs")
                val result3 = expectMostRecentItem()
                assertEquals(5, result3.size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `selectCity 关闭搜索页并刷新指定城市天气`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepo = MockWeatherRepository(simulateDelayMs = 0L)
            val testCity = City(
                name = "佛山市 顺德区",
                pinyin = "shunde",
                abbreviation = "sdq",
                latitude = 22.8400,
                longitude = 113.2800
            )
            val viewModel = WeatherViewModel(
                weatherRepository = weatherRepo,
                cityRepository = LocalCityRepository()
            )

            viewModel.uiState.test {
                // 初始状态
                assertTrue(awaitItem() is WeatherUiState.Loading)

                // 打开城市搜索
                viewModel.openCitySearch()

                // 选择城市：应触发天气刷新
                viewModel.selectCity(testCity)

                // 刷新中（Loading 态，因为初始就是 Loading，refreshWithCoordinates 不改变状态）
                // 直接等 Success
                val success = awaitItem() as WeatherUiState.Success
                assertEquals("佛山市 顺德区", success.snapshot.locationName)
                cancelAndIgnoreRemainingEvents()
            }

            // 验证搜索页已关闭
            viewModel.showCitySearch.test {
                assertFalse("selectCity 后应关闭搜索页", expectMostRecentItem())
            }
        }

    @Test
    fun `useGpsLocation 关闭搜索页并触发正常刷新`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val weatherRepo = MockWeatherRepository(simulateDelayMs = 0L)
            val locationRepo = object : LocationRepository {
                override suspend fun getCurrentLocation(): Result<LocationResult> =
                    Result.success(
                        LocationResult(31.2304, 121.4737, 10f, 1_000_000L)
                    )
            }
            val geocodingRepo = object : GeocodingRepository {
                override suspend fun getLocationName(lat: Double, lon: Double): String? =
                    "上海市 黄浦区"
            }
            val viewModel = WeatherViewModel(
                weatherRepository = weatherRepo,
                locationRepository = locationRepo,
                geocodingRepository = geocodingRepo,
                cityRepository = LocalCityRepository()
            )

            // 先打开搜索页
            viewModel.openCitySearch()

            viewModel.showCitySearch.test {
                assertTrue(awaitItem()) // 打开状态

                // 点击 GPS 定位
                viewModel.useGpsLocation()

                // 应关闭搜索页
                assertFalse("useGpsLocation 后应关闭搜索页", awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `cityRepository 为 null 时城市搜索功能不崩溃`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = WeatherViewModel(
                weatherRepository = MockWeatherRepository(simulateDelayMs = 0L)
                // cityRepository 不传，默认为 null
            )

            // 这些操作不应抛异常
            viewModel.openCitySearch()
            viewModel.searchCity("测试")

            // 打开搜索后 showCitySearch 应为 true（不受 cityRepository 影响）
            viewModel.showCitySearch.test {
                assertTrue("openCitySearch 后 showCitySearch 应为 true", expectMostRecentItem())
            }

            // 搜索结果为空列表（因为 cityRepository 为 null）
            viewModel.citySearchResults.test {
                assertTrue("cityRepository 为 null 时搜索结果为空", expectMostRecentItem().isEmpty())
            }

            // 关闭也不应崩溃
            viewModel.closeCitySearch()
            viewModel.showCitySearch.test {
                assertFalse("closeCitySearch 后应为 false", expectMostRecentItem())
            }
        }
}
