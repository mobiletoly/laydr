package dev.goquick.laydr.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LaydrPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: Path

    @Test
    fun generateLaydrRoutesWritesConfiguredPackageSource() {
        writeConsumerProject(importGeneratedRoutes = false)

        val result = gradle("generateLaydrRoutes").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateLaydrRoutes")?.outcome)
        val generatedSource = generatedRoutesSource()
        assertTrue(generatedSource.exists(), "Expected $generatedSource to exist")
        assertTrue(generatedSource.readText().contains("package dev.goquick.consumer.generated"))
        assertTrue(generatedSource.readText().contains("public object Home : LaydrParameterlessScreenRouteRef"))
        assertTrue(generatedSource.readText().contains("public override val route: LaydrRoute"))
        assertTrue(generatedSource.readText().contains("LaydrRouteMetadata(name = \"Home\")"))
        assertTrue(generatedSource.readText().contains("public val screenRoutes: List<LaydrRoute>"))
        assertTrue(generatedSource.readText().contains("public val layoutRoutes: List<LaydrRoute>"))
        assertTrue(generatedSource.readText().contains("public val appGraph: LaydrAppGraph"))
        assertTrue(generatedSource.readText().contains("public fun path(): String"))
        assertTrue(generatedSource.readText().contains("public fun destination(): Destination"))
        assertTrue(generatedSource.readText().contains("public object Destination : LaydrScreenDestination"))
        assertTrue(generatedSource.readText().contains("internal fun requireDestination(routeMatch: LaydrRouteMatch): Destination"))
        val generatedComposeSource = generatedComposeRoutesSource()
        assertTrue(generatedComposeSource.notExists(), "Expected $generatedComposeSource not to exist")
        val generatedRouteDefSource = generatedRouteDefSource()
        assertTrue(generatedRouteDefSource.notExists(), "Expected $generatedRouteDefSource not to exist")
    }

    @Test
    fun generateLaydrRoutesWritesComposeSourcesWhenEnabled() {
        writeConsumerProject(importGeneratedRoutes = false, compose = true)

        val result = gradle("generateLaydrRoutes").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateLaydrRoutes")?.outcome)
        val generatedComposeSource = generatedComposeRoutesSource()
        assertTrue(generatedComposeSource.exists(), "Expected $generatedComposeSource to exist")
        assertTrue(generatedComposeSource.readText().contains("internal object LaydrComposeRoutes"))
        assertTrue(generatedComposeSource.readText().contains("public val definitions: LaydrComposeRouteDefinitions"))
        val generatedRouteDefSource = generatedRouteDefSource()
        assertTrue(generatedRouteDefSource.exists(), "Expected $generatedRouteDefSource to exist")
        assertTrue(generatedRouteDefSource.readText().contains("internal object LaydrRouteDef"))
        assertTrue(generatedRouteDefSource.readText().contains("content: @Composable ("))
    }

    @Test
    fun generateLaydrRoutesWritesNav3KmpHelpersWhenEnabled() {
        writeConsumerProject(importGeneratedRoutes = false, compose = true, nav3Kmp = true)

        val result = gradle("generateLaydrRoutes").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateLaydrRoutes")?.outcome)
        val generatedNavSource = generatedNavRoutesSource()
        assertTrue(generatedNavSource.exists(), "Expected $generatedNavSource to exist")
        assertTrue(generatedNavSource.readText().contains("internal object LaydrNavRoutes"))
        assertTrue(generatedNavSource.readText().contains("public fun <Data : Any> rememberSections("))
        assertTrue(generatedNavSource.readText().contains("public fun rememberStack("))
        assertTrue(generatedNavSource.readText().contains("): LaydrNavStack = rememberLaydrNavStack("))
        assertTrue(!generatedNavSource.readText().contains("remember" + "Shell"))
        assertTrue(!generatedNavSource.readText().contains("rememberStack" + "Shell"))
        assertTrue(generatedNavSource.readText().contains("public fun <Data : Any> section(sectionData: Data)"))
    }

    @Test
    fun generateLaydrRoutesWritesAndroidxNav3HelpersWhenEnabled() {
        writeConsumerProject(importGeneratedRoutes = false, compose = true, nav3Androidx = true)

        val result = gradle("generateLaydrRoutes").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateLaydrRoutes")?.outcome)
        val generatedNavSource = generatedNavRoutesSource()
        assertTrue(generatedNavSource.exists(), "Expected $generatedNavSource to exist")
        assertTrue(generatedNavSource.readText().contains("internal object LaydrNavRoutes"))
        assertTrue(generatedNavSource.readText().contains("dev.goquick.laydr.nav3.androidx"))
        assertTrue(!generatedNavSource.readText().contains("dev.goquick.laydr.nav3.kmp"))
    }

    @Test
    fun generateLaydrRoutesRejectsNav3KmpHelpersWithoutCompose() {
        assertTaskRejectsNav3HelpersWithoutCompose(
            taskName = "generateLaydrRoutes",
            nav3Kmp = true,
            expectedMessage = "Nav3 KMP helper generation requires generateCompose=true",
        )
    }

    @Test
    fun checkLaydrRoutesRejectsNav3KmpHelpersWithoutCompose() {
        assertTaskRejectsNav3HelpersWithoutCompose(
            taskName = "checkLaydrRoutes",
            nav3Kmp = true,
            expectedMessage = "Nav3 KMP helper generation requires generateCompose=true",
        )
    }

    @Test
    fun generateLaydrRoutesRejectsAndroidxNav3HelpersWithoutCompose() {
        assertTaskRejectsNav3HelpersWithoutCompose(
            taskName = "generateLaydrRoutes",
            nav3Androidx = true,
            expectedMessage = "AndroidX Nav3 helper generation requires generateCompose=true",
        )
    }

    @Test
    fun checkLaydrRoutesRejectsMultipleNav3HelperTargets() {
        writeConsumerProject(importGeneratedRoutes = false, compose = true, nav3Kmp = true, nav3Androidx = true)

        val result = gradle("checkLaydrRoutes").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":checkLaydrRoutes")?.outcome)
        assertTrue(result.output.contains("Only one Nav3 helper target can be enabled"))
    }

    @Test
    fun generatedRoutesAreCompiledFromCommonMain() {
        writeConsumerProject(importGeneratedRoutes = true)

        val result = gradle("compileKotlinJvm").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateLaydrRoutes")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlinJvm")?.outcome)
    }

    @Test
    fun androidApplicationCompilesGeneratedRoutesFromMainSourceSet() {
        assertAndroidConsumerCompilesGeneratedRoutes(pluginId = "com.android.application")
    }

    @Test
    fun androidLibraryCompilesGeneratedRoutesFromMainSourceSet() {
        assertAndroidConsumerCompilesGeneratedRoutes(pluginId = "com.android.library")
    }

    @Test
    fun androidOnlyRouteValidationFailsForMissingMainRoutesRoot() {
        writeAndroidConsumerProject(writeDefaultRoute = false)

        val result = gradle("checkLaydrRoutes").buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":checkLaydrRoutes")?.outcome)
        assertTrue(result.output.contains("src/main/kotlin/routes"))
    }

    @Test
    fun checkLaydrRoutesValidatesWithoutWritingGeneratedSource() {
        writeConsumerProject(importGeneratedRoutes = false)

        val result = gradle("checkLaydrRoutes").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkLaydrRoutes")?.outcome)
        assertTrue(generatedRoutesSource().notExists(), "Expected checkLaydrRoutes not to write generated source")
    }

    @Test
    fun checkLaydrRoutesAllowsNamespaceSegmentDirectories() {
        writeConsumerProject(importGeneratedRoutes = false, writeDefaultRoute = false, compose = true)
        writeRouteFile(
            relativePath = "src/commonMain/kotlin/routes/catalog/Route.kt",
            source = routeSource("routes.catalog", compose = true),
        )
        writeRouteFile(
            relativePath = "src/commonMain/kotlin/routes/catalog/bundle/by_activity_bundle_id/Route.kt",
            source = routeSource("routes.catalog.bundle.by_activity_bundle_id", compose = true),
        )

        val result = gradle("checkLaydrRoutes").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkLaydrRoutes")?.outcome)
    }

    @Test
    fun checkLaydrRoutesFailsForInvalidRouteTree() {
        writeConsumerProject(importGeneratedRoutes = false, writeDefaultRoute = false)
        writeRouteFile(
            relativePath = "src/commonMain/kotlin/routes/home/Route.kt",
            source =
            """
            package routes.home

            object HomeRoute
            """,
        )

        assertCheckLaydrRoutesFails("Route.kt must bind exactly one package-visible top-level Route value")
    }

    @Test
    fun checkLaydrRoutesAllowsComposeWithoutRouteAppAlias() {
        writeConsumerProject(
            importGeneratedRoutes = false,
            compose = true,
        )

        val result = gradle("checkLaydrRoutes").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkLaydrRoutes")?.outcome)
    }

    @Test
    fun checkLifecycleRunsLaydrRouteValidation() {
        writeConsumerProject(importGeneratedRoutes = false)

        val result = gradle("check").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":checkLaydrRoutes")?.outcome)
    }

    private fun writeConsumerProject(
        importGeneratedRoutes: Boolean,
        writeDefaultRoute: Boolean = true,
        compose: Boolean = false,
        nav3Kmp: Boolean = false,
        nav3Androidx: Boolean = false,
    ) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    mavenCentral()
                    google()
                    gradlePluginPortal()
                }
            }

            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    mavenCentral()
                    google()
                }
            }

            rootProject.name = "laydr-consumer"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("org.jetbrains.kotlin.multiplatform") version "$kotlinVersion"
                id("dev.goquick.laydr")
            }

            kotlin {
                jvm()

                sourceSets {
                    commonMain.dependencies {
                    }
                }
            }

            laydr {
                generatedPackage.set("dev.goquick.consumer.generated")
                ${if (compose) "compose.set(true)" else ""}
                ${if (nav3Kmp) "adapters { nav3Kmp.set(true) }" else ""}
                ${if (nav3Androidx) "adapters { nav3Androidx.set(true) }" else ""}
            }
            """.trimIndent(),
        )

        writeRouteFile(
            relativePath = "src/commonMain/kotlin/dev/goquick/laydr/core/LaydrCoreStubs.kt",
            source =
            """
            package dev.goquick.laydr.core

            class LaydrRoute(
                val id: String,
                val segments: List<LaydrRouteSegment>,
                val children: List<LaydrRoute> = emptyList(),
                val metadata: LaydrRouteMetadata = LaydrRouteMetadata(),
            ) {
                val pathTemplate: String = id
                fun buildPath(parameters: Map<String, String> = emptyMap()): String = id
                fun key(parameters: Map<String, String> = emptyMap()): LaydrRouteKey = LaydrRouteKey(id, parameters)
            }
            sealed interface LaydrRouteSegment {
                class Static(val value: String) : LaydrRouteSegment
                class Dynamic(val name: String) : LaydrRouteSegment
            }
            class LaydrRouteMetadata(
                val name: String = "",
                val labels: Map<String, String> = emptyMap(),
            )
            class LaydrRouteKey(val routeId: String, val parameters: Map<String, String> = emptyMap())
            class LaydrRouteMatch(val route: LaydrRoute, val parameters: Map<String, String>)
            class LaydrRouteMap(
                val routes: List<LaydrRoute>,
                val screenRoutes: List<LaydrRoute>,
                val layoutRoutes: List<LaydrRoute>,
            ) {
                fun pathFor(key: LaydrRouteKey): String? = key.routeId
                fun contains(parentRoute: LaydrRouteRef, key: LaydrRouteKey): Boolean = true
            }
            class LaydrAppGraph(val routeMap: LaydrRouteMap)
            interface LaydrRouteRef {
                val route: LaydrRoute
            }
            interface LaydrDestination {
                val routeKey: LaydrRouteKey
            }
            interface LaydrScreenDestination : LaydrDestination
            interface LaydrScreenRouteRef : LaydrRouteRef
            interface LaydrParameterlessScreenRouteRef : LaydrScreenRouteRef {
                val defaultDestination: LaydrScreenDestination
            }
            interface LaydrLayoutRouteRef : LaydrRouteRef
            enum class LaydrRouteKind {
                SCREEN,
                LAYOUT,
                SCREEN_AND_LAYOUT,
            }
            class LaydrRouteDeclaration private constructor(
                val kind: LaydrRouteKind,
                val name: String?,
                val labels: Map<String, String>,
            ) {
                companion object {
                    fun screen(
                        name: String? = null,
                        labels: Map<String, String> = emptyMap(),
                    ): LaydrRouteDeclaration = LaydrRouteDeclaration(LaydrRouteKind.SCREEN, name, labels)
                    fun layout(
                        name: String? = null,
                        labels: Map<String, String> = emptyMap(),
                    ): LaydrRouteDeclaration = LaydrRouteDeclaration(LaydrRouteKind.LAYOUT, name, labels)
                    fun screenAndLayout(
                        name: String? = null,
                        labels: Map<String, String> = emptyMap(),
                    ): LaydrRouteDeclaration =
                        LaydrRouteDeclaration(LaydrRouteKind.SCREEN_AND_LAYOUT, name, labels)
                }
            }
            """,
        )

        if (compose) {
            writeComposeStubs()
        }

        if (writeDefaultRoute) {
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/home/Route.kt",
                source = routeSource("routes.home", compose),
            )
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/home/Screen.kt",
                source =
                """
                package routes.home

                fun HomeScreen() = Unit
                """,
            )
        }

        if (importGeneratedRoutes) {
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/user_profile/Route.kt",
                source = routeSource("routes.user_profile", compose),
            )
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/user_profile/Screen.kt",
                source =
                """
                package routes.user_profile

                fun UserProfileScreen() = Unit
                """,
            )
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/home/by_route/Route.kt",
                source = routeSource("routes.home.by_route", compose),
            )
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/home/by_route/Screen.kt",
                source =
                """
                package routes.home.by_route

                fun ByRouteScreen() = Unit
                """,
            )
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/settings/Route.kt",
                source = routeSource("routes.settings", compose, FixtureRouteKind.LAYOUT),
            )
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/settings/Layout.kt",
                source =
                """
                package routes.settings

                fun SettingsLayout() = Unit
                """,
            )
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/settings/profile/Route.kt",
                source = routeSource("routes.settings.profile", compose),
            )
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/routes/settings/profile/Screen.kt",
                source =
                """
                package routes.settings.profile

                fun ProfileScreen() = Unit
                """,
            )
            writeRouteFile(
                relativePath = "src/commonMain/kotlin/dev/goquick/consumer/UseRoutes.kt",
                source =
                """
                package dev.goquick.consumer

                import dev.goquick.consumer.generated.LaydrRoutes

                fun homePath(): String = LaydrRoutes.Home.path()
                fun byRoutePath(route: String): String =
                    LaydrRoutes.Home.ByRoute.path(route = LaydrRoutes.Home.ByRoute.route(route))
                fun profilePath(): String = LaydrRoutes.Settings.Profile.path()
                fun userProfilePath(): String = LaydrRoutes.UserProfile.path()
                fun userProfilePathTemplate(): String = LaydrRoutes.UserProfile.route.pathTemplate
                fun homeMetadataName(): String = LaydrRoutes.Home.route.metadata.name
                fun screenRouteCount(): Int = LaydrRoutes.screenRoutes.size
                fun layoutRouteCount(): Int = LaydrRoutes.layoutRoutes.size
                """,
            )
        }
    }

    private fun writeAndroidConsumerProject(
        pluginId: String = "com.android.application",
        writeDefaultRoute: Boolean = true,
    ) {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement {
                repositories {
                    mavenCentral()
                    google()
                    gradlePluginPortal()
                }
            }

            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    mavenCentral()
                    google()
                }
            }

            rootProject.name = "laydr-android-consumer"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("$pluginId") version "$androidGradlePluginVersion"
                id("dev.goquick.laydr")
            }

            android {
                namespace = "dev.goquick.consumer"
                compileSdk = 36

                defaultConfig {
                    minSdk = 26
                }
            }

            laydr {
                generatedPackage.set("dev.goquick.consumer.generated")
            }
            """.trimIndent(),
        )

        writeRouteFile(
            relativePath = "src/main/kotlin/dev/goquick/laydr/core/LaydrCoreStubs.kt",
            source =
            """
            package dev.goquick.laydr.core

            class LaydrRoute(
                val id: String,
                val segments: List<LaydrRouteSegment>,
                val children: List<LaydrRoute> = emptyList(),
                val metadata: LaydrRouteMetadata = LaydrRouteMetadata(),
            ) {
                val pathTemplate: String = id
                fun buildPath(parameters: Map<String, String> = emptyMap()): String = id
                fun key(parameters: Map<String, String> = emptyMap()): LaydrRouteKey = LaydrRouteKey(id, parameters)
            }
            sealed interface LaydrRouteSegment {
                class Static(val value: String) : LaydrRouteSegment
                class Dynamic(val name: String) : LaydrRouteSegment
            }
            class LaydrRouteMetadata(
                val name: String = "",
                val labels: Map<String, String> = emptyMap(),
            )
            class LaydrRouteKey(val routeId: String, val parameters: Map<String, String> = emptyMap())
            class LaydrRouteMatch(val route: LaydrRoute, val parameters: Map<String, String>)
            class LaydrRouteMap(
                val routes: List<LaydrRoute>,
                val screenRoutes: List<LaydrRoute>,
                val layoutRoutes: List<LaydrRoute>,
            ) {
                fun pathFor(key: LaydrRouteKey): String? = key.routeId
                fun contains(parentRoute: LaydrRouteRef, key: LaydrRouteKey): Boolean = true
            }
            class LaydrAppGraph(val routeMap: LaydrRouteMap)
            interface LaydrRouteRef {
                val route: LaydrRoute
            }
            interface LaydrDestination {
                val routeKey: LaydrRouteKey
            }
            interface LaydrScreenDestination : LaydrDestination
            interface LaydrScreenRouteRef : LaydrRouteRef
            interface LaydrParameterlessScreenRouteRef : LaydrScreenRouteRef {
                val defaultDestination: LaydrScreenDestination
            }
            interface LaydrLayoutRouteRef : LaydrRouteRef
            enum class LaydrRouteKind {
                SCREEN,
                LAYOUT,
                SCREEN_AND_LAYOUT,
            }
            class LaydrRouteDeclaration private constructor(
                val kind: LaydrRouteKind,
                val name: String?,
                val labels: Map<String, String>,
            ) {
                companion object {
                    fun screen(
                        name: String? = null,
                        labels: Map<String, String> = emptyMap(),
                    ): LaydrRouteDeclaration = LaydrRouteDeclaration(LaydrRouteKind.SCREEN, name, labels)
                    fun layout(
                        name: String? = null,
                        labels: Map<String, String> = emptyMap(),
                    ): LaydrRouteDeclaration = LaydrRouteDeclaration(LaydrRouteKind.LAYOUT, name, labels)
                    fun screenAndLayout(
                        name: String? = null,
                        labels: Map<String, String> = emptyMap(),
                    ): LaydrRouteDeclaration =
                        LaydrRouteDeclaration(LaydrRouteKind.SCREEN_AND_LAYOUT, name, labels)
                }
            }
            """,
        )
        if (writeDefaultRoute) {
            writeRouteFile(
                relativePath = "src/main/kotlin/routes/home/Route.kt",
                source = routeSource("routes.home", compose = false),
            )
            writeRouteFile(
                relativePath = "src/main/kotlin/dev/goquick/consumer/UseRoutes.kt",
                source =
                """
                package dev.goquick.consumer

                import dev.goquick.consumer.generated.LaydrRoutes

                fun homePath(): String = LaydrRoutes.Home.path()
                """,
            )
        }
    }

    private fun writeRouteFile(
        relativePath: String,
        source: String,
    ) {
        val file = projectDir.resolve(relativePath)
        file.parent.createDirectories()
        file.writeText(source.trimIndent())
    }

    private fun routeSource(
        packageName: String,
        compose: Boolean,
        kind: FixtureRouteKind = FixtureRouteKind.SCREEN,
    ): String =
        when {
            compose && kind == FixtureRouteKind.SCREEN -> {
                """
                package $packageName

                val Route = LaydrRouteDef.screen { _ -> Unit }
                """
            }
            compose && kind == FixtureRouteKind.LAYOUT -> {
                """
                package $packageName

                val Route = LaydrRouteDef.layout { _, content -> content() }
                """
            }
            !compose && kind == FixtureRouteKind.SCREEN -> {
                """
                package $packageName

                import dev.goquick.laydr.core.LaydrRouteDeclaration

                val Route = LaydrRouteDeclaration.screen()
                """
            }
            else -> {
                """
                package $packageName

                import dev.goquick.laydr.core.LaydrRouteDeclaration

                val Route = LaydrRouteDeclaration.layout()
                """
            }
        }

    private fun writeComposeStubs() {
        writeRouteFile(
            relativePath = "src/commonMain/kotlin/androidx/compose/runtime/Composable.kt",
            source =
            """
            package androidx.compose.runtime

            @Target(
                AnnotationTarget.FUNCTION,
                AnnotationTarget.TYPE,
            )
            annotation class Composable
            """,
        )

        writeRouteFile(
            relativePath = "src/commonMain/kotlin/dev/goquick/laydr/compose/LaydrComposeStubs.kt",
            source =
            """
            package dev.goquick.laydr.compose

            import androidx.compose.runtime.Composable
            import dev.goquick.laydr.core.LaydrRoute
            import dev.goquick.laydr.core.LaydrRouteMap
            import dev.goquick.laydr.core.LaydrRouteMatch

            class LaydrScreenContent(val content: @Composable () -> Unit)
            class LaydrLayoutContext
            class LaydrComposeScreenRouteDefinition(
                val route: LaydrRoute,
                val content: @Composable (LaydrRouteMatch) -> LaydrScreenContent,
            )
            class LaydrComposeLayoutRouteDefinition(
                val route: LaydrRoute,
                val content: @Composable (LaydrLayoutContext, @Composable () -> Unit) -> Unit,
            )
            class LaydrComposeScreenAndLayoutRouteDefinition(
                val screenDefinition: LaydrComposeScreenRouteDefinition,
                val layoutDefinition: LaydrComposeLayoutRouteDefinition,
            )
            class LaydrComposeRouteDefinitions(
                val routeMap: LaydrRouteMap,
                val screenDefinitions: List<LaydrComposeScreenRouteDefinition>,
                val layoutDefinitions: List<LaydrComposeLayoutRouteDefinition>,
            )
            """,
        )
    }

    private fun generatedRoutesSource(): Path =
        generatedSource("commonMain/kotlin/dev/goquick/consumer/generated/LaydrRoutes.kt")

    private fun generatedAndroidRoutesSource(): Path =
        generatedSource("main/kotlin/dev/goquick/consumer/generated/LaydrRoutes.kt")

    private fun generatedComposeRoutesSource(): Path =
        generatedSource("commonMain/kotlin/dev/goquick/consumer/generated/LaydrComposeRoutes.kt")

    private fun generatedNavRoutesSource(): Path =
        generatedSource("commonMain/kotlin/dev/goquick/consumer/generated/LaydrNavRoutes.kt")

    private fun generatedRouteDefSource(): Path =
        generatedSource("commonMain/kotlin/routes/home/LaydrRouteDef.kt")

    private fun generatedSource(relativePath: String): Path =
        projectDir.resolve("build/generated/laydr/$relativePath")

    private fun gradle(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withArguments(arguments.toList() + "--stacktrace")
            .withPluginClasspath()

    private fun assertCheckLaydrRoutesFails(vararg expectedMessages: String) {
        val result = gradle("checkLaydrRoutes").buildAndFail()
        assertEquals(TaskOutcome.FAILED, result.task(":checkLaydrRoutes")?.outcome)
        expectedMessages.forEach { expectedMessage ->
            assertTrue(result.output.contains(expectedMessage))
        }
    }

    private fun assertTaskRejectsNav3HelpersWithoutCompose(
        taskName: String,
        nav3Kmp: Boolean = false,
        nav3Androidx: Boolean = false,
        expectedMessage: String,
    ) {
        writeConsumerProject(
            importGeneratedRoutes = false,
            nav3Kmp = nav3Kmp,
            nav3Androidx = nav3Androidx,
        )

        val result = gradle(taskName).buildAndFail()

        assertEquals(TaskOutcome.FAILED, result.task(":$taskName")?.outcome)
        assertTrue(result.output.contains(expectedMessage))
    }

    private fun assertAndroidConsumerCompilesGeneratedRoutes(pluginId: String) {
        writeAndroidConsumerProject(pluginId = pluginId)

        val result = gradle("compileDebugKotlin").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateLaydrRoutes")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":compileDebugKotlin")?.outcome)
        val generatedSource = generatedAndroidRoutesSource()
        assertTrue(generatedSource.exists(), "Expected $generatedSource to exist")
    }

    private companion object {
        private const val kotlinVersion = "2.4.0"
        private const val androidGradlePluginVersion = "9.2.1"
    }

    private enum class FixtureRouteKind {
        SCREEN,
        LAYOUT,
    }
}
