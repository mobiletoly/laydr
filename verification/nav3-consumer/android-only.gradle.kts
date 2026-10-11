plugins {
    // Select the consumer's KGP classpath without applying any KMP plugin.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.laydr) apply false
}
