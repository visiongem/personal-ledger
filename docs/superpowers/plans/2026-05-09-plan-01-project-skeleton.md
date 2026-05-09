# Plan 01 · 项目骨架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭起 Personal Ledger 多模块 Gradle 工程骨架，跑通 Hilt + Compose 的最小空壳 App，所有 module 占位齐备，可执行 `./gradlew assembleDebug` 成功并启动到 "Hello, Ledger" Compose 屏幕。

**Architecture:** Gradle 8.x 多模块 + Version Catalog 统一版本 + build-logic convention plugins 统一 module 配置 + Hilt 依赖注入 + 纯 Compose UI（无 ViewBinding）。本 Plan 不写业务代码，只产出可运行的脚手架。

**Tech Stack:** Gradle 8.10、AGP 8.7、Kotlin 2.0、Compose BOM 2024.10、Hilt 2.52、KSP 2.0.21-1.0.25、JUnit5、min SDK 26 / target SDK 36

**Spec 参考：** `docs/specs/2026-05-09-personal-ledger-design.md` §2（技术栈与模块结构）

---

## File Structure（本 Plan 创建的所有文件）

```
personal-ledger/
├── settings.gradle.kts                      # T1
├── build.gradle.kts                         # T1
├── gradle.properties                        # T1
├── .gitignore                               # T1
├── gradle/
│   ├── libs.versions.toml                   # T2
│   └── wrapper/                             # 由 gradle wrapper 命令生成
├── build-logic/
│   ├── settings.gradle.kts                  # T3
│   ├── build.gradle.kts                     # T3
│   └── src/main/kotlin/
│       ├── AndroidApplicationConventionPlugin.kt   # T3
│       ├── AndroidLibraryConventionPlugin.kt       # T3
│       ├── AndroidComposeConventionPlugin.kt       # T3
│       ├── AndroidHiltConventionPlugin.kt          # T3
│       └── KotlinJvmConventionPlugin.kt            # T3
├── app/
│   ├── build.gradle.kts                     # T4
│   ├── proguard-rules.pro                   # T4
│   └── src/main/
│       ├── AndroidManifest.xml              # T4
│       ├── kotlin/io/github/nia/ledger/
│       │   ├── LedgerApplication.kt         # T4
│       │   └── MainActivity.kt              # T4
│       └── res/
│           ├── values/strings.xml           # T4
│           ├── values/themes.xml            # T4
│           └── mipmap-anydpi-v26/           # 可选，T4
├── core-base/build.gradle.kts               # T5
├── core-utils/build.gradle.kts              # T5
├── core-extensions/build.gradle.kts         # T5
├── core-network/build.gradle.kts            # T5
├── core-ui/build.gradle.kts                 # T5
├── core-data/build.gradle.kts               # T5
├── feature-record/build.gradle.kts          # T6
├── feature-account/build.gradle.kts         # T6
├── feature-stats/build.gradle.kts           # T6
├── feature-settings/build.gradle.kts        # T6
├── app/src/test/kotlin/io/github/nia/ledger/HiltSmokeTest.kt   # T7
└── README.md                                # T8
```

> **包名占位**：本 Plan 全程使用 `io.github.nia.ledger`。GitHub username 确认后用 IDE 全局 refactor 替换 `io.github.nia` → `io.github.<username>`，5 分钟可完成。

---

## Task 1：Gradle Bootstrap

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `.gitignore`

- [ ] **Step 1: 创建 `.gitignore`**

```gitignore
# Gradle
.gradle/
build/
local.properties
captures/

# Android
*.iml
.idea/
*.apk
*.aab
*.dex
*.class

# Kotlin / Java
*.hprof

# OS
.DS_Store
Thumbs.db

# Brainstorming companion (临时文件)
.superpowers/

# Keys / secrets
keystore.properties
*.jks
*.keystore
```

- [ ] **Step 2: 创建 `gradle.properties`**

