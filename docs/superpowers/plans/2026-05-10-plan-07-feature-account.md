# Plan 07 · feature-account（账户管理）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development.

**Goal:** 把 `feature-account` 模块从 placeholder 落到可用——账户列表 + 创建/编辑账户。同时给 `:app` 接入 Navigation 3，让账户页面成为 v1 的第一个真实业务画面。

**Architecture:**

- **Nav3 basic recipe**：路由用 `data class` / `data object`，`mutableStateListOf<Any>` 当 backStack；`:app` 的 MainActivity 用 `NavDisplay` 直接在 `entryProvider` 里 `when`-分发各路由（暂不引入 modular Hilt 方案——v1 只有一个 feature 时 overkill；Plan 08 加 feature-record 时再升级）
- **feature-account 暴露**：路由数据类（`AccountListRoute` / `AccountEditRoute(id: Long?)`）+ 顶层 `@Composable` 屏幕函数，参数接 navigation 回调。不依赖 Navigator class
- **ViewModel**：每个屏幕一个 ViewModel，继承 `BaseViewModel`；用单一 `UiState` data class + `StateFlow<UiState>` 暴露状态；写操作走 `launchCatching` → `errorFlow`
- **`:app`**：MainActivity 用 `LedgerTheme` 包裹 `NavDisplay`；起始路由是 `AccountListRoute`

**Tech Stack:** Hilt + hilt-navigation-compose + navigation3 runtime/ui（已接）；BaseViewModel + AccountRepository（Plan 03/06）；ViaTopBar / ViaFillButton / ViaOutlineButton / 三态页面（Plan 04）

**Spec 参考：** spec §1.1（账户范围）、§3.2（数据层）、§7（Nav3）

---

## 范围决策

- **不做归档/删除/排序**：留 Plan 08 一起处理；本 Plan 只 create / edit
- **不做币种选择 BottomSheet**：暂用普通文本输入框接收 ISO 4217 字母（"USD"），交由用户输入。Plan 09 再做 picker
- **不做账户余额聚合显示**：列表只展示 name + currencyCode + openingBalance，"当前余额"留 Plan 10（接 RecordRepository 算）
- **不做 modular Hilt navigation**：等 feature-record 落位时再升级到 `EntryProviderInstaller` `@IntoSet` 模式

---

## File Structure

```
feature-account/src/main/kotlin/io/github/visiongem/ledger/feature/account/
├── nav/AccountRoutes.kt              # T3 data class/object 定义
├── list/
│   ├── AccountListViewModel.kt       # T1
│   ├── AccountListUiState.kt         # T1
│   └── AccountListScreen.kt          # T3
└── edit/
    ├── AccountEditViewModel.kt       # T2
    ├── AccountEditUiState.kt         # T2
    └── AccountEditScreen.kt          # T3

feature-account/src/test/kotlin/io/github/visiongem/ledger/feature/account/
├── list/AccountListViewModelTest.kt  # T1
└── edit/AccountEditViewModelTest.kt  # T2

app/src/main/kotlin/io/github/visiongem/ledger/
└── MainActivity.kt                   # T4 改写 setContent 用 NavDisplay

# 删除：
feature-account/.../Placeholder.kt    # T3
```

---

## 通用规则

1. **包名**：`io.github.visiongem.ledger.feature.account.{nav,list,edit}`
2. **不依赖 Navigator class**：屏幕接 navigation 回调（`(AccountEditRoute) -> Unit`）
3. **TDD ViewModel**：mockk Repository，验证 StateFlow 转换 + 业务校验
4. **UI tests skip**：Compose UI tests 需要 instrumentation；本 Plan 跳过，靠 Preview 视觉确认

---

## Task 1：AccountListViewModel + tests

**Files:**
- Create: `list/AccountListUiState.kt` — `data class AccountListUiState(val accounts: List<Account>, val loading: Boolean = true)`
- Create: `list/AccountListViewModel.kt` — `@HiltViewModel`，注入 `AccountRepository`；`state: StateFlow<AccountListUiState>` 由 `repo.observeActive()` `.map` + `.stateIn(... WhileSubscribed)` 构造；初始 `loading = true`，第一次 emit 后 `loading = false`
- Create: `AccountListViewModelTest.kt` — mockk repo 返回 flow，Turbine 验证 state 序列

要点：
- 使用 `viewModelScope` + `WhileSubscribed(5_000)` 让 Flow 在 UI 不再订阅 5s 后停
- 不实现"删除"、"归档"动作；纯只读
- 测试 ≥3 个：初始 loading true / 收到列表后 loading false 且数据正确 / 空列表也合法

Commit: `feat(feature-account): add AccountListViewModel observing active accounts`

---

## Task 2：AccountEditViewModel + tests

