// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.androidx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions
import dev.goquick.laydr.compose.LaydrLayoutContext
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteMatch
import dev.goquick.laydr.nav.runtime.LaydrNavResolvedEntry

/**
 * Creates an AndroidX Nav3 entry provider for generated Laydr route
 * descriptors.
 *
 * The returned function resolves Laydr [NavKey] values against [routeMap] and
 * returns a [NavEntry] that renders the matched screen content wrapped by
 * inherited Laydr layouts. Invalid, unknown, foreign, and layout-only keys
 * render [notFoundContent].
 */
public fun laydrNavEntryProvider(
    routeMap: LaydrRouteMap,
    notFoundContent: @Composable (notFound: LaydrNavNotFound) -> Unit,
    layoutContent: @Composable (
        context: LaydrLayoutContext,
        content: @Composable () -> Unit,
    ) -> Unit = { _, content -> content() },
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: (LaydrNavEntryContext) -> Map<String, Any> = { emptyMap() },
    screenContent: @Composable (match: LaydrRouteMatch) -> LaydrScreenContent,
): (NavKey) -> NavEntry<NavKey> =
    laydrNavEntryProvider(
        routeMap = routeMap,
        notFoundContent = notFoundContent,
        layoutContent = layoutContent,
        sceneSupport = sceneSupport,
        entryMetadata = entryMetadata,
        entryStore = null,
        screenContent = screenContent,
    )

internal fun laydrNavEntryProvider(
    routeMap: LaydrRouteMap,
    notFoundContent: @Composable (notFound: LaydrNavNotFound) -> Unit,
    layoutContent: @Composable (
        context: LaydrLayoutContext,
        content: @Composable () -> Unit,
    ) -> Unit = { _, content -> content() },
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: (LaydrNavEntryContext) -> Map<String, Any> = { emptyMap() },
    entryStore: LaydrNavEntryStore?,
    screenContent: @Composable (match: LaydrRouteMatch) -> LaydrScreenContent,
): (NavKey) -> NavEntry<NavKey> = { key ->
    val laydrKey = key as? LaydrNavKey
    val resolvedEntry = laydrKey
        ?.let { currentKey ->
            resolveLaydrNavEntry(
                key = currentKey,
                routeMap = routeMap,
            )
        }
    val metadata = if (laydrKey == null || resolvedEntry == null) {
        emptyMap()
    } else {
        sceneSupport.metadataFor(resolvedEntry.match.route) +
            entryMetadata(
                LaydrNavEntryContext(
                    key = laydrKey,
                    match = resolvedEntry.match,
                    routePlacement = routePlacementFor(
                        routeMap = routeMap,
                        route = resolvedEntry.match.route,
                    ),
                ),
            ) +
            laydrKey.entryMetadata.values
    }

    NavEntry(
        key = key,
        metadata = metadata,
    ) {
        if (resolvedEntry == null) {
            notFoundContent(laydrNavNotFound(key = key, routeMap = routeMap))
        } else {
            val screen = screenContent(resolvedEntry.match)
            CompositionLocalProvider(
                LocalLaydrNavPayloadLookup provides (
                    entryStore?.lookupPayload(laydrKey) ?: LaydrNavPayloadLookup.Missing
                ),
                LocalLaydrNavResultLookup provides (
                    entryStore?.lookupResult(laydrKey) ?: LaydrNavResultLookup.Missing
                ),
            ) {
                RenderLaydrNavContent(
                    index = 0,
                    resolvedEntry = resolvedEntry,
                    screen = screen,
                    layoutContent = layoutContent,
                )
            }
        }
    }
}

/**
 * Creates an AndroidX Nav3 entry provider backed by generated route-owned
 * Compose definitions.
 */
public fun laydrNavEntryProvider(
    routeDefinitions: LaydrComposeRouteDefinitions,
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: (LaydrNavEntryContext) -> Map<String, Any> = { emptyMap() },
    notFoundContent: @Composable (notFound: LaydrNavNotFound) -> Unit,
): (NavKey) -> NavEntry<NavKey> =
    laydrNavEntryProvider(
        routeDefinitions = routeDefinitions,
        sceneSupport = sceneSupport,
        entryMetadata = entryMetadata,
        entryStore = null,
        notFoundContent = notFoundContent,
    )

