import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

val laydrGroup = providers.gradleProperty("laydr.group").get()
val laydrVersion = providers.gradleProperty("laydr.version").get()

allprojects {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    plugins.withType<BinaryenPlugin>().configureEach {
        extensions.configure<BinaryenEnvSpec>(BinaryenEnvSpec.EXTENSION_NAME) {
            downloadBaseUrl.unset()
        }
    }
    plugins.withType<WasmNodeJsPlugin>().configureEach {
        extensions.configure<WasmNodeJsEnvSpec>(WasmNodeJsEnvSpec.EXTENSION_NAME) {
            downloadBaseUrl.unset()
        }
    }
}

plugins.withType<WasmYarnPlugin>().configureEach {
    extensions.configure<WasmYarnRootEnvSpec>(WasmYarnRootEnvSpec.YARN) {
        downloadBaseUrl.unset()
    }
}

subprojects {
    group = laydrGroup
    version = laydrVersion
}

// Internal runner input: historical Kotlin/AGP client with current runtime dependencies.
val consumerVersionRows = listOf(
    listOf(
        libs.versions.compatibilityKotlin.get(), libs.versions.compose.get(),
        libs.versions.nav3Kmp.get(), libs.versions.compatibilityAgp.get(),
        libs.versions.activity.get(),
    ),
    listOf(
        libs.versions.kotlin.get(), libs.versions.compose.get(), libs.versions.nav3Kmp.get(),
        libs.versions.agp.get(), libs.versions.activity.get(),
    ),
).distinct()
val consumerVersionsFile = layout.buildDirectory.file("nav3-validation/consumer-versions.tsv")
val consumerPublicationTasksFile = layout.buildDirectory.file("nav3-validation/jvm-android-publication-tasks.txt")
val consumerPublicationTasks = providers.provider {
    val publications = setOf("kotlinMultiplatform", "jvm", "maven", "pluginMaven", "laydrPluginMarkerMaven")
    subprojects.filter { it.name.startsWith("laydr-") }.flatMap { module ->
        module.tasks.withType<PublishToMavenRepository>().filter { task ->
            task.repository.name == "Nav3Verification" && task.publication.name in publications
        }.map { it.path }
    }.sorted()
}
tasks.register("writeNav3ConsumerVersions") {
    group = "verification"
    description = "Writes catalog-owned consumer profiles and JVM/Android verification publication tasks."
    notCompatibleWithConfigurationCache("Reads configured verification publication task models.")
    val profileText = consumerVersionRows.joinToString("\n", postfix = "\n") { it.joinToString("\t") }
    val outputFile = consumerVersionsFile.get().asFile
    inputs.property("profiles", profileText)
    inputs.property("jvmAndroidPublicationTasks", consumerPublicationTasks)
    outputs.file(outputFile)
    outputs.file(consumerPublicationTasksFile)
    doLast {
        outputFile.apply {
            parentFile.mkdirs()
            writeText(profileText)
        }
        consumerPublicationTasksFile.get().asFile.writeText(
            consumerPublicationTasks.get().joinToString("\n", postfix = "\n"),
        )
    }
}
