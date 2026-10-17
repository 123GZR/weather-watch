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
import com.galaxy.weatherwatch.data.QWeatherRepository
import com.galaxy.weatherwatch.data.SharedPreferencesCitySelectionRepository
import com.galaxy.weatherwatch.data.WeatherRepository
import com.galaxy.weatherwatch.ui.WeatherAppScreen
import com.galaxy.weatherwatch.ui.WeatherViewModel
import com.galaxy.weatherwatch.ui.WeatherViewModelFactory
import com.galaxy.weatherwatch.ui.theme.GalaxyWeatherWatchTheme
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    /**
     * 天气数据源：和风天气（QWeather），凭据从 BuildConfig 注入
     * （值来自 local.properties，见 app/build.gradle.kts）。
     * 凭据缺失时请求会返回业务错误码，UI 展示「天气服务暂不可用」错误页。
     */
    private val weatherRepository: WeatherRepository = QWeatherRepository(
        apiKey = BuildConfig.QWEATHER_API_KEY,
        apiHost = BuildConfig.QWEATHER_API_HOST
    )

    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(
            weatherRepository = weatherRepository,
            locationRepository = FusedLocationRepository(
                LocationServices.getFusedLocationProviderClient(this)
            ),
            geocodingRepository = AndroidGeocodingRepository(this),
            cityRepository = LocalCityRepository(),
            // 记忆最近一次手动选择的城市，GPS 定位失败时回退到该城市
            citySelectionRepository = SharedPreferencesCitySelectionRepository(
                getSharedPreferences(CITY_SELECTION_PREFS, MODE_PRIVATE)
            )
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

    companion object {
        /** 记忆城市 SharedPreferences 文件名 */
        private const val CITY_SELECTION_PREFS = "city_selection"
    }
}
