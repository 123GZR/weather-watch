package com.galaxy.weatherwatch.data.model

import androidx.compose.runtime.Immutable

/**
 * 城市数据模型。
 *
 * 包含中文名称、拼音、缩写（首字母）以及经纬度，
 * 用于本地城市库的搜索与天气查询。
 *
 * 性能说明：加 [Immutable] 后 Compose 编译器视为稳定类型，
 * 列表刷新时内容未变的 item 可跳过重组。
 */
@Immutable
data class City(
    /** 中文显示名，如「佛山市 禅城区」 */
    val name: String,
    /** 完整拼音（无空格无音调），如「foshanshichanchengqu」 */
    val pinyin: String,
    /** 拼音首字母缩写，如「fssccq」 */
    val abbreviation: String,
    /** 纬度 */
    val latitude: Double,
    /** 经度 */
    val longitude: Double
)
