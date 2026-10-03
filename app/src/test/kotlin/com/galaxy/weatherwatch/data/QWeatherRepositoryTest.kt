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
import java.time.OffsetDateTime

/**
 * 覆盖 QWeatherRepository 的 HTTP 状态码处理、业务状态码校验与 JSON 映射。
 *
 * 与 OpenWeatherRepositoryTest 相同，通过 OkHttp 拦截器伪造响应，
 * 按路径返回不同接口（now / 24h / 3d）的固定 JSON，不发起真实网络请求。
 */
class QWeatherRepositoryTest {

    /**
     * 三个接口在 Dispatchers.IO 上并行请求，记录列表必须线程安全，
     * 否则普通 ArrayList 并发写入会丢元素。
     */
    private val recordedUrls = java.util.concurrent.CopyOnWriteArrayList<HttpUrl>()

    private fun clientReturning(vararg pathToBody: Pair<String, String>): OkHttpClient {
        val bodyByPath = pathToBody.toMap()
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val url = chain.request().url
                recordedUrls += url
                val body = bodyByPath[url.encodedPath] ?: "{}"
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("stub")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }

    private fun repo(client: OkHttpClient, key: String = "test-key", host: String = "k76aq2e5g7.re.qweatherapi.com") =
        QWeatherRepository(apiKey = key, apiHost = host, httpClient = client)

    @Test
    fun `fetchWeather maps payload when API returns 200`() = runTest {
        val client = clientReturning(
            "/v7/weather/now" to NOW_BODY,
            "/v7/weather/24h" to HOURLY_BODY,
            "/v7/weather/3d" to DAILY_BODY
        )

        val snapshot = repo(client).fetchWeather(lat = 39.9042, lon = 116.4074).getOrThrow()

        // 实时天气
        assertEquals(31.0, snapshot.current.temp, 0.001)
        assertEquals(34.0, snapshot.current.feelsLike, 0.001)
        assertEquals(WeatherIcon.BROKEN_CLOUDS_DAY, snapshot.current.icon)
        assertEquals("阴", snapshot.current.description)
        assertTrue(snapshot.current.updatedAt > 0)

        // 今日摘要：min/max 来自 3d 接口，降水概率取当日逐时 pop 最大值（70%）
        assertEquals(26.0, snapshot.today.minTemp, 0.001)
        assertEquals(31.0, snapshot.today.maxTemp, 0.001)
        assertEquals(0.70, snapshot.today.precipProbability, 0.001)

        // 逐时预报：按 fxTime 顺序保留，pop 百分比转 0.0~1.0
        assertEquals(3, snapshot.hourly.size)
        assertEquals(
            OffsetDateTime.parse("2026-10-03T16:00+08:00").toEpochSecond(),
            snapshot.hourly[0].epochSecond
        )
        assertEquals(30.0, snapshot.hourly[0].temp, 0.001)
        assertEquals(WeatherIcon.SCATTERED_CLOUDS_DAY, snapshot.hourly[0].icon)
        assertEquals(0.32, snapshot.hourly[0].precipProbability, 0.001)

        assertEquals(WeatherIcon.THUNDERSTORM_DAY, snapshot.hourly[1].icon)
        assertEquals(0.70, snapshot.hourly[1].precipProbability, 0.001)

        // 次日（fxDate 不匹配今日）的逐时不参与今日降水概率，但仍在逐时列表中
        assertEquals(WeatherIcon.CLEAR_DAY, snapshot.hourly[2].icon)
        assertEquals(0.51, snapshot.hourly[2].precipProbability, 0.001)
    }

    @Test
    fun `fetchWeather builds request url with lon-lat key lang and api host`() = runTest {
        val client = clientReturning(
            "/v7/weather/now" to NOW_BODY,
            "/v7/weather/24h" to HOURLY_BODY,
            "/v7/weather/3d" to DAILY_BODY
        )

        repo(client).fetchWeather(lat = 39.9042, lon = 116.4074).getOrThrow()

        val paths = recordedUrls.map { it.encodedPath }
        assertEquals(3, recordedUrls.size)
        assertTrue("/v7/weather/now not requested", paths.contains("/v7/weather/now"))
        assertTrue("/v7/weather/24h not requested", paths.contains("/v7/weather/24h"))
        assertTrue("/v7/weather/3d not requested", paths.contains("/v7/weather/3d"))
        recordedUrls.forEach { url ->
            assertEquals("k76aq2e5g7.re.qweatherapi.com", url.host)
            assertEquals("116.4074,39.9042", url.queryParameter("location"))
            assertEquals("test-key", url.queryParameter("key"))
            assertEquals("zh", url.queryParameter("lang"))
        }
    }

