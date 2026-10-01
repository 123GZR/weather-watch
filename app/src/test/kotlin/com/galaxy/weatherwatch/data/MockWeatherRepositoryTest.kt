package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.MockWeatherRepository.FailureMode
import com.galaxy.weatherwatch.data.model.WeatherIcon
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockWeatherRepositoryTest {

    private val repo = MockWeatherRepository(simulateDelayMs = 0L)

    @Test
    fun `fetchWeather returns snapshot with current weather`() = runTest {
        val result = repo.fetchWeather(lat = 39.9, lon = 116.4)

        assertTrue(result.isSuccess)
        val snapshot = result.getOrThrow()
        assertEquals(18.5, snapshot.current.temp, 0.01)
        assertEquals(17.2, snapshot.current.feelsLike, 0.01)
        assertEquals(WeatherIcon.FEW_CLOUDS_DAY, snapshot.current.icon)
    }

    @Test
    fun `fetchWeather returns today summary with min max and pop`() = runTest {
        val snapshot = repo.fetchWeather(0.0, 0.0).getOrThrow()

        assertEquals(12.0, snapshot.today.minTemp, 0.01)
        assertEquals(22.0, snapshot.today.maxTemp, 0.01)
        assertEquals(0.2, snapshot.today.precipProbability, 0.001)
    }

    @Test
    fun `fetchWeather returns 12 hourly forecasts`() = runTest {
        val snapshot = repo.fetchWeather(0.0, 0.0).getOrThrow()

        assertEquals(12, snapshot.hourly.size)
        snapshot.hourly.forEach { item ->
            assertTrue(item.epochSecond > 0)
            assertTrue(item.precipProbability in 0.0..1.0)
        }
    }

    @Test
    fun `hourly timestamps increase by one hour`() = runTest {
        val snapshot = repo.fetchWeather(0.0, 0.0).getOrThrow()

        for (i in 1 until snapshot.hourly.size) {
            val delta = snapshot.hourly[i].epochSecond - snapshot.hourly[i - 1].epochSecond
            assertEquals(3600L, delta)
        }
    }

    @Test
    fun `network failure mode returns NoNetworkException`() = runTest {
        val failingRepo = MockWeatherRepository(simulateDelayMs = 0L, failureMode = FailureMode.NETWORK)
        val result = failingRepo.fetchWeather(0.0, 0.0)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoNetworkException)
    }

    @Test
    fun `timeout failure mode returns TimeoutException`() = runTest {
        val failingRepo = MockWeatherRepository(simulateDelayMs = 0L, failureMode = FailureMode.TIMEOUT)
        val result = failingRepo.fetchWeather(0.0, 0.0)

        assertTrue(result.exceptionOrNull() is TimeoutException)
    }

    @Test
    fun `api failure mode returns ApiException`() = runTest {
        val failingRepo = MockWeatherRepository(simulateDelayMs = 0L, failureMode = FailureMode.API)
        val result = failingRepo.fetchWeather(0.0, 0.0)

        val ex = result.exceptionOrNull()
        assertTrue(ex is ApiException)
        assertEquals(500, (ex as ApiException).httpCode)
    }

    @Test
    fun `gps failure mode returns GpsUnavailableException`() = runTest {
        val failingRepo = MockWeatherRepository(simulateDelayMs = 0L, failureMode = FailureMode.GPS)
        val result = failingRepo.fetchWeather(0.0, 0.0)

        assertTrue(result.exceptionOrNull() is GpsUnavailableException)
    }
}
