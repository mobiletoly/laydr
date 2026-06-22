// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.gradle

import dev.goquick.laydr.codegen.LaydrCodegen
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Gradle plugin that wires Laydr route generation into supported Kotlin source sets.
 */
public class LaydrPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("laydr", LaydrExtension::class.java)
        val sourceSetName = project.providers.provider {
            if (project.isAndroidOnlyLaydrProject()) {
                androidMainSourceSetName
            } else {
                kmpMainSourceSetName
            }
        }
        extension.routesDirectory.convention(
            sourceSetName.map { name -> project.layout.projectDirectory.dir(defaultRoutesDirectory(name)) },
        )
        val generateRoutes = project.tasks.register(
            "generateLaydrRoutes",
            GenerateLaydrRoutesTask::class.java,
        ) { task ->
            task.description = "Generates Laydr route graph source from the configured routes directory."
            task.group = "laydr"
            task.routesDirectory.set(extension.routesDirectory)
            task.generatedPackage.set(extension.generatedPackage)
            task.compose.set(extension.compose)
            task.nav3Kmp.set(extension.adapters.nav3Kmp)
            task.nav3Androidx.set(extension.adapters.nav3Androidx)
            task.outputDirectory.set(
                project.layout.buildDirectory.dir(
                    sourceSetName.map { name -> generatedSourceDirectory(name) },
                ),
            )
        }
        val checkRoutes = project.tasks.register(
            "checkLaydrRoutes",
            CheckLaydrRoutesTask::class.java,
        ) { task ->
            task.description = "Validates Laydr route declarations without writing generated source."
            task.group = LifecycleBasePlugin.VERIFICATION_GROUP
            task.routesDirectory.set(extension.routesDirectory)
            task.generatedPackage.set(extension.generatedPackage)
            task.compose.set(extension.compose)
            task.nav3Kmp.set(extension.adapters.nav3Kmp)
            task.nav3Androidx.set(extension.adapters.nav3Androidx)
        }

        project.tasks.matching { task -> task.name == LifecycleBasePlugin.CHECK_TASK_NAME }
            .configureEach { task ->
                task.dependsOn(checkRoutes)
            }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
            kotlin.sourceSets.named("commonMain") { sourceSet ->
                sourceSet.kotlin.srcDir(generateRoutes.flatMap { it.outputDirectory })
            }
        }

        project.configureAndroidOnlySourceSetWiring(generateRoutes)
    }

    private companion object {
        private const val kmpMainSourceSetName = "commonMain"
        private const val androidMainSourceSetName = "main"
        private fun defaultRoutesDirectory(sourceSetName: String): String =
            "src/$sourceSetName/kotlin/routes"

        private fun generatedSourceDirectory(sourceSetName: String): String =
            "generated/laydr/$sourceSetName/kotlin"
    }
}

/**
 * Configuration for Laydr route generation in a Gradle project.
 */
public open class LaydrExtension @Inject constructor(
    objects: ObjectFactory,
    layout: ProjectLayout,
) {
    /**
     * Directory containing filesystem route declarations.
     */
    public val routesDirectory: DirectoryProperty = objects.directoryProperty()

    /**
     * Kotlin package used by generated Laydr route graph source.
     */
    public val generatedPackage: Property<String> = objects.property(String::class.java)
        .convention(defaultGeneratedPackage)

    /**
     * Enables generated Compose route definitions and route-local helper files.
     *
     * The default is `false`, which emits only the core `LaydrRoutes.kt` graph
     * and keeps generated output free of Compose and `laydr-compose` symbols.
     */
    public val compose: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    /**
     * Optional generated adapter helper integrations.
     */
    public val adapters: LaydrAdaptersExtension =
        objects.newInstance(LaydrAdaptersExtension::class.java, objects)

    /**
     * Configures optional generated adapter helper integrations.
     */
    public fun adapters(action: Action<in LaydrAdaptersExtension>) {
        action.execute(adapters)
    }
    private companion object {
        private const val defaultGeneratedPackage = "dev.goquick.laydr.generated"
    }
}

/**
 * Optional generated adapter helper integrations for Laydr route generation.
 */
public open class LaydrAdaptersExtension @Inject constructor(
    objects: ObjectFactory,
) {
    /**
     * Enables generated helpers for Laydr's JetBrains Navigation3 KMP adapter.
     *
     * This requires `compose.set(true)` because the generated navigation helper
     * delegates to generated Compose route definitions.
     */
    public val nav3Kmp: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    /**
     * Enables generated helpers for Laydr's AndroidX Navigation 3 adapter.
     *
     * This requires `compose.set(true)` because the generated navigation helper
     * delegates to generated Compose route definitions. It cannot be enabled
     * for the same route tree as [nav3Kmp].
     */
    public val nav3Androidx: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)
}

/**
 * Validates the configured Laydr route tree without writing generated source.
 */
@DisableCachingByDefault(
    because = "Validation has no task outputs and is fast enough to run directly.",
)
public abstract class CheckLaydrRoutesTask : DefaultTask() {
    /**
     * Route directory scanned by the generator.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val routesDirectory: DirectoryProperty

    /**
     * Kotlin package for the generated route graph used during validation.
     */
    @get:Input
    public abstract val generatedPackage: Property<String>

    /**
     * Whether validation includes generated Compose route definition sources.
     */
    @get:Input
    public abstract val compose: Property<Boolean>

    /**
     * Whether validation includes generated Nav3 KMP helper sources.
     */
    @get:Input
    public abstract val nav3Kmp: Property<Boolean>

    /**
     * Whether validation includes generated AndroidX Nav3 helper sources.
     */
    @get:Input
    public abstract val nav3Androidx: Property<Boolean>

