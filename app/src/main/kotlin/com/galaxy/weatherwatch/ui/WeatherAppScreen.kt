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

    // 稳定 lambda：避免每次重组产生新实例导致下游无谓重组
    val onRefresh = remember(viewModel) { { viewModel.refresh() } }
    val onRetry = remember(viewModel) { { viewModel.refresh() } }

    // 首次进入时触发权限检查流程
    LaunchedEffect(Unit) {
        onRequestPermission()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is WeatherUiState.Loading -> LoadingScreen()
            is WeatherUiState.Success -> HomeScreen(
                snapshot = s.snapshot,
                isRefreshing = s.isRefreshing,
                onRefresh = onRefresh
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
