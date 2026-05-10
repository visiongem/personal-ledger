# Plan 08-10 · feature-record + feature-stats + feature-settings + 底部导航

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development.

**Goal:** 把剩余三个 feature 模块从 placeholder 落到可用，并在 :app 加上底部导航条让 4 个 feature 互通。完成后 v1 主体功能闭环：账户管理 / 流水录入 / 月度统计 / 设置。

---

## 范围切割（压缩到 v1 必需）

为防止无限延展，本 plan **明确收敛**：

- **feature-record**：列表 + 创建/编辑（含 INCOME / EXPENSE）。**转账（TRANSFER）作为特殊 record 留 Plan 11**，需要双账户 picker，自带工作量
- **feature-stats**：当月 income / expense / net 三个汇总数字 + 按 category 的 expense 分布列表。**图表组件留 Plan 12**（外部库或 Canvas 自绘 都是独立工作量）
- **feature-settings**：主题切换（system/light/dark） + 默认币种持久化。**CSV/ZIP 备份留 Plan 13**；语言切换暂跟系统
- **DataStore**：本 plan 接入 `core-data/local/prefs/UserPreferencesRepository`，feature-settings 通过它读写
- **底部导航**：4 个顶级 destination（Records / Accounts / Stats / Settings）；用单 backStack 简化版（切换 tab 直接换栈底）；多 backstack 留 Plan 14

---

## File Structure

```
core-data/src/main/kotlin/io/github/visiongem/ledger/core/data/
├── domain/UserPreferences.kt           # T1（含 ThemeMode enum）
└── local/prefs/
    ├── UserPreferencesRepository.kt    # T1 接口
    └── UserPreferencesRepositoryImpl.kt# T1 用 DataStore<Preferences>

core-data/src/main/kotlin/io/github/visiongem/ledger/core/data/di/
└── PreferencesModule.kt                # T1 提供 DataStore + Repository

feature-record/src/main/kotlin/io/github/visiongem/ledger/feature/record/
├── nav/RecordRoutes.kt                 # T2
├── list/RecordList{UiState,ViewModel,Screen}.kt   # T2
└── edit/RecordEdit{UiState,ViewModel,Screen}.kt   # T3

feature-stats/src/main/kotlin/io/github/visiongem/ledger/feature/stats/
├── nav/StatsRoutes.kt                  # T4
└── StatsHome{UiState,ViewModel,Screen}.kt   # T4

feature-settings/src/main/kotlin/io/github/visiongem/ledger/feature/settings/
├── nav/SettingsRoutes.kt               # T5
└── SettingsHome{UiState,ViewModel,Screen}.kt   # T5

app/src/main/kotlin/io/github/visiongem/ledger/
├── MainActivity.kt                     # T6 改写：底部导航 + 4 个 root destination
└── nav/HomeNavGraph.kt                 # T6 NavDisplay 配置 + bottom bar
```

---

## Task 1：UserPreferences + DataStore（核心数据层）

**Files:**
- Create: `core-data/.../domain/UserPreferences.kt` — `ThemeMode` enum (SYSTEM/LIGHT/DARK) + `data class UserPreferences(themeMode, defaultCurrency: String)`
- Create: `core-data/.../local/prefs/UserPreferencesRepository.kt` — interface with `flow: Flow<UserPreferences>`, suspend `setThemeMode(...)`, suspend `setDefaultCurrency(...)`
- Create: `core-data/.../local/prefs/UserPreferencesRepositoryImpl.kt` — wraps `DataStore<Preferences>` with stringPreferencesKey
- Create: `core-data/.../di/PreferencesModule.kt` — `@Provides @Singleton DataStore<Preferences>` via `preferencesDataStore("user_prefs")`
- Update: `core-data/.../di/RepositoryBindings.kt` — bind `UserPreferencesRepository`
- Test：JVM 测 impl 用 mockk DataStore 不太友好；改用 hilt-disabled 的最小 fake test 跳过；只确保编译通过

Commit: `feat(core-data): add UserPreferences DataStore-backed repository`

---

## Task 2：feature-record list

**Files:**
- Routes: `RecordListRoute(filterAccountId: Long? = null)`, `RecordEditRoute(recordId: Long?)`
- ViewModel：`@HiltViewModel`，注入 `RecordRepository` + `AccountRepository` + `CategoryRepository`；UiState 含 `records: List<Record>`, `accountsById: Map<Long, Account>`, `categoriesById: Map<Long, Category>`, `loading: Boolean`；按 occurredOn 倒序展示，附 account/category 名称
- Screen：`Scaffold + ViaTopBar("Records") + LazyColumn(records) + FAB add`；ListItem 显示金额（含币种）+ 分类名 + 日期
- 测试：≥3 个（loading flow / data composition / empty state）

Commit: `feat(feature-record): add RecordList ViewModel and Screen`

---

## Task 3：feature-record edit

