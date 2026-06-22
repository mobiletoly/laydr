package dev.goquick.laydr.nav3.kmp

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions
import dev.goquick.laydr.compose.LaydrComposeScreenRouteDefinition
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrParameterlessScreenRouteRef
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrScreenDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LaydrNavEntryProviderTest {
    @Test
    fun resolvesScreenRoute() {
        val home = route(id = "Home", segments = listOf(static("home")))

        val resolvedEntry = resolveLaydrNavEntry(
            key = home.key().navKey(),
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
        )

        assertNotNull(resolvedEntry)
        assertSame(home, resolvedEntry.match.route)
        assertEquals(emptyList(), resolvedEntry.layoutRoutes)
    }

    @Test
    fun returnsNullForInvalidKey() {
        val byId = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )

        assertNull(
            resolveLaydrNavEntry(
                key = LaydrNavKey(routeId = "Users.ById"),
                routeMap = routeMap(
                    routes = listOf(byId),
                    screenRoutes = listOf(byId),
                    layoutRoutes = emptyList(),
                ),
            ),
        )
    }

    @Test
    fun returnsNullForLayoutOnlyKeysExcludedFromScreenRoutes() {
        val detail = route(
            id = "Items.ById",
            segments = listOf(static("items"), dynamic("id")),
        )
        val items = route(
            id = "Items",
            segments = listOf(static("items")),
            children = listOf(detail),
        )

        assertNull(
            resolveLaydrNavEntry(
                key = items.key().navKey(),
                routeMap = routeMap(
                    routes = listOf(items),
                    screenRoutes = listOf(detail),
                    layoutRoutes = listOf(items),
                ),
            ),
        )

        val detailLayout = route(
            id = "Items.Detail",
            segments = listOf(static("items"), static("detail")),
        )
        val itemsScreen = route(
            id = "Items",
            segments = listOf(static("items")),
            children = listOf(detailLayout),
        )

        assertNull(
            resolveLaydrNavEntry(
                key = detailLayout.key().navKey(),
                routeMap = routeMap(
                    routes = listOf(itemsScreen),
                    screenRoutes = listOf(itemsScreen),
                    layoutRoutes = listOf(detailLayout),
                ),
            ),
        )
    }

    @Test
    fun wrapsInheritedLayoutsOuterToInner() {
        val detail = route(
            id = "Items.ById",
            segments = listOf(static("items"), dynamic("id")),
        )
        val items = route(
            id = "Items",
            segments = listOf(static("items")),
            children = listOf(detail),
        )

        val resolvedEntry = resolveLaydrNavEntry(
            key = detail.key(mapOf("id" to "alpha")).navKey(),
            routeMap = routeMap(
                routes = listOf(items),
                screenRoutes = listOf(detail),
                layoutRoutes = listOf(items, detail),
            ),
        )

        assertEquals(listOf(items, detail), resolvedEntry?.layoutRoutes)
    }

    @Test
    fun attachesAppMetadataToResolvedEntries() {
        val user = route(
            id = "Users.ById",
            segments = listOf(static("users"), dynamic("id")),
        )
        var receivedContext: LaydrNavEntryContext? = null
        val provider = laydrNavEntryProvider(
            routeMap = routeMap(
                routes = listOf(user),
                screenRoutes = listOf(user),
                layoutRoutes = emptyList(),
            ),
            notFoundContent = { _ -> },
            entryMetadata = { context ->
                receivedContext = context
                mapOf(
                    "app:route" to context.match.route.id,
                    "app:id" to context.match.parameters.getValue("id"),
                )
            },
        ) {
            LaydrScreenContent {
            }
        }

        val key = user.key(mapOf("id" to "ada")).navKey()
        val entry = provider(key)

        assertSame(key, receivedContext?.key)
        assertSame(user, receivedContext?.match?.route)
        assertEquals("ada", receivedContext?.match?.parameters?.get("id"))
        assertEquals(listOf(user), receivedContext?.routePlacement?.routeChain)
        assertSame(user, receivedContext?.routePlacement?.topLevelRoute)
        assertEquals(true, receivedContext?.routePlacement?.isTopLevelRoute)
        assertEquals(0, receivedContext?.routePlacement?.depthFromTopLevelRoute)
        assertEquals("Users.ById", entry.metadata["app:route"])
        assertEquals("ada", entry.metadata["app:id"])
    }

    @Test
    fun appMetadataOverridesSceneMetadataOnCollision() {
        val detail = route(
            id = "Contacts.ById",
            segments = listOf(static("contacts"), dynamic("id")),
        )
        val contacts = route(
            id = "Contacts",
            segments = listOf(static("contacts")),
            children = listOf(detail),
        )
        val provider = laydrNavEntryProvider(
            routeMap = routeMap(
                routes = listOf(contacts),
                screenRoutes = listOf(contacts, detail),
                layoutRoutes = emptyList(),
            ),
            notFoundContent = { _ -> },
            sceneSupport = testSceneSupport(contacts, detail),
            entryMetadata = { context ->
                mapOf(
                    "test:list" to "app-list",
                    "app:key" to context.key.routeId,
                )
            },
        ) {
            LaydrScreenContent {
            }
        }

        val entry = provider(contacts.key().navKey())

        assertEquals("app-list", entry.metadata["test:list"])
        assertEquals("Contacts", entry.metadata["app:key"])
    }

    @Test
    fun launchMetadataOverridesStackMetadataOnCollision() {
        val home = route(id = "Home", segments = listOf(static("home")))
        val provider = laydrNavEntryProvider(
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
            notFoundContent = { _ -> },
            entryMetadata = {
                mapOf("test:presentation" to "stack")
            },
        ) {
            LaydrScreenContent {
            }
        }

        val entry = provider(
            home.key().navKey().withEntryMetadata(
                LaydrNavEntryMetadata("test:presentation" to "launch"),
            ),
        )

        assertEquals("launch", entry.metadata["test:presentation"])
    }

    @Test
    fun skipsAppMetadataForNotFoundEntries() {
        val home = route(id = "Home", segments = listOf(static("home")))
        val settings = route(id = "Settings", segments = listOf(static("settings")))
        var evaluations = 0
        val provider = laydrNavEntryProvider(
            routeMap = routeMap(
                routes = listOf(home, settings),
                screenRoutes = listOf(home),
                layoutRoutes = listOf(settings),
            ),
            notFoundContent = { _ -> },
            entryMetadata = {
                evaluations += 1
                mapOf("app:route" to it.match.route.id)
            },
        ) {
            LaydrScreenContent {
            }
        }

        val missingEntry = provider(LaydrNavKey(routeId = "Missing"))
        val foreignEntry = provider(ForeignKey)
        val layoutOnlyEntry = provider(settings.key().navKey())

        assertTrue(missingEntry.metadata.isEmpty())
        assertTrue(foreignEntry.metadata.isEmpty())
        assertTrue(layoutOnlyEntry.metadata.isEmpty())
        assertEquals(0, evaluations)
    }

    @Test
    fun createsEntryWithoutEagerlyEvaluatingScreenContent() {
        val home = route(id = "Home", segments = listOf(static("home")))
        var evaluations = 0

        val provider = laydrNavEntryProvider(
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
            notFoundContent = { _ -> },
        ) {
            evaluations += 1
            LaydrScreenContent {
            }
        }

        val entry = provider(home.key().navKey())

        assertEquals(home.key().navKey().toString(), entry.contentKey)
        assertEquals(0, evaluations)
    }

    @Test
    fun entryProviderAcceptsRouteDefinitionsWithoutEagerEvaluation() {
        val home = route(id = "Home", segments = listOf(static("home")))
        var evaluations = 0
        val definitions = LaydrComposeRouteDefinitions(
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
            screenDefinitions = listOf(
                LaydrComposeScreenRouteDefinition(
                    route = home,
                ) { _ ->
                    evaluations += 1
                    LaydrScreenContent {
                    }
                },
            ),
            layoutDefinitions = emptyList(),
        )

        val provider = laydrNavEntryProvider(
            routeDefinitions = definitions,
            notFoundContent = { _ -> },
        )

        provider(home.key().navKey())

        assertEquals(0, evaluations)
    }

    @Test
    fun routeDefinitionsEntryProviderForwardsMetadata() {
        val home = route(id = "Home", segments = listOf(static("home")))
        val definitions = LaydrComposeRouteDefinitions(
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
            screenDefinitions = listOf(
                LaydrComposeScreenRouteDefinition(
                    route = home,
                ) { _ ->
                    LaydrScreenContent {
                    }
                },
            ),
            layoutDefinitions = emptyList(),
        )
        val provider = laydrNavEntryProvider(
            routeDefinitions = definitions,
            entryMetadata = { context ->
                mapOf("app:route" to context.match.route.id)
            },
            notFoundContent = { _ -> },
        )

        val entry = provider(home.key().navKey())

        assertEquals("Home", entry.metadata["app:route"])
    }

    @Test
    fun appStateEntryProviderForwardsAppMetadata() {
        val home = route(id = "Home", segments = listOf(static("home")))
        val routeMap = routeMap(
            routes = listOf(home),
            screenRoutes = listOf(home),
            layoutRoutes = emptyList(),
        )
        val appGraph = LaydrAppGraph(routeMap)
        val homeRef = TestParameterlessScreenRouteRef(home)
        val sections = laydrNavSectionSet(
            appGraph,
            laydrNavSection(homeRef),
        )
        val coordinator = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = mapOf(
                sections.items.first() to LaydrNavStackCoordinator(
                    appGraph = appGraph,
                    backStack = NavBackStack<NavKey>(home.key().navKey()),
                ),
            ),
        )
        val definitions = LaydrComposeRouteDefinitions(
            routeMap = routeMap,
            screenDefinitions = listOf(
                LaydrComposeScreenRouteDefinition(
                    route = home,
                ) { _ ->
                    LaydrScreenContent {
                    }
                },
            ),
            layoutDefinitions = emptyList(),
        )
        val provider = laydrNavSectionEntryProvider(
            routeDefinitions = definitions,
            sectionSet = sections,
            entryMetadata = { context ->
                mapOf("app:key" to context.key.routeId)
            },
            entryStore = coordinator.entryStore,
            notFoundContent = { _ -> },
        )

        val entry = provider(home.key().navKey())

        assertEquals("Home", entry.metadata["app:key"])
    }

    @Test
    fun appStateEntryProviderForwardsSectionMetadata() {
        val detail = route(
            id = "Contacts.ById",
            segments = listOf(static("contacts"), dynamic("id")),
        )
        val contacts = route(
            id = "Contacts",
            segments = listOf(static("contacts")),
            children = listOf(detail),
        )
        val about = route(id = "About", segments = listOf(static("about")))
        val routeMap = routeMap(
            routes = listOf(contacts, about),
            screenRoutes = listOf(contacts, detail, about),
            layoutRoutes = emptyList(),
        )
        val appGraph = LaydrAppGraph(routeMap)
        val contactsRef = TestParameterlessScreenRouteRef(contacts)
        val sections = laydrNavSectionSet(
            appGraph,
            laydrNavSection(contactsRef, "Contacts"),
        )
        val coordinator = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = mapOf(
                sections.items.first() to LaydrNavStackCoordinator(
                    appGraph = appGraph,
                    backStack = NavBackStack<NavKey>(contacts.key().navKey()),
                ),
            ),
        )
        val definitions = LaydrComposeRouteDefinitions(
            routeMap = routeMap,
            screenDefinitions = listOf(
                LaydrComposeScreenRouteDefinition(route = contacts) { _ ->
                    LaydrScreenContent {
                    }
                },
                LaydrComposeScreenRouteDefinition(route = detail) { _ ->
                    LaydrScreenContent {
                    }
                },
                LaydrComposeScreenRouteDefinition(route = about) { _ ->
                    LaydrScreenContent {
                    }
                },
            ),
            layoutDefinitions = emptyList(),
        )
        var evaluations = 0
        val provider = laydrNavSectionEntryProvider(
            routeDefinitions = definitions,
            sectionSet = sections,
            entryMetadata = { context ->
                evaluations += 1
                mapOf(
                    "app:section" to context.placement.section.data,
                    "app:root" to context.placement.isSectionRoot,
                    "app:depth" to context.placement.depthFromSectionRoot,
                    "app:topRoute" to context.routePlacement.topLevelRoute.id,
                    "app:topDepth" to context.routePlacement.depthFromTopLevelRoute,
                    "app:isTopRoute" to context.routePlacement.isTopLevelRoute,
                    "app:id" to context.match.parameters.getValue("id"),
                )
            },
            entryStore = coordinator.entryStore,
            notFoundContent = { _ -> },
        )

        val detailEntry = provider(detail.key(mapOf("id" to "ada")).navKey())
        val aboutEntry = provider(about.key().navKey())

        assertEquals("Contacts", detailEntry.metadata["app:section"])
        assertEquals(false, detailEntry.metadata["app:root"])
        assertEquals(1, detailEntry.metadata["app:depth"])
        assertEquals("Contacts", detailEntry.metadata["app:topRoute"])
        assertEquals(1, detailEntry.metadata["app:topDepth"])
        assertEquals(false, detailEntry.metadata["app:isTopRoute"])
        assertEquals("ada", detailEntry.metadata["app:id"])
        assertTrue(aboutEntry.metadata.isEmpty())
        assertEquals(1, evaluations)
    }

    @Test
    fun createsFallbackEntryForInvalidKeyWithoutScreenContent() {
        val home = route(id = "Home", segments = listOf(static("home")))
        var evaluations = 0

        val provider = laydrNavEntryProvider(
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
            notFoundContent = { _ -> },
        ) {
            evaluations += 1
            LaydrScreenContent {
            }
        }

        val key = LaydrNavKey(routeId = "Missing")
        val entry = provider(key)

        assertEquals(key.toString(), entry.contentKey)
        assertEquals(0, evaluations)
    }

    @Test
    fun createsFallbackEntryForForeignNavKeyWithoutScreenContent() {
        val home = route(id = "Home", segments = listOf(static("home")))
        var evaluations = 0

        val provider = laydrNavEntryProvider(
            routeMap = routeMap(
                routes = listOf(home),
                screenRoutes = listOf(home),
                layoutRoutes = emptyList(),
            ),
            notFoundContent = { _ -> },
        ) {
            evaluations += 1
            LaydrScreenContent {
            }
        }

        val entry = provider(ForeignKey)

        assertEquals(ForeignKey.toString(), entry.contentKey)
        assertEquals(0, evaluations)
    }

    @Test
    fun classifiesNotFoundReasons() {
        val home = route(id = "Home", segments = listOf(static("home")))
        val settings = route(id = "Settings", segments = listOf(static("settings")))
        val routeMap = routeMap(
            routes = listOf(home, settings),
            screenRoutes = listOf(home),
            layoutRoutes = listOf(settings),
        )

        val foreign = laydrNavNotFound(ForeignKey, routeMap)
        assertEquals(LaydrNavNotFoundReason.ForeignKey, foreign.reason)
        assertNull(foreign.laydrKey)
        assertNull(foreign.displayPath)

        val missing = laydrNavNotFound(LaydrNavKey(routeId = "Missing"), routeMap)
        assertEquals(LaydrNavNotFoundReason.UnknownRoute, missing.reason)
        assertNotNull(missing.laydrKey)
        assertNull(missing.displayPath)

        val layoutOnly = laydrNavNotFound(settings.key().navKey(), routeMap)
        assertEquals(LaydrNavNotFoundReason.LayoutOnlyRoute, layoutOnly.reason)
        assertEquals("/settings", layoutOnly.displayPath)

        val invalidParameters = laydrNavNotFound(
            LaydrNavKey(routeId = "Home", parameters = mapOf("unused" to "value")),
            routeMap,
        )
        assertEquals(LaydrNavNotFoundReason.InvalidParameters, invalidParameters.reason)
        assertTrue(invalidParameters.laydrKey is LaydrNavKey)
        assertNull(invalidParameters.displayPath)

        val outsideSection = laydrNavOutsideSectionNotFound(home.key().navKey(), routeMap)
        assertEquals(LaydrNavNotFoundReason.OutsideDeclaredSection, outsideSection.reason)
        assertEquals("/home", outsideSection.displayPath)
    }

    private object ForeignKey : NavKey

    private data class TestDestination(
        override val routeKey: LaydrRouteKey,
    ) : LaydrScreenDestination

    private class TestParameterlessScreenRouteRef(
        override val route: LaydrRoute,
    ) : LaydrParameterlessScreenRouteRef {
        override val defaultDestination: LaydrScreenDestination = TestDestination(route.key())
    }

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
