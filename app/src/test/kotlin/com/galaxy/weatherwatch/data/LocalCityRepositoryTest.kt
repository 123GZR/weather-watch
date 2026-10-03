package com.galaxy.weatherwatch.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalCityRepositoryTest {

    private lateinit var repository: LocalCityRepository

    @Before
    fun setUp() {
        repository = LocalCityRepository()
    }

    @Test
    fun `getAllCities 返回佛山五区`() {
        val cities = repository.getAllCities()
        assertEquals(5, cities.size)
        assertTrue(cities.any { it.name == "佛山市 禅城区" })
        assertTrue(cities.any { it.name == "佛山市 南海区" })
        assertTrue(cities.any { it.name == "佛山市 顺德区" })
        assertTrue(cities.any { it.name == "佛山市 三水区" })
        assertTrue(cities.any { it.name == "佛山市 高明区" })
    }

    @Test
    fun `searchCities 空字符串返回全部城市`() {
        val result = repository.searchCities("")
        assertEquals(5, result.size)
    }

    @Test
    fun `searchCities 空白字符串返回全部城市`() {
        val result = repository.searchCities("   ")
        assertEquals(5, result.size)
    }

    // ---- 中文搜索 ----

    @Test
    fun `searchCities 中文区名匹配`() {
        val result = repository.searchCities("禅城")
        assertEquals(1, result.size)
        assertEquals("佛山市 禅城区", result[0].name)
    }

    @Test
    fun `searchCities 中文佛山市匹配全部`() {
        val result = repository.searchCities("佛山市")
        assertEquals(5, result.size)
    }

    @Test
    fun `searchCities 中文南海匹配`() {
        val result = repository.searchCities("南海")
        assertEquals(1, result.size)
        assertEquals("佛山市 南海区", result[0].name)
    }

    // ---- 拼音搜索 ----

    @Test
    fun `searchCities 全拼 chancheng 匹配禅城区`() {
        val result = repository.searchCities("chancheng")
        assertEquals(1, result.size)
        assertEquals("佛山市 禅城区", result[0].name)
    }

    @Test
    fun `searchCities 全拼 nanhai 匹配南海区`() {
        val result = repository.searchCities("nanhai")
        assertEquals(1, result.size)
        assertEquals("佛山市 南海区", result[0].name)
    }

    @Test
    fun `searchCities 全拼 shunde 匹配顺德区`() {
        val result = repository.searchCities("shunde")
        assertEquals(1, result.size)
        assertEquals("佛山市 顺德区", result[0].name)
    }

    @Test
    fun `searchCities 拼音 foshanshi 匹配全部`() {
        val result = repository.searchCities("foshanshi")
        assertEquals(5, result.size)
    }

    // ---- 拼音首字母缩写搜索 ----

    @Test
    fun `searchCities 缩写 fs 匹配全部佛山`() {
        val result = repository.searchCities("fs")
        assertEquals(5, result.size)
    }

    @Test
    fun `searchCities 缩写 fss 匹配全部`() {
        val result = repository.searchCities("fss")
        assertEquals(5, result.size)
    }

    @Test
    fun `searchCities 缩写 ccq 匹配禅城区`() {
        val result = repository.searchCities("ccq")
        assertEquals(1, result.size)
        assertEquals("佛山市 禅城区", result[0].name)
    }

    @Test
    fun `searchCities 缩写 nhq 匹配南海区`() {
        val result = repository.searchCities("nhq")
        assertEquals(1, result.size)
        assertEquals("佛山市 南海区", result[0].name)
    }

    @Test
    fun `searchCities 缩写 sdq 匹配顺德区`() {
        val result = repository.searchCities("sdq")
        assertEquals(1, result.size)
        assertEquals("佛山市 顺德区", result[0].name)
    }

    @Test
    fun `searchCities 缩写 ssq 匹配三水区`() {
        val result = repository.searchCities("ssq")
        assertEquals(1, result.size)
        assertEquals("佛山市 三水区", result[0].name)
    }

    @Test
    fun `searchCities 缩写 gmq 匹配高明区`() {
        val result = repository.searchCities("gmq")
        assertEquals(1, result.size)
        assertEquals("佛山市 高明区", result[0].name)
    }

    // ---- 大小写不敏感 ----

    @Test
    fun `searchCities 大写拼音也能匹配`() {
        val result = repository.searchCities("CHANCHENG")
        assertEquals(1, result.size)
        assertEquals("佛山市 禅城区", result[0].name)
    }

    @Test
    fun `searchCities 大写缩写也能匹配`() {
        val result = repository.searchCities("FS")
        assertEquals(5, result.size)
    }

    // ---- 无匹配 ----

    @Test
    fun `searchCities 无匹配时返回空列表`() {
        val result = repository.searchCities("北京市")
        assertEquals(0, result.size)
    }

    @Test
    fun `searchCities 随机字符无匹配`() {
        val result = repository.searchCities("xyz")
        assertEquals(0, result.size)
    }

    // ---- GPS 默认城市 ----

    @Test
    fun `GPS_DEFAULT_CITY 是佛山市禅城区`() {
        val defaultCity = LocalCityRepository.GPS_DEFAULT_CITY
        assertEquals("佛山市 禅城区", defaultCity.name)
        assertTrue(defaultCity.latitude in 22.0..24.0)
        assertTrue(defaultCity.longitude in 112.0..114.0)
    }

    // ---- 城市数据完整性 ----

    @Test
    fun `所有城市都有合法的经纬度`() {
        val cities = repository.getAllCities()
        cities.forEach { city ->
            assertTrue("${city.name} 纬度应在有效范围内", city.latitude in -90.0..90.0)
            assertTrue("${city.name} 经度应在有效范围内", city.longitude in -180.0..180.0)
        }
    }

    @Test
    fun `所有城市都有拼音和缩写`() {
        val cities = repository.getAllCities()
        cities.forEach { city ->
            assertTrue("${city.name} 应有非空拼音", city.pinyin.isNotBlank())
            assertTrue("${city.name} 应有非空缩写", city.abbreviation.isNotBlank())
        }
    }
}
