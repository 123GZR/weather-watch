package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.model.WeatherIcon
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 覆盖 OpenWeatherRepository 的 HTTP 状态码处理与 JSON 映射。
 *
 * 该文件是回归测试：仓库早期引用了不存在的 `okhttp3.HttpURLConnection`，
 * 导致 `compileDebugKotlin` 直接编译失败；这里通过真实构造 OkHttpClient
 * （用拦截器伪造响应，不发起网络请求）确保状态码比较使用 java.net.HttpURLConnection。
 */
class OpenWeatherRepositoryTest {

    private var lastRequestUrl: HttpUrl? = null

    private fun clientReturning(code: Int, body: String): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                lastRequestUrl = chain.request().url
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("stub")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

    @Test
    fun `fetchWeather maps payload when API returns 200`() = runTest {
        val repo = OpenWeatherRepository(
            apiKey = "test-key",
            httpClient = clientReturning(200, SUCCESS_BODY)
        )

        val snapshot = repo.fetchWeather(lat = 39.9042, lon = 116.4074).getOrThrow()

        assertEquals(18.5, snapshot.current.temp, 0.001)
        assertEquals(17.2, snapshot.current.feelsLike, 0.001)
        assertEquals(WeatherIcon.FEW_CLOUDS_DAY, snapshot.current.icon)
        assertEquals("少云", snapshot.current.description)
        assertTrue(snapshot.current.updatedAt > 0)

        assertEquals(12.0, snapshot.today.minTemp, 0.001)
        assertEquals(22.0, snapshot.today.maxTemp, 0.001)
        assertEquals(0.2, snapshot.today.precipProbability, 0.001)

        assertEquals(1, snapshot.hourly.size)
        assertEquals(1_700_000_000L, snapshot.hourly[0].epochSecond)
        assertEquals(WeatherIcon.RAIN_DAY, snapshot.hourly[0].icon)
        assertEquals(0.4, snapshot.hourly[0].precipProbability, 0.001)
    }

    @Test
    fun `fetchWeather builds request url with coordinates units and api key`() = runTest {
        val repo = OpenWeatherRepository(
            apiKey = "my-key",
            httpClient = clientReturning(200, SUCCESS_BODY)
        )

        repo.fetchWeather(lat = 39.9042, lon = 116.4074).getOrThrow()

        val url = requireNotNull(lastRequestUrl) { "request was not issued" }
        assertEquals("39.9042", url.queryParameter("lat"))
        assertEquals("116.4074", url.queryParameter("lon"))
        assertEquals("metric", url.queryParameter("units"))
        assertEquals("my-key", url.queryParameter("appid"))
        assertEquals("minutely,alerts", url.queryParameter("exclude"))
    }

    @Test
    fun `fetchWeather returns ApiException with status code when API returns non-200`() = runTest {
        val repo = OpenWeatherRepository(
            apiKey = "test-key",
            httpClient = clientReturning(401, """{"cod":401,"message":"Invalid API key"}""")
        )

        val result = repo.fetchWeather(0.0, 0.0)

        assertTrue("expected failure but got $result", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("expected ApiException but got $error", error is ApiException)
        assertEquals(401, (error as ApiException).httpCode)
    }

    @Test
    fun `fetchWeather returns failure on malformed json`() = runTest {
        val repo = OpenWeatherRepository(
            apiKey = "test-key",
            httpClient = clientReturning(200, "not-json")
        )

        val result = repo.fetchWeather(0.0, 0.0)

        assertTrue("expected failure but got $result", result.isFailure)
        assertTrue(
            "expected serialization error but got ${result.exceptionOrNull()}",
            result.exceptionOrNull() !is ApiException
        )
    }

    @Test
    fun `fetchWeather returns failure when http client throws`() = runTest {
        val failingClient = OkHttpClient.Builder()
            .addInterceptor { throw IOException("network down") }
            .build()
        val repo = OpenWeatherRepository(apiKey = "test-key", httpClient = failingClient)

        val result = repo.fetchWeather(0.0, 0.0)

        assertTrue("expected failure but got $result", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    private companion object {
        const val SUCCESS_BODY = """
        {
          "current": {
            "temp": 18.5,
            "feels_like": 17.2,
            "weather": [{"id": 801, "main": "Clouds", "description": "少云", "icon": "02d"}]
          },
          "hourly": [
            {
              "dt": 1700000000,
              "temp": 16.0,
              "pop": 0.4,
              "weather": [{"id": 500, "main": "Rain", "description": "小雨", "icon": "10d"}]
            }
          ],
          "daily": [
            {
              "dt": 1700000000,
              "temp": {"min": 12.0, "max": 22.0},
              "pop": 0.2,
              "weather": [{"id": 801, "main": "Clouds", "description": "少云", "icon": "02d"}]
            }
          ]
        }
        """
    }
}
