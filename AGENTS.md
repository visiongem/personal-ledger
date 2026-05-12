# 项目 AI 编程指南

本文档是 Agent 的主要入口：**关键框架知识、项目约定、架构决策**直接写在此处；细则见 `.claude/rules/` 下各规则文件。

---

## 一、仓库目录结构

```
personal-ledger/
├── app/                              # 业务壳：MainActivity、Routes、ScaffoldShell
│   └── src/main/kotlin/io/github/visiongem/ledger/
│       ├── MainActivity.kt           # 入口、4 tab back stack、Nav3 entryProvider
│       ├── LedgerApplication.kt      # @HiltAndroidApp + WorkManager Configuration.Provider
│       └── work/                     # WorkManager Worker（每日汇率刷新）
├── core-base/                        # BaseViewModel、launchCatching、通用基类
├── core-utils/                       # CurrencyFormatter 等纯 Kotlin 工具
├── core-extensions/                  # Kotlin / Compose 扩展函数
├── core-network/                     # Retrofit + Moshi + ApiResponse 封装
├── core-ui/                          # Compose 组件库（Via*）+ Theme
├── core-data/                        # Room + Repository + Domain Model + 汇率 + 备份
├── feature-record/                   # 流水列表 / 录入（含 Material 3 DatePicker）
├── feature-account/                  # 账户管理（支持归档撤销）
├── feature-stats/                    # 月度统计图表 + 月份切换
├── feature-settings/                 # 主题、默认币种、汇率刷新、备份、分类、预算
├── build-logic/                      # Gradle convention plugins
├── gradle/libs.versions.toml         # Version Catalog（依赖与版本统一管理）
└── .claude/rules/                    # AI 编码细则（架构、风格、资源等）
```

### 模块职责

| 模块 | 职责 | 兼容性要求 |
|------|------|-----------|
| `:app` | 业务壳：Hilt 入口、Activity、顶层导航 | — |
| `:core-base` | `BaseViewModel`、`launchCatching` 错误处理脚手架 | 被所有 feature 依赖，修改需谨慎 |
| `:core-utils` | 纯 Kotlin 工具（不依赖 Android），如 `CurrencyFormatter` | 被多模块依赖，修改需谨慎 |
| `:core-extensions` | Kotlin / Compose 扩展函数 | 被多模块依赖，修改需谨慎 |
| `:core-network` | `ApiResponseConverterFactory`、OkHttp 拦截器 | 被 `:core-data` 依赖 |
| `:core-ui` | Compose 组件 + Theme | 被所有 feature 依赖 |
| `:core-data` | Room、Repository、Domain Model、汇率拉取、备份 | 被所有 feature 依赖；DAO/Entity 改动需同步 mapper、CSV、迁移 |
| `:feature-*` | 业务功能模块，互不直接依赖（通过路由跳转） | — |
| `:build-logic` | Convention plugins（`ledger.android.application` 等） | 不打包到 APK |

> **跨模块兼容性**：`:core-*` 模块的公共类 / 方法签名修改时，须确认上游 feature 模块同步适配；优先在调用方扩展、避免改动公共接口。

---

## 二、技术栈与框架（必读）

| 类别 | 选型 |
|------|------|
| 语言 | Kotlin 2.0（JVM 17） |
| UI | Jetpack Compose + Material 3（新需求一律 Compose） |
| 导航 | Navigation 3（`androidx.navigation3` alpha02，basic + saveable backstack recipe） |
| 架构 | MVVM（ViewModel + StateFlow + SharedFlow） |
| 依赖注入 | Hilt（`@HiltAndroidApp`、`@HiltViewModel`、`@Binds`、`@Provides`） |
| 网络 | Retrofit2 + Moshi + OkHttp + `ApiResponseConverterFactory` |
| 异步 | Kotlin Coroutines + Flow |
| 序列化 | Moshi（@JsonClass(generateAdapter = true) by KSP） |
| 存储 | Room（KSP）+ DataStore Preferences |
| 后台任务 | WorkManager + `androidx.hilt:hilt-work` + `Configuration.Provider` |
| 构建 | Gradle Kotlin DSL + Version Catalog（`libs.versions.toml`） + build-logic 约定插件 |
| 测试 | JUnit 5 (Jupiter) + Robolectric（JUnit 4 vintage）+ mockk + Turbine + Truth |

---

## 三、架构决策与约定（必须遵守）

### 3.1 页面与基类

