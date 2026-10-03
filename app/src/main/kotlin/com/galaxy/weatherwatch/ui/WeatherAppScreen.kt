package com.galaxy.weatherwatch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.galaxy.weatherwatch.ui.screens.CitySearchScreen
import com.galaxy.weatherwatch.ui.screens.ErrorScreen
import com.galaxy.weatherwatch.ui.screens.HomeScreen
import com.galaxy.weatherwatch.ui.screens.LoadingScreen

@Composable
fun WeatherAppScreen(
    viewModel: WeatherViewModel,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val showCitySearch by viewModel.showCitySearch.collectAsState()
    val citySearchResults by viewModel.citySearchResults.collectAsState()

    // 稳定 lambda：避免每次重组产生新实例导致下游无谓重组
    val onRefresh = remember(viewModel) { { viewModel.refresh() } }
    val onRetry = remember(viewModel) { { viewModel.refresh() } }
    val onLocationClick = remember(viewModel) { { viewModel.openCitySearch() } }
    val onCitySearch = remember(viewModel) { { query: String -> viewModel.searchCity(query) } }
    val onSelectCity = remember(viewModel) { { city: com.galaxy.weatherwatch.data.model.City -> viewModel.selectCity(city) } }
    val onUseGps = remember(viewModel) { { viewModel.useGpsLocation() } }
    val onCloseCitySearch = remember(viewModel) { { viewModel.closeCitySearch() } }

    // 首次进入时触发权限检查流程
    LaunchedEffect(Unit) {
        onRequestPermission()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 城市搜索页（覆盖在主界面之上）
        if (showCitySearch) {
            CitySearchScreen(
                cities = citySearchResults,
                onSearch = onCitySearch,
                onSelectCity = onSelectCity,
                onUseGps = onUseGps,
                onClose = onCloseCitySearch
            )
        } else {
            // 主界面：天气展示
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
                    isRefreshing = s.isRefreshing,
                    onRetry = onRetry,
                    onOpenAppSettings = onOpenAppSettings
                )
            }
        }
    }
}
