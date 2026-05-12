# 资源使用规范

## 字符串

### 文件位置

| 模块 | 默认（英文） | 简体中文 |
|------|--------------|----------|
| `:app` | `app/src/main/res/values/strings.xml` | `app/src/main/res/values-zh/strings.xml` |
| `:core-ui` | （无；core 模块不放业务字符串） | — |
| `:feature-*` | `<module>/src/main/res/values/strings.xml` | `<module>/src/main/res/values-zh/strings.xml` |

> 每个 feature 模块自己持有 R.string；business 字符串 MUST 放在该 feature 模块内，禁止跨模块引用 R.string。

### 新增字符串流程

1. **优先复用**：先在目标模块的 `strings.xml` 中查找是否已存在同语义键。
   - 存在：直接复用 `name`，不要重复新增。
   - 不存在：进入第 2 步。
2. **同步两份文件**：
   - 默认 `values/strings.xml`（英文）末尾新增一行
   - `values-zh/strings.xml`（简体中文）末尾新增一行
   - 两份文件的 `name` 必须完全一致
3. **占位符优于拼接**：动态内容用 `%1$s` / `%1$d` / `%2$s`，禁止字符串拼接。
4. **命名规则**：`{module}_{feature}_{purpose}`，例如：
   - `record_edit_title_add`
   - `account_archive_undo_action`
   - `settings_rates_last_updated_fmt`（带 `_fmt` 后缀表示包含格式化占位符）
5. **不本地化的文案**（如品牌名 `Personal Ledger`、ISO 货币代码、技术常量）：两份文件值保持一致即可，不需要加 `translatable="false"`。

### 在 ViewModel 中使用

ViewModel 内的错误消息需要本地化时，注入 `@ApplicationContext private val context: Context`，用 `context.getString(R.string.xxx)`：

```kotlin
@HiltViewModel
class RecordEditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    ...
) : BaseViewModel() {
    fun save() {
        if (s.accountId == null) {
            _state.update {
                it.copy(errorMessage = context.getString(R.string.record_err_pick_account))
            }
        }
    }
}
```

### 在 Composable 中使用

```kotlin
Text(stringResource(R.string.record_save))
Text(stringResource(R.string.settings_backup_imported_fmt, count))
```

## 颜色

### 主题源

颜色 token 集中在 `:core-ui` 的 `theme/`：
- `Color.kt` — Compose Color 定义
- `Theme.kt` — `colorScheme` light / dark 配置

### 使用规则

- Compose 中颜色 MUST 通过 `MaterialTheme.colorScheme.xxx` 引用。
- **严禁**硬编码 `Color(0xFF...)`，**例外**：
  - `:app` 的 `res/values/colors.xml` 定义启动屏 / 图标的固定品牌色（`splash_background`、`ic_launcher_background`）
  - 矢量 `drawable/ic_launcher_foreground.xml` 中的内嵌颜色（图标本身就是颜色资源）
- 透明度用 `.copy(alpha = 0.1f)`，不要预定义 `xxxAlpha10` 之类的额外 token。

## 矢量图标

- `Icons.Default.*`、`Icons.AutoMirrored.Filled.*` 优先用 Material Icons（已通过 `androidx.compose.material:material-icons-core` 引入）。
- 需要自定义图标时放在 `:core-ui` 的 `res/drawable/`，命名 `ic_xxx.xml`。
- 启动器图标 / Splash 图标在 `:app` 模块（`mipmap-anydpi-v26/ic_launcher.xml` + `drawable/ic_launcher_foreground.xml`）。

## 字体

- 项目目前未引入自定义字体；全部使用系统默认。
- 若将来需要 DIN 等金额字体，放在 `:core-ui/src/main/res/font/` 并通过 Compose `FontFamily` 暴露。

## 主题（亮 / 深）

- 主题切换通过 `UserPreferencesRepository` 持久化 `ThemeMode`（SYSTEM / LIGHT / DARK）。
- `MainActivity` 顶层从 `MainViewModel.themeMode` 收集，传给 `LedgerTheme(darkTheme = ...)`。
- 单个 Composable **禁止**判断 `isSystemInDarkTheme()` 后切换颜色；统一由 `MaterialTheme.colorScheme` 处理。

## Drawable / Shape

- Compose 不用 XML Shape 资源，圆角直接 `Modifier.clip(RoundedCornerShape(...))`。
- 仅 Splash / Launcher / Splash 背景等需要 vector drawable XML 时才在 `res/drawable/` 写 XML。

## 当前支持语言

- `values/` — 英语（默认）
- `values-zh/` — 简体中文

**禁止** AI 在没有用户授权的情况下添加新的 `values-xx-rXX/` 语言文件。如果用户要求加繁中 / 日 / 韩 / 俄 / 西，再走完整翻译流程。
