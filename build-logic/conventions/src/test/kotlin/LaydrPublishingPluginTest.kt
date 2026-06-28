import kotlin.io.path.createDirectory
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

class LaydrPublishingPluginTest {
    @Test
    fun usesLocalSnapshotFallbackForMavenLocalWhenVersionIsMissing() {
        val project = laydrProject(taskNames = listOf("publishToMavenLocal"))

        project.pluginManager.apply(LaydrPublishingPlugin::class.java)

        assertEquals("0.1.0-SNAPSHOT", project.version.toString())
    }

    @Test
    fun failsRemotePublishWhenVersionIsMissing() {
        val project = laydrProject(taskNames = listOf("publishToMavenCentral"))

        val failure = assertFailsWith<GradleException> {
            project.pluginManager.apply(LaydrPublishingPlugin::class.java)
        }

        assertTrue(
            failure.causes().any { error ->
                error.message.orEmpty().contains("laydr.version is required")
            },
            failure.stackTraceToString(),
        )
    }

    @Test
    fun usesParentGradlePropertiesVersionForRemotePublish() {
        val project = laydrProject(
            taskNames = listOf("publishToMavenCentral"),
            parentGradleProperties = "laydr.version=1.2.3\n",
        )

        project.pluginManager.apply(LaydrPublishingPlugin::class.java)

        assertEquals("1.2.3", project.version.toString())
    }

    private fun laydrProject(
        taskNames: List<String>,
        parentGradleProperties: String? = null,
    ) = ProjectBuilder.builder()
        .withName("laydr-core")
        .withProjectDir(testProjectDir(parentGradleProperties))
        .build()
        .also { project ->
            project.gradle.startParameter.setTaskNames(taskNames)
        }

    private fun testProjectDir(parentGradleProperties: String?): java.io.File {
        val root = createTempDirectory("laydr-publishing-test")
        if (parentGradleProperties != null) {
            root.resolve("gradle.properties").writeText(parentGradleProperties)
        }
        return root.resolve("project").createDirectory().toFile()
    }

    private fun Throwable.causes(): Sequence<Throwable> =
        generateSequence(this) { error -> error.cause }
}
