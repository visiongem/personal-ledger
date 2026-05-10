# Plan 06 · Repositories + Frankfurter API

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development.

**Goal:** 给 `core-data` 加上 Repository 层（feature 模块的入口），并接入 Frankfurter 汇率 API。完成后 feature-* 业务模块就能直接消费 domain 类型的数据流。

**Architecture:**

- **Repository = DAO 的 domain 包装**：每个 repository 接口暴露 `Flow<DomainType>` 与 suspend CRUD；impl 类通过 `@Inject` 持有 DAO，做双向 mapper 转换
- **Frankfurter 是 raw（非 envelope）API**：复用 NetworkModule 的 `OkHttpClient` + `Moshi`，在 core-data 内部建第二个 Retrofit 实例（`@FrankfurterRetrofit` qualifier）走标准 `MoshiConverterFactory`
- **ExchangeRateRepository 同时管 cache + 拉取**：先查 DAO，缺失/过期时 fallback 到 Frankfurter，结果回写
- **聚合在 Kotlin 层做**：BigDecimal 列存的是 String，SQL 不能 SUM；Repository 拉数据后 Kotlin 累加
- **Hilt 接线**：`@Binds` 把 RepositoryImpl 绑到 Repository 接口；`RemoteModule` 提供 FrankfurterRetrofit + FrankfurterApi

**Tech Stack 增量:** mockk（已接，用于 DAO 模拟测试）

**Spec 参考：** spec §3.2（数据层）、§3.3（多币种与汇率快照）

---

## 范围决策

- **DataStore**：留 Plan 07/08（接 settings 页时一起）
- **CSV 导入导出 / ZIP 备份**：Plan 09
- **Paging 3**：v1 不引入；列表页用 Flow<List<Record>> 直出（数据量小）
- **多 Retrofit 拓展机制**：本 Plan 只加 1 个 raw Retrofit qualifier；后续要扩另一种 API 时再补

---

## File Structure

```
core-network/src/main/kotlin/io/github/visiongem/ledger/core/network/
├── di/NetworkModule.kt                 # T3 加 @LedgerRetrofit qualifier
└── di/Qualifiers.kt                    # T3 NEW（@LedgerRetrofit + @FrankfurterRetrofit）

core-data/src/main/kotlin/io/github/visiongem/ledger/core/data/
├── repo/
│   ├── AccountRepository.kt            # T1 interface
│   ├── AccountRepositoryImpl.kt        # T1
│   ├── CategoryRepository.kt           # T1
│   ├── CategoryRepositoryImpl.kt       # T1
│   ├── RecordRepository.kt             # T2
│   ├── RecordRepositoryImpl.kt         # T2
│   ├── BudgetRepository.kt             # T2
│   ├── BudgetRepositoryImpl.kt         # T2
│   ├── ExchangeRateRepository.kt       # T4
│   └── ExchangeRateRepositoryImpl.kt   # T4
├── remote/
│   ├── FrankfurterApi.kt               # T3
│   └── FrankfurterRatesResponse.kt     # T3
└── di/
    ├── RemoteModule.kt                 # T3
    └── RepositoryBindings.kt           # T5

core-data/src/test/kotlin/io/github/visiongem/ledger/core/data/
└── repo/*RepositoryImplTest.kt         # T1/T2/T4（5 个 test 文件）
```

---

## 通用规则

1. **包名**：`io.github.visiongem.ledger.core.data.{repo,remote,di}`
2. **Repository 接口暴露 Flow<Domain>，不暴露 Entity**
3. **Repository 实现 `@Inject constructor(private val dao: ...)`**
4. **TDD**：每个 RepositoryImpl 写 ≥3 个 mockk-based JVM 测试（验证 mapper 在 Flow 上正确传播 + suspend 方法委派给 DAO）
5. **聚合方法明确归属 Repository**（不归 DAO）

---

## Task 1：Account + Category Repositories

接口形态（CategoryRepository 同模式）：

```kotlin
interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    fun observeActive(): Flow<List<Account>>
    fun observeById(id: Long): Flow<Account?>
    suspend fun upsert(account: Account): Long
    suspend fun deleteById(id: Long)
}
```

Impl 用 `dao.observeXxx().map { list -> list.map { it.toDomain() } }`。

测试：mockk `AccountDao`，返回 `flowOf(listOf(entity))`，用 Turbine 验证收到的 domain 列表字段一致；suspend 方法用 `coVerify` 检查 entity 转换正确。

Commit: `feat(core-data): add Account/Category repositories with domain Flow`

---

## Task 2：Record + Budget Repositories（带聚合）

`RecordRepository` 多 1 个聚合方法：
```kotlin
fun observeCategoryTotalsInRange(
    type: RecordType,
    startInclusive: LocalDate,
    endInclusive: LocalDate,
): Flow<Map<Long, BigDecimal>>  // categoryId -> sum(amount)
```

