package dev.goquick.laydr.nav.runtime

import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteSegment
import dev.goquick.laydr.core.LaydrScreenDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LaydrNavRuntimeTest {
    @Test
    fun resolvesPathsAndExternalTargetsWithStructuredReasons() {
        val graph = testGraph()

        val accepted = assertIs<LaydrNavPathAccepted>(
            laydrNavPathResult("/contacts/alpha", graph.appGraph.routeMap),
        )
        assertEquals("Contacts.ById", accepted.key.routeId)
        assertEquals(mapOf("id" to "alpha"), accepted.key.parameters)

        val external = assertIs<LaydrNavExternalTargetAccepted>(
            laydrNavExternalTargetResult(
                "https://example.test/contacts/bravo?tab=notes#top",
                graph.appGraph.routeMap,
            ),
        )
        assertEquals("/contacts/bravo", external.path)
        assertEquals("tab=notes", external.query)
        assertEquals("top", external.fragment)

        assertEquals(
            LaydrNavPathRejectionReason.UnsupportedPath,
            assertIs<LaydrNavPathRejected>(
                laydrNavPathResult("/contacts/alpha?tab=notes", graph.appGraph.routeMap),
            ).reason,
        )
        assertEquals(
            LaydrNavPathRejectionReason.LayoutOnlyRoute,
            assertIs<LaydrNavPathRejected>(
                laydrNavPathResult("/settings", graph.appGraph.routeMap),
            ).reason,
        )
    }

    @Test
    fun entryStoreScopesPayloadsAndResultsToActiveEntryTokens() {
        val active = mutableListOf<LaydrNavEntryKey>()
        val store = LaydrNavEntryStore { active }
        val base = LaydrNavEntryKey("Contacts")
        var cancelled = 0
        var completed: String? = null

        val key = store.keyForLaunchAndResult(
            key = base,
            launch = LaydrNavLaunch(
                destination = TestDestination(base.toRouteKey()),
                payload = "payload",
            ),
            resultType = String::class,
            onCancel = { cancelled++ },
            onResult = { result -> completed = result as String },
        )
        active += key

        assertIs<LaydrNavPayloadLookup.Present>(store.lookupPayload(key))
        val sink = assertIs<LaydrNavResultLookup.Present>(store.lookupResult(key)).sink
        @Suppress("UNCHECKED_CAST")
        (sink as LaydrNavResultSink<String>).complete("done")
        assertEquals("done", completed)
        assertEquals(0, cancelled)

        active.clear()
        store.prune()
        assertEquals(0, store.payloadCount)
        assertEquals(0, store.resultCount)
    }

    @Test
    fun stackEngineMutatesOnlyTrailingLaydrSuffix() {
        val graph = testGraph()
        val contacts = LaydrNavEntryKey("Contacts")
        val detail = LaydrNavEntryKey("Contacts.ById", mapOf("id" to "alpha"))
        val stack = mutableListOf<TestKey>(ForeignKey, TestLaydrKey(contacts))
        val engine = LaydrNavStackEngine(
            appGraph = graph.appGraph,
            backStack = stack,
            keyAdapter = TestKeyAdapter,
        )

        engine.pushKey(detail)
        assertEquals(listOf(ForeignKey, TestLaydrKey(contacts), TestLaydrKey(detail)), stack)
        assertTrue(engine.popLaydrEntry())
        assertEquals(listOf(ForeignKey, TestLaydrKey(contacts)), stack)

        engine.resetKey(detail)
        assertEquals(listOf(ForeignKey, TestLaydrKey(detail)), stack)

        stack += ForeignKey
        val failure = runCatching { engine.replaceKey(contacts) }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
    }

    @Test
    fun sectionEngineRestoresReturnHistoryAcrossSectionStacks() {
        val graph = testGraph()
        val contacts = Section(
            id = "contacts",
            root = TestDestination(LaydrRouteKey("Contacts")),
        )
        val profile = Section(
            id = "profile",
            root = TestDestination(LaydrRouteKey("Profile")),
        )
        val selected = MutableSelectedSectionState(profile.id)
        val controllers = mapOf(
            contacts to stackEngine(graph, contacts.root),
            profile to stackEngine(graph, profile.root),
        )
        val history = mutableListOf<LaydrNavReturnEntry>()
        val store = LaydrNavEntryStore {
            controllers.values.flatMap { engine -> engine.allEntryKeys().orEmpty() }
        }
        val engine = LaydrNavSectionsEngine(
            sections = listOf(contacts, profile),
            initialSection = profile,
            selectedSectionState = selected,
            sectionStateId = { section -> section.id },
            sectionLabel = { section -> section.id },
            sectionForStateId = { stateId -> listOf(contacts, profile).firstOrNull { it.id == stateId } },
            sectionForDestination = { destination ->
                when (destination.routeKey.routeId) {
                    "Contacts", "Contacts.ById" -> contacts
                    "Profile" -> profile
                    else -> null
                }
            },
            sectionForEntryKey = { key ->
                when (key.routeId) {
                    "Contacts", "Contacts.ById" -> contacts
                    "Profile" -> profile
                    else -> null
                }
            },
            controllerFor = { section -> controllers.getValue(section) },
            returnHistory = history,
            entryStore = store,
        )

        engine.pushWithReturn(
            LaydrNavLaunch(
                destination = TestDestination(
                    LaydrRouteKey("Contacts.ById", mapOf("id" to "alpha")),
                ),
            ),
        )
        assertEquals(contacts, engine.selectedSection)
        assertTrue(engine.canReturn)

        assertTrue(engine.back())
        assertEquals(profile, engine.selectedSection)
        assertFalse(engine.canReturn)
    }

    @Test
    fun sectionReplaceFailureOnForeignTopDoesNotMutateStateOrTransientStore() {
        val graph = testGraph()
        val contacts = Section(
            id = "contacts",
            root = TestDestination(LaydrRouteKey("Contacts")),
        )
        val profile = Section(
            id = "profile",
            root = TestDestination(LaydrRouteKey("Profile")),
        )
        val selected = MutableSelectedSectionState(profile.id)
        val contactsController = stackEngine(graph, contacts.root)
        val profileController = stackEngine(graph, profile.root)
        val controllers = mapOf(
            contacts to contactsController,
            profile to profileController,
        )
        contactsController.backStack += ForeignKey
        val historyEntry = LaydrNavReturnEntry(
            sourceSectionStateId = profile.id,
            sourceBackStack = listOf(LaydrNavEntryKey("Profile")),
            targetSectionStateId = contacts.id,
            targetBackStackBeforeNavigation = listOf(LaydrNavEntryKey("Contacts")),
            targetKey = LaydrNavEntryKey("Contacts"),
        )
        val history = mutableListOf(historyEntry)
        val store = LaydrNavEntryStore {
            controllers.values.flatMap { engine -> engine.allEntryKeys().orEmpty() }
        }
        val engine = LaydrNavSectionsEngine(
            sections = listOf(contacts, profile),
            initialSection = profile,
            selectedSectionState = selected,
            sectionStateId = { section -> section.id },
            sectionLabel = { section -> section.id },
            sectionForStateId = { stateId -> listOf(contacts, profile).firstOrNull { it.id == stateId } },
            sectionForDestination = { destination ->
                when (destination.routeKey.routeId) {
                    "Contacts", "Contacts.ById" -> contacts
                    "Profile" -> profile
                    else -> null
                }
            },
            sectionForEntryKey = { key ->
                when (key.routeId) {
                    "Contacts", "Contacts.ById" -> contacts
                    "Profile" -> profile
                    else -> null
                }
            },
            controllerFor = { section -> controllers.getValue(section) },
            returnHistory = history,
            entryStore = store,
        )
        val contactsStackBefore = contactsController.backStack.toList()
        val profileStackBefore = profileController.backStack.toList()

        val failure = runCatching {
            engine.replace(
                LaydrNavLaunch(
                    destination = TestDestination(
                        LaydrRouteKey("Contacts.ById", mapOf("id" to "alpha")),
                    ),
                    payload = "payload",
                ),
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals(profile, engine.selectedSection)
        assertEquals(listOf(historyEntry), history)
        assertEquals(contactsStackBefore, contactsController.backStack)
        assertEquals(profileStackBefore, profileController.backStack)
        assertEquals(0, store.payloadCount)
    }

    @Test
    fun resolvesEntriesAndClassifiesNotFoundReasons() {
        val graph = testGraph()
        val detail = LaydrNavEntryKey("Contacts.ById", mapOf("id" to "alpha"))

        val resolved = resolveLaydrNavEntry(detail, graph.appGraph.routeMap)
        assertEquals("Contacts.ById", resolved?.match?.route?.id)
        assertEquals(
            LaydrNavNotFoundReason.UnknownRoute,
            laydrNavNotFoundReason(LaydrNavEntryKey("Missing"), graph.appGraph.routeMap),
        )
        assertEquals(
            LaydrNavNotFoundReason.LayoutOnlyRoute,
            laydrNavNotFoundReason(LaydrNavEntryKey("Settings"), graph.appGraph.routeMap),
        )
        assertNull(resolveLaydrNavEntry(LaydrNavEntryKey("Missing"), graph.appGraph.routeMap))
    }

    private fun stackEngine(
        graph: TestGraph,
        root: TestDestination,
    ): LaydrNavStackEngine<TestKey> =
        LaydrNavStackEngine(
            appGraph = graph.appGraph,
            backStack = mutableListOf(TestLaydrKey(root.routeKey.navEntryKey())),
            keyAdapter = TestKeyAdapter,
        )

    private fun testGraph(): TestGraph {
        val detail = route(
            id = "Contacts.ById",
            segments = listOf(static("contacts"), dynamic("id")),
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
            screenRoutes = listOf(contacts, detail, profile),
            layoutRoutes = listOf(settings),
        )
        return TestGraph(
            appGraph = LaydrAppGraph(routeMap),
        )
    }

    private data class TestGraph(
        val appGraph: LaydrAppGraph,
    )

    private data class TestDestination(
        override val routeKey: LaydrRouteKey,
    ) : LaydrScreenDestination

    private data class Section(
        val id: String,
        val root: TestDestination,
    )

    private class MutableSelectedSectionState(
        override var value: String,
    ) : LaydrNavSelectedSectionState

    private sealed interface TestKey

    private data object ForeignKey : TestKey

    private data class TestLaydrKey(
        val entryKey: LaydrNavEntryKey,
    ) : TestKey

    private object TestKeyAdapter : LaydrNavKeyAdapter<TestKey> {
        override fun entryKey(key: TestKey): LaydrNavEntryKey? =
            (key as? TestLaydrKey)?.entryKey

        override fun key(entryKey: LaydrNavEntryKey): TestKey =
            TestLaydrKey(entryKey)
    }
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
