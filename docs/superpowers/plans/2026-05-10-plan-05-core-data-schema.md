# Plan 05 · core-data Schema (Domain + Room + Mappers)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development.

**Goal:** 给 `core-data` 落 v1 的数据层骨架——纯 Kotlin 领域模型、Room schema (entities/DAOs/AppDatabase)、双向 Mapper、Hilt provides。Plan 06 接 Repository 实现 + Frankfurter API。

**Architecture:**

- **三层分离**：
  - `domain/`：纯 Kotlin（`BigDecimal` / `LocalDate` / enum），不依赖 Room/Android
  - `local/entity/`、`local/dao/`、`local/AppDatabase.kt`：Room 持久化层
  - `local/mapper/`：domain ↔ entity 转换（extension functions）
- **不引入 Repository 层（留 Plan 06）**：本 Plan 只暴露 DAO 给 Hilt，Repository 在 Plan 06 落地后才会成为 feature 模块的依赖
- **多币种存储**：每个 `AccountEntity` 记 ISO 4217 currency code；`RecordEntity` 金额按账户币种存；`BudgetEntity` 自带币种字段（与账户独立）
- **转账作为特殊 Record**：`type = TRANSFER`，关联 `transferToAccountId` + `transferAmount`（目标账户币种），单条 record 表达双账户变动
- **TypeConverters**：BigDecimal ↔ String、LocalDate ↔ epochDay (Long)、YearMonth ↔ "yyyy-MM"、Instant ↔ epochMilli (Long)

**Tech Stack:** Room 2.6.1（已接 + KSP）、java.time（minSdk 26 直接可用）

**Spec 参考：** spec §1.1（数据模型）、§3.2（DataStore；本 Plan **不**碰 DataStore）

---

## 范围决策

- **不做 DataStore 偏好**：Plan 06+；本 Plan 只 Room
- **不做 Repository**：Plan 06；DAOs 已经能给 Repository 调用
- **不做 ExchangeRate 拉取**：Plan 06 接 Frankfurter；本 Plan 只定义 `ExchangeRate` 表/DAO，让快照可以被持久化
- **不做 androidTest**：DAO 跑 in-memory Room 需要 Android Context → Robolectric 或 instrumentation。**仅 mapper 写 JVM 单测**；DAO 测试推到后续 plan
- **不做 schema 导出**：`exportSchema = false`（v1 私人项目，没有迁移历史负担；v2 启用）

---

## 实体清单（v1 数据模型）

| Domain / Entity | 字段 |
|---|---|
| `Account` | id, name, currencyCode (ISO 4217), openingBalance: BigDecimal, archived: Boolean, createdAt: Instant |
| `Category` | id, name, type (INCOME/EXPENSE), iconKey: String?, sortOrder: Int |
| `Record` | id, accountId, categoryId?, type (INCOME/EXPENSE/TRANSFER), amount: BigDecimal, occurredOn: LocalDate, note?, transferToAccountId?, transferAmount?: BigDecimal |
| `Budget` | id, categoryId, month: YearMonth, limit: BigDecimal, currencyCode |
| `ExchangeRate` | baseCurrency, quoteCurrency, rate: BigDecimal, asOf: LocalDate（联合主键 base+quote+asOf） |

枚举：`CategoryType`、`RecordType`。

---

## File Structure

```
core-data/src/main/kotlin/io/github/visiongem/ledger/core/data/
├── domain/
│   ├── Account.kt                # T1
│   ├── Category.kt               # T1（含 CategoryType enum）
│   ├── Record.kt                 # T1（含 RecordType enum）
│   ├── Budget.kt                 # T1
│   └── ExchangeRate.kt           # T1
├── local/
│   ├── AppDatabase.kt            # T5
│   ├── converter/LedgerTypeConverters.kt  # T2
│   ├── entity/
│   │   ├── AccountEntity.kt      # T3
│   │   ├── CategoryEntity.kt     # T3
│   │   ├── RecordEntity.kt       # T4
│   │   ├── BudgetEntity.kt       # T4
│   │   └── ExchangeRateEntity.kt # T5
│   ├── dao/
│   │   ├── AccountDao.kt         # T3
│   │   ├── CategoryDao.kt        # T3
│   │   ├── RecordDao.kt          # T4
│   │   ├── BudgetDao.kt          # T4
│   │   └── ExchangeRateDao.kt    # T5
│   └── mapper/
│       ├── AccountMapper.kt      # T6
│       ├── CategoryMapper.kt     # T6
│       ├── RecordMapper.kt       # T6
│       ├── BudgetMapper.kt       # T6
│       └── ExchangeRateMapper.kt # T6
└── di/DataModule.kt              # T7

core-data/src/test/kotlin/...
└── local/mapper/*MapperTest.kt   # T6（5 个 mapper 各一个 JVM 测试）

# 删除：
core-data/.../Placeholder.kt      # T7
```

---

## 通用规则

