plugins {
    alias(libs.plugins.ledger.android.library)
    alias(libs.plugins.ledger.android.compose)
}

android {
    namespace = "io.github.visiongem.ledger.core.ui"
}

dependencies {
    implementation(libs.android.core.ktx)
    implementation(libs.android.lifecycle.runtime.compose)
    implementation(libs.coil.compose)
}
