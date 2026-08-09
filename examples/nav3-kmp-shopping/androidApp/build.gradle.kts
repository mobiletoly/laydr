import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.goquick.laydr.examples.nav3kmpshopping.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.goquick.laydr.examples.nav3kmpshopping.android"
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":examples:nav3-kmp-shopping:shared"))
    implementation(libs.androidx.activity.compose)
    debugImplementation(project(":laydr-nav3-kmp"))
    debugImplementation(libs.compose.foundation)
    androidTestImplementation(project(":laydr-nav3-kmp"))
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.compose.foundation)
}
