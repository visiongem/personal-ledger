# Personal Ledger · 设计文档

**日期**：2026-05-09
**作者**：Nia
**状态**：Draft（设计已对齐，待落地）

## 1. 项目概览

个人记账 Android App，目的双重：

1. **真实可用的工具** —— 解决自己日常多账户、多币种记账需求
2. **starter kit 的展示与压力测试** —— 把自己沉淀的"网络/协程框架、数学/格式化工具、Compose UI Kit、基础设施层"放到一个完整业务里跑起来，作为通用代码库的活案例发布到 GitHub + Google Play

### 1.1 范围（实用档 B）

| 类别 | 包含 | 不包含 |
|---|---|---|
| 数据模型 | 账户 + 分类（自定义）+ 转账 + 多币种 + 月度预算 | 标签、多账本、负债管理 |
| 录入 | FAB + 底部抽屉，带计算器键盘 | 通知栏快捷、Widget |
| 多币种 | Frankfurter API + 账户币种锁定 + 汇率快照 | 加密货币、自定义汇率 |
| 统计 | 月趋势折线、分类饼图、账户分布 | 多维交叉、自定义看板 |
| 备份 | CSV 导入导出 + ZIP 全量备份到 Storage Access Framework | 云同步（Google Drive/Firebase 列入 v2）|
| 平台 | Android（min SDK 26 / target 36），中英双语 | iOS、Web、平板适配（Nav3 Scenes 留口子） |
| 上架 | Google Play | 国内市场、App Store |

### 1.2 非目标（v1 明确不做）

- 用户账号系统、登录、云同步
- 多账本切换
- OCR 拍发票识别
- 周期账单提醒（前台服务）
- 投资组合/股票联动
- AI 自动分类
- Crash 上报与匿名分析（v1.1 评估接 Sentry 或 Firebase Crashlytics；v1 不引入第三方 SDK，避免上架隐私政策膨胀）
- 应用内购、广告、订阅

## 2. 技术栈与模块结构

### 2.1 模块拆分（多模块，starter kit 独立）

```
personal-ledger/                         # 主仓
├── app/                                 # 业务壳（Application、Hilt 入口、Activity）
├── feature-record/                      # 记录管理（流水列表、录入抽屉、记录详情）
├── feature-account/                     # 账户管理（账户列表、新建/编辑、归档）
├── feature-stats/                       # 统计页（图表、汇总）
├── feature-settings/                    # 设置（主题、语言、币种、备份）
├── core-data/                           # Room + Repository + Domain Model + 汇率拉取
├── core-ui/             # ⭐ starter kit · Compose UI Kit + Theme
├── core-network/        # ⭐ starter kit · NetRequestManager + ApiResponse + 拦截器
├── core-base/           # ⭐ starter kit · BaseViewModel（BaseBindingActivity 在纯 Compose 项目用不上，但留模块占位）
├── core-utils/          # ⭐ starter kit · BigDecimalUtil、NumberUtil、ClickUtils...
├── core-extensions/     # ⭐ starter kit · Extension.kt 系列（含 Compose 扩展）
└── build-logic/         # convention plugins（统一 module 配置）
```

**🌟 设计意图**：`core-*` 五个模块是抽象出来的通用基础设施代码，依赖关系完全单向（业务依赖 core，core 不依赖业务）。未来这五个模块可独立发布到 Maven Central（或拆成单独 GitHub repo），其他项目通过 `implementation("io.github.visiongem:via-network:1.0.0")` 引入。

### 2.2 依赖图（单向，避免循环）

```
app
 ├─→ feature-record ─┐
 ├─→ feature-account ┼─→ core-data ─→ core-network
 ├─→ feature-stats ──┤              ↘
 └─→ feature-settings┘                core-utils ─┐
                     ↓                              ├─ core-base
                     core-ui ─→ core-extensions ───┘
```

### 2.3 关键技术选型

