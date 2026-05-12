# Kotlin 编码规范

- 行宽 120 字符（package / import 除外）
- 禁止匈牙利命名（`mName`、`s_name`）
- 禁止使用全限定类名，MUST import 后使用短名（build.gradle.kts 例外，因为 Gradle DSL 偶尔需要）
- 4 空格缩进，K&R 大括号

## 常量定义

- 仅本类用的：定义在 `companion object { private const val ... }` 中
- 多类共用：抽取到 `{Feature}Constants.kt`，使用 `object` 声明 + `const val`
- 禁止在代码中硬编码字符串常量 / magic number；ISO 4217 长度、超时毫秒等数值都要命名

```kotlin
// Bad
if (currency.length == 3) onChange(currency)

// Good
private const val ISO_LENGTH = 3
if (normalized.length == ISO_LENGTH) onChange(normalized)
```

## 注释规则

- **默认不写注释**。
- 仅当"为什么"非显而易见（隐藏约束、补丁、反直觉行为）时才加。
- 禁止解释"做了什么"（命名应自解释）。
- 禁止引用当前任务 / PR / Issue 号（它们会随时间烂掉）。
- 多行注释最多两行；超过两行说明命名 / 抽象需要重做。

```kotlin
// Bad — 解释做了什么
// Loop through accounts and find one matching id
val account = accounts.firstOrNull { it.id == accountId }

// Bad — 引用任务上下文
// Added for T67 to support last refresh time
override suspend fun setLastRateRefreshAt(instant: Instant) { ... }

// Good — 解释为什么
// FK record.categoryId → category.id is ON DELETE SET NULL, so records
// using this category become "no category" instead of throwing.
categoryRepository.deleteById(category.id)
```

## 控制流格式

- 多行 `if/else` 表达式（如作为参数或赋值）MUST 使用花括号并换行，禁止单行省略花括号
- `when` 分支：所有分支都返回值时用表达式形式；否则用语句形式

## 错误处理

- 业务异常用 `runCatching { ... }` 包裹；ViewModel 内用 `launchCatching { ... }`（在 `BaseViewModel` 中）。
- 禁止裸 `try { ... } catch (e: Throwable) { ... }`；必须指明捕获类型（如 `IOException`、`SQLiteConstraintException`）。
- 禁止 `e.printStackTrace()`；要么写日志要么向上抛。

## Null 处理

- 优先 `?.`、`?:` 链式表达；避免 `!!` 强转。
- 跨模块 smart cast 失败时改用 `let`：
  ```kotlin
  // Bad — smart cast fails across modules
  if (record.categoryId != null) {
      use(record.categoryId)
  }

  // Good
  record.categoryId?.let { categoryId ->
      use(categoryId)
  }
  ```

## Compose 规范

- 同级 Compose 函数调用之间用空行分隔（多个相邻 `Text` 例外）。
- `@Composable` 函数 PascalCase；私有辅助 Composable 也是 PascalCase。
- 状态尽量提升到 ViewModel；Composable 内 `remember { mutableStateOf(...) }` 仅用于纯 UI 临时状态（如对话框开关、本地 draft）。
- `Modifier` MUST 作为第一个可选参数（默认 `Modifier`），紧跟必传参数后。

## Flow / Coroutines

- 收集状态 MUST 用 `collectAsStateWithLifecycle()`；禁止 `collectAsState()`。
- VM 内 Flow → StateFlow 用 `.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = ...)`。
- `combine` 超过 5 个 flow 时拆分子 flow 后再 combine（Kotlin 标准库的 `combine` 最大参数数有限制）。
