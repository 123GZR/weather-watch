package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.model.LocationResult

interface LocationRepository {
    suspend fun getCurrentLocation(): Result<LocationResult>
}
