package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.model.City

/**
 * 本地城市库实现，当前仅包含佛山市五区。
 *
 * 搜索支持：
 * - 中文名称模糊匹配（如「禅城」「南海」）
 * - 全拼匹配（如「chancheng」「nanhai」）
 * - 拼音首字母缩写匹配（如「fs」「ccq」「nh」）
 *
 * 搜索不区分大小写。
 */
class LocalCityRepository : CityRepository {

    override fun searchCities(query: String): List<City> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return ALL_CITIES

        val lowerQuery = trimmed.lowercase()
        return ALL_CITIES.filter { city ->
            city.name.contains(trimmed) ||
                city.pinyin.contains(lowerQuery) ||
                city.abbreviation.contains(lowerQuery)
        }
    }

    override fun getAllCities(): List<City> = ALL_CITIES

    companion object {
        /** 佛山市五区城市数据库 */
        private val ALL_CITIES = listOf(
            City(
                name = "佛山市 禅城区",
                pinyin = "foshanshichanchengqu",
                abbreviation = "fssccq",
                latitude = 23.0218,
                longitude = 113.1220
            ),
            City(
                name = "佛山市 南海区",
                pinyin = "foshanshinanhaiqu",
                abbreviation = "fssnhq",
                latitude = 23.0323,
                longitude = 113.1480
            ),
            City(
                name = "佛山市 顺德区",
                pinyin = "foshanshishundequ",
                abbreviation = "fsssdq",
                latitude = 22.8400,
                longitude = 113.2800
            ),
            City(
                name = "佛山市 三水区",
                pinyin = "foshanshisanshuiqu",
                abbreviation = "fssssq",
                latitude = 23.1700,
                longitude = 112.8900
            ),
            City(
                name = "佛山市 高明区",
                pinyin = "foshanshigaomingqu",
                abbreviation = "fssgmq",
                latitude = 22.9000,
                longitude = 112.7900
            )
        )

        /**
         * GPS 定位默认城市（佛山市 禅城区）。
         * 当用户选择「GPS 定位」时，使用此坐标。
         */
        val GPS_DEFAULT_CITY: City = ALL_CITIES.first()
    }
}
