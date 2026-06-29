import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

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
