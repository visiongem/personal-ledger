# 组件使用规范

> 所有项目自研通用 Compose 组件以 `Via` 前缀命名，集中在 `:core-ui` 的 `component/` 包；新建前 MUST 先全局搜确认是否已有现成的。

## 顶栏 / 容器

| 组件 | 用途 | 路径 |
|------|------|------|
| `ViaTopBar` | CenterAligned 顶部导航栏，支持 `onBack` 回调和右侧 `actions` 槽 | `core-ui/.../component/ViaTopBar.kt` |
| `ViaLoadingPage` | 全屏加载指示器 | `core-ui/.../component/ViaLoadingPage.kt` |
| `ViaEmptyPage` | 空状态页（支持自定义文案） | `core-ui/.../component/ViaEmptyPage.kt` |

## 按钮

| 组件 | 用途 |
|------|------|
| `ViaFillButton` | 填充主按钮，支持 `loading` 状态 |
| `ViaOutlineButton` | 描边次按钮，支持 `enabled` |

```kotlin
ViaFillButton(
    text = stringResource(R.string.record_save),
    onClick = viewModel::save,
    modifier = Modifier.fillMaxWidth(),
    loading = state.saving,
)
```

## 选择器

| 组件 | 用途 | 备注 |
|------|------|------|
| `ViaSelectorField` | 看起来像 `OutlinedTextField` 的可点按字段，用于触发选择器 | 内部 `Surface { onClick }`，不可输入 |
| `ViaBottomSelector` | `ModalBottomSheet` 选择器，泛型支持任意 item 类型 | 配合 `ViaSelectorField` 使用 |

典型用法：
```kotlin
@Composable
private fun AccountPicker(
    label: String,
    accounts: List<Account>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
) {
    var sheetOpen by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    ViaSelectorField(
        label = label,
        valueLabel = selected?.let { "${it.name} (${it.currencyCode})" },
        onClick = { sheetOpen = true },
    )
    if (sheetOpen) {
        ViaBottomSelector(
            title = label,
            items = accounts,
            itemLabel = { "${it.name} (${it.currencyCode})" },
            onSelect = { onSelect(it.id) },
            onDismiss = { sheetOpen = false },
        )
    }
}
```

## 图表

| 组件 | 用途 |
|------|------|
| `ViaPieChart` | Compose Canvas 甜甜圈图 + 侧边图例 |
| `ViaHorizontalBarChart` | 横向柱状图（带数值文本） |
| `rememberPiePalette()` | 默认 6 色调色板（取自 `MaterialTheme.colorScheme`） |

## 日期 / 时间

- 选日期 MUST 用 Material 3 `DatePickerDialog` + `rememberDatePickerState`，并把 millis 用 `Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()` 转 `LocalDate` 后写回 VM。
- 选月份 MUST 用项目自己的"前/后月按钮"模式（参考 `feature-stats/.../StatsHomeScreen.kt` 的 `MonthSwitcher`），不要用第三方月份选择器。
- 显示日期 MUST 用 `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())`，禁止硬编码 `yyyy-MM-dd`。

## Snackbar

- Snackbar 用 Material 3 原生 `SnackbarHostState`：
  ```kotlin
  val snackbarHostState = remember { SnackbarHostState() }
  Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { ... }
  LaunchedEffect(viewModel) {
      viewModel.deletionEvents.collectLatest { deleted ->
          val result = snackbarHostState.showSnackbar(
              message = deletedMessage,
              actionLabel = undoLabel,
          )
          if (result == SnackbarResult.ActionPerformed) {
              viewModel.undoDelete(deleted)
          }
      }
  }
  ```
- 禁止直接 `Toast.makeText(...)`；项目没有 Toast 工具，所有反馈走 Snackbar。

## Dialog

- 简单确认 / 表单弹窗用 Material 3 `AlertDialog`。
- 选择器用 `ViaBottomSelector`。
- 禁止使用 `DialogFragment` / `AndroidDialog`。

## 列表

- 简单列表用 Compose `LazyColumn` + `items(key = { ... })`。
- 项目目前没用 Paging 3；若将来流水数据量上千需要加分页，新建 `Paging3` 封装时优先放 `:core-ui` 复用。

## 输入框

- 文本输入直接用 Material 3 `OutlinedTextField`。
- ISO 4217 货币代码等需要约束的字段，MUST 用本地 `var draft by remember { mutableStateOf(...) }` 缓冲，在达到完整条件后才回写 VM（避免每次 keystroke 写 DataStore 导致光标卡死）。
- 金额输入用 `KeyboardOptions(keyboardType = KeyboardType.Decimal)`，singleLine = true。

## Chip / Filter

- 横向可滚动的筛选条用 `Row + horizontalScroll(rememberScrollState())` + 多个 `FilterChip`，参考 `feature-record/.../RecordListScreen.kt` 的 `FilterRow`。
- 二选一 / 三选一互斥状态用 `SingleChoiceSegmentedButtonRow` + `SegmentedButton`。

## Insets

- 每个 feature 自己的 `Scaffold` 默认会消化 status bar inset。
- `MainActivity` 外层 Scaffold 设 `contentWindowInsets = WindowInsets(0, 0, 0, 0)`，避免内外双重 padding 让标题被挤下来。
- `enableEdgeToEdge()` 在 `MainActivity.onCreate` 中调用，且必须在 `super.onCreate` 之前。
