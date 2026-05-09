# Personal Ledger

个人记账 Android App，基于 Jetpack Compose + Navigation 3 + Hilt + Room + Coroutines。

## 特点

- 多账户、多币种（Frankfurter 汇率）、月度预算、转账
- 完全本地、无账号系统、无云依赖
- 中英双语，深浅主题
- 多模块架构，`core-*` 模块设计为可独立发布的 starter kit

## 模块结构

```
app                   # 业务壳（Application、Activity、Routes、ScaffoldShell）
core-base             # BaseViewModel + 通用基类
core-utils            # BigDecimalUtil、NumberUtil、ClickUtils...
core-extensions       # Kotlin/Compose 扩展函数
core-network          # NetRequestManager + ApiResponse + 拦截器
core-ui               # Compose UI Kit + Theme
core-data             # Room + Repository + Domain Model + 汇率拉取
feature-record        # 流水列表、记录详情、录入抽屉、转账
feature-account       # 账户管理
feature-stats         # 统计图表
feature-settings      # 设置页（主题、语言、备份等）
build-logic           # Gradle convention plugins
```

## 构建

要求：JDK 17、Android SDK 36、Gradle 8.11.1（项目自带 wrapper）

```bash
./gradlew assembleDebug         # 构建 debug APK
./gradlew :app:installDebug     # 装到设备
./gradlew test                  # 单元测试
./gradlew connectedAndroidTest  # 仪器测试，含 Hilt 烟雾（需模拟器/真机）
```

> **测试框架现状**（v0.1）：`test/` 与 `androidTest/` 源集当前均使用 JUnit 4（`junit:4.13.2`）。Spec §2.3 规划 JVM 单测迁移到 JUnit 5（`junit-jupiter` 已在 Version Catalog 预声明），将在 Plan 02 启动时连同 `useJUnitPlatform()` 配置一并落地。`androidTest/` 因 `HiltAndroidRule` / Espresso 依赖 JUnit 4 Rule API，永久保留 JUnit 4。

## 开发文档

- 设计文档：`docs/specs/`
- 实施计划：`docs/superpowers/plans/`

## 状态

🚧 v0.1 · 项目骨架（开发中）

## 协议

Apache 2.0（待补 LICENSE 文件）
