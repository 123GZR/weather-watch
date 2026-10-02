pluginManagement {
    repositories {
        // 阿里云镜像，解决 SSL 握手失败和下载超时问题
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin/") }
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/public/") }

        // 官方源作为备用（如果镜像没有找到依赖，会尝试回退到这里）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 这里表示只允许在 settings 中声明仓库，禁止在子项目的 build.gradle 中单独声明
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像，解决 SSL 握手失败和下载超时问题
        maven { url = uri("https://maven.aliyun.com/repository/google/") }
        maven { url = uri("https://maven.aliyun.com/repository/public/") }

        // 官方源作为备用
        google()
        mavenCentral()
    }
}

// 你的项目名称 (可以根据需要修改)
rootProject.name = "WeatherApp"
// 引入 app 模块 (根据你截图的项目结构，模块名应该是 app)
include(":app")
