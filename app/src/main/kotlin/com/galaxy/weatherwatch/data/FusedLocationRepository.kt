package com.galaxy.weatherwatch.data

import android.location.Location
import com.galaxy.weatherwatch.data.model.LocationResult
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 Google Play Services FusedLocationProviderClient 的定位实现。
 *
 * 多级降级策略（适配 Wear OS / 三星 Galaxy Watch 4C）：
 * 1. 快速定位（网络/蓝牙/Wi-Fi）— 5 秒超时，室内可用
 * 2. 高精度定位（GPS） — 20 秒超时，户外精准
 * 3. 回退 lastLocation 缓存 — 立即返回
 * 4. 全部失败 → LocationUnavailableException
 *
 * 为什么这样设计：
 * - Wear OS 手表 GPS 冷启动通常需要 10-30 秒（需搜星）
 * - 室内环境 GPS 无法定位，但网络定位可用
 * - 先快速定位让用户尽快看到结果，再异步尝试高精度（本实现用串行降级）
 */
@Singleton
class FusedLocationRepository @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationRepository {

    override suspend fun getCurrentLocation(): Result<LocationResult> = try {
        // 第一级：快速定位（平衡精度，网络/蓝牙/Wi-Fi）
        val quickLocation = tryGetCurrentLocation(
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            timeoutMs = QUICK_LOCATION_TIMEOUT_MS
        )
        if (quickLocation != null) {
            Result.success(quickLocation.toResult())
        } else {
            // 第二级：高精度定位（GPS）
            val highAccuracyLocation = tryGetCurrentLocation(
                priority = Priority.PRIORITY_HIGH_ACCURACY,
                timeoutMs = HIGH_ACCURACY_TIMEOUT_MS
            )
            if (highAccuracyLocation != null) {
                Result.success(highAccuracyLocation.toResult())
            } else {
                // 第三级：回退 lastLocation 缓存
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
        }
    } catch (_: SecurityException) {
        Result.failure(LocationPermissionDeniedException())
    } catch (_: Exception) {
        Result.failure(LocationUnavailableException())
    }

    /**
     * 尝试一次 getCurrentLocation 调用，失败或超时返回 null。
     */
    private suspend fun tryGetCurrentLocation(
        priority: Int,
        timeoutMs: Long
    ): Location? = try {
        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setDurationMillis(timeoutMs)
            .build()
        fusedLocationClient.getCurrentLocation(request, null).await()
    } catch (_: Exception) {
        null
    }

    private fun Location.toResult(): LocationResult = LocationResult(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        epochSecond = time / 1000
    )

    companion object {
        private const val QUICK_LOCATION_TIMEOUT_MS = 5_000L
        private const val HIGH_ACCURACY_TIMEOUT_MS = 20_000L
    }
}

class LocationPermissionDeniedException : RuntimeException("定位权限被拒绝")
class LocationUnavailableException : RuntimeException("无法获取位置信息")
