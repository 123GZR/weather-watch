package com.galaxy.weatherwatch.ui

import app.cash.turbine.test
import com.galaxy.weatherwatch.data.MockWeatherRepository
import com.galaxy.weatherwatch.data.WeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherViewModelTest {

    @Test
    fun `initial state is Loading then Success when repository returns data`() = runTest {
        val repo = MockWeatherRepository(simulateDelayMs = 0L)
        val viewModel = WeatherViewModel(repository = repo)

        viewModel.uiState.test {
            val first = awaitItem()
            assertTrue("initial state should be Loading", first is WeatherUiState.Loading)

            val second = awaitItem()
            assertTrue("next state should be Success", second is WeatherUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state surfaces when repository fails`() = runTest {
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
            cancelAndIgnoreRemainingEvents()
        }
    }
}
