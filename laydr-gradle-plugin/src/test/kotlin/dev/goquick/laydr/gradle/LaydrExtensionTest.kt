package dev.goquick.laydr.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.language.base.plugins.LifecycleBasePlugin
import kotlin.test.Test
import kotlin.test.assertEquals

class LaydrExtensionTest {
    @Test
    fun usesDefaultExtensionValues() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply(LaydrPlugin::class.java)

        val extension = project.extensions.getByType(LaydrExtension::class.java)
        assertEquals(
            project.layout.projectDirectory.dir("src/commonMain/kotlin/routes").asFile,
            extension.routesDirectory.get().asFile,
        )
        assertEquals("dev.goquick.laydr.generated", extension.generatedPackage.get())
        assertEquals(false, extension.compose.get())
        assertEquals(false, extension.adapters.nav3Kmp.get())
        assertEquals(false, extension.adapters.nav3Androidx.get())
    }

    @Test
    fun registersGenerateRoutesTask() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply(LaydrPlugin::class.java)

        val task = project.tasks.named(
            "generateLaydrRoutes",
            GenerateLaydrRoutesTask::class.java,
        ).get()
        assertEquals("laydr", task.group)
        assertEquals(
            project.layout.buildDirectory.dir("generated/laydr/commonMain/kotlin").get().asFile,
            task.outputDirectory.get().asFile,
        )
        assertEquals(false, task.compose.get())
        assertEquals(false, task.nav3Kmp.get())
        assertEquals(false, task.nav3Androidx.get())
    }

    @Test
    fun registersCheckRoutesTask() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply(LaydrPlugin::class.java)

        val task = project.tasks.named(
            "checkLaydrRoutes",
            CheckLaydrRoutesTask::class.java,
        ).get()
        assertEquals(LifecycleBasePlugin.VERIFICATION_GROUP, task.group)
        assertEquals(
            project.layout.projectDirectory.dir("src/commonMain/kotlin/routes").asFile,
            task.routesDirectory.get().asFile,
        )
        assertEquals("dev.goquick.laydr.generated", task.generatedPackage.get())
        assertEquals(false, task.compose.get())
        assertEquals(false, task.nav3Kmp.get())
        assertEquals(false, task.nav3Androidx.get())
    }

    @Test
    fun wiresAdapterOptionsIntoRouteTasks() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply(LaydrPlugin::class.java)

        val extension = project.extensions.getByType(LaydrExtension::class.java)
        extension.compose.set(true)
        extension.adapters.nav3Kmp.set(true)
        extension.adapters.nav3Androidx.set(true)

        val generateTask = project.tasks.named(
            "generateLaydrRoutes",
            GenerateLaydrRoutesTask::class.java,
        ).get()
        val checkTask = project.tasks.named(
            "checkLaydrRoutes",
            CheckLaydrRoutesTask::class.java,
        ).get()

        assertEquals(true, generateTask.compose.get())
        assertEquals(true, generateTask.nav3Kmp.get())
        assertEquals(true, generateTask.nav3Androidx.get())
        assertEquals(true, checkTask.compose.get())
        assertEquals(true, checkTask.nav3Kmp.get())
        assertEquals(true, checkTask.nav3Androidx.get())
    }
}
