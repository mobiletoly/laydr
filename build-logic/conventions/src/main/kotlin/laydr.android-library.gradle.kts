import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("com.android.library")
}

android {
    namespace = "dev.goquick.laydr"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    explicitApi()

    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val libraryCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libraryCatalog.findLibrary("junit-platform-launcher").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
