// 根 build.gradle.kts —— 所有 plugin 在 build-logic 中通过 convention 加载，
// 这里只负责跨模块的清理任务和 Compose Compiler 配置（KGP 2.0+ 必需）
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
