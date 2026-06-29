package dev.goquick.laydr.nav3.androidx

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteSegment
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.core.LaydrScreenRouteRef
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LaydrNavAndroidxAdapterTest {
    @Test
    fun convertsDestinationsAndPathsToAndroidxKeys() {
        val graph = testGraph()

        assertEquals(
            LaydrNavKey(routeId = "Contacts.ById", parameters = mapOf("id" to "alpha")),
            TestDestination(graph.detail.key(mapOf("id" to "alpha"))).navKey(),
        )
        assertEquals(
            LaydrNavKey(routeId = "Contacts.ById", parameters = mapOf("id" to "a/b")),
            laydrNavKeyForPath("/contacts/a%2Fb", graph.appGraph.routeMap),
        )

        val accepted = assertAccepted(
            laydrNavPathResult("/contacts/alpha", graph.appGraph.routeMap),
        )
        assertEquals(graph.detail.key(mapOf("id" to "alpha")).navKey(), accepted.key)
        assertRejected(
            laydrNavPathResult("/contacts/alpha/", graph.appGraph.routeMap),
            LaydrNavPathRejectionReason.UnsupportedPath,
        )
        assertRejected(
            laydrNavPathResult("/settings", graph.appGraph.routeMap),
            LaydrNavPathRejectionReason.LayoutOnlyRoute,
        )
    }

    @Test
    fun keySerializationRestoresOnlyRouteIdentity() {
        val key = LaydrNavKey(
            routeId = "Contacts.ById",
            parameters = mapOf("id" to "alpha"),
            entryToken = "token",
            entryMetadata = LaydrNavEntryMetadata("transition" to "modal"),
        )

        val encoded = Json.encodeToString(LaydrNavKeySerializer, key)
        val restored = Json.decodeFromString(LaydrNavKeySerializer, encoded)

        assertEquals("""{"routeId":"Contacts.ById","parameters":{"id":"alpha"}}""", encoded)
        assertEquals(LaydrNavKey(routeId = "Contacts.ById", parameters = mapOf("id" to "alpha")), restored)
        assertNull(restored.entryToken)
        assertEquals(emptyMap(), restored.entryMetadata.values)
    }

    @Test
    fun stackDelegatesMutationsToSharedRuntime() {
        val graph = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = graph.appGraph,
            backStack = NavBackStack<NavKey>(graph.contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> androidx.navigation3.runtime.NavEntry(key = key) {} },
        )

        stack.navigator.push(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))

        assertEquals("/contacts/alpha", stack.currentPath)
        assertTrue(stack.canPop)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            stack.backStack.toList(),
        )

        stack.reset(TestDestination(graph.contacts.key()))

        assertEquals("/contacts", stack.currentPath)
        assertFalse(stack.canPop)
        assertEquals(listOf(graph.contacts.key().navKey()), stack.backStack.toList())

        stack.navigator.push(TestDestination(graph.detail.key(mapOf("id" to "bravo"))))
        assertTrue(stack.navigator.back())
        assertEquals(listOf(graph.contacts.key().navKey()), stack.backStack.toList())
        assertFalse(stack.navigator.back())
    }

    @Test
    fun stackPreservesForeignPrefixesAndRejectsReplacingForeignTop() {
        val graph = testGraph()
        val stack = LaydrNavStack(
            coordinator = LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(ForeignKey),
            ),
            entryProvider = { key -> androidx.navigation3.runtime.NavEntry(key = key) {} },
        )

        val failure = assertFailsWith<IllegalStateException> {
            stack.navigator.replace(TestDestination(graph.contacts.key()))
        }

        assertTrue(failure.message?.contains("Use push(...) or owner-facing reset(...)") == true)
        assertEquals(listOf(ForeignKey), stack.backStack.toList())

        stack.navigator.push(TestDestination(graph.contacts.key()))
        stack.navigator.push(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))

        assertEquals(
            listOf(
                ForeignKey,
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            stack.backStack.toList(),
        )
    }

    @Test
    fun replaceLaunchFailureOnForeignTopDoesNotRetainPayload() {
        val graph = testGraph()
        val stack = LaydrNavStack(
            coordinator = LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(ForeignKey),
            ),
            entryProvider = { key -> androidx.navigation3.runtime.NavEntry(key = key) {} },
        )

        assertFailsWith<IllegalStateException> {
            stack.navigator.replace(
                LaydrNavLaunch(
                    destination = TestDestination(graph.detail.key(mapOf("id" to "alpha"))),
                    payload = "payload",
                ),
            )
        }

        assertEquals(listOf(ForeignKey), stack.backStack.toList())
        assertEquals(0, stack.entryStore.payloadCount)
    }

    @Test
    fun stackPrunesPayloadsAndCompletesResultsThroughSharedEntryStore() {
        val graph = testGraph()
        val stack = LaydrNavStack(
            coordinator = LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(graph.contacts.key().navKey()),
            ),
            entryProvider = { key -> androidx.navigation3.runtime.NavEntry(key = key) {} },
        )
        val results = mutableListOf<TestRouteResult>()
        var cancelCount = 0

        stack.navigator.push(
            LaydrNavLaunch(
                destination = TestDestination(graph.detail.key(mapOf("id" to "alpha"))),
                payload = "payload",
            ),
        )
        val payloadKey = stack.backStack.last() as LaydrNavKey

        assertTrue(stack.entryStore.lookupPayload(payloadKey) is LaydrNavPayloadLookup.Present)
        assertEquals(1, stack.entryStore.payloadCount)

        stack.navigator.replace(TestDestination(graph.detail.key(mapOf("id" to "bravo"))))

        assertTrue(stack.entryStore.lookupPayload(payloadKey) is LaydrNavPayloadLookup.Missing)
        assertEquals(0, stack.entryStore.payloadCount)

        stack.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(destination = TestDestination(graph.detail.key(mapOf("id" to "charlie")))),
            onCancel = { cancelCount++ },
        ) { result ->
            results += result
        }
        val resultKey = stack.backStack.last() as LaydrNavKey
        val lookup = stack.entryStore.lookupResult(resultKey)

        assertTrue(lookup is LaydrNavResultLookup.Present)

        @Suppress("UNCHECKED_CAST")
        val sink = lookup.sink as LaydrNavResultSink<TestRouteResult>
        sink.complete(TestRouteResult("done"))
        sink.cancel()

        assertEquals(listOf(TestRouteResult("done")), results)
        assertEquals(0, cancelCount)
        assertEquals(0, stack.entryStore.resultCount)
    }

    @Test
    fun notFoundClassifiesForeignMissingAndLayoutOnlyKeys() {
        val graph = testGraph()

        assertEquals(
            LaydrNavNotFoundReason.ForeignKey,
            laydrNavNotFound(ForeignKey, graph.appGraph.routeMap).reason,
        )
        assertEquals(
            LaydrNavNotFoundReason.UnknownRoute,
            laydrNavNotFound(LaydrNavKey(routeId = "Missing"), graph.appGraph.routeMap).reason,
        )
        assertEquals(
            LaydrNavNotFoundReason.LayoutOnlyRoute,
            laydrNavNotFound(graph.settings.key().navKey(), graph.appGraph.routeMap).reason,
        )
    }

    @Test
    fun sectionRuntimeRoutesOperationsThroughOwningStacks() {
        val graph = testGraph()
        val sections = laydrNavSectionSet(
            graph.appGraph,
            laydrNavSection(screenRef(graph.contacts), TestDestination(graph.contacts.key())),
            laydrNavSection(screenRef(graph.profile), TestDestination(graph.profile.key())),
        )
        val controllers = sections.items.associateWith { section ->
            LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(section.rootDestination.navKey()),
            )
        }
        val coordinator = LaydrNavSectionsCoordinator(
            sections = sections,
            initialSection = sections.items.first(),
            sectionControllers = controllers,
        )

        coordinator.push(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))

        assertSame(sections.items.first(), coordinator.selectedSection)
        assertEquals("/contacts/alpha", coordinator.currentPath)
        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                graph.detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            controllers.getValue(sections.items.first()).backStack.toList(),
        )

        coordinator.pushWithReturn(TestDestination(graph.profile.key()))

        assertSame(sections.items.last(), coordinator.selectedSection)
        assertEquals("/profile", coordinator.currentPath)
        assertTrue(coordinator.canReturn)
        assertTrue(coordinator.back())
        assertSame(sections.items.first(), coordinator.selectedSection)
        assertEquals("/contacts/alpha", coordinator.currentPath)
    }

    @Test
    fun sectionSetFindsPathAndExternalTargetOwners() {
        val graph = testGraph()
        val sections = laydrNavSectionSet(
            graph.appGraph,
            laydrNavSection(screenRef(graph.contacts), TestDestination(graph.contacts.key())),
            laydrNavSection(screenRef(graph.profile), TestDestination(graph.profile.key())),
        )

        assertSame(sections.items.first(), sections.sectionForPath("/contacts/alpha"))
        assertSame(sections.items.last(), sections.sectionForExternalTarget("/profile?from=test"))
        assertNull(sections.sectionForPath("/settings"))
        assertNull(sections.sectionForPath("/missing"))
    }

    private fun testGraph(): TestGraph {
        val edit = route(
            id = "Contacts.ById.Edit",
            segments = listOf(static("contacts"), dynamic("id"), static("edit")),
        )
        val detail = route(
            id = "Contacts.ById",
            segments = listOf(static("contacts"), dynamic("id")),
            children = listOf(edit),
        )
        val contacts = route(
            id = "Contacts",
            segments = listOf(static("contacts")),
            children = listOf(detail),
        )
        val profile = route(id = "Profile", segments = listOf(static("profile")))
        val settings = route(id = "Settings", segments = listOf(static("settings")))
        val routeMap = LaydrRouteMap(
            routes = listOf(contacts, profile, settings),
            screenRoutes = listOf(contacts, detail, edit, profile),
            layoutRoutes = listOf(settings),
        )
        return TestGraph(
            appGraph = LaydrAppGraph(routeMap),
            contacts = contacts,
            detail = detail,
            profile = profile,
            settings = settings,
        )
    }

    private data class TestGraph(
        val appGraph: LaydrAppGraph,
        val contacts: LaydrRoute,
        val detail: LaydrRoute,
        val profile: LaydrRoute,
        val settings: LaydrRoute,
    )

    private data class TestDestination(
        override val routeKey: LaydrRouteKey,
    ) : LaydrScreenDestination

    private data class TestRouteResult(val value: String)

    private object ForeignKey : NavKey
}

private fun route(
    id: String,
    segments: List<LaydrRouteSegment>,
    children: List<LaydrRoute> = emptyList(),
): LaydrRoute =
    LaydrRoute(
        id = id,
        segments = segments,
        children = children,
    )

private fun static(value: String): LaydrRouteSegment.Static =
    LaydrRouteSegment.Static(value)

private fun dynamic(parameterName: String): LaydrRouteSegment.Dynamic =
    LaydrRouteSegment.Dynamic(parameterName)

private fun screenRef(route: LaydrRoute): LaydrScreenRouteRef =
    object : LaydrScreenRouteRef {
        override val route: LaydrRoute = route
    }

private fun assertAccepted(result: LaydrNavPathResult): LaydrNavPathAccepted {
    assertTrue(result.accepted)
    assertTrue(result is LaydrNavPathAccepted)
    return result
}

private fun assertRejected(
    result: LaydrNavPathResult,
    reason: LaydrNavPathRejectionReason,
): LaydrNavPathRejected {
    assertFalse(result.accepted)
    assertTrue(result is LaydrNavPathRejected)
    assertEquals(reason, result.reason)
    return result
}
