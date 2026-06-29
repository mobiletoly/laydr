package dev.goquick.laydr.nav3.kmp

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrScreenDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LaydrNavStackTest {
    @Test
    fun stackDelegatesToSingleStackController() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val provider: (NavKey) -> NavEntry<NavKey> = { key ->
            NavEntry(key = key) {
            }
        }
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = provider,
        )

        assertSame(coordinator, stack.coordinator)
        assertSame(coordinator.backStack, stack.backStack)
        assertSame(provider, stack.entryProvider)
        assertEquals("/contacts", stack.currentPath)
        assertFalse(stack.canPop)
        assertTrue(stack.sceneStrategies.isEmpty())

        stack.navigator.push(TestDestination(detail.key(mapOf("id" to "alpha"))))

        assertEquals("/contacts/alpha", stack.currentPath)
        assertTrue(stack.canPop)
        assertEquals(
            listOf(
                contacts.key().navKey(),
                detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            stack.backStack.toList(),
        )

        stack.reset(TestDestination(contacts.key()))

        assertEquals("/contacts", stack.currentPath)
        assertFalse(stack.canPop)
        assertEquals(listOf(contacts.key().navKey()), stack.backStack.toList())

        stack.navigator.push(TestDestination(detail.key(mapOf("id" to "alpha"))))

        assertTrue(stack.navigator.back())
        assertEquals(listOf(contacts.key().navKey()), stack.backStack.toList())
        assertFalse(stack.navigator.back())
    }

    @Test
    fun stackPushesPayloadEntriesAsDistinctRouteInstances() {
        val (appGraph, contacts) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )

        stack.navigator.push(
            LaydrNavLaunch(
                destination = TestDestination(contacts.key()),
                payload = "first",
            ),
        )
        val firstPayloadKey = stack.backStack.last() as LaydrNavKey
        stack.navigator.push(
            LaydrNavLaunch(
                destination = TestDestination(contacts.key()),
                payload = "second",
            ),
        )
        val secondPayloadKey = stack.backStack.last() as LaydrNavKey

        assertEquals(3, stack.backStack.size)
        assertEquals(2, stack.entryStore.payloadCount)
        assertEquals(contacts.key(), firstPayloadKey.toLaydrRouteKey())
        assertEquals(contacts.key(), secondPayloadKey.toLaydrRouteKey())
        assertFalse(firstPayloadKey == secondPayloadKey)
        assertTrue(stack.entryStore.lookupPayload(firstPayloadKey) is LaydrNavPayloadLookup.Present)
        assertTrue(stack.entryStore.lookupPayload(secondPayloadKey) is LaydrNavPayloadLookup.Present)
    }

    @Test
    fun stackPayloadEntriesArePrunedWhenEntriesLeaveStack() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )

        stack.navigator.push(
            LaydrNavLaunch(
                destination = TestDestination(detail.key(mapOf("id" to "alpha"))),
                payload = "alpha",
            ),
        )
        val payloadKey = stack.backStack.last() as LaydrNavKey
        assertEquals(1, stack.entryStore.payloadCount)
        assertTrue(stack.entryStore.lookupPayload(payloadKey) is LaydrNavPayloadLookup.Present)

        assertTrue(stack.navigator.back())

        assertEquals(0, stack.entryStore.payloadCount)
        assertTrue(stack.entryStore.lookupPayload(payloadKey) is LaydrNavPayloadLookup.Missing)

        stack.navigator.push(
            LaydrNavLaunch(
                destination = TestDestination(detail.key(mapOf("id" to "alpha"))),
                payload = "alpha",
            ),
        )
        assertEquals(1, stack.entryStore.payloadCount)
        stack.navigator.replace(
            LaydrNavLaunch(
                destination = TestDestination(detail.key(mapOf("id" to "bravo"))),
                payload = "bravo",
            ),
        )
        assertEquals(1, stack.entryStore.payloadCount)
        stack.reset(TestDestination(contacts.key()))
        assertEquals(0, stack.entryStore.payloadCount)
    }

    @Test
    fun stackPrunesPayloadsAfterDirectBackStackMutation() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )

        stack.navigator.push(
            LaydrNavLaunch(
                destination = TestDestination(detail.key(mapOf("id" to "alpha"))),
                payload = "alpha",
            ),
        )
        val payloadKey = stack.backStack.last() as LaydrNavKey
        assertEquals(1, stack.entryStore.payloadCount)

        stack.backStack.removeLastOrNull()

        assertTrue(stack.entryStore.lookupPayload(payloadKey) is LaydrNavPayloadLookup.Missing)
        assertEquals(0, stack.entryStore.payloadCount)
    }

    @Test
    fun entryStoreTreatsRestoredKeysAsMissingPayloads() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )

        stack.navigator.push(
            LaydrNavLaunch(
                destination = TestDestination(detail.key(mapOf("id" to "alpha"))),
                payload = "alpha",
            ),
        )
        val payloadKey = stack.backStack.last() as LaydrNavKey
        val restoredKey = LaydrNavKey(
            routeId = payloadKey.routeId,
            parameters = payloadKey.parameters,
        )

        assertTrue(stack.entryStore.lookupPayload(restoredKey) is LaydrNavPayloadLookup.Missing)
        assertEquals(payloadKey.toLaydrRouteKey(), restoredKey.toLaydrRouteKey())
    }

    @Test
    fun stackPushForResultRegistersDistinctEntryAndCompletesOnceWithoutPopping() {
        val (appGraph, contacts) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )
        val results = mutableListOf<TestRouteResult>()
        var cancelCount = 0

        stack.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(destination = TestDestination(contacts.key())),
            onCancel = { cancelCount++ },
        ) { result ->
            results += result
        }
        val resultKey = stack.backStack.last() as LaydrNavKey
        val lookup = stack.entryStore.lookupResult(resultKey)

        assertEquals(2, stack.backStack.size)
        assertEquals(1, stack.entryStore.resultCount)
        assertTrue(lookup is LaydrNavResultLookup.Present)

        @Suppress("UNCHECKED_CAST")
        val sink = lookup.sink as LaydrNavResultSink<TestRouteResult>
        sink.complete(TestRouteResult.Done("alpha"))
        sink.complete(TestRouteResult.Done("ignored"))

        assertEquals(listOf<TestRouteResult>(TestRouteResult.Done("alpha")), results)
        assertEquals(0, cancelCount)
        assertEquals(0, stack.entryStore.resultCount)
        assertEquals(2, stack.backStack.size)
    }

    @Test
    fun stackPushForResultCanPairPayloadAndResultOnSameEntry() {
        val (appGraph, contacts) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )

        stack.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(
                destination = TestDestination(contacts.key()),
                payload = "launch",
            ),
        ) {
        }
        val key = stack.backStack.last() as LaydrNavKey

        assertTrue(stack.entryStore.lookupPayload(key) is LaydrNavPayloadLookup.Present)
        assertTrue(stack.entryStore.lookupResult(key) is LaydrNavResultLookup.Present)
    }

    @Test
    fun stackResultCancellationIsOneShot() {
        val (appGraph, contacts) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )
        var cancelCount = 0
        val results = mutableListOf<TestRouteResult>()

        stack.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(destination = TestDestination(contacts.key())),
            onCancel = { cancelCount++ },
        ) { result ->
            results += result
        }
        val resultKey = stack.backStack.last() as LaydrNavKey
        val lookup = stack.entryStore.lookupResult(resultKey)

        assertTrue(lookup is LaydrNavResultLookup.Present)

        @Suppress("UNCHECKED_CAST")
        val sink = lookup.sink as LaydrNavResultSink<TestRouteResult>
        sink.cancel()
        sink.cancel()

        assertEquals(1, cancelCount)
        assertTrue(results.isEmpty())
        assertEquals(0, stack.entryStore.resultCount)
    }

    @Test
    fun stackCancelsPendingResultsWhenEntriesLeaveStack() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )
        var cancelCount = 0

        stack.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(destination = TestDestination(detail.key(mapOf("id" to "alpha")))),
            onCancel = { cancelCount++ },
        ) {
        }
        assertEquals(1, stack.entryStore.resultCount)

        assertTrue(stack.navigator.back())

        assertEquals(1, cancelCount)
        assertEquals(0, stack.entryStore.resultCount)

        stack.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(destination = TestDestination(detail.key(mapOf("id" to "alpha")))),
            onCancel = { cancelCount++ },
        ) {
        }
        stack.navigator.replace(TestDestination(detail.key(mapOf("id" to "bravo"))))

        assertEquals(2, cancelCount)

        stack.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(destination = TestDestination(detail.key(mapOf("id" to "charlie")))),
            onCancel = { cancelCount++ },
        ) {
        }
        stack.reset(TestDestination(contacts.key()))

        assertEquals(3, cancelCount)
    }

    @Test
    fun stackPrunesResultsAfterDirectBackStackMutation() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )
        var cancelCount = 0

        stack.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(destination = TestDestination(detail.key(mapOf("id" to "alpha")))),
            onCancel = { cancelCount++ },
        ) {
        }
        val resultKey = stack.backStack.last() as LaydrNavKey

        stack.backStack.removeLastOrNull()

        assertTrue(stack.entryStore.lookupResult(resultKey) is LaydrNavResultLookup.Missing)
        assertEquals(1, cancelCount)
        assertEquals(0, stack.entryStore.resultCount)
    }

    @Test
    fun entryStoreTreatsDestinationOnlyAndRestoredKeysAsMissingResults() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )

        assertTrue(stack.entryStore.lookupResult(contacts.key().navKey()) is LaydrNavResultLookup.Missing)

        stack.navigator.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(destination = TestDestination(detail.key(mapOf("id" to "alpha")))),
        ) {
        }
        val resultKey = stack.backStack.last() as LaydrNavKey
        val restoredKey = LaydrNavKey(
            routeId = resultKey.routeId,
            parameters = resultKey.parameters,
        )

        assertTrue(stack.entryStore.lookupResult(restoredKey) is LaydrNavResultLookup.Missing)
    }

    @Test
    fun stackExposesSceneSupportFacts() {
        val graph = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = graph.appGraph,
            backStack = NavBackStack<NavKey>(graph.contacts.key().navKey()),
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key ->
                NavEntry(key = key) {
                }
            },
        )

        stack.navigator.push(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))

        assertEquals(
            LaydrNavListDetailStackShape.ListAndDetail,
            stack.listDetailStackState?.shape,
        )
    }

    @Test
    fun stackNavigatorDestinationPushUsesAdaptiveDetailSelection() {
        val graph = testGraph()
        val stack = LaydrNavStack(
            coordinator = LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(graph.contacts.key().navKey()),
                sceneSupport = testSceneSupport(graph.contacts, graph.detail),
            ),
            entryProvider = { key -> NavEntry(key = key) {} },
        )
        val alpha = graph.detail.key(mapOf("id" to "alpha")).navKey()
        val bravo = graph.detail.key(mapOf("id" to "bravo")).navKey()
        val charlie = graph.detail.key(mapOf("id" to "charlie")).navKey()

        stack.navigator.push(TestDestination(alpha.toLaydrRouteKey()))
        stack.navigator.push(TestDestination(bravo.toLaydrRouteKey()))

        assertEquals(
            listOf(graph.contacts.key().navKey(), bravo),
            stack.backStack.toList(),
        )

        stack.navigator.push(
            LaydrNavLaunch(
                destination = TestDestination(charlie.toLaydrRouteKey()),
                payload = "charlie",
            ),
        )

        assertEquals(
            listOf(graph.contacts.key().navKey(), bravo, stack.backStack.last()),
            stack.backStack.toList(),
        )
        assertEquals(charlie.toLaydrRouteKey(), (stack.backStack.last() as LaydrNavKey).toLaydrRouteKey())
        assertEquals(1, stack.entryStore.payloadCount)
    }

    @Test
    fun validatesInitialDestinationKeyPath() {
        val (appGraph, contacts, _, settings) = testGraph()

        assertEquals(
            contacts.key().navKey(),
            appGraph.validatedNavKey(TestDestination(contacts.key())),
        )
        assertFailsWith<IllegalArgumentException> {
            appGraph.validatedNavKey(TestDestination(settings.key()))
        }
    }

    @Test
    fun pushesValidatedDestinationsUnlessAlreadyCurrent() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )

        assertEquals("/contacts", coordinator.currentPath)

        coordinator.push(TestDestination(contacts.key()))

        assertEquals(listOf(contacts.key().navKey()), coordinator.backStack.toList())

        coordinator.push(TestDestination(detail.key(mapOf("id" to "alpha"))))

        assertEquals(
            listOf(
                contacts.key().navKey(),
                detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            coordinator.backStack.toList(),
        )
        assertEquals("/contacts/alpha", coordinator.currentPath)
    }

    @Test
    fun pushesValidPathsAndIgnoresInvalidPaths() {
        val (appGraph, contacts, detail, settings) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )

        assertAccepted(coordinator.pushPath("/contacts/alpha"))
        assertEquals(
            listOf(
                contacts.key().navKey(),
                detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            coordinator.backStack.toList(),
        )
        assertAccepted(coordinator.pushPath("/contacts/alpha"))
        assertEquals(
            listOf(
                contacts.key().navKey(),
                detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            coordinator.backStack.toList(),
        )

        val stackBeforeInvalidPaths = coordinator.backStack.toList()

        assertRejected(
            coordinator.pushPath("/contacts/alpha/"),
            LaydrNavPathRejectionReason.UnsupportedPath,
        )
        assertRejected(
            coordinator.pushPath("/contacts/alpha?tab=edit"),
            LaydrNavPathRejectionReason.UnsupportedPath,
        )
        assertRejected(coordinator.pushPath("/missing"), LaydrNavPathRejectionReason.UnknownRoute)
        assertRejected(coordinator.pushPath(settings.buildPath()), LaydrNavPathRejectionReason.LayoutOnlyRoute)
        assertEquals(stackBeforeInvalidPaths, coordinator.backStack.toList())
    }

    @Test
    fun pushesAndReplacesExternalTargetsAndIgnoresInvalidTargets() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )

        assertFalse(coordinator.canPopLaydrEntry)

        val accepted = assertExternalAccepted(
            coordinator.pushExternalTarget("https://example.test/contacts/alpha?mode=preview#notes"),
        )

        assertEquals("/contacts/alpha", accepted.path)
        assertEquals("mode=preview", accepted.query)
        assertEquals("notes", accepted.fragment)
        assertTrue(coordinator.canPopLaydrEntry)
        assertEquals(
            listOf(
                contacts.key().navKey(),
                detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            coordinator.backStack.toList(),
        )

        val stackBeforeRejectedTarget = coordinator.backStack.toList()

        assertExternalRejected(
            coordinator.pushExternalTarget("https://example.test"),
            LaydrNavExternalTargetRejectionReason.UnsupportedTarget,
        )
        assertEquals(stackBeforeRejectedTarget, coordinator.backStack.toList())

        assertExternalAccepted(coordinator.replaceExternalTarget("/contacts/bravo?mode=replace"))
        assertEquals(
            listOf(
                contacts.key().navKey(),
                detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            coordinator.backStack.toList(),
        )
    }

    @Test
    fun replacesCurrentEntryOrAddsWhenEmpty() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )

        coordinator.replace(TestDestination(detail.key(mapOf("id" to "bravo"))))

        assertEquals(
            listOf(detail.key(mapOf("id" to "bravo")).navKey()),
            coordinator.backStack.toList(),
        )

        coordinator.backStack.clear()
        coordinator.replace(TestDestination(contacts.key()))

        assertEquals(listOf(contacts.key().navKey()), coordinator.backStack.toList())
    }

    @Test
    fun resetsStackToValidatedDestination() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(
                contacts.key().navKey(),
                detail.key(mapOf("id" to "alpha")).navKey(),
                ForeignKey,
            ),
        )

        coordinator.reset(TestDestination(detail.key(mapOf("id" to "bravo"))))

        assertEquals(
            listOf(
                contacts.key().navKey(),
                detail.key(mapOf("id" to "alpha")).navKey(),
                ForeignKey,
                detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            coordinator.backStack.toList(),
        )
        assertEquals("/contacts/bravo", coordinator.currentPath)
    }

    @Test
    fun navigatorPreservesForeignKeysAndMutatesOnlyTrailingLaydrSuffix() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(
                ForeignKey,
                contacts.key().navKey(),
                ForeignKey,
            ),
        )
        val stack = LaydrNavStack(
            coordinator = coordinator,
            entryProvider = { key -> NavEntry(key = key) {} },
        )

        stack.navigator.push(TestDestination(contacts.key()))
        stack.navigator.push(TestDestination(detail.key(mapOf("id" to "alpha"))))

        assertEquals(
            listOf(
                ForeignKey,
                contacts.key().navKey(),
                ForeignKey,
                contacts.key().navKey(),
                detail.key(mapOf("id" to "alpha")).navKey(),
            ),
            stack.backStack.toList(),
        )

        stack.navigator.replace(TestDestination(detail.key(mapOf("id" to "bravo"))))

        assertEquals(
            listOf(
                ForeignKey,
                contacts.key().navKey(),
                ForeignKey,
                contacts.key().navKey(),
                detail.key(mapOf("id" to "bravo")).navKey(),
            ),
            stack.backStack.toList(),
        )

        assertTrue(stack.navigator.back())
        assertTrue(stack.navigator.back())
        assertFalse(stack.navigator.back())
        assertEquals(listOf(ForeignKey, contacts.key().navKey(), ForeignKey), stack.backStack.toList())

        stack.reset(TestDestination(detail.key(mapOf("id" to "charlie"))))

        assertEquals(
            listOf(
                ForeignKey,
                contacts.key().navKey(),
                ForeignKey,
                detail.key(mapOf("id" to "charlie")).navKey(),
            ),
            stack.backStack.toList(),
        )
    }

    @Test
    fun replaceFailsWhenForeignKeyIsCurrent() {
        val graph = testGraph()
        val stack = LaydrNavStack(
            coordinator = LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(ForeignKey),
                sceneSupport = testSceneSupport(graph.contacts, graph.detail),
            ),
            entryProvider = { key -> NavEntry(key = key) {} },
        )

        val failure = assertFailsWith<IllegalStateException> {
            stack.navigator.replace(TestDestination(graph.contacts.key()))
        }

        assertTrue(failure.message?.contains("Use push(...) or owner-facing reset(...)") == true)
        assertEquals(listOf(ForeignKey), stack.backStack.toList())

        val navigatorFailure = assertFailsWith<IllegalStateException> {
            stack.navigator.replace(TestDestination(graph.detail.key(mapOf("id" to "alpha"))))
        }

        assertTrue(navigatorFailure.message?.contains("Use push(...) or owner-facing reset(...)") == true)
        assertEquals(listOf(ForeignKey), stack.backStack.toList())

        val externalFailure = assertFailsWith<IllegalStateException> {
            stack.replaceExternalTarget("/contacts/bravo")
        }

        assertTrue(externalFailure.message?.contains("Use push(...) or owner-facing reset(...)") == true)
        assertEquals(listOf(ForeignKey), stack.backStack.toList())
    }

    @Test
    fun replaceLaunchFailureOnForeignTopDoesNotRetainPayload() {
        val graph = testGraph()
        val stack = LaydrNavStack(
            coordinator = LaydrNavStackCoordinator(
                appGraph = graph.appGraph,
                backStack = NavBackStack<NavKey>(ForeignKey),
            ),
            entryProvider = { key -> NavEntry(key = key) {} },
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
    fun launchMetadataWinsOverStackMetadata() {
        val (appGraph, contacts, detail) = testGraph()
        val provider = laydrNavEntryProvider(
            routeMap = appGraph.routeMap,
            notFoundContent = { _ -> },
            sceneSupport = testSceneSupport(contacts, detail),
            entryMetadata = {
                mapOf(
                    "test:list" to "stack",
                    "app:source" to "stack",
                )
            },
            screenContent = {
                LaydrScreenContent {
                }
            },
        )
        val key = contacts
            .key()
            .navKey()
            .withEntryMetadata(
                LaydrNavEntryMetadata(
                    "test:list" to "launch",
                    "app:launch" to "metadata",
                ),
            )

        val entry = provider(key)

        assertEquals("launch", entry.metadata["test:list"])
        assertEquals("stack", entry.metadata["app:source"])
        assertEquals("metadata", entry.metadata["app:launch"])
    }

    @Test
    fun sceneSupportExpandRegisteredDetailDestinations() {
        val graph = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = graph.appGraph,
            backStack = NavBackStack<NavKey>(),
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val alpha = graph.detail.key(mapOf("id" to "alpha")).navKey()
        val bravo = graph.detail.key(mapOf("id" to "bravo")).navKey()

        coordinator.push(TestDestination(alpha.toLaydrRouteKey()))

        assertEquals(
            listOf(graph.contacts.key().navKey(), alpha),
            coordinator.backStack.toList(),
        )

        coordinator.push(TestDestination(alpha.toLaydrRouteKey()))

        assertEquals(
            listOf(graph.contacts.key().navKey(), alpha),
            coordinator.backStack.toList(),
        )

        coordinator.push(TestDestination(bravo.toLaydrRouteKey()))

        assertEquals(
            listOf(graph.contacts.key().navKey(), bravo),
            coordinator.backStack.toList(),
        )

        coordinator.backStack.add(ForeignKey)
        coordinator.push(TestDestination(alpha.toLaydrRouteKey()))

        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                bravo,
                ForeignKey,
                graph.contacts.key().navKey(),
                alpha,
            ),
            coordinator.backStack.toList(),
        )

        coordinator.replace(TestDestination(alpha.toLaydrRouteKey()))

        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                bravo,
                ForeignKey,
                graph.contacts.key().navKey(),
                alpha,
            ),
            coordinator.backStack.toList(),
        )

        coordinator.backStack.add(ForeignKey)
        coordinator.reset(TestDestination(bravo.toLaydrRouteKey()))

        assertEquals(
            listOf(
                graph.contacts.key().navKey(),
                bravo,
                ForeignKey,
                graph.contacts.key().navKey(),
                alpha,
                ForeignKey,
                graph.contacts.key().navKey(),
                bravo,
            ),
            coordinator.backStack.toList(),
        )
    }

    @Test
    fun sceneSupportExpandRegisteredDetailPaths() {
        val graph = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = graph.appGraph,
            backStack = NavBackStack<NavKey>(graph.contacts.key().navKey()),
            sceneSupport = testSceneSupport(graph.contacts, graph.detail),
        )
        val alpha = graph.detail.key(mapOf("id" to "alpha")).navKey()
        val bravo = graph.detail.key(mapOf("id" to "bravo")).navKey()

        assertAccepted(coordinator.replacePath("/contacts/alpha"))

        assertEquals(
            listOf(graph.contacts.key().navKey(), alpha),
            coordinator.backStack.toList(),
        )

        assertAccepted(coordinator.pushPath("/contacts/bravo"))

        assertEquals(
            listOf(graph.contacts.key().navKey(), bravo),
            coordinator.backStack.toList(),
        )

        coordinator.backStack.clear()

        assertAccepted(coordinator.pushPath("/contacts/bravo"))

        assertEquals(
            listOf(graph.contacts.key().navKey(), bravo),
            coordinator.backStack.toList(),
        )
    }

    @Test
    fun replacesValidPathsAndIgnoresInvalidPaths() {
        val (appGraph, contacts, detail, settings) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )

        assertAccepted(coordinator.replacePath("/contacts/bravo"))
        assertEquals(
            listOf(detail.key(mapOf("id" to "bravo")).navKey()),
            coordinator.backStack.toList(),
        )

        coordinator.backStack.clear()
        assertAccepted(coordinator.replacePath("/contacts"))
        assertEquals(listOf(contacts.key().navKey()), coordinator.backStack.toList())

        val stackBeforeInvalidPaths = coordinator.backStack.toList()

        assertRejected(coordinator.replacePath("/contacts/"), LaydrNavPathRejectionReason.UnsupportedPath)
        assertRejected(coordinator.replacePath("/missing"), LaydrNavPathRejectionReason.UnknownRoute)
        assertRejected(coordinator.replacePath(settings.buildPath()), LaydrNavPathRejectionReason.LayoutOnlyRoute)
        assertEquals(stackBeforeInvalidPaths, coordinator.backStack.toList())
    }

    @Test
    fun popsOnlyWhenStackHasMoreThanOneEntry() {
        val (appGraph, contacts, detail) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(
                contacts.key().navKey(),
                detail.key(mapOf("id" to "alpha")).navKey(),
            ),
        )

        assertTrue(coordinator.popLaydrEntry())
        assertFalse(coordinator.canPopLaydrEntry)
        assertEquals(listOf(contacts.key().navKey()), coordinator.backStack.toList())
        assertFalse(coordinator.popLaydrEntry())
        assertEquals(listOf(contacts.key().navKey()), coordinator.backStack.toList())
    }

    @Test
    fun exposesCurrentKeysAndNullPathForForeignKeys() {
        val (appGraph, contacts) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(contacts.key().navKey()),
        )

        assertSame(coordinator.backStack.last(), coordinator.currentKey)
        assertEquals(contacts.key().navKey(), coordinator.currentLaydrKey)
        assertEquals("/contacts", coordinator.currentPath)

        coordinator.backStack.add(ForeignKey)

        assertSame(ForeignKey, coordinator.currentKey)
        assertNull(coordinator.currentLaydrKey)
        assertNull(coordinator.currentPath)
    }

    @Test
    fun exposesNullCurrentPathForLayoutOnlyKeys() {
        val (appGraph, _, _, settings) = testGraph()
        val key = settings.key().navKey()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(key),
        )

        assertEquals(key, coordinator.currentLaydrKey)
        assertNull(coordinator.currentPath)
    }

    @Test
    fun rejectsUnknownLayoutOnlyOrInvalidDestinations() {
        val (appGraph, _, _, settings) = testGraph()
        val coordinator = LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = NavBackStack<NavKey>(),
        )

        assertFailsWith<IllegalArgumentException> {
            coordinator.push(TestDestination(LaydrRouteKey(routeId = "Missing")))
        }
        assertFailsWith<IllegalArgumentException> {
            coordinator.push(TestDestination(settings.key()))
        }
        assertFailsWith<IllegalArgumentException> {
            coordinator.push(
                TestDestination(
                    LaydrRouteKey(
                        routeId = "Contacts.ById",
                        parameters = mapOf("id" to " "),
                    ),
                ),
            )
        }
    }

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
        val settings = route(id = "Settings", segments = listOf(static("settings")))
        val routeMap = LaydrRouteMap(
            routes = listOf(contacts, settings),
            screenRoutes = listOf(contacts, detail),
            layoutRoutes = listOf(settings),
        )
        return TestGraph(
            appGraph = LaydrAppGraph(routeMap),
            contacts = contacts,
            detail = detail,
            settings = settings,
        )
    }

    private data class TestGraph(
        val appGraph: LaydrAppGraph,
        val contacts: LaydrRoute,
        val detail: LaydrRoute,
        val settings: LaydrRoute,
    )

    private data class TestDestination(
        override val routeKey: LaydrRouteKey,
    ) : LaydrScreenDestination

    private sealed interface TestRouteResult {
        data class Done(val value: String) : TestRouteResult
    }

    private object ForeignKey : NavKey
}

internal fun assertAccepted(result: LaydrNavPathResult): LaydrNavPathAccepted {
    assertTrue(result.accepted)
    assertTrue(result is LaydrNavPathAccepted)
    return result
}

internal fun assertRejected(
    result: LaydrNavPathResult,
    reason: LaydrNavPathRejectionReason,
): LaydrNavPathRejected {
    assertFalse(result.accepted)
    assertTrue(result is LaydrNavPathRejected)
    assertEquals(reason, result.reason)
    return result
}
