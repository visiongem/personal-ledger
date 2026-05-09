# Plan 02 · Foundation Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `core-utils` 与 `core-extensions` 两个 starter kit 模块从占位符状态，实现为完整可用的"通用工具+扩展函数"集合，并附带启用 JUnit 5 + 清理 Plan 01 final review 的遗留项。完成后这两个模块可作为独立 Maven 包发布。

**Architecture:** 按 spec §2.1 设计意图，落地 `core-utils` 与 `core-extensions` 模块的初始内容，聚焦纯技术能力（数学/格式化/UI 操作/Android 基础扩展），不引入任何业务专属类与领域术语。每个工具采用 TDD：先写 JUnit 5 测试、再实现源、最后跑通。

**Tech Stack:** 同 Plan 01。新启用：JUnit 5 (Jupiter) + Truth + MockK 单测。

**Spec 参考：** `docs/specs/2026-05-09-personal-ledger-design.md` §2、§6.1（starter kit 模块清单）

**Plan 01 final review 遗留项一并处理**：
- I-1 JUnit 5 wiring（本 Plan T1）
- M-2 catalog 未用 alias 决断（本 Plan T2）
- 依赖体检（Compose BOM/lifecycle/nav3 是否升）（本 Plan T2）
- nav3 → core-ui 收敛风险面（本 Plan **不做**，留 Plan 04 一并处理）

---

## File Structure（本 Plan 创建/修改的所有文件）

### 入口清理（T1–T2）

```
build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt    # T1 修改：注入 useJUnitPlatform
build-logic/src/main/kotlin/KotlinJvmConventionPlugin.kt          # T1 修改：注入 useJUnitPlatform
gradle/libs.versions.toml                                          # T1 + T2 修改：测试 alias 切换 + 清理
[6 个 core-* + 4 个 feature-*]/build.gradle.kts                    # T1 修改：testImplementation 切到 junit-jupiter
```

### core-utils 实现（T3–T6）

```
core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/
├── BigDecimalUtil.kt           # T3 BigDecimal 安全运算 + 舍入 + 格式化
├── NumberUtil.kt               # T4 百分比格式化 + 数值比较
├── LargeNumberFormatUtil.kt    # T4 数量级换算（K/M/G/T/P）
├── ClickUtils.kt               # T5 防快速点击 View 扩展
├── KeyBoardUtils.kt            # T5 软键盘开关
├── SpanUtil.kt                 # T6 SpannableString helper
└── FileUtil.kt                 # T6 文件读写 + 保存到相册

core-utils/src/test/kotlin/io/github/visiongem/ledger/core/utils/
├── BigDecimalUtilTest.kt       # T3
├── NumberUtilTest.kt           # T4
├── LargeNumberFormatUtilTest.kt # T4
└── (UI helpers 不写 JVM 测试，只验证编译)

# 删除：
core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/Placeholder.kt   # T6 末尾删除
```

### core-extensions 实现（T7–T9）

```
core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/
├── Extension.kt                # T7 Context/Activity/Fragment/View/Intent/Bitmap 通用扩展
├── TextExtension.kt            # T8 SpannableString/SpannableStringBuilder 操作
├── ImageExtension.kt           # T8 Coil 3 加载封装
├── ComposeExtension.kt         # T9 Compose 生命周期/键盘高度等 effect
└── ModifierExtension.kt        # T9 Modifier 扩展（unreadDot/dashedBorder 等）

# 删除：
core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/Placeholder.kt   # T9 末尾删除
```

### 验证（T10）

无新文件，仅跑 `./gradlew clean assembleDebug test`。

---

## 通用实现规则（每个 task 都遵循）

1. **包名规范**：`io.github.visiongem.ledger.core.utils.*` / `io.github.visiongem.ledger.core.extensions.*`
2. **不引入业务专属依赖**：所有工具/扩展只能依赖 Kotlin 标准库、Android Framework API、AndroidX 标准组件、Compose 标准库、Coil 3。**禁止**引入只在某个具体业务里有意义的类、字段、文案。
3. **保留：纯 Kotlin/Android 标准库 API、Coil 3、Compose 标准库**
4. **注释**：写算法/边界条件说明；不写业务领域注释
5. **测试**：每个工具类写 JUnit 5 + Truth 单测；UI 类（依赖 Android Framework）跳过 JVM 单测，只验证编译；测试用例数量取舍见各 task