- **MainActivity**：唯一 Activity，承载 4 个 tab 的 back stack。**禁止**新增 Activity，所有页面用 Compose Composable 实现。
- **Composable 页面**：每个页面 = 一个 `XxxScreen.kt` + `XxxViewModel.kt` + `XxxUiState.kt`，遵循无状态 Composable + 状态提升模式。
- **ViewModel**：所有 ViewModel MUST 继承 `BaseViewModel`（在 `:core-base`），通过 `@HiltViewModel` + `@Inject constructor` 注入依赖；获取实例用 `hiltViewModel()`。
- **路由参数**：所有 Route 类 MUST `@Parcelize` + 实现 `Parcelable`，定义在对应 feature 模块的 `nav/` 子包；数据类参数走构造参数（如 `data class RecordEditRoute(val recordId: Long?) : Parcelable`）。

### 3.2 网络请求

- 所有 Retrofit 接口 MUST 定义在 `:core-data` 的 `remote/` 包，按业务来源拆分文件（如 `FrankfurterApi.kt`）。
- 业务接口走 `ApiResponseConverterFactory`，第三方接口（如 Frankfurter）用独立的"raw" Retrofit 实例，**禁止**让它们共享同一个 `Retrofit` Builder。
- ViewModel 内调用 Repository，Repository 内调用 API；ViewModel **禁止**直接持有 Retrofit 接口。
- 错误处理：Repository 方法返回 `Result<T>` 或抛业务异常；ViewModel 用 `launchCatching { ... }` 接住异常并写入 `errorMessage` state。

### 3.3 数据层（Room）

- 五个核心实体：`Account`、`Category`、`Record`、`Budget`、`ExchangeRate`，对应五个 Entity / DAO / Repository。
- **DAO 查询规范**：Flow 系列方法用 `observeXxx` 命名；`suspend fun getAll()` 用于备份导出。
- **外键级联**：
  - `record.accountId` → `account.id` ：`ON DELETE RESTRICT`（账户有流水时禁止硬删，必须软归档）
  - `record.transferToAccountId` → `account.id`：`ON DELETE RESTRICT`
  - `record.categoryId` → `category.id`：`ON DELETE SET NULL`（分类删除后流水变"无分类"）
- **TypeConverter**：`BigDecimal`、`LocalDate`、`Instant`、`YearMonth`、各种 enum 通过 `LedgerTypeConverters`。
- **新增 Entity / 字段**：必须同步更新 mapper、CSV 备份格式、Robolectric DAO 测试；若涉及 schema 变化须写迁移（Room 不允许 fallback to destructive 在 release）。

### 3.4 长按删除 + Snackbar Undo 模式

流水、账户、分类列表都遵循同一套模式：
1. ViewModel 暴露 `deletionEvents: SharedFlow<Entity>`（extraBufferCapacity = 1）。
2. `onLongPress(entity)`：实际删除（流水/分类用 hard delete；账户因 FK RESTRICT 用 `archived=true` 软删），`tryEmit(entity)`。
3. `undoDelete(entity)` / `undoArchive(entity)`：upsert 原 entity 恢复。
4. Screen 用 `SnackbarHostState` + `LaunchedEffect { collectLatest { showSnackbar(undoAction) } }` 接驳。

### 3.5 协程与线程

- 网络 / 文件 IO MUST 显式指定 `Dispatchers.IO`（除非已经在 Repository / DAO 内）。
- `viewModelScope.launch` 用于 UI 触发的请求；跨 VM / 跨页面长任务用 WorkManager。
- 收集 Flow MUST 用 `collectAsStateWithLifecycle()`，**禁止**裸 `collectAsState()`。

### 3.6 后台任务

- WorkManager 配置走自定义 `Configuration.Provider`（`LedgerApplication`），用 `HiltWorkerFactory` 注入 Worker 依赖。
- AndroidManifest MUST 通过 `tools:node="remove"` 关掉 androidx-startup 的 `WorkManagerInitializer`，避免双初始化。
- 周期任务用 `PeriodicWorkRequest` + `ExistingPeriodicWorkPolicy.KEEP`。

---

## 四、资源与规范（摘要）

### 字符串

- 所有 UI 字符串 MUST 入 `res/values/strings.xml`（英文）+ `res/values-zh/strings.xml`（简体中文），**两套文件同时同步**。
- 新增字符串：先全文搜确认无同义键；新增放在文件**最底部**；不要按字母排序破坏既有顺序。
- 动态部分用 `%1$s`、`%1$d` 等占位符，**禁止**字符串拼接。
- 仅本地化生效的文案才进 `strings.xml`；不需要本地化的（如品牌名 "Personal Ledger"）直接硬编码到 string resource 但两份文件值一致。