| 类别 | 选型 | 理由 |
|---|---|---|
| UI | Jetpack Compose + Material3 | 唯一 UI 框架，无 View 系统 |
| Navigation | **Navigation 3** | 直接跑最新栈管理；Tab 多 back stack 用 `TopLevelBackStack`；BottomSheet/Dialog/List-Detail 用 Scenes API |
| DI | Hilt | starter kit 默认 |
| 数据库 | Room + KSP | 不用 KAPT |
| 网络 | Retrofit + OkHttp + Moshi（starter kit 的 NetRequestManager） | 复用 ApiResponse + CatchingCallAdapter + MoshiApiResponseTypeAdapterFactory |
| 异步 | Coroutines + Flow | 配合 starter kit 的 BaseViewModel.launchCatching 系列 |
| 图表 | **vico** | Compose 原生、活跃度高、API 干净；不用 MPAndroidChart |
| 图片 | Coil 3 | starter kit 已封装 ImageExtension |
| Json | Moshi + KSP | starter kit 配套 |
| 偏好存储 | DataStore + Protobuf | starter kit 的 DataStoreManager |
| 加密存储 | EncryptedSharedPreferences | 仅敏感字段（用户预留） |
| 测试 | JUnit5 + MockK + Turbine + Compose Test | 标准栈 |
| 构建 | Gradle Version Catalog（`libs.versions.toml`）+ build-logic convention plugins | KSP；统一 module 配置 |
| 最低 SDK | **min 26 / target 36** | 26（Android 8.0）覆盖 95%+，可用 java.time |
| 包名 | `io.github.visiongem.ledger` | 上 Play 必需稳定 applicationId |

### 2.4 不引入

- **不引入 KAPT**（用 KSP）
- **不引入 RxJava**（纯 Coroutines）
- **不引入 ButterKnife / DataBinding**（纯 Compose）
- **不引入云端依赖**（v1 全本地）

## 3. 数据模型

### 3.1 Room Entity

```kotlin
@Entity(tableName = "accounts", indices = [Index("currency"), Index("archived")])
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,                  // CASH / DEBIT_CARD / CREDIT_CARD / E_WALLET / OTHER
    val currency: String,                    // ISO 4217，锁定不可改
    val initialBalance: String,              // BigDecimal-as-String
    val icon: String,
    val color: Long,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories", indices = [Index("type"), Index("archived")])
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val type: RecordType,                    // EXPENSE / INCOME（TRANSFER 无分类）
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false
)

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(AccountEntity::class, ["id"], ["account_id"], onDelete = RESTRICT),
        ForeignKey(AccountEntity::class, ["id"], ["transfer_to_account_id"], onDelete = RESTRICT),
        ForeignKey(CategoryEntity::class, ["id"], ["category_id"], onDelete = RESTRICT)
    ],
    indices = [Index("occurred_at"), Index("account_id"), Index("transfer_to_account_id"),
               Index("category_id"), Index("type")]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RecordType,                    // EXPENSE / INCOME / TRANSFER

    @ColumnInfo(name = "account_id") val accountId: Long,
    val amount: String,                      // 正数；EXPENSE 表示出账，INCOME 表示入账
    val currency: String,                    // 冗余（=account.currency）
    val exchangeRateToBase: String,          // 当时汇率快照，主币种为 "1"

    // 转账专用
    @ColumnInfo(name = "transfer_to_account_id") val transferToAccountId: Long? = null,
    val transferToAmount: String? = null,
    val transferToCurrency: String? = null,
    val transferToExchangeRateToBase: String? = null,  // 收款方主币种汇率快照

    @ColumnInfo(name = "category_id") val categoryId: Long? = null,

    val note: String = "",
    val occurredAt: Long,                    // 用户填的发生时间
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scope: BudgetScope,                  // TOTAL / CATEGORY
    val categoryId: Long? = null,
    val period: BudgetPeriod,                // MONTHLY
    val amount: String,                      // 主币种额度
    val effectiveFrom: Long
)

@Entity(tableName = "exchange_rates", primaryKeys = ["base", "quote", "date"])
data class ExchangeRateEntity(
    val base: String,                        // "EUR"（Frankfurter 默认）
    val quote: String,
    val date: String,                        // "2026-05-08"
    val rate: String,
    val fetchedAt: Long
)

enum class RecordType { EXPENSE, INCOME, TRANSFER }
enum class AccountType { CASH, DEBIT_CARD, CREDIT_CARD, E_WALLET, OTHER }
enum class BudgetScope { TOTAL, CATEGORY }
enum class BudgetPeriod { MONTHLY }
```

