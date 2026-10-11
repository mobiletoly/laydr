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
            api(project(":laydr-nav-runtime"))
            api(project(":laydr-compose"))
            api(libs.nav3.kmp.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.runtime.saveable)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}