**Files:**
- ViewModel：`loadIfNeeded(id)`, `onTypeChange(RecordType.INCOME or EXPENSE)`, `onAccountChange(Long)`, `onCategoryChange(Long?)`, `onAmountChange(String)`, `onDateChange(LocalDate)`, `onNoteChange(String)`, `save()`
- 校验：account 必选 / category 必选（非 transfer 时）/ amount > 0 / date 必填
- Screen：表单字段：金额输入 + 类型 toggle + 账户 dropdown + 分类 dropdown + 日期 picker（用 stock DatePicker，等 BottomSheet picker 后续优化） + 备注；底部 Save
- 测试：≥4 个（load / 校验失败 / 保存成功 / type 切换清 categoryId）

> 暂不支持 TRANSFER（留 Plan 11），所以不出现 `transferToAccountId` 字段

Commit: `feat(feature-record): add RecordEdit ViewModel and Screen for INCOME/EXPENSE`

---

## Task 4：feature-stats（最小汇总）

**Files:**
- Routes: `StatsHomeRoute`
- ViewModel：注入 `RecordRepository` + `CategoryRepository` + `UserPreferencesRepository`；UiState 含 `month: YearMonth`, `incomeTotal: BigDecimal`, `expenseTotal: BigDecimal`, `net: BigDecimal`, `byCategory: List<CategoryTotal>`, `currencyCode: String`
- 计算：取当前月范围，调用 `recordRepository.observeInRange(start, end)` 在 Kotlin 层聚合（按 type 分；EXPENSE 再按 categoryId 分）；币种暂用账户币种 mismatch 不处理（v1 简化：默认显示用户首选币种，跨币种汇总未来 Plan 接 ExchangeRate）
- Screen：`ViaTopBar("Statistics") + Column { 三大 summary 卡 + 分类 breakdown LazyColumn }`
- 测试：≥2 个（聚合数学 / 空数据回 ZERO）

> v1 不出图表；只有数字汇总。图表（线图/饼图）留 Plan 12 评估外部库

Commit: `feat(feature-stats): add monthly totals StatsHome screen`

---

## Task 5：feature-settings（主题 + 默认币种）

**Files:**
- Routes: `SettingsHomeRoute`
- ViewModel：注入 `UserPreferencesRepository`；暴露 `prefs: StateFlow<UserPreferences>`，`onThemeModeChange(ThemeMode)`, `onDefaultCurrencyChange(String)`
- Screen：`ViaTopBar("Settings") + Column`：
  - 主题：3-radio (System/Light/Dark)
  - 默认币种：OutlinedTextField，3 字母（与 AccountEdit 同模式）
  - "About" 区域：版本号（暂硬编码 "0.1.0"）
- 测试：≥2 个（state 流 / 字段更新）

Commit: `feat(feature-settings): add theme mode + default currency settings`

---

## Task 6：底部导航 + LedgerTheme 接 ThemeMode

**Files:**
- Create: `app/.../nav/HomeNavGraph.kt`（or 直接写在 MainActivity）：
  - 顶级 destination：`AccountListRoute / RecordListRoute / StatsHomeRoute / SettingsHomeRoute`
  - 切 tab 时 `backStack.clear(); backStack.add(newTopLevel)`
  - 子页面（AccountEdit / RecordEdit）压栈时仍正常 add
- Modify: `MainActivity.kt`：
  - `LedgerTheme(darkTheme = ...)` 根据 `userPreferencesRepository.flow.collectAsStateWithLifecycle().themeMode` 动态切换
  - Scaffold + bottomBar = 4 个 NavigationBarItem
  - bottomBar 仅在 backStack 顶为顶级 destination 时显示
- Modify: `app/build.gradle.kts` — 加 `:feature-record` `:feature-stats` `:feature-settings`（已有），`:core-data`（确保有），DataStore 类型支持（已经透传）

Commit: `feat(app): add bottom navigation with theme-aware LedgerTheme`

---

## Task 7：最终验证

- Run `./gradlew clean assembleDebug test`
- 检查 4 个 feature 都无 Placeholder.kt
- 检查 navigation 切换可用（手动观察 + 编译通过）

Commit: `chore: drop remaining feature placeholders` (汇总删除剩余 Placeholder.kt)

---

## Definition of Done

- [x] UserPreferences DataStore 完整接通
- [x] feature-record list + edit (INCOME/EXPENSE) 可用
- [x] feature-stats 月度汇总可用
- [x] feature-settings theme + currency 持久化可用
- [x] 底部导航 + 主题动态切换
- [x] 4 个 Placeholder.kt 删除
- [x] `./gradlew clean assembleDebug test` 通过
- [x] 7 个 commit

---

## 已知限制（明确留给后续 Plan）

- TRANSFER record 类型 → Plan 11
- 图表组件（折线/饼图） → Plan 12
- CSV / ZIP 备份导入导出 → Plan 13
- 多 backstack 底部导航（每 tab 独立栈） → Plan 14
- Account/Category picker BottomSheet → Plan 15
- DAO instrumentation tests → Plan 16
- 跨币种汇总（用 ExchangeRateRepository） → Plan 17
