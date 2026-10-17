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
| Android Studio | 2025.x 或更新 |
| JDK | 17（Temurin，位于 `D:\jdk17`；命令行构建必须显式设置 `JAVA_HOME`） |
| Android SDK Platform | API 36（compileSdk/targetSdk = 36，minSdk = 30） |
| Gradle | 8.13（自动下载） |
| Kotlin | 2.2.21 |
| AGP | 8.13.2 |
| Compose BOM | 2025.09.00（Compose 1.9.2，与 Wear Compose 1.6.2 声明的 1.9.0 对齐） |
| Wear Compose | 1.6.2（material3 / foundation） |
| Navigation | navigation-compose 2.9.0（NavHost，API 36 首屏黑屏修复方案） |
| Hilt | 2.57.2（KSP 编译） |
| KSP | 2.2.21-2.0.5 |

> ⚠️ **命令行构建必须使用 JDK 17**：Gradle 8.13 不支持在更高版本 JDK（如 Studio 自带 JBR 25）上运行。
> 每次执行 Gradle 命令前：
> ```powershell
> $env:JAVA_HOME = "D:\jdk17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
> ```

依赖版本统一管理在 `gradle/libs.versions.toml`，升级依赖请修改该文件。

---

## 首次打开

1. 用 Android Studio 打开 `d:\app` 目录
2. Studio 会自动检测 `gradle-wrapper.properties`，并下载 Gradle 8.13
   - 如果提示 `gradle-wrapper.jar` 缺失，在 Studio 弹窗中选择 "Use Gradle Wrapper" 即可自动补全
3. SDK Manager 勾选 API 36 + Build-Tools（AGP 首次同步会自动安装）
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
    │   │   │   ├── WeatherExceptions.kt       # 天气业务异常（网络/超时/服务端/GPS）
    │   │   │   ├── QWeatherRepository.kt      # 和风天气实现（当前默认）
    │   │   │   ├── OpenWeatherRepository.kt   # OWM One Call 3.0 实现（保留参考）
    │   │   │   ├── LocationRepository.kt      # 定位仓库接口
    │   │   │   ├── FusedLocationRepository.kt # GPS 定位实现
    │   │   │   ├── GeocodingRepository.kt     # 地理编码接口
    │   │   │   ├── AndroidGeocodingRepository.kt # 反向地理编码实现
    │   │   │   ├── CityRepository.kt          # 城市搜索仓库接口
    │   │   │   ├── LocalCityRepository.kt     # 本地城市库（佛山五区）
    │   │   │   ├── CitySelectionRepository.kt # 城市选择记忆仓库接口
    │   │   │   └── SharedPreferencesCitySelectionRepository.kt # 记忆城市持久化（SharedPreferences）
    │   │   └── ui/                     # UI 层
    │   │       ├── WeatherAppScreen.kt # 顶层导航根（NavHost）
    │   │       ├── WeatherViewModel.kt # ViewModel + UI 状态（Hilt 注入）
    │   │       ├── di/DataModule.kt    # Hilt 装配（Provides / Binds）
    │   │       ├── theme/Theme.kt      # 主题配置（Wear M3）
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
│               QWeatherRepository              │
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

- ⌚ **Wear OS 原生体验**：基于 Wear Compose Material 3 + Compose Navigation（NavHost），适配圆形表盘
- 🌀 **表圈交互**：旋转表圈控制逐时预报列表滚动（snap 行为）
- 🔍 **手动城市搜索**：支持中文名、拼音、首字母缩写搜索，本地佛山五区城市库
- 📍 **GPS 定位 + 手动切换**：自动 GPS 定位，也可手动选择城市
- 🧠 **记忆城市回退**：记住最近一次手动选择的城市，GPS 定位失败时自动回退到该城市取天气，避免手表室内无信号时进入错误页
- 🔧 **失败页位置恢复**：错误页展示当前位置，可点击顶部位置条或「手动选择城市」按钮，按所选位置重新获取天气
- 🎨 **Canvas 矢量图标**：纯绘制天气图标，无字体依赖，性能更好
- ⚡ **Compose Strong Skipping**：Kotlin 2.0 编译器默认开启，跳过稳定参数的重组
- 📊 **单一状态流**：合并加载状态与数据，减少重组次数
- 🔄 **增量刷新**：已有数据时刷新不闪回 Loading 态
- 🚀 **Baseline Profile**：预编译热路径，提升首帧与滚动流畅度

---

## 性能优化（P0，已完成）

