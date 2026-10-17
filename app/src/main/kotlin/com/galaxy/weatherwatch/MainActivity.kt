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
import com.galaxy.weatherwatch.ui.WeatherAppScreen
import com.galaxy.weatherwatch.ui.WeatherViewModel
import com.galaxy.weatherwatch.ui.theme.GalaxyWeatherWatchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** 依赖由 Hilt 注入（见 di/DataModule.kt 与各 Repository 的 @Inject 构造） */
    private val viewModel: WeatherViewModel by viewModels()

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
