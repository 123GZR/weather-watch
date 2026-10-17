plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.galaxy.weatherwatch.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
}

// Workaround：KGP 2.2.21 + AGP 8.13 下，baselineprofile 插件注入的 nonMinifiedRelease /
// benchmarkRelease 变体 KotlinCompilation 的 AGP compileClasspath 映射为空
// （compileDependencyFiles 不含任何依赖），导致 compile*ReleaseKotlin 全部符号 Unresolved。
// 此处对这两个 compilation 显式注入 benchmark-macro-junit4 / uiautomator 依赖。
// 待升级 Kotlin/AGP/baselineprofile 组合后删除本段。
afterEvaluate {
    kotlin.target.compilations.forEach { comp ->
        comp.dependencies {
            implementation(project.dependencies.create("androidx.benchmark:benchmark-macro-junit4:1.5.0"))
            implementation(project.dependencies.create("androidx.test.uiautomator:uiautomator:2.4.0"))
        }
    }
}