```properties
# JVM
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

# Android
android.useAndroidX=true
android.nonTransitiveRClass=true
android.nonFinalResIds=true

# Kotlin
kotlin.code.style=official

# KSP
ksp.incremental=true
```

- [ ] **Step 3: 创建 `settings.gradle.kts`**

```kotlin
@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "personal-ledger"

include(":app")
include(":core-base")
include(":core-utils")
include(":core-extensions")
include(":core-network")
include(":core-ui")
include(":core-data")
include(":feature-record")
include(":feature-account")
include(":feature-stats")
include(":feature-settings")
```

- [ ] **Step 4: 创建根 `build.gradle.kts`**

```kotlin
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
```

- [ ] **Step 5: 生成 Gradle wrapper（用本地 Gradle 8.10）**

Run: `cd /Users/nia/PersonalProjects/personal-ledger && gradle wrapper --gradle-version 8.10 --distribution-type all`
Expected: 生成 `gradlew`、`gradlew.bat`、`gradle/wrapper/`

> 如果系统没有 Gradle，先 `brew install gradle`。

- [ ] **Step 6: 提交**

```bash
cd /Users/nia/PersonalProjects/personal-ledger
git add .gitignore gradle.properties settings.gradle.kts build.gradle.kts gradle/wrapper/ gradlew gradlew.bat
git commit -m "chore: bootstrap Gradle 8.10 multi-module workspace"
```

---

## Task 2：Version Catalog (`libs.versions.toml`)

**Files:**
- Create: `gradle/libs.versions.toml`

- [ ] **Step 1: 创建版本目录**

```toml
[versions]
agp = "8.7.2"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.25"
coreKtx = "1.13.1"
appcompat = "1.7.0"
activity = "1.9.3"
lifecycle = "2.8.7"
composeBom = "2024.10.01"
material3 = "1.3.0"
navigation3 = "1.0.0-alpha02"  # 锁 alpha 版本，详见 spec §10 风险
hilt = "2.52"
hiltCompose = "1.2.0"
room = "2.6.1"
moshi = "1.15.1"
retrofit = "2.11.0"
okhttp = "4.12.0"
coil = "3.0.4"
coroutines = "1.9.0"
datastore = "1.1.1"
junit = "4.13.2"
junit5 = "5.11.3"
mockk = "1.13.13"
turbine = "1.2.0"
truth = "1.4.4"

[libraries]
android-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
android-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }
android-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity" }
android-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
android-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
android-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }

compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-foundation = { module = "androidx.compose.foundation:foundation" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "material3" }
compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }

navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "navigation3" }

hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltCompose" }

room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }

moshi = { module = "com.squareup.moshi:moshi", version.ref = "moshi" }
moshi-kotlin-codegen = { module = "com.squareup.moshi:moshi-kotlin-codegen", version.ref = "moshi" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-converter-moshi = { module = "com.squareup.retrofit2:converter-moshi", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-okhttp = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }

coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
datastore = { module = "androidx.datastore:datastore", version.ref = "datastore" }

# Test
junit = { module = "junit:junit", version.ref = "junit" }
junit-jupiter-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit-jupiter-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
mockk-android = { module = "io.mockk:mockk-android", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
hilt-android-testing = { module = "com.google.dagger:hilt-android-testing", version.ref = "hilt" }

# build-logic 用（在 convention plugin 内引用）
android-gradle-plugin = { module = "com.android.tools.build:gradle", version.ref = "agp" }
kotlin-gradle-plugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
ksp-gradle-plugin = { module = "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }

# 自定义 convention plugin（在 build-logic 中注册 ID）
ledger-android-application = { id = "ledger.android.application" }
ledger-android-library = { id = "ledger.android.library" }
ledger-android-compose = { id = "ledger.android.compose" }
ledger-android-hilt = { id = "ledger.android.hilt" }
ledger-kotlin-jvm = { id = "ledger.kotlin.jvm" }
```

