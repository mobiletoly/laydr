plugins {
    id("laydr.kotlin-multiplatform")
    id("laydr.publishing")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // Compose browser tests need webpack to bundle the Skiko runtime.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { binaries.executable() }

    sourceSets {
        commonMain.dependencies {
            api(project(":laydr-nav3-kmp"))
            implementation(libs.compose.material3.adaptive.nav3)
            implementation(libs.compose.ui)
        }
    }
}
