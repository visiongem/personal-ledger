# 架构规范

## 单 Activity + Compose

- 项目只有一个 `MainActivity`，承载 4 个 tab 的 back stack。**禁止**新增 Activity 子类。
- 所有页面都是 Composable 函数：`{Page}Screen.kt` + `{Page}ViewModel.kt` + `{Page}UiState.kt`。
- `MainActivity` 在 `onCreate` 中：
  1. 先调用 `installSplashScreen()`（AndroidX 兼容启动屏）
  2. 再 `enableEdgeToEdge()`
  3. 然后 `super.onCreate(savedInstanceState)`
  顺序不可换。

## Navigation 3 路由

项目使用 `androidx.navigation3` alpha（basic + saveable backstack recipe）。

### 路由定义

每个 feature 模块在 `nav/` 子包定义自己的 Route 类，MUST 加 `@Parcelize` 并实现 `Parcelable`，否则 saveable back stack 还原会崩。

```kotlin
package io.github.visiongem.ledger.feature.record.nav

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data object RecordListRoute : Parcelable

@Parcelize
data class RecordEditRoute(val recordId: Long?) : Parcelable
```

模块的 `build.gradle.kts` MUST 加 `id("kotlin-parcelize")` 插件。

### 路由注册

在 `MainActivity.kt` 的 `entryProvider` 中按类型分发：

```kotlin
entryProvider = { key ->
    when (key) {
        is RecordListRoute -> NavEntry(key) {
            RecordListScreen(
                onRecordClick = { id -> recordsStack.add(RecordEditRoute(id)) },
                onAddClick = { recordsStack.add(RecordEditRoute(null)) },
            )
        }
        is RecordEditRoute -> NavEntry(key) {
            RecordEditScreen(
                recordId = key.recordId,
                onDone = { recordsStack.removeLastOrNull() },
            )
        }
        // ...
        else -> error("Unknown route: $key")
    }
}
```

- 每个 tab 一个 `rememberSaveable(saver = backStackSaver) { mutableStateListOf<Any>(...) }`，根 route 放在初始 list 中。
- Screen 通过构造参数接受导航回调（`onXxxClick`、`onBack`、`onDone`），**禁止**在 Screen 内部直接拿 NavController；保持 feature 模块对 Nav3 类型无依赖。

### Back 处理

- `Scaffold` 外的 NavDisplay 用 `onBack = { currentStack.removeLastOrNull() }`。
- 单个 Screen 想覆盖返回行为（如保存提示）须在 Composable 内 `BackHandler { ... }`，禁止 override Activity 的 onBackPressed。

## ViewModel 基类与初始化

- 所有 ViewModel MUST 继承 `BaseViewModel`（位于 `:core-base`），获得 `launchCatching { ... }` 异常处理脚手架。
- 用 `@HiltViewModel` + `@Inject constructor(...)` 构造。
- Composable 内取实例：`val vm: XxxViewModel = hiltViewModel()`。
- 需要本地化错误消息时，注入 `@ApplicationContext private val context: Context`，用 `context.getString(R.string.xxx)`。

```kotlin
@HiltViewModel
class RecordEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordRepository: RecordRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(RecordEditUiState())
    val state: StateFlow<RecordEditUiState> = _state.asStateFlow()

    fun save() {
        // ...
        launchCatching(onError = { /* 写到 state.errorMessage */ }) {
            _state.update { it.copy(saving = true, errorMessage = null) }
            recordRepository.upsert(record)
            _state.update { it.copy(saving = false, saved = true) }
        }
    }
}
```

## Repository 与数据流

- 每个核心实体一个 `XxxRepository` interface（在 `:core-data` 的 `repo/`）+ `XxxRepositoryImpl`；通过 Hilt `@Binds` 绑定。
- Flow 系列方法用 `observeXxx(...)` 命名；suspend 方法用动词 `upsert / deleteById / refreshLatest` 命名。
- Repository **禁止**直接持有 Retrofit 接口暴露给 ViewModel；网络调用封装在 Repository 内部。
- ViewModel **禁止**直接持有 DAO。

## Room 与 FK

五个 Entity / DAO：`Account`、`Category`、`Record`、`Budget`、`ExchangeRate`。

### 外键策略（记牢，AI 改 schema 前必须先确认）

| Child → Parent | onDelete | 影响 |
|----------------|----------|------|
| `record.accountId` → `account.id` | RESTRICT | 账户有流水时硬删抛异常；改走软归档（`archived = true`） |
| `record.transferToAccountId` → `account.id` | RESTRICT | 同上 |
| `record.categoryId` → `category.id` | SET NULL | 分类硬删后流水变"无分类"，可重建分类 |
| `budget.categoryId` → `category.id` | CASCADE | 分类删则该分类预算一起删 |

### TypeConverter

`BigDecimal`、`LocalDate`、`Instant`、`YearMonth`、各种 enum 通过 `LedgerTypeConverters`（在 `:core-data` 的 `local/converter/`）。

### 测试

- DAO 测试用 Robolectric + JUnit 4：`@RunWith(RobolectricTestRunner::class)` + `InMemoryDatabaseFactory`。
- ViewModel 测试用 JUnit 5 (Jupiter) + mockk + Turbine：模板见 `:feature-record/src/test/`。

## 备份格式

- 五份 CSV 打包为 ZIP：`accounts.csv` / `categories.csv` / `records.csv` / `budgets.csv` / `rates.csv`。
- CSV escape / split 走 `CsvFormat` 内部对象（RFC 4180）；每个 Entity 有自己的 `XxxCsv.kt` 格式化器。
- 备份 / 恢复 入口在 `RecordBackupRepository`：`exportZip()` / `importZip(bytes)`。

## WorkManager

- 周期任务用 `PeriodicWorkRequest` + `ExistingPeriodicWorkPolicy.KEEP`。
- Worker 继承 `CoroutineWorker` + `@HiltWorker`，注入 Repository 依赖。
- `LedgerApplication` 实现 `Configuration.Provider`，提供 `HiltWorkerFactory`。
- AndroidManifest MUST 通过 `tools:node="remove"` 关掉默认 `WorkManagerInitializer`，否则会双初始化崩。

## 长按删除 + Snackbar Undo 模式

所有列表（流水 / 账户 / 分类）遵循同一套模式：

1. ViewModel 暴露：
   ```kotlin
   private val _deletionEvents = MutableSharedFlow<Entity>(extraBufferCapacity = 1)
   val deletionEvents: SharedFlow<Entity> = _deletionEvents.asSharedFlow()
   ```
2. `onLongPress(entity)`：流水 / 分类用 hard delete；账户因 FK RESTRICT 走 `upsert(account.copy(archived = true))`。然后 `_deletionEvents.tryEmit(entity)`。
3. `undoDelete(entity)` / `undoArchive(entity)`：upsert 原 entity 恢复（分类用 `id = 0L` 让 Room 重新分配）。
4. Screen：`SnackbarHostState` + `LaunchedEffect(viewModel) { viewModel.deletionEvents.collectLatest { showSnackbar(undoLabel) ... } }`。

ListItem 用 `Modifier.combinedClickable(onClick = ..., onLongClick = ...)`，需要 `@OptIn(ExperimentalFoundationApi::class)`。

## 偏好存储

- `UserPreferencesRepository` 包装 DataStore Preferences，暴露 `Flow<UserPreferences>`。
- 不要直接在 ViewModel 操作 DataStore。
- 新增偏好字段：domain `UserPreferences` 加字段 + Impl 读写 + interface 暴露 setter。
