package dev.goquick.laydr.nav3.kmp

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrScreenDestination
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
        assertEquals("0", resultKey.entryToken)
        assertTrue(parentStack.entryStore.lookupPayload(resultKey) is LaydrNavPayloadLookup.Present)
        val resultLookup = parentStack.entryStore.lookupResult(resultKey)
        assertTrue(resultLookup is LaydrNavResultLookup.Present)

        @Suppress("UNCHECKED_CAST")
        val sink = resultLookup.sink as LaydrNavResultSink<TestRouteResult>
        sink.complete(TestRouteResult("signed-in"))

        assertEquals(listOf(TestRouteResult("signed-in")), results)
        assertEquals(0, cancelCount)
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
