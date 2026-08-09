package dev.goquick.laydr.nav3.kmp

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.core.LaydrScreenRouteRef
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions
import dev.goquick.laydr.compose.LaydrComposeScreenRouteDefinition
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.nav.runtime.LaydrNavEntryKey
import dev.goquick.laydr.nav.runtime.LaydrNavEntryStateOwner
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LaydrNavStackNavigatorLocalTest {
    @Test
    fun nullableAccessorReturnsNullWithoutProvider() {
        var navigator: LaydrNavStackNavigator? = null

        compose {
            navigator = laydrNavStackNavigatorOrNull()
        }

        assertNull(navigator)
    }

    @Test
    fun requireAccessorNamesProviderWhenMissing() {
        val failure = assertFailsWith<IllegalStateException> {
            compose {
                requireLaydrNavStackNavigator()
            }
        }

        assertTrue(failure.message?.contains("ProvideLaydrNavStackNavigator") == true)
    }

    @Test
    fun providedNavigatorCanPushForTypedResultOnParentStack() {
        val (appGraph, home, signIn) = testGraph()
        val parentStack = LaydrNavStack(
            coordinator = LaydrNavStackCoordinator(
                appGraph = appGraph,
                backStack = NavBackStack<NavKey>(
                    ForeignKey,
                    home.key().navKey(),
                ),
            ),
            entryProvider = { key -> NavEntry(key = key) {} },
        )
        var nestedNavigator: LaydrNavStackNavigator? = null

        compose {
            ProvideLaydrNavStackNavigator(parentStack.navigator) {
                nestedNavigator = requireLaydrNavStackNavigator()
            }
        }

        assertSame(parentStack.navigator, nestedNavigator)

        val results = mutableListOf<TestRouteResult>()
        var cancelCount = 0
        nestedNavigator?.pushForResult<TestRouteResult>(
            launch = LaydrNavLaunch(
                destination = TestDestination(signIn.key()),
                payload = "payload",
            ),
            onCancel = { cancelCount++ },
        ) { result ->
            results += result
        }

        assertEquals(
            listOf(
                ForeignKey,
                home.key().navKey(),
            ),
            parentStack.backStack.dropLast(1),
        )
        val resultKey = parentStack.backStack.last() as LaydrNavKey
        assertEquals(signIn.key(), resultKey.toLaydrRouteKey())
        assertTrue(resultKey.entryInstanceId?.isNotEmpty() == true)
        assertTrue(parentStack.entryStore.lookupPayload(resultKey) is LaydrNavPayloadLookup.Present)
        val resultLookup = parentStack.entryStore.lookupResult(resultKey)
        assertTrue(resultLookup is LaydrNavResultLookup.Present)

        @Suppress("UNCHECKED_CAST")
        val sink = resultLookup.sink as LaydrNavResultSink<TestRouteResult>
        sink.complete(TestRouteResult("signed-in"))

        assertEquals(listOf(TestRouteResult("signed-in")), results)
        assertEquals(0, cancelCount)
    }

    @Test
    fun activeStackObserverPrunesDirectlyRemovedEntryWithoutLookup() = runBlocking {
        val (appGraph, home, signIn) = testGraph()
        val definitions = LaydrComposeRouteDefinitions(
            routeMap = appGraph.routeMap,
            screenDefinitions = listOf(home, signIn).map { route ->
                LaydrComposeScreenRouteDefinition(route) { LaydrScreenContent {} }
            },
            layoutDefinitions = emptyList(),
        )
        val backStack = NavBackStack<NavKey>(home.key().navKey())
        val owner = LaydrNavEntryStateOwner()
        val frameClock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + frameClock)
        val composition = Composition(TestApplier(), recomposer)
        val recomposerJob = launch(frameClock) {
            recomposer.runRecomposeAndApplyChanges()
        }
        var stack: LaydrNavStack? = null
        var cancellationCount = 0
        var compositionPassCount = 0

        try {
            yield()
            composition.setContent {
                compositionPassCount++
                stack = rememberLaydrNavStack(
                    routeDefinitions = definitions,
                    backStack = backStack,
                    entryStateOwner = owner,
                    notFoundContent = { _ -> },
                )
            }
            frameClock.sendFrame(0L)
            recomposer.awaitIdle()

            stack?.navigator?.pushForResult<String>(
                launch = LaydrNavLaunch(
                    destination = TestDestination(signIn.key()),
                    payload = "challenge",
                ),
                onCancel = { cancellationCount++ },
            ) {}
            Snapshot.sendApplyNotifications()
            repeat(10) { yield() }
            val compositionPassesBeforeRemoval = compositionPassCount
            val removedKey = backStack.removeLastOrNull() as LaydrNavKey
            Snapshot.sendApplyNotifications()

            repeat(100) {
                if (cancellationCount == 0) {
                    yield()
                }
            }

            assertEquals(1, cancellationCount)
            assertEquals(0, stack?.entryStore?.payloadCount)
            assertEquals(0, stack?.entryStore?.resultCount)
            assertTrue(stack?.entryStore?.lookupPayload(removedKey) is LaydrNavPayloadLookup.Missing)
            assertEquals(compositionPassesBeforeRemoval, compositionPassCount)
        } finally {
            composition.dispose()
            recomposer.close()
            recomposerJob.cancel()
            recomposerJob.join()
            owner.clear()
        }
    }

    @Test
    fun sectionBindingPrunesInitiallyAndObserverPrunesWithoutRecomposition() = runBlocking {
        val (appGraph, home, signIn) = testGraph()
        val definitions = LaydrComposeRouteDefinitions(
            routeMap = appGraph.routeMap,
            screenDefinitions = listOf(home, signIn).map { route ->
                LaydrComposeScreenRouteDefinition(route) { LaydrScreenContent {} }
            },
            layoutDefinitions = emptyList(),
        )
        val sectionSpecs = listOf(
            laydrNavSection(screenRef(home), TestDestination(home.key())),
            laydrNavSection(screenRef(signIn), TestDestination(signIn.key())),
        )
        val owner = LaydrNavEntryStateOwner()
        var cancellationCount = 0
        owner.bind().keyForLaunchAndResult(
            key = LaydrNavEntryKey(routeId = signIn.id),
            launch = LaydrNavLaunch(
                destination = TestDestination(signIn.key()),
                payload = "orphan",
            ),
            resultType = String::class,
            onCancel = { cancellationCount++ },
            onResult = {},
        )
        val unrelatedState = mutableStateOf(0)
        val frameClock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + frameClock)
        val composition = Composition(TestApplier(), recomposer)
        val recomposerJob = launch(frameClock) {
            recomposer.runRecomposeAndApplyChanges()
        }
        var sections: LaydrNavSections<Unit>? = null
        var compositionPassCount = 0

        try {
            yield()
            composition.setContent {
                unrelatedState.value
                compositionPassCount++
                sections = rememberLaydrNavSections(
                    routeDefinitions = definitions,
                    sectionSpecs = sectionSpecs,
                    entryStateOwner = owner,
                    notFoundContent = { _ -> },
                )
            }
            frameClock.sendFrame(0L)
            recomposer.awaitIdle()

            assertEquals(1, cancellationCount)
            assertEquals(0, owner.payloadCount)
            assertEquals(0, owner.resultCount)

            unrelatedState.value++
            Snapshot.sendApplyNotifications()
            yield()
            frameClock.sendFrame(1L)
            recomposer.awaitIdle()
            assertEquals(1, cancellationCount)

            sections?.navigator?.pushForResult<String>(
                launch = LaydrNavLaunch(
                    destination = TestDestination(signIn.key()),
                    payload = "challenge",
                ),
                onCancel = { cancellationCount++ },
            ) {}
            Snapshot.sendApplyNotifications()
            repeat(10) { yield() }
            val compositionPassesBeforeRemoval = compositionPassCount
            val removedKey = sections?.selectedBackStack?.removeLastOrNull() as LaydrNavKey
            Snapshot.sendApplyNotifications()

            repeat(100) {
                if (cancellationCount == 1) {
                    yield()
                }
            }

            assertEquals(2, cancellationCount)
            assertEquals(0, owner.payloadCount)
            assertEquals(0, owner.resultCount)
            assertTrue(sections.entryStore.lookupPayload(removedKey) is LaydrNavPayloadLookup.Missing)
            assertEquals(compositionPassesBeforeRemoval, compositionPassCount)
        } finally {
            composition.dispose()
            recomposer.close()
            recomposerJob.cancel()
            recomposerJob.join()
            owner.clear()
        }
    }

    private fun testGraph(): TestGraph {
        val signIn = route(
            id = "Auth.SignIn",
            segments = listOf(static("auth"), static("sign-in")),
        )
        val home = route(id = "Home", segments = listOf(static("home")))
        val routeMap = LaydrRouteMap(
            routes = listOf(home, signIn),
            screenRoutes = listOf(home, signIn),
            layoutRoutes = emptyList(),
        )
        return TestGraph(
            appGraph = LaydrAppGraph(routeMap),
            home = home,
            signIn = signIn,
        )
    }

    private data class TestGraph(
        val appGraph: LaydrAppGraph,
        val home: LaydrRoute,
        val signIn: LaydrRoute,
    )

    private data class TestDestination(
        override val routeKey: LaydrRouteKey,
    ) : LaydrScreenDestination

    private fun screenRef(route: LaydrRoute): LaydrScreenRouteRef =
        object : LaydrScreenRouteRef {
            override val route: LaydrRoute = route
        }

    private data class TestRouteResult(val value: String)

    private object ForeignKey : NavKey
}

private fun compose(content: @Composable () -> Unit) {
    runBlocking {
        val frameClock = BroadcastFrameClock()
        val recomposer = Recomposer(coroutineContext + frameClock)
        val composition = Composition(TestApplier(), recomposer)
        val recomposerJob = launch(frameClock) {
            recomposer.runRecomposeAndApplyChanges()
        }

        try {
            yield()
            composition.setContent {
                content()
            }
            frameClock.sendFrame(0L)
            recomposer.awaitIdle()
        } finally {
            recomposer.close()
            recomposerJob.join()
            composition.dispose()
        }
    }
}

private class TestApplier : AbstractApplier<Unit>(Unit) {
    override fun insertBottomUp(
        index: Int,
        instance: Unit,
    ) {
    }

    override fun insertTopDown(
        index: Int,
        instance: Unit,
    ) {
    }

    override fun move(
        from: Int,
        to: Int,
        count: Int,
    ) {
    }

    override fun onClear() {
    }

    override fun remove(
        index: Int,
        count: Int,
    ) {
    }
}
