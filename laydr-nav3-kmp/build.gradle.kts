plugins {
    id("laydr.kotlin-multiplatform")
    id("laydr.publishing")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":laydr-nav-runtime"))
            api(project(":laydr-compose"))
            api(libs.nav3.kmp.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.runtime.saveable)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.serialization.core)
        }
    }
}