- [ ] **Step 2: 提交**

```bash
git add gradle/libs.versions.toml
git commit -m "build: add version catalog with full dependency set"
```

---

## Task 3：Build-Logic Convention Plugins

**Files:**
- Create: `build-logic/settings.gradle.kts`
- Create: `build-logic/build.gradle.kts`
- Create: `build-logic/src/main/kotlin/AndroidApplicationConventionPlugin.kt`
- Create: `build-logic/src/main/kotlin/AndroidLibraryConventionPlugin.kt`
- Create: `build-logic/src/main/kotlin/AndroidComposeConventionPlugin.kt`
- Create: `build-logic/src/main/kotlin/AndroidHiltConventionPlugin.kt`
- Create: `build-logic/src/main/kotlin/KotlinJvmConventionPlugin.kt`

- [ ] **Step 1: `build-logic/settings.gradle.kts`**

```kotlin
@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
```

- [ ] **Step 2: `build-logic/build.gradle.kts`**

```kotlin
plugins {
    `kotlin-dsl`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "ledger.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "ledger.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "ledger.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "ledger.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("kotlinJvm") {
            id = "ledger.kotlin.jvm"
            implementationClass = "KotlinJvmConventionPlugin"
        }
    }
}
```

- [ ] **Step 3: `KotlinJvmConventionPlugin.kt`**

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class KotlinJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }
}
```

- [ ] **Step 4: `AndroidLibraryConventionPlugin.kt`**

```kotlin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")

        extensions.configure<LibraryExtension> {
            compileSdk = 36
            defaultConfig {
                minSdk = 26
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }
    }
}
```

- [ ] **Step 5: `AndroidApplicationConventionPlugin.kt`**

```kotlin
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.android")

        extensions.configure<ApplicationExtension> {
            compileSdk = 36
            defaultConfig {
                minSdk = 26
                targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables { useSupportLibrary = true }
            }
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
                getByName("debug") {
                    applicationIdSuffix = ".debug"
                    isDebuggable = true
                }
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            packaging {
                resources.excludes += setOf(
                    "META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/LICENSE*",
                    "META-INF/NOTICE*",
                    "META-INF/DEPENDENCIES"
                )
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
            compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        }
    }
}
```

- [ ] **Step 6: `AndroidComposeConventionPlugin.kt`**

```kotlin
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        extensions.configure<CommonExtension<*, *, *, *, *, *>>("android") {
            buildFeatures.compose = true
        }

        dependencies {
            val bom = platform(libs.findLibrary("compose.bom").get())
            "implementation"(bom)
            "androidTestImplementation"(bom)
            "implementation"(libs.findLibrary("compose.foundation").get())
            "implementation"(libs.findLibrary("compose.ui").get())
            "implementation"(libs.findLibrary("compose.material3").get())
            "implementation"(libs.findLibrary("compose.material.icons.extended").get())
            "implementation"(libs.findLibrary("compose.ui.tooling.preview").get())
            "debugImplementation"(libs.findLibrary("compose.ui.tooling").get())
        }
    }
}
```

- [ ] **Step 7: `AndroidHiltConventionPlugin.kt`**

```kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.google.dagger.hilt.android")

        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        dependencies {
            "implementation"(libs.findLibrary("hilt.android").get())
            "ksp"(libs.findLibrary("hilt.compiler").get())
        }
    }
}
```

- [ ] **Step 8: 验证 build-logic 编译通过**

Run: `./gradlew :build-logic:build --no-configuration-cache`
Expected: BUILD SUCCESSFUL（首次会下载 Gradle、Android Gradle Plugin、Kotlin 等，5–10 分钟）

> 失败常见原因：JDK 17 未安装。Run `java -version` 检查；如果不是 17，参考 `https://adoptium.net/` 装 Temurin 17。

- [ ] **Step 9: 提交**

```bash
git add build-logic/
git commit -m "build: add convention plugins for Android app/library/compose/hilt"
```