    @Test
    fun `fetchWeather strips scheme and trailing slash from api host`() = runTest {
        val client = clientReturning(
            "/v7/weather/now" to NOW_BODY,
            "/v7/weather/24h" to HOURLY_BODY,
            "/v7/weather/3d" to DAILY_BODY
        )

        repo(client, host = "https://k76aq2e5g7.re.qweatherapi.com/").fetchWeather(0.0, 0.0).getOrThrow()

        assertTrue(recordedUrls.isNotEmpty())
        assertEquals("k76aq2e5g7.re.qweatherapi.com", recordedUrls.first().host)
    }

    @Test
    fun `fetchWeather returns ApiException when HTTP status is non-200`() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("""{"error":{"status":401}}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val result = repo(client).fetchWeather(0.0, 0.0)

        assertTrue("expected failure but got $result", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("expected ApiException but got $error", error is ApiException)
        assertEquals(401, (error as ApiException).httpCode)
    }

    @Test
    fun `fetchWeather returns ApiException when business code is not 200`() = runTest {
        // HTTP 200 但业务 code != 200（如 204 无数据），也应视为失败
        val client = clientReturning(
            "/v7/weather/now" to """{"code":"204","now":{}}""",
            "/v7/weather/24h" to """{"code":"204","hourly":[]}""",
            "/v7/weather/3d" to """{"code":"204","daily":[]}"""
        )

        val result = repo(client).fetchWeather(0.0, 0.0)

        assertTrue("expected failure but got $result", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue("expected ApiException but got $error", error is ApiException)
        assertEquals(204, (error as ApiException).httpCode)
    }

    @Test
    fun `fetchWeather returns failure on malformed json`() = runTest {
        val client = clientReturning(
            "/v7/weather/now" to "not-json",
            "/v7/weather/24h" to "not-json",
            "/v7/weather/3d" to "not-json"
        )

        val result = repo(client).fetchWeather(0.0, 0.0)

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

        val result = repo(failingClient).fetchWeather(0.0, 0.0)

        assertTrue("expected failure but got $result", result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    private companion object {
        const val NOW_BODY = """
        {
          "code": "200",
          "updateTime": "2026-10-03T15:32+08:00",
          "now": {
            "obsTime": "2026-10-03T15:25+08:00",
            "temp": "31",
            "feelsLike": "34",
            "icon": "104",
            "text": "阴",
            "wind360": "180",
            "humidity": "64"
          },
          "refer": {"sources": ["QWeather"]}
        }
        """

        const val HOURLY_BODY = """
        {
          "code": "200",
          "updateTime": "2026-10-03T15:28+08:00",
          "hourly": [
            {
              "fxTime": "2026-10-03T16:00+08:00",
              "temp": "30",
              "icon": "101",
              "text": "多云",
              "windDir": "东南风",
              "pop": "32"
            },
            {
              "fxTime": "2026-10-03T18:00+08:00",
              "temp": "29",
              "icon": "302",
              "text": "雷阵雨",
              "pop": "70"
            },
            {
              "fxTime": "2026-10-04T08:00+08:00",
              "temp": "27",
              "icon": "100",
              "text": "晴",
              "pop": "51"
            }
          ]
        }
        """

        const val DAILY_BODY = """
        {
          "code": "200",
          "updateTime": "2026-10-03T15:28+08:00",
          "daily": [
            {
              "fxDate": "2026-10-03",
              "sunrise": "06:20",
              "sunset": "18:15",
              "tempMax": "31",
              "tempMin": "26",
              "textDay": "雷阵雨"
            },
            {
              "fxDate": "2026-10-04",
              "tempMax": "29",
              "tempMin": "23"
            }
          ]
        }
        """
    }
}
