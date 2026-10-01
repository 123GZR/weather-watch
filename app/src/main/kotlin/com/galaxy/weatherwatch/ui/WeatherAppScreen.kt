package com.galaxy.weatherwatch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.galaxy.weatherwatch.ui.screens.ErrorScreen
import com.galaxy.weatherwatch.ui.screens.HomeScreen
import com.galaxy.weatherwatch.ui.screens.LoadingScreen

@Composable
fun WeatherAppScreen(viewModel: WeatherViewModel) {
    val state by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val s = state) {
            is WeatherUiState.Loading -> LoadingScreen()
            is WeatherUiState.Success -> HomeScreen(
                snapshot = s.snapshot,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            )
            is WeatherUiState.Error -> ErrorScreen(
                type = s.type,
                onRetry = viewModel::refresh
            )
        }
    }
}
