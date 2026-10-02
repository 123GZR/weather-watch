package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.model.CurrentWeather
import com.galaxy.weatherwatch.data.model.DailySummary
import com.galaxy.weatherwatch.data.model.HourlyForecast
import com.galaxy.weatherwatch.data.model.WeatherIcon
import com.galaxy.weatherwatch.data.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import kotlin.time.Duration.Companion.seconds

/**
 * OpenWeatherMap One Call API 3.0 实现。
 * TODO(里程碑3)：注入真实 API Key 后启用。
 */
class OpenWeatherRepository(
    private val apiKey: String,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val endpoint: String = "https://api.openweathermap.org/data/3.0/onecall"
) : WeatherRepository {

    override suspend fun fetchWeather(lat: Double, lon: Double): Result<WeatherSnapshot> = runCatching {
        withContext(Dispatchers.IO) {
            withTimeout(15.seconds) {
                val url = buildString {
                    append(endpoint)
                    append("?lat=").append(lat)
                    append("&lon=").append(lon)
                    append("&units=metric")
                    append("&lang=zh_cn")
                    append("&appid=").append(apiKey)
                    append("&exclude=minutely,alerts")
                }
                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                if (response.code != HttpURLConnection.HTTP_OK) {
                    throw ApiException(response.code)
                }
                val body = response.body?.string().orEmpty()
                json.decodeFromString(OpenWeatherResponse.serializer(), body).toSnapshot()
            }
        }
    }

    @kotlinx.serialization.Serializable
    private data class OpenWeatherResponse(
        val current: OwmCurrent,
        val hourly: List<OwmHourly>,
        val daily: List<OwmDaily>
    ) {
        fun toSnapshot(): WeatherSnapshot {
            val now = System.currentTimeMillis()
            val current = CurrentWeather(
                temp = current.temp,
                feelsLike = current.feelsLike,
                icon = WeatherIcon.fromCode(current.weather.firstOrNull()?.icon),
                description = current.weather.firstOrNull()?.description.orEmpty(),
                updatedAt = now
            )
            val today = daily.firstOrNull()?.let {
                DailySummary(
                    minTemp = it.temp.min,
                    maxTemp = it.temp.max,
                    precipProbability = it.pop
                )
            } ?: DailySummary(0.0, 0.0, 0.0)
            val hourly = hourly.take(12).map {
                HourlyForecast(
                    epochSecond = it.dt,
                    temp = it.temp,
                    icon = WeatherIcon.fromCode(it.weather.firstOrNull()?.icon),
                    precipProbability = it.pop
                )
            }
            return WeatherSnapshot(current = current, today = today, hourly = hourly)
        }
    }

    @kotlinx.serialization.Serializable
    private data class OwmCurrent(
        val temp: Double,
        @kotlinx.serialization.SerialName("feels_like") val feelsLike: Double,
        val weather: List<OwmWeather>
    )

    @kotlinx.serialization.Serializable
    private data class OwmHourly(
        val dt: Long,
        val temp: Double,
        val pop: Double = 0.0,
        val weather: List<OwmWeather>
    )

    @kotlinx.serialization.Serializable
    private data class OwmDaily(
        val dt: Long,
        val temp: OwmDailyTemp,
        val pop: Double = 0.0,
        val weather: List<OwmWeather>
    )

    @kotlinx.serialization.Serializable
    private data class OwmDailyTemp(val min: Double, val max: Double)

    @kotlinx.serialization.Serializable
    private data class OwmWeather(
        val id: Int,
        val main: String,
        val description: String,
        val icon: String
    )
}
