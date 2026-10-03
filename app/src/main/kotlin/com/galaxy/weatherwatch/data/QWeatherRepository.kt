package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.model.CurrentWeather
import com.galaxy.weatherwatch.data.model.DailySummary
import com.galaxy.weatherwatch.data.model.HourlyForecast
import com.galaxy.weatherwatch.data.model.WeatherIcon
import com.galaxy.weatherwatch.data.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.time.Duration.Companion.seconds

/**
 * 和风天气（QWeather）实现，当前应用的默认天气数据源。
 *
 * 一次 [fetchWeather] 并行请求三个接口：
 * - `/v7/weather/now`：实时天气
 * - `/v7/weather/24h`：逐时预报（取前 12 小时）
 * - `/v7/weather/3d`：天级预报（取今日最高/最低温）
 *
 * 与 OpenWeatherMap 的差异：
 * - `location` 参数格式为 **经度,纬度**（lon,lat），顺序与 OWM 相反
 * - 各数值字段均为**字符串**类型（如 `"31"`），映射时需转换
 * - 业务状态码在响应体 `code` 字段（`"200"` 表示成功），HTTP 层错误单独判断
 * - 天气图标使用 QWeather 码（100~104 白天 / 150~154 夜间 / 300+ 雨雪雷雾），
 *   见 [WeatherIcon.fromQWeatherCode]
 * - 天级接口不返回降水概率 `pop`，今日降水概率取当日逐时 `pop` 最大值
 *
 * @param apiHost 和风天气控制台分配的专属 API Host（如 `h2a9cf3mhs.xy.qweatherapi.com`），
 *   2026 年起公共域名 `api.qweather.com` / `devapi.qweather.com` 已逐步停用，请求会被拒绝
 */
class QWeatherRepository(
    private val apiKey: String,
    apiHost: String,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val lang: String = "zh"
) : WeatherRepository {

    /** 归一化 Host：容忍用户粘贴时带 scheme 或尾部斜杠。 */
    private val normalizedHost: String = apiHost
        .removePrefix("https://")
        .removePrefix("http://")
        .trimEnd('/')

    override suspend fun fetchWeather(lat: Double, lon: Double): Result<WeatherSnapshot> = runCatching {
        withContext(Dispatchers.IO) {
            withTimeout(15.seconds) {
                val location = "$lon,$lat"
                coroutineScope {
                    val nowDeferred = async { get<QwNowResponse>("weather/now", location) }
                    val hourlyDeferred = async { get<QwHourlyResponse>("weather/24h", location) }
                    val dailyDeferred = async { get<QwDailyResponse>("weather/3d", location) }
                    nowDeferred.await().toSnapshot(
                        hourly = hourlyDeferred.await(),
                        daily = dailyDeferred.await()
                    )
                }
            }
        }
    }

    /** 发起单个 GET 请求并校验 HTTP 状态码与业务状态码。 */
    private suspend inline fun <reified T : QwEnvelope> get(path: String, location: String): T {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host(normalizedHost)
            .addPathSegments("v7/$path")
            .addQueryParameter("location", location)
            .addQueryParameter("key", apiKey)
            .addQueryParameter("lang", lang)
            .build()
        val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
        if (response.code != HttpURLConnection.HTTP_OK) {
            throw ApiException(response.code)
        }
        val payload = json.decodeFromString<T>(response.body?.string().orEmpty())
        if (payload.code != "200") {
            throw ApiException(payload.code.toIntOrNull() ?: -1)
        }
        return payload
    }

    /** 各接口响应都携带业务状态码 `code`，统一在此校验。 */
    private interface QwEnvelope {
        val code: String
    }

    // ---- 内部 JSON 模型：数值均为字符串，与和风天气实际返回一致 ----

    @Serializable
    private data class QwNowResponse(
        override val code: String,
        val now: QwNow
    ) : QwEnvelope

    @Serializable
    private data class QwNow(
        val temp: String,
        @SerialName("feelsLike") val feelsLike: String,
        val icon: String,
        val text: String
    )

    @Serializable
    private data class QwHourlyResponse(
        override val code: String,
        val hourly: List<QwHourly>
    ) : QwEnvelope

    @Serializable
    private data class QwHourly(
        @SerialName("fxTime") val fxTime: String,
        val temp: String,
        val icon: String,
        val text: String,
        /** 降水概率（百分比字符串，如 `"32"` 表示 32%），部分时段可能缺失。 */
        val pop: String? = null
    )

    @Serializable
    private data class QwDailyResponse(
        override val code: String,
        val daily: List<QwDaily>
    ) : QwEnvelope

    @Serializable
    private data class QwDaily(
        @SerialName("fxDate") val fxDate: String,
        @SerialName("tempMax") val tempMax: String,
        @SerialName("tempMin") val tempMin: String
    )

    private companion object {

        /** 将三份响应合并为业务模型 [WeatherSnapshot]。 */
        fun QwNowResponse.toSnapshot(
            hourly: QwHourlyResponse,
            daily: QwDailyResponse
        ): WeatherSnapshot {
            val nowWeather = this.now
            val today = daily.daily.firstOrNull()
            val todayDate = today?.fxDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val todayHourly = hourly.hourly.filter { entry ->
                val date = runCatching { OffsetDateTime.parse(entry.fxTime).toLocalDate() }.getOrNull()
                date != null && date == todayDate
            }
            val todayPrecipProbability = todayHourly.maxOfOrNull { it.pop.toPercent() } ?: 0.0

            return WeatherSnapshot(
                current = CurrentWeather(
                    temp = nowWeather.temp.toDoubleOrNull() ?: 0.0,
                    feelsLike = nowWeather.feelsLike.toDoubleOrNull() ?: 0.0,
                    icon = WeatherIcon.fromQWeatherCode(nowWeather.icon),
                    description = nowWeather.text,
                    updatedAt = System.currentTimeMillis()
                ),
                today = DailySummary(
                    minTemp = today?.tempMin?.toDoubleOrNull() ?: 0.0,
                    maxTemp = today?.tempMax?.toDoubleOrNull() ?: 0.0,
                    precipProbability = todayPrecipProbability
                ),
                hourly = hourly.hourly.take(12).map { entry ->
                    HourlyForecast(
                        epochSecond = runCatching { OffsetDateTime.parse(entry.fxTime).toEpochSecond() }
                            .getOrDefault(0L),
                        temp = entry.temp.toDoubleOrNull() ?: 0.0,
                        icon = WeatherIcon.fromQWeatherCode(entry.icon),
                        precipProbability = entry.pop.toPercent()
                    )
                }
            )
        }

        /** 和风 `pop` 为百分比字符串，转成 0.0~1.0；缺失或非法时为 0.0。 */
        private fun String?.toPercent(): Double = (this?.toDoubleOrNull() ?: 0.0) / 100.0
    }
}
