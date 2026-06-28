plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.vanniktech.maven.publish.plugin)
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

gradlePlugin {
    plugins {
        create("laydrPublishing") {
            id = "laydr.publishing"
            implementationClass = "LaydrPublishingPlugin"
        }
    }
}
