plugins {
    alias(libs.plugins.ledger.android.application)
    alias(libs.plugins.ledger.android.compose)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "io.github.visiongem.ledger"
    defaultConfig {
        applicationId = "io.github.visiongem.ledger"
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "io.github.visiongem.ledger.HiltTestRunner"
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
