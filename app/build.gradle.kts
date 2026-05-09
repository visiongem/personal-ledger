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
    }
}

dependencies {
    implementation(project(":feature-record"))
    implementation(project(":feature-account"))
    implementation(project(":feature-stats"))
    implementation(project(":feature-settings"))

    implementation(libs.android.core.ktx)
    implementation(libs.android.activity.compose)
    implementation(libs.android.lifecycle.runtime.ktx)
    implementation(libs.android.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
