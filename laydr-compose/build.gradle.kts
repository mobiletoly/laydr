plugins {
    id("laydr.kotlin-multiplatform")
    id("laydr.publishing")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":laydr-core"))
            api(libs.compose.runtime)
        }
    }
}
