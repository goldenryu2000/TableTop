plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.tabletop.feedback"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
}

kotlin { jvmToolchain(21) }
