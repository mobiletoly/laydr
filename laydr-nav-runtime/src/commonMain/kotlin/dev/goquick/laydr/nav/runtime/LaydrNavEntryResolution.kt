// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteMatch

/**
 * Adapter-neutral resolved entry data used to build platform Nav entries.
 */
public data class LaydrNavResolvedEntry(
    /**
     * Matched generated route and decoded path parameters.
     */
    public val match: LaydrRouteMatch,
    /**
     * Inherited layout routes from outermost to innermost.
     */
    public val layoutRoutes: List<LaydrRoute>,
)

/**
 * Placement of one resolved Laydr screen route inside the generated route tree.
 */
public class LaydrNavRoutePlacement public constructor(
    /**
     * Generated route chain from top-level route to matched route.
     */
    public val routeChain: List<LaydrRoute>,
    /**
     * Top-level generated route that contains the matched route.
     */
    public val topLevelRoute: LaydrRoute,
    /**
     * True when the matched route is itself the top-level generated route.
     */
    public val isTopLevelRoute: Boolean,
    /**
     * Number of renderable screen routes below [topLevelRoute].
     */
    public val depthFromTopLevelRoute: Int,
)

/**
 * Reason a navigation key could not be rendered by Laydr.
 */
public enum class LaydrNavNotFoundReason {
    /**
     * The key is not a Laydr navigation key.
     */
    ForeignKey,

    /**
     * The Laydr route id is not present in the generated route map.
     */
    UnknownRoute,

    /**
     * The Laydr route exists but is not renderable as a screen entry.
     */
    LayoutOnlyRoute,

    /**
     * The Laydr key has invalid route parameters for its route.
     */
    InvalidParameters,

    /**
     * The Laydr key is valid for the app graph but outside the declared app
     * section set that owns this entry provider.
     */
    OutsideDeclaredSection,
}

/**
 * Resolves [key] against [routeMap].
 */
public fun resolveLaydrNavEntry(
    key: LaydrNavEntryKey,
    routeMap: LaydrRouteMap,
): LaydrNavResolvedEntry? {
    val match = routeMap.laydrMatchForKey(key) ?: return null

    return LaydrNavResolvedEntry(
        match = match,
        layoutRoutes = routeMap.layoutChainFor(match.route),
    )
}

/**
 * Classifies why [key] could not render.
 */
public fun laydrNavNotFoundReason(
    key: LaydrNavEntryKey?,
    routeMap: LaydrRouteMap,
): LaydrNavNotFoundReason {
    key ?: return LaydrNavNotFoundReason.ForeignKey
    val routeKey = key.toRouteKey()
    val route = routeMap.routeFor(routeKey) ?: return LaydrNavNotFoundReason.UnknownRoute
    val path = routeMap.pathFor(routeKey) ?: return LaydrNavNotFoundReason.InvalidParameters
    if (routeMap.screenRouteFor(routeKey) == null) {
        return LaydrNavNotFoundReason.LayoutOnlyRoute
    }
    return if (route.match(path) != null) {
        LaydrNavNotFoundReason.InvalidParameters
    } else {
        LaydrNavNotFoundReason.InvalidParameters
    }
}

/**
 * Returns route placement for [route] inside [routeMap].
 */
public fun routePlacementFor(
    routeMap: LaydrRouteMap,
    route: LaydrRoute,
): LaydrNavRoutePlacement {
    val routeChain = routeMap.routeChainFor(route)
    val topLevelRoute = routeChain.firstOrNull() ?: route
    val depth = routeChain
        .drop(1)
        .count { chainedRoute -> chainedRoute in routeMap.screenRoutes }
    return LaydrNavRoutePlacement(
        routeChain = routeChain,
        topLevelRoute = topLevelRoute,
        isTopLevelRoute = topLevelRoute == route,
        depthFromTopLevelRoute = depth,
    )
}