---

## Task 1：启用 JUnit 5 + 切换所有模块测试依赖

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt`
- Modify: `build-logic/src/main/kotlin/KotlinJvmConventionPlugin.kt`
- Modify: `core-utils/build.gradle.kts`
- Modify: `core-extensions/build.gradle.kts`
- Modify: `core-base/build.gradle.kts`
- Modify: `core-network/build.gradle.kts`
- Modify: `core-data/build.gradle.kts`
- Modify: `feature-record/build.gradle.kts`
- Modify: `feature-account/build.gradle.kts`
- Modify: `feature-stats/build.gradle.kts`
- Modify: `feature-settings/build.gradle.kts`

> Note: `core-ui` 和 `app` 不动 —— `app` 用 JUnit 4（保留 Hilt androidTest 兼容），`core-ui` 当前没有 testImplementation。

- [ ] **Step 1: 在 catalog 添加 jupiter 引擎并新增 bundle**

读 `gradle/libs.versions.toml`。在 `[libraries]` 已有 `junit-jupiter-api` 和 `junit-jupiter-engine` 之后追加：

```toml
junit-jupiter-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }
```

并在文件末尾追加 `[bundles]` 段（如不存在则新建）：

```toml
[bundles]
junit5 = ["junit-jupiter-api", "junit-jupiter-params"]
```

- [ ] **Step 2: 修改两个 convention plugin 启用 JUnit Platform**

在 `build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt` 的 `apply` 方法末尾追加（在 `extensions.configure<KotlinAndroidProjectExtension>` 之后）：

```kotlin
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    useJUnitPlatform()
}
```

同样修改 `KotlinJvmConventionPlugin.kt`：在 `extensions.configure<KotlinJvmProjectExtension>` 之后追加同一段。

> 不要忘了引入 `import org.gradle.api.tasks.testing.Test`（如已自动导入则跳过）

- [ ] **Step 3: 切换 9 个模块的 testImplementation**

对 `core-utils / core-extensions / core-base / core-network / core-data / feature-record / feature-account / feature-stats / feature-settings` 共 9 个 `build.gradle.kts`：

将所有 `testImplementation(libs.junit)` 行替换为：

```kotlin
testImplementation(libs.bundles.junit5)
testRuntimeOnly(libs.junit.jupiter.engine)
```

> Hint：`grep -rn "testImplementation(libs.junit)" core-* feature-*` 定位精确行。

- [ ] **Step 4: 验证编译**

Run: `./gradlew assembleDebug` (timeout 600000ms)
Expected: BUILD SUCCESSFUL.

> 没有测试代码，但 `assembleDebug` 不跑 test，主要验证 build 配置无语法错。

- [ ] **Step 5: 写一个最小 JUnit 5 自检测试**

`core-utils/src/test/kotlin/io/github/visiongem/ledger/core/utils/JUnit5SanityTest.kt`：

```kotlin
package io.github.visiongem.ledger.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class JUnit5SanityTest {
    @Test
    fun jupiterPlatformWorks() {
        assertThat(1 + 1).isEqualTo(2)
    }
}
```

Run: `./gradlew :core-utils:test`
Expected: 1 test passes via JUnit Platform.

> 跑通则证明：catalog 引擎引入正确、convention plugin `useJUnitPlatform()` 生效、Truth 工作。

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt build-logic/src/main/kotlin/KotlinJvmConventionPlugin.kt core-*/build.gradle.kts feature-*/build.gradle.kts core-utils/src/test/
git commit -m "build: wire JUnit 5 (Jupiter) for all library/feature modules"
```

---

## Task 2：Catalog 清理 + 依赖体检

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: 删除未用 alias**

读 `gradle/libs.versions.toml`，删除以下未在任何 module 引用的 alias（用 `grep -rn "libs.<alias>"` 验证后再删）：

- `android-appcompat`（spec §2.3 是纯 Compose，不用 appcompat）
- `appcompat` 版本号同时删除
- `datastore-preferences`（spec §3.2 用 Protobuf DataStore，不用 preferences）

> 保留：`coil-network-okhttp`、`mockk-android` —— 后续 Plan 引入 Coil/instrumentation MockK 时会用。

