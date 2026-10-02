# 定位功能实现方案

> 目标：为 Wear OS 天气应用增加 GPS 定位功能，定位成功后仍然使用 Mock 模拟天气数据。

---

## 一、背景与现状

### 1.1 当前状态
| 项 | 状态 | 说明 |
|----|------|------|
| 定位权限声明 | ✅ 已完成 | AndroidManifest 已声明 `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` |
| 定位依赖 | ✅ 已完成 | `play-services-location:21.3.0` 已加入依赖 |
| 运行时权限申请 | ❌ 未实现 | 代码中无权限请求逻辑 |
| 定位获取逻辑 | ❌ 未实现 | ViewModel 中硬编码北京坐标 (39.9042, 116.4074) |
| 错误状态定义 | ✅ 已完成 | `ErrorType.PERMISSION_DENIED` / `GPS_UNAVAILABLE` 已定义 |
| 天气数据源 | Mock | 当前使用 `MockWeatherRepository` |

### 1.2 需求
1. 应用启动/刷新时先获取当前 GPS 位置
2. 定位成功后，使用获取到的经纬度调用 WeatherRepository（**仍然使用 Mock 数据源**）
3. 定位失败（权限拒绝、GPS 不可用等）时展示对应错误界面
4. 符合项目现有架构规范（MVVM + Repository、`Result<T>` 异常封装、单一 `StateFlow`）

---

## 二、架构设计

### 2.1 整体流程

```
用户启动/点击刷新
       │
       ▼
  检查定位权限 ──→ 未授权 ──→ 请求权限 ──→ 被拒 ──→ Error(PERMISSION_DENIED)
       │                                    │
       │ 已授权                              │ 授权
       ▼                                    │
  获取当前位置 ──→ 失败 ──→ Error(GPS_UNAVAILABLE)
       │
       ▼ 成功 (lat, lon)
  fetchWeather(lat, lon)  ──→ MockWeatherRepository（忽略坐标，返回固定数据）
       │
       ▼
  WeatherUiState.Success
```

### 2.2 模块职责

| 层级 | 新增/修改 | 职责 |
|------|----------|------|
| data | **新增** `LocationRepository` + `FusedLocationRepository` | 封装定位能力，对外暴露 `getCurrentLocation()` |
| data/model | **新增** `LocationResult` 数据类 | 定位结果模型 |
| ui | **修改** `WeatherViewModel` | 串联「定位 → 取天气」流程 |
| ui | **修改** `WeatherAppScreen` / `MainActivity` | 权限申请的宿主与回调 |
| ui/screens | **修改** `ErrorScreen` | 权限拒绝时跳转应用设置 |

---

## 三、详细设计

### 3.1 数据层

#### 3.1.1 新增文件：`data/LocationRepository.kt`

```kotlin
interface LocationRepository {
    suspend fun getCurrentLocation(): Result<LocationResult>
}
```

#### 3.1.2 新增文件：`data/model/LocationResult.kt`

```kotlin
@Immutable
data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val epochSecond: Long
)
```

#### 3.1.3 新增文件：`data/FusedLocationRepository.kt`

- 构造参数：`FusedLocationProviderClient`、`CoroutineDispatcher`（默认 `Dispatchers.Default`）
- 实现 `getCurrentLocation()`：
  - 调用 `fusedLocationClient.lastLocation`（挂起等待，使用 `kotlinx-coroutines-play-services` 的 `await()`）
  - 若 lastLocation 为 null，尝试 `requestLocationUpdates` 单次获取（Wear OS 上 lastLocation 可能为空）
  - 成功 → `Result.success(LocationResult)`
  - 失败 → `Result.failure(LocationUnavailableException)`
- 文件底部定义自定义异常：
  - `LocationPermissionDeniedException : Exception("定位权限被拒绝")`
  - `LocationUnavailableException : Exception("无法获取位置信息")`

> **注意**：权限检查在 ViewModel 层（或 Activity 层）做，Repository 只负责「有权限时的定位获取」。

### 3.2 ViewModel 层

#### 3.2.1 修改：`ui/WeatherViewModel.kt`

**构造函数变更**：
```kotlin
class WeatherViewModel(
    private val weatherRepository: WeatherRepository = MockWeatherRepository(),
    private val locationRepository: LocationRepository? = null // 新增，可空以兼容现有测试
) : ViewModel()
```

**新增状态**：
- 在 `WeatherUiState` 中无需新增状态，复用现有的 `Loading` / `Success` / `Error`。
- 新增一个 **权限检查入口** 方法：

```kotlin
/**
 * 检查定位权限并启动刷新流程
 * @param hasPermission 调用方（Activity/Compose）传入的权限状态
 */
fun refreshWithPermissionCheck(hasPermission: Boolean) {
    if (!hasPermission) {
        _uiState.value = WeatherUiState.Error(ErrorType.PERMISSION_DENIED, isRefreshing = false)
        return
    }
    refresh()
}
```

**修改 `refresh()` 方法**：

当前流程（伪代码）：
```
loading → repository.fetchWeather(39.9, 116.4) → success/error
```