1. **包名**：`io.github.visiongem.ledger.core.data.{domain,local,di}`
2. **不引入业务**：domain 类只描述数据形态，不带业务规则方法
3. **金额用 BigDecimal**：Room 层用 String 持久化（TypeConverter），domain 层用 `java.math.BigDecimal`
4. **TDD（mapper）**：每个 mapper 写 ↔ 双向测试 + 一个 round-trip
5. **DAO 暂不测**：JVM 测 DAO 需要 Robolectric/instrumentation；推到后续

---

## Task 1：Domain 模型

5 个 data class + 2 个 enum。所有字段都用基本不可变类型（String / Long / BigDecimal / LocalDate / Instant / YearMonth）。`Account.id = 0L` 表示新建未持久化（Room 自增主键约定）。

Commit: `feat(core-data): add domain models for v1 ledger entities`

---

## Task 2：TypeConverters

`object LedgerTypeConverters` 标 `@TypeConverter` 方法对：
- BigDecimal ↔ String（`toPlainString` / `BigDecimal(...)`，null 安全）
- LocalDate ↔ Long (epochDay)
- Instant ↔ Long (epochMilli)
- YearMonth ↔ String（`yyyy-MM`）

Commit: `feat(core-data): add Room type converters for BigDecimal/temporal types`

---

## Task 3：Account + Category 实体 + DAO

- `AccountEntity` (`@Entity(tableName = "account")`)、`@PrimaryKey(autoGenerate = true) id`
- `CategoryEntity` (`@Entity(tableName = "category")`)、type 字段用 `@TypeConverter` 不行（enum 直接写 ordinal/name）—— Room 6+ 支持直接持久化 enum，用 name string
- DAO：`upsert`、`observeAll(): Flow<List<...>>`、`observeById(id): Flow<...>`、`deleteById(id)`
- Account DAO 增加：`observeActive(): Flow<List<AccountEntity>>` (archived = false)
- Category DAO 增加：`observeByType(type: CategoryType): Flow<List<CategoryEntity>>`

Commit: `feat(core-data): add Account/Category entities and DAOs`

---

## Task 4：Record + Budget 实体 + DAO

- `RecordEntity`：FK 到 Account（`onDelete = RESTRICT`）、Category（`onDelete = SET_NULL`，因为 category 可删但 record 应保留）；index 在 `accountId`、`categoryId`、`occurredOn`
- `BudgetEntity`：FK 到 Category（`onDelete = CASCADE`，category 删了对应预算也清掉）；联合 unique index `(categoryId, month)`
- DAO：`upsert`、`observeByAccount(accountId)`、`observeByMonth(year, month)`、`sumAmountByCategory(start, end)` 用于统计页

Commit: `feat(core-data): add Record/Budget entities and DAOs with FK + indexes`

---

## Task 5：ExchangeRate 实体 + DAO + AppDatabase

- `ExchangeRateEntity`：联合主键 `(baseCurrency, quoteCurrency, asOf)`；index 在 `asOf`
- DAO：`upsertAll(List)`、`getRate(base, quote, asOf): ExchangeRateEntity?`、`observeLatest(base, quote): Flow<ExchangeRateEntity?>`
- `@Database(entities = [...5 个 entity...], version = 1, exportSchema = false)` `@TypeConverters(LedgerTypeConverters::class)` `abstract class AppDatabase : RoomDatabase()`，5 个 abstract DAO accessor

Commit: `feat(core-data): add ExchangeRate schema and AppDatabase`

---

## Task 6：Mappers + JVM tests

每个 entity 一个 mapper 文件，提供 `Entity.toDomain()` + `Domain.toEntity()` extension。Tests：

- 每个 mapper 写 ≥3 个测试：domain → entity 字段映射、entity → domain 字段映射、round-trip 等价
- 用 Truth + JUnit 5

Commit: `feat(core-data): add domain↔entity mappers with JUnit 5 coverage`

---

## Task 7：Hilt @Module + delete Placeholder + 验证

- `DataModule` `@Provides @Singleton`：
  - `provideAppDatabase(@ApplicationContext context): AppDatabase` — `Room.databaseBuilder(...).build()`
  - `provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()`（其余 4 个 DAO 同模式）
- 删 `Placeholder.kt`
- Run `./gradlew clean assembleDebug test`：编译通过 + mapper 测试 + 已有 56 单测全绿

Commit: `feat(core-data): wire Hilt @Module and drop placeholder`

---

## Definition of Done

- [x] 5 domain + 5 entity + 5 DAO + 5 mapper + AppDatabase + Hilt @Module
- [x] Mapper JVM 测试 ≥15 个全部通过
- [x] core-data Placeholder.kt 删除
- [x] `./gradlew clean assembleDebug test` 全部通过
- [x] 7 个 commit

---

## 已知限制（留给 Plan 06+）

- Repository 层未实现：feature 模块暂不能直接消费 core-data
- DAO 测试缺：需 Robolectric 或 androidTest，留 Plan 07
- DataStore 偏好（主题、语言、上次同步时间）：Plan 06 一起接
- DB 迁移：v1 不出 schema，v2 引入版本时再补
