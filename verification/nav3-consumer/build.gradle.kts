import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.laydr) apply false
}

allprojects {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    plugins.withType<BinaryenPlugin>().configureEach {
        extensions.configure<BinaryenEnvSpec>(BinaryenEnvSpec.EXTENSION_NAME) { downloadBaseUrl.unset() }
    }
    plugins.withType<WasmNodeJsPlugin>().configureEach {
        extensions.configure<WasmNodeJsEnvSpec>(WasmNodeJsEnvSpec.EXTENSION_NAME) { downloadBaseUrl.unset() }
    }
}
plugins.withType<WasmYarnPlugin>().configureEach {
    extensions.configure<WasmYarnRootEnvSpec>(WasmYarnRootEnvSpec.YARN) { downloadBaseUrl.unset() }
}