- [ ] **Step 2: 依赖体检（不实际升级，仅记录）**

不修改 catalog 任何版本号；在 `gradle/libs.versions.toml` 的 `[versions]` 段顶部追加注释：

```toml
# Last dep audit: 2026-05-09 (Plan 02 entry)
# Reviewed targets:
#   - Compose BOM 2024.10.01 → keep（latest stable BOM as of audit date; Plan 04 重审）
#   - Lifecycle 2.8.7 → keep（与 AGP 8.9.2 兼容良好）
#   - Navigation 3 1.0.0-alpha02 → keep（无新 alpha 可升；Plan 04 评估抽到 core-ui）
#   - 其他依赖均无紧急升级理由
```

- [ ] **Step 3: 验证编译仍通过**

Run: `./gradlew assembleDebug` (timeout 300000ms)
Expected: BUILD SUCCESSFUL（不应受影响 —— 删的都是未用 alias）

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build(catalog): drop unused appcompat/datastore-preferences aliases, log dep audit"
```

---

## Task 3：实现 BigDecimalUtil（核心数学工具）

**Files:**
- Create: `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/BigDecimalUtil.kt`
- Create: `core-utils/src/test/kotlin/io/github/visiongem/ledger/core/utils/BigDecimalUtilTest.kt`

**TDD 顺序：先写测试，再实现源**

- [ ] **Step 1: 写失败测试**

`BigDecimalUtilTest.kt` 内容（覆盖关键 API：加减乘除、舍入、格式化、零值、负数）：

```kotlin
package io.github.visiongem.ledger.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BigDecimalUtilTest {

    // ===== 加法 =====
    @Test fun addBasic() = assertThat("1.1".add("2.2")).isEqualTo("3.3")
    @Test fun addNullLeft() = assertThat((null as String?).add("1.5")).isEqualTo("1.5")
    @Test fun addNullRight() = assertThat("1.5".add(null)).isEqualTo("1.5")
    @Test fun addBothNull() = assertThat((null as String?).add(null)).isEqualTo("0")
    @Test fun addEmptyTreatedAsZero() = assertThat("".add("3.14")).isEqualTo("3.14")

    // ===== 减法 =====
    @Test fun subBasic() = assertThat("5.5".sub("2.2")).isEqualTo("3.3")
    @Test fun subNegativeResult() = assertThat("1.0".sub("3.0")).isEqualTo("-2.0")

    // ===== 乘法 =====
    @Test fun mulBasic() = assertThat("2.5".mul("4")).isEqualTo("10.0")
    @Test fun mulZero() = assertThat("3.14".mul("0")).isEqualTo("0.00")

    // ===== 除法 =====
    @Test fun divBasic() = assertThat("10".div("4", 2)).isEqualTo("2.50")
    @Test fun divPrecision() = assertThat("1".div("3", 6)).isEqualTo("0.333333")
    @Test fun divByZero() = assertThat("5".div("0", 2)).isEqualTo("0")

    // ===== 舍入 =====
    @Test fun roundDown() = assertThat("1.999".roundDown(2)).isEqualTo("1.99")
    @Test fun roundHalfUp() = assertThat("1.235".roundHalfUp(2)).isEqualTo("1.24")
    @Test fun roundHalfUpFiveDown() = assertThat("1.234".roundHalfUp(2)).isEqualTo("1.23")

    // ===== 去末尾零 =====
    @Test fun trimZeroSimple() = assertThat("1.2300".trimZero()).isEqualTo("1.23")
    @Test fun trimZeroAllZeros() = assertThat("1.000".trimZero()).isEqualTo("1")
    @Test fun trimZeroIntegerUnchanged() = assertThat("100".trimZero()).isEqualTo("100")

    // ===== 比较 =====
    @Test fun gtTrue() = assertThat("3.14".gt("3.13")).isTrue()
    @Test fun gtFalse() = assertThat("3.14".gt("3.15")).isFalse()
    @Test fun gtEqual() = assertThat("3.14".gt("3.14")).isFalse()  // 严格大于
    @Test fun ltTrue() = assertThat("1".lt("2")).isTrue()
}
```

> 共 ~22 个 test。失败原因预期：所有扩展函数都未定义。

Run: `./gradlew :core-utils:test --tests "io.github.visiongem.ledger.core.utils.BigDecimalUtilTest"`
Expected: 编译失败 / Unresolved reference for `add/sub/mul/div/...`

- [ ] **Step 2: 实现 BigDecimalUtil**

新建 `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/BigDecimalUtil.kt`，要求：

1. 包名：`package io.github.visiongem.ledger.core.utils`
2. 仅依赖 `java.math.BigDecimal` + `java.math.RoundingMode`（不引入第三方）
3. 提供以下 String 扩展函数族（签名以测试为准）：
   - `add(other: String?): String` — null/空安全，null 视作 0
   - `sub(other: String?): String`
   - `mul(other: String?): String`
   - `div(other: String?, scale: Int): String` — 除零返回 `"0"`，scale 控制小数位
   - `roundDown(scale: Int): String`
   - `roundHalfUp(scale: Int): String`
   - `trimZero(): String` — 去末尾零；纯整数不动
   - `gt / lt / ge / le / eq(other: String?): Boolean`
4. 必要的辅助：`safeBigDecimal(s: String?): BigDecimal`（null/空/解析失败 → `BigDecimal.ZERO`）
5. 注释：写算法/边界条件说明；不写业务领域注释

- [ ] **Step 3: 跑测试验证**

Run: `./gradlew :core-utils:test --tests "io.github.visiongem.ledger.core.utils.BigDecimalUtilTest"`
Expected: 22+ tests pass.

如果有 fail：
- 优先修复 BigDecimalUtil 实现
- 不要降低测试期望（除非测试本身有 typo）

- [ ] **Step 4: Commit**

```bash
git add core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/BigDecimalUtil.kt core-utils/src/test/kotlin/io/github/visiongem/ledger/core/utils/BigDecimalUtilTest.kt
git commit -m "feat(core-utils): add BigDecimalUtil with JUnit 5 test coverage"
```

---

## Task 4：实现 NumberUtil + LargeNumberFormatUtil

**Files:**
- Create: `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/NumberUtil.kt`
- Create: `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/LargeNumberFormatUtil.kt`
- Create: `core-utils/src/test/kotlin/io/github/visiongem/ledger/core/utils/NumberUtilTest.kt`
- Create: `core-utils/src/test/kotlin/io/github/visiongem/ledger/core/utils/LargeNumberFormatUtilTest.kt`

- [ ] **Step 1: 写 NumberUtilTest**

```kotlin
package io.github.visiongem.ledger.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class NumberUtilTest {
    @Test fun toPercentBasic() = assertThat("0.123".toPercent(2)).isEqualTo("12.30")
    @Test fun toPercentZero() = assertThat("0".toPercent(2)).isEqualTo("0.00")
    @Test fun toPercentTrimZero() = assertThat("0.5".toPercent(2, trimZero = true)).isEqualTo("50")

    @Test fun compareEqual() = assertThat("1.0".compareWith("1.00")).isEqualTo(0)
    @Test fun compareGreater() = assertThat("2.0".compareWith("1.0")).isGreaterThan(0)
    @Test fun compareLess() = assertThat("1.0".compareWith("2.0")).isLessThan(0)
}
```

- [ ] **Step 2: 写 LargeNumberFormatUtilTest**

```kotlin
package io.github.visiongem.ledger.core.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class LargeNumberFormatUtilTest {
    // 输入数字 → 自动换算单位（K/M/G/T/P）
    @Test fun belowK() = assertThat(LargeNumberFormatUtil.format("999")).isEqualTo("999")
    @Test fun toK() = assertThat(LargeNumberFormatUtil.format("1500")).isEqualTo("1.5K")
    @Test fun toM() = assertThat(LargeNumberFormatUtil.format("1500000")).isEqualTo("1.5M")
    @Test fun toG() = assertThat(LargeNumberFormatUtil.format("1500000000")).isEqualTo("1.5G")
    @Test fun toT() = assertThat(LargeNumberFormatUtil.format("1500000000000")).isEqualTo("1.5T")
    @Test fun toP() = assertThat(LargeNumberFormatUtil.format("1500000000000000")).isEqualTo("1.5P")

    @Test fun zero() = assertThat(LargeNumberFormatUtil.format("0")).isEqualTo("0")
    @Test fun negative() = assertThat(LargeNumberFormatUtil.format("-1500")).isEqualTo("-1.5K")
}
```

- [ ] **Step 3: 跑测试看失败**

Run: `./gradlew :core-utils:test --tests "*NumberUtilTest" --tests "*LargeNumberFormatUtilTest"`
Expected: 测试编译失败（类不存在）。

- [ ] **Step 4: 实现 NumberUtil**

新建 `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/NumberUtil.kt`，要求：

1. 包名 `io.github.visiongem.ledger.core.utils`
2. 提供：
   - `String.toPercent(scale: Int, trimZero: Boolean = false): String` — 数值 × 100，保留 scale 位小数；`trimZero=true` 时去掉末尾零
   - `String.compareWith(other: String): Int` — 用 BigDecimal 安全比较，返回 -1/0/1
3. null/空安全：null 视作 0

- [ ] **Step 5: 实现 LargeNumberFormatUtil**

新建 `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/LargeNumberFormatUtil.kt`，要求：

1. 包名 `io.github.visiongem.ledger.core.utils`
2. `object LargeNumberFormatUtil { fun format(value: String): String }`
3. 单位阶梯：每 1000 一档，K / M / G / T / P
4. 格式：保留 1 位小数，整百整千不显示 `.0`（`"1500" → "1.5K"`，`"1000" → "1K"`）
5. 负数支持：保留负号
6. 零返回 `"0"`
7. 不带任何业务后缀（如 `H/s` 等）—— 这是纯数量级换算工具

- [ ] **Step 6: 跑测试**

Run: `./gradlew :core-utils:test --tests "*NumberUtilTest" --tests "*LargeNumberFormatUtilTest"`
Expected: 全部通过。

- [ ] **Step 7: Commit**

```bash
git add core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/NumberUtil.kt core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/LargeNumberFormatUtil.kt core-utils/src/test/
git commit -m "feat(core-utils): add NumberUtil and LargeNumberFormatUtil"
```

---

## Task 5：实现 ClickUtils + KeyBoardUtils（UI 操作工具）

**Files:**
- Create: `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/ClickUtils.kt`
- Create: `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/KeyBoardUtils.kt`

> Note: 这两个文件涉及 Android Framework API（View、Context、InputMethodManager），**不写 JVM 单测**（需 Robolectric 才能跑），仅验证编译。

- [ ] **Step 1: 实现 ClickUtils**

新建 `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/ClickUtils.kt`，要求：

1. 包名 `io.github.visiongem.ledger.core.utils`
2. 提供 View 扩展 `View.setOnSingleClickListener(intervalMs: Long = 500L, block: (View) -> Unit)`：在指定毫秒内连续点击只触发一次
3. 实现思路：在 View 的 `tag` / `setTag(R.id.xxx, ...)` 上挂上次点击时间戳；当前点击时间 - 上次 < interval 直接 return
4. 可选：包级单例 `OnClickListener` 复用（避免每次创建新实例）

- [ ] **Step 2: 实现 KeyBoardUtils**

新建 `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/KeyBoardUtils.kt`，要求：

1. 包名同上
2. 提供：
   - `View.showKeyBoard()` — `requestFocus()` + `InputMethodManager.showSoftInput`
   - `View.hideKeyBoard()` — `InputMethodManager.hideSoftInputFromWindow`
   - `Activity.toggleKeyBoard()` — 切换显示/隐藏

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core-utils:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/ClickUtils.kt core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/KeyBoardUtils.kt
git commit -m "feat(core-utils): add ClickUtils and KeyBoardUtils"
```

