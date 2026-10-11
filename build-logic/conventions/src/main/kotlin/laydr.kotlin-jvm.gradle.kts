import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    kotlin("jvm")
}

kotlin {
    explicitApi()
    jvmToolchain(17)

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
