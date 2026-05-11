# Plan 16 · DAO Instrumentation Tests (via Robolectric)

> **For agentic workers:** Use superpowers:subagent-driven-development.

**Goal:** 给 5 个 DAO 加 JVM 集成测试，跑 in-memory Room 数据库验证 SQL、FK、Upsert、索引行为。**不用真机**——Robolectric 4.13+ 的 JUnit 5 扩展把 Android Context 跑在 JVM 上。

---

## 范围决策

- Robolectric 配置只在 `core-data` 模块加；其他模块的 UI/逻辑测试不受影响
- DAO 测试落在 `src/test/`（JVM 路径），不进 `androidTest/`，CI 上能直接 `./gradlew test` 跑
- 每个 DAO 至少覆盖：upsert + 读回、observe Flow emit、关键 FK / 索引约束
- Mapper 测试已有，本 Plan 不重复

---

## Tasks

### T1：Robolectric + Room in-memory 测试基础设施

- Add to catalog: `robolectric = "4.14"`、`robolectric-junit5 = ...`、`androidx-test-core = ...`
- Add to `core-data/build.gradle.kts` testImpl
- `testOptions { unitTests { isIncludeAndroidResources = true } }` 在 core-data 的 android 块加（让 Robolectric 能找 Manifest）—— 通过 build-logic 已统一 / 单独 patch core-data
- Create `core-data/src/test/.../local/AppDatabaseTestRule.kt`：suspending utility that builds `AppDatabase` in-memory + provides clean-up
- Create `core-data/src/test/.../local/dao/AppDatabaseSmokeTest.kt`：tiny test that proves Robolectric + Room + JUnit 5 wires correctly (open DB, close DB)

Commit: `test(core-data): add Robolectric harness for DAO tests`

### T2：Account + Category DAO tests

Test cases each:
- **Account**: upsert + observeById; observeActive 过滤 archived; deleteById; observeAll order by createdAt DESC
- **Category**: upsert + observeByType filtering INCOME vs EXPENSE; deleteById

Commit: `test(core-data): AccountDao and CategoryDao coverage`

### T3：Record DAO tests（含 FK 关键路径）

- upsert; observeByAccount; observeInRange BETWEEN bounds
- observeByCategoryInRange
- FK RESTRICT 在删除 account 时（先 insert account + record → delete account 抛 SQLiteConstraintException）
- FK SET_NULL 在删除 category 时（categoryId 自动归 null）

Commit: `test(core-data): RecordDao with FK constraint coverage`

### T4：Budget + ExchangeRate DAO tests + 最终验证

- Budget: upsert; observeByMonth; observeByCategoryAndMonth; unique (categoryId, month) 索引（重复 insert 同月同类抛 exception 或被 upsert 替换）；CASCADE 删除（删 category → budget 跟着没）
- ExchangeRate: 联合主键 upsertAll 覆盖；observeLatest 取最新 asOf；deleteBefore 清理早期
- Run `./gradlew clean assembleDebug test`：原有 152 + 新增 ≥15 个 DAO 测试全绿

Commit: `test(core-data): Budget/ExchangeRate DAO coverage + final verify`

---

## Definition of Done

- [x] Robolectric + JUnit 5 集成走通；Smoke test 通过
- [x] 5 个 DAO 都有 JVM 测试覆盖关键路径
- [x] FK RESTRICT / SET_NULL / CASCADE 各一个验证
- [x] `./gradlew clean assembleDebug test` 全部通过

---

## 已知风险

- Robolectric 4.14 的 JUnit 5 支持仍标 experimental——API 可能在小版本间微调
- Room 编译版 SQL 行为与设备实际 SQLite 版本可能略有差异（PRAGMA defaults 等）——本 Plan 测的是 SQL/FK 语义，对小差异不敏感
