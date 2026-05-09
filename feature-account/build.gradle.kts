plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.compose)
    alias(libs.plugins.ledger.android.hilt)
}

android {
    namespace = "io.github.visiongem.ledger.feature.account"
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
