plugins {
    alias(libs.plugins.ledger.android.library)
}

android {
    namespace = "io.github.visiongem.ledger.core.utils"
}

dependencies {
    implementation(libs.android.core.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
