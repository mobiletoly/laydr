plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.goquick.laydr.examples.basic.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.goquick.laydr.examples.basic.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":examples:compose-basic:shared"))
    implementation(libs.androidx.activity.compose)
}
