# Plan 03 · core-network + core-base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement task-by-task. TDD where feasible.

**Goal:** 把 starter kit 的网络层（`core-network`）与 ViewModel 基类层（`core-base`）从占位符落地到可用状态。完成后 v1 业务层（Plan 04+）可直接消费 `Retrofit`/`Hilt` provided 客户端，并继承 `BaseViewModel.launchCatching` 家族。

**Architecture:**

- `core-network` 围绕一个**典型 envelope 协议**（`{code, msg, data}`）构建。提供：
  - `ApiResponse<T>`：envelope 数据类
  - `ApiException`：业务错误（envelope.code != 0）；`NetworkException`：网络/解析错误
  - `ApiResponseCallAdapterFactory`：让 Retrofit suspend 函数直接返回 `T`（成功）或抛异常（失败）—— 自动解包 envelope
  - `ContentTypeInterceptor`：缺省 `Content-Type: application/json`，避免每个 API 重复声明
  - `NetRequestManager`：构建 `OkHttpClient` + `Retrofit` 的工厂；调试日志可开关
  - Hilt module 提供单例 `Retrofit`、`Moshi`
- `core-base` 提供一个最小 `BaseViewModel`：
  - `launchCatching(onError, block)`：捕获异常的 viewModelScope.launch
  - `launchOnIO(onError, block)`：固定 `Dispatchers.IO` 的版本
  - 所有错误回调默认通过一个 `errorState: StateFlow<Throwable?>` 广播，UI 层可观察

**Tech Stack 增量:** Retrofit 2.11 + Moshi 1.15 + OkHttp + KSP（已接）+ Hilt（已接）+ Turbine + coroutines-test + mockk（测试侧）。

**Spec 参考：** spec §2.3（网络/异步）、§6.1（starter kit 模块）

**与 Plan 02 的衔接：** core-extensions 的 `Flow.collectIn` 后续在 ViewModel 端会用上；`launchCatching` 的错误广播 + UI 端 `keyboardAsState` 等扩展共同构成"标准 ViewModel ↔ UI"耦合面。

---

## 范围决策（v1 不做）

- **`ApiResponsePagingSource`**：spec 未启用 Paging 3，跳过
- **`DevDns`**：仅在内网开发环境有意义，个人 app 不需要
- **`HashMapJsonAdapter`**：泛型 Map 解码 helper，本 v1 没有需求
- **多 Retrofit 实例支持**（envelope vs raw）：v1 只做 envelope；Frankfurter API 调用如需直接解码，后续 Plan 用第二个 Hilt 限定符提供 raw `Retrofit`

---

## File Structure

```
core-network/src/main/kotlin/io/github/visiongem/ledger/core/network/
├── ApiResponse.kt                    # T1
├── ApiException.kt                   # T1
├── ApiResponseCallAdapterFactory.kt  # T2
├── ContentTypeInterceptor.kt         # T3
├── NetRequestManager.kt              # T4
└── di/NetworkModule.kt               # T4 (Hilt @Module)

core-network/src/test/kotlin/io/github/visiongem/ledger/core/network/
├── ApiResponseTest.kt                # T1（Moshi 解码 + envelope 字段映射）
└── ApiResponseCallAdapterFactoryTest.kt  # T2（mockk Retrofit Call → 成功/业务失败/网络失败）

core-base/src/main/kotlin/io/github/visiongem/ledger/core/base/
└── BaseViewModel.kt                  # T5

core-base/src/test/kotlin/io/github/visiongem/ledger/core/base/
└── BaseViewModelTest.kt              # T5（Turbine + coroutines-test）

# 删除：
core-network/.../Placeholder.kt       # T4
core-base/.../Placeholder.kt          # T5
```

---

## 通用规则

1. **包名规范**：`io.github.visiongem.ledger.core.network.*` / `io.github.visiongem.ledger.core.base.*`
2. **不引入业务类**：所有 API 不绑定记账业务概念
3. **TDD**：纯逻辑（ApiResponse、CallAdapter、BaseViewModel）写 JUnit 5 + Truth；网络/拦截器集成验证靠编译 + 手工断言
4. **MockWebServer 不引入**：用 mockk 模拟 `retrofit2.Call` 行为，更轻
5. **Hilt @Module 不写测试**（配置代码）

---

## Task 1：ApiResponse + ApiException

**Files:**
- Create: `core-network/.../ApiResponse.kt`
- Create: `core-network/.../ApiException.kt`
- Create: `core-network/src/test/.../ApiResponseTest.kt`

要点：
- `data class ApiResponse<T>(val code: Int, val msg: String?, val data: T?)`，`@JsonClass(generateAdapter = true)` 让 Moshi codegen 处理
- `sealed class NetworkException : RuntimeException`：
  - `ApiException(val code: Int, override val message: String)` —— envelope code != 0
  - `IoFailure(cause: Throwable)` —— socket/timeout/4xx/5xx
  - `ParseFailure(cause: Throwable)` —— JSON 解析失败
- 默认 `successCode = 0`：留作常量 `ApiResponse.SUCCESS_CODE`，可被项目 override（构造时传，或 CallAdapter 接 successCode 参数）
- 测试：构造一个 Moshi 实例，对 `ApiResponse<String>` 序列化/反序列化，验证字段映射 + null data 处理