### 3.2 DataStore（Protobuf）

```protobuf
message AppPreferences {
  string base_currency = 1;          // "CNY"，默认主币种
  string theme = 2;                  // SYSTEM / LIGHT / DARK
  string language = 3;               // SYSTEM / ZH / EN
  int64 last_backup_at = 4;
  bool require_unlock = 5;           // 生物识别开关，v1.1
  string last_used_account = 6;      // 录入抽屉默认账户
  int64 last_used_category = 7;
  bool first_launch_done = 8;
}
```

### 3.3 关键设计取舍（已确认）

| 决策 | 选择 |
|---|---|
| 金额存储 | `String`（BigDecimal-as-String），SQL 不做 SUM、App 层 BigDecimalUtil 累加 |
| 转账存储 | 单条 Record，`type=TRANSFER` + `transfer_to_*` 字段 |
| 跨币种转账金额 | 用户输入两端，App 不强制汇率；汇率快照各存一份 |
| 汇率快照 | 每条记录冗余存 `exchangeRateToBase`，保证主币种切换/历史还原精确 |
| 软删除 | 账户/分类 `archived`，记录物理删除 |
| 外键 | RESTRICT（强制走归档流程） |
| 时间戳 | UTC `Long` millis，UI 层 `java.time.Instant` 转 |
| ID | autoIncrement Long（v1 不做 sync） |

### 3.4 Domain Model（Repository 暴露）

```kotlin
data class Record(
    val id: Long,
    val type: RecordType,
    val account: Account,
    val amount: BigDecimal,
    val currency: String,
    val baseAmount: BigDecimal,              // = amount × exchangeRateToBase
    val category: Category?,
    val transferTo: TransferLeg?,
    val note: String,
    val occurredAt: Instant
)

data class TransferLeg(
    val account: Account,
    val amount: BigDecimal,
    val currency: String,
    val baseAmount: BigDecimal
)
```

Mapper 层（`RecordEntity` ↔ `Record`）集中在 `core-data` 内，String ↔ BigDecimal、ID ↔ 关联对象的转换都在这里。

## 4. UX 关键流程

### 4.1 录入抽屉（FAB → BottomSheet Scene）

**触发**：流水 Tab 右下 FAB 点击，弹出 BottomSheet（覆盖屏幕 70%）。

**布局自上而下**：
1. **顶部 Segment**：`支出 | 收入 | 转账` 三段切换（Material3 SegmentedButtonRow），切换时下方表单字段动态调整
2. **金额输入**（最大字号）：右侧显示账户币种符号（¥/$/€），点击弹出**内置计算器键盘**（含 +/-/×/÷）
3. **分类 Chip 行**（横滑 LazyRow）：常用分类图标 + 名字，最右一个 "+" 跳转分类管理（仅 EXPENSE/INCOME 显示）
4. **账户行**：
   - 支出/收入：单选账户下拉
   - 转账：左右两个账户下拉（出账 + 入账），跨币种时下方显示「收到金额」输入
5. **备注输入**（单行）
6. **时间**：默认今天，点击展开日期选择器
7. **底部**：主按钮「保存」+ 次按钮「保存并连记」（保存后清空金额，保留分类/账户，便于连续录入）

