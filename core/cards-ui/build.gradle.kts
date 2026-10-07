plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.tabletop.cards.ui"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
}

kotlin { jvmToolchain(21) }

dependencies {
    api(project(":core:cards-model"))
    api(project(":core:design"))
    testImplementation(libs.junit)
}
