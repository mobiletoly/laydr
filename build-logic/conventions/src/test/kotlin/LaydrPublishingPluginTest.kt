import kotlin.io.path.createDirectory
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner

class LaydrPublishingPluginTest {
    @Test
    fun mixedLocalPublicationRequestsKeepRemoteSigning() {
        val remoteTask = "publishToMavenCentral"
        val remoteSigningTasks = configuredSigningTasks(listOf(remoteTask))
        assertTrue(remoteSigningTasks.isNotEmpty(), "Remote publications must have signing tasks")

        for (localTask in listOf("publishAllPublicationsToNav3VerificationRepository", "publishToMavenLocal")) {
            for (requests in listOf(
                listOf(remoteTask, localTask),
                listOf(":laydr-core:$localTask", ":laydr-core:$remoteTask"),
            )) {
                assertEquals(remoteSigningTasks, configuredSigningTasks(requests), requests.toString())
            }
        }
    }

    @Test
    fun localOnlyPublicationRequestsRemainUnsignedWithCredentials() {
        val localTasks = listOf("publishToMavenLocal", "publishAllPublicationsToNav3VerificationRepository")
        for (requests in listOf(listOf(localTasks[0]), listOf(localTasks[1]), localTasks)) {
            assertTrue(configuredSigningTasks(requests).isEmpty(), requests.toString())
        }
    }

    @Test
    fun mixedPublicationRequestsStillRequireExplicitRemoteVersion() {
        val project = laydrProject(
            taskNames = listOf("publishAllPublicationsToNav3VerificationRepository", "publishToMavenCentral"),
        )

        val failure = assertFailsWith<GradleException> {
            project.pluginManager.apply(LaydrPublishingPlugin::class.java)
        }

        assertTrue(failure.causes().any { it.message.orEmpty().contains("laydr.version is required") })
    }

    @Test
    fun verificationPublicationUsesFileRepositoryWithoutRemoteVersionRequirement() {
        val project = laydrProject(taskNames = listOf("publishAllPublicationsToNav3VerificationRepository"))
        val repository = project.layout.buildDirectory.dir("consumer-repository").get().asFile
        project.extensions.extraProperties.set("nav3VerificationRepository", repository.absolutePath)

        project.pluginManager.apply(LaydrPublishingPlugin::class.java)

        val publishing = project.extensions.getByType(org.gradle.api.publish.PublishingExtension::class.java)
        val configured = publishing.repositories.getByName("Nav3Verification") as org.gradle.api.artifacts.repositories.MavenArtifactRepository
        assertEquals(repository.toURI(), configured.url)
        assertEquals("0.3.0-SNAPSHOT", project.version.toString())
    }

    @Test
    fun usesLocalSnapshotFallbackForMavenLocalWhenVersionIsMissing() {
        val project = laydrProject(taskNames = listOf("publishToMavenLocal"))

        project.pluginManager.apply(LaydrPublishingPlugin::class.java)

        assertEquals("0.3.0-SNAPSHOT", project.version.toString())
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

    private fun configuredSigningTasks(taskNames: List<String>): List<String> {
        val directory = testProjectDir(null)
        directory.resolve("settings.gradle").writeText(
            "rootProject.name = 'publishing-test'\ninclude 'laydr-core'\n",
        )
        val module = directory.resolve("laydr-core").also { it.mkdir() }
        module.resolve("build.gradle").writeText(
            """
            plugins {
                id 'java-library'
                id 'laydr.publishing'
            }
            gradle.projectsEvaluated {
                println('LAYDR_SIGNING_TASKS=' + tasks.withType(org.gradle.plugins.signing.Sign)
                    .collect { it.name }.sort().join(','))
            }
            """.trimIndent(),
        )
        val result = GradleRunner.create()
            .withProjectDir(directory)
            .withPluginClasspath()
            .withTestKitDir(directory.resolve("test-kit"))
            .withEnvironment(
                mapOf(
                    "JAVA_HOME" to System.getProperty("java.home"),
                    "PATH" to checkNotNull(System.getenv("PATH")),
                ),
            )
            .withArguments(
                taskNames + listOf(
                    "--dry-run",
                    "--no-configuration-cache",
                    "-Playdr.version=1.2.3",
                    "-Psigning.gnupg.keyName=configuration-test",
                    "-PmavenCentralUsername=configuration-test",
                    "-PmavenCentralPassword=configuration-test",
                    "-Pnav3VerificationRepository=${directory.resolve("repository")}",
                ),
            )
            .build()

        return result.output.lineSequence()
            .single { it.startsWith("LAYDR_SIGNING_TASKS=") }
            .removePrefix("LAYDR_SIGNING_TASKS=")
            .split(',')
            .filter(String::isNotEmpty)
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
