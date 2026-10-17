package com.galaxy.weatherwatch.data

/**
 * 天气数据源业务异常。
 *
 * 说明：这些异常类原本定义在 MockWeatherRepository.kt 底部，
 * 随模拟数据源移除后提取为独立文件，供真实数据源（QWeatherRepository 等）与
 * ViewModel 错误类型映射共用。
 */

/** 无网络连接 */
class NoNetworkException : RuntimeException("网络不可用")

/** 请求超时 */
class TimeoutException : RuntimeException("请求超时")

/** 服务端错误（HTTP 非 200 或业务状态码非 200） */
class ApiException(val httpCode: Int) : RuntimeException("天气服务暂不可用 ($httpCode)")

/** 无法获取位置（天气层 GPS 不可用） */
class GpsUnavailableException : RuntimeException("无法获取位置")
