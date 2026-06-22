plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.goquick.laydr.examples.nav3kmp.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.goquick.laydr.examples.nav3kmp.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":examples:nav3-kmp:shared"))
    implementation(libs.androidx.activity.compose)
}