修改后流程：
```
loading → locationRepository.getCurrentLocation()
              ├─ 失败 → Error(GPS_UNAVAILABLE)
              └─ 成功(lat, lon) → weatherRepository.fetchWeather(lat, lon)
                                              ├─ 失败 → Error(对应类型)
                                              └─ 成功 → Success(snapshot)
```

- 当 `locationRepository` 为 null 时，回退到硬编码坐标（兼容旧测试和未注入场景）
- 新增异常映射：`LocationUnavailableException` → `ErrorType.GPS_UNAVAILABLE`

### 3.3 UI 层 - 权限申请

#### 3.3.1 修改：`MainActivity.kt`

使用 **Activity Result API** 注册权限请求 launcher：

```kotlin
private val requestLocationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    viewModel.refreshWithPermissionCheck(isGranted)
}
```

将 `requestLocationPermissionLauncher` 传递给 `WeatherAppScreen`，供权限拒绝时的"去设置"按钮和首次启动使用。

#### 3.3.2 修改：`ui/WeatherAppScreen.kt`

- 新增参数：`onRequestPermission: () -> Unit`、`onOpenAppSettings: () -> Unit`
- 首次进入 Composition 时，若状态为初始 Loading 且未授权，触发权限请求
- 将回调传递给 `ErrorScreen`

#### 3.3.3 修改：`ui/screens/ErrorScreen.kt`

- 当 `ErrorType.PERMISSION_DENIED` 时：
  - 按钮文字改为「去设置」
  - 点击调用 `onOpenAppSettings`（跳转应用详情设置页，让用户手动授权）
- 其他错误类型保持「重试」不变

#### 3.3.4 跳转应用设置

在 `MainActivity` 中提供 `openAppSettings()` 方法：
```kotlin
fun openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}
```

### 3.4 天气数据层保持不变

- `WeatherRepository.fetchWeather(lat, lon)` 接口签名不变
- `MockWeatherRepository` 仍然忽略传入的坐标，返回固定的模拟天气数据
- 定位功能只影响「能否进入 fetchWeather」，不影响天气数据本身

---

## 四、测试计划

### 4.1 新增测试：`LocationRepository` 单元测试

**文件**：`test/.../data/FusedLocationRepositoryTest.kt`

- 使用 `mockk` 模拟 `FusedLocationProviderClient`
- 测试用例：
  - `getCurrentLocation 成功返回位置`
  - `getCurrentLocation lastLocation 为 null 时的降级处理`
  - `getCurrentLocation 失败时返回 LocationUnavailableException`

### 4.2 更新测试：`WeatherViewModelTest.kt`

- 新增 Mock `LocationRepository`
- 新增测试用例：
  - `有权限时 refresh 先定位再取天气`
  - `定位失败时状态为 Error(GPS_UNAVAILABLE)`
  - `refreshWithPermissionCheck 无权限时直接返回 PERMISSION_DENIED`
  - `locationRepository 为 null 时回退到硬编码坐标`（兼容旧测试）

### 4.3 运行验证

```bash
./gradlew :app:testDebugUnitTest
```

---

## 五、涉及文件清单

| 操作 | 文件路径 |
|------|---------|
| 新增 | `app/src/main/kotlin/com/galaxy/weatherwatch/data/LocationRepository.kt` |
| 新增 | `app/src/main/kotlin/com/galaxy/weatherwatch/data/FusedLocationRepository.kt` |
| 新增 | `app/src/main/kotlin/com/galaxy/weatherwatch/data/model/LocationResult.kt` |
| 修改 | `app/src/main/kotlin/com/galaxy/weatherwatch/ui/WeatherViewModel.kt` |
| 修改 | `app/src/main/kotlin/com/galaxy/weatherwatch/ui/WeatherAppScreen.kt` |
| 修改 | `app/src/main/kotlin/com/galaxy/weatherwatch/ui/screens/ErrorScreen.kt` |
| 修改 | `app/src/main/kotlin/com/galaxy/weatherwatch/MainActivity.kt` |
| 新增 | `app/src/test/kotlin/com/galaxy/weatherwatch/data/FusedLocationRepositoryTest.kt` |
| 修改 | `app/src/test/kotlin/com/galaxy/weatherwatch/ui/WeatherViewModelTest.kt` |

---

## 六、里程碑与后续

- 本次实现完成 **M2（GPS 定位与权限）** 的核心功能
- 后续 M3 接入真实 OWM API 时，只需将 `WeatherRepository` 实现从 `MockWeatherRepository` 切换为 `OpenWeatherRepository`，定位层无需改动
- README.md 里程碑状态更新为 M2 已完成

---

## 七、风险与注意事项

1. **Wear OS 定位特殊性**：手表 GPS 首次获取可能较慢，`lastLocation` 经常返回 null，需要 fallback 到 `requestLocationUpdates`。
2. **权限对话框时机**：首次启动即请求权限可能显得突兀，但天气应用必须有位置才能工作，符合用户预期。
3. **Mock 数据一致性**：定位成功但天气仍是北京数据，需确认这是预期行为（用户已明确要求使用模拟数据）。
4. **测试可维护性**：ViewModel 中 `locationRepository` 设为可空默认 null，确保现有测试无需全部修改。
