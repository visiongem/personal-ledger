# Plan 19 · Pie chart + Auto-refresh exchange rates

> **For agentic workers:** Use superpowers:subagent-driven-development.

**Goal:** 关掉 v2 剩下两个口袋——给 stats 加饼图（更直观看分类占比），给 ExchangeRateRepository 加每日自动刷新（WorkManager 周期任务）。

---

## 范围决策

- 饼图：Compose Canvas 自绘 donut（圆环），中间留空填总额；不用外部图表库
- 把 bar chart 留着——饼图给"占比"，bar chart 给"绝对值 + label"，互补不冲突
- 折线图：当前数据形态没有"每日聚合"，需要先在 VM 加新数据流；留 v3
- WorkManager：用 `androidx.work` + `androidx.hilt:hilt-work` 的 `@HiltWorker`
- 周期 24h；首次启动也立即跑一次（OneTimeWorkRequest）；用 `ExistingPeriodicWorkPolicy.KEEP` 避免重复入队
- 设置里现有的 "Refresh exchange rates" 手动按钮**保留**——失败 / 跨币种新增账户后用户可以手动重试

---

## Tasks

### T1：ViaPieChart + 用到 stats

- Create `core-ui/.../component/ViaPieChart.kt`：
  - `data class PieSlice(label, value: BigDecimal, color: Color)`
  - `@Composable ViaPieChart(slices: List<PieSlice>, modifier, strokeWidth: Dp = 24.dp)`：Canvas drawArc 画环；空/全零返回空容器
- Stats screen 顶部加 `ViaPieChart`（占比）+ legend；bar chart 保留在下方"明细"区
- 颜色用 MaterialTheme.colorScheme 的 primary / secondary / tertiary + 各自 container 系，轮换

Commit: `feat(core-ui): add ViaPieChart and render category share on stats`

### T2：WorkManager + 自动 refresh rates

- 加 catalog 别名 `androidx-work-runtime`、`androidx-hilt-work`、`androidx-hilt-compiler`
- `:app` deps 加上 work-runtime + hilt-work + ksp(androidx-hilt-compiler)
- `LedgerApplication: Configuration.Provider`：@Inject HiltWorkerFactory，override workManagerConfiguration
- `ExchangeRateRefreshWorker(@HiltWorker)`：拉 prefs 拿 default currency，拿账户币种 distinct list，调 `refreshLatest`；成功→Result.success，失败→Result.retry
- Application.onCreate 调度：OneTimeWorkRequest（立即跑）+ PeriodicWorkRequest(24h, KEEP)
- Hilt module provides Worker dependencies已经都 @Singleton（AccountRepository / UserPreferencesRepository / ExchangeRateRepository），不用补

Commit: `feat: schedule daily ExchangeRateRefreshWorker via WorkManager`

### T3：最终验证

- Run `./gradlew clean assembleDebug test`
- 203 单测全绿（pie/work 不写 JVM 单测——pie 是 UI，work 需 WorkManagerTestInitHelper，留后续）

Commit: 无

---

## Definition of Done

- [x] ViaPieChart Compose Canvas 自绘
- [x] Stats 页同时展示饼图 + 柱图
- [x] WorkManager 集成 Hilt
- [x] 应用启动调度 24h 周期 + 立即触发
- [x] Build + 单测全通过

---

## 已知限制

- 折线图（趋势）：v3
- WorkManager 单测：需 `androidx.work:work-testing`，v3
- 没有"上次刷新时间"显示给用户：v3 polish
- Refresh worker 不区分网络环境，可能在用户走流量时触发——后续可加 `setRequiredNetworkType(NetworkType.UNMETERED)` 约束
