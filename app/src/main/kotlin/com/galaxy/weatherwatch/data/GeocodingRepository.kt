package com.galaxy.weatherwatch.data

interface GeocodingRepository {
    /**
     * 根据经纬度反向解析地址名称。
     * @return 成功时返回格式化的地址字符串，失败时返回 null
     */
    suspend fun getLocationName(lat: Double, lon: Double): String?
}