Impl：用 `recordDao.observeInRange(...)` 拿到 `List<RecordEntity>`，过滤 type + categoryId != null，按 `categoryId` 分组用 `BigDecimal.add` 累加。

`BudgetRepository` 类似 Account 模式 + 1 个组合方法 `observeBudgetWithUsage(month: YearMonth, type: RecordType)` 返回每个 budget 配套的实际花费（用 RecordRepository 的聚合 + BudgetDao 的 observeByMonth 组合 Flow）。

> 注：组合两个 Flow 用 `combine(...)` 操作符。BudgetWithUsage 是一个 data class（domain 层）。

测试：mockk DAO 返回 entity flow，验证聚合结果数学正确；组合 flow 用 Turbine 多步 emit。

Commit: `feat(core-data): add Record/Budget repositories with aggregation`

---

## Task 3：FrankfurterApi + RemoteModule + Retrofit qualifiers

**Files:**
- Create: `core-network/.../di/Qualifiers.kt` — 两个 `@Qualifier` annotation：`@LedgerRetrofit`、`@FrankfurterRetrofit`
- Modify: `core-network/.../di/NetworkModule.kt` — 现有 `provideRetrofit` 加 `@LedgerRetrofit` qualifier
- Create: `core-data/.../remote/FrankfurterRatesResponse.kt` — Moshi `@JsonClass` envelope-less DTO（amount/base/date/rates）
- Create: `core-data/.../remote/FrankfurterApi.kt` — Retrofit interface（`@GET("latest")` 和 `@GET("{date}")`）
- Create: `core-data/.../di/RemoteModule.kt` — `@FrankfurterRetrofit` 专属 Retrofit + `FrankfurterApi` provides

要点：
- `FrankfurterRatesResponse.rates: Map<String, Double>` —— 小心 `Double` 精度，本 Plan 暂用 Double 接收，存 DB 前转 BigDecimal（`BigDecimal.valueOf(double)`）。后续 Plan 评估直接接收 BigDecimal 字符串
- BASE_URL: `"https://api.frankfurter.app/"`
- 不写测试（Retrofit 接口 + Hilt 配置）

Commit: `feat(core-data): add FrankfurterApi and RemoteModule with raw Retrofit qualifier`

---

## Task 4：ExchangeRateRepository

```kotlin
interface ExchangeRateRepository {
    fun observeLatest(base: String, quote: String): Flow<ExchangeRate?>
    suspend fun getRateOnDate(base: String, quote: String, asOf: LocalDate): ExchangeRate?
    suspend fun refreshLatest(base: String, quotes: List<String>): Result<List<ExchangeRate>>
}
```

Impl：
- `observeLatest`：直接代理 `dao.observeLatest`
- `getRateOnDate`：先 `dao.getRate(...)`，命中返回；未命中可选择拉 API（`api.getOnDate(...)`），写入 DAO，返回
- `refreshLatest`：调 `api.getLatest(base, quotes.joinToString(","))`，把返回 rates 转 BigDecimal 存 DAO，返回写入的列表 `Result.success(...)`；任何抛错（`NetworkException` / `IOException` / `HttpException`）→ `Result.failure(t)`

测试：mockk `ExchangeRateDao` 和 `FrankfurterApi`，验证 cache miss 后是否调 API + 回写、refresh 在异常时返回 Result.failure。

Commit: `feat(core-data): add ExchangeRateRepository with cache + Frankfurter fallback`

---

## Task 5：Hilt RepositoryBindings + 最终验证

`RepositoryBindings.kt`：
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindings {
    @Binds @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository
    // ... 其他 4 个
}
```

最终：
- Run `./gradlew clean assembleDebug test`：编译通过 + 所有现有 + 新增单测全绿
- 检查无新 Placeholder（core-data 已删，本 Plan 不引入新的）

Commit: `feat(core-data): wire repository @Binds module`

---

## Definition of Done

- [x] 5 Repository 接口 + 5 RepositoryImpl
- [x] 1 FrankfurterApi + 1 RemoteModule + 2 Retrofit qualifiers
- [x] RepositoryBindings 全 5 个 binding
- [x] Repository JVM 测试 ≥15 个全部通过
- [x] `./gradlew clean assembleDebug test` 全部通过
- [x] 5 个 commit

---

## 已知限制（留给 Plan 07+）

- DAO 集成测试（Robolectric/instrumentation）
- Frankfurter rate 缓存有效期策略（v1 暂不做 TTL，每次启动可调用 refreshLatest）
- 网络错误重试与离线降级：v1 用 `Result` 暴露 failure，由 ViewModel 决定 UI 提示
- DataStore（主题、语言、上次同步时间）
