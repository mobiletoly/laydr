package dev.goquick.laydr.nav3.androidx

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Rule
import androidx.navigation3.ui.NavDisplay
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions
import dev.goquick.laydr.compose.LaydrComposeScreenRouteDefinition
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteSegment
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.core.LaydrScreenRouteRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LaydrNavBindingUiTest {
    @get:Rule
    val rule = createComposeRule()
    @Test
    fun stackSceneReplacementPreservesCachedEntry(): Unit = with(rule) {
        val generation = mutableIntStateOf(0)
        val graph = BindingUiGraph(generation)
        lateinit var stack: LaydrNavStack
        var cancellationCount = 0
        var result: String? = null
        setContent {
            generation.intValue
            stack = rememberLaydrNavStack(
                routeDefinitions = graph.definitions,
                initialDestination = graph.home.destination(),
                sceneSupport = remember(generation.intValue) {
                    LaydrNavSceneSupport(dev.goquick.laydr.nav.runtime.LaydrNavSceneSupport.None)
                },
                notFoundContent = { error("Unexpected route: $it") },
            )
            NavDisplay(
                backStack = stack.backStack,
                onBack = { stack.navigator.back() },
                entryProvider = stack.entryProvider,
            )
        }
        runOnIdle {
            stack.navigator.pushForResult<String>(
                launch = LaydrNavLaunch(graph.detail.destination(), payload = "challenge"),
                onCancel = { cancellationCount++ },
                onResult = { result = it },
            )
        }
        onNodeWithText("challenge").assertExists()
        repeat(2) {
            runOnIdle { generation.intValue++ }
            waitForIdle()
        }
        onNodeWithText("generation 2").assertExists()
        onNodeWithText("challenge").assertExists()
        onNodeWithText("Complete").performClick()
        runOnIdle {
            assertEquals("accepted", result)
            assertTrue(stack.navigator.back())
        }
        onNodeWithText("Home").assertExists()
        runOnIdle {
            assertEquals(0, cancellationCount)
            assertEquals(1, stack.backStack.size)
        }
    }

    @Test
    fun sectionSceneReplacementPreservesHistoryAndPayload(): Unit = with(rule) {
        val generation = mutableIntStateOf(0)
        val graph = BindingUiGraph(generation)
        lateinit var sections: LaydrNavSections<Unit>
        var cancellationCount = 0
        var result: String? = null
        setContent {
            generation.intValue
            sections = rememberLaydrNavSections(
                routeDefinitions = graph.definitions,
                sectionSpecs = listOf(graph.home.section(), graph.other.section()),
                sceneSupport = remember(generation.intValue) {
                    LaydrNavSceneSupport(dev.goquick.laydr.nav.runtime.LaydrNavSceneSupport.None)
                },
                notFoundContent = { error("Unexpected route: $it") },
            )
            NavDisplay(
                backStack = sections.selectedBackStack,
                onBack = { sections.back() },
                entryProvider = sections.entryProvider,
            )
        }
        runOnIdle {
            sections.navigator.pushForResult<String>(
                launch = LaydrNavLaunch(graph.otherDetail.destination(), payload = "section challenge"),
                onCancel = { cancellationCount++ },
                onResult = { result = it },
            )
        }
        onNodeWithText("section challenge").assertExists()
        repeat(2) {
            runOnIdle { generation.intValue++ }
            waitForIdle()
        }
        onNodeWithText("generation 2").assertExists()
        onNodeWithText("section challenge").assertExists()
        onNodeWithText("Complete").performClick()
        runOnIdle {
            assertEquals("accepted", result)
            assertTrue(sections.canReturn)
            assertTrue(sections.back())
            assertEquals(graph.home.id, sections.selectedSection.route.route.id)
            assertEquals(0, cancellationCount)
        }
        onNodeWithText("Home").assertExists()
    }
}

internal class BindingUiGraph(generation: State<Int>) {
    val detail = LaydrRoute("Home.Detail", listOf(LaydrRouteSegment.Static("home"), LaydrRouteSegment.Static("detail")))
    val home = LaydrRoute("Home", listOf(LaydrRouteSegment.Static("home")), children = listOf(detail))
    val otherDetail = LaydrRoute("Other.Detail", listOf(LaydrRouteSegment.Static("other"), LaydrRouteSegment.Static("detail")))
    val other = LaydrRoute("Other", listOf(LaydrRouteSegment.Static("other")), children = listOf(otherDetail))
    private val routes = listOf(home, detail, other, otherDetail)
    val definitions = LaydrComposeRouteDefinitions(
        routeMap = LaydrRouteMap(listOf(home, other), routes, emptyList()),
        screenDefinitions = routes.map { route ->
            LaydrComposeScreenRouteDefinition(route) {
                // Reading in the screen factory forces the cached entry's lookup to recompose.
                val currentGeneration = generation.value
                LaydrScreenContent {
                    val payload = laydrNavPayloadOrNull<String>()
                    val sink = laydrNavResultSinkOrNull<String>()
                    Column {
                        BasicText(payload ?: route.id)
                        BasicText("generation $currentGeneration")
                        if (sink != null) {
                            BasicText("Complete", Modifier.clickable { sink.complete("accepted") })
                        }
                    }
                }
            }
        },
        layoutDefinitions = emptyList(),
    )
}

internal fun LaydrRoute.destination(): LaydrScreenDestination = object : LaydrScreenDestination {
    override val routeKey = LaydrRouteKey(id)
}

internal fun LaydrRoute.section(): LaydrNavSectionSpec<Unit> = laydrNavSection(
    route = object : LaydrScreenRouteRef { override val route: LaydrRoute = this@section },
    rootDestination = destination(),
)
