package com.galaxy.weatherwatch.ui

import app.cash.turbine.test
import com.galaxy.weatherwatch.data.MockWeatherRepository
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
    fun `initial state is Loading then Success when repository returns data`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repo = MockWeatherRepository(simulateDelayMs = 0L)
            val viewModel = WeatherViewModel(repository = repo)

            viewModel.uiState.test {
                val first = awaitItem()
                assertTrue("initial state should be Loading", first is WeatherUiState.Loading)

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
            val viewModel = WeatherViewModel(repository = repo)

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)
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
            val viewModel = WeatherViewModel(repository = repo)

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)
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
            val viewModel = WeatherViewModel(repository = repo)

            viewModel.uiState.test {
                assertTrue(awaitItem() is WeatherUiState.Loading)
                assertTrue(awaitItem() is WeatherUiState.Error)

                fail = false
                viewModel.refresh()

                assertTrue("retry should show Loading", awaitItem() is WeatherUiState.Loading)
                assertTrue("then Success", awaitItem() is WeatherUiState.Success)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