**保存流程**：
- 主线程做表单校验（金额 > 0、账户必选、分类非转账时必选）
- ViewModel `launchWithLoading` 调用 Repository
- Repository 流程：
  1. 拉当日汇率（命中缓存就跳过）
  2. 写 RecordEntity（含汇率快照）
  3. 触发 Flow 更新流水列表
- 保存成功 → toast「记账成功」+ 关闭抽屉（或清空连记）
- 失败 → 表单内错误提示

**离线汇率缺失**：使用 DataStore 缓存的最近成功汇率，UI 顶部小字提示「汇率为 X-X-X 缓存」。

### 4.2 转账逻辑

**同币种**：用户只输入一个金额，自动应用到两端。  
**跨币种**：用户输入「转出 X」和「收到 Y」两个值。App 计算"实际汇率 = Y / X"作为参考显示，但不强制等于 API 汇率（手续费/汇差由用户自行承担）。

**对账影响**：
- 出账户余额 -= X
- 入账户余额 += Y
- 不计入收入也不计入支出（统计页过滤 `type != TRANSFER`）

### 4.3 首页（流水 Tab）

```
┌─────────────────────────────┐
│  ╔═══════════════════════╗  │  顶部资产卡（渐变背景）
│  ║  总资产（CNY）         ║  │  - 主币种汇总
│  ║  ¥ 86,420.50          ║  │  - 各账户余额 chip 横滑
│  ║  💵 ¥1,200  💳 ¥85K   ║  │
│  ╚═══════════════════════╝  │
├─────────────────────────────┤
│ 本月支出 ¥3,420  收入 ¥12,000│  月汇总条 + 月份切换
│ 2026 / 05 ›                  │
├─────────────────────────────┤
│ 5月7日 · 周二 · 支出 ¥85    │  日期分组 sticky header
│ 🍱 餐饮·午饭     -¥35      │  流水 LazyColumn
│ 🚕 交通·打车     -¥50      │
│ ─────────                   │
│ 5月6日 · 周一 · 支出 ¥120   │
│ 🛍️ 购物·超市     -¥120     │
│ ...                         │
└─────────────────────────────┘
                          ┌──┐
                          │ +│ FAB
                          └──┘
```

**交互**：
- 资产卡点击 → 跳转账户列表
- 账户 chip 点击 → 跳转该账户记录筛选
- 月份切换条点击 → 弹日期选择器（年/月）
- 流水条目点击 → 跳记录详情
- 流水条目左滑 → 删除/编辑（Material3 SwipeToDismiss）

### 4.4 统计 Tab

**默认展示**：本月

**结构**：
1. **时间维度切换**：周 / 月 / 年（顶部 Tab）+ 左右切换具体周期
2. **总览卡**：周期内支出、收入、结余（三个数字 + 同比/环比小字）
3. **月趋势折线图**（vico LineChart）：日支出按日打点，叠加收入虚线
4. **分类饼图**（vico PieChart）：占比可点击，下面跟着展示该分类明细
5. **账户分布**（横条形图）：哪些账户出/入最多

### 4.5 我的 Tab（设置入口）

```
- 账户管理 ›
- 分类管理 ›
- 预算设置 ›
- 主币种 ›（CNY 默认）
- 主题 ›（跟随系统）
- 语言 ›（跟随系统）
- 数据备份 ›
  - 导出 CSV
  - 导出全量 ZIP（含 Room db + DataStore）
  - 从备份恢复
- 关于
  - 版本号
  - 开源协议（Apache 2.0）
  - GitHub 链接
```

### 4.6 数据备份

**CSV 导出**：
- 路径：用户通过 Storage Access Framework 选保存位置
- 格式：UTF-8 BOM、表头中英双语
- 字段：日期 / 类型 / 账户 / 分类 / 金额 / 币种 / 主币种金额 / 备注

**ZIP 全量备份**：
- 内容：`records.db` + `app_preferences.pb` + `metadata.json`（版本号、备份时间）
- 路径：SAF 选保存位置
- 恢复时校验 schema 版本，做必要 migration