    /**
     * Runs route scanning and generation validation without writing output.
     */
    @TaskAction
    public fun check() {
        LaydrCodegen.generateSources(
            routesDirectory = routesDirectory.get().asFile.toPath(),
            generatedPackage = generatedPackage.get(),
            generateCompose = compose.get(),
            generateNav3Kmp = nav3Kmp.get(),
            generateNav3Androidx = nav3Androidx.get(),
        )
    }
}

/**
 * Generates the Laydr route graph source file from a configured route directory.
 */
@DisableCachingByDefault(
    because = "Generated output is deterministic but task caching is not part of the current contract.",
)
public abstract class GenerateLaydrRoutesTask : DefaultTask() {
    /**
     * Route directory scanned by the generator.
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val routesDirectory: DirectoryProperty

    /**
     * Kotlin package for the generated `LaydrRoutes.kt` file.
     */
    @get:Input
    public abstract val generatedPackage: Property<String>

    /**
     * Whether to generate Compose route definition sources.
     */
    @get:Input
    public abstract val compose: Property<Boolean>

    /**
     * Whether to generate Nav3 KMP helper sources.
     */
    @get:Input
    public abstract val nav3Kmp: Property<Boolean>

    /**
     * Whether to generate AndroidX Nav3 helper sources.
     */
    @get:Input
    public abstract val nav3Androidx: Property<Boolean>

    /**
     * Root directory where generated Kotlin source is written.
     */
    @get:OutputDirectory
    public abstract val outputDirectory: DirectoryProperty

    /**
     * Runs route scanning and writes the generated route graph source file.
     */
    @TaskAction
    public fun generate() {
        val outputRoot = outputDirectory.get().asFile
        outputRoot.deleteRecursively()

        val generatedSources = LaydrCodegen.generateSources(
            routesDirectory = routesDirectory.get().asFile.toPath(),
            generatedPackage = generatedPackage.get(),
            generateCompose = compose.get(),
            generateNav3Kmp = nav3Kmp.get(),
            generateNav3Androidx = nav3Androidx.get(),
        )
        generatedSources.forEach { source ->
            val outputFile = outputRoot.resolve(source.relativePath)
            Files.createDirectories(outputFile.parentFile.toPath())
            Files.writeString(outputFile.toPath(), source.content)
        }
    }
}

private fun Project.isAndroidOnlyLaydrProject(): Boolean =
    !plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") &&
        (
            plugins.hasPlugin("com.android.application") ||
                plugins.hasPlugin("com.android.library")
            )

private fun Project.configureAndroidOnlySourceSetWiring(
    generateRoutes: TaskProvider<GenerateLaydrRoutesTask>,
) {
    // Keep AGP optional for non-Android consumers while using AGP's generated-source API when present.
    val wireWhenReady = {
        if (isAndroidOnlyLaydrProject()) {
            val androidComponents = extensions.getByName("androidComponents")
            androidComponents.androidOnVariants { variant ->
                variant.androidSources()
                    .androidKotlinSourceDirectories()
                    .androidAddGeneratedSourceDirectory(generateRoutes)
            }
        }
    }
    pluginManager.withPlugin("com.android.application") { wireWhenReady() }
    pluginManager.withPlugin("com.android.library") { wireWhenReady() }
}

private fun Any.androidOnVariants(action: Action<Any>) {
    val selector = invokeAndroidNoArg("selector")
    val actionMethod = javaClass.methods.firstOrNull { method ->
        method.name == "onVariants" &&
            method.parameterCount == 2 &&
            method.parameterTypes[1].name == Action::class.java.name
    }
    val functionMethod = javaClass.methods.firstOrNull { method ->
        method.name == "onVariants" &&
            method.parameterCount == 2 &&
            method.parameterTypes[1].name == Function1::class.java.name
    }
    when {
        actionMethod != null -> actionMethod.invoke(this, selector, action)
        functionMethod != null -> functionMethod.invoke(
            this,
            selector,
            { variant: Any -> action.execute(variant) },
        )
        else -> error("Android Gradle Plugin extension does not expose onVariants(...).")
    }
}

private fun Any.androidSources(): Any =
    invokeAndroidNoArg("getSources")

private fun Any.androidKotlinSourceDirectories(): Any =
    invokeAndroidNoArg("getKotlin")

private fun Any.androidAddGeneratedSourceDirectory(
    generateRoutes: TaskProvider<GenerateLaydrRoutesTask>,
) {
    val outputDirectory: (GenerateLaydrRoutesTask) -> DirectoryProperty = { task -> task.outputDirectory }
    invokeAndroidTwoArgs(
        methodName = "addGeneratedSourceDirectory",
        firstArgument = generateRoutes,
        secondArgument = outputDirectory,
    )
}

private fun Any.invokeAndroidNoArg(methodName: String): Any =
    javaClass.methods
        .firstOrNull { method -> method.name == methodName && method.parameterCount == 0 }
        ?.invoke(this)
        ?: error("Android Gradle Plugin extension does not expose $methodName().")

private fun Any.invokeAndroidTwoArgs(
    methodName: String,
    firstArgument: Any,
    secondArgument: Any,
): Any? {
    val method = javaClass.methods
        .firstOrNull { method ->
            method.name == methodName &&
                method.parameterCount == 2 &&
                method.parameterTypes[0].isInstance(firstArgument) &&
                method.parameterTypes[1].isInstance(secondArgument)
        }
        ?: javaClass.methods
            .firstOrNull { method -> method.name == methodName && method.parameterCount == 2 }
        ?: error("Android Gradle Plugin extension does not expose $methodName(...).")
    return method.invoke(this, firstArgument, secondArgument)
}