---

## Task 6：实现 SpanUtil + FileUtil + 删除 Placeholder

**Files:**
- Create: `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/SpanUtil.kt`
- Create: `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/FileUtil.kt`
- Delete: `core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/Placeholder.kt`

- [ ] **Step 1: 实现 SpanUtil**

新建文件，要求：

1. 包名 `io.github.visiongem.ledger.core.utils`
2. 提供 SpannableStringBuilder helper，常见诉求：
   - 给字符串某段染色：`buildColored(text: String, ranges: List<Pair<IntRange, Int>>): SpannableStringBuilder`
   - 给某段加点击：`buildClickable(text: String, range: IntRange, onClick: () -> Unit): SpannableStringBuilder`
   - 给某段插入图标：`appendImage(drawableRes: Int, context: Context): SpannableStringBuilder`（Plan 04 之前可以暂用 ImageSpan，业务图标资源由调用方传 res id）
3. 不引入项目专属的自定义 Span 类；如果后续需要再补

- [ ] **Step 2: 实现 FileUtil**

新建文件，要求：

1. 包名同上
2. 提供：
   - 文件读：`File.readTextSafely(): String?`（IO 异常 → null）
   - 文件写：`File.writeTextSafely(content: String): Boolean`
   - 保存图片到相册：`Context.saveImageToGallery(bitmap: Bitmap, displayName: String): Uri?` —— 用 MediaStore（API 29+ 推荐路径）
   - Uri → File path：`Context.uriToFile(uri: Uri): File?`
