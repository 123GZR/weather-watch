package com.galaxy.weatherwatch.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val epochSecond: Long
)