---

## Task 4：app Module（最小可启动 Compose Activity）

**Files:**
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/kotlin/io/github/nia/ledger/LedgerApplication.kt`
- Create: `app/src/main/kotlin/io/github/nia/ledger/MainActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.ledger.android.application)
    alias(libs.plugins.ledger.android.compose)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "io.github.nia.ledger"
    defaultConfig {
        applicationId = "io.github.nia.ledger"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(libs.android.core.ktx)
    implementation(libs.android.activity.compose)
    implementation(libs.android.lifecycle.runtime.ktx)
    implementation(libs.android.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
```

- [ ] **Step 2: `app/proguard-rules.pro`**

```proguard
# 保留 Hilt 注入入口
-keep class * extends androidx.lifecycle.ViewModel
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Compose 编译器已处理保留规则
```

- [ ] **Step 3: `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".LedgerApplication"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Ledger">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Ledger">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

- [ ] **Step 4: 创建 backup XML 占位文件**

Create `app/src/main/res/xml/backup_rules.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content />
```

Create `app/src/main/res/xml/data_extraction_rules.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup />
    <device-transfer />
</data-extraction-rules>
```

- [ ] **Step 5: `app/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Personal Ledger</string>
    <string name="hello_ledger">Hello, Ledger</string>
</resources>
```

- [ ] **Step 6: `app/src/main/res/values/themes.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Ledger" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 7: `app/src/main/kotlin/io/github/nia/ledger/LedgerApplication.kt`**

```kotlin
package io.github.nia.ledger

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LedgerApplication : Application()
```

- [ ] **Step 8: `app/src/main/kotlin/io/github/nia/ledger/MainActivity.kt`**

```kotlin
package io.github.nia.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HelloScreen()
            }
        }
    }
}

@Composable
private fun HelloScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Hello, Ledger",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelloScreenPreview() {
    MaterialTheme { HelloScreen() }
}
```

- [ ] **Step 9: 验证 app build 通过**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL，APK 输出到 `app/build/outputs/apk/debug/`

- [ ] **Step 10: 提交**

```bash
git add app/
git commit -m "feat(app): add minimal Compose Activity with Hilt application"
```

---

## Task 5：core-* 五个 starter kit 模块占位

**Files:**
- Create: `core-base/build.gradle.kts`
- Create: `core-utils/build.gradle.kts`
- Create: `core-extensions/build.gradle.kts`
- Create: `core-network/build.gradle.kts`
- Create: `core-ui/build.gradle.kts`
- Create: `core-data/build.gradle.kts`

> **本 Plan 只创建 build.gradle.kts**，模块代码在 Plan 02 迁移。每个模块创建一个 placeholder Kotlin 文件让 KSP 不报"empty source set"。

- [ ] **Step 1: `core-utils/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.ledger.android.library)
}

android {
    namespace = "io.github.nia.ledger.core.utils"
}

dependencies {
    implementation(libs.android.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
```

- [ ] **Step 2: `core-extensions/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.compose)
}

android {
    namespace = "io.github.nia.ledger.core.extensions"
}

dependencies {
    implementation(libs.android.core.ktx)
    implementation(libs.android.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
}
```

- [ ] **Step 3: `core-base/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "io.github.nia.ledger.core.base"
}

dependencies {
    implementation(libs.android.core.ktx)
    implementation(libs.android.lifecycle.viewmodel.compose)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
```

- [ ] **Step 4: `core-network/build.gradle.kts`**

> 注意：`ledger.android.hilt` convention plugin 内部已 apply KSP，无需重复 alias。

```kotlin
plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "io.github.nia.ledger.core.network"
}

dependencies {
    api(libs.retrofit)
    api(libs.retrofit.converter.moshi)
    api(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
```

- [ ] **Step 5: `core-ui/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.compose)
}

android {
    namespace = "io.github.nia.ledger.core.ui"
}

