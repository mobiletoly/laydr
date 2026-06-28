@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("dev.goquick.laydr")
}

kotlin {
    android {
        namespace = "dev.goquick.laydr.examples.nav3kmpshopping.shared"
        compileSdk = 37
        minSdk = 26
    }

    jvm("desktop")
    wasmJs {
        browser()
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Nav3KmpShoppingShared"
            binaryOption("bundleId", "dev.goquick.laydr.examples.nav3kmpshopping.shared")
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":laydr-nav3-kmp"))
            implementation(project(":laydr-nav3-kmp-adaptive"))
            implementation(project(":laydr-workflow"))
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.koin.compose)
            implementation(libs.koin.core)
        }
    }
}

laydr {
    generatedPackage.set("dev.goquick.laydr.examples.nav3kmpshopping.generated")
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
