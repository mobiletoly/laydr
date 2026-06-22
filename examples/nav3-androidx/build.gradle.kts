plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("dev.goquick.laydr")
}

android {
    namespace = "dev.goquick.laydr.examples.nav3androidx"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.goquick.laydr.examples.nav3androidx"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(project(":laydr-nav3-androidx"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.kotlinx.coroutines.core)
}

laydr {
    generatedPackage.set("dev.goquick.laydr.examples.nav3androidx.generated")
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
