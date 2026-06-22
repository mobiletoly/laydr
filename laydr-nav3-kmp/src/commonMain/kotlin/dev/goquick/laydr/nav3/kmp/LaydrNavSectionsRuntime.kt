// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions

/**
 * Remembers complete Laydr Nav section runtime wiring.
 *
 * The returned [LaydrNavSections] owns selected-section state, one Nav3 stack
 * per declared section, payload/result entry storage for section launches, the
 * selected-stack entry provider, and a route-facing section navigator. Apps
 * still render Nav3 `NavDisplay` directly and own all visual chrome.
 *
 * @param routeDefinitions generated route-owned Compose definitions and app graph.
 * @param sectionSpecs explicit section declarations in app display order.
 * @param sceneSupport optional scene support supplied by Nav3 adapter modules.
 * @param entryMetadata app-owned Nav3 metadata for each resolved Laydr entry.
 * @param initialSection optional initial selected section.
 * @param savedStateConfiguration saved-state configuration for Laydr keys.
 * @param notFoundContent content rendered for invalid Nav3 keys.
 */
@Composable
public fun <Data : Any> rememberLaydrNavSections(
    routeDefinitions: LaydrComposeRouteDefinitions,
    sectionSpecs: List<LaydrNavSectionSpec<Data>>,
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: (LaydrNavSectionEntryContext<Data>) -> Map<String, Any> = { emptyMap() },
    initialSection: LaydrNavSection<Data>? = null,
    savedStateConfiguration: SavedStateConfiguration =
        laydrNavSavedStateConfiguration(),
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
        savedStateConfiguration = savedStateConfiguration,
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
