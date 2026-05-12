---
description: 根据当前 git 暂存区（staged）的变更生成符合规范的 commit message
---

根据当前 git 暂存区的变更生成 1 条 commit message。

请先执行 `git diff --cached` 获取已暂存的变更内容。如果暂存区为空，请提示用户先 stage 变更。

要求：
- 格式：`type(scope): description`
- `type` 可选：
  - `feat`: 新功能
  - `fix`: 修复
  - `docs`: 文档
  - `style`: UI 样式调整，无逻辑变更
  - `refactor`: 重构，无新增功能且非修复
  - `perf`: 性能优化
  - `test`: 测试
  - `build`: 构建（Gradle、Catalog、CI 之外的构建配置）
  - `ci`: CI / CD（GitHub Actions、Workflow）
  - `chore`: 杂项、非业务代码
  - `revert`: 回滚
- `scope` 用模块短名（不带 `feature-` / `core-` 前缀）：`record`、`account`、`stats`、`settings`、`data`、`ui`、`utils`、`app`、`brand`、`rates`、`backup`
- `style` 仅指 UI 样式，不指代码格式
- description 用英文，第一句不超过 72 字符
- 多类变更时选最主要的一类

可选 body：
- 用空行与 subject 隔开
- 回答 what / why；不解释 how（diff 自己看）
- 不引用任务编号（T63 之类）；这些在对话里有，commit 里不重复

AI 协作的 commit 必须在 footer 加：
```
Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```

例：

```
feat(record): long-press to delete with Snackbar undo

Long-pressing a record now removes it from the database and surfaces an
Undo snackbar. The ViewModel re-upserts the original Record (which
preserves its id) when the action button is tapped, so balances and
sort order recover on the next observe emission.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
```
