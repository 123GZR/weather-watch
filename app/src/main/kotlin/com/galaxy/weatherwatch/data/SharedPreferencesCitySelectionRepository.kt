package com.galaxy.weatherwatch.data

import android.content.SharedPreferences
import com.galaxy.weatherwatch.data.model.City

/**
 * 基于 SharedPreferences 的城市选择记忆仓库实现。
 *
 * 将最近一次手动选择的城市持久化为 5 个字段（名称/拼音/缩写/纬度/经度），
 * 应用重启后仍可读取回退。
 *
 * 设计要点：
 * - SharedPreferences 实例由调用方注入（而非持有 Context），
 *   便于单元测试（MockK 模拟）与未来替换存储实现
 * - 数据量小且为单键写入，使用同步 API 足够；写入采用 [android.content.SharedPreferences.Editor.apply]
 *   异步落盘，不阻塞主线程
 */
class SharedPreferencesCitySelectionRepository(
    private val prefs: SharedPreferences
) : CitySelectionRepository {

    override fun getSelectedCity(): City? {
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val pinyin = prefs.getString(KEY_PINYIN, null) ?: return null
        val abbreviation = prefs.getString(KEY_ABBREVIATION, null) ?: return null
        // 经纬度以字符串存储，避免 Float 精度损失；解析失败视为未记忆
        val latitude = prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull() ?: return null
        val longitude = prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull() ?: return null
        return City(
            name = name,
            pinyin = pinyin,
            abbreviation = abbreviation,
            latitude = latitude,
            longitude = longitude
        )
    }

    override fun saveSelectedCity(city: City) {
        prefs.edit()
            .putString(KEY_NAME, city.name)
            .putString(KEY_PINYIN, city.pinyin)
            .putString(KEY_ABBREVIATION, city.abbreviation)
            .putString(KEY_LATITUDE, city.latitude.toString())
            .putString(KEY_LONGITUDE, city.longitude.toString())
            .apply()
    }

    override fun clearSelectedCity() {
        prefs.edit().clear().apply()
    }

    companion object {
        internal const val KEY_NAME = "name"
        internal const val KEY_PINYIN = "pinyin"
        internal const val KEY_ABBREVIATION = "abbreviation"
        internal const val KEY_LATITUDE = "latitude"
        internal const val KEY_LONGITUDE = "longitude"
    }
}
