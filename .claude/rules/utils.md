# 工具类使用规范

> 实现功能前，MUST 先在以下目录搜索是否已有现成工具：
> - `:core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/`
> - `:core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/`
>
> 禁止重复造轮子。

## 货币 / 金额（CurrencyFormatter）

涉及金额展示的所有 UI **MUST** 走 `io.github.visiongem.ledger.core.utils.CurrencyFormatter`。

```kotlin
import io.github.visiongem.ledger.core.utils.CurrencyFormatter

Text("$sign ${CurrencyFormatter.format(amount, currencyCode)}")
```

- `format(amount, currencyCode, locale = Locale.getDefault())` — 输出 `"USD 1,234.50"`、`"JPY 1,234"`（按 ISO 4217 小数位）。
- `formatAmount(amount, scale = 2, locale)` — 仅数字部分（不带币种前缀）。
- `defaultScaleFor(currencyCode)` — 拿 ISO 4217 标准小数位（USD=2、JPY=0、BHD=3 等）。

**禁止**：
- `BigDecimal.toPlainString()` 直接渲染到 UI（会丢千分位，且不按币种区分小数位）。
- 在 UI 层手动 `String.format("%.2f", ...)`（locale 不可控）。

## BigDecimal（大数计算）

涉及金额 / 数量 / 价格 / 比例 / 精度的计算，**绝对禁止** `Double / Float` 做业务计算。

### 必须遵守

- 加减乘除：`BigDecimal.add(...)` / `.subtract(...)` / `.multiply(...)` / `.divide(... scale, RoundingMode)`。
- 舍入：显式指定 `RoundingMode`（通常 `HALF_UP`）；`.divide(other)` 不指定 scale 会在无限循环小数时抛 `ArithmeticException`。
- 构造：用 `BigDecimal("1.23")`（字符串）或 `BigDecimal.valueOf(1.23)`；**禁止** `BigDecimal(1.23)`（double 构造会引入二进制误差）。
- 测试断言：`BigDecimal("1.50").equals(BigDecimal("1.5"))` 为 `false`（scale 不同），需要用 `.compareTo() == 0` 或 `.stripTrailingZeros()`；Truth 的 `isEqualTo` 直接调 `equals`，断言时建议用 `BigDecimal("1.50")` 显式 scale。

### 推荐扩展

项目目前没有 BigDecimal 扩展库；如需要重复写 `BigDecimal.ZERO + ` 模式时，先在 `:core-utils` 增补扩展函数。

## 日期 / 时间

- `LocalDate` 持久化为 `Long`（epochDay）通过 `LedgerTypeConverters`。
- `Instant` 持久化为 `Long`（epochMilli）。
- `YearMonth` 持久化为 `String`（`"yyyy-MM"` via `LedgerTypeConverters`）。
- UI 显示日期 MUST 用 `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())`。
- UI 显示日期时间 MUST 用 `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())`（Instant 转字符串前必须绑 ZoneId）。

## 偏好（UserPreferencesRepository）

- 读取：`userPreferencesRepository.flow` → `Flow<UserPreferences>`。
- 写入：MUST 通过 interface 的 setter（`setThemeMode`、`setDefaultCurrency`、`setLastRateRefreshAt`），不直接操作 DataStore。

## 网络（ApiResponse + Retrofit）

- 业务接口走 `:core-network` 的 `ApiResponseConverterFactory`；接口签名直接返回 domain 类型，错误自动抛 `ApiException`。
- 第三方接口（如 Frankfurter）用独立的 raw Retrofit 实例，**禁止**复用同一个 `Retrofit.Builder`。

## 备份（RecordBackupRepository）

- 导出：`exportZip(): ByteArray`
- 导入：`importZip(bytes: ByteArray): Result<Int>`（返回导入条数）
- 单 entity CSV 工具：`AccountCsv`、`CategoryCsv`、`RecordCsv`、`BudgetCsv`、`ExchangeRateCsv`。
- 共享的 RFC 4180 escape / split：`CsvFormat` internal object。

## 启动 / Application

- `LedgerApplication` 是 `@HiltAndroidApp` + `Configuration.Provider`；周期任务在 `onCreate` 里 `scheduleExchangeRateRefresh()`。
- 不要在 Application 里加任何会阻塞主线程的初始化；耗时操作丢到 `getApplicationScope()` 协程。

## 扩展函数索引（:core-extensions）

| 用途 | 位置 |
|------|------|
| Kotlin 通用扩展 | `core-extensions/.../KotlinExt.kt`（如有） |
| Compose Modifier 扩展 | `core-extensions/.../ComposeModifierExt.kt`（如有） |

> 目前 `:core-extensions` 模块基本是占位；新增扩展前请先评估是否应该归到具体的 feature 或 `:core-utils`。
