// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrScreenRouteRef

/**
 * Declarative Material adaptive Nav3 list/detail scene relationship for
 * two generated Laydr screen routes.
 *
 * The list route owns the primary pane. The detail route owns the detail pane
 * and must be a screen route descendant of the list route in the generated app
 * graph. When the list route has dynamic parameters, the concrete list key is
 * derived from the detail key's shared parameter values.
 */
public class LaydrNavListDetailSceneSpec internal constructor(
    /**
     * Generated screen route used as the list pane.
     */
    public val list: LaydrScreenRouteRef,
    /**
     * Generated screen route used as the detail pane.
     */
    public val detail: LaydrScreenRouteRef,
    internal val detailPlaceholder: @Composable () -> Unit,
)

/**
 * Creates a Material adaptive list/detail scene declaration from generated
 * Laydr route refs.
 *
 * [detailPlaceholder] is rendered by Material adaptive Nav3 in the
 * detail pane on wide layouts when the list route is visible but no detail
 * entry has been selected. It is scene chrome, not a synthetic Laydr route.
 */
public fun laydrNavListDetailScene(
    list: LaydrScreenRouteRef,
    detail: LaydrScreenRouteRef,
    detailPlaceholder: @Composable () -> Unit,
): LaydrNavListDetailSceneSpec =
    LaydrNavListDetailSceneSpec(
        list = list,
        detail = detail,
        detailPlaceholder = detailPlaceholder,
    )

/**
 * Remembers Material adaptive Nav3 scene support for generated Laydr
 * routes.
 *
 * Pass [LaydrNavSceneSupport.sceneStrategies] to Nav3
 * `NavDisplay` and pass the returned object to Laydr entry-provider and
 * app-state APIs. Laydr validates the declared route relationships, attaches
 * Material adaptive list/detail metadata to matching entries, and normalizes
 * direct detail navigation to include the list route.
 *
 * Compact devices fall back to Nav3's single-pane scene, with
 * transition metadata attached to registered list/detail entries so moving
 * between them swaps directly instead of crossfading the outgoing list.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
public fun rememberLaydrNavAdaptiveScenes(
    appGraph: LaydrAppGraph,
    vararg specs: LaydrNavListDetailSceneSpec,
): LaydrNavSceneSupport {
    val listDetailSceneStrategy = rememberListDetailSceneStrategy<NavKey>()
    val specList = specs.toList()
    val stateKey = specList.map { spec ->
        LaydrNavSceneStateKey(
            listRouteId = spec.list.route.id,
            detailRouteId = spec.detail.route.id,
        )
    }
    val detailPlaceholders = rememberUpdatedState(specList.map { spec -> spec.detailPlaceholder })
    return remember(appGraph, stateKey, listDetailSceneStrategy) {
        val rememberedSpecs = specList.mapIndexed { index, spec ->
            LaydrNavListDetailSceneSpec(
                list = spec.list,
                detail = spec.detail,
                detailPlaceholder = {
                    detailPlaceholders.value[index]()
                },
            )
        }
        laydrNavAdaptiveScenes(
            appGraph = appGraph,
            specs = rememberedSpecs,
            sceneStrategies = listOf(listDetailSceneStrategy),
        )
    }
}

/**
 * Builds Material adaptive Nav3 scene metadata without remembering a
 * Material scene strategy.
 *
 * Prefer [rememberLaydrNavAdaptiveScenes] when passing scene strategies
 * to `NavDisplay`. This builder is useful for tests and for code that supplies
 * its own Nav3 scene strategies.
 */
public fun laydrNavAdaptiveScenes(
    appGraph: LaydrAppGraph,
    vararg specs: LaydrNavListDetailSceneSpec,
): LaydrNavSceneSupport =
    laydrNavAdaptiveScenes(
        appGraph = appGraph,
        specs = specs.asList(),
        sceneStrategies = emptyList(),
    )

private fun laydrNavAdaptiveScenes(
    appGraph: LaydrAppGraph,
    specs: List<LaydrNavListDetailSceneSpec>,
    sceneStrategies: List<SceneStrategy<NavKey>>,
): LaydrNavSceneSupport {
    if (specs.isEmpty()) {
        return laydrNavSceneSupport(sceneStrategies = sceneStrategies)
    }

    val listRouteIds = mutableSetOf<String>()
    val detailRouteIds = mutableSetOf<String>()
    val scenes = specs.map { spec ->
        require(listRouteIds.add(spec.list.route.id)) {
            "Duplicate Laydr Nav list-detail list route: ${spec.list.route.id}"
        }
        require(detailRouteIds.add(spec.detail.route.id)) {
            "Duplicate Laydr Nav list-detail detail route: ${spec.detail.route.id}"
        }
        validateListDetailSceneSpec(appGraph = appGraph, spec = spec)
    }
    return laydrNavSceneSupport(
        sceneStrategies = sceneStrategies,
        listDetailScenes = scenes.map { scene ->
            LaydrNavListDetailSceneSupport(
                listRoute = scene.listRoute,
                detailRoute = scene.detailRoute,
                listMetadata = scene.listMetadata,
                detailMetadata = scene.detailMetadata,
            )
        },
    )
}

private fun validateListDetailSceneSpec(
    appGraph: LaydrAppGraph,
    spec: LaydrNavListDetailSceneSpec,
): LaydrNavListDetailScene {
    val listRoute = appGraph.screenRoutes.firstOrNull { route -> route == spec.list.route }
        ?: throw IllegalArgumentException(
            "Laydr Nav list-detail list route must be a screen route in this app graph: " +
                spec.list.route.id,
        )
    val detailRoute = appGraph.screenRoutes.firstOrNull { route -> route == spec.detail.route }
        ?: throw IllegalArgumentException(
            "Laydr Nav list-detail detail route must be a screen route in this app graph: " +
                spec.detail.route.id,
        )
    require(detailRoute != listRoute) {
        "Laydr Nav list-detail detail route must differ from list route: ${detailRoute.id}"
    }
    require(appGraph.routeMap.contains(spec.list, detailRoute.keyWithPlaceholderParameters())) {
        "Laydr Nav list-detail detail route must be inside list route subtree: " +
            "${detailRoute.id} outside ${listRoute.id}"
    }
    return LaydrNavListDetailScene(
        listRoute = listRoute,
        detailRoute = detailRoute,
        detailPlaceholder = spec.detailPlaceholder,
    )
}

private class LaydrNavListDetailScene(
    val listRoute: LaydrRoute,
    val detailRoute: LaydrRoute,
    detailPlaceholder: @Composable () -> Unit,
) {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    val listMetadata: Map<String, Any> =
        ListDetailSceneStrategy.listPane(sceneKey = listRoute.id) {
            detailPlaceholder()
        } + instantSceneTransitionMetadata()

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    val detailMetadata: Map<String, Any> =
        ListDetailSceneStrategy.detailPane(sceneKey = listRoute.id) + instantSceneTransitionMetadata()
}

private fun LaydrRoute.keyWithPlaceholderParameters(): LaydrRouteKey =
    LaydrRouteKey(
        routeId = id,
        parameters = parameterNames.associateWith { parameterName -> parameterName },
    )

private fun instantSceneTransitionMetadata(): Map<String, Any> =
    NavDisplay.transitionSpec { instantSceneTransition() } +
        NavDisplay.popTransitionSpec { instantSceneTransition() } +
        NavDisplay.predictivePopTransitionSpec { _ -> instantSceneTransition() }

private fun instantSceneTransition(): ContentTransform =
    EnterTransition.None togetherWith ExitTransition.None