### 颜色与主题

- Compose 颜色 MUST 用 `MaterialTheme.colorScheme.xxx`；**严禁** `Color(0xFF...)` 字面值（启动图等矢量资源除外）。
- 主题源在 `:core-ui` 的 `theme/Theme.kt`、`Color.kt`；新增颜色 token 须加到这两份。

### 金额 / 数字格式化

- 显示金额 MUST 走 `io.github.visiongem.ledger.core.utils.CurrencyFormatter.format(amount, currencyCode)`，**禁止**用 `BigDecimal.toPlainString()` 直接渲染（会丢千分位、币种小数位约定）。
- 内部业务计算 MUST 用 `BigDecimal`；**绝对禁止** `Double / Float` 做金额运算。

详见 `.claude/rules/resources.md` 和 `.claude/rules/utils.md`。

---

## 五、代码风格（摘要）

- **Kotlin**：4 空格、行宽 120、K&R 大括号；禁止通配符 import、禁止代码里写完整包名。
- **命名**：类 PascalCase，变量/函数 camelCase，Composable 函数 PascalCase，常量 UPPER_SNAKE_CASE，资源文件 snake_case，路由类带 `Route` 后缀。
- **注释**：默认不写注释。仅当"为什么这么做"非显而易见（隐藏约束、补丁、反直觉行为）时才加一行 `// 为什么` 注释；禁止解释"做了什么"或引用 issue / PR 号。

详见 `.claude/rules/code_style-kotlin.md`。

---

## 六、Compose 约定

- **无状态化**：Composable 尽量保持无状态，状态提升到 ViewModel；UI state 用 `data class` + `StateFlow` 暴露。
- **样式统一**：使用 `MaterialTheme.colorScheme` / `typography`；**严禁**硬编码颜色或字号。
- **组件复用**：所有项目通用组件以 `Via` 前缀命名，集中在 `:core-ui`（`ViaTopBar`、`ViaFillButton`、`ViaOutlineButton`、`ViaBottomSelector`、`ViaSelectorField`、`ViaPieChart`、`ViaHorizontalBarChart`、`ViaEmptyPage`、`ViaLoadingPage`）；新建前 MUST 先全局搜确认是否已有现成的。
- **生命周期**：`collectAsStateWithLifecycle()`、`LaunchedEffect(key)` 收集事件流。
- **Insets**：每个 feature 自己的 `Scaffold` 消化 status bar inset；外层 `MainActivity` 的 Scaffold 设置 `contentWindowInsets = WindowInsets(0, 0, 0, 0)` 避免双 padding。
- **Preview**：建议每个非平凡 Composable 提供一个 `LedgerTheme {} ` 包裹的 `@Preview`，未做硬性要求。

详见 `.claude/rules/widgets.md`。

---

## 七、构建与测试

- **JDK**：17
- **Android**：`compileSdk 36`、`minSdk 26`、`targetSdk 36`
- 所有命令在仓库根执行 `./gradlew xxx`。
- 依赖与版本统一在 `gradle/libs.versions.toml`；新增库前先查是否已有 alias。
- 应用约定插件由 `:build-logic` 提供，名字一律 `ledger.android.{application|library|hilt|compose}`。

### 常用命令

| 用途 | 命令 |
|------|------|
| 清理 | `./gradlew clean` |
| 装 debug 到设备 | `./gradlew :app:installDebug` |
| 构建 debug APK | `./gradlew :app:assembleDebug` |
| 构建 release APK（已签名） | `./gradlew :app:assembleRelease` |
| 全部 JVM 单测 | `./gradlew test` |
| 单模块测试 | `./gradlew :core-data:testDebugUnitTest` |
| 单个测试类 | `./gradlew :core-utils:test --tests "io.github.visiongem.ledger.core.utils.CurrencyFormatterTest"` |
| 仪器化测试（含 Hilt smoke） | `./gradlew connectedAndroidTest` |
| Lint | `./gradlew lint` |

### 测试目录

- JVM 单测：`<模块>/src/test/kotlin/`（JUnit 5 Jupiter）
- Robolectric DAO 测试：仍在 `src/test/`，但用 JUnit 4 `@RunWith(RobolectricTestRunner::class)`（项目装了 JUnit Vintage Engine 同时跑两种）
- 仪器化测试：`<模块>/src/androidTest/kotlin/`（JUnit 4 + HiltAndroidRule）

### Release 签名

