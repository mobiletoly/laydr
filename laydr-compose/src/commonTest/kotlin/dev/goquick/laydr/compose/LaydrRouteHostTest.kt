package dev.goquick.laydr.compose

import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteMatch
import dev.goquick.laydr.core.LaydrRouteMetadata
import dev.goquick.laydr.core.LaydrRouteSegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class LaydrRouteHostTest {
    @Test
    fun rejectsBlankLayoutKeyName() {
        val error = assertFailsWith<IllegalArgumentException> {
            LaydrLayoutKey<String>(" ")
        }

        assertEquals("Layout key name must not be blank", error.message)
    }

    @Test
    fun readsLayoutValueByKeyIdentity() {
        val firstKey = LaydrLayoutKey<String>("settings.shell")
        val secondKey = LaydrLayoutKey<String>("settings.shell")
        val values = LaydrLayoutValues.build {
            put(firstKey, "profile")
        }

        assertEquals("profile", values[firstKey])
        assertNull(values[secondKey])
    }

    @Test
    fun returnsNullForMissingLayoutValue() {
        val key = LaydrLayoutKey<String>("missing")

        assertNull(LaydrLayoutValues.Empty[key])
    }

    @Test
    fun layoutValuesAreImmutableSnapshots() {
        val key = LaydrLayoutKey<String>("settings.shell")
        val builder = LaydrLayoutValues.Builder()

        builder.put(key, "profile")
        val values = builder.build()
        builder.put(key, "account")

        assertEquals("profile", values[key])
    }

    @Test
    fun routeDefinitionsAcceptRegisteredScreenContent() {
        val home = route(
            id = "Home",
            segments = listOf(static("home")),
        )
        val expected = LaydrScreenContent {
        }
        LaydrComposeRouteDefinitions(
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
            screenDefinitions = listOf(
                LaydrComposeScreenRouteDefinition(
                    route = home,
                ) { _ -> expected },
            ),
            layoutDefinitions = emptyList(),
        )
    }

    @Test
    fun routeDefinitionsFailForMissingScreenDefinition() {
        val home = route(
            id = "Home",
            segments = listOf(static("home")),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            LaydrComposeRouteDefinitions(
                routeMap = routeMap(
                    routes = listOf(home),
                    screenRoutes = listOf(home),
                    layoutRoutes = emptyList(),
                ),
                screenDefinitions = emptyList(),
                layoutDefinitions = emptyList(),
            )
        }

        assertEquals("Missing Laydr screen definition for route Home", error.message)
    }

    @Test
    fun routeDefinitionsRejectDuplicateDefinitions() {
        val home = route(
            id = "Home",
            segments = listOf(static("home")),
        )
        val screenDefinition = LaydrComposeScreenRouteDefinition(
            route = home,
        ) { _ -> LaydrScreenContent {} }

        val screenError = assertFailsWith<IllegalArgumentException> {
            LaydrComposeRouteDefinitions(
                routeMap = routeMap(
                    routes = listOf(home),
                    screenRoutes = listOf(home),
                    layoutRoutes = emptyList(),
                ),
                screenDefinitions = listOf(screenDefinition, screenDefinition),
                layoutDefinitions = emptyList(),
            )
        }
        assertEquals("Duplicate Laydr screen definition for route Home", screenError.message)

        val layoutDefinition = LaydrComposeLayoutRouteDefinition(
            route = home,
        ) { _, content -> content() }

        val layoutError = assertFailsWith<IllegalArgumentException> {
            LaydrComposeRouteDefinitions(
                routeMap = routeMap(
                    routes = listOf(home),
                    screenRoutes = emptyList(),
                    layoutRoutes = listOf(home),
                ),
                screenDefinitions = emptyList(),
                layoutDefinitions = listOf(layoutDefinition, layoutDefinition),
            )
        }
        assertEquals("Duplicate Laydr layout definition for route Home", layoutError.message)
    }

    @Test
    fun routeDefinitionsExposeLayoutDefinitions() {
        val settings = route(
            id = "Settings",
            segments = listOf(static("settings")),
        )
        val layoutDefinition = LaydrComposeLayoutRouteDefinition(
            route = settings,
        ) { _, content -> content() }

        val definitions = LaydrComposeRouteDefinitions(
            routeMap = routeMap(
                routes = listOf(settings),
                screenRoutes = emptyList(),
                layoutRoutes = listOf(settings),
            ),
            screenDefinitions = emptyList(),
            layoutDefinitions = listOf(layoutDefinition),
        )

        assertNotNull(definitions.layoutDefinitionFor(settings))
    }

    @Test
    fun findsMatchedRoute() {
        val home = route(
            id = "Home",
            segments = listOf(static("home")),
        )

        val hostMatch = findLaydrRouteHostMatch(
            currentPath = "/home",
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
        )

        assertSame(home, hostMatch?.match?.route)
        assertEquals(emptyMap(), hostMatch?.match?.parameters)
        assertEquals(emptyList(), hostMatch?.layoutRoutes)
    }

    @Test
    fun returnsNullWhenNoRouteMatches() {
        val home = route(
            id = "Home",
            segments = listOf(static("home")),
        )

        assertNull(
            findLaydrRouteHostMatch(
                currentPath = "/missing",
                routeMap = routeMap(
                    routes = listOf(home),
                    screenRoutes = listOf(home),
                    layoutRoutes = emptyList(),
                ),
            ),
        )
        assertNull(
            findLaydrRouteHostMatch(
                currentPath = "/home?tab=profile",
                routeMap = routeMap(
                    routes = listOf(home),
                    screenRoutes = listOf(home),
                    layoutRoutes = emptyList(),
                ),
            ),
        )
        assertNull(
            findLaydrRouteHostMatch(
                currentPath = "/home/",
                routeMap = routeMap(
                    routes = listOf(home),
                    screenRoutes = listOf(home),
                    layoutRoutes = emptyList(),
                ),
            ),
        )
    }

    @Test
    fun returnsNullWhenOnlyLayoutRouteMatches() {
        val profile = route(
            id = "Settings.Profile",
            segments = listOf(static("settings"), static("profile")),
        )
        val settings = route(
            id = "Settings",
            segments = listOf(static("settings")),
            children = listOf(profile),
        )

        assertNull(
            findLaydrRouteHostMatch(
                currentPath = "/settings",
                routeMap = routeMap(
                    routes = listOf(settings),
                    screenRoutes = listOf(profile),
                    layoutRoutes = listOf(settings),
                ),
            ),
        )
    }

    @Test
    fun searchesTopLevelRoutesBySpecificity() {
        val dynamic = route(
            id = "Dynamic",
            segments = listOf(dynamic("id")),
        )
        val home = route(
            id = "Home",
            segments = listOf(static("home")),
        )

        val hostMatch = findLaydrRouteHostMatch(
            currentPath = "/home",
            routeMap = routeMap(
                routes = listOf(dynamic, home),
                screenRoutes = listOf(dynamic, home),
                layoutRoutes = emptyList(),
            ),
        )

        assertSame(home, hostMatch?.match?.route)
        assertEquals(emptyMap(), hostMatch?.match?.parameters)
    }

    @Test
    fun includesMatchedLayoutRouteChainInOuterToInnerOrder() {
        val profile = route(
            id = "Settings.Profile",
            segments = listOf(static("settings"), static("profile")),
        )
        val settings = route(
            id = "Settings",
            segments = listOf(static("settings")),
            children = listOf(profile),
        )

        val hostMatch = findLaydrRouteHostMatch(
            currentPath = "/settings/profile",
            routeMap = routeMap(
                routes = listOf(settings),
                screenRoutes = listOf(profile),
                layoutRoutes = listOf(settings),
            ),
        )

        assertSame(profile, hostMatch?.match?.route)
        assertEquals(listOf(settings), hostMatch?.layoutRoutes)
    }

    @Test
    fun includesScreenRouteWhenItAlsoOwnsLayout() {
        val settings = route(
            id = "Settings",
            segments = listOf(static("settings")),
        )

        val hostMatch = findLaydrRouteHostMatch(
            currentPath = "/settings",
            routeMap = routeMap(
                routes = listOf(settings),
                screenRoutes = listOf(settings),
                layoutRoutes = listOf(settings),
            ),
        )

        assertSame(settings, hostMatch?.match?.route)
        assertEquals(listOf(settings), hostMatch?.layoutRoutes)
    }

    @Test
    fun deliversDecodedDynamicParametersFromDescendantRoute() {
        val byId = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )
        val users = route(
            id = "Users",
            segments = listOf(static("users")),
            children = listOf(byId),
        )

        val hostMatch = findLaydrRouteHostMatch(
            currentPath = "/users/a%2Fb",
            routeMap = routeMap(
                routes = listOf(users),
                screenRoutes = listOf(byId),
                layoutRoutes = emptyList(),
            ),
        )

        assertSame(byId, hostMatch?.match?.route)
        assertEquals(mapOf("id" to "a/b"), hostMatch?.match?.parameters)
    }

    @Test
    fun deliversMatchedRouteMetadata() {
        val home = route(
            id = "Home",
            segments = listOf(static("home")),
            metadata = LaydrRouteMetadata(name = "Home Page"),
        )

        val hostMatch = findLaydrRouteHostMatch(
            currentPath = "/home",
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
        )

        assertEquals("Home Page", hostMatch?.match?.route?.metadata?.name)
    }

    @Test
    fun deliversLayoutRouteMetadata() {
        val profile = route(
            id = "Settings.Profile",
            segments = listOf(static("settings"), static("profile")),
            metadata = LaydrRouteMetadata(name = "Settings Profile"),
        )
        val settings = route(
            id = "Settings",
            segments = listOf(static("settings")),
            children = listOf(profile),
            metadata = LaydrRouteMetadata(name = "Settings Shell"),
        )

        val hostMatch = findLaydrRouteHostMatch(
            currentPath = "/settings/profile",
            routeMap = routeMap(
                routes = listOf(settings),
                screenRoutes = listOf(profile),
                layoutRoutes = listOf(settings),
            ),
        )

        assertEquals(listOf("Settings Shell"), hostMatch?.layoutRoutes?.map { it.metadata.name })
        assertEquals("Settings Profile", hostMatch?.match?.route?.metadata?.name)
    }

    @Test
    fun buildsLayoutContextWithScreenOwnedValues() {
        val key = LaydrLayoutKey<String>("settings.shell")
        val profile = route(
            id = "Settings.Profile",
            segments = listOf(static("settings"), static("profile")),
        )
        val settings = route(
            id = "Settings",
            segments = listOf(static("settings")),
            children = listOf(profile),
        )
        val match = LaydrRouteMatch(route = profile, parameters = emptyMap())
        val screen = LaydrScreenContent(
            layoutValues = LaydrLayoutValues.build {
                put(key, "profile")
            },
        ) {
        }

        val context = layoutContextForRoute(
            layoutRoute = settings,
            match = match,
            screen = screen,
        )

        assertSame(settings, context.route)
        assertEquals(match, context.match)
        assertEquals("profile", context[key])
    }

    private fun route(
        id: String,
        segments: List<LaydrRouteSegment>,
        children: List<LaydrRoute> = emptyList(),
        metadata: LaydrRouteMetadata = LaydrRouteMetadata(name = id),
    ): LaydrRoute =
        LaydrRoute(
            id = id,
            segments = segments,
            children = children,
            metadata = metadata,
        )

    private fun static(value: String): LaydrRouteSegment.Static =
        LaydrRouteSegment.Static(value)

    private fun dynamic(parameterName: String): LaydrRouteSegment.Dynamic =
        LaydrRouteSegment.Dynamic(parameterName)

    private fun routeMap(
        routes: List<LaydrRoute>,
        screenRoutes: List<LaydrRoute>,
        layoutRoutes: List<LaydrRoute>,
    ): LaydrRouteMap =
        LaydrRouteMap(
            routes = routes,
            screenRoutes = screenRoutes,
            layoutRoutes = layoutRoutes,
        )
}
