import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.laydr)
}

val expectedCompiler = providers.gradleProperty("consumerKotlinVersion").get()

kotlin {
    jvmToolchain(17)
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_4)
        apiVersion.set(KotlinVersion.KOTLIN_2_4)
    }
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Nav3Consumer"
            binaryOption("bundleId", "dev.goquick.laydr.verification.nav3consumer")
            isStatic = true
        }
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.nav3.kmp)
            implementation(libs.laydr.nav3.kmp.adaptive)
            implementation(libs.compose.foundation)
            implementation(libs.nav3.kmp.ui)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}

laydr {
    generatedPackage.set("consumer.generated")
    compose.set(true)
    adapters { nav3Kmp.set(true) }
}

// Fail rather than allowing a transitive plugin dependency to silently upgrade the client.
val loadedKotlinPlugin = project.getKotlinPluginVersion()
val compilerConfiguration = configurations.getByName("kotlinCompilerClasspath")
tasks.register("verifyConsumerCompiler") {
    group = "verification"
    description = "Checks the actual KGP and compiler artifacts used by this isolated consumer."
    notCompatibleWithConfigurationCache("Records resolved compiler evidence.")
    doLast {
        check(loadedKotlinPlugin == expectedCompiler) {
            "Requested KGP $expectedCompiler but loaded $loadedKotlinPlugin"
        }
        val compilers = compilerConfiguration.resolvedConfiguration.resolvedArtifacts
            .filter { it.moduleVersion.id.name == "kotlin-compiler-embeddable" }
        check(compilers.isNotEmpty()) { "No resolved Kotlin compiler evidence" }
        check(compilers.all { it.moduleVersion.id.version == expectedCompiler }) {
            "Compiler does not match $expectedCompiler: ${compilers.map { it.moduleVersion.id }}"
        }
        val evidence = layout.buildDirectory.file("reports/compiler.txt").get().asFile
        evidence.parentFile.mkdirs()
        evidence.writeText("KGP=$loadedKotlinPlugin\ncompiler=${compilers.map { it.moduleVersion.id }}\n")
        println(evidence.readText())
    }
}
