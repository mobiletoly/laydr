import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.laydr)
}

android {
    namespace = "consumer.androidx"
    compileSdk = 37
    defaultConfig {
        applicationId = "consumer.androidx"
        minSdk = 26
        targetSdk = 36
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
dependencies {
    implementation(libs.laydr.nav3.androidx)
    implementation(libs.androidx.activity.compose)
}
laydr {
    generatedPackage.set("consumer.androidx.generated")
    compose.set(true)
    adapters { nav3Androidx.set(true) }
}

val expectedCompiler = providers.gradleProperty("consumerKotlinVersion").get()
val loadedKotlinPlugin = project.getKotlinPluginVersion()
val compilerConfiguration = configurations.getByName("kotlinCompilerClasspath")
tasks.register("verifyConsumerCompiler") {
    group = "verification"
    description = "Checks the actual Kotlin plugin/compiler used by the Android-only consumer."
    notCompatibleWithConfigurationCache("Records resolved compiler evidence.")
    doLast {
        check(loadedKotlinPlugin == expectedCompiler)
        check(!plugins.hasPlugin("org.jetbrains.kotlin.multiplatform"))
        val compilers = compilerConfiguration.resolvedConfiguration.resolvedArtifacts
            .filter { it.moduleVersion.id.name == "kotlin-compiler-embeddable" }
        check(compilers.isNotEmpty() && compilers.all { it.moduleVersion.id.version == expectedCompiler })
        val evidence = layout.buildDirectory.file("reports/compiler.txt").get().asFile
        evidence.parentFile.mkdirs()
        evidence.writeText("KGP=$loadedKotlinPlugin\ncompiler=${compilers.map { it.moduleVersion.id }}\n")
        println(evidence.readText())
    }
}
