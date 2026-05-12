import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.ledger.android.application)
    alias(libs.plugins.ledger.android.compose)
    alias(libs.plugins.ledger.android.hilt)
}

// Read the current short git sha at configuration time. providers.exec keeps
// the configuration cache happy by treating the command as a tracked input;
// failures fall back to "unknown" so source-export builds still compile.
val gitShortSha: String = runCatching {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        workingDir = rootProject.rootDir
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrNull()?.takeIf { it.isNotEmpty() } ?: "unknown"

val buildTimestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

android {
    namespace = "io.github.visiongem.ledger"
    defaultConfig {
        applicationId = "io.github.visiongem.ledger"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "io.github.visiongem.ledger.HiltTestRunner"
        buildConfigField("String", "GIT_SHA", "\"$gitShortSha\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTimestamp\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // Release signing reads credentials from ~/.gradle/gradle.properties (or any
    // project-local override) so the keystore path and passwords never enter the
    // repository. Missing properties → release builds without a signing config
    // (AGP produces an unsigned APK, useful for inspection but not installable).
    val releaseStoreFilePath = project.findProperty("LEDGER_RELEASE_STORE_FILE") as String?
    val releaseStoreFile = releaseStoreFilePath?.let { path ->
        rootProject.file(path).takeIf { it.exists() }
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = releaseStoreFile
                storePassword = project.findProperty("LEDGER_RELEASE_STORE_PASSWORD") as String?
                keyAlias = project.findProperty("LEDGER_RELEASE_KEY_ALIAS") as String?
                keyPassword = project.findProperty("LEDGER_RELEASE_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Personal app: skip R8/resource shrinking so we don't need to maintain
            // ProGuard rules for Hilt/Moshi/Retrofit/Room reflection. Trades APK
            // size for build reliability.
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

dependencies {
    implementation(project(":core-ui"))
    implementation(project(":core-data"))
    implementation(project(":feature-record"))
    implementation(project(":feature-account"))
    implementation(project(":feature-stats"))
    implementation(project(":feature-settings"))

    implementation(libs.android.core.ktx)
    implementation(libs.android.core.splashscreen)
    implementation(libs.android.activity.compose)
    implementation(libs.android.lifecycle.runtime.ktx)
    implementation(libs.android.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    // Explicit dep so AS Lint resolves androidx.startup.InitializationProvider in the
    // AndroidManifest. WorkManager brings it in transitively, but lint doesn't always
    // follow transitives — without this, the provider declaration shows red in the IDE.
    implementation(libs.androidx.startup.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
