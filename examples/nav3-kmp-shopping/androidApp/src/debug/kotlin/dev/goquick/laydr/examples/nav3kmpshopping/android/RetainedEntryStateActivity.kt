package dev.goquick.laydr.examples.nav3kmpshopping.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation3.ui.NavDisplay
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions
import dev.goquick.laydr.compose.LaydrComposeScreenRouteDefinition
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteSegment
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.nav.runtime.LaydrNavEntryStateOwner
import dev.goquick.laydr.nav3.kmp.LaydrNavLaunch
import dev.goquick.laydr.nav3.kmp.LaydrNavResultSink
import dev.goquick.laydr.nav3.kmp.LaydrNavStackNavigator
import dev.goquick.laydr.nav3.kmp.pushForResult
import dev.goquick.laydr.nav3.kmp.rememberLaydrNavStack
import dev.goquick.laydr.nav3.kmp.requireLaydrNavPayload
import dev.goquick.laydr.nav3.kmp.requireLaydrNavResultSink

public class RetainedEntryStateActivity : ComponentActivity() {
    internal lateinit var model: RetainedEntryStateModel
    internal var currentNavigator: LaydrNavStackNavigator? = null
    internal var visiblePayload: String? = null
    internal var visibleResultSink: LaydrNavResultSink<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[RetainedEntryStateModel::class.java]
        val definitions = retainedTestRouteDefinitions(
            onAuthenticationEntry = { payload, resultSink ->
                visiblePayload = payload
                visibleResultSink = resultSink
            },
        )

        setContent {
            val stack = rememberLaydrNavStack(
                routeDefinitions = definitions,
                initialDestination = HomeDestination,
                entryStateOwner = model.entryStateOwner,
                notFoundContent = { notFound -> BasicText("Not found: ${notFound.reason}") },
            )
            SideEffect {
                currentNavigator = stack.navigator
            }
            LaunchedEffect(stack) {
                if (!model.launched) {
                    model.launched = true
                    stack.navigator.pushForResult<String>(
                        launch = LaydrNavLaunch(
                            destination = AuthenticationDestination,
                            payload = "configuration-challenge",
                        ),
                        onCancel = { model.cancelCount++ },
                    ) { result ->
                        model.resultCount++
                        model.lastResult = result
                    }
                }
            }
            NavDisplay(
                backStack = stack.backStack,
                onBack = { stack.navigator.back() },
                entryProvider = stack.entryProvider,
            )
        }
    }
}

public class RetainedEntryStateModel : ViewModel() {
    internal val entryStateOwner = LaydrNavEntryStateOwner()
    internal var launched: Boolean = false
    internal var resultCount: Int = 0
    internal var cancelCount: Int = 0
    internal var lastResult: String? = null

    override fun onCleared() {
        entryStateOwner.clear()
    }
}

private val HomeRoute = LaydrRoute(
    id = "Test.Home",
    segments = listOf(LaydrRouteSegment.Static("home")),
)
private val AuthenticationRoute = LaydrRoute(
    id = "Test.Authentication",
    segments = listOf(LaydrRouteSegment.Static("authentication")),
)

internal data object HomeDestination : LaydrScreenDestination {
    override val routeKey: LaydrRouteKey = LaydrRouteKey(routeId = HomeRoute.id)
}

private data object AuthenticationDestination : LaydrScreenDestination {
    override val routeKey: LaydrRouteKey = LaydrRouteKey(routeId = AuthenticationRoute.id)
}

private fun retainedTestRouteDefinitions(
    onAuthenticationEntry: (String, LaydrNavResultSink<String>) -> Unit,
): LaydrComposeRouteDefinitions =
    LaydrComposeRouteDefinitions(
        routeMap = LaydrRouteMap(
            routes = listOf(HomeRoute, AuthenticationRoute),
            screenRoutes = listOf(HomeRoute, AuthenticationRoute),
            layoutRoutes = emptyList(),
        ),
        screenDefinitions = listOf(
            LaydrComposeScreenRouteDefinition(HomeRoute) {
                LaydrScreenContent {
                    BasicText("Home")
                }
            },
            LaydrComposeScreenRouteDefinition(AuthenticationRoute) {
                LaydrScreenContent {
                    val payload = requireLaydrNavPayload<String>()
                    val resultSink = requireLaydrNavResultSink<String>()
                    SideEffect {
                        onAuthenticationEntry(payload, resultSink)
                    }
                    BasicText(payload)
                }
            },
        ),
        layoutDefinitions = emptyList(),
    )
