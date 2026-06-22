// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.compose

import androidx.compose.runtime.Composable
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteMatch

/**
 * Renders the first screen route that matches [currentPath].
 *
 * The app owns navigation state and passes the current path into this host. The
 * app may pass low-level route-to-screen and route-to-layout lambdas with a
 * generated [LaydrRouteMap], while ordinary generated integrations should use
 * the overload that accepts [LaydrComposeRouteDefinitions].
 *
 * When no route matches, [notFoundContent] is rendered with [currentPath].
 * Laydr does not provide a default unmatched policy; apps may render a
 * not-found screen, redirect from their own state layer, or throw from the
 * supplied lambda.
 *
 * Layout routes are applied from outermost to innermost. Each inherited layout
 * receives a [LaydrLayoutContext] with the matched screen route and the
 * screen-owned layout values returned by [screenContent].
 */
@Composable
public fun LaydrRouteHost(
    currentPath: String,
    routeMap: LaydrRouteMap,
    notFoundContent: @Composable (path: String) -> Unit,
    layoutContent: @Composable (
        context: LaydrLayoutContext,
        content: @Composable () -> Unit,
    ) -> Unit = { _, content -> content() },
    screenContent: @Composable (match: LaydrRouteMatch) -> LaydrScreenContent,
) {
    val hostMatch = findLaydrRouteHostMatch(
        currentPath = currentPath,
        routeMap = routeMap,
    )

    if (hostMatch == null) {
        notFoundContent(currentPath)
    } else {
        val screen = screenContent(hostMatch.match)
        RenderLaydrRouteContent(
            index = 0,
            layoutRoutes = hostMatch.layoutRoutes,
            match = hostMatch.match,
            screen = screen,
            layoutContent = layoutContent,
        )
    }
}

/**
 * Renders the first screen route that matches [currentPath] using generated
 * route-owned Compose definitions.
 *
 * Route-local content resolves dependencies from app-owned Compose code.
 */
@Composable
public fun LaydrRouteHost(
    currentPath: String,
    routeDefinitions: LaydrComposeRouteDefinitions,
    notFoundContent: @Composable (path: String) -> Unit,
): Unit =
    LaydrRouteHost(
        currentPath = currentPath,
        routeMap = routeDefinitions.routeMap,
        notFoundContent = notFoundContent,
        layoutContent = { context, content ->
            routeDefinitions.layoutContent(
                context = context,
                content = content,
            )
        },
        screenContent = { match ->
            routeDefinitions.screenContent(match = match)
        },
    )

@Composable
private fun RenderLaydrRouteContent(
    index: Int,
    layoutRoutes: List<LaydrRoute>,
    match: LaydrRouteMatch,
    screen: LaydrScreenContent,
    layoutContent: @Composable (
        context: LaydrLayoutContext,
        content: @Composable () -> Unit,
    ) -> Unit,
) {
    val layoutRoute = layoutRoutes.getOrNull(index)
    if (layoutRoute == null) {
        screen.content()
    } else {
        layoutContent(layoutContextForRoute(layoutRoute, match, screen)) {
            RenderLaydrRouteContent(
                index = index + 1,
                layoutRoutes = layoutRoutes,
                match = match,
                screen = screen,
                layoutContent = layoutContent,
            )
        }
    }
}

internal fun layoutContextForRoute(
    layoutRoute: LaydrRoute,
    match: LaydrRouteMatch,
    screen: LaydrScreenContent,
): LaydrLayoutContext =
    LaydrLayoutContext(
        route = layoutRoute,
        match = match,
        layoutValues = screen.layoutValues,
    )

internal data class LaydrRouteHostMatch(
    val match: LaydrRouteMatch,
    val layoutRoutes: List<LaydrRoute>,
)

internal fun findLaydrRouteHostMatch(
    currentPath: String,
    routeMap: LaydrRouteMap,
): LaydrRouteHostMatch? {
    val key = routeMap.keyForPath(currentPath) ?: return null
    val route = routeMap.screenRouteFor(key) ?: return null
    val path = routeMap.pathFor(key) ?: return null
    val match = route.match(path) ?: return null

    return LaydrRouteHostMatch(
        match = match,
        layoutRoutes = routeMap.layoutChainFor(route),
    )
}
