# Plan 18 · ZIP Full Backup

> **For agentic workers:** Use superpowers:subagent-driven-development.

**Goal:** 把 backup 升级成 ZIP 全量备份——账户、分类、流水、预算、汇率 全 schema 一起导出/导入。**v2 比 v1 的 CSV-only 升级**：原本只 records 出问题，restore 后账户/分类全没了；现在 ZIP 一次性恢复全部数据。

---

## 范围决策

- ZIP 内每个 entity 一份 `.csv`，文件名定死：`accounts.csv` / `categories.csv` / `records.csv` / `budgets.csv` / `rates.csv`
- CSV 用与 `RecordCsv` 相同的 RFC 4180 风格（引号转义）
- Import 按 FK 依赖序处理：accounts → categories → records → budgets → rates；遇到坏 CSV 整次回滚（Result.failure）
- **保留 row id 不重映射**：用 DAO 的 `@Upsert(entity with explicit id)`；id 冲突 = 覆盖
- 替换 Settings 的两个 CSV 按钮为两个 ZIP 按钮（旧的 CSV-only 方法保留在 repo 内做兼容，UI 不暴露）

---

## File Structure

```
core-data/src/main/.../backup/
├── RecordCsv.kt              (existing)
├── AccountCsv.kt             # T1
├── CategoryCsv.kt            # T1
├── BudgetCsv.kt              # T2
├── ExchangeRateCsv.kt        # T2
├── LedgerZipBackup.kt        # T3 ZIP 包装：5 个 entry 写/读
├── RecordBackupRepository.kt # 现存接口加 exportZip / importZip 方法
└── RecordBackupRepositoryImpl.kt  # T3 实现 ZIP 方法
```

---

## Tasks

### T1：Account + Category CSV

- 各自一个 `object` (toCsv / fromCsv) 像 RecordCsv
- AccountCsv 列：id, name, currencyCode, openingBalance, archived(0/1), createdAt(ISO 8601)
- CategoryCsv 列：id, name, type, iconKey, sortOrder
- JVM 测试 ≥6（round-trip / 空 / 引号转义 / 不合法 → null）

Commit: `feat(core-data): add Account/Category CSV format helpers`

### T2：Budget + ExchangeRate CSV

- BudgetCsv 列：id, categoryId, month(yyyy-MM), limit, currencyCode
- ExchangeRateCsv 列：baseCurrency, quoteCurrency, rate, asOf
- JVM 测试 ≥6

Commit: `feat(core-data): add Budget/ExchangeRate CSV format helpers`

### T3：LedgerZipBackup + Repository ZIP 方法

- `LedgerZipBackup` object：`writeTo(OutputStream, dump: Dump)` + `readFrom(InputStream): Result<Dump>`；`Dump` 是个 data class 包 5 个列表
- `RecordBackupRepository` 接口加 `suspend fun exportZip(): Result<Dump>` 和 `suspend fun importZip(text: ByteArray): Result<Int>`（返回导入条目总数）
- Impl 依赖所有 5 个 DAO（按 FK 序写入）
- 在 ZipOutputStream 写 5 个 entry；ZipInputStream 按 entry 名读出
- 集成测试：往 in-memory Room 塞数据 → exportZip 拿到 ByteArray → importZip → 验证所有 5 张表数据一致
- 测试用 ByteArray*Stream 跑 JVM；通过 Robolectric 跑 DB

Commit: `feat(core-data): add ZIP full-backup export/import`

### T4：Settings UI 切到 ZIP + strings + 最终验证

- `feature-settings` 的 backup section：两个按钮改为 "Export backup (ZIP)" / "Import backup (ZIP)"
- ViewModel 的 export/import URI 方法切到 `exportZip` / `importZip`，MIME `application/zip`
- 新中英 strings：`settings_backup_export_zip` / `settings_backup_import_zip`
- Run `./gradlew clean assembleDebug test`

Commit: `feat: wire ZIP backup into Settings`

---

## Definition of Done

- [x] 4 个新 CSV format helper + tests
- [x] LedgerZipBackup writes/reads 5-entry ZIP
- [x] Round-trip 集成测试通过：write → read → 数据一致
- [x] Settings UI 用 ZIP 按钮取代 CSV
- [x] 全部已有 + 新增测试通过

---

## 已知限制

- 大数据量：当前 export 一次性把全表 load 进内存。10K+ 条 records 后可能需要流式。v3 评估
- 跨版本兼容：ZIP 里没存 schema 版本号；v2.x → v3 schema 变了时需要迁移层
- 部分恢复：当前要么全成功要么全失败（FK 顺序约束）；用户没法只导某张表
