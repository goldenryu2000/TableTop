plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.tabletop.design"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    buildFeatures { compose = true }
}

kotlin { jvmToolchain(21) }

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material3)
    api(libs.compose.material.icons)
    api(libs.androidx.core.ktx)
    api(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
