# Plan 11 · TRANSFER record support

> **For agentic workers:** Use superpowers:subagent-driven-development.

**Goal:** 把流水录入扩到第三种类型——`TRANSFER`（转账）。完成后 v1 数据模型的所有 `RecordType` 都可达。

**Architecture:**

- `RecordEditViewModel` 增加 `targetAccountId`、`transferAmount` 字段；当类型为 TRANSFER 时绕过 category，改要求 source ≠ target；当源/目标账户币种不同时强制要求 `transferAmount`
- `RecordEditScreen` 第三个 SegmentedButton "Transfer"；切到 TRANSFER 后 category dropdown 替换为 target account dropdown，并条件显示 destination amount 输入
- `RecordListScreen` 的 TRANSFER 行用 "→" + 显示 source → target 账户名

---

## 范围决策

- v1 不做转账手续费（fee 字段，spec 也没要求）
- v1 不做"自动汇率填充 transferAmount"（用 ExchangeRateRepository 算）—— 留 Plan 17 跨币种汇总时一起做
- 用户在跨币种转账时必须手输 transferAmount

---

## Tasks

### T1：RecordEditViewModel 接 TRANSFER

- UiState 加 `targetAccountId: Long?`, `transferAmount: String`
- `onTypeChange(TRANSFER)` 不再被静默拒绝；切到 TRANSFER 时 clear categoryId
- `onTargetAccountChange(id)`, `onTransferAmountChange(value)` 新增 setters
- `save()` 分支：
  - 共通：accountId 必填、amount > 0、date 合法
  - INCOME/EXPENSE：categoryId 必填、不读 target/transferAmount
  - TRANSFER：targetAccountId 必填、source ≠ target；若两账户币种不同则 transferAmount 必填 + 必须正数；categoryId 写入 null
- 测试 ≥4 个：TRANSFER same-currency 成功 / 不同币种缺 transferAmount 失败 / 不同币种填 transferAmount 成功 / source==target 失败

Commit: `feat(feature-record): support TRANSFER in RecordEditViewModel`

### T2：RecordEditScreen 接 TRANSFER UI

- SegmentedButton 加 Transfer
- 切到 TRANSFER：替换 category dropdown 为 target account dropdown；条件渲染 transferAmount 字段（仅在源/目标币种不同时）
- 视觉上 TRANSFER 主金额 label 改为 "Amount (source)"，转账金额字段 label "Amount (destination, ${target currency})"

Commit: `feat(feature-record): show TRANSFER fields in RecordEdit screen`

### T3：RecordList 显示 TRANSFER

- RecordRow 加 `targetAccountName: String?`
- ViewModel 用 accountById 也查 transferToAccountId
- Screen 的 supporting line：TRANSFER 行显示 "$accountName → $targetAccountName"，headline 用 "→ ${currency} ${amount}"（去掉 sign）
- 测试 +1：TRANSFER row composition

Commit: `feat(feature-record): render TRANSFER rows in RecordList`

### T4：最终验证

- `./gradlew clean assembleDebug test`
- 测试全绿，无 Placeholder

Commit: 无（验证 task）

---

## Definition of Done

- [x] RecordEditViewModel 支持 TRANSFER + 校验
- [x] RecordEditScreen UI 暴露第三类型 + 条件字段
- [x] RecordListScreen 正确渲染 TRANSFER 行
- [x] 单测增量覆盖 TRANSFER 路径
- [x] `./gradlew clean assembleDebug test` 通过

---

## 留给后续

- 跨币种转账时自动用 ExchangeRateRepository 提示参考汇率 → Plan 17
- 转账手续费 → 未来 v2