dependencies {
    implementation(libs.android.core.ktx)
    implementation(libs.android.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
}
```

- [ ] **Step 6: `core-data/build.gradle.kts`**

> 注意：`ledger.android.hilt` convention plugin 内部已 apply KSP，无需重复 alias。

```kotlin
plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "io.github.nia.ledger.core.data"
}

dependencies {
    implementation(project(":core-network"))
    implementation(project(":core-utils"))

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.room.testing)
}
```

- [ ] **Step 7: 创建 placeholder 源文件（避免 empty module 警告）**

For each `core-*` module, create `src/main/kotlin/io/github/nia/ledger/<package>/Placeholder.kt`:

```kotlin
package io.github.nia.ledger.core.utils

internal object Placeholder
```

> 6 个 module 各创建一个，包名分别是 `core.utils`、`core.extensions`、`core.base`、`core.network`、`core.ui`、`core.data`。

- [ ] **Step 8: 验证全模块编译通过**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL，所有模块都跑过

- [ ] **Step 9: 提交**

```bash
git add core-base/ core-utils/ core-extensions/ core-network/ core-ui/ core-data/
git commit -m "build: scaffold 6 core-* library modules with placeholders"
```

---

## Task 6：feature-* 四个业务模块占位

**Files:**
- Create: `feature-record/build.gradle.kts`
- Create: `feature-account/build.gradle.kts`
- Create: `feature-stats/build.gradle.kts`
- Create: `feature-settings/build.gradle.kts`

- [ ] **Step 1: 4 个 feature module 用同款 build.gradle.kts**

通用模板（package 名各自调整为 `feature.record` / `feature.account` / `feature.stats` / `feature.settings`）：

```kotlin
plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.compose)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "io.github.nia.ledger.feature.<NAME>"  // 替换 <NAME>
}