- Keystore **不入库**：放在 `~/keystores/personal-ledger-release.jks`。
- 凭据 **不入库**：从 `~/.gradle/gradle.properties` 读 `LEDGER_RELEASE_STORE_FILE` / `LEDGER_RELEASE_STORE_PASSWORD` / `LEDGER_RELEASE_KEY_ALIAS` / `LEDGER_RELEASE_KEY_PASSWORD`。
- `app/build.gradle.kts` 的 `signingConfigs` 块自动读取这些属性；缺失时 release 构建跳过签名而不是失败。

### 提交前检查

提交 commit / PR 前，**必须**依次完成：

1. **编译**：`./gradlew :app:assembleDebug` — 通过
2. **单测**：`./gradlew test` — 全部绿
3. **格式**：人工核对第五章；没集成 ktlint / spotless

---

## 八、Commit 与 PR 规范

### Commit Message

格式：`type(scope): 描述`，参考 `.claude/commands/commit.md`（或直接用 `/commit` 自动生成）。

- `type` 可选：`feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`build`、`ci`、`chore`、`revert`
- `scope` 用模块短名：`record`、`account`、`stats`、`settings`、`data`、`ui`、`app`、`brand`、`rates` 等
- 描述用英文，第一句不超过 72 字符
- body 可选，回答 **what / why**；超过 1 行用空行分段
- AI 协作的 commit 在 footer 加 `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`

### PR

- **标题**：跟 commit 同款 `type(scope): 描述`，单 PR 只表达一件事
- **描述**回答：
  - 改了什么？
  - 为什么改？
  - 有无破坏性变更？
- **测试**：列出验证步骤；UI 变更附截图

---

## 九、国际化

- 支持语言：
  - `values/` — 英语（默认，所有 UI 默认值）
  - `values-zh/` — 简体中文（**手动同步**）
- **新增字符串流程**：
  1. 先在两份 `strings.xml` 末尾各加一行
  2. ViewModel 内若需要本地化错误消息，注入 `@ApplicationContext private val context: Context`，用 `context.getString(R.string.xxx)`
- 当前没有繁体中文 / 日 / 韩 / 法 / 西。如果将来加，按 `values-zh-rTW/`、`values-ja/` 等放，**禁止** AI 在没有用户授权的情况下自行添加新语种文件。

---

## 十、避雷指南 (Common Pitfalls)

1. **金额禁用浮点**：`Double / Float` 做金额计算会丢精度；MUST 用 `BigDecimal`，渲染走 `CurrencyFormatter`。
2. **FK RESTRICT**：删账户前必须确认无流水，否则抛 SQLite 约束异常；默认走 `archived=true` 软删除，列表查询用 `observeActive()` 过滤。
3. **路由类必须 @Parcelize**：缺了会导致 saveable back stack 还原崩溃。
4. **Scaffold contentWindowInsets 双重消化**：外层 + 内层 Scaffold 都消化 status bar 时标题被挤下来；外层设 `WindowInsets(0,0,0,0)`。
5. **WorkManager 重复初始化**：自定义 `Configuration.Provider` 时 AndroidManifest 必须 `tools:node="remove"` 掉默认 initializer，否则启动崩。
6. **OutlinedTextField 双向绑定卡顿**：每次 keystroke 写回 DataStore 会让光标卡死；用本地 `var draft by remember { mutableStateOf(...) }`，达到完整条件（如 ISO 4217 3 字母）再 `onChange`。
7. **JVM-only 库放错模块**：纯 Kotlin 工具（如 `java.text.DecimalFormat`）入 `:core-utils`，禁止依赖 Android SDK 类型。
8. **测试 Locale 不稳定**：货币 / 日期格式化测试 MUST 显式传 `Locale.US`（或固定 locale），否则 CI 与本地差异爆炸。

---

## 十一、规则文件索引

| 主题 | 规则路径 |
|------|----------|
| 架构规范 | `.claude/rules/architecture.md` |
| 资源规范（字符串 / 颜色 / 字体） | `.claude/rules/resources.md` |
| Kotlin 风格 | `.claude/rules/code_style-kotlin.md` |
| 组件使用规范（`Via*` 组件 / Snackbar / Dialog） | `.claude/rules/widgets.md` |
| 工具类总览（CurrencyFormatter / BigDecimal） | `.claude/rules/utils.md` |

| 命令 | 路径 |
|------|------|
| `/commit` | `.claude/commands/commit.md` |
| `/code-review` | `.claude/commands/code-review.md` |
| `/explain` | `.claude/commands/explain.md` |
