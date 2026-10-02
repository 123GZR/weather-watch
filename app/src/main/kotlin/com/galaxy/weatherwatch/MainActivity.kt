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
import com.galaxy.weatherwatch.data.MockWeatherRepository
import com.galaxy.weatherwatch.ui.WeatherAppScreen
import com.galaxy.weatherwatch.ui.WeatherViewModel
import com.galaxy.weatherwatch.ui.WeatherViewModelFactory
import com.galaxy.weatherwatch.ui.theme.GalaxyWeatherWatchTheme
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels {
        WeatherViewModelFactory(
            weatherRepository = MockWeatherRepository(),
            locationRepository = FusedLocationRepository(
                LocationServices.getFusedLocationProviderClient(this)
            ),
            geocodingRepository = AndroidGeocodingRepository(this)
        )
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshWithPermissionCheck(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

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
