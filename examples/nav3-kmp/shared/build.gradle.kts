plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("dev.goquick.laydr")
}

kotlin {
    android {
        namespace = "dev.goquick.laydr.examples.nav3kmp.shared"
        compileSdk = 37
        minSdk = 26
    }

    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":laydr-nav3-kmp"))
            implementation(project(":laydr-nav3-kmp-adaptive"))
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
        }
    }
}

laydr {
    generatedPackage.set("dev.goquick.laydr.examples.nav3kmp.generated")
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