**主动备份提醒**：v1 不做主动通知，但 DataStore 记 `last_backup_at`，超过 30 天在 我的 Tab → 数据备份 入口显示红点。

**SAF 兼容性**：Storage Access Framework 在 Android 7.0+（API 24）全面可用，min SDK 26 完全覆盖。

## 5. Navigation 3 路由设计

### 5.1 Routes 定义

```kotlin
// app/navigation/Routes.kt
sealed interface TopLevelRoute { val icon: ImageVector; val labelRes: Int }

data object Records : TopLevelRoute {
    override val icon = Icons.Default.Receipt
    override val labelRes = R.string.tab_records
}
data object Stats : TopLevelRoute {
    override val icon = Icons.Default.PieChart
    override val labelRes = R.string.tab_stats
}
data object Mine : TopLevelRoute {
    override val icon = Icons.Default.Person
    override val labelRes = R.string.tab_mine
}

val TOP_LEVEL_ROUTES = listOf(Records, Stats, Mine)

// ===== 流水 Tab 子路由 =====
data class RecordDetail(val recordId: Long)
data class RecordEdit(val recordId: Long? = null)              // null=新建，BottomSheet Scene
data class AccountFilter(val accountId: Long)                  // 该账户记录列表

// ===== 账户管理 =====
data object AccountList
data class AccountEdit(val accountId: Long? = null)

// ===== 分类管理 =====
data object CategoryManager
data class CategoryEdit(val categoryId: Long? = null, val type: RecordType)

// ===== 预算 =====
data object BudgetList
data class BudgetEdit(val budgetId: Long? = null)

// ===== 设置 =====
data object Settings
data object CurrencySettings
data object ThemeSettings
data object LanguageSettings
data object BackupSettings
data object About

// ===== 转账（BottomSheet Scene）=====
data class TransferSheet(val fromAccountId: Long? = null)
```

### 5.2 BackStack 包装

```kotlin
// app/navigation/LedgerBackStack.kt
class LedgerBackStack : TopLevelBackStack<Any>(startKey = Records) {
    // 顶级切换
    fun switchTab(route: TopLevelRoute) = addTopLevel(route)

    // 子页面跳转（在当前 Tab 栈内 push）
    fun goRecordDetail(id: Long) = add(RecordDetail(id))
    fun goAccountEdit(id: Long? = null) = add(AccountEdit(id))
    fun goRecordEditSheet(id: Long? = null) = add(RecordEdit(id))   // Sheet Scene
    fun goTransferSheet(fromId: Long? = null) = add(TransferSheet(fromId))
    // ...
}
```

### 5.3 EntryProvider DSL

> 下面是设计示意，最终 API 以 Navigation 3 当时锁定版本为准（alpha/beta 阶段 metadata API 可能微调）。

```kotlin
// app/navigation/EntryProvider.kt
@Composable
fun ledgerEntryProvider(backStack: LedgerBackStack) = entryProvider {
    entry<Records> { RecordsScreen(backStack) }
    entry<Stats> { StatsScreen(backStack) }
    entry<Mine> { MineScreen(backStack) }

    entry<RecordDetail> { key -> RecordDetailScreen(key.recordId, backStack) }

    // BottomSheet Scene
    entry<RecordEdit>(metadata = BottomSheetScene.metadata()) { key ->
        RecordEditSheet(key.recordId, onDismiss = { backStack.removeLast() })
    }
    entry<TransferSheet>(metadata = BottomSheetScene.metadata()) { key ->
        TransferEditSheet(key.fromAccountId, onDismiss = { backStack.removeLast() })
    }

    // 普通页面
    entry<AccountList> { AccountListScreen(backStack) }
    entry<AccountEdit> { key -> AccountEditScreen(key.accountId, backStack) }
    // ... 略
}
```

### 5.4 主 Activity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerTheme {
                val backStack = remember { LedgerBackStack() }
                LedgerScaffold(backStack = backStack)
            }
        }
    }
}