Commit: `feat(core-network): add ApiResponse envelope and exception hierarchy`

---

## Task 2：ApiResponseCallAdapterFactory

**Files:**
- Create: `core-network/.../ApiResponseCallAdapterFactory.kt`
- Create: `core-network/src/test/.../ApiResponseCallAdapterFactoryTest.kt`

要点：
- 实现 Retrofit `CallAdapter.Factory`
- 处理签名：Retrofit suspend `fun foo(): User` 由 Retrofit 内部 wrap 成 `Call<ApiResponse<User>>`，这里把 envelope unwrap 成 `User`
  - 实际做法：拦截 `suspend` 模式下的 `Call<T>`，包装为返回 `data` 或抛 `ApiException`
  - 参考 `Retrofit.coroutines.suspendCancellableCoroutine` 模式
- 业务码可配置（构造时传入 `successCode = 0`）
- 测试：mockk Retrofit `Call`，模拟成功/业务失败/IO 失败三种 response，断言 unwrap / throw 行为

Commit: `feat(core-network): add ApiResponseCallAdapterFactory unwrapping envelope`

---

## Task 3：ContentTypeInterceptor

**Files:**
- Create: `core-network/.../ContentTypeInterceptor.kt`

要点：
- OkHttp `Interceptor`：如果请求没有显式 `Content-Type`，加上 `application/json; charset=utf-8`
- 不写单测（一行逻辑）；T4 集成时验证

Commit: 与 T4 合并（小改动）

---

## Task 4：NetRequestManager + Hilt @Module + delete core-network Placeholder

**Files:**
- Create: `core-network/.../NetRequestManager.kt`
- Create: `core-network/.../di/NetworkModule.kt`
- Delete: `core-network/.../Placeholder.kt`

要点：
- `object NetRequestManager`（或注入式 `class`）：
  - `fun buildOkHttpClient(loggingEnabled: Boolean): OkHttpClient`
  - `fun buildMoshi(): Moshi`
  - `fun buildRetrofit(baseUrl: String, client: OkHttpClient, moshi: Moshi, successCode: Int = 0): Retrofit`
- Hilt `NetworkModule`：`@Provides @Singleton` 暴露 `Moshi`、`OkHttpClient`、`Retrofit`
  - `BASE_URL` 暂用占位 `"https://example.invalid/"`，等 v1 真实接入再改
  - logging 默认在 `BuildConfig.DEBUG` 开（暂硬编码 `true`，等 BuildConfig 接入后切换）
- 不写单测（配置代码）；编译通过 + Hilt 在 `:app` smoke test 仍跑通即可

Commit: `feat(core-network): add NetRequestManager + Hilt module, drop placeholder`

---

## Task 5：BaseViewModel + launchCatching + delete core-base Placeholder

**Files:**
- Create: `core-base/.../BaseViewModel.kt`
- Create: `core-base/src/test/.../BaseViewModelTest.kt`
- Delete: `core-base/.../Placeholder.kt`

要点：
- `abstract class BaseViewModel : ViewModel()`：
  - `protected val _errorFlow = MutableSharedFlow<Throwable>()`
  - `val errorFlow: SharedFlow<Throwable> = _errorFlow.asSharedFlow()`
  - `protected fun launchCatching(onError: (Throwable) -> Unit = ::onUnhandledError, block: suspend CoroutineScope.() -> Unit): Job`
  - `protected fun launchOnIO(onError: (Throwable) -> Unit = ::onUnhandledError, block: suspend CoroutineScope.() -> Unit): Job` —— `viewModelScope.launch(Dispatchers.IO)`
  - `protected open fun onUnhandledError(t: Throwable) { _errorFlow.tryEmit(t) }` —— 子类可 override
- 测试用 Turbine 验证 `errorFlow` 在 block 抛异常时收到对应 Throwable，正常 block 不发射

Commit: `feat(core-base): add BaseViewModel with launchCatching family, drop placeholder`

---

## Task 6：最终验证

- Run: `./gradlew clean assembleDebug test` (timeout 600000ms)
- Expected: BUILD SUCCESSFUL；T1/T2/T5 单测全部通过
- 检查 `core-network` 与 `core-base` 已无 `Placeholder.kt`
- 检查 `:app:test` 的 Hilt smoke test 仍 PASS（说明 NetworkModule 没把 DI graph 搞坏）

Commit: 无（验证 task）

---

## Definition of Done

- [x] core-network 6 个 Kt 文件 + 1 个 test 文件就位
- [x] core-base 1 个 Kt 文件 + 1 个 test 文件就位
- [x] 两个模块的 Placeholder.kt 已删除
- [x] `./gradlew clean assembleDebug test` 全部通过
- [x] 5 个 commit（T1, T2, T4, T5, 加 T3 折入 T4），每个独立可 revert

---

## 已知限制（留给 Plan 04+）

- 真实 BASE_URL 需要等 v1 业务对接（Frankfurter 直接走 raw Retrofit，第二个 Hilt 限定符）
- BuildConfig.DEBUG 还没接入 logging 开关——T4 暂硬编码 `true`
- envelope `successCode` 暂硬编码 `0`，多服务端协议不同时再做配置化
