package dev.goquick.laydr.codegen

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LaydrCodegenTest {
    @Test
    fun scansSimpleRoute(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "home")

        val tree = RouteDirectoryScanner().scan(routesRoot, requirePackageNames = true)

        assertEquals(routesRoot.toAbsolutePath().normalize(), tree.routesRoot)
        val home = tree.onlyRoute()
        assertEquals("/home", home.routePath)
        assertEquals("HomeRoute", home.routeName)
        assertEquals(routesRoot.resolve("home"), home.directory)
        assertEquals(routesRoot.resolve("home/Route.kt"), home.routeFile)
        assertEquals("routes.home", home.routePackageName)
        assertEquals(RouteDirectoryRouteKind.SCREEN, home.routeKind)
        assertEquals(emptyList(), home.children)
    }

    @Test
    fun scansScreenWithLayoutValuesAsScreenRoute(): Unit = withRoutesRoot { routesRoot ->
        val home = routesRoot.resolve("home")
        Files.createDirectories(home)
        Files.writeString(
            home.resolve("Route.kt"),
            """
            package routes.home

            internal val Route = LaydrRouteDef.screenWithLayoutValues { _ ->
                TODO()
            }
            """.trimIndent(),
        )

        val route = RouteDirectoryScanner()
            .scan(routesRoot, requirePackageNames = true, generateCompose = true)
            .onlyRoute()

        assertEquals(RouteDirectoryRouteKind.SCREEN, route.routeKind)
    }

    @Test
    fun scansRouteMetadataFromRouteDeclaration(): Unit = withRoutesRoot { routesRoot ->
        val product = routesRoot.resolve("product")
        Files.createDirectories(product)
        Files.writeString(
            product.resolve("Route.kt"),
            """
            package routes.product

            internal val Route = LaydrRouteDeclaration.screen(
                name = "Product detail",
                labels = mapOf("area" to "shop", "priority" to "primary"),
            )
            """,
        )

        val route = RouteDirectoryScanner()
            .scan(routesRoot, requirePackageNames = true)
            .onlyRoute()

        assertEquals("Product detail", route.metadataName)
        assertEquals(
            mapOf("area" to "shop", "priority" to "primary"),
            route.metadataLabels,
        )
    }

    @Test
    fun generatesRouteMetadataFromRouteDeclaration(): Unit = withRoutesRoot { routesRoot ->
        val product = routesRoot.resolve("product")
        Files.createDirectories(product)
        Files.writeString(
            product.resolve("Route.kt"),
            """
            package routes.product

            internal val Route = LaydrRouteDeclaration.screen(
                name = "Product detail",
                labels = mapOf("area" to "shop"),
            )
            """,
        )

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(
            source,
            """metadata = LaydrRouteMetadata(name = "Product detail", labels = mapOf("area" to "shop"))""",
        )
    }

    @Test
    fun rejectsDynamicRouteMetadataExpressions(): Unit = withRoutesRoot { routesRoot ->
        val product = routesRoot.resolve("product")
        Files.createDirectories(product)
        Files.writeString(
            product.resolve("Route.kt"),
            """
            package routes.product

            internal val routeName = "Product detail"
            internal val Route = LaydrRouteDeclaration.screen(name = routeName)
            """,
        )

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Route metadata name must be a string literal")
        assertMessageContains(error, product.resolve("Route.kt").toString())
    }

    @Test
    fun rejectsDynamicRouteMetadataLabels(): Unit = withRoutesRoot { routesRoot ->
        val product = routesRoot.resolve("product")
        Files.createDirectories(product)
        Files.writeString(
            product.resolve("Route.kt"),
            """
            package routes.product

            internal val routeLabels = mapOf("area" to "shop")
            internal val Route = LaydrRouteDef.screen(labels = routeLabels) { _ -> Unit }
            """,
        )

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot, generateCompose = true)
        }

        assertMessageContains(error, "Route metadata labels must be mapOf string pairs")
        assertMessageContains(error, product.resolve("Route.kt").toString())
    }

    @Test
    fun rejectsDynamicPositionalRouteMetadataLabels(): Unit = withRoutesRoot { routesRoot ->
        val product = routesRoot.resolve("product")
        Files.createDirectories(product)
        Files.writeString(
            product.resolve("Route.kt"),
            """
            package routes.product

            internal val routeLabels = mapOf("area" to "shop")
            internal val Route = LaydrRouteDef.screen("Product", routeLabels, content = ::ProductScreen)
            """,
        )

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot, generateCompose = true)
        }

        assertMessageContains(error, "Route metadata labels must be mapOf string pairs")
        assertMessageContains(error, product.resolve("Route.kt").toString())
    }

    @Test
    fun rejectsStringTemplateRouteMetadataName(): Unit = withRoutesRoot { routesRoot ->
        val product = routesRoot.resolve("product")
        Files.createDirectories(product)
        Files.writeString(
            product.resolve("Route.kt"),
            """
            package routes.product

            internal val suffix = "detail"
            internal val Route = LaydrRouteDeclaration.screen(name = "Product ${'$'}suffix")
            """,
        )

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Route metadata name must be a string literal")
        assertMessageContains(error, product.resolve("Route.kt").toString())
    }

    @Test
    fun rejectsStringTemplateRouteMetadataLabelValues(): Unit = withRoutesRoot { routesRoot ->
        val product = routesRoot.resolve("product")
        Files.createDirectories(product)
        Files.writeString(
            product.resolve("Route.kt"),
            """
            package routes.product

            internal val suffix = "shop"
            internal val Route = LaydrRouteDeclaration.screen(
                labels = mapOf("area" to "Product ${'$'}suffix"),
            )
            """,
        )

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Route metadata label values must be string literals")
        assertMessageContains(error, product.resolve("Route.kt").toString())
    }

    @Test
    fun scansLayoutRouteWithoutScreen(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "settings", screen = false, layout = true)
        createRoute(routesRoot, "settings/profile")

        val settings = RouteDirectoryScanner().scan(routesRoot).onlyRoute()
        val profile = settings.onlyChild()

        assertEquals("/settings", settings.routePath)
        assertEquals(RouteDirectoryRouteKind.LAYOUT, settings.routeKind)
        assertEquals(RouteDirectoryRouteKind.SCREEN, profile.routeKind)
    }

    @Test
    fun scansLayoutRouteWithNamespaceSegmentDescendant(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "settings", screen = false, layout = true)
        createRoute(routesRoot, "settings/profile/details")

        val settings = RouteDirectoryScanner().scan(routesRoot).onlyRoute()
        val profile = settings.onlyChild()
        val details = profile.onlyChild()

        assertEquals(RouteDirectoryRouteKind.LAYOUT, settings.routeKind)
        assertEquals(false, profile.isDeclaredRoute)
        assertEquals("/settings/profile", profile.routePath)
        assertEquals("/settings/profile/details", details.routePath)
        assertEquals(RouteDirectoryRouteKind.SCREEN, details.routeKind)
    }

    @Test
    fun generatesSimpleRouteGraph(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "home")

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public val routes: List<LaydrRoute> = listOf(Home.route)")
        assertSourceContains(source, "public val routeMap: LaydrRouteMap =")
        assertSourceContains(source, "public val appGraph: LaydrAppGraph = LaydrAppGraph(routeMap = routeMap)")
        assertSourceContains(source, "public object Home : LaydrParameterlessScreenRouteRef")
        assertSourceContains(source, "public override val route: LaydrRoute =")
        assertSourceContains(source, "public fun matches(key: LaydrRouteKey): Boolean")
        assertSourceContains(source, "public fun contains(key: LaydrRouteKey): Boolean")
        assertSourceContains(source, "public fun path(): String = route.buildPath()")
        assertSourceContains(source, "public fun destination(): Destination = Destination")
        assertSourceContains(source, "public override val defaultDestination: Destination = Destination")
        assertSourceContains(source, "internal fun requireDestination(routeMatch: LaydrRouteMatch): Destination")
        assertSourceContains(source, "public object Destination : LaydrScreenDestination")
        assertSourceContains(source, "public override val routeKey: LaydrRouteKey = LaydrRoutes.Home.route.key()")
        assertSourceContains(source, "public val path: String = LaydrRoutes.Home.route.buildPath()")
        assertSourceDoesNotContain(source, "public fun key(): LaydrRouteKey")
        assertSourceDoesNotContain(source, "public fun match(routeMatch: LaydrRouteMatch)")
        assertSourceDoesNotContain(source, "public fun requireMatch(routeMatch: LaydrRouteMatch)")
    }

    @Test
    fun generatesNonCollidingDestinationTypeWhenChildRoutesUseDestinationNames(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "home")
        createRoute(routesRoot, "home/destination")
        createRoute(routesRoot, "home/route_destination")

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public object Destination : LaydrParameterlessScreenRouteRef")
        assertSourceContains(source, "public object RouteDestination : LaydrParameterlessScreenRouteRef")
        assertSourceContains(source, "public fun destination(): RouteDestination2 = RouteDestination2")
        assertSourceContains(source, "public override val defaultDestination: RouteDestination2 = RouteDestination2")
        assertSourceContains(
            source,
            "internal fun requireDestination(routeMatch: LaydrRouteMatch): RouteDestination2",
        )
        assertSourceContains(source, "public object RouteDestination2 : LaydrScreenDestination")
    }

    @Test
    fun scansNestedRoutes(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "settings")
        createRoute(routesRoot, "settings/profile")

        val settings = RouteDirectoryScanner().scan(routesRoot).onlyRoute()
        val profile = settings.onlyChild()

        assertEquals("/settings", settings.routePath)
        assertEquals("SettingsRoute", settings.routeName)
        assertEquals("/settings/profile", profile.routePath)
        assertEquals("SettingsProfileRoute", profile.routeName)
    }

    @Test
    fun scansNamespaceSegmentDirectoriesWithoutRouteDeclarations(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "catalog")
        createRoute(routesRoot, "catalog/bundle/by_activity_bundle_id")

        val catalog = RouteDirectoryScanner().scan(routesRoot).onlyRoute()
        val bundle = catalog.onlyChild()
        val detail = bundle.onlyChild()

        assertEquals("/catalog", catalog.routePath)
        assertEquals(RouteDirectoryRouteKind.SCREEN, catalog.routeKind)
        assertEquals("/catalog/bundle", bundle.routePath)
        assertEquals(null, bundle.routeFile)
        assertEquals(null, bundle.routeKind)
        assertEquals(false, bundle.isDeclaredRoute)
        assertEquals("/catalog/bundle/{activity_bundle_id}", detail.routePath)
        assertEquals("CatalogBundleByActivityBundleIdRoute", detail.routeName)
        assertEquals(RouteDirectoryRouteKind.SCREEN, detail.routeKind)
    }

    @Test
    fun scansStaticSnakeCaseRouteAsHyphenatedPath(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "settings")
        createRoute(routesRoot, "settings/user_profile")

        val settings = RouteDirectoryScanner().scan(routesRoot).onlyRoute()
        val profile = settings.onlyChild()

        assertEquals("/settings/user-profile", profile.routePath)
        assertEquals("SettingsUserProfileRoute", profile.routeName)
        assertEquals("user_profile", profile.segments.last().sourceName)
        assertEquals("user-profile", profile.segments.last().pathPart)
    }

    @Test
    fun generatesNestedRouteGraph(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "settings")
        createRoute(routesRoot, "settings/profile")

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public val screenRoutes: List<LaydrRoute> = listOf(Settings.route, Settings.Profile.route)")
        assertSourceContains(source, "children = listOf(Profile.route)")
        assertSourceContains(source, "public object Settings : LaydrParameterlessScreenRouteRef")
        assertSourceContains(source, "public object Profile : LaydrParameterlessScreenRouteRef")
        assertSourceContains(source, "Expected route match for Settings.Profile but was ")
    }

    @Test
    fun generatesNamespaceSegmentRouteGraph(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "catalog")
        createRoute(routesRoot, "catalog/bundle/by_activity_bundle_id")

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public val routes: List<LaydrRoute> = listOf(Catalog.route)")
        assertSourceContains(source, "listOf(Catalog.route, Catalog.Bundle.ByActivityBundleId.route)")
        assertSourceContains(source, "children = listOf(Bundle.ByActivityBundleId.route)")
        assertSourceContains(source, "public object Bundle {")
        assertSourceContains(source, "public object ByActivityBundleId : LaydrScreenRouteRef")
        assertSourceContains(source, "Expected route match for Catalog.Bundle.ByActivityBundleId but was ")
        assertSourceDoesNotContain(source, "public object Bundle :")
        assertSourceDoesNotContain(source, "Expected route match for Catalog.Bundle but was ")
    }

    @Test
    fun generatesLayoutRouteGraph(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "settings", screen = false, layout = true)
        createRoute(routesRoot, "settings/profile")

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public val screenRoutes: List<LaydrRoute> = listOf(Settings.Profile.route)")
        assertSourceContains(source, "public val layoutRoutes: List<LaydrRoute> = listOf(Settings.route)")
        assertSourceContains(source, "public object Settings : LaydrLayoutRouteRef")
        assertSourceContains(source, "public object Profile : LaydrParameterlessScreenRouteRef")
        assertTrue(!source.contains("public object Settings : LaydrParameterlessScreenRouteRef"))
        assertTrue(!source.contains("public object Settings : LaydrScreenRouteRef"))
        assertTrue(!source.contains("public fun destination(): Destination = Destination\n\n    public object Profile"))
    }

    @Test
    fun generatesStaticSnakeCaseRouteGraphWithHyphenatedPath(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "user_profile")

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public object UserProfile : LaydrParameterlessScreenRouteRef")
        assertSourceContains(source, """LaydrRouteSegment.Static("user-profile")""")
        assertSourceContains(source, "public fun destination(): Destination = Destination")
        assertSourceContains(source, "public override val defaultDestination: Destination = Destination")
        assertSourceContains(source, "public object Destination : LaydrScreenDestination")
    }

    @Test
    fun generatesScreenAndLayoutRouteGraph(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "settings", layout = true)

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public object Settings : LaydrParameterlessScreenRouteRef, LaydrLayoutRouteRef")
        assertSourceContains(source, "public fun destination(): Destination = Destination")
        assertSourceContains(source, "public override val defaultDestination: Destination = Destination")
        assertSourceContains(source, "internal fun requireDestination(routeMatch: LaydrRouteMatch): Destination")
    }

    @Test
    fun generatesRouteOwnedComposeSources(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "contacts", layout = true, composeDeclaration = true)
        createRoute(routesRoot, "contacts/by_id", composeDeclaration = true)

        val sources = LaydrCodegen.generateSources(
            routesDirectory = routesRoot,
            generatedPackage = "dev.goquick.laydr.generated",
            generateCompose = true,
        )
        val sourcesByPath = sources.associateBy { source -> source.relativePath }

        val aggregate = sourcesByPath.getValue("dev/goquick/laydr/generated/LaydrComposeRoutes.kt").content
        assertSourceContains(aggregate, "internal object LaydrComposeRoutes")
        assertSourceContains(aggregate, "public val definitions: LaydrComposeRouteDefinitions")
        assertSourceContains(aggregate, "routeMap = LaydrRoutes.routeMap")
        assertSourceContains(aggregate, "screenDefinitions = listOf(routes.contacts.Route.screenDefinition, routes.contacts.by_id.Route)")
        assertSourceContains(aggregate, "layoutDefinitions = listOf(routes.contacts.Route.layoutDefinition)")

        val contactsHelper = sourcesByPath.getValue("routes/contacts/LaydrRouteDef.kt").content
        assertSourceContains(contactsHelper, "internal object LaydrRouteDef")
        assertSourceContains(contactsHelper, "public fun screen(")
        assertSourceContains(contactsHelper, "public fun screenWithLayoutValues(")
        assertSourceContains(contactsHelper, "public fun screenAndLayout(")
        assertSourceContains(contactsHelper, "public class LaydrScreenAndLayoutRouteDefBuilder")
        assertSourceContains(contactsHelper, "import androidx.compose.runtime.Composable")
        assertSourceContains(contactsHelper, "content: @Composable (route: LaydrRoutes.Contacts.Destination) -> Unit")
        assertSourceContains(
            contactsHelper,
            "content: @Composable (route: LaydrRoutes.Contacts.Destination) -> LaydrScreenContent",
        )
        assertSourceContains(contactsHelper, "route: LaydrRoutes.Contacts.Destination")
        assertSourceContains(contactsHelper, "LaydrRoutes.Contacts.requireDestination(match)")
        assertSourceContains(contactsHelper, "LaydrScreenContent { content(route) }")
        assertSourceDoesNotContain(contactsHelper, "import routes." + "LaydrApp")
        assertSourceDoesNotContain(contactsHelper, "app: " + "LaydrApp")
        assertSourceDoesNotContain(contactsHelper, "<reified App : Any>")
        assertSourceDoesNotContain(contactsHelper, "app as App")

        val byIdHelper = sourcesByPath.getValue("routes/contacts/by_id/LaydrRouteDef.kt").content
        assertSourceContains(byIdHelper, "internal object LaydrRouteDef")
        assertSourceContains(byIdHelper, "public fun screen(")
        assertSourceContains(byIdHelper, "public fun screenWithLayoutValues(")
        assertSourceContains(byIdHelper, "content: @Composable (route: LaydrRoutes.Contacts.ById.Destination) -> Unit")
        assertSourceContains(
            byIdHelper,
            "content: @Composable (route: LaydrRoutes.Contacts.ById.Destination) -> LaydrScreenContent",
        )
        assertSourceContains(byIdHelper, "route: LaydrRoutes.Contacts.ById.Destination")
        assertSourceContains(byIdHelper, "LaydrRoutes.Contacts.ById.requireDestination(match)")
        assertSourceContains(byIdHelper, "LaydrScreenContent { content(route) }")
        assertSourceDoesNotContain(byIdHelper, "app: " + "LaydrApp")
        assertSourceDoesNotContain(byIdHelper, "<reified App : Any>")
        assertSourceDoesNotContain(byIdHelper, "app as App")
    }

    @Test
    fun generateSourcesDefaultsToCoreOnlySources(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "contacts", layout = true)
        createRoute(routesRoot, "contacts/by_id")

        val sources = LaydrCodegen.generateSources(
            routesDirectory = routesRoot,
            generatedPackage = "dev.goquick.laydr.generated",
        )

        assertEquals(
            listOf("dev/goquick/laydr/generated/LaydrRoutes.kt"),
            sources.map { source -> source.relativePath },
        )
        assertTrue(!sources.single().content.contains("androidx.compose.runtime"))
        assertTrue(!sources.single().content.contains("LaydrComposeRoutes"))
    }

    @Test
    fun generatesNav3KmpHelperSourcesWhenEnabled(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "contacts", composeDeclaration = true)
        createRoute(routesRoot, "contacts/by_id", composeDeclaration = true)
        createRoute(routesRoot, "profile", composeDeclaration = true)

        val sources = LaydrCodegen.generateSources(
            routesDirectory = routesRoot,
            generatedPackage = "dev.goquick.laydr.generated",
            generateCompose = true,
            generateNav3Kmp = true,
        )
        val sourcesByPath = sources.associateBy { source -> source.relativePath }
        val source = sourcesByPath.getValue("dev/goquick/laydr/generated/LaydrNavRoutes.kt").content

        assertSourceContains(source, "internal object LaydrNavRoutes")
        assertSourceContains(source, "public fun <Data : Any> rememberSections(")
        assertSourceContains(source, "): LaydrNavSections<Data> = rememberLaydrNavSections(routeDefinitions = LaydrComposeRoutes.definitions")
        assertSourceContains(source, "public fun rememberStack(")
        assertSourceContains(source, "initialDestination: LaydrScreenDestination")
        assertSourceContains(source, "backStack: NavBackStack<NavKey>")
        assertSourceContains(source, "savedStateConfiguration: SavedStateConfiguration = laydrNavSavedStateConfiguration()")
        assertSourceContains(source, "entryMetadata: LaydrNavEntryMetadataProvider = LaydrNavEntryMetadataProvider { LaydrNavEntryMetadata.Empty }")
        assertSourceContains(source, "): LaydrNavStack = rememberLaydrNavStack(routeDefinitions = LaydrComposeRoutes.definitions")
        assertSourceContains(source, "backStack = backStack")
        assertSourceContains(source, "public object Contacts")
        assertSourceContains(source, "public fun <Data : Any> section(sectionData: Data): LaydrNavSectionSpec<Data>")
        assertSourceContains(source, "laydrNavSection(LaydrRoutes.Contacts, sectionData)")
        assertSourceContains(
            source,
            "public fun <Data : Any> section(rootDestination: LaydrRoutes.Contacts.ById.Destination, sectionData: Data)",
        )
        assertSourceContains(
            source,
            "laydrNavSection(LaydrRoutes.Contacts.ById, rootDestination, sectionData)",
        )
    }

    @Test
    fun generatesAndroidxNav3HelperSourcesWhenEnabled(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "contacts", composeDeclaration = true)
        createRoute(routesRoot, "contacts/by_id", composeDeclaration = true)
        createRoute(routesRoot, "profile", composeDeclaration = true)

        val sources = LaydrCodegen.generateSources(
            routesDirectory = routesRoot,
            generatedPackage = "dev.goquick.laydr.generated",
            generateCompose = true,
            generateNav3Androidx = true,
        )
        val sourcesByPath = sources.associateBy { source -> source.relativePath }
        val source = sourcesByPath.getValue("dev/goquick/laydr/generated/LaydrNavRoutes.kt").content

        assertSourceContains(source, "internal object LaydrNavRoutes")
        assertSourceContains(source, "import dev.goquick.laydr.nav3.androidx.LaydrNavSections")
        assertSourceContains(source, "import dev.goquick.laydr.nav3.androidx.LaydrNavStack")
        assertSourceContains(source, "import dev.goquick.laydr.nav3.androidx.rememberLaydrNavSections")
        assertSourceContains(source, "import androidx.navigation3.runtime.NavBackStack")
        assertSourceContains(source, "public fun <Data : Any> rememberSections(")
        assertSourceContains(source, "public fun rememberStack(")
        assertSourceContains(source, "backStack: NavBackStack<NavKey>")
        assertSourceContains(source, "): LaydrNavStack = rememberLaydrNavStack(routeDefinitions = LaydrComposeRoutes.definitions")
        assertSourceContains(source, "public object Contacts")
        assertSourceContains(source, "public fun <Data : Any> section(sectionData: Data): LaydrNavSectionSpec<Data>")
        assertSourceContains(source, "laydrNavSection(LaydrRoutes.Contacts, sectionData)")
        assertSourceDoesNotContain(source, "dev.goquick.laydr.nav3.kmp")
        assertSourceDoesNotContain(source, "SnapshotStateList")
        assertSourceDoesNotContain(source, "SavedStateConfiguration")
        assertSourceDoesNotContain(source, "laydrNavSavedStateConfiguration")
    }

    @Test
    fun generateSourcesDoesNotEmitNav3KmpHelpersByDefault(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "contacts", composeDeclaration = true)

        val sources = LaydrCodegen.generateSources(
            routesDirectory = routesRoot,
            generatedPackage = "dev.goquick.laydr.generated",
            generateCompose = true,
        )

        assertTrue(sources.none { source -> source.relativePath.endsWith("LaydrNavRoutes.kt") })
    }

    @Test
    fun generateSourcesRequiresComposeWhenNav3KmpHelpersAreEnabled(): Unit = withRoutesRoot { routesRoot ->
        assertNav3HelperRequiresCompose(
            routesRoot = routesRoot,
            generateNav3Kmp = true,
            expectedMessage = "Nav3 KMP helper generation requires generateCompose=true",
        )
    }

    @Test
    fun generateSourcesRequiresComposeWhenAndroidxNav3HelpersAreEnabled(): Unit = withRoutesRoot { routesRoot ->
        assertNav3HelperRequiresCompose(
            routesRoot = routesRoot,
            generateNav3Androidx = true,
            expectedMessage = "AndroidX Nav3 helper generation requires generateCompose=true",
        )
    }

    @Test
    fun generateSourcesRejectsMultipleNav3HelperTargets(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "contacts", composeDeclaration = true)

        val error = assertFailsWith<RouteGraphGeneratorException> {
            LaydrCodegen.generateSources(
                routesDirectory = routesRoot,
                generatedPackage = "dev.goquick.laydr.generated",
                generateCompose = true,
                generateNav3Kmp = true,
                generateNav3Androidx = true,
            )
        }

        assertMessageContains(error, "Only one Nav3 helper target can be enabled")
    }

    @Test
    fun generateSourcesAllowsPackageLessCoreDeclarationsWhenComposeIsDisabled(): Unit = withRoutesRoot { routesRoot ->
        val home = routesRoot.resolve("home")
        Files.createDirectories(home)
        Files.writeString(home.resolve("Route.kt"), "internal val Route = LaydrRouteDeclaration.screen()\n")

        val sources = LaydrCodegen.generateSources(
            routesDirectory = routesRoot,
            generatedPackage = "dev.goquick.laydr.generated",
            generateCompose = false,
        )

        assertEquals(
            listOf("dev/goquick/laydr/generated/LaydrRoutes.kt"),
            sources.map { source -> source.relativePath },
        )
    }

    @Test
    fun generateSourcesRequiresPackageDeclarationsWhenComposeIsEnabled(): Unit = withRoutesRoot { routesRoot ->
        val home = routesRoot.resolve("home")
        Files.createDirectories(home)
        Files.writeString(home.resolve("Route.kt"), "internal val Route = LaydrRouteDef.screen { _ -> Unit }\n")

        val error = assertFailsWith<RouteDirectoryScannerException> {
            LaydrCodegen.generateSources(
                routesDirectory = routesRoot,
                generatedPackage = "dev.goquick.laydr.generated",
                generateCompose = true,
            )
        }

        assertMessageContains(error, "Route.kt is missing package declaration")
        assertMessageContains(error, home.resolve("Route.kt").toString())
    }

    @Test
    fun scansDynamicByNameRoute(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "users")
        createRoute(routesRoot, "users/by_user_id")

        val dynamicRoute = RouteDirectoryScanner()
            .scan(routesRoot)
            .onlyRoute()
            .onlyChild()

        assertEquals("/users/{user_id}", dynamicRoute.routePath)
        assertEquals("UsersByUserIdRoute", dynamicRoute.routeName)
        assertEquals(
            RouteDirectorySegment.Dynamic(
                sourceName = "by_user_id",
                parameterName = "user_id",
                kotlinParameterName = "userId",
            ),
            dynamicRoute.segments.last(),
        )
    }

    @Test
    fun generatesSemanticDynamicKotlinParameters(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "users")
        createRoute(routesRoot, "users/by_user_id")

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public object ByUserId : LaydrScreenRouteRef")
        assertSourceContains(source, "@JvmInline")
        assertSourceContains(source, "public value class UserIdParam(")
        assertSourceContains(source, "public fun userId(rawValue: String): UserIdParam = UserIdParam(rawValue)")
        assertSourceContains(
            source,
            """public fun path(userId: UserIdParam): String = this.route.buildPath(mapOf("user_id" to userId.value))""",
        )
        assertSourceContains(
            source,
            """public fun destination(userId: UserIdParam): Destination = Destination(userId = userId)""",
        )
        assertSourceContains(source, "public val userId: UserIdParam,")
        assertSourceContains(
            source,
            """LaydrRoutes.Users.ByUserId.route.key(mapOf("user_id" to userId.value))""",
        )
        assertSourceContains(
            source,
            """return Destination(userId = UserIdParam(routeMatch.parameters.getValue("user_id")))""",
        )
        assertSourceDoesNotContain(source, "user_id: String")
    }

    @Test
    fun generatesDynamicRouteGraph(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "users")
        createRoute(routesRoot, "users/by_id")

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public object Users : LaydrParameterlessScreenRouteRef")
        assertSourceContains(source, "public object ById : LaydrScreenRouteRef")
        assertSourceContains(source, "public override val defaultDestination: Destination = Destination")
        assertSourceContains(source, "public value class IdParam(")
        assertSourceContains(source, "public fun id(rawValue: String): IdParam = IdParam(rawValue)")
        assertSourceContains(source, """public fun path(id: IdParam): String = this.route.buildPath(mapOf("id" to id.value))""")
        assertSourceContains(source, """public fun destination(id: IdParam): Destination = Destination(id = id)""")
        assertSourceContains(source, "public data class Destination(")
        assertSourceContains(source, "public val id: IdParam,")
        assertSourceContains(source, """public override val routeKey: LaydrRouteKey =""")
        assertSourceContains(source, """LaydrRoutes.Users.ById.route.key(mapOf("id" to id.value))""")
        assertSourceContains(source, """public val path: String = LaydrRoutes.Users.ById.route.buildPath(mapOf("id" to id.value))""")
        assertSourceContains(source, """return Destination(id = IdParam(routeMatch.parameters.getValue("id")))""")
    }

    @Test
    fun generatesRootNamespaceSegmentRouteGraph(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "users/by_id")

        val source = generateRouteGraph(routesRoot)

        assertSourceContains(source, "public val routes: List<LaydrRoute> = listOf(Users.ById.route)")
        assertSourceContains(source, "public object Users {")
        assertSourceContains(source, "public object ById : LaydrScreenRouteRef")
        assertSourceDoesNotContain(source, "public object Users :")
        assertSourceContains(source, "Expected route match for Users.ById but was ")
        assertSourceDoesNotContain(source, "Expected route match for Users but was ")
    }

    @Test
    fun generatesDynamicPathBuilderWhenParameterShadowsRouteProperty(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "users")
        createRoute(routesRoot, "users/by_route")

        val source = generateRouteGraph(routesRoot)

        assertTrue(
            source.contains(
                """public fun route(rawValue: String): RouteParam = RouteParam(rawValue)""",
            ),
        )
        assertTrue(
            source.contains(
                """public fun path(route: RouteParam): String = this.route.buildPath(mapOf("route" to route.value))""",
            ),
        )
        assertTrue(
            source.contains(
                """public fun destination(route: RouteParam): Destination = Destination(route = route)""",
            ),
        )
        assertTrue(
            source.contains(
                """public val route: RouteParam,""",
            ),
        )
        assertTrue(source.contains("""LaydrRoutes.Users.ByRoute.route.key(mapOf("route" to route.value))"""))
        assertTrue(source.contains("""return Destination(route = RouteParam(routeMatch.parameters.getValue("route")))"""))
    }

    @Test
    fun failsWhenGeneratedPathParametersDuplicate(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "by_id")
        createRoute(routesRoot, "by_id/by_id")

        val error = assertFailsWith<RouteGraphGeneratorException> {
            generateRouteGraph(routesRoot)
        }

        assertMessageContains(error, "Duplicate generated route path parameter id")
        assertMessageContains(error, "/{id}/{id}")
    }

    @Test
    fun scansRoutesInDeterministicDirectoryNameOrder(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "zeta")
        createRoute(routesRoot, "alpha")
        createRoute(routesRoot, "middle")

        val routes = RouteDirectoryScanner().scan(routesRoot).routes

        assertEquals(
            listOf("AlphaRoute", "MiddleRoute", "ZetaRoute"),
            routes.map { it.routeName },
        )
    }

    @Test
    fun generatesRoutesInDeterministicDirectoryNameOrder(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "zeta")
        createRoute(routesRoot, "alpha")
        createRoute(routesRoot, "middle")

        val source = generateRouteGraph(routesRoot)

        assertTrue(source.indexOf("object Alpha") < source.indexOf("object Middle"))
        assertTrue(source.indexOf("object Middle") < source.indexOf("object Zeta"))
    }

    @Test
    fun allowsEmptyRoutesRoot(): Unit = withRoutesRoot { routesRoot ->
        val tree = RouteDirectoryScanner().scan(routesRoot)

        assertEquals(emptyList(), tree.routes)
    }

    @Test
    fun generatesEmptyRouteGraph(): Unit = withRoutesRoot { routesRoot ->
        assertGeneratedRouteGraph(
            routesRoot = routesRoot,
            expectedSource =
            """
            |package dev.goquick.laydr.generated
            |
            |import dev.goquick.laydr.core.LaydrAppGraph
            |import dev.goquick.laydr.core.LaydrRoute
            |import dev.goquick.laydr.core.LaydrRouteMap
            |import kotlin.collections.List
            |import kotlin.collections.listOf
            |
            |/**
            | * Generated route graph for the Laydr route tree.
            | */
            |public object LaydrRoutes {
            |  /**
            |   * Top-level generated route descriptors.
            |   */
            |  public val routes: List<LaydrRoute> = listOf()
            |
            |  /**
            |   * Flattened generated route descriptors for `screenRoutes`.
            |   */
            |  public val screenRoutes: List<LaydrRoute> = listOf()
            |
            |  /**
            |   * Flattened generated route descriptors for `layoutRoutes`.
            |   */
            |  public val layoutRoutes: List<LaydrRoute> = listOf()
            |
            |  /**
            |   * Structural route map for generated route lookup.
            |   */
            |  public val routeMap: LaydrRouteMap =
            |      LaydrRouteMap(routes = routes, screenRoutes = screenRoutes, layoutRoutes = layoutRoutes)
            |
            |  /**
            |   * App-level route graph facade for runtime adapters.
            |   */
            |  public val appGraph: LaydrAppGraph = LaydrAppGraph(routeMap = routeMap)
            |}
            |
            """,
        )
    }

    @Test
    fun usesCallerSuppliedGeneratedPackageName(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "home")

        val source = LaydrCodegen.generateRouteGraph(
            routesDirectory = routesRoot,
            generatedPackage = "dev.goquick.app.generated",
        )

        assertTrue(source.startsWith("package dev.goquick.app.generated"))
    }

    @Test
    fun failsWhenGeneratedPackageNameIsInvalid(): Unit = withRoutesRoot { routesRoot ->
        val error = assertFailsWith<RouteGraphGeneratorException> {
            RouteGraphGenerator().generateRouteGraph(
                routeTree = RouteDirectoryScanner().scan(routesRoot),
                packageName = "Dev.GoQuick",
            )
        }

        assertMessageContains(error, "Generated package name must be dot-separated")
        assertMessageContains(error, "Dev.GoQuick")
    }

    @Test
    fun failsWhenGeneratedRouteObjectNamesDuplicateAtSameLevel(): Unit = withRoutesRoot { routesRoot ->
        val routeFile = routesRoot.resolve("Route.kt")
        val first = routeForGeneratedTest(
            routesRoot = routesRoot,
            routeFile = routeFile,
            routeKind = RouteDirectoryRouteKind.SCREEN,
            routePath = "/foo_bar",
            sourceName = "foo_bar",
        )
        val second = routeForGeneratedTest(
            routesRoot = routesRoot,
            routeFile = routeFile,
            routeKind = RouteDirectoryRouteKind.SCREEN,
            routePath = "/foo/bar",
            sourceName = "foo_bar",
        )

        val error = assertFailsWith<RouteGraphGeneratorException> {
            RouteGraphGenerator().generateRouteGraph(
                routeTree = RouteDirectoryTree(routesRoot = routesRoot, routes = listOf(first, second)),
                packageName = "dev.goquick.laydr.generated",
            )
        }

        assertMessageContains(error, "Duplicate generated route object FooBar")
        assertMessageContains(error, "LaydrRoutes")
    }

    @Test
    fun failsWhenRoutesRootDoesNotExist(): Unit = withRoutesRoot { routesRoot ->
        val missingRoot = routesRoot.resolve("missing")

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(missingRoot)
        }

        assertMessageContains(error, "Routes root does not exist")
        assertMessageContains(error, missingRoot.toAbsolutePath().normalize().toString())
    }

    @Test
    fun failsWhenRoutesRootIsNotDirectory(): Unit = withRoutesRoot { routesRoot ->
        val fileRoot = routesRoot.resolve("routes.txt")
        Files.writeString(fileRoot, "")

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(fileRoot)
        }

        assertMessageContains(error, "Routes root is not a directory")
        assertMessageContains(error, fileRoot.toAbsolutePath().normalize().toString())
    }

    @Test
    fun failsWhenSegmentDirectoryContainsKotlinFilesWithoutRouteDeclaration(): Unit = withRoutesRoot { routesRoot ->
        val home = routesRoot.resolve("home")
        Files.createDirectories(home)
        Files.writeString(home.resolve("Screen.kt"), "package routes.home\n")
        Files.writeString(home.resolve("Layout.kt"), "package routes.home\n")

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot, requirePackageNames = true)
        }

        assertMessageContains(error, "Kotlin files under route segment directories require Route.kt")
        assertMessageContains(error, home.resolve("Layout.kt").toString())
    }

    @Test
    fun ignoresInvalidSegmentDirectoryWithoutRouteDeclarationOrDescendants(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "home")
        Files.createDirectories(routesRoot.resolve(".tmp/nested"))
        Files.createDirectories(routesRoot.resolve("_notes"))

        val tree = RouteDirectoryScanner().scan(routesRoot)

        assertEquals(listOf("/home"), tree.routes.map { route -> route.routePath })
    }

    @Test
    fun failsWhenRouteFileHasNoRouteKindDeclaration(): Unit = withRoutesRoot { routesRoot ->
        val home = routesRoot.resolve("home")
        Files.createDirectories(home)
        Files.writeString(home.resolve("Route.kt"), "package routes.home\n")

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Route.kt must bind exactly one package-visible top-level Route value")
        assertMessageContains(error, "LaydrRouteDeclaration.screen")
        assertMessageContains(error, home.resolve("Route.kt").toString())
    }

    @Test
    fun failsWhenRouteFileHasStrayRouteKindDeclaration(): Unit = withRoutesRoot { routesRoot ->
        val home = routesRoot.resolve("home")
        Files.createDirectories(home)
        Files.writeString(
            home.resolve("Route.kt"),
            """
            package routes.home

            internal val Route = LaydrRouteDeclaration.screen()
            internal val Duplicate = LaydrRouteDeclaration.layout()
            """,
        )

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Route.kt route kind declarations must be assigned to the top-level Route binding")
        assertMessageContains(error, home.resolve("Route.kt").toString())
    }

    @Test
    fun ignoresCommentedOutRouteKindDeclarations(): Unit = withRoutesRoot { routesRoot ->
        val home = routesRoot.resolve("home")
        Files.createDirectories(home)
        Files.writeString(
            home.resolve("Route.kt"),
            """
            package routes.home

            // internal val Route = LaydrRouteDeclaration.screen()
            /*
            internal val Other = LaydrRouteDef.layout { _, content -> content() }
            */
            """,
        )

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Route.kt must bind exactly one package-visible top-level Route value")
    }

    @Test
    fun failsWhenLayoutRouteIsLeafWithoutScreen(): Unit = withRoutesRoot { routesRoot ->
        val settings = routesRoot.resolve("settings")
        Files.createDirectories(settings)
        Files.writeString(
            settings.resolve("Route.kt"),
            """
            package routes.settings

            internal val Route = LaydrRouteDeclaration.layout()
            """,
        )

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Layout-only route directory must contain child routes")
        assertMessageContains(error, settings.toString())
    }

    @Test
    fun allowsRouteNestedUnderNamespaceSegmentAncestor(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "users/by_id")

        val users = RouteDirectoryScanner().scan(routesRoot).onlyRoute()
        val byId = users.onlyChild()

        assertEquals(false, users.isDeclaredRoute)
        assertEquals("/users", users.routePath)
        assertEquals("/users/{id}", byId.routePath)
        assertEquals(RouteDirectoryRouteKind.SCREEN, byId.routeKind)
    }

    @Test
    fun failsWhenNamespaceSegmentDirectoryContainsKotlinFile(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "catalog/bundle/by_activity_bundle_id")
        Files.writeString(
            routesRoot.resolve("catalog/bundle/Helper.kt"),
            """
            package routes.catalog.bundle

            internal val Helper = Unit
            """.trimIndent(),
        )

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Kotlin files under route segment directories require Route.kt")
        assertMessageContains(error, routesRoot.resolve("catalog/bundle/Helper.kt").toString())
    }

    @Test
    fun failsWhenRouteDirectoryNameIsInvalid(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "UserProfile")

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Route directory name must be lowercase snake_case")
        assertMessageContains(error, routesRoot.resolve("UserProfile").toString())
    }

    @Test
    fun failsWhenDynamicRouteDirectoryNameHasNoParameter(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "users")
        createRoute(routesRoot, "users/by_")

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Dynamic route directory must be 'by_' plus")
        assertMessageContains(error, routesRoot.resolve("users/by_").toString())
    }

    @Test
    fun failsWhenGeneratedRouteNamesDuplicate(): Unit = withRoutesRoot { routesRoot ->
        createRoute(routesRoot, "foo")
        createRoute(routesRoot, "foo/bar")
        createRoute(routesRoot, "foo_bar")

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot)
        }

        assertMessageContains(error, "Duplicate generated route name FooBarRoute")
        assertMessageContains(error, routesRoot.resolve("foo/bar").toString())
        assertMessageContains(error, routesRoot.resolve("foo_bar").toString())
    }

    @Test
    fun failsWhenRouteFileHasNoPackageDeclarationWhenPackageNamesAreRequired(): Unit = withRoutesRoot { routesRoot ->
        val home = routesRoot.resolve("home")
        Files.createDirectories(home)
        Files.writeString(home.resolve("Route.kt"), "internal val Route = LaydrRouteDeclaration.screen()\n")

        val error = assertFailsWith<RouteDirectoryScannerException> {
            RouteDirectoryScanner().scan(routesRoot, requirePackageNames = true)
        }

        assertMessageContains(error, "Route.kt is missing package declaration")
        assertMessageContains(error, home.resolve("Route.kt").toString())
    }

    private fun withRoutesRoot(block: (Path) -> Unit) {
        val routesRoot = Files.createTempDirectory("laydr-routes-")
        try {
            block(routesRoot)
        } finally {
            routesRoot.toFile().deleteRecursively()
        }
    }

    private fun createRoute(
        routesRoot: Path,
        relativePath: String,
        screen: Boolean = true,
        layout: Boolean = false,
        composeDeclaration: Boolean = false,
    ) {
        val routeDirectory = routesRoot.resolve(relativePath)
        Files.createDirectories(routeDirectory)
        Files.writeString(
            routeDirectory.resolve("Route.kt"),
            routeSource(
                packageName = "routes.${relativePath.replace("/", ".")}",
                screen = screen,
                layout = layout,
                composeDeclaration = composeDeclaration,
            ),
        )
    }

    private fun routeSource(
        packageName: String,
        screen: Boolean,
        layout: Boolean,
        composeDeclaration: Boolean,
    ): String {
        val declaration = when {
            screen && layout -> {
                if (composeDeclaration) {
                    """
                    internal val Route = LaydrRouteDef.screenAndLayout {
                        screen { _ -> Unit }
                        layout { _, content -> content() }
                    }
                    """
                } else {
                    "internal val Route = LaydrRouteDeclaration.screenAndLayout()"
                }
            }
            screen -> {
                if (composeDeclaration) {
                    "internal val Route = LaydrRouteDef.screen { _ -> Unit }"
                } else {
                    "internal val Route = LaydrRouteDeclaration.screen()"
                }
            }
            layout -> {
                if (composeDeclaration) {
                    "internal val Route = LaydrRouteDef.layout { _, content -> content() }"
                } else {
                    "internal val Route = LaydrRouteDeclaration.layout()"
                }
            }
            else -> error("Route test fixture must declare a screen, layout, or both")
        }

        return """
            package $packageName

            $declaration
            """.trimIndent()
    }

    private fun generateRouteGraph(routesRoot: Path): String =
        LaydrCodegen.generateRouteGraph(
            routesDirectory = routesRoot,
            generatedPackage = "dev.goquick.laydr.generated",
        )

    private fun assertNav3HelperRequiresCompose(
        routesRoot: Path,
        generateNav3Kmp: Boolean = false,
        generateNav3Androidx: Boolean = false,
        expectedMessage: String,
    ) {
        createRoute(routesRoot, "contacts")

        val error = assertFailsWith<RouteGraphGeneratorException> {
            LaydrCodegen.generateSources(
                routesDirectory = routesRoot,
                generatedPackage = "dev.goquick.laydr.generated",
                generateNav3Kmp = generateNav3Kmp,
                generateNav3Androidx = generateNav3Androidx,
            )
        }

        assertMessageContains(error, expectedMessage)
    }

    private fun assertGeneratedRouteGraph(
        routesRoot: Path,
        expectedSource: String,
    ) {
        assertEquals(expectedSource.trimMargin(), generateRouteGraph(routesRoot))
    }

    private fun assertSourceContains(
        source: String,
        expected: String,
    ) {
        assertTrue(
            source.contains(expected),
            "Expected generated source to contain '$expected' but was:\n$source",
        )
    }

    private fun assertSourceDoesNotContain(
        source: String,
        unexpected: String,
    ) {
        assertTrue(
            !source.contains(unexpected),
            "Expected generated source to not contain '$unexpected' but was:\n$source",
        )
    }

    private fun routeForGeneratedTest(
        routesRoot: Path,
        routeFile: Path,
        routeKind: RouteDirectoryRouteKind,
        routePath: String,
        sourceName: String,
    ): RouteDirectoryRoute =
        RouteDirectoryRoute(
            directory = routesRoot.resolve(sourceName),
            routeFile = routeFile,
            routePackageName = "routes.${sourceName.replace("/", ".")}",
            routeKind = routeKind,
            metadataName = null,
            metadataLabels = emptyMap(),
            routePath = routePath,
            routeName = "${sourceName.replace("/", "")}Route",
            segments = listOf(RouteDirectorySegment.Static(sourceName = sourceName)),
            children = emptyList(),
        )

    private fun RouteDirectoryTree.onlyRoute(): RouteDirectoryRoute {
        assertEquals(1, routes.size)
        return routes.single()
    }

    private fun RouteDirectoryRoute.onlyChild(): RouteDirectoryRoute {
        assertEquals(1, children.size)
        return children.single()
    }

    private fun assertMessageContains(
        error: Throwable,
        expected: String,
    ) {
        assertTrue(
            error.message.orEmpty().contains(expected),
            "Expected message to contain '$expected' but was '${error.message}'",
        )
    }
}
