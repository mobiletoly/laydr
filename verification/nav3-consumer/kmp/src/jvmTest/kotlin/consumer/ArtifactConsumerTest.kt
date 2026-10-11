package consumer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.ui.NavDisplay
import consumer.generated.LaydrNavRoutes
import consumer.generated.LaydrRoutes
import dev.goquick.laydr.nav3.kmp.LaydrNavLaunch
import dev.goquick.laydr.nav3.kmp.LaydrNavStack
import dev.goquick.laydr.nav3.kmp.laydrNavSceneSupport
import dev.goquick.laydr.nav3.kmp.pushForResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ArtifactConsumerTest {
    @Test
    fun publishedGeneratedAndInlineApisSurviveRecomposition() = runComposeUiTest {
        assertTrue(consumerSmoke())
        val generation = mutableIntStateOf(0)
        lateinit var stack: LaydrNavStack
        var result: String? = null
        var cancellations = 0
        setContent {
            CompositionLocalProvider(LocalConsumerGeneration provides generation.intValue) {
                stack = LaydrNavRoutes.rememberStack(
                    initialDestination = LaydrRoutes.Home.destination(),
                    sceneSupport = laydrNavSceneSupport(),
                    notFoundContent = { error("Unexpected route: $it") },
                )
                NavDisplay(stack.backStack, onBack = { stack.navigator.back() }, entryProvider = stack.entryProvider)
            }
        }
        runOnIdle {
            stack.navigator.pushForResult<String>(
                LaydrNavLaunch(LaydrRoutes.Home.Detail.destination(), payload = "artifact payload"),
                onCancel = { cancellations++ },
                onResult = { result = it },
            )
        }
        onNodeWithText("artifact payload").assertExists()
        runOnIdle { generation.intValue++ }
        onNodeWithText("generation 1").assertExists()
        onNodeWithText("artifact payload").assertExists()
        onNodeWithText("Complete").performClick()
        runOnIdle {
            assertEquals("accepted", result)
            assertEquals(0, cancellations)
            assertTrue(stack.navigator.back())
        }
        onNodeWithText("home").assertExists()
    }
}
