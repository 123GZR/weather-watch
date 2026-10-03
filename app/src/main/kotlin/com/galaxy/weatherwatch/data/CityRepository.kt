package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.model.City

/**
 * 城市搜索仓库接口。
 *
 * 提供城市查询能力，支持按中文名、拼音、缩写进行模糊搜索。
 */
interface CityRepository {
    /**
     * 根据查询关键字搜索城市。
     *
     * 匹配规则（任一满足即匹配）：
     * - 中文名称包含查询关键字
     * - 完整拼音包含查询关键字
     * - 拼音首字母缩写以查询关键字开头
     *
     * @param query 搜索关键字，为空时返回全部城市
     * @return 匹配的城市列表
     */
    fun searchCities(query: String): List<City>

    /**
     * 获取全部城市列表。
     */
    fun getAllCities(): List<City>
}
