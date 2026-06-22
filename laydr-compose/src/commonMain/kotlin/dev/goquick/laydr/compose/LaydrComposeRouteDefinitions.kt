// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.compose

import androidx.compose.runtime.Composable
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteMatch

/**
 * Route-owned screen render definition assembled by generated Laydr code.
 *
 * Apps normally create values of this type through generated route-local
 * `LaydrRouteDef` helpers instead of constructing it directly.
 *
 * @param route route descriptor rendered by this definition.
 * @param content screen render lambda for the route match.
 */
public class LaydrComposeScreenRouteDefinition public constructor(
    /**
     * Route descriptor rendered by this definition.
     */
    public val route: LaydrRoute,
    private val content: @Composable (match: LaydrRouteMatch) -> LaydrScreenContent,
) {
    /**
     * Returns screen content for [match].
     */
    @Composable
    public fun screenContent(match: LaydrRouteMatch): LaydrScreenContent {
        if (match.route != route) {
            error("Expected Laydr screen route ${route.id} but received ${match.route.id}")
        }
        return content(match)
    }
}

/**
 * Route-owned layout render definition assembled by generated Laydr code.
 *
 * Apps normally create values of this type through generated route-local
 * `LaydrRouteDef` helpers instead of constructing it directly.
 *
 * @param route route descriptor rendered by this definition.
 * @param content layout render lambda for the layout context.
 */
public class LaydrComposeLayoutRouteDefinition public constructor(
    /**
     * Route descriptor rendered by this definition.
     */
    public val route: LaydrRoute,
    private val content: @Composable (
        context: LaydrLayoutContext,
        content: @Composable () -> Unit,
    ) -> Unit,
) {
    /**
     * Renders [content] through the layout definition for [context].
     */
    @Composable
    public fun layoutContent(
        context: LaydrLayoutContext,
        content: @Composable () -> Unit,
    ) {
        if (context.route != route) {
            error("Expected Laydr layout route ${route.id} but received ${context.route.id}")
        }
        this.content(context, content)
    }
}

/**
 * Route-owned render definition for a route that is both a screen endpoint and
 * an inherited layout.
 *
 * @param screenDefinition screen render definition for the route.
 * @param layoutDefinition layout render definition for the same route.
 */
public class LaydrComposeScreenAndLayoutRouteDefinition public constructor(
    /**
     * Screen render definition for the route.
     */
    public val screenDefinition: LaydrComposeScreenRouteDefinition,
    /**
     * Layout render definition for the same route.
     */
    public val layoutDefinition: LaydrComposeLayoutRouteDefinition,
) {
    init {
        require(screenDefinition.route == layoutDefinition.route) {
            "Screen and layout definitions must target the same Laydr route"
        }
    }
}

/**
 * Generated collection of route-owned Compose render definitions.
 *
 * The collection is built from generated route descriptors and app-authored
 * `Route.kt` declarations. It owns route-to-render lookup but does not own app
 * navigation state, dependency injection, or platform lifecycle policy.
 *
 * @param routeMap framework-neutral generated route map.
 * @param screenDefinitions route-owned screen render definitions.
 * @param layoutDefinitions route-owned layout render definitions.
 */
public class LaydrComposeRouteDefinitions public constructor(
    /**
     * Framework-neutral generated route map.
     */
    public val routeMap: LaydrRouteMap,
    screenDefinitions: List<LaydrComposeScreenRouteDefinition>,
    layoutDefinitions: List<LaydrComposeLayoutRouteDefinition>,
) {
    /**
     * Framework-neutral app graph derived from [routeMap].
     */
    public val appGraph: LaydrAppGraph = LaydrAppGraph(routeMap)

    /**
     * Flattened descriptors for routes that render screens.
     */
    public val screenRoutes: List<LaydrRoute> = routeMap.screenRoutes

    /**
     * Flattened descriptors for routes that render layouts.
     */
    public val layoutRoutes: List<LaydrRoute> = routeMap.layoutRoutes

    /**
     * Top-level generated route descriptors.
     */
    public val routes: List<LaydrRoute> = routeMap.routes

    private val screenDefinitionsByRoute: Map<LaydrRoute, LaydrComposeScreenRouteDefinition> =
        screenDefinitions.uniqueScreenDefinitionsByRoute()
    private val layoutDefinitionsByRoute: Map<LaydrRoute, LaydrComposeLayoutRouteDefinition> =
        layoutDefinitions.uniqueLayoutDefinitionsByRoute()

    init {
        requireDefinitionSets()
    }

    private fun requireDefinitionSets() {
        requireDefinitions(
            expectedRoutes = screenRoutes,
            actualRoutes = screenDefinitionsByRoute.keys,
            kind = "screen",
        )
        requireDefinitions(
            expectedRoutes = layoutRoutes,
            actualRoutes = layoutDefinitionsByRoute.keys,
            kind = "layout",
        )
    }

    /**
     * Returns screen content for [match].
     */
    @Composable
    public fun screenContent(match: LaydrRouteMatch): LaydrScreenContent {
        val definition = screenDefinitionsByRoute[match.route]
            ?: error("Missing Laydr screen definition for route ${match.route.id}")
        return definition.screenContent(match = match)
    }

    /**
     * Renders [content] through the layout definition for [context].
     */
    @Composable
    public fun layoutContent(
        context: LaydrLayoutContext,
        content: @Composable () -> Unit,
    ) {
        val definition = layoutDefinitionsByRoute[context.route]
            ?: error("Missing Laydr layout definition for route ${context.route.id}")
        definition.layoutContent(context = context, content = content)
    }

    internal fun layoutDefinitionFor(route: LaydrRoute): LaydrComposeLayoutRouteDefinition? =
        layoutDefinitionsByRoute[route]
}

private fun List<LaydrComposeScreenRouteDefinition>.uniqueScreenDefinitionsByRoute():
    Map<LaydrRoute, LaydrComposeScreenRouteDefinition> {
    val definitions = linkedMapOf<LaydrRoute, LaydrComposeScreenRouteDefinition>()
    for (definition in this) {
        val previous = definitions.put(definition.route, definition)
        require(previous == null) {
            "Duplicate Laydr screen definition for route ${definition.route.id}"
        }
    }
    return definitions
}

private fun List<LaydrComposeLayoutRouteDefinition>.uniqueLayoutDefinitionsByRoute():
    Map<LaydrRoute, LaydrComposeLayoutRouteDefinition> {
    val definitions = linkedMapOf<LaydrRoute, LaydrComposeLayoutRouteDefinition>()
    for (definition in this) {
        val previous = definitions.put(definition.route, definition)
        require(previous == null) {
            "Duplicate Laydr layout definition for route ${definition.route.id}"
        }
    }
    return definitions
}

private fun requireDefinitions(
    expectedRoutes: List<LaydrRoute>,
    actualRoutes: Set<LaydrRoute>,
    kind: String,
) {
    val missing = expectedRoutes.firstOrNull { route -> route !in actualRoutes }
    require(missing == null) {
        "Missing Laydr $kind definition for route ${missing?.id}"
    }

    val unexpected = actualRoutes.firstOrNull { route -> route !in expectedRoutes }
    require(unexpected == null) {
        "Unexpected Laydr $kind definition for route ${unexpected?.id}"
    }
}
