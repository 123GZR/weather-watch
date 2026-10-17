import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // Baseline Profile：注册 baselineProfile 依赖配置，并把生成器模块的基线配置打包进 APK
    alias(libs.plugins.androidx.baselineprofile)
    // Hilt + KSP：依赖注入（替换手写 ViewModel Factory）
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// 和风天气凭据：从 local.properties 读取（该文件已被 .gitignore 排除，避免密钥入库）。
// 缺失时注入空字符串，MainActivity 构造的 QWeatherRepository 请求会失败并展示错误页。
val qweatherProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.galaxy.weatherwatch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.galaxy.weatherwatch"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 和风天气 Key / API Host 注入 BuildConfig，供 MainActivity 构造 QWeatherRepository
        buildConfigField(
            "String",
            "QWEATHER_API_KEY",
            "\"${qweatherProperties.getProperty("qweather.apiKey", "")}\""
        )
        buildConfigField(
            "String",
            "QWEATHER_API_HOST",
            "\"${qweatherProperties.getProperty("qweather.apiHost", "")}\""
        )
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Workaround：androidx.baselineprofile 1.5.0 插件在 AGP 8.13 下会给 nonMinifiedRelease /
// benchmarkRelease sourceSet 的 kotlin srcDirs 追加异常 provider 路径（D:\app\app\provider(?），
// 导致 KSP 的 isParentOf 路径解析失败（release 变体构建崩溃）。
// KSP 遍历的是 KGP 的 allKotlinSourceSets，因此需在 KGP 侧用替换语义 setSrcDirs 重设。
// 待 baselineprofile 插件发布修复版本后删除本段。
afterEvaluate {
    kotlin.sourceSets.matching { it.name == "nonMinifiedRelease" || it.name == "benchmarkRelease" }.all {
        kotlin.setSrcDirs(listOf("src/${name}/kotlin", "src/${name}/java", "src/release/java"))
    }
}

// 组合性能说明：Kotlin 2.0.20 的 Compose 编译器默认开启 strong skipping
// （带稳定参数的 Composable 可跳过重组），无需额外配置；
// 若未来升级 Kotlin 版本后默认值变化，可通过 featureFlags 显式控制：
//   featureFlags.add(org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag.StrongSkipping)

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    // 显式提升 fragment（play-services 传递依赖 1.1.0 过旧）：保证
    // registerForActivityResult 可用且通过 lintVital（release 构建必需）
    implementation(libs.androidx.fragment.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // Wear OS Compose
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.navigation)
    debugImplementation(libs.wear.compose.ui.tooling)

    // Hilt（依赖注入）
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Location
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Networking
    implementation(libs.okhttp)

    // Performance: 在 debug/release 包中安装 Baseline Profile，提升首帧与滚动流畅度
    implementation(libs.androidx.profileinstaller)
    // 将 :baselineprofile 模块生成的基线配置（baseline-prof.txt）打包进 APK，
    // 由 profileinstaller 在首次启动时预编译热路径。
    // 生成方式：手表连接后执行 ./gradlew :baselineprofile:generateBaselineProfile
    baselineProfile(project(":baselineprofile"))

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
