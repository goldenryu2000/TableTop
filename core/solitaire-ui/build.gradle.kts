plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.tabletop.solitaire.ui"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
}

kotlin { jvmToolchain(21) }

dependencies {
    api(project(":core:cards-ui"))
    api(project(":core:game-api"))
    testImplementation(libs.junit)
}
