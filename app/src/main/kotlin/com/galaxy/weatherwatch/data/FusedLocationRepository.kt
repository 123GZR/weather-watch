package com.galaxy.weatherwatch.data

import android.location.Location
import com.galaxy.weatherwatch.data.model.LocationResult
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/**
 * 基于 Google Play Services FusedLocationProviderClient 的定位实现。
 *
 * 策略：
 * 1. 优先使用 getCurrentLocation() 主动请求一次新鲜定位
 *    - 内部会主动打开 GPS/网络定位获取当前位置
 *    - Wear OS 上 lastLocation 经常为 null，因此优先主动请求
 * 2. 失败或超时时回退到 lastLocation（缓存位置）
 * 3. 都失败则返回 LocationUnavailableException
 */
class FusedLocationRepository(
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationRepository {

    override suspend fun getCurrentLocation(): Result<LocationResult> = try {
        // 第一步：主动请求一次新鲜定位（CurrentLocationRequest 自带超时）
        val freshLocation = try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(LOCATION_TIMEOUT_MS)
                .setMaxUpdateAgeMillis(0)
                .build()
            fusedLocationClient.getCurrentLocation(request, null).await()
        } catch (_: Exception) {
            null
        }

        if (freshLocation != null) {
            Result.success(freshLocation.toResult())
        } else {
            // 第二步：回退到 lastLocation
            val lastLocation = try {
                fusedLocationClient.lastLocation.await()
            } catch (_: Exception) {
                null
            }
            if (lastLocation != null) {
                Result.success(lastLocation.toResult())
            } else {
                Result.failure(LocationUnavailableException())
            }
        }
    } catch (_: SecurityException) {
        Result.failure(LocationPermissionDeniedException())
    } catch (_: Exception) {
        Result.failure(LocationUnavailableException())
    }

    private fun Location.toResult(): LocationResult = LocationResult(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        epochSecond = time / 1000
    )

    companion object {
        private const val LOCATION_TIMEOUT_MS = 10_000L
    }
}

class LocationPermissionDeniedException : RuntimeException("定位权限被拒绝")
class LocationUnavailableException : RuntimeException("无法获取位置信息")
