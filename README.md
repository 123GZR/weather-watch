# Galaxy Watch 4 Classic 天气 App

为 Galaxy Watch 4 Classic 打造的独立天气应用，利用机械表圈浏览逐时预报。

基于 **Kotlin + Jetpack Compose + Wear OS** 技术栈，采用 MVVM 架构，注重手表端性能优化。

---

## 目录

- [环境要求](#环境要求)
- [首次打开](#首次打开)
- [调试链路（无 WLAN）](#调试链路无-wlan)
- [项目结构](#项目结构)
- [架构概览](#架构概览)
- [核心特性](#核心特性)
- [性能优化](#性能优化)
- [生成 Baseline Profile](#生成-baseline-profile可选建议做一次)
- [测试](#测试)
- [数据源](#数据源)
- [里程碑进度](#里程碑进度)
- [开发文档](#开发文档)

---

## 环境要求

| 组件 | 版本 |
|------|------|
| Android Studio | Hedgehog (2023.1) 或更新 |
| JDK | 17（Android Studio 自带） |
| Android SDK Platform | API 34（或 API 35） |
| Gradle | 8.9（自动下载） |
| Kotlin | 2.0.20 |
| AGP | 8.6.0 |
| Wear Compose | 1.4.0 |

依赖版本统一管理在 `gradle/libs.versions.toml`，升级依赖请修改该文件。

---

## 首次打开

1. 用 Android Studio 打开 `d:\app` 目录
2. Studio 会自动检测 `gradle-wrapper.properties`，并下载 Gradle 8.9
   - 如果提示 `gradle-wrapper.jar` 缺失，在 Studio 弹窗中选择 "Use Gradle Wrapper" 即可自动补全
3. SDK Manager 勾选 API 34（或 35）+ Build-Tools
4. 等待首次 Sync 完成

> 项目已配置阿里云 Maven 镜像，国内网络环境下依赖下载通常无需额外配置。

---

## 调试链路（无 WLAN）

电脑只负责编译，APK 通过手机端 WearOS 工具箱无线安装到手表：

1. 手机开移动热点，手表连热点
2. 手表：设置 → 关于手表 → 连续点"软件版本"7 次开启开发者模式
3. 手表：设置 → 开发者选项 → 开启 ADB 调试 + 无线调试
4. 手机端打开 WearOS 工具箱，输入配对码连接手表
5. 在 Android Studio 中 `Build → Build APK(s)` 生成 `app-debug.apk`
6. 把 APK 文件传到手机（微信/QQ/USB 均可）
7. 在 WearOS 工具箱中选 APK 安装

---

## 项目结构

```
app/                                    # 主应用模块
├── build.gradle.kts                    # 模块依赖与编译配置
├── proguard-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml         # 权限、MainActivity、Wear OS 声明
    │   ├── kotlin/com/galaxy/weatherwatch/
    │   │   ├── WeatherApplication.kt   # Application 入口
    │   │   ├── MainActivity.kt         # 主 Activity
    │   │   ├── data/                   # 数据层
    │   │   │   ├── model/
    │   │   │   │   ├── Weather.kt      # 天气数据模型（Immutable）
    │   │   │   │   ├── WeatherIcon.kt  # 天气图标枚举（OWM / 和风映射）
    │   │   │   │   ├── LocationResult.kt # 定位结果模型
    │   │   │   │   └── City.kt         # 城市数据模型
    │   │   │   ├── WeatherRepository.kt       # 天气仓库接口
    │   │   │   ├── MockWeatherRepository.kt   # 本地假数据
    │   │   │   ├── QWeatherRepository.kt      # 和风天气实现（当前默认）
    │   │   │   ├── OpenWeatherRepository.kt   # OWM One Call 3.0 实现（保留参考）
    │   │   │   ├── LocationRepository.kt      # 定位仓库接口
    │   │   │   ├── FusedLocationRepository.kt # GPS 定位实现
    │   │   │   ├── GeocodingRepository.kt     # 地理编码接口
    │   │   │   ├── AndroidGeocodingRepository.kt # 反向地理编码实现
    │   │   │   ├── CityRepository.kt          # 城市搜索仓库接口
    │   │   │   └── LocalCityRepository.kt     # 本地城市库（佛山五区）
    │   │   └── ui/                     # UI 层
    │   │       ├── WeatherAppScreen.kt # 顶层路由（状态分发/城市搜索导航）
    │   │       ├── WeatherViewModel.kt # ViewModel + UI 状态
    │   │       ├── theme/Theme.kt      # 主题配置
    │   │       ├── components/
    │   │       │   ├── WeatherIcon.kt  # Canvas 矢量天气图标
    │   │       │   └── LocationPinIcon.kt # 定位 Pin 图标
    │   │       └── screens/
    │   │           ├── HomeScreen.kt   # 实时天气+今日摘要+逐时列表
    │   │           ├── CitySearchScreen.kt # 城市搜索/选择
    │   │           ├── LoadingScreen.kt
    │   │           └── ErrorScreen.kt
    │   └── res/
    │       ├── values/{strings,colors,themes}.xml
    │       ├── drawable/ic_launcher_foreground.xml
    │       └── mipmap-anydpi-v26/ic_launcher{,_round}.xml
    └── test/kotlin/com/galaxy/weatherwatch/
        ├── util/MainDispatcherRule.kt  # 协程测试调度器规则
        ├── data/MockWeatherRepositoryTest.kt
        ├── data/OpenWeatherRepositoryTest.kt
        ├── data/model/WeatherIconTest.kt
        ├── ui/WeatherViewModelTest.kt
        ├── ui/components/WeatherIconGlyphTest.kt
        └── ui/theme/ThemeTest.kt

baselineprofile/                        # Baseline Profile 生成模块
└── src/main/kotlin/.../BaselineProfileGenerator.kt
```

---

## 架构概览

项目采用 **MVVM + Repository** 架构：

```
┌─────────────────────────────────────────────┐
│                  UI Layer                    │
│  Screens / Components / WeatherAppScreen     │
│              (Jetpack Compose)               │
└──────────────────┬──────────────────────────┘
                   │ StateFlow (collectAsState)
                   ▼
┌─────────────────────────────────────────────┐
│              ViewModel Layer                 │
│             WeatherViewModel                 │
│    (状态管理、业务逻辑编排、协程调度)         │
└──────────────────┬──────────────────────────┘
                   │ WeatherRepository 接口
                   ▼
┌─────────────────────────────────────────────┐
│               Data Layer                     │
│  MockWeatherRepository / QWeatherRepository   │
│     (数据获取、解析、异常封装为 Result)       │
└─────────────────────────────────────────────┘
```
（当前默认数据源为和风天气 QWeatherRepository；OWM OpenWeatherRepository 保留作参考。）

**数据流方向：** 单向数据流
- Repository → ViewModel（`Result<WeatherSnapshot>`）
- ViewModel → UI（`StateFlow<WeatherUiState>`）
- UI → ViewModel（事件回调：`onRefresh` 等）

详细架构说明请参考 [DEVELOPMENT.md](./DEVELOPMENT.md)。

---

## 核心特性

- ⌚ **Wear OS 原生体验**：基于 Wear Compose Material，适配圆形表盘
- 🌀 **表圈交互**：旋转表圈控制逐时预报列表滚动（snap 行为）
- 🔍 **手动城市搜索**：支持中文名、拼音、首字母缩写搜索，本地佛山五区城市库
- 📍 **GPS 定位 + 手动切换**：自动 GPS 定位，也可手动选择城市
- 🔧 **失败页位置恢复**：错误页展示当前位置，可点击顶部位置条或「手动选择城市」按钮，按所选位置重新获取天气
- 🎨 **Canvas 矢量图标**：纯绘制天气图标，无字体依赖，性能更好
- ⚡ **Compose Strong Skipping**：Kotlin 2.0 编译器默认开启，跳过稳定参数的重组
- 📊 **单一状态流**：合并加载状态与数据，减少重组次数
- 🔄 **增量刷新**：已有数据时刷新不闪回 Loading 态
- 🧪 **完善测试**：数据层、ViewModel、UI 组件均有单元测试
- 🚀 **Baseline Profile**：预编译热路径，提升首帧与滚动流畅度

---

## 性能优化（P0，已完成）

- 数据模型加 `@Immutable`，Compose 编译器可跳过未变化子树的重组
- `uiState`/`isRefreshing` 合并为单一状态流，一次刷新只触发一轮重组
- 列表 item 使用稳定 `key`，刷新时只重组数据变化的行
- 天气图标由 emoji 改为 Canvas 矢量绘制，消除手表字体回退扫描卡顿
- 开启 Compose strong skipping（`composeCompiler.enableStrongSkipping`）
- 集成 Baseline Profile（profileinstaller + `:baselineprofile` 生成模块）

---

## 生成 Baseline Profile（可选，建议做一次）

手表开启 ADB/无线调试并连接电脑后执行，生成结果会自动打包进 APK：

```bash
./gradlew :baselineprofile:generateBaselineProfile
```

生成一次后重新 Build APK 并安装，首次启动时 profileinstaller 会预编译热路径，
进一步提升首帧与滚动流畅度；未生成时不影响功能，只是暂不享受基线优化。

---

## 测试

运行单元测试：

```bash
./gradlew :app:testDebugUnitTest
```

测试覆盖范围：
- **数据层**：Repository 实现、数据模型、异常处理
- **ViewModel 层**：状态流转、刷新逻辑、错误处理
- **UI 层**：组件绘制逻辑（如 WeatherIcon）、主题

测试工具栈：JUnit4 + MockK + Turbine + Coroutines Test

---

## 数据源

和风天气（QWeather）实时天气 API（当前默认，M3 已完成接入）。

- 接口文档：https://dev.qweather.com/docs/api/
- 实现类：`QWeatherRepository`（并行请求 now / 24h / 3d 三个接口）
- 凭据：在 `local.properties`（已被 .gitignore 排除，不会入库）中配置：
  ```
  qweather.apiKey=你的和风天气 Key
  qweather.apiHost=你的专属 API Host
  ```
  > API Host 在 console.qweather.com → 控制台 → 设置 中查看；2026 年起和风天气已停用公共域名
  > `api.qweather.com` / `devapi.qweather.com`，必须使用账号专属 Host（形如 `h2a9cf3mhs.xy.qweatherapi.com`）。
- 凭据缺失时自动回退 `MockWeatherRepository`（本地假数据），保证项目开箱可跑
- 历史实现：`OpenWeatherRepository`（OWM One Call API 3.0，保留作参考）

> 切换数据源：只需修改 `MainActivity` 中 `weatherRepository` 的注入，`WeatherViewModel` 只依赖接口。

---

## 里程碑进度

- [x] M1 项目骨架
- [x] M1.5 性能优化基线（Immutable / Strong Skipping / Baseline Profile）
- [x] M2 GPS 定位与权限
- [x] M2.5 手动输入城市（本地佛山五区城市库 + 搜索）
- [x] M3 实时天气主屏（接入和风天气 API）
- [ ] M4 表圈逐时预报校准
- [ ] M5 手动刷新 + 错误处理
- [ ] M6 真机验收

---

## 开发文档

更详细的开发指南请参考：

| 文档 | 说明 |
|------|------|
| [DEVELOPMENT.md](./DEVELOPMENT.md) | 架构设计、模块详解、编码规范、构建发布 |
| [AGENTS.md](./AGENTS.md) | AI 协作规范、文档更新要求、提交规范（AI 必读） |

> ⚠️ **AI 开发者注意**：每次新增或修改功能后，必须同步更新相关文档。
> 具体规则请阅读 [AGENTS.md](./AGENTS.md)。
