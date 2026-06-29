plugins {
    id("laydr.android-library")
    id("laydr.publishing")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
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
}
