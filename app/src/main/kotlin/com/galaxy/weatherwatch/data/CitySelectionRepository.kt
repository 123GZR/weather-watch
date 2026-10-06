package com.galaxy.weatherwatch.data

import com.galaxy.weatherwatch.data.model.City

/**
 * 城市选择记忆仓库接口。
 *
 * 持久化用户最近一次手动选择的城市，用于：
 * - GPS 定位失败时回退展示该城市天气，避免直接进入错误页
 * - 应用重启后保留上次选择，无需重新搜索
 */
interface CitySelectionRepository {
    /**
     * 读取最近一次手动选择的城市。
     *
     * @return 记忆的城市；从未手动选择过（或已清除）时返回 null
     */
    fun getSelectedCity(): City?

    /**
     * 保存最近一次手动选择的城市。
     *
     * @param city 用户手动选择的城市
     */
    fun saveSelectedCity(city: City)

    /**
     * 清除记忆城市。
     */
    fun clearSelectedCity()
}