3. 异常处理：用标准 `IOException` / `IllegalStateException`；**不引入业务异常类**

- [ ] **Step 3: 删除 Placeholder.kt**

`core-utils` 现在已经有 7+ 个真实类，删除 Placeholder：

```bash
rm core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/Placeholder.kt
```

- [ ] **Step 4: 验证编译 + 跑全部 core-utils 测试**

Run: `./gradlew :core-utils:assembleDebug :core-utils:test`
Expected: BUILD SUCCESSFUL，所有已写测试通过。

- [ ] **Step 5: Commit**

```bash
git add core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/SpanUtil.kt core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/FileUtil.kt
git rm core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/Placeholder.kt
git commit -m "feat(core-utils): add SpanUtil/FileUtil and remove placeholder"
```

---

## Task 7：实现 Extension.kt（最大的扩展集）

**Files:**
- Create: `core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/Extension.kt`

> 这个文件承载量最大：覆盖 Context、Activity、Fragment、View、Intent、Bitmap、Color、dp/sp/px、Toast、Flow、SharedPreferences 等大量小扩展。

- [ ] **Step 1: 列出本文件要提供的扩展清单**

按以下分类设计：

- **尺寸**：`Int.dp` / `Float.dp` / `Int.sp` / `Float.sp` / `Int.px` —— 用 `Resources.getSystem().displayMetrics`
- **View 显隐**：`View.gone()` / `View.visible()` / `View.invisible()` / `View.toggleVisible(condition: Boolean)`
- **Context 派生**：`Context.toast(msg: String, long: Boolean = false)` / `Context.color(@ColorRes id: Int)` / `Context.string(@StringRes id: Int, vararg args: Any)` / `Context.dimen(@DimenRes id: Int)`
- **Activity / Fragment**：`Fragment.requireContext()` 已是 AndroidX；提供 `Fragment.activityViewModelOwner` 之类；`Activity.startActivity<T>()` reified inline
- **Bitmap**：`Bitmap.toFile(file: File): Boolean`
- **Color**：`@ColorInt Int.toHexString(): String` / `String.toColorIntOrNull(): Int?`
- **Flow**：`Flow<T>.collectIn(scope: CoroutineScope, action: suspend (T) -> Unit)`（替代每次写 `scope.launch { collect ... }`）
- **Intent**：`Intent.putExtraSafe(key, value)`（避免对类型重载混淆）