**组合/重组层**
- 数据模型加 `@Immutable`，Compose 编译器可跳过未变化子树的重组
- `uiState`/`isRefreshing` 合并为单一状态流，一次刷新只触发一轮重组
- 列表 item 使用稳定 `key`，刷新时只重组数据变化的行
- Compose strong skipping 由 Kotlin 2.0.20 编译器默认开启（无需显式配置）

**绘制层**
- 天气图标由 emoji 改为 Canvas 矢量绘制，消除手表字体回退扫描卡顿
- 图标使用 `drawWithCache` 缓存绘制指令（缩放换算、贝塞尔路径在组合期构建一次），
  滚动逐时预报时帧绘制直接复用，避免每帧重复计算变换矩阵

**启动层**
- 主题声明深色 `windowBackground`，冷启动到首帧期间窗口不白屏闪烁
- 移除 Activity 中冗余的重复 `setTheme` 调用

**网络/数据层**
- OkHttp 收紧超时（连接 5s / 读取 10s / 整体 15s）并共享客户端连接池，网络异常更快暴露
- `Json` 反序列化器提为共享单例，避免重复创建配置对象
- 时间格式化器静态共享，刷新/滚动时不重复创建 `DateTimeFormatter`

**构建/底层**
- 集成 Baseline Profile：app 模块应用 `androidx.baselineprofile` 插件并声明
  `baselineProfile(project(":baselineprofile"))`，release 变体自动产出
  `assets/dexopt/baseline.prof`（dex 布局优化），设备端生成的基线配置可打包进 APK
- release 开启 R8 混淆 + 资源压缩（APK 约 2.5MB）
- 显式提升 `fragment-ktx` 1.8.5（修复 play-services 传递依赖 fragment 1.1.0 导致的
  `registerForActivityResult` lint 错误，保证 release 构建可通过）

---

## 生成 Baseline Profile（可选，建议做一次）

release 构建已自动包含 AGP 生成的 dex 布局优化 profile（`assets/dexopt/baseline.prof`）。
如需进一步预编译**设备端实测热路径**（首帧 + 列表滚动），按以下步骤生成：

1. 手表开启 ADB/无线调试并连接电脑
2. 执行（生成结果经 `baselineProfile` 依赖自动打包进 APK）：

```bash
./gradlew :baselineprofile:generateBaselineProfile
```

3. 重新 Build APK 并安装，首次启动时 profileinstaller 会预编译热路径，
   进一步提升首帧与滚动流畅度；未生成时不影响功能，只是暂不享受设备基线优化。

---

## 测试

> ⚠️ 单元测试代码已移除（2026-10），构建脚本中的测试依赖（JUnit / MockK / Turbine 等）保留，
> 便于将来恢复测试。需要恢复时在 `app/src/test/` 重新添加测试文件即可。

历史测试范围（删除前）：
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
- 凭据缺失时请求会返回业务错误码，UI 展示「天气服务暂不可用」错误页（项目不再包含本地假数据源）
- 历史实现：`OpenWeatherRepository`（OWM One Call API 3.0，保留作参考）

> 切换数据源：只需修改 `MainActivity` 中 `weatherRepository` 的注入，`WeatherViewModel` 只依赖接口。

---

## 里程碑进度

- [x] M1 项目骨架
- [x] M1.5 性能优化基线（Immutable / Strong Skipping / Baseline Profile）
- [x] M2 GPS 定位与权限
- [x] M2.5 手动输入城市（本地佛山五区城市库 + 搜索）
- [x] M2.6 记忆城市回退（记住最近选择城市，定位失败自动回退）
- [x] M3 实时天气主屏（接入和风天气 API）
- [x] M3.5 Hilt 依赖注入（替换手写 ViewModel Factory）
- [x] M4 表圈逐时预报校准
- [x] M5 手动刷新 + 错误处理
- [x] M6 Wear M3 迁移 + Compose Navigation 导航
- [x] M7 真机验收（修复 API 36 导航首屏黑屏：改用 NavHost，放弃 Wear 右滑手势）

---

## 开发文档

更详细的开发指南请参考：

| 文档 | 说明 |
|------|------|
| [DEVELOPMENT.md](./DEVELOPMENT.md) | 架构设计、模块详解、编码规范、构建发布 |
| [AGENTS.md](./AGENTS.md) | AI 协作规范、文档更新要求、提交规范（AI 必读） |

> ⚠️ **AI 开发者注意**：每次新增或修改功能后，必须同步更新相关文档。
> 具体规则请阅读 [AGENTS.md](./AGENTS.md)。
