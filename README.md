# Personal Ledger

个人记账 Android App，基于 Jetpack Compose + Navigation 3 + Hilt + Room + Coroutines。

## 特点

- 多账户、多币种（[Frankfurter](https://www.frankfurter.app/) 汇率，WorkManager 每日刷新）
- 收入 / 支出 / 跨币种转账，支持长按删除并 Snackbar 撤销
- 月度统计：分类饼图 + 横向柱状图，跨币种自动换算到默认币种
- ZIP 备份与恢复（accounts / categories / records / budgets / rates 五份 CSV）
- 完全本地、无账号系统、无云依赖
- 中英双语，浅色 / 深色 / 跟随系统三种主题
- 多模块架构，`core-*` 模块设计为可独立复用的 starter kit

## 模块结构

```
app                   # 业务壳（Application、Activity、Routes、ScaffoldShell）
core-base             # BaseViewModel + 通用基类
core-utils            # CurrencyFormatter、BigDecimalUtil 等工具
core-extensions       # Kotlin / Compose 扩展函数
core-network          # Retrofit + Moshi + ApiResponse 封装
core-ui               # Compose UI Kit + Theme
core-data             # Room + Repository + Domain Model + 汇率拉取 + 备份
feature-record        # 流水列表 / 录入（含 Material 3 DatePicker）
feature-account       # 账户管理（支持归档撤销）
feature-stats         # 月度统计图表
feature-settings      # 主题、默认币种、汇率刷新、备份导入导出
build-logic           # Gradle convention plugins
```

## 构建

要求：JDK 17、Android SDK 36、Gradle 8.11.1（项目自带 wrapper）

```bash
./gradlew assembleDebug         # 构建 debug APK
./gradlew :app:installDebug     # 装到设备
./gradlew test                  # 单元测试（JUnit 5 + Robolectric for DAO）
./gradlew connectedAndroidTest  # 仪器测试，含 Hilt 烟雾（需模拟器/真机）
```

测试栈：JVM 单测使用 JUnit 5 (Jupiter)，DAO 测试通过 JUnit Vintage 引擎跑 Robolectric，
`androidTest/` 因 `HiltAndroidRule` / Espresso 依赖 JUnit 4 Rule API 保持 JUnit 4。

## 开发文档

- 设计文档：`docs/specs/`
- 实施计划：`docs/superpowers/plans/`
- 隐私政策：[`docs/PRIVACY.md`](./docs/PRIVACY.md)（中英双语）

## 状态

🚧 v0.1 · 项目骨架（开发中）

## 协议

Apache License 2.0 — 详见 [`LICENSE`](./LICENSE)。
