// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.androidx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions

/**
 * Remembers complete Laydr AndroidX Nav3 section runtime wiring.
 */
@Composable
public fun <Data : Any> rememberLaydrNavSections(
    routeDefinitions: LaydrComposeRouteDefinitions,
    sectionSpecs: List<LaydrNavSectionSpec<Data>>,
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: (LaydrNavSectionEntryContext<Data>) -> Map<String, Any> = { emptyMap() },
    initialSection: LaydrNavSection<Data>? = null,
    notFoundContent: @Composable (notFound: LaydrNavNotFound) -> Unit,
): LaydrNavSections<Data> {
    val sectionSet = rememberLaydrNavSectionSet(
        routeDefinitions.appGraph,
        *sectionSpecs.toTypedArray(),
    )
    val coordinator = rememberLaydrNavSectionsCoordinator(
        sections = sectionSet,
        initialSection = initialSection ?: sectionSet.items.first(),
        sceneSupport = sceneSupport,
    )
    val entryProvider = laydrNavSectionEntryProvider(
        routeDefinitions = routeDefinitions,
        sectionSet = sectionSet,
        sceneSupport = sceneSupport,
        entryMetadata = entryMetadata,
        entryStore = coordinator.entryStore,
        notFoundContent = notFoundContent,
    )

    return remember(sectionSet, coordinator, entryProvider) {
        LaydrNavSections(
            sectionSet = sectionSet,
            coordinator = coordinator,
            entryProvider = entryProvider,
        )
    }
}