> 业务相关的扩展（如登录跳转、特定页面跳转、用户态判断）**不在本文件**。这是通用扩展模块。

- [ ] **Step 2: 实现 Extension.kt**

按 Step 1 清单逐个实现。每个函数 5-15 行，整文件预估 200-400 行。注意：

1. 包名 `io.github.visiongem.ledger.core.extensions`
2. 所有函数必须只依赖 Android Framework / AndroidX / Kotlin 标准库
3. `Context.toast` 内部用 `Toast.makeText(this, msg, ...).show()`，不调用其他工具类
4. 涉及主题色的扩展：接收 `@ColorInt color: Int` 参数让调用方传入；TODO 注释说明 Plan 04 主题落地后可考虑提供默认值

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core-extensions:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/Extension.kt
git commit -m "feat(core-extensions): add Extension.kt with general-purpose Android extensions"
```

---

## Task 8：实现 TextExtension + ImageExtension

**Files:**
- Create: `core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/TextExtension.kt`
- Create: `core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/ImageExtension.kt`

- [ ] **Step 1: 实现 TextExtension**

新建文件，要求：

1. 包名 `io.github.visiongem.ledger.core.extensions`
2. 提供：
   - `String.toSpannable(): SpannableStringBuilder`
   - `SpannableStringBuilder.colorRange(range: IntRange, @ColorInt color: Int): SpannableStringBuilder` —— 链式
   - `SpannableStringBuilder.boldRange(range: IntRange): SpannableStringBuilder`
   - `SpannableStringBuilder.clickRange(range: IntRange, onClick: (View) -> Unit): SpannableStringBuilder`
   - `TextView.setSpannedText(builder: SpannableStringBuilder)` —— 自动设 `movementMethod = LinkMovementMethod.getInstance()`
3. 不依赖业务字体类（如有自定义 FontFamily 的需求，调用方自行用 `setTypeface`）

- [ ] **Step 2: 实现 ImageExtension**

新建文件，要求：

1. 包名同上
2. 用 Coil 3 实现：
   - `ImageView.load(url: String?, @DrawableRes placeholder: Int = 0, @DrawableRes error: Int = 0)`
   - `@Composable AsyncImage(model: Any?, contentDescription: String?)` 的二次封装函数（如果有意义；否则跳过）
3. 不引入额外加载库

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core-extensions:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/TextExtension.kt core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/ImageExtension.kt
git commit -m "feat(core-extensions): add TextExtension and ImageExtension"
```