dependencies {
    implementation(project(":core-base"))
    implementation(project(":core-data"))
    implementation(project(":core-ui"))
    implementation(project(":core-utils"))
    implementation(project(":core-extensions"))

    implementation(libs.android.lifecycle.viewmodel.compose)
    implementation(libs.android.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
}
```

> 创建 4 份，分别替换 `<NAME>`：record / account / stats / settings

- [ ] **Step 2: 4 个 feature 各创建 Placeholder.kt**

`feature-record/src/main/kotlin/io/github/nia/ledger/feature/record/Placeholder.kt`:
```kotlin
package io.github.nia.ledger.feature.record
internal object Placeholder
```

其余 account / stats / settings 同样。

- [ ] **Step 3: app 依赖 4 个 feature module**

修改 `app/build.gradle.kts` 的 `dependencies {}` 块，新增：

```kotlin
implementation(project(":feature-record"))
implementation(project(":feature-account"))
implementation(project(":feature-stats"))
implementation(project(":feature-settings"))
```

- [ ] **Step 4: 验证全工程编译**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add feature-record/ feature-account/ feature-stats/ feature-settings/ app/build.gradle.kts
git commit -m "build: scaffold 4 feature-* modules and wire to app"
```

---

## Task 7：Hilt 烟雾测试

**Files:**
- Create: `app/src/androidTest/kotlin/io/github/nia/ledger/HiltSmokeTest.kt`
- Modify: `app/build.gradle.kts`（加测试依赖）

> 烟雾测试目的：用真实 Hilt 容器启动 Application，确认依赖图能构建（防止上线后崩溃在 Application.onCreate）。

- [ ] **Step 1: 加测试依赖到 `app/build.gradle.kts`**

在 `dependencies {}` 块追加：

```kotlin
androidTestImplementation(libs.hilt.android.testing)
kspAndroidTest(libs.hilt.compiler)
androidTestImplementation("androidx.test:runner:1.6.2")
androidTestImplementation("androidx.test:rules:1.6.1")
androidTestImplementation("androidx.test.ext:junit:1.2.1")
```

修改 `defaultConfig` 中的 testInstrumentationRunner：

```kotlin
testInstrumentationRunner = "io.github.nia.ledger.HiltTestRunner"
```

- [ ] **Step 2: 创建 `app/src/androidTest/kotlin/io/github/nia/ledger/HiltTestRunner.kt`**

```kotlin
package io.github.nia.ledger

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
```

- [ ] **Step 3: 写 failing test**

`app/src/androidTest/kotlin/io/github/nia/ledger/HiltSmokeTest.kt`:

```kotlin
package io.github.nia.ledger

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltSmokeTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun hiltContainerStarts() {
        hiltRule.inject()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(context)
    }
}
```

- [ ] **Step 4: 跑测试**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: BUILD SUCCESSFUL（前提：模拟器已启动 / 真机已连接 ADB）

> 模拟器启动方法：Android Studio 打开 → AVD Manager → 启动一个 API 26+ 模拟器。或命令行 `emulator -avd <name>`。

> 如果 connectedAndroidTest 跑不了（无设备）：跑 `./gradlew :app:assembleDebugAndroidTest` 验证编译通过即可（实际运行可在 Android Studio 内点 Run）。

- [ ] **Step 5: 提交**

```bash
git add app/src/androidTest/ app/build.gradle.kts
git commit -m "test: add Hilt smoke test verifying DI container boots"
```

---

## Task 8：README + 收尾

**Files:**
- Create: `README.md`

- [ ] **Step 1: 写 README**

```markdown
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

要求：JDK 17、Android SDK 36、Gradle 8.10（项目自带 wrapper）

```bash
./gradlew assembleDebug         # 构建 debug APK
./gradlew :app:installDebug     # 装到设备
./gradlew test                  # 单元测试
./gradlew connectedAndroidTest  # 仪器测试（需模拟器/真机）
```

## 开发文档

- 设计文档：`docs/specs/`
- 实施计划：`docs/superpowers/plans/`

## 状态

🚧 v0.1 · 项目骨架（开发中）

## 协议

Apache 2.0（待补 LICENSE 文件）
```

- [ ] **Step 2: 全量编译 + 测试 最终验证**

Run: `./gradlew clean assembleDebug test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add README.md
git commit -m "docs: add README with module overview and build instructions"
```

- [ ] **Step 4: 查看本 Plan 产出**

Run: `git log --oneline`
Expected output:
```
<sha> docs: add README with module overview and build instructions
<sha> test: add Hilt smoke test verifying DI container boots
<sha> build: scaffold 4 feature-* modules and wire to app
<sha> build: scaffold 6 core-* library modules with placeholders
<sha> feat(app): add minimal Compose Activity with Hilt application
<sha> build: add convention plugins for Android app/library/compose/hilt
<sha> build: add version catalog with full dependency set
<sha> chore: bootstrap Gradle 8.10 multi-module workspace
<sha> docs: add Personal Ledger v1 design spec
```

9 个 commit，每个独立可 revert。

---

## 完成标准（Definition of Done）

- [x] `./gradlew assembleDebug` 成功
- [x] `./gradlew test` 成功
- [x] `./gradlew :app:installDebug` 装机成功，启动看到 "Hello, Ledger" 居中显示
- [x] 11 个 module（含 build-logic）编译都通过
- [x] Hilt smoke test（在 Android Studio 内跑）通过
- [x] git log 包含本 Plan 9 个 commit

---

## 已知限制（Plan 02 处理）

- core-* 模块只有 Placeholder，无实际代码
- feature-* 模块只有 Placeholder，无实际代码
- 没有 Theme、Color、Typography（沿用 `Theme.Material.Light.NoActionBar`）
- 没有 Navigation 3 接入（`MainActivity` 直接 setContent 一个 Composable）
- 没有 R8/ProGuard 完整配置（只有最小骨架）
- 没有 CI 配置（Plan 08 处理）
- 没有签名配置（Plan 08 处理）
