package com.galaxy.weatherwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.galaxy.weatherwatch.ui.WeatherAppScreen
import com.galaxy.weatherwatch.ui.WeatherViewModel
import com.galaxy.weatherwatch.ui.theme.GalaxyWeatherWatchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            GalaxyWeatherWatchTheme {
                WeatherAppScreen(viewModel = viewModel)
            }
        }
    }
}
