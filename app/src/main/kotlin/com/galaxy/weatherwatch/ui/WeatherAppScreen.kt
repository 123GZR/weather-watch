package com.galaxy.weatherwatch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.wear.compose.material3.AppScaffold
import com.galaxy.weatherwatch.data.model.City
import com.galaxy.weatherwatch.ui.screens.CitySearchScreen
import com.galaxy.weatherwatch.ui.screens.ErrorScreen
import com.galaxy.weatherwatch.ui.screens.HomeScreen
import com.galaxy.weatherwatch.ui.screens.LoadingScreen

/**
 * 顶层导航根：导航探针版（临时）。
 *
 * 诊断：Wear Compose 1.6.2 的 SwipeDismissableNavHost 在 Wear OS 6（API 36）上走
 * PredictiveBackNavHost 预测性返回路径，存在首屏内容不渲染的嫌疑（真机黑屏、仅 TimeText 可见）。
 * 本探针改用 navigation-compose 的普通 NavHost 验证该假设：
 * - 若正常显示 → 根因锁定为 Wear 预测性返回路径，再定长期方案；
 * - 若仍黑屏 → 问题不在导航宿主，需继续排查 AppScaffold/主题/内容区。
 * 时间由 [AppScaffold] 自动提供 TimeText。
 */
@Composable
fun WeatherAppScreen(
    viewModel: WeatherViewModel,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nav = rememberNavController()
    val state by viewModel.uiState.collectAsState()
    val citySearchResults by viewModel.citySearchResults.collectAsState()

    // 稳定 lambda：避免每次重组产生新实例导致下游无谓重组
    val onRefresh = remember(viewModel) { { viewModel.refresh() } }
    val onLocationClick = remember(viewModel, nav) {
        {
            viewModel.initializeCitySearch()
            nav.navigate("city_search")
        }
    }
    val onSearch = remember(viewModel) { { query: String -> viewModel.searchCity(query) } }
    val onSelectCity = remember(viewModel, nav) {
        { city: City ->
            viewModel.selectCity(city)
            nav.popBackStack()
            Unit
        }
    }
    val onUseGps = remember(viewModel, nav) {
        {
            viewModel.useGpsLocation()
            nav.popBackStack()
            Unit
        }
    }
    val onBack = remember(nav) {
        {
            nav.popBackStack()
            Unit
        }
    }

    // 首次进入时触发权限检查流程
    LaunchedEffect(Unit) {
        onRequestPermission()
    }

    AppScaffold(modifier = modifier) {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                when (val s = state) {
                    is WeatherUiState.Loading -> LoadingScreen()
                    is WeatherUiState.Success -> HomeScreen(
                        snapshot = s.snapshot,
                        isRefreshing = s.isRefreshing,
                        onRefresh = onRefresh,
                        onLocationClick = onLocationClick
                    )
                    is WeatherUiState.Error -> ErrorScreen(
                        type = s.type,
                        locationName = s.locationName,
                        isRefreshing = s.isRefreshing,
                        onRetry = onRefresh,
                        onOpenAppSettings = onOpenAppSettings,
                        onLocationClick = onLocationClick,
                        onPickCity = onLocationClick
                    )
                }
            }
            composable("city_search") {
                CitySearchScreen(
                    cities = citySearchResults,
                    onSearch = onSearch,
                    onSelectCity = onSelectCity,
                    onUseGps = onUseGps,
                    onBack = onBack
                )
            }
        }
    }
}