---

## Task 9：实现 ComposeExtension + ModifierExtension + 删除 Placeholder

**Files:**
- Create: `core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/ComposeExtension.kt`
- Create: `core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/ModifierExtension.kt`
- Delete: `core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/Placeholder.kt`

- [ ] **Step 1: 实现 ComposeExtension**

新建文件，要求：

1. 包名 `io.github.visiongem.ledger.core.extensions`
2. 提供：
   - `@Composable fun keyboardAsState(): State<Boolean>` —— 监听 IME 高度判定键盘是否打开
   - `@Composable fun <T> Flow<T>.collectAsStateWithLifecycle(initial: T): State<T>`（如果 androidx.lifecycle:lifecycle-runtime-compose 已引入则直接用，避免重复造轮子；本扩展只补缺）
   - `@Composable fun OnLifecycleEvent(onEvent: (Lifecycle.Event) -> Unit)` —— 简化 DisposableEffect 写法
3. 仅依赖 Compose runtime + foundation + lifecycle-runtime-compose

- [ ] **Step 2: 实现 ModifierExtension**

新建文件，要求：

1. 包名同上
2. 提供：
   - `Modifier.unreadDot(visible: Boolean, @ColorInt color: Int = Color.RED, sizeDp: Float = 6f): Modifier` —— 右上角小红点
   - `Modifier.dashedBorder(strokeWidthDp: Float, dashLengthDp: Float, gapLengthDp: Float, @ColorInt color: Int): Modifier`
   - `Modifier.shadowed(elevationDp: Float, shape: Shape = RectangleShape, @ColorInt shadowColor: Int = Color.BLACK): Modifier`
3. 涉及颜色处的参数都让调用方传入；不在本扩展内引用任何主题资源

- [ ] **Step 3: 删除 Placeholder.kt**

```bash
rm core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/Placeholder.kt
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew :core-extensions:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/ComposeExtension.kt core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/ModifierExtension.kt
git rm core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/Placeholder.kt
git commit -m "feat(core-extensions): add Compose/Modifier extensions and remove placeholder"
```

---

## Task 10：最终验证

**Files:** 无新文件

- [ ] **Step 1: clean + 全量构建 + 全部测试**

Run: `./gradlew clean assembleDebug test` (timeout 600000ms)
Expected: BUILD SUCCESSFUL，所有已写测试通过。

- [ ] **Step 2: 验证 Plan 02 提交完整**

Run: `git log --oneline | head -15`

