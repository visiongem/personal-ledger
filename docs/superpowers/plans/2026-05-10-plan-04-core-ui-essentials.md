# Plan 04 · core-ui Theme + Essentials

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** 给 `core-ui` 落地 v1 必需的 UI 基础——Theme + 几个高频组件（Button、TopBar、三态页面）。完成后 `feature-*` 模块就能开始拼界面。

**Architecture:**

- **Theme**：基于 Material 3 (`androidx.compose.material3`)。提供 `LedgerTheme { ... }` 顶层包装；`LedgerColors` / `LedgerTypography` 通过 `MaterialTheme.colorScheme` / `MaterialTheme.typography` 暴露给消费方
- **品牌色**：选蓝绿系（teal）作主色。本 Plan 选 **Teal 700 (#00897B)** 作 primary，可 v1 上架前微调
- **组件**：以 Material 3 标准组件为底，包一层 "Via" 前缀的命名 + 项目默认尺寸/间距 / loading 状态等约定。**不重写 Material 内部行为**
- **三态页面**：`ViaLoadingPage` / `ViaEmptyPage` / `ViaErrorPage` 各占满父容器，居中显示状态

**Tech Stack 增量:** Material 3 1.3.0（已接）；compose-material-icons-extended（已接，用于 TopBar 返回箭头等）。

**Spec 参考：** spec §1.1（中英双语）、§2.1（Compose-only）、§6.1（core-ui 内容清单）

---

## 范围决策（v1 不做）

- **ViaInput / ViaCodeInput**：留 Plan 05；输入组件需求在 record 录入页面落地时一并设计（避免空想 API）
- **ViaSwitch / ViaCheckbox**：直接用 Material 3 原生（够用），等需要项目化时再包
- **ViaBottomSelectorDialog / ViaBottomExplainDialog**：留 Plan 05；账户/分类选择器用上时
- **ViaPullToRefreshBox**：留 Plan 05；列表页落地时
- **暗色模式**：本 Plan 提供 `darkColorScheme`，但默认仍跟随系统；UI 调试可在 LedgerTheme(darkTheme = true) 强制

---

## File Structure

```
core-ui/src/main/kotlin/io/github/visiongem/ledger/core/ui/
├── theme/
│   ├── Color.kt           # T1 品牌色板 + light/dark ColorScheme
│   ├── Typography.kt      # T1 Material 3 typography 微调
│   └── Theme.kt           # T1 LedgerTheme @Composable 入口
├── component/
│   ├── ViaButton.kt       # T2 ViaFillButton + ViaOutlineButton
│   ├── ViaTopBar.kt       # T3 标题 + 可选返回 + actions slot
│   └── ViaStatePage.kt    # T4 ViaLoadingPage / ViaEmptyPage / ViaErrorPage

# 删除：
core-ui/.../Placeholder.kt  # T5
```

---

## 通用规则

1. **包名**：`io.github.visiongem.ledger.core.ui.{theme,component}`
2. **不引入业务**：Via* 组件签名只接受通用类型（String、() -> Unit、Modifier、Painter 等），不出现"账户"、"分类"等领域词
3. **测试**：core-ui 没有 testImpl；UI 行为靠 `@Preview` 在 IDE 中视觉验证 + assembleDebug 编译通过
4. **Modifier 优先**：每个 `@Composable` 签名第一个非必需参数是 `modifier: Modifier = Modifier`
5. **可访问性**：图标按钮必须有 `contentDescription`（即使只是装饰也用 `null` 显式标）

---

## Task 1：Theme 系统

**Files:**
- Create: `core-ui/.../theme/Color.kt`
- Create: `core-ui/.../theme/Typography.kt`
- Create: `core-ui/.../theme/Theme.kt`

要点：
- `Color.kt`：定义品牌主色 `LedgerTeal500 = Color(0xFF00897B)` 等品牌专属色；构造 `lightColorScheme(...)` 和 `darkColorScheme(...)` 各一份
- `Typography.kt`：基于 `Typography()` 默认值，按需 override 标题字号/字重；不引入自定义字体（v1 不打字体进 APK）
- `Theme.kt`：`@Composable fun LedgerTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false, content: @Composable () -> Unit)` —— `dynamicColor` 默认关（避免 Android 12+ 用户主题色覆盖品牌）；调用 `MaterialTheme(colorScheme, typography, content)` 包内容
- 加 1 个 `@Preview` Composable：`LedgerThemePreview` 显示一个 `Surface { Text }` 验证主题渲染

Commit: `feat(core-ui): add LedgerTheme with teal-based color scheme`

---

## Task 2：ViaButton 家族

**Files:**
- Create: `core-ui/.../component/ViaButton.kt`

要点：
- `ViaFillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false)`：
  - 包 Material 3 `Button`；高度 48dp 固定；圆角 12dp
  - `loading = true` 时禁用点击 + 内部显示 `CircularProgressIndicator`（小尺寸，颜色用 `onPrimary`）
- `ViaOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)`：
  - 包 Material 3 `OutlinedButton`；同样高度 48dp + 圆角 12dp
- `@Preview` 显示两按钮 normal / loading / disabled 三态

Commit: `feat(core-ui): add ViaFillButton and ViaOutlineButton`

---

## Task 3：ViaTopBar

**Files:**
- Create: `core-ui/.../component/ViaTopBar.kt`

要点：
- `ViaTopBar(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {})`：
  - 包 Material 3 `CenterAlignedTopAppBar`
  - `onBack != null` 时左侧显示返回箭头（`Icons.AutoMirrored.Filled.ArrowBack`，`contentDescription = "Back"`）
  - `actions` slot 暴露给调用方放右侧操作按钮
- `@Preview`：显示有返回 + 一个 settings action 的样例

Commit: `feat(core-ui): add ViaTopBar with optional back and actions slot`

---

## Task 4：三态页面

**Files:**
- Create: `core-ui/.../component/ViaStatePage.kt`

要点：
- `ViaLoadingPage(modifier: Modifier = Modifier)`：占满父容器，居中 `CircularProgressIndicator`
- `ViaEmptyPage(message: String, modifier: Modifier = Modifier, illustration: @Composable (() -> Unit)? = null)`：占满 + 居中文字；可选插画 slot
- `ViaErrorPage(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null)`：占满 + 居中文字；`onRetry != null` 时下方放一个 "Retry" `ViaOutlineButton`
- `@Preview`：三态各一个

Commit: `feat(core-ui): add ViaLoadingPage / ViaEmptyPage / ViaErrorPage`

---

## Task 5：最终验证 + 删除 Placeholder

- Delete: `core-ui/.../Placeholder.kt`
- Run: `./gradlew clean assembleDebug` (timeout 600000ms) —— 全工程编译通过
- Run: `./gradlew test` —— 已有单测仍 56/56 通过
- 检查 `core-ui` 已无 `Placeholder.kt`

Commit: `chore(core-ui): drop placeholder after Plan 04 essentials landed`

---

## Definition of Done

- [x] 3 个 theme 文件 + 3 个 component 文件就位
- [x] core-ui Placeholder.kt 删除
- [x] `./gradlew clean assembleDebug test` 全部通过
- [x] 5 个 commit

---

## 已知限制（留给 Plan 05+）

- ViaInput 系列：等 record 录入页面落地一起设计
- 列表组件（ViaPullToRefreshBox / ViaIndicator）：等 feature-record 列表页时
- BottomSheet 选择器：等账户/分类选择 UI 时
- 真实品牌色微调：上架前由用户决定
- 国际化（中英双语）：本 Plan 不接 strings.xml，组件不出现硬编码业务字符串；feature 模块自己接 `Context.string(R.string.xxx)`