@Composable
fun LedgerScaffold(backStack: LedgerBackStack) {
    Scaffold(
        bottomBar = { LedgerBottomBar(backStack) },
        floatingActionButton = {
            if (backStack.topLevelKey == Records) {
                FloatingActionButton(onClick = { backStack.goRecordEditSheet() }) {
                    Icon(Icons.Default.Add, null)
                }
            }
        }
    ) { padding ->
        NavDisplay(
            modifier = Modifier.padding(padding),
            backStack = backStack.backStack,
            onBack = { backStack.removeLast() },
            entryProvider = ledgerEntryProvider(backStack),
            sceneStrategy = SceneStrategy.builtIn() + bottomSheetSceneStrategy()
        )
    }
}
```

## 6. 模块依赖与代码组织

### 6.1 starter kit 模块（v1 即可独立发布）

| 模块 | 内容 | 后续独立发布形态 |
|---|---|---|
| `core-network` | NetRequestManager、ApiResponse、CatchingCallAdapterFactory、MoshiApiResponseTypeAdapterFactory、HashMapJsonAdapter、ContentTypeInterceptor、DevDns、ApiResponsePagingSource | `io.github.visiongem:via-network:1.0.0` |
| `core-base` | BaseViewModel（launchCatching 系列） | `io.github.visiongem:via-base:1.0.0` |
| `core-utils` | BigDecimalUtil（String 扩展数学）、NumberUtil、ClickUtils、KeyBoardUtils、SpanUtil、FileUtil | `io.github.visiongem:via-utils:1.0.0` |
| `core-extensions` | Extension.kt、TextExtension.kt、ImageExtension.kt、ComposeExtension.kt、ModifierExtension.kt | `io.github.visiongem:via-ext:1.0.0` |
| `core-ui` | Theme（Color/Type/Font/Theme）+ ViaInput、ViaFillButton、ViaOutlineButton、ViaTopBar、ViaDivider、ViaTip、ViaSwitch、ViaCheckbox、ViaPullToRefreshBox、ViaLoadingPage/EmptyPage/ErrorPage、ViaBottomSelectorDialog/ViaBottomExplainDialog | `io.github.visiongem:via-compose-ui:1.0.0` |

### 6.2 业务模块

| 模块 | 内容 |
|---|---|
| `core-data` | Room DB + DAO + Repository + Domain Model + Mapper + 汇率拉取（用 core-network 框架）+ FrankfurterApi 接口定义 |
| `feature-record` | 流水列表、记录详情、录入 BottomSheet、转账 BottomSheet、ViewModel |
| `feature-account` | 账户列表、账户编辑 |
| `feature-stats` | 统计页（vico 图表、月趋势、分类饼图、账户分布） |
| `feature-settings` | 设置入口、主币种、主题、语言、备份、关于 |
| `app` | Application、Hilt 入口、MainActivity、Routes、LedgerBackStack、EntryProvider、ScaffoldShell |

### 6.3 包结构（每个 feature module 内）

```
feature-record/
├── data/                     # 仅 ViewModel-facing repository wrapper（如有）
├── domain/                   # use case（可选，简单场景直接 repository）
├── ui/
│   ├── records/              # 流水列表
│   │   ├── RecordsScreen.kt
│   │   ├── RecordsViewModel.kt
│   │   └── RecordsUiState.kt
│   ├── detail/
│   ├── edit/                 # BottomSheet
│   └── transfer/
└── di/                       # @Module @InstallIn(...)
```

## 7. 测试策略

### 7.1 单元测试（JUnit5 + MockK + Turbine）

| 层 | 测什么 | 覆盖率目标 |
|---|---|---|
| Domain Model / Mapper | Entity↔Domain 双向、BigDecimal 精度、汇率换算 | 100% |
| Repository | Room DAO mock + 汇率 API mock，跑业务流程 | 80% |
| ViewModel | UiState 转换、错误处理、launch* 系列 | 70% |
| BigDecimalUtil（core-utils） | 边界值、精度、舍入 | 100% |
| 网络层（core-network） | ApiResponse 反序列化、CatchingCall 异常分类 | 80% |

### 7.2 UI 测试（Compose Test）

- 录入抽屉关键路径：填金额→选分类→选账户→保存
- 转账跨币种：两端金额非空校验
- 流水列表筛选：账户/分类/时间过滤
- 主题切换：浅色↔深色不闪退

### 7.3 端到端

v1 跳过；v1.1 用 Espresso + Hilt Testing 做 1-2 个关键流程。

### 7.4 数据库迁移测试

每次 schema 变动都写 `MigrationTest`：插入旧版数据 → 跑 migration → 断言新结构 + 数据完整。

## 8. 里程碑（5–8 周）

| 周 | 目标 | 关键产出 |
|---|---|---|
| **W1** | 项目骨架 + starter kit 落位 | 多模块结构、版本目录、Hilt 配置、core-* 5 个模块代码迁移完毕 + 单测全绿 |
| **W2** | Room + Repository | accounts/categories/records 表、DAO、Repository、Mapper、Domain Model；账户/分类管理 UI（feature-account） |
| **W3** | 录入主流程 | FAB + BottomSheet、内置计算器、保存逻辑、首页流水列表（不含资产卡） |
| **W4** | 多币种 + 转账 | Frankfurter API 接入、汇率快照逻辑、汇率缓存、转账 BottomSheet（同币种）、统计页空壳 |
| **W5** | 统计 + 资产卡 | 资产卡组件、统计页 vico 图表（折线 + 饼图 + 条形图） |
| **W6** | 预算 + 设置 | 月度预算 CRUD、超支提示、设置页所有子项、主题/语言切换 |
| **W7** | 备份 + i18n + 打磨 | CSV 导入导出、ZIP 全量备份/恢复、英文翻译、空状态/错误页打磨、Edge-to-edge 适配 |
| **W8** | 上架前打磨 | 应用图标、启动图、Play Store 资料（截图、隐私政策、商品描述）、签名配置、Play Internal Track 上传、邀请 3-5 个 beta 用户 |

**Buffer**：每周预留 1 天处理 starter kit 模块抽离时发现的问题（API 设计调整、依赖剥离）。

## 9. 副项目接口（API 调试工具）

`personal-ledger` 完成 W2 的 starter kit 落位后，**core-network 模块即可独立发布**。届时启动副项目 `via-api-tester`：

- 单独 GitHub repo，依赖 `io.github.visiongem:via-network:1.0.0`
- 让 core-network 在第二个真实项目里跑起来 → 暴露 API 设计问题，反哺 personal-ledger
- 副项目预算：2-3 周，与 personal-ledger 后期并行

副项目设计在自己的 spec 里另写，不在本文档范围。

## 10. 已知风险与决策日志

| 风险 | 应对 |
|---|---|
| Navigation 3 仍在 alpha/beta 阶段 | 锁定具体 alpha 版本；如果 W3 之前 API 大改，回退 Nav2 + 重写路由层（成本约 1 天） |
| vico 图表跨版本断层 | 锁定 minor 版本；图表层代码隔离在 feature-stats，替换图表库不影响其他模块 |
| Frankfurter 服务可用性 | 实现降级：DataStore 缓存 + UI 提示「汇率为缓存」+ 允许用户手动设置当日汇率（v1.1） |
| Google Play 上架审核 | 提前准备隐私政策（GitHub Pages 托管）；v1 不申请敏感权限 |
| BigDecimal-as-String SQL 不能 SUM | 接受；记账场景单月 < 200 条，App 层累加无性能压力 |

## 11. 后续待决（不影响 v1 启动）

- App 显示名（中文 + 英文）—— 上架时定，工作名 `Personal Ledger`
- 应用图标设计 —— W7 处理
- GitHub repo 名 —— 工作名 `personal-ledger`，账号 `visiongem` 已就绪
- Play Store 描述文案 —— W8 处理

