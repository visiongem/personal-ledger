---
description: 对当前变更进行 Code Review，按严重程度输出问题
---

对当前 git 变更（staged + unstaged + untracked）进行 code review。如果提供了 `$ARGUMENTS`，则仅 review 指定范围。

先执行 `git diff HEAD` 和 `git diff --cached` 获取变更内容，只审查变更涉及的代码。

---

## 审查维度

### 1. Critical — 业务与安全

- 金额 / 数量 / 汇率等业务计算禁止 `Double / Float`；必须用 `BigDecimal`，构造避免 `BigDecimal(double)`
- UI 显示金额禁止 `BigDecimal.toPlainString()` 直出；MUST 走 `CurrencyFormatter.format(...)`
- Log / 备份 / 截图禁止打印 PII（虽然项目无账号体系，仍要警惕将来引入时的旧代码）
- Room 外键级联策略：
  - `record.accountId → account.id` 是 RESTRICT；不能硬删账户，必须用 `archived = true` 软删
  - `record.categoryId → category.id` 是 SET NULL；分类硬删后流水会变"无分类"，删除路径需要测试覆盖
- WorkManager 自定义 `Configuration.Provider` 时 AndroidManifest MUST `tools:node="remove"` 掉默认 initializer

### 2. Warning — 架构与错误处理

- 路由类 MUST `@Parcelize` + `Parcelable`，否则 saveable back stack 还原崩
- ViewModel MUST 继承 `BaseViewModel`，走 `launchCatching { ... }`，禁止裸 `try { ... } catch (Throwable) { ... }`
- ViewModel **禁止**直接持有 DAO / Retrofit 接口；必须通过 Repository
- ViewModel 内本地化错误消息走 `@ApplicationContext private val context: Context` + `context.getString(R.string.xxx)`
- Flow 收集 MUST 用 `collectAsStateWithLifecycle()`，禁止 `collectAsState()`
- 协程 IO 操作 MUST 指定 `Dispatchers.IO`（除非已在 Repository 内）
- Hilt 注入：`@HiltViewModel` + `@Inject constructor(...)`；模块用 `@Module @InstallIn(SingletonComponent::class)`
- `!!` 强转、`@Nullable` 返回值未处理
- 跨模块 smart cast 失败时改用 `let { ... }` 而不是 `!!`

### 3. Suggestion — UI 与资源

- Compose 颜色 MUST 用 `MaterialTheme.colorScheme.xxx`；禁止 `Color(0xFF...)` 字面值（启动图 / 图标 vector 除外）
- 通用组件 MUST 用 `:core-ui` 的 `Via*` 系列（`ViaTopBar`、`ViaFillButton`、`ViaOutlineButton`、`ViaBottomSelector`、`ViaSelectorField`、`ViaEmptyPage`、`ViaLoadingPage`、`ViaPieChart`、`ViaHorizontalBarChart`）；不要重新发明轮子
- 新增字符串 MUST 同步 `values/strings.xml`（英文）与 `values-zh/strings.xml`（中文），追加到文件最底部
- 字符串动态部分用 `%1$s` 占位符，禁止字符串拼接
- 日期 / 时间 / 货币展示 MUST 用 locale-aware 的 `DateTimeFormatter` / `CurrencyFormatter`；禁止硬编码 `yyyy-MM-dd`
- 默认不写注释；仅"为什么"非显而易见时才加一行
- 单 Activity 架构：禁止新增 `Activity` 子类，所有页面走 Compose Composable

---

## 输出格式

按以下格式输出，无问题则回复 "未发现问题"：

### 🚨 Critical
- **[文件:行号]**: 问题描述。**建议**: 修复方案。

### ⚠️ Warning
- **[文件:行号]**: 问题描述。**建议**: 修复方案。

### 💡 Suggestion
- **[文件:行号]**: 问题描述。**建议**: 修复方案。

只输出有把握的问题，不凑数。如果改动是文档 / 配置 / 测试，可以跳过不适用的维度。
