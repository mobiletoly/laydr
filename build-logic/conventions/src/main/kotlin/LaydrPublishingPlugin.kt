// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import java.util.Properties
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class LaydrPublishingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.ensureLaydrCoordinates()
        project.pluginManager.apply("com.vanniktech.maven.publish")

        val metadata = moduleMetadata.getValue(project.name)
        project.extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
            publishToMavenCentral()
            if (project.plugins.hasPlugin("com.android.library")) {
                configure(AndroidSingleVariantLibrary(javadocJar = JavadocJar.Empty()))
            }
            if (!project.isPublishingToMavenLocal() && project.hasSigningCredentials()) {
                signAllPublications()
            }
            coordinates(
                project.group.toString(),
                metadata.artifactId,
                project.version.toString(),
            )
            pom {
                name.set(metadata.name)
                description.set(metadata.description)
                inceptionYear.set("2026")
                url.set("https://github.com/mobiletoly/laydr/")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("mobiletoly")
                        name.set("Toly Pochkin")
                        url.set("https://github.com/mobiletoly")
                    }
                }
                scm {
                    url.set("https://github.com/mobiletoly/laydr")
                    connection.set("scm:git:git://github.com/mobiletoly/laydr.git")
                    developerConnection.set("scm:git:ssh://git@github.com/mobiletoly/laydr.git")
                }
            }
        }
    }

    private fun Project.ensureLaydrCoordinates() {
        if (group.toString().isBlank() || group.toString() == "unspecified") {
            group = laydrProperty("laydr.group", DEFAULT_LAYDR_GROUP)
        }
        if (version.toString().isBlank() || version.toString() == "unspecified") {
            version = laydrVersion()
        }
    }

    private fun Project.laydrProperty(name: String, fallback: String): String =
        explicitLaydrProperty(name) ?: fallback

    private fun Project.laydrVersion(): String =
        explicitLaydrProperty("laydr.version")
            ?: if (isRemotePublishingRequested()) {
                throw GradleException(
                    "laydr.version is required for remote publishing. " +
                        "Set laydr.version in gradle.properties or pass -Playdr.version=...",
                )
            } else {
                LOCAL_DEVELOPMENT_VERSION
            }

    private fun Project.explicitLaydrProperty(name: String): String? =
        providers.gradleProperty(name).orNull?.trim()?.takeIf(String::isNotEmpty)
            ?: parentGradleProperties().getProperty(name)?.trim()?.takeIf(String::isNotEmpty)

    private fun Project.parentGradleProperties(): Properties {
        val properties = Properties()
        val propertiesFile = rootProject.layout.projectDirectory.file("../gradle.properties").asFile
        if (propertiesFile.isFile) {
            propertiesFile.inputStream().use(properties::load)
        }
        return properties
    }

    private fun Project.isPublishingToMavenLocal(): Boolean =
        gradle.startParameter.taskNames.any { taskName ->
            taskName.isPublishToMavenLocalTask()
        }

    private fun Project.isRemotePublishingRequested(): Boolean =
        gradle.startParameter.taskNames.any { taskName ->
            taskName.contains("publish", ignoreCase = true) &&
                !taskName.isPublishToMavenLocalTask()
        }

    private fun String.isPublishToMavenLocalTask(): Boolean =
        contains("publishToMavenLocal", ignoreCase = true)

    private fun Project.hasSigningCredentials(): Boolean =
        providers.environmentVariable("SIGNING_KEY").isPresent ||
            providers.environmentVariable("SIGNING_KEY_ID").isPresent ||
            providers.environmentVariable("SIGNING_PASSWORD").isPresent ||
            providers.gradleProperty("signingInMemoryKey").isPresent ||
            providers.gradleProperty("signing.keyId").isPresent ||
            providers.gradleProperty("signing.password").isPresent ||
            providers.gradleProperty("signing.secretKeyRingFile").isPresent ||
            providers.gradleProperty("signing.gnupg.keyName").isPresent

    private data class ModuleMetadata(
        val artifactId: String,
        val name: String,
        val description: String,
    )

    private companion object {
        const val DEFAULT_LAYDR_GROUP = "dev.goquick.laydr"
        const val LOCAL_DEVELOPMENT_VERSION = "0.2.0"

        val moduleMetadata = mapOf(
            "laydr-core" to ModuleMetadata(
                artifactId = "laydr-core",
                name = "Laydr Core",
                description = "Kotlin Multiplatform runtime contracts for Laydr route graphs.",
            ),
            "laydr-compose" to ModuleMetadata(
                artifactId = "laydr-compose",
                name = "Laydr Compose",
                description = "Compose Multiplatform route host and route definition contracts for Laydr.",
            ),
            "laydr-workflow" to ModuleMetadata(
                artifactId = "laydr-workflow",
                name = "Laydr Workflow",
                description = "Pure Kotlin Multiplatform route-local workflow runtime for Laydr.",
            ),
            "laydr-nav-runtime" to ModuleMetadata(
                artifactId = "laydr-nav-runtime",
                name = "Laydr Navigation Runtime",
                description = "Adapter-neutral Kotlin Multiplatform navigation runtime for Laydr.",
            ),
            "laydr-workflow-compose" to ModuleMetadata(
                artifactId = "laydr-workflow-compose",
                name = "Laydr Workflow Compose",
                description = "Compose Multiplatform hosting for Laydr route-local workflows.",
            ),
            "laydr-nav3-kmp" to ModuleMetadata(
                artifactId = "laydr-nav3-kmp",
                name = "Laydr Nav3 KMP",
                description = "JetBrains Navigation3 KMP integration for generated Laydr route contracts.",
            ),
            "laydr-nav3-androidx" to ModuleMetadata(
                artifactId = "laydr-nav3-androidx",
                name = "Laydr Nav3 AndroidX",
                description = "AndroidX Navigation 3 integration for generated Laydr route contracts.",
            ),
            "laydr-nav3-kmp-adaptive" to ModuleMetadata(
                artifactId = "laydr-nav3-kmp-adaptive",
                name = "Laydr Nav3 KMP Adaptive",
                description = "Optional Material adaptive Nav3 scene support for Laydr route contracts.",
            ),
            "laydr-codegen" to ModuleMetadata(
                artifactId = "laydr-codegen",
                name = "Laydr Codegen",
                description = "JVM route scanner, validator, and Kotlin source generator for Laydr.",
            ),
            "laydr-gradle-plugin" to ModuleMetadata(
                artifactId = "laydr-gradle-plugin",
                name = "Laydr Gradle Plugin",
                description = "Gradle plugin for Laydr route generation and route validation.",
            ),
            "laydr-product-plugin" to ModuleMetadata(
                artifactId = "laydr-gradle-plugin",
                name = "Laydr Gradle Plugin",
                description = "Gradle plugin for Laydr route generation and route validation.",
            ),
        )
    }
}
