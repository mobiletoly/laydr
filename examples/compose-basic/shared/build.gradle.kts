plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("dev.goquick.laydr")
}

kotlin {
    android {
        namespace = "dev.goquick.laydr.examples.basic.shared"
        compileSdk = 36
        minSdk = 26
    }

    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":laydr-compose"))
            implementation(libs.compose.foundation)
            implementation(libs.compose.runtime)
        }
    }
}

laydr {
    generatedPackage.set("dev.goquick.laydr.examples.basic.generated")
    compose.set(true)
}
