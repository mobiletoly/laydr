package dev.goquick.laydr.nav3.kmp

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions
import dev.goquick.laydr.compose.LaydrComposeScreenRouteDefinition
import dev.goquick.laydr.nav.runtime.LaydrNavEntryStateOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LaydrNavBindingLifecycleUiTest {
    @Test
    fun stackDisposalCancelsOnceAndRejectsOldNavigator() = assertDisposal(false)

    @Test
    fun sectionDisposalCancelsOnceAndRejectsOldNavigator() = assertDisposal(true)

    private fun assertDisposal(sectioned: Boolean) = runComposeUiTest {
        val shown = mutableStateOf(true)
        val graph = BindingUiGraph(mutableIntStateOf(0))
        var cancellations = 0
        lateinit var navigation: BindingUiNavigation
        setContent {
            if (shown.value) {
                BindingNavigation(graph, sectioned, null) { navigation = it }
            } else {
                BasicText("disposed")
            }
        }
        runOnIdle { navigation.launch({ cancellations++ }, {}) }
        onNodeWithText("pending").assertExists()
        val oldBack = navigation.back
        runOnIdle { shown.value = false }
        onNodeWithText("disposed").assertExists()
        runOnIdle {
            assertEquals(1, cancellations)
            assertFailsWith<IllegalStateException> { oldBack() }
        }
    }

    @Test
    fun retainedStackOwnerSurvivesRecreationWithOldNavigatorRejected() = assertRetainedRecreation(false)

    @Test
    fun retainedSectionOwnerRestoresPayloadResultAndReturnHistory() = assertRetainedRecreation(true)

    private fun assertRetainedRecreation(sectioned: Boolean) = runComposeUiTest {
        val shown = mutableStateOf(true)
        val graph = BindingUiGraph(mutableIntStateOf(0))
        val owner = LaydrNavEntryStateOwner()
        var cancellations = 0
        var completions = 0
        lateinit var navigation: BindingUiNavigation
        setContent {
            val stateHolder = rememberSaveableStateHolder()
            if (shown.value) {
                stateHolder.SaveableStateProvider("navigation") {
                    BindingNavigation(graph, sectioned, owner) { navigation = it }
                }
            } else {
                BasicText("hidden")
            }
        }
        runOnIdle { navigation.launch({ cancellations++ }, { completions++ }) }
        onNodeWithText("pending").assertExists()
        val oldBack = navigation.back
        runOnIdle { shown.value = false }
        onNodeWithText("hidden").assertExists()
        runOnIdle {
            assertEquals(0, cancellations)
            assertEquals(1, owner.payloadCount)
            assertFailsWith<IllegalStateException> { oldBack() }
            shown.value = true
        }
        onNodeWithText("pending").assertExists()
        onNodeWithText("Complete").performClick()
        runOnIdle {
            assertEquals(1, completions)
            assertEquals(0, cancellations)
            assertTrue(navigation.back())
        }
        onNodeWithText("Home").assertExists()
        runOnIdle { shown.value = false }
        onNodeWithText("hidden").assertExists()
        runOnIdle {
            owner.clear()
            assertEquals(0, cancellations)
            assertEquals(0, owner.payloadCount)
            assertEquals(0, owner.resultCount)
        }
    }

    @Test
    fun backingStackReplacementWithEqualKeysRefreshesCachedAccess() = runComposeUiTest {
        val generation = mutableIntStateOf(0)
        val graph = BindingUiGraph(generation)
        val backingStack = mutableStateOf(
            NavBackStack<NavKey>(graph.home.destination().navKey()),
            referentialEqualityPolicy(),
        )
        lateinit var stack: LaydrNavStack
        var completions = 0
        var cancellations = 0
        setContent {
            generation.intValue
            stack = rememberLaydrNavStack(
                routeDefinitions = graph.definitions,
                backStack = backingStack.value,
                notFoundContent = { error("Unexpected route: $it") },
            )
            NavDisplay(stack.backStack, onBack = { stack.navigator.back() }, entryProvider = stack.entryProvider)
        }
        runOnIdle {
            stack.navigator.pushForResult<String>(
                LaydrNavLaunch(graph.detail.destination(), payload = "replacement"),
                onCancel = { cancellations++ },
                onResult = { completions++ },
            )
        }
        onNodeWithText("replacement").assertExists()
        val oldNavigator = stack.navigator
        runOnIdle {
            val keys = backingStack.value.toList()
            backingStack.value = NavBackStack<NavKey>(*keys.toTypedArray())
            generation.intValue++
        }
        onNodeWithText("generation 1").assertExists()
        onNodeWithText("replacement").assertExists()
        onNodeWithText("Complete").performClick()
        runOnIdle {
            assertFailsWith<IllegalStateException> { oldNavigator.back() }
            assertEquals(1, completions)
            assertEquals(0, cancellations)
            assertTrue(stack.navigator.back())
        }
        onNodeWithText("Home").assertExists()
    }

    @Test
    fun ownerReplacementRefreshesCachedAccessWithoutClearingRetainedOwner() = runComposeUiTest {
        val generation = mutableIntStateOf(0)
        val shown = mutableStateOf(true)
        val graph = BindingUiGraph(generation)
        val originalOwner = LaydrNavEntryStateOwner()
        val replacementOwner = LaydrNavEntryStateOwner()
        val owner = mutableStateOf(originalOwner)
        lateinit var stack: LaydrNavStack
        var cancellations = 0
        setContent {
            generation.intValue
            if (shown.value) {
                stack = rememberLaydrNavStack(
                    routeDefinitions = graph.definitions,
                    initialDestination = graph.home.destination(),
                    entryStateOwner = owner.value,
                    notFoundContent = { error("Unexpected route: $it") },
                )
                NavDisplay(stack.backStack, onBack = { stack.navigator.back() }, entryProvider = stack.entryProvider)
            } else {
                BasicText("hidden")
            }
        }
        runOnIdle {
            stack.navigator.pushForResult<String>(
                LaydrNavLaunch(graph.detail.destination(), payload = "old owner"),
                onCancel = { cancellations++ },
            ) {}
        }
        onNodeWithText("old owner").assertExists()
        val oldNavigator = stack.navigator
        runOnIdle {
            owner.value = replacementOwner
            generation.intValue++
        }
        onNodeWithText("Home.Detail").assertExists()
        runOnIdle {
            assertFailsWith<IllegalStateException> { oldNavigator.back() }
            assertEquals(1, originalOwner.resultCount)
            assertEquals(0, replacementOwner.resultCount)
            assertEquals(0, cancellations)
            originalOwner.clear()
            assertEquals(1, cancellations)
            shown.value = false
        }
        onNodeWithText("hidden").assertExists()
        runOnIdle { replacementOwner.clear() }
    }

    @Test
    fun equivalentDefinitionsReplacementPreservesCachedEntry() = runComposeUiTest {
        val generation = mutableIntStateOf(0)
        val graph = BindingUiGraph(generation)
        lateinit var stack: LaydrNavStack
        var cancellations = 0
        setContent {
            generation.intValue
            val definitions = LaydrComposeRouteDefinitions(
                graph.definitions.routeMap,
                graph.definitions.screenRoutes.map { route ->
                    LaydrComposeScreenRouteDefinition(route) { match -> graph.definitions.screenContent(match) }
                },
                emptyList(),
            )
            stack = rememberLaydrNavStack(
                routeDefinitions = definitions,
                initialDestination = graph.home.destination(),
                notFoundContent = { error("Unexpected route: $it") },
            )
            NavDisplay(stack.backStack, onBack = { stack.navigator.back() }, entryProvider = stack.entryProvider)
        }
        runOnIdle {
            stack.navigator.pushForResult<String>(
                LaydrNavLaunch(graph.detail.destination(), payload = "definitions"),
                onCancel = { cancellations++ },
            ) {}
        }
        onNodeWithText("definitions").assertExists()
        repeat(2) {
            runOnIdle { generation.intValue++ }
            waitForIdle()
        }
        onNodeWithText("generation 2").assertExists()
        onNodeWithText("definitions").assertExists()
        runOnIdle { assertTrue(stack.navigator.back()) }
        onNodeWithText("Home").assertExists()
        runOnIdle { assertEquals(1, cancellations) }
    }
}

