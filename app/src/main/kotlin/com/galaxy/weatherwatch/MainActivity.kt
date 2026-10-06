package com.galaxy.weatherwatch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.galaxy.weatherwatch.data.AndroidGeocodingRepository
import com.galaxy.weatherwatch.data.FusedLocationRepository
import com.galaxy.weatherwatch.data.LocalCityRepository
import com.galaxy.weatherwatch.data.MockWeatherRepository
import com.galaxy.weatherwatch.data.QWeatherRepository
import com.galaxy.weatherwatch.data.WeatherRepository
import com.galaxy.weatherwatch.ui.WeatherAppScreen
import com.galaxy.weatherwatch.ui.WeatherViewModel
import com.galaxy.weatherwatch.ui.WeatherViewModelFactory
import com.galaxy.weatherwatch.ui.theme.GalaxyWeatherWatchTheme
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    /**
     * 默认数据源为和风天气（QWeather），凭据从 BuildConfig 注入
     * （值来自 local.properties，见 app/build.gradle.kts）。
     * 凭据缺失时（如未配置 local.properties 的开发者机器）回退到 Mock 数据，
     * 保证项目开箱可跑。
     */
    private val weatherRepository: WeatherRepository =
        if (BuildConfig.QWEATHER_API_KEY.isNotBlank() && BuildConfig.QWEATHER_API_HOST.isNotBlank()) {
            QWeatherRepository(
                apiKey = BuildConfig.QWEATHER_API_KEY,
                apiHost = BuildConfig.QWEATHER_API_HOST
            )
        } else {
            MockWeatherRepository()
        }

    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(
            weatherRepository = weatherRepository,
            locationRepository = FusedLocationRepository(
                LocationServices.getFusedLocationProviderClient(this)
            ),
            geocodingRepository = AndroidGeocodingRepository(this),
            cityRepository = LocalCityRepository()
        )
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshWithPermissionCheck(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 主题已在 manifest 中声明（Theme.GalaxyWeatherWatch），无需重复 setTheme

        setContent {
            GalaxyWeatherWatchTheme {
                WeatherAppScreen(
                    viewModel = viewModel,
                    onRequestPermission = ::checkAndRequestLocationPermission,
                    onOpenAppSettings = ::openAppSettings
                )
            }
        }
    }

    private fun checkAndRequestLocationPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.refreshWithPermissionCheck(true)
        } else {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}
