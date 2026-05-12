---
description: 用中文解释指定的代码或概念，结合本项目的架构与避雷指南
---

用中文解释以下代码或概念。**必须结合项目上下文**（参考 AGENTS.md 中的架构约定、`.claude/rules/architecture.md`、避雷指南）：

$ARGUMENTS

解释格式：

1. **是什么**：用一句话说清楚
2. **怎么工作的**：拆解核心逻辑（涉及 UI 请指出是 Compose Screen 还是 Composable 组件；涉及数据请指出走的是 Repository / DAO / Worker 哪一层）
3. **为什么这样设计**：项目层面的考虑，例如：
   - MVVM + Hilt 解耦
   - `:core-*` 复用 vs `:feature-*` 隔离
   - FK RESTRICT/SET NULL 的级联选择
   - `CurrencyFormatter` 统一金额展示
   - Snackbar Undo 模式统一长按删除体验
4. **坑在哪里**：结合 AGENTS.md 第十章避雷指南，指出边界情况：
   - 浮点金额运算
   - FK RESTRICT 硬删账户
   - `@Parcelize` 路由
   - Scaffold inset 双重消化
   - WorkManager 双 initializer
   - OutlinedTextField 写 DataStore 卡顿
   - JVM-only 工具放错模块
   - 测试中未固定 Locale
