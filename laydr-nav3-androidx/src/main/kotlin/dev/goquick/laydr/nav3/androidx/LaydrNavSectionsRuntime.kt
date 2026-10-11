// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.androidx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions

/**
 * Remembers complete Laydr AndroidX Nav3 section runtime wiring.
 *
 * Each section owns an AndroidX [androidx.navigation3.runtime.NavBackStack]
 * remembered with AndroidX saved-stack state. Restored Laydr entries contain
 * route id and route parameters only; payloads, route-result callbacks, entry
 * tokens, and entry metadata are transient.
 *
 * Equivalent scene reconstruction preserves payloads, results, and return
 * history. One binding covers the helper composition and owner; cleanup reads
 * the current aggregate of section stacks. Acquire current wrappers after
 * reconfiguration; composition recreation still invalidates old bindings.
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
    val currentEntryStore = rememberUpdatedState(coordinator.entryStore)
    val entryProvider = laydrNavSectionEntryProvider(
        routeDefinitions = routeDefinitions,
        sectionSet = sectionSet,
        sceneSupport = sceneSupport,
        entryMetadata = entryMetadata,
        entryStore = { currentEntryStore.value },
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
