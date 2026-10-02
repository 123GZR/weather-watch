# Galaxy Watch 4 Classic 天气 App

为 Galaxy Watch 4 Classic 打造的独立天气应用，利用机械表圈浏览逐时预报。

## 环境要求

| 组件 | 版本 |
|------|------|
| Android Studio | Hedgehog (2023.1) 或更新 |
| JDK | 17（Android Studio 自带） |
| Android SDK Platform | API 34（或 API 35） |
| Gradle | 8.9（自动下载） |
| Kotlin | 2.0.20 |

## 首次打开

1. 用 Android Studio 打开 `d:\app` 目录
2. Studio 会自动检测 `gradle-wrapper.properties`，并下载 Gradle 8.9
   - 如果提示 `gradle-wrapper.jar` 缺失，在 Studio 弹窗中选择 "Use Gradle Wrapper" 即可自动补全
3. SDK Manager 勾选 API 34（或 35）+ Build-Tools
4. 等待首次 Sync 完成

## 调试链路（无 WLAN）

电脑只负责编译，APK 通过手机端 WearOS 工具箱无线安装到手表：

1. 手机开移动热点，手表连热点
2. 手表：设置 → 关于手表 → 连续点"软件版本"7 次开启开发者模式
3. 手表：设置 → 开发者选项 → 开启 ADB 调试 + 无线调试
4. 手机端打开 WearOS 工具箱，输入配对码连接手表
5. 在 Android Studio 中 `Build → Build APK(s)` 生成 `app-debug.apk`
6. 把 APK 文件传到手机（微信/QQ/USB 均可）
7. 在 WearOS 工具箱中选 APK 安装

## 性能优化（P0，已完成）

- 数据模型加 `@Immutable`，Compose 编译器可跳过未变化子树的重组
- `uiState`/`isRefreshing` 合并为单一状态流，一次刷新只触发一轮重组
- 列表 item 使用稳定 `key`，刷新时只重组数据变化的行
- 天气图标由 emoji 改为 Canvas 矢量绘制，消除手表字体回退扫描卡顿
- 开启 Compose strong skipping（`composeCompiler.enableStrongSkipping`）
- 集成 Baseline Profile（profileinstaller + `:baselineprofile` 生成模块）

## 生成 Baseline Profile（可选，建议做一次）

手表开启 ADB/无线调试并连接电脑后执行，生成结果会自动打包进 APK：

```bash
./gradlew :baselineprofile:generateBaselineProfile
```

生成一次后重新 Build APK 并安装，首次启动时 profileinstaller 会预编译热路径，
进一步提升首帧与滚动流畅度；未生成时不影响功能，只是暂不享受基线优化。

## 项目结构

```
app/
├── build.gradle.kts                  # 模块依赖与编译配置
├── proguard-rules.pro
└── src/
    ├── main/
    │   ├── AndroidManifest.xml       # 权限、MainActivity、Wear OS 声明
    │   ├── kotlin/com/galaxy/weatherwatch/
    │   │   ├── WeatherApplication.kt
    │   │   ├── MainActivity.kt
    │   │   ├── data/
    │   │   │   ├── model/
    │   │   │   │   ├── Weather.kt           # 数据模型
    │   │   │   │   └── WeatherIcon.kt       # OWM 图标枚举
    │   │   │   ├── WeatherRepository.kt     # 仓库接口
    │   │   │   ├── MockWeatherRepository.kt # 本地假数据（当前使用）
    │   │   │   └── OpenWeatherRepository.kt # One Call API 3.0 实现
    │   │   └── ui/
    │   │       ├── WeatherAppScreen.kt      # 顶层路由
    │   │       ├── WeatherViewModel.kt      # 状态管理
    │   │       ├── theme/Theme.kt
    │   │       ├── components/WeatherIcon.kt
    │   │       └── screens/
    │   │           ├── HomeScreen.kt         # 实时天气+今日摘要+逐时
    │   │           ├── LoadingScreen.kt
    │   │           └── ErrorScreen.kt
    │   └── res/
    │       ├── values/{strings,colors,themes}.xml
    │       ├── drawable/ic_launcher_foreground.xml
    │       └── mipmap-anydpi-v26/ic_launcher{,_round}.xml
    └── test/kotlin/com/galaxy/weatherwatch/
        ├── data/MockWeatherRepositoryTest.kt
        ├── data/model/WeatherIconTest.kt
        ├── ui/WeatherViewModelTest.kt
        └── ui/components/WeatherIconGlyphTest.kt
```

## 里程碑进度

- [x] M1 项目骨架（本提交）
- [ ] M2 GPS 定位与权限
- [ ] M3 实时天气主屏（接入 OWM API）
- [ ] M4 表圈逐时预报校准
- [ ] M5 手动刷新 + 错误处理
- [ ] M6 真机验收

## 测试

```bash
./gradlew :app:testDebugUnitTest
```

## 数据源

OpenWeatherMap One Call API 3.0（暂未接入 Key，当前用 Mock 数据）。