internal fun laydrNavEntryProvider(
    routeDefinitions: LaydrComposeRouteDefinitions,
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: (LaydrNavEntryContext) -> Map<String, Any> = { emptyMap() },
    entryStore: LaydrNavEntryStore?,
    notFoundContent: @Composable (notFound: LaydrNavNotFound) -> Unit,
): (NavKey) -> NavEntry<NavKey> =
    laydrNavEntryProvider(
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
        sceneSupport = sceneSupport,
        entryMetadata = entryMetadata,
        entryStore = entryStore,
    )

/**
 * App-owned metadata context for one resolved generated Laydr AndroidX Nav3
 * entry.
 */
public class LaydrNavEntryContext public constructor(
    /**
     * Laydr AndroidX Nav3 key resolved for this entry.
     */
    public val key: LaydrNavKey,
    /**
     * Matched generated route and decoded path parameters for this entry.
     */
    public val match: LaydrRouteMatch,
    /**
     * Placement of the matched route inside the generated route tree.
     */
    public val routePlacement: LaydrNavRoutePlacement,
)

/**
 * App-owned metadata context for one section-owned generated Laydr AndroidX
 * Nav3 entry.
 */
public class LaydrNavSectionEntryContext<Data : Any> public constructor(
    /**
     * Laydr AndroidX Nav3 key resolved for this entry.
     */
    public val key: LaydrNavKey,
    /**
     * Matched generated route and decoded path parameters for this entry.
     */
    public val match: LaydrRouteMatch,
    /**
     * Placement of the matched route inside the generated route tree.
     */
    public val routePlacement: LaydrNavRoutePlacement,
    /**
     * Section placement for this entry inside the declared app sections.
     */
    public val placement: LaydrNavSectionPlacement<Data>,
)

/**
 * Structured context for an AndroidX Nav3 key Laydr could not render.
 */
public data class LaydrNavNotFound(
    /**
     * Raw AndroidX Nav3 key supplied by Nav3.
     */
    public val key: NavKey,
    /**
     * Laydr key when [key] uses Laydr's AndroidX Nav3 key shape.
     */
    public val laydrKey: LaydrNavKey?,
    /**
     * Concrete generated path when Laydr can build one for [laydrKey].
     */
    public val displayPath: String?,
    /**
     * Reason Laydr could not render this key.
     */
    public val reason: LaydrNavNotFoundReason,
)

/**
 * Builds structured not-found context for [key].
 */
public fun laydrNavNotFound(
    key: NavKey,
    routeMap: LaydrRouteMap,
): LaydrNavNotFound {
    val laydrKey = key as? LaydrNavKey
        ?: return LaydrNavNotFound(
            key = key,
            laydrKey = null,
            displayPath = null,
            reason = LaydrNavNotFoundReason.ForeignKey,
        )

    val routeKey = laydrKey.toLaydrRouteKey()
    val displayPath = routeMap.pathFor(routeKey)
    return LaydrNavNotFound(
        key = key,
        laydrKey = laydrKey,
        displayPath = displayPath?.takeIf { path ->
            routeMap.routeFor(routeKey)?.match(path) != null
        },
        reason = dev.goquick.laydr.nav.runtime.laydrNavNotFoundReason(
            key = laydrKey.entryKey,
            routeMap = routeMap,
        ),
    )
}

internal fun laydrNavOutsideSectionNotFound(
    key: LaydrNavKey,
    routeMap: LaydrRouteMap,
): LaydrNavNotFound =
    LaydrNavNotFound(
        key = key,
        laydrKey = key,
        displayPath = key.path(routeMap),
        reason = LaydrNavNotFoundReason.OutsideDeclaredSection,
    )

