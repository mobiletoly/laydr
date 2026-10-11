plugins {
    id("laydr.android-library")
    id("laydr.publishing")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    api(project(":laydr-nav-runtime"))
    api(project(":laydr-compose"))
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)
    implementation(libs.compose.runtime)
    implementation(libs.compose.runtime.saveable)
    implementation(libs.kotlinx.serialization.core)
    testImplementation(libs.kotlinx.serialization.json)
    androidTestImplementation(kotlin("test-junit"))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.compose.foundation)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