Expected output（约 9–10 个新 commit，从 Plan 01 收尾后开始）：
```
<sha> feat(core-extensions): add Compose/Modifier extensions and remove placeholder
<sha> feat(core-extensions): add TextExtension and ImageExtension
<sha> feat(core-extensions): add Extension.kt with general-purpose Android extensions
<sha> feat(core-utils): add SpanUtil/FileUtil and remove placeholder
<sha> feat(core-utils): add ClickUtils and KeyBoardUtils
<sha> feat(core-utils): add NumberUtil and LargeNumberFormatUtil
<sha> feat(core-utils): add BigDecimalUtil with JUnit 5 test coverage
<sha> build(catalog): drop unused appcompat/datastore-preferences aliases, log dep audit
<sha> build: wire JUnit 5 (Jupiter) for all library/feature modules
... (Plan 01 commits follow)
```

- [ ] **Step 3: 完成清单核对**

`core-utils/src/main/kotlin/io/github/visiongem/ledger/core/utils/` 下应包含：
- BigDecimalUtil.kt
- NumberUtil.kt
- LargeNumberFormatUtil.kt
- ClickUtils.kt
- KeyBoardUtils.kt
- SpanUtil.kt
- FileUtil.kt
- 无 Placeholder.kt

`core-extensions/src/main/kotlin/io/github/visiongem/ledger/core/extensions/` 下应包含：
- Extension.kt
- TextExtension.kt
- ImageExtension.kt
- ComposeExtension.kt
- ModifierExtension.kt
- 无 Placeholder.kt

`core-utils/src/test/kotlin/io/github/visiongem/ledger/core/utils/` 下应包含：
- JUnit5SanityTest.kt
- BigDecimalUtilTest.kt
- NumberUtilTest.kt
- LargeNumberFormatUtilTest.kt

- [ ] **Step 4: APK 装机自测可选**

如果有设备：
```bash
./gradlew :app:installDebug
```
启动 App，确认 "Hello, Ledger" 仍正常显示（本 Plan 不应破坏 app 功能，因为 app 还没用 core-utils/extensions 的代码）。

不强制；本 Plan 不修改 UI。

---

## 完成标准（Definition of Done）

- [x] JUnit 5 在 9 个 library 模块跑得起来
- [x] catalog 已清理未用 alias 并记录依赖体检结果
- [x] core-utils 7 个工具类落地完毕
- [x] core-utils 至少 3 个工具类（BigDecimal/Number/LargeNumberFormat）有 JUnit 5 单测
- [x] core-extensions 5 个扩展文件落地完毕
- [x] 两个模块的 Placeholder.kt 已删除
- [x] `./gradlew clean assembleDebug test` 全部通过
- [x] 9–10 个 commit，每个独立可 revert

---

## 已知限制（留给 Plan 03+）

- `core-base` 未实现（含 BaseViewModel.launchCatching 系列）→ Plan 03
- `core-network` 未实现（NetRequestManager + ApiResponse）→ Plan 03
- `core-ui` 未实现（Theme + Compose 组件）→ Plan 04
- ⚠️ `core-extensions` 中部分 Modifier 扩展依赖项目主题色，目前用接收参数兜底，等 Plan 04 主题落地后回填默认值
- ClickUtils / KeyBoardUtils / FileUtil / SpanUtil 没有 JVM 单测（需 Robolectric），如果 Plan 03+ 想加，可在 androidTest 跑

---

## Plan 02 实施时的"在飞修复"预案

参考 Plan 01 经验，遇到这几类情况按下面处理：

| 情形 | 处理 |
|------|------|
| 实现里写到一半发现需要某个工具类（暂时没落地） | 先写最小 stub 在文件内 private fun 里，保证当前类自洽；在 Plan 03+ 把 stub 抽出去 |
| 用了已废弃 API（如 `Handler()` 无参构造） | 直接用现行推荐 API（`Handler(Looper.getMainLooper())`） |
| 实现引用项目主题色 | 改成接受 `color: Color` 或 `@ColorInt Int` 参数；标 TODO 等 Plan 04 |
| 测试期望与实现行为不匹配 | 先确认期望是否正确（数学/边界条件常被低估）；期望对则修实现，期望错则修测试 |
| 跑测时 BUILD FAILED 但 test 实际有跑 | 看 fail 数量；可能是 `useJUnitPlatform()` 没生效（检查 convention plugin） |

执行 Plan 02 时建议：每个 task 后由 spec reviewer + code reviewer 双层审，与 Plan 01 节奏相同。