private class BindingUiNavigation(
    val launch: (onCancel: () -> Unit, onResult: (String) -> Unit) -> Unit,
    val back: () -> Boolean,
)

@Composable
private fun BindingNavigation(
    graph: BindingUiGraph,
    sectioned: Boolean,
    owner: LaydrNavEntryStateOwner?,
    onNavigation: (BindingUiNavigation) -> Unit,
) {
    if (sectioned) {
        val sections = rememberLaydrNavSections(
            routeDefinitions = graph.definitions,
            sectionSpecs = listOf(graph.home.section(), graph.other.section()),
            entryStateOwner = owner,
            notFoundContent = { error("Unexpected route: $it") },
        )
        onNavigation(BindingUiNavigation(
            launch = { cancel, result ->
                sections.navigator.pushForResult<String>(
                    LaydrNavLaunch(graph.otherDetail.destination(), payload = "pending"),
                    onCancel = cancel, onResult = result,
                )
            },
            back = { sections.back() },
        ))
        NavDisplay(sections.selectedBackStack, onBack = { sections.back() }, entryProvider = sections.entryProvider)
    } else {
        val stack = rememberLaydrNavStack(
            routeDefinitions = graph.definitions,
            initialDestination = graph.home.destination(),
            entryStateOwner = owner,
            notFoundContent = { error("Unexpected route: $it") },
        )
        onNavigation(BindingUiNavigation(
            launch = { cancel, result ->
                stack.navigator.pushForResult<String>(
                    LaydrNavLaunch(graph.detail.destination(), payload = "pending"),
                    onCancel = cancel, onResult = result,
                )
            },
            back = { stack.navigator.back() },
        ))
        NavDisplay(stack.backStack, onBack = { stack.navigator.back() }, entryProvider = stack.entryProvider)
    }
}
