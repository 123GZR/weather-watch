package com.galaxy.weatherwatch.data

import android.location.Location
import com.galaxy.weatherwatch.data.model.LocationResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * 基于 Google Play Services FusedLocationProviderClient 的定位实现。
 * 优先获取 lastLocation，若为空则主动请求一次单次定位更新。
 */
class FusedLocationRepository(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val dispatcher: CoroutineContext = Dispatchers.Default
) : LocationRepository {

    override suspend fun getCurrentLocation(): Result<LocationResult> = withContext(dispatcher) {
        try {
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                return@withContext Result.success(lastLocation.toResult())
            }
            // lastLocation 为 null（常见于 Wear OS 首次启动或长时间未使用定位），
            // 主动请求一次单次定位更新
            val freshLocation = requestSingleLocationUpdate()
            if (freshLocation != null) {
                Result.success(freshLocation.toResult())
            } else {
                Result.failure(LocationUnavailableException())
            }
        } catch (e: SecurityException) {
            Result.failure(LocationPermissionDeniedException())
        } catch (e: Exception) {
            Result.failure(LocationUnavailableException())
        }
    }

    private suspend fun requestSingleLocationUpdate(): Location? =
        suspendCancellableCoroutine { continuation ->
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATE_INTERVAL_MS
            )
                .setMaxUpdates(1)
                .setWaitForAccurateLocation(false)
                .build()

            val callback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    continuation.resume(result.lastLocation)
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    null // 使用调用线程的 looper
                ).addOnFailureListener {
                    fusedLocationClient.removeLocationUpdates(callback)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            } catch (e: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }

    private fun Location.toResult(): LocationResult = LocationResult(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        epochSecond = time / 1000
    )

    companion object {
        private const val LOCATION_UPDATE_INTERVAL_MS = 5000L
    }
}

class LocationPermissionDeniedException : RuntimeException("定位权限被拒绝")
class LocationUnavailableException : RuntimeException("无法获取位置信息")
