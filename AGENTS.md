# AI 协作开发规范

本项目主要由 AI 辅助开发，以下规则所有 AI 开发者必须严格遵守。

---

## 一、基础工作流（每次改动必须执行）

1、每次改动完成后，都必须创建一个对应的 Git commit，以便后续追踪和回滚。

2、每次改动后，都必须完成构建验证（`:app:assembleDebug`），并在交付给用户前确保验证通过。
   （单元测试已移除，见「四、测试要求」；如已恢复测试，仍需确保测试全部通过。）

3、**每次新增、修改或删除功能后，必须同步更新相关文档**（README.md / DEVELOPMENT.md / 本文件），
   确保文档与代码保持一致。文档更新应与代码改动在同一个 commit 中提交。

4、开始新任务前，先阅读 DEVELOPMENT.md 了解项目架构与约定，避免重复造轮子或破坏现有设计。

---

## 二、文档更新检查清单

每次提交代码前，请对照以下清单检查是否需要更新文档：

| 改动类型 | 需要更新的文档 |
|---------|--------------|
| 新增/删除/重命名模块或文件 | README.md 项目结构章节、DEVELOPMENT.md 架构章节 |
| 新增数据模型或修改字段 | DEVELOPMENT.md 数据模型章节 |
| 新增页面或 UI 组件 | DEVELOPMENT.md UI 层章节 |
| 修改状态管理或数据流 | DEVELOPMENT.md 状态管理章节 |
| 新增依赖或升级版本 | README.md 环境要求、libs.versions.toml 注释 |
| 新增 API 接口或数据源 | DEVELOPMENT.md 数据层章节 |
| 新增构建/部署流程 | DEVELOPMENT.md 构建与发布章节 |
| 修改里程碑进度 | README.md 里程碑进度 |
| 修改 AI 协作规则 | 本文件（AGENTS.md） |

> **提醒**：如果不确定是否需要更新文档，默认选择「更新」。
> 文档滞后比文档冗余的危害大得多。

---

## 三、编码约定

### 3.1 Kotlin / Compose

- 数据类一律加 `@Immutable`，确保 Compose 编译器可跳过未变化子树的重组
- 列表 item 必须提供稳定 `key`，避免刷新时全列表重组
- 所有 Composable 参数类型应保持稳定（`Immutable` 数据类 / `remember` 后的 lambda）
- 状态流合并为单一 `StateFlow`，一次刷新只推送一次状态、只触发一轮重组
- 天气图标使用 Canvas 矢量绘制，**禁止使用 emoji**（手表字体回退扫描会导致卡顿）

### 3.2 命名规范

- 包名统一使用 `com.galaxy.weatherwatch` 前缀
- ViewModel 类以 `ViewModel` 结尾，UI 状态以 `UiState` 结尾
- 屏幕级 Composable 以 `Screen` 结尾，组件级以组件功能命名
- Repository 接口直接命名，实现类加前缀（如 `QWeatherRepository`、`FusedLocationRepository`）

### 3.3 异常处理

- 数据层所有异常封装为 `Result<T>` 返回
- 业务异常统一放在 `data/WeatherExceptions.kt`（天气层）与 `FusedLocationRepository.kt` 底部（定位层）
- UI 层通过 `WeatherUiState.ErrorType` 枚举区分错误类型，不直接展示异常消息

---

## 四、测试要求

> **当前状态（2026-10）**：单元测试代码已移除，本项目暂不要求编写单元测试。
> 构建脚本中的测试依赖（JUnit / MockK / Turbine / coroutines-test）仍保留，
> 将来恢复测试时直接在 `app/src/test/` 添加即可。

- 若重新引入单元测试，遵循：新增 Repository 实现、ViewModel 逻辑、数据模型转换逻辑应有对应测试
- 运行测试命令：`./gradlew :app:testDebugUnitTest`

---

## 五、Git 提交规范

### 5.1 Commit Message 格式

```
<type>(<scope>): <subject>

<body>
```

**type 类型：**

| type | 说明 |
|------|------|
| feat | 新功能 |
| fix | 修复 bug |
| docs | 文档变更 |
| style | 代码格式调整（不影响功能） |
| refactor | 重构（既不新增功能也不修 bug） |
| perf | 性能优化 |
| test | 测试相关 |
| build | 构建系统或依赖变更 |
| chore | 杂项 |

### 5.2 示例

```
feat(home): 添加逐时预报列表滚动支持

- 使用 TransformingLazyColumn 替代 Column
- 绑定旋转表圈 snap 行为
- 列表项添加稳定 key 优化重组性能
```

---

## 六、项目文档索引

| 文档 | 内容 | 何时阅读 |
|------|------|---------|
| [README.md](./README.md) | 项目概览、环境搭建、快速开始、里程碑 | 首次接触项目 |
| [DEVELOPMENT.md](./DEVELOPMENT.md) | 架构设计、数据流、模块详解、编码规范 | 开始开发前必读 |
| [AGENTS.md](./AGENTS.md) | AI 协作规则、文档更新要求、提交规范 | 每次提交前检查 |