**Files:**
- Create: `edit/AccountEditUiState.kt` — 字段：`id`, `name`, `currencyCode`, `openingBalance` (String, 用户输入), `saving: Boolean`, `saved: Boolean`, `errorMessage: String?`
- Create: `edit/AccountEditViewModel.kt`：
  - `@HiltViewModel`，注入 `AccountRepository`
  - 暴露 `state: StateFlow<AccountEditUiState>`
  - `init {}` 不做事；屏幕通过 `loadIfNeeded(id: Long?)` 触发载入（id=null 表示创建）
  - 字段更新方法：`onNameChange`, `onCurrencyChange`, `onOpeningBalanceChange`
  - `save()`：校验 name 非空 + currencyCode 长度 == 3 + balance 可解析为 BigDecimal；通过则调 `repo.upsert(...)` 并把 `saved = true`；任一校验失败设 `errorMessage`
- Test：mockk repo，验证 load 流转 / 字段更新 / save 成功 vs 校验失败 / 错误后再编辑清掉 error

要点：
- 用 `MutableStateFlow.update {}` 改状态
- `save` 走 `launchCatching`；repo 抛异常时 `errorFlow` 接住，但 UI 也会通过 `state.saving = false` 回弹
- 不依赖 Instant.now() 实例化时机——save 时构造 Account 用 `Instant.now()` 即可（生产用），测试时用 mockk 时钟或 just-construct-and-verify 字段

Commit: `feat(feature-account): add AccountEditViewModel for create/edit flow`

---

## Task 3：Screens + Routes + delete Placeholder

**Files:**
- Create: `nav/AccountRoutes.kt`：
  ```kotlin
  data object AccountListRoute
  data class AccountEditRoute(val accountId: Long?)
  ```
- Create: `list/AccountListScreen.kt` — `@Composable fun AccountListScreen(onAccountClick: (Long) -> Unit, onAddClick: () -> Unit, viewModel: AccountListViewModel = hiltViewModel())`：
  - `Scaffold` + `ViaTopBar(title = "Accounts")`
  - 内容根据 state.loading / accounts 选 `ViaLoadingPage` / `ViaEmptyPage("No accounts. Tap + to add.")` / `LazyColumn { items(accounts) { ListItem(...) onClick = onAccountClick(it.id) } }`
  - `FloatingActionButton(onClick = onAddClick) { Icon(Add) }`
- Create: `edit/AccountEditScreen.kt` — `@Composable fun AccountEditScreen(accountId: Long?, onDone: () -> Unit, viewModel: AccountEditViewModel = hiltViewModel())`：
  - `LaunchedEffect(accountId) { viewModel.loadIfNeeded(accountId) }`
  - `LaunchedEffect(state.saved) { if (state.saved) onDone() }`
  - `Scaffold` + `ViaTopBar(title = if (accountId == null) "Add account" else "Edit account", onBack = onDone)`
  - Form：3 个 OutlinedTextField + 错误信息（state.errorMessage）+ 底部 ViaFillButton("Save", onClick = viewModel::save, loading = state.saving)
- Delete `Placeholder.kt`

> 不写 UI tests（按规则）；每屏幕加 1 个 `@Preview` 跑空状态。

Commit: `feat(feature-account): add list and edit screens with routes`

---

## Task 4：:app MainActivity 接入 NavDisplay + 最终验证

**Files:**
- Modify: `app/src/main/kotlin/io/github/visiongem/ledger/MainActivity.kt`：
  - `setContent { LedgerTheme { val backStack = remember { mutableStateListOf<Any>(AccountListRoute) }; NavDisplay(...) } }`
  - `entryProvider` `when`：`AccountListRoute` → `AccountListScreen(onAccountClick = { id -> backStack.add(AccountEditRoute(id)) }, onAddClick = { backStack.add(AccountEditRoute(null)) })`
  - `AccountEditRoute` → `AccountEditScreen(accountId = key.accountId, onDone = { backStack.removeLastOrNull() })`
- Modify: `app/build.gradle.kts` — 加 `implementation(project(":feature-account"))` 等依赖（如果还没加）
- Run `./gradlew clean assembleDebug test`

要点：
- LedgerTheme 包整个 NavDisplay
- :app 无需声明 Activity 路由——startDestination 是 AccountListRoute 即可
- HiltAndroidApp 已经在 `:app` 配置好；不动它

Commit: `feat(app): wire NavDisplay with feature-account as start destination`

---

## Definition of Done

- [x] 2 ViewModel + 2 UiState + 2 Screen + 1 routes file
- [x] :app MainActivity 用 NavDisplay 渲染 feature-account
- [x] feature-account Placeholder.kt 已删除
- [x] ViewModel JVM 测试 ≥6 个全部通过
- [x] `./gradlew clean assembleDebug test` 全部通过
- [x] 4 个 commit

---

## 已知限制（留给后续 Plan）

- 账户归档/删除/排序：Plan 08 一起补
- 币种选择 BottomSheet：Plan 09（跟分类选择一起做 ViaBottomSelectorDialog）
- 当前余额计算：Plan 10（依赖 RecordRepository）
- modular Hilt navigation：Plan 08 加 feature-record 时升级
- 边到边沉浸 + 系统栏配色：暂跟 Material 默认；Plan 11 接 Compose edge-to-edge
