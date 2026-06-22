// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategy
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.nav.runtime.LaydrNavListDetailSceneSupport as RuntimeLaydrNavListDetailSceneSupport
import dev.goquick.laydr.nav.runtime.LaydrNavSceneSupport as RuntimeLaydrNavSceneSupport

/**
 * Recognized list-detail shape for a Nav3 back stack.
 */
public typealias LaydrNavListDetailStackShape =
    dev.goquick.laydr.nav.runtime.LaydrNavListDetailStackShape

/**
 * Route facts for a stack recognized by Laydr list-detail scene support.
 *
 * This state is derived from Nav3 keys only. It does not choose
 * breakpoint, pane visibility, app bar, or back handling policy.
 */
public data class LaydrNavListDetailStackState(
    /**
     * Registered list route key at the start of the stack.
     */
    public val listKey: LaydrNavKey,
    /**
     * Registered detail route key when the stack includes a selected detail.
     */
    public val detailKey: LaydrNavKey?,
    /**
     * Recognized list-detail stack shape.
     */
    public val shape: LaydrNavListDetailStackShape,
)

/**
 * Stable scene route identity pair.
 */
public typealias LaydrNavSceneStateKey =
    dev.goquick.laydr.nav.runtime.LaydrNavSceneStateKey

/**
 * Validated list/detail scene metadata supplied by an optional Nav3
 * scene adapter.
 *
 * This advanced API lets adapter modules provide metadata and stack
 * normalization without making the base Nav3 artifact depend on a
 * concrete scene implementation.
 */
public class LaydrNavListDetailSceneSupport(
    /**
     * Generated list route rendered as the primary entry. When this route has
     * dynamic parameters, matching detail keys must include those shared
     * parameter values.
     */
    public val listRoute: LaydrRoute,
    /**
     * Generated detail route rendered beside or after the list route.
     */
    public val detailRoute: LaydrRoute,
    /**
     * Metadata attached to entries for [listRoute].
     */
    public val listMetadata: Map<String, Any>,
    /**
     * Metadata attached to entries for [detailRoute].
     */
    public val detailMetadata: Map<String, Any>,
) {
    internal val runtimeSupport: RuntimeLaydrNavListDetailSceneSupport =
        RuntimeLaydrNavListDetailSceneSupport(
            listRoute = listRoute,
            detailRoute = detailRoute,
            listMetadata = listMetadata,
            detailMetadata = detailMetadata,
        )

    internal fun listKeyFor(detailKey: LaydrNavKey): LaydrNavKey? {
        return runtimeSupport.listKeyFor(detailKey.entryKey)?.let(::LaydrNavKey)
    }

    internal fun validListKey(key: LaydrNavKey): Boolean =
        runtimeSupport.validListKey(key.entryKey)
}

/**
 * Builds neutral Nav3 scene support for generated Laydr routes.
 *
 * Most apps should use [LaydrNavSceneSupport.None]. Optional scene
 * adapter modules use this function to supply validated scene strategies and
 * route metadata while keeping the base Nav3 artifact dependency-light.
 */
public fun laydrNavSceneSupport(
    sceneStrategies: List<SceneStrategy<NavKey>> = emptyList(),
    listDetailScenes: List<LaydrNavListDetailSceneSupport> = emptyList(),
): LaydrNavSceneSupport {
    if (listDetailScenes.isEmpty()) {
        return LaydrNavSceneSupport(
            sceneStrategies = sceneStrategies,
            scenesByListRouteId = emptyMap(),
            scenesByDetailRouteId = emptyMap(),
            runtimeSupport = RuntimeLaydrNavSceneSupport.None,
        )
    }

    val listRouteIds = mutableSetOf<String>()
    val detailRouteIds = mutableSetOf<String>()
    listDetailScenes.forEach { scene ->
        require(listRouteIds.add(scene.listRoute.id)) {
            "Duplicate Laydr Nav list-detail list route: ${scene.listRoute.id}"
        }
        require(detailRouteIds.add(scene.detailRoute.id)) {
            "Duplicate Laydr Nav list-detail detail route: ${scene.detailRoute.id}"
        }
    }
    return LaydrNavSceneSupport(
        sceneStrategies = sceneStrategies,
        scenesByListRouteId = listDetailScenes.associateBy { scene -> scene.listRoute.id },
        scenesByDetailRouteId = listDetailScenes.associateBy { scene -> scene.detailRoute.id },
        runtimeSupport = dev.goquick.laydr.nav.runtime.laydrNavSceneSupport(
            listDetailScenes = listDetailScenes.map { scene -> scene.runtimeSupport },
        ),
    )
}

/**
 * Neutral Nav3 scene support for a Laydr app graph.
 *
 * [sceneStrategies] is the list an app can pass to Nav3 `NavDisplay`.
 * The same object should be passed to Laydr entry-provider and controller APIs
 * so entry metadata and direct-detail stack normalization use the same
 * declarations.
 */
public class LaydrNavSceneSupport internal constructor(
    /**
     * Nav3 scene strategies for Laydr entries.
     */
    public val sceneStrategies: List<SceneStrategy<NavKey>>,
    private val scenesByListRouteId: Map<String, LaydrNavListDetailSceneSupport>,
    private val scenesByDetailRouteId: Map<String, LaydrNavListDetailSceneSupport>,
    internal val runtimeSupport: RuntimeLaydrNavSceneSupport,
) {
    internal fun metadataFor(route: LaydrRoute): Map<String, Any> =
        runtimeSupport.metadataFor(route)

    internal fun expandedStackFor(key: LaydrNavKey): List<LaydrNavKey>? {
        return runtimeSupport.expandedStackFor(key.entryKey)
            ?.map(::LaydrNavKey)
    }

    internal fun detailSelectionIndex(
        backStack: List<NavKey>,
        key: LaydrNavKey,
    ): Int? {
        return runtimeSupport.detailSelectionIndex(
            backStack = backStack.mapNotNull { stackKey -> (stackKey as? LaydrNavKey)?.entryKey },
            key = key.entryKey,
        )
    }

    internal fun registeredDetailRouteId(routeId: String): Boolean =
        runtimeSupport.registeredDetailRouteId(routeId)

    /**
     * Returns list-detail route facts for [backStack], or `null` when the
     * stack is not recognized by this scene support.
     */
    public fun listDetailStackState(backStack: List<NavKey>): LaydrNavListDetailStackState? {
        val state = runtimeSupport.listDetailStackState(
            backStack.map { key -> (key as? LaydrNavKey)?.entryKey ?: return null },
        ) ?: return null
        return LaydrNavListDetailStackState(
            listKey = LaydrNavKey(state.listKey),
            detailKey = state.detailKey?.let(::LaydrNavKey),
            shape = state.shape,
        )
    }

    /**
     * Stable route identities used to remember scene support declarations.
     */
    public val stateKey: List<LaydrNavSceneStateKey> =
        runtimeSupport.stateKey

    public companion object {
        /**
         * No-op scene support.
         */
        public val None: LaydrNavSceneSupport =
            LaydrNavSceneSupport(
                sceneStrategies = emptyList(),
                scenesByListRouteId = emptyMap(),
                scenesByDetailRouteId = emptyMap(),
                runtimeSupport = RuntimeLaydrNavSceneSupport.None,
            )
    }
}
