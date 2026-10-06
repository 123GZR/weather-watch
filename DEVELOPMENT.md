# 开发文档

本文档详细描述 WeatherWatch 项目的架构设计、模块划分、数据流、编码规范等，供开发者在进行功能开发或修改时参考。

> 本文档是 AI 开发者的**必读文档**。开始任何编码工作前，请先阅读本文档。

---

## 目录

- [1. 架构设计](#1-架构设计)
  - [1.1 总体架构](#11-总体架构)
  - [1.2 分层原则](#12-分层原则)
  - [1.3 单向数据流](#13-单向数据流)
- [2. 数据层详解](#2-数据层详解)
  - [2.1 WeatherRepository 接口](#21-weatherrepository-接口)
  - [2.2 MockWeatherRepository](#22-mockweatherrepository)
  - [2.3 OpenWeatherRepository](#23-openweatherrepository)
  - [2.4 LocationRepository 接口](#24-locationrepository-接口)
  - [2.5 FusedLocationRepository](#25-fusedlocationrepository)
  - [2.6 GeocodingRepository 接口](#26-geocodingrepository-接口)
  - [2.7 AndroidGeocodingRepository](#27-androidgeocodingrepository)
  - [2.8 CityRepository 接口](#28-cityrepository-接口)
  - [2.9 LocalCityRepository](#29-localcityrepository)
  - [2.10 数据模型](#210-数据模型)
  - [2.11 QWeatherRepository（和风天气，当前默认）](#211-qweatherrepository和风天气当前默认数据源)
  - [2.12 异常体系](#212-异常体系)
- [3. ViewModel 层详解](#3-viewmodel-层详解)
  - [3.1 WeatherViewModel](#31-weatherviewmodel)
  - [3.2 WeatherUiState](#32-weatheruistate)
  - [3.3 状态流转](#33-状态流转)
- [4. UI 层详解](#4-ui-层详解)
  - [4.1 WeatherAppScreen（顶层路由）](#41-weatherappscreen顶层路由)
  - [4.2 HomeScreen（主屏）](#42-homescreen主屏)
  - [4.3 LoadingScreen / ErrorScreen](#43-loadingscreen--errorscreen)
  - [4.4 WeatherIcon 组件](#44-weathericon-组件)
  - [4.5 主题系统](#45-主题系统)
- [5. 性能优化指南](#5-性能优化指南)
  - [5.1 Compose 重组优化](#51-compose-重组优化)
  - [5.2 列表性能](#52-列表性能)
  - [5.3 Baseline Profile](#53-baseline-profile)
- [6. 测试指南](#6-测试指南)
  - [6.1 测试结构](#61-测试结构)
  - [6.2 测试工具](#62-测试工具)
  - [6.3 编写测试的原则](#63-编写测试的原则)
- [7. 构建与发布](#7-构建与发布)
  - [7.1 构建类型](#71-构建类型)
  - [7.2 版本管理](#72-版本管理)
  - [7.3 混淆规则](#73-混淆规则)
- [8. 编码规范](#8-编码规范)
  - [8.1 Kotlin 风格](#81-kotlin-风格)
  - [8.2 Compose 约定](#82-compose-约定)
  - [8.3 命名规范](#83-命名规范)
- [9. 依赖管理](#9-依赖管理)
- [10. 常见问题](#10-常见问题)

---

## 1. 架构设计

### 1.1 总体架构

项目采用 **MVVM (Model-View-ViewModel) + Repository** 架构，分为三层：

```
┌───────────────────────────────────────────────────────┐
│                     UI Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │ HomeScreen   │  │ LoadingScreen│  │ ErrorScreen │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬──────┘ │
│         │                 │                  │        │
│         └─────────────────┼──────────────────┘        │
│                           │                           │
│                  WeatherAppScreen                     │
│              (状态分发 / 顶层路由)                     │
└───────────────────┬───────────────────────────────────┘
                    │ StateFlow<WeatherUiState>
                    ▼
┌───────────────────────────────────────────────────────┐
│                  ViewModel Layer                       │
│              ┌──────────────────────┐                  │
│              │   WeatherViewModel   │                  │
│              │  - 状态管理           │                  │
│              │  - 业务逻辑编排       │                  │
│              │  - 协程生命周期管理   │                  │
│              └──────────┬───────────┘                  │
└───────────┬─────────────┴──────────────────────────────┘
            │ WeatherRepository 接口
            │ LocationRepository 接口
            ▼
┌───────────────────────────────────────────────────────┐
│                    Data Layer                          │
│  ┌──────────────────────┐  ┌──────────────────────┐  │
│  │ MockWeatherRepository│  │ QWeatherRepository   │  │
│  │ (本地假数据)          │  │ (和风天气 API, 默认) │  │
│  └──────────┬───────────┘  └──────────┬───────────┘  │
│             │                         │               │
│             └───────────┬─────────────┘               │
│                         │                             │
│                   WeatherRepository                    │
│                    (接口契约)                          │
│                                                       │
│  ┌────────────────────────────────────────────────┐   │
│  │          FusedLocationRepository                │   │
│  │          (Google Play Services 定位)            │   │
│  └───────────────────┬────────────────────────────┘   │
│                      │                                │
│                LocationRepository                     │
│                 (接口契约)                            │
└───────────────────────────────────────────────────────┘
```

### 1.2 分层原则

| 层级 | 职责 | 依赖方向 | 可依赖的内容 |
|------|------|---------|-------------|
| UI 层 | 渲染界面、处理用户交互 | 向下依赖 ViewModel | ViewModel、UiState、Compose 框架 |
| ViewModel 层 | 状态管理、业务逻辑编排、协程调度 | 向下依赖 Repository 接口 | Repository 接口、数据模型、协程 |
| Data 层 | 数据获取、解析、持久化、异常封装 | 向上实现 Repository 接口 | 网络库、序列化、数据模型 |

**关键原则：**
- 上层依赖下层的**抽象**（接口），而非具体实现
- 数据层的具体实现对 ViewModel 透明，切换数据源只需修改注入
- ViewModel 不持有任何 UI 相关引用（Context、View 等）

### 1.3 单向数据流

项目遵循**单向数据流**（Unidirectional Data Flow）模式：

```
  用户事件 (onClick / onRefresh)
         │
         ▼
┌─────────────────┐
│  WeatherViewModel │ ← 处理事件，更新状态
└────────┬────────┘
         │
         ▼
  StateFlow<WeatherUiState>
         │
         ▼
┌─────────────────┐
│   UI (Compose)  │ ← 收集状态，重组渲染
└─────────────────┘
         │
         ▼
  用户看到新界面
```

**为什么用单向数据流：**
- 状态唯一来源（Single Source of Truth），避免状态不一致
- 可预测性：相同输入必然产生相同输出
- 便于测试：ViewModel 的状态流转可独立测试
- 调试方便：通过状态日志即可追踪问题

---

## 2. 数据层详解

### 2.1 WeatherRepository 接口

**文件：** `data/WeatherRepository.kt`

```kotlin
interface WeatherRepository {
    suspend fun fetchWeather(lat: Double, lon: Double): Result<WeatherSnapshot>
}
```

**设计要点：**
- 使用 `suspend` 函数，调用方在协程中调用
- 返回 `Result<T>`，成功/失败统一封装，不直接抛异常
- 接口参数为经纬度，不关心具体定位实现（遵循单一职责）

### 2.2 MockWeatherRepository

**文件：** `data/MockWeatherRepository.kt`

本地假数据实现，用于：
- UI 开发与调试（无需网络）
- 单元测试（可控制失败模式）
- 演示与原型验证

**核心能力：**
- `simulateDelayMs`：模拟网络延迟
- `FailureMode`：可配置的失败模式（NONE / NETWORK / TIMEOUT / API / GPS）
- 生成 12 小时逐时预报数据
- 每次刷新时间戳递增

### 2.3 OpenWeatherRepository

**文件：** `data/OpenWeatherRepository.kt`

OpenWeatherMap One Call API 3.0 的实现。

**技术栈：**
- 网络请求：OkHttp
- JSON 解析：kotlinx.serialization
- 超时控制：`withTimeout(15.seconds)`
- 线程调度：`withContext(Dispatchers.IO)`

**内部数据模型：**
- `OpenWeatherResponse`：API 原始响应结构
- `OwmCurrent` / `OwmHourly` / `OwmDaily` / `OwmWeather`：各字段映射
- 通过 `toSnapshot()` 方法转换为业务模型 `WeatherSnapshot`

> **状态：** 已被 [2.11 QWeatherRepository](#211-qweatherrepository和风天气当前默认数据源)（和风天气）取代为默认数据源，本实现保留作参考。

### 2.4 LocationRepository 接口

**文件：** `data/LocationRepository.kt`

```kotlin
interface LocationRepository {
    suspend fun getCurrentLocation(): Result<LocationResult>
}
```

**设计要点：**
- 与 WeatherRepository 解耦，各自遵循单一职责
- 返回 `Result<LocationResult>`，统一异常封装
- ViewModel 层负责串联「定位 → 取天气」的流程

### 2.5 FusedLocationRepository

**文件：** `data/FusedLocationRepository.kt`

基于 Google Play Services `FusedLocationProviderClient` 的定位实现。

**技术栈：**
- 定位引擎：`FusedLocationProviderClient`（Wear OS 上优先使用 GPS + 网络融合）
- 协程桥接：`kotlinx-coroutines-play-services` 的 `Task.await()` 扩展
- 线程调度：`withContext(Dispatchers.Default)`

**获取策略：**
1. 优先调用 `lastLocation` 获取缓存位置（快速、低功耗）
2. 若 `lastLocation` 为 null（常见于 Wear OS 首次启动或长时间未使用定位），
   降级为 `requestLocationUpdates` 单次请求（`maxUpdates = 1`）
3. 成功 → `Result.success(LocationResult)`
4. 失败 → `Result.failure(LocationUnavailableException)`
5. 权限缺失 → `Result.failure(LocationPermissionDeniedException)`

**注意**：运行时权限检查在 Activity/Compose 层进行，Repository 只负责「有权限时的定位获取」。

### 2.6 GeocodingRepository 接口

**文件：** `data/GeocodingRepository.kt`

```kotlin
interface GeocodingRepository {
    suspend fun getLocationName(lat: Double, lon: Double): String?
}
```

反向地理编码接口，根据经纬度解析地址名称。

### 2.7 AndroidGeocodingRepository

**文件：** `data/AndroidGeocodingRepository.kt`

基于 Android 原生 Geocoder 的反向地理编码实现。

**技术栈：**
- API 33+ 使用异步 listener 方式
- API 33 以下使用阻塞式调用（IO 线程）
- 失败时返回 null，由调用方决定降级策略

### 2.8 CityRepository 接口

**文件：** `data/CityRepository.kt`

```kotlin
interface CityRepository {
    fun searchCities(query: String): List<City>
    fun getAllCities(): List<City>
}
```

城市搜索仓库接口，提供城市查询能力。

### 2.9 LocalCityRepository

**文件：** `data/LocalCityRepository.kt`

本地城市库实现，当前仅包含佛山市五区。

**城市库内容：**
| 城市名 | 纬度 | 经度 |
|--------|------|------|
| 佛山市 禅城区 | 23.0218 | 113.1220 |
| 佛山市 南海区 | 23.0323 | 113.1480 |
| 佛山市 顺德区 | 22.8400 | 113.2800 |
| 佛山市 三水区 | 23.1700 | 112.8900 |
| 佛山市 高明区 | 22.9000 | 112.7900 |

**搜索规则（任一满足即匹配，不区分大小写）：**
1. 中文名称包含查询关键字（如「禅城」「佛山市」）
2. 完整拼音包含查询关键字（如「chancheng」「nanhai」）
3. 拼音首字母缩写包含查询关键字（如「fs」「ccq」「sdq」）

**GPS 默认城市：** `GPS_DEFAULT_CITY` = 佛山市禅城区（点击 GPS 定位时的回退城市）

### 2.10 数据模型

**文件：** `data/model/Weather.kt`

所有数据类均添加 `@Immutable` 注解，确保 Compose 编译器识别为稳定类型。

#### WeatherSnapshot

天气数据的顶层容器，一次获取包含三部分：

```kotlin
data class WeatherSnapshot(
    val current: CurrentWeather,   // 实时天气
    val today: DailySummary,       // 今日摘要
    val hourly: List<HourlyForecast> // 逐时预报（12小时）
)
```

#### CurrentWeather

当前天气详情：

| 字段 | 类型 | 说明 |
|------|------|------|
| temp | Double | 当前温度（摄氏度） |
| feelsLike | Double | 体感温度（摄氏度） |
| icon | WeatherIcon | 天气图标枚举 |
| description | String | 天气描述文本 |
| updatedAt | Long | 更新时间戳（毫秒） |

#### HourlyForecast

逐时预报：

| 字段 | 类型 | 说明 |
|------|------|------|
| epochSecond | Long | 时间戳（秒级） |
| temp | Double | 温度（摄氏度） |
| icon | WeatherIcon | 天气图标枚举 |
| precipProbability | Double | 降水概率（0.0 ~ 1.0） |

#### DailySummary

今日摘要：

| 字段 | 类型 | 说明 |
|------|------|------|
| minTemp | Double | 最低温度 |
| maxTemp | Double | 最高温度 |
| precipProbability | Double | 降水概率 |

#### WeatherIcon

**文件：** `data/model/WeatherIcon.kt`

OWM 天气图标的枚举映射，共 18 种（白天/夜晚各 9 种）：

| 枚举值 | OWM Code | 说明 |
|--------|----------|------|
| CLEAR_DAY / CLEAR_NIGHT | 01d / 01n | 晴 |
| FEW_CLOUDS_DAY / FEW_CLOUDS_NIGHT | 02d / 02n | 少云 |
| SCATTERED_CLOUDS_DAY / SCATTERED_CLOUDS_NIGHT | 03d / 03n | 多云 |
| BROKEN_CLOUDS_DAY / BROKEN_CLOUDS_NIGHT | 04d / 04n | 阴天 |
| SHOWER_RAIN_DAY / SHOWER_RAIN_NIGHT | 09d / 09n | 阵雨 |
| RAIN_DAY / RAIN_NIGHT | 10d / 10n | 雨 |
| THUNDERSTORM_DAY / THUNDERSTORM_NIGHT | 11d / 11n | 雷暴 |
| SNOW_DAY / SNOW_NIGHT | 13d / 13n | 雪 |
| MIST_DAY / MIST_NIGHT | 50d / 50n | 雾 |

`fromCode(code: String?)` 方法用于从 OWM 返回的图标代码反查枚举，默认返回 `CLEAR_DAY`。

#### LocationResult

**文件：** `data/model/LocationResult.kt`

定位结果数据模型：

| 字段 | 类型 | 说明 |
|------|------|------|
| latitude | Double | 纬度 |
| longitude | Double | 经度 |
| accuracy | Float | 精度（米） |
| epochSecond | Long | 定位时间戳（秒级） |

#### City

**文件：** `data/model/City.kt`

城市数据模型，用于本地城市库搜索：

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 中文显示名（如「佛山市 禅城区」） |
| pinyin | String | 完整拼音（无空格无音调） |
| abbreviation | String | 拼音首字母缩写 |
| latitude | Double | 纬度 |
| longitude | Double | 经度 |

### 2.11 QWeatherRepository（和风天气，当前默认数据源）

**文件：** `data/QWeatherRepository.kt`

和风天气（QWeather）实现，当前应用的默认天气数据源。一次 `fetchWeather` 并行请求三个接口：

| 接口 | 用途 | 映射到 |
|------|------|--------|
| `/v7/weather/now` | 实时天气 | `CurrentWeather`（temp / feelsLike / icon / text） |
| `/v7/weather/24h` | 逐时预报 | 前 12 条 `HourlyForecast`（fxTime / temp / icon / pop） |
| `/v7/weather/3d` | 天级预报 | `DailySummary`（tempMin / tempMax） |

**与 OWM 实现的关键差异：**

| 差异点 | 说明 |
|--------|------|
| 坐标参数顺序 | `location=经度,纬度`（lon,lat），与 OWM 相反 |
| 数值类型 | 所有数值均为字符串（如 `"31"`），映射时转 `Double` |
| 状态码 | 业务码在响应体 `code` 字段，`"200"` 才视为成功；HTTP 非 200 抛 `ApiException` |
| 图标码 | QWeather 数字码（100~104 白天 / 150~154 夜间 / 300+ 雨雪雷雾），见 `WeatherIcon.fromQWeatherCode()` |
| 降水概率 | `pop` 为百分比字符串（`"32"` = 32%），转 0.0~1.0；3d 接口无 pop，今日降水概率取当日逐时 pop 最大值 |
| 域名 | 使用控制台分配的专属 API Host（2026 年起公共域名 `api.qweather.com` / `devapi.qweather.com` 已停用） |

**凭据注入：**
- `local.properties`（已被 .gitignore 排除）配置 `qweather.apiKey` / `qweather.apiHost`
- `app/build.gradle.kts` 通过 `buildConfigField` 注入 `BuildConfig.QWEATHER_API_KEY` / `QWEATHER_API_HOST`
- `MainActivity` 构造 `QWeatherRepository`；凭据缺失时回退 `MockWeatherRepository`，保证项目开箱可跑

### 2.12 异常体系

天气相关异常定义在 `MockWeatherRepository.kt` 文件底部，定位相关异常定义在 `FusedLocationRepository.kt` 文件底部：

| 异常类 | 触发场景 | UI 展示 |
|--------|---------|---------|
| NoNetworkException | 无网络连接 | 请检查网络连接 |
| TimeoutException | 请求超时 | 网络请求超时，请重试 |
| ApiException | 服务端错误（非 200） | 天气服务暂不可用 |
| GpsUnavailableException | 无法获取位置（天气层） | 无法获取位置信息 |
| LocationPermissionDeniedException | 定位权限被拒绝 | 缺少位置权限 |
| LocationUnavailableException | 定位服务不可用（定位层） | 无法获取位置信息 |

异常在 ViewModel 层通过 `toErrorType()` 转换为 `WeatherUiState.ErrorType` 枚举，UI 层不直接处理异常类。

---

## 3. ViewModel 层详解

### 3.1 WeatherViewModel

**文件：** `ui/WeatherViewModel.kt`

**职责：**
- 管理 UI 状态（`WeatherUiState`）
- 协调数据获取（调用 Repository）
- 处理刷新逻辑（防重复、状态合并）
- 异常转换（Throwable → ErrorType）

**关键属性：**

| 属性 | 类型 | 说明 |
|------|------|------|
| uiState | `StateFlow<WeatherUiState>` | UI 状态流（只读对外暴露） |
| showCitySearch | `StateFlow<Boolean>` | 是否显示城市搜索页 |
| citySearchResults | `StateFlow<List<City>>` | 城市搜索结果列表 |
| refreshJob | `Job?` | 当前刷新任务，用于防重复刷新 |
| lastLocationName | `String` | 最近一次成功获取的位置名称，失败页展示兜底 |
| weatherRepository | `WeatherRepository` | 天气数据源（构造注入，默认 Mock） |
| locationRepository | `LocationRepository?` | 定位数据源（构造注入，可空） |
| geocodingRepository | `GeocodingRepository?` | 地理编码数据源（构造注入，可空） |
| cityRepository | `CityRepository?` | 城市搜索数据源（构造注入，可空） |

**关键方法：**

- `refreshWithPermissionCheck(hasPermission: Boolean)`：权限检查入口
  - 无权限 → 直接设置 `Error(PERMISSION_DENIED)`
  - 有权限 → 调用 `refresh()`
  - 由 Activity/Compose 层调用，因为权限状态只能在宿主层获取

- `refresh()`：发起刷新（GPS 定位 → 取天气）
  - 已有任务进行中则直接返回（防抖）
  - 已有数据时保留旧快照、只翻转 `isRefreshing` 标记
  - 无数据或错误时进入 Loading 状态
  - **流程**：先定位 → 再用定位坐标取天气
    - `locationRepository` 不为 null 时调用 `getCurrentLocation()`
    - 定位失败 → `Error(GPS_UNAVAILABLE)`
    - 定位成功 → 并行获取位置名称 + 天气数据
    - `locationRepository` 为 null 时回退到硬编码坐标
  - 通过 `viewModelScope` 启动协程，ViewModel 销毁时自动取消

- `openCitySearch()`：打开城市搜索页，初始化搜索结果为全部城市
- `closeCitySearch()`：关闭城市搜索页
- `searchCity(query: String)`：根据关键字搜索城市（中文名/拼音/缩写）
- `selectCity(city: City)`：选择城市，获取该城市天气并关闭搜索页
- `useGpsLocation()`：使用 GPS 定位获取天气并关闭搜索页

**注入方式：**
- 构造函数参数注入，默认使用 `MockWeatherRepository()`
- `locationRepository` / `geocodingRepository` / `cityRepository` 默认为 null（兼容测试）
- 未来接入 DI 框架（如 Hilt）后可改为自动注入
- 测试时可传入 Mock/Fake 实现

### 3.2 WeatherUiState

**文件：** `ui/WeatherViewModel.kt`（定义在文件底部）

UI 状态的密封接口，三种状态：

```kotlin
sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val snapshot: WeatherSnapshot, val isRefreshing: Boolean = false) : WeatherUiState
    data class Error(
        val type: ErrorType,
        val isRefreshing: Boolean = false,
        val locationName: String = ""   // 失败页展示的位置名称（最后一次成功定位/手动选择的位置）
    ) : WeatherUiState
}
```

**设计要点：**
- 使用 `sealed interface` 确保状态穷尽，`when` 表达式无需 `else`
- `Success` 和 `Error` 状态携带 `isRefreshing` 字段
  - 支持「已有数据 + 正在刷新」的中间状态
  - 避免刷新时整屏闪回 Loading，提升体验
- `Error` 状态携带 `locationName`：定位成功但取天气失败时展示已解析的位置名，
  定位失败时展示上一次成功的位置名（ViewModel 中 `lastLocationName` 兜底），
  失败页据此显示位置条并支持「手动选择城市」恢复
- `ErrorType` 枚举区分错误类型，UI 根据类型展示不同文案

**ErrorType 枚举：**

| 值 | 场景 |
|----|------|
| PERMISSION_DENIED | 定位权限被拒绝 |
| NO_NETWORK | 无网络连接 |
| TIMEOUT | 请求超时 |
| API_ERROR | API 服务错误 |
| GPS_UNAVAILABLE | GPS 不可用 |

### 3.3 状态流转

```
       ┌─────────────┐
       │   Loading   │  ← 初始状态 / 首次加载
       └──────┬──────┘
              │
     ┌────────┴────────┐
     ▼                 ▼
┌─────────┐       ┌─────────┐
│ Success │       │  Error  │
└────┬────┘       └────┬────┘
     │  刷新            │  重试
     ▼                  ▼
┌─────────────┐  ┌─────────────┐
│ Success +   │  │ Error +     │
│ isRefreshing│  │ isRefreshing│
└──────┬──────┘  └──────┬──────┘
       │                │
       └───────┬────────┘
               ▼
         成功/失败 → 回到 Success 或 Error
```

---

## 4. UI 层详解

### 4.1 WeatherAppScreen（顶层路由）

**文件：** `ui/WeatherAppScreen.kt`

应用的顶层 Composable，负责状态分发、页面路由与城市搜索导航：

```kotlin
@Composable
fun WeatherAppScreen(viewModel: WeatherViewModel, ...) {
    val state by viewModel.uiState.collectAsState()
    val showCitySearch by viewModel.showCitySearch.collectAsState()
    val citySearchResults by viewModel.citySearchResults.collectAsState()

    Box {
        if (showCitySearch) {
            CitySearchScreen(...) // 城市搜索页（覆盖层）
        } else {
            when (state) {     // 主界面
                is Loading -> LoadingScreen()
                is Success -> HomeScreen(...)
                is Error -> ErrorScreen(...)
            }
        }
    }
}
```

**导航逻辑：**
- 城市搜索页以覆盖层形式展示在主界面之上
- 点击 HomeScreen 顶部位置栏 → 打开城市搜索页
- 选择城市 / 点击 GPS 定位 / 点击返回 → 关闭城市搜索页

**性能要点：**
- 所有回调使用 `remember(viewModel)` 包裹，避免每次重组产生新 lambda
- `when` 表达式对 `sealed interface` 穷尽检查，无需 `else` 分支

### 4.2 HomeScreen（主屏）

**文件：** `ui/screens/HomeScreen.kt`

应用主界面，包含三部分：
1. **当前天气头部**：图标 + 温度 + 体感 + 描述
2. **今日摘要卡片**：最高/最低温、降水概率
3. **逐时预报列表**：可滚动的 12 小时预报

**列表实现：** `ScalingLazyColumn`（Wear OS 专用的可缩放懒加载列表）

**表圈交互：**
```kotlin
rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollableState = listState)
```
- 绑定旋转表圈输入，旋转表圈可滚动列表
- 使用 snap 行为，每次旋转对齐到 item 边界

**性能优化：**
- 每个 item 都有稳定 `key`（如 `"hourly-${forecast.epochSecond}"`）
- 刷新时 Compose 可复用对应 item 的 Composable 实例，只重组数据变化的行

**组件拆分：**
- `CurrentWeatherHeader`：当前天气头部
- `TodaySummaryRow`：今日摘要卡片
- `HourlyForecastRow`：逐时预报单行
- `RefreshFooter`：刷新按钮 + 更新时间
- `SummaryCell`：摘要单元格

### 4.3 CitySearchScreen（城市搜索页）

**文件：** `ui/screens/CitySearchScreen.kt`

城市搜索/选择界面，以覆盖层形式展示在主界面之上。

**组成部分：**
1. **顶部标题栏**：返回按钮 + 「选择城市」标题
2. **搜索输入框**：支持中文/拼音/首字母搜索，带清空按钮
3. **GPS 定位项**：高亮显示，点击后使用 GPS 定位
4. **城市列表**：匹配搜索结果的城市 Chip 列表
5. **无结果提示**：搜索无匹配时显示

**交互：**
- 进入页面自动聚焦搜索框
- 搜索框输入实时过滤城市列表
- 点击城市 Chip → 选择该城市并获取天气 → 关闭搜索页
- 点击 GPS 定位 Chip → 走 GPS 定位流程 → 关闭搜索页
- 点击返回按钮 → 关闭搜索页
- 旋转表圈控制列表滚动（snap 行为）

**列表实现：** `ScalingLazyColumn` + `RotaryScrollableDefaults.snapBehavior`

**性能优化：**
- 每个 item 都有稳定 `key`（如 `"city-${city.name}"`）
- 搜索输入框状态由 Composable 内部管理（`remember`），不触发全页重组

### 4.4 LoadingScreen / ErrorScreen

**文件：** `ui/screens/LoadingScreen.kt`、`ui/screens/ErrorScreen.kt`

- **LoadingScreen**：居中显示圆形进度条
- **ErrorScreen**：根据 `ErrorType` 展示不同错误信息，并提供位置相关的恢复能力
  - 顶部**位置条**（与 HomeScreen 共用 `LocationBar` 组件）：展示最后一次已知位置，
    位置为空时显示「未获取位置」；点击进入城市选择
  - 主按钮：**重试**（权限拒绝时为「去设置」）
  - 次按钮：**手动选择城市**——按用户所选位置重新获取天气，不依赖 GPS/网络自动定位
  - 位置名由 `WeatherUiState.Error.locationName` 提供，来源见 [3.2 WeatherUiState](#32-weatheruistate)

### 4.4 WeatherIcon 组件

**文件：** `ui/components/WeatherIcon.kt`

Canvas 矢量绘制的天气图标组件。

**为什么不用 emoji：**
- 手表系统字体回退扫描会导致卡顿
- emoji 在不同设备上渲染不一致
- Canvas 绘制性能更好，可控性更强

**支持的图标类型：** 与 `WeatherIcon` 枚举一一对应（18 种）

**绘制方式：** 使用 `androidx.compose.ui.graphics.Canvas` 绘制矢量图形

### 4.5 主题系统

**文件：** `ui/theme/Theme.kt`

基于 Wear Compose Material 的主题配置。

**主题组成：**
- 颜色：深色主题为主（适配手表 AMOLED 屏幕）
- 排版：Material 3 typography
- 形状：圆角风格

---

## 5. 性能优化指南

手表端性能敏感，所有 UI 改动都应考虑性能影响。

### 5.1 Compose 重组优化

**已实施的优化：**

| 优化手段 | 说明 | 位置 |
|---------|------|------|
| `@Immutable` 数据类 | Compose 编译器识别为稳定类型，内容未变则跳过重组 | Weather.kt / City.kt |
| Strong Skipping | Kotlin 2.0 Compose 编译器默认开启，稳定参数的 Composable 可跳过重组 | build.gradle.kts |
| 稳定 lambda | `remember` 包裹回调，避免每次重组产生新实例 | WeatherAppScreen.kt |
| 单一状态流 | 合并 `uiState` 与 `isRefreshing`，一次刷新只触发一轮重组 | WeatherViewModel.kt |
| 静态格式化器 | `DateTimeFormatter` 静态共享，刷新/滚动不重复创建 | HomeScreen.kt |

**开发时注意：**
- 新增数据类必须加 `@Immutable`
- Composable 函数的参数应尽量使用稳定类型
- 不要在 Composable 中直接创建对象（用 `remember` 包裹）
- lambda 回调如果传给子组件，用 `remember` 包裹

### 5.1.1 绘制缓存

**已实施的优化：**

| 优化手段 | 说明 | 位置 |
|---------|------|------|
| `drawWithCache` | 图标缩放换算、贝塞尔路径在组合期构建并缓存，帧绘制直接复用，避免每帧重建变换矩阵 | WeatherIcon.kt / LocationPinIcon.kt |
| 路径预构建 | `Path.combine`（月牙）与贝塞尔 Path 在 `remember` 中构建一次 | 同上 |

**开发时注意：**
- 新增矢量图标优先用 `drawWithCache` + `remember` 缓存路径，不要用 emoji（字体回退扫描卡顿）
- 绘制参数（颜色、尺寸换算）放进缓存块，避免在每帧绘制 lambda 中重复计算

### 5.1.2 冷启动

**已实施的优化：**

| 优化手段 | 说明 | 位置 |
|---------|------|------|
| 深色 `windowBackground` | 冷启动到首帧期间窗口显示深色背景，不白屏闪烁 | themes.xml |
| 移除冗余 `setTheme` | 主题已在 manifest 声明，Activity 不再重复设置 | MainActivity.kt |

### 5.1.3 网络层

| 优化手段 | 说明 | 位置 |
|---------|------|------|
| 共享 OkHttpClient | 连接池跨刷新复用，收紧超时（连接 5s/读取 10s/整体 15s），网络异常更快暴露 | QWeatherRepository.kt |
| 共享 Json 实例 | 反序列化器单例复用，避免重复创建配置对象 | 同上 |

### 5.2 列表性能

**已实施的优化：**

| 优化手段 | 说明 |
|---------|------|
| `ScalingLazyColumn` | Wear OS 懒加载列表，只渲染可见区域 |
| 稳定 `key` | 每个 item 有唯一 key，刷新时 Compose 可复用实例 |
| `@Immutable` 列表项数据 | 数据未变时跳过对应 item 的重组 |

**开发时注意：**
- 列表 item 的 key 必须稳定且唯一
- 不要在 item 的 Composable 中做耗时计算
- 列表数据类必须是 `@Immutable`

### 5.3 Baseline Profile

**作用：** 预编译应用热路径，提升首次启动和滚动流畅度

**生成模块：** `:baselineprofile`（`com.android.test` 类型）
**打包配置：** app 模块应用 `androidx.baselineprofile` 插件并声明
`baselineProfile(project(":baselineprofile"))` 依赖；release 变体构建时
AGP 自动生成 dex 布局优化 profile（`assets/dexopt/baseline.prof`）
**安装器：** `androidx.profileinstaller`（manifest 自动合并
`ProfileInstallerInitializer`，首次启动安装基线配置）
**生成命令：** `./gradlew :baselineprofile:generateBaselineProfile`

**注意：**
- 生成需要连接手表（或模拟器）并开启 ADB
- 生成一次即可，重新 Build APK 会自动打包
- 未生成不影响功能，只是没有设备实测热路径的基线优化（release 仍含 AGP 自动生成的 dex 布局 profile）

---

## 6. 测试指南

### 6.1 测试结构

```
test/kotlin/com/galaxy/weatherwatch/
├── util/
│   └── MainDispatcherRule.kt    # 协程测试调度器规则（必须）
├── data/
│   ├── MockWeatherRepositoryTest.kt
│   └── OpenWeatherRepositoryTest.kt
├── data/model/
│   └── WeatherIconTest.kt
└── ui/
    ├── WeatherViewModelTest.kt
    ├── components/
    │   └── WeatherIconGlyphTest.kt
    └── theme/
        └── ThemeTest.kt
```

### 6.2 测试工具

| 工具 | 用途 | 依赖 |
|------|------|------|
| JUnit4 | 测试框架 | junit |
| MockK | Mock 框架 | mockk |
| Turbine | Flow 测试 | turbine |
| kotlinx-coroutines-test | 协程测试 | kotlinx-coroutines-test |

### 6.3 编写测试的原则

1. **数据层必须测试**：Repository 的成功/失败路径、数据转换、异常映射
2. **ViewModel 必须测试**：状态流转、刷新逻辑、错误处理、防抖
3. **有计算逻辑的 UI 组件要测试**：如图标绘制、时间格式化
4. **测试应独立**：每个测试方法不依赖其他测试的状态
5. **使用 TestDispatcher**：协程测试必须使用 `TestDispatcher` 和 `MainDispatcherRule`

**运行测试：**
```bash
./gradlew :app:testDebugUnitTest
```

---

## 7. 构建与发布

### 7.1 构建类型

| 类型 | 混淆 | 资源压缩 | 用途 |
|------|------|---------|------|
| debug | 关闭 | 关闭 | 日常开发调试 |
| release | 开启 | 开启 | 正式发布 |

### 7.2 版本管理

- `versionCode`：整数，每次发布递增
- `versionName`：语义化版本号（如 "1.0"）
- 配置位置：`app/build.gradle.kts` → `defaultConfig`

### 7.3 混淆规则

- 配置文件：`app/proguard-rules.pro`
- 默认使用 Android 优化混淆规则
- 数据类使用 `@Serializable`，序列化字段需确保不被混淆（kotlinx-serialization 自动处理）

---

## 8. 编码规范

### 8.1 Kotlin 风格

- 遵循官方 [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- 使用表达式函数体（当函数体只有一个表达式时）
- 优先使用 `val` 而非 `var`
- 优先使用不可变集合（`List` / `Map` 而非 `MutableList` / `MutableMap`）
- 使用 `Result<T>` 处理可恢复错误，不滥用异常

### 8.2 Compose 约定

- Composable 函数首字母大写
- Modifier 参数放在第一个可选参数位置（紧跟必需参数之后）
- 有状态的 Composable 应提供「无状态版本 + 有状态版本」
- 事件回调参数命名为 `onXxx`（如 `onRefresh`、`onRetry`）
- 列表 item 必须提供 `key`

### 8.3 命名规范

| 类型 | 规则 | 示例 |
|------|------|------|
| 包名 | 全小写，点分隔 | `com.galaxy.weatherwatch.data` |
| 类/接口 | 大驼峰 | `WeatherViewModel`、`WeatherRepository` |
| 函数 | 小驼峰 | `fetchWeather()`、`toSnapshot()` |
| 变量/属性 | 小驼峰 | `uiState`、`isRefreshing` |
| 常量 | 全大写下划线分隔 | `MAX_RETRY_COUNT` |
| 枚举值 | 全大写下划线分隔 | `CLEAR_DAY` |
| Composable | 大驼峰 | `HomeScreen`、`WeatherIcon` |
| ViewModel | 以 `ViewModel` 结尾 | `WeatherViewModel` |
| UI 状态 | 以 `UiState` 结尾 | `WeatherUiState` |
| 屏幕级 Composable | 以 `Screen` 结尾 | `HomeScreen` |
| Repository 实现 | 前缀 + Repository | `MockWeatherRepository` |

---

## 9. 依赖管理

所有依赖版本统一在 `gradle/libs.versions.toml` 中管理。

**结构：**
- `[versions]`：版本号定义
- `[libraries]`：库依赖定义
- `[plugins]`：插件定义

**升级依赖：**
1. 修改 `libs.versions.toml` 中的版本号
2. 同步 Gradle
3. 运行测试确保无问题
4. 更新 README.md 中的环境要求表格

---

## 10. 常见问题

### Q: 如何切换数据源？

当前默认数据源已是和风天气 `QWeatherRepository`（凭据在 `local.properties`，经 BuildConfig 注入，见 2.11）。
如需切换回本地假数据，将 `MainActivity` 中 `weatherRepository` 改为 `MockWeatherRepository()` 即可；
`WeatherViewModel` 只依赖 `WeatherRepository` 接口，数据层替换无需改动上层。未来可通过 DI 框架（如 Hilt）统一管理。

### Q: 如何添加新的天气状态？

1. 在 `WeatherIcon` 枚举中添加新值
2. 在 `WeatherIcon` Composable 中添加对应绘制逻辑
3. 更新 `WeatherIconTest` 测试
4. 更新本文档的数据模型章节

### Q: 如何添加新的错误类型？

1. 在数据层添加对应的异常类
2. 在 `WeatherUiState.ErrorType` 中添加枚举值
3. 在 `toErrorType()` 扩展函数中添加映射
4. 在 `ErrorScreen` 中添加对应文案
5. 更新 ViewModel 测试
6. 更新本文档

### Q: 为什么用 Result 而不是直接抛异常？

- 调用方必须处理错误（`fold` / `getOrNull` / `getOrElse`）
- 异常信息不会泄漏到 UI 层
- 便于测试错误路径
- 符合函数式编程风格

### Q: 为什么用 sealed interface 而不是 sealed class？

- `sealed interface` 更轻量，无构造函数开销
- 状态实现类可以是 `data object`（单例）或 `data class`
- 语义上，状态是「行为接口」而非「抽象类」
