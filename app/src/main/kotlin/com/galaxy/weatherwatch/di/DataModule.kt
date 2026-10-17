package com.galaxy.weatherwatch.di

import android.content.Context
import android.content.SharedPreferences
import com.galaxy.weatherwatch.BuildConfig
import com.galaxy.weatherwatch.data.AndroidGeocodingRepository
import com.galaxy.weatherwatch.data.CityRepository
import com.galaxy.weatherwatch.data.CitySelectionRepository
import com.galaxy.weatherwatch.data.FusedLocationRepository
import com.galaxy.weatherwatch.data.GeocodingRepository
import com.galaxy.weatherwatch.data.LocalCityRepository
import com.galaxy.weatherwatch.data.LocationRepository
import com.galaxy.weatherwatch.data.QWeatherRepository
import com.galaxy.weatherwatch.data.SharedPreferencesCitySelectionRepository
import com.galaxy.weatherwatch.data.WeatherRepository
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import javax.inject.Singleton

/**
 * Hilt 依赖注入装配模块。
 *
 * [DataProvidesModule]：提供需要手工构造的绑定（QWeatherRepository 需要 BuildConfig 凭据、
 * FusedLocationProviderClient 需要 ApplicationContext、SharedPreferences 需要文件名）。
 * [DataBindsModule]：接口 → 实现的绑定（实现类均带 [javax.inject.Inject] 构造注解）。
 */
@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {
    @Provides @Singleton
    fun provideWeatherRepository(): WeatherRepository =
        QWeatherRepository(BuildConfig.QWEATHER_API_KEY, BuildConfig.QWEATHER_API_HOST)

    @Provides @Singleton
    fun provideFusedLocationClient(@ApplicationContext context: Context) =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides @Singleton
    fun provideCitySelectionPrefs(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("city_selection", Context.MODE_PRIVATE)

    /** AndroidGeocodingRepository 的 IO 调度器（默认参数 Dispatchers.IO 的 Hilt 绑定） */
    @Provides @Singleton
    fun provideIoDispatcher(): CoroutineContext = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds @Singleton abstract fun bindLocation(impl: FusedLocationRepository): LocationRepository
    @Binds @Singleton abstract fun bindGeocoding(impl: AndroidGeocodingRepository): GeocodingRepository
    @Binds @Singleton abstract fun bindCity(impl: LocalCityRepository): CityRepository
    @Binds @Singleton abstract fun bindCitySelection(impl: SharedPreferencesCitySelectionRepository): CitySelectionRepository
}
