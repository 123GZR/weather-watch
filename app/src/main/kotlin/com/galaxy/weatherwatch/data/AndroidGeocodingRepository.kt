package com.galaxy.weatherwatch.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * 基于 Android 原生 Geocoder 的反向地理编码实现。
 *
 * 注意：
 * - Geocoder 需要网络连接，且在部分 Wear OS 设备上可能不可用
 * - API 33+ 使用 Geocoder.getFromLocation(lat, lon, maxResults, listener) 异步回调
 * - API 33 以下使用阻塞式 getFromLocation(lat, lon, maxResults)
 * - 失败时返回 null，由调用方决定降级策略
 */
class AndroidGeocodingRepository(
    private val context: Context,
    private val dispatcher: CoroutineContext = Dispatchers.IO
) : GeocodingRepository {

    override suspend fun getLocationName(lat: Double, lon: Double): String? =
        withContext(dispatcher) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // API 33+：使用异步 listener
                    getFromLocationAsync(geocoder, lat, lon)
                } else {
                    // API 33 以下：阻塞式调用（已在 IO 线程）
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    addresses?.firstOrNull()?.let { formatAddress(it) }
                }
            } catch (e: IOException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        }

    private suspend fun getFromLocationAsync(
        geocoder: Geocoder,
        lat: Double,
        lon: Double
    ): String? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lon, 1) { addresses ->
                    val name = addresses.firstOrNull()?.let { formatAddress(it) }
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.success(name))
                    }
                }
            } else {
                continuation.resumeWith(Result.success(null))
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWith(Result.success(null))
            }
        }
    }

    /**
     * 格式化地址为「城市 区/县」形式。
     * 优先顺序：locality（城市）+ subLocality（区）→ 仅城市 → 仅行政区 → null
     */
    private fun formatAddress(address: Address): String? {
        val city = address.locality ?: address.adminArea ?: return null
        val district = address.subLocality ?: address.subAdminArea

        return if (district != null && district != city) {
            "$city $district"
        } else {
            city
        }
    }
}