internal fun <Data : Any> laydrNavSectionEntryProvider(
    routeDefinitions: LaydrComposeRouteDefinitions,
    sectionSet: LaydrNavSectionSet<Data>,
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: (LaydrNavSectionEntryContext<Data>) -> Map<String, Any> = { emptyMap() },
    entryStore: LaydrNavEntryStore?,
    notFoundContent: @Composable (notFound: LaydrNavNotFound) -> Unit,
): (NavKey) -> NavEntry<NavKey> = { key ->
    val laydrKey = key as? LaydrNavKey
    val resolvedEntry = laydrKey
        ?.let { currentKey ->
            resolveLaydrNavEntry(
                key = currentKey,
                routeMap = routeDefinitions.routeMap,
            )
        }
    val placement = laydrKey
        ?.let { currentKey -> sectionSet.placementFor(currentKey.toLaydrRouteKey()) }
    val metadata = if (laydrKey == null || resolvedEntry == null || placement == null) {
        emptyMap()
    } else {
        sceneSupport.metadataFor(resolvedEntry.match.route) +
            entryMetadata(
                LaydrNavSectionEntryContext(
                    key = laydrKey,
                    match = resolvedEntry.match,
                    routePlacement = routePlacementFor(
                        routeMap = routeDefinitions.routeMap,
                        route = resolvedEntry.match.route,
                    ),
                    placement = placement,
                ),
            ) +
            laydrKey.entryMetadata.values
    }

    NavEntry(
        key = key,
        metadata = metadata,
    ) {
        when {
            resolvedEntry == null ->
                notFoundContent(laydrNavNotFound(key = key, routeMap = routeDefinitions.routeMap))
            placement == null ->
                notFoundContent(
                    laydrNavOutsideSectionNotFound(
                        key = laydrKey,
                        routeMap = routeDefinitions.routeMap,
                    ),
                )
            else -> {
                val screen = routeDefinitions.screenContent(match = resolvedEntry.match)
                CompositionLocalProvider(
                    LocalLaydrNavPayloadLookup provides (
                        entryStore?.lookupPayload(laydrKey) ?: LaydrNavPayloadLookup.Missing
                    ),
                    LocalLaydrNavResultLookup provides (
                        entryStore?.lookupResult(laydrKey) ?: LaydrNavResultLookup.Missing
                    ),
                ) {
                    RenderLaydrNavContent(
                        index = 0,
                        resolvedEntry = resolvedEntry,
                        screen = screen,
                        layoutContent = { context, content ->
                            routeDefinitions.layoutContent(
                                context = context,
                                content = content,
                            )
                        },
                    )
                }
            }
        }
    }
}

internal fun resolveLaydrNavEntry(
    key: LaydrNavKey,
    routeMap: LaydrRouteMap,
): LaydrNavResolvedEntry? =
    dev.goquick.laydr.nav.runtime.resolveLaydrNavEntry(
        key = key.entryKey,
        routeMap = routeMap,
    )

private fun routePlacementFor(
    routeMap: LaydrRouteMap,
    route: dev.goquick.laydr.core.LaydrRoute,
): LaydrNavRoutePlacement =
    dev.goquick.laydr.nav.runtime.routePlacementFor(routeMap = routeMap, route = route)

@Composable
private fun RenderLaydrNavContent(
    index: Int,
    resolvedEntry: LaydrNavResolvedEntry,
    screen: LaydrScreenContent,
    layoutContent: @Composable (
        context: LaydrLayoutContext,
        content: @Composable () -> Unit,
    ) -> Unit,
) {
    val layoutRoute = resolvedEntry.layoutRoutes.getOrNull(index)
    if (layoutRoute == null) {
        screen.content()
    } else {
        layoutContent(
            LaydrLayoutContext(
                route = layoutRoute,
                match = resolvedEntry.match,
                layoutValues = screen.layoutValues,
            ),
        ) {
            RenderLaydrNavContent(
                index = index + 1,
                resolvedEntry = resolvedEntry,
                screen = screen,
                layoutContent = layoutContent,
            )
        }
    }
}
