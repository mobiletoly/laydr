// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

import dev.goquick.laydr.core.LaydrRoute

/**
 * Recognized list-detail shape for a Laydr navigation stack.
 */
public enum class LaydrNavListDetailStackShape {
    /**
     * The stack contains only the registered list route.
     */
    ListOnly,

    /**
     * The stack contains the registered list route and a detail route.
     */
    ListAndDetail,

    /**
     * The stack contains list, detail, and additional nested entries.
     */
    Nested,
}

/**
 * Route facts for a stack recognized by Laydr list-detail support.
 */
public data class LaydrNavListDetailStackState(
    /**
     * Registered list route key at the start of the stack.
     */
    public val listKey: LaydrNavEntryKey,
    /**
     * Registered detail route key when the stack includes a selected detail.
     */
    public val detailKey: LaydrNavEntryKey?,
    /**
     * Recognized list-detail stack shape.
     */
    public val shape: LaydrNavListDetailStackShape,
)

/**
 * Stable scene route identity pair.
 */
public data class LaydrNavSceneStateKey(
    /**
     * Route id of the list route.
     */
    public val listRouteId: String,
    /**
     * Route id of the detail route.
     */
    public val detailRouteId: String,
)

/**
 * Validated list/detail scene metadata supplied by an adapter module.
 */
public class LaydrNavListDetailSceneSupport public constructor(
    /**
     * Generated list route rendered as the primary entry.
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
    /**
     * Returns the list key represented by [detailKey], or `null` when the
     * detail key does not contain the list route's required parameters.
     */
    public fun listKeyFor(detailKey: LaydrNavEntryKey): LaydrNavEntryKey? {
        val parameters = listRoute.parameterNames.associateWith { parameterName ->
            detailKey.parameters[parameterName] ?: return null
        }
        return runCatching { listRoute.key(parameters).navEntryKey() }.getOrNull()
    }

    /**
     * Returns true when [key] is a valid key for [listRoute].
     */
    public fun validListKey(key: LaydrNavEntryKey): Boolean =
        key.routeId == listRoute.id &&
            runCatching { listRoute.buildPath(key.parameters) }.isSuccess
}

/**
 * Builds adapter-neutral scene support for generated Laydr routes.
 */
public fun laydrNavSceneSupport(
    listDetailScenes: List<LaydrNavListDetailSceneSupport> = emptyList(),
): LaydrNavSceneSupport {
    if (listDetailScenes.isEmpty()) {
        return LaydrNavSceneSupport(
            scenesByListRouteId = emptyMap(),
            scenesByDetailRouteId = emptyMap(),
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
        scenesByListRouteId = listDetailScenes.associateBy { scene -> scene.listRoute.id },
        scenesByDetailRouteId = listDetailScenes.associateBy { scene -> scene.detailRoute.id },
    )
}

/**
 * Adapter-neutral scene support for a Laydr app graph.
 */
public class LaydrNavSceneSupport internal constructor(
    private val scenesByListRouteId: Map<String, LaydrNavListDetailSceneSupport>,
    private val scenesByDetailRouteId: Map<String, LaydrNavListDetailSceneSupport>,
) {
    /**
     * Returns metadata for [route].
     */
    public fun metadataFor(route: LaydrRoute): Map<String, Any> =
        scenesByListRouteId[route.id]?.listMetadata
            ?: scenesByDetailRouteId[route.id]?.detailMetadata
            ?: emptyMap()

    /**
     * Returns a normalized list/detail stack for [key], or `null`.
     */
    public fun expandedStackFor(key: LaydrNavEntryKey): List<LaydrNavEntryKey>? {
        val scene = scenesByDetailRouteId[key.routeId] ?: return null
        val listKey = scene.listKeyFor(key) ?: return null
        return listOf(listKey, key)
    }

    /**
     * Returns the existing detail entry index in [backStack], or `null`.
     */
    public fun detailSelectionIndex(
        backStack: List<LaydrNavEntryKey>,
        key: LaydrNavEntryKey,
    ): Int? {
        val scene = scenesByDetailRouteId[key.routeId] ?: return null
        val listKey = scene.listKeyFor(key) ?: return null
        if (!backStack.firstOrNull().sameRouteIdentityAs(listKey)) {
            return null
        }
        val index = backStack.indexOfFirst { stackKey ->
            stackKey.routeId == scene.detailRoute.id
        }
        return index.takeIf { it >= 0 }
    }

    /**
     * Returns true when [routeId] is a registered detail route id.
     */
    public fun registeredDetailRouteId(routeId: String): Boolean =
        scenesByDetailRouteId.containsKey(routeId)

    /**
     * Returns list-detail route facts for [backStack], or `null`.
     */
    public fun listDetailStackState(backStack: List<LaydrNavEntryKey>): LaydrNavListDetailStackState? {
        if (backStack.isEmpty()) {
            return null
        }
        val listKey = backStack.first()
        val scene = scenesByListRouteId[listKey.routeId] ?: return null
        if (!scene.validListKey(listKey)) {
            return null
        }
        if (backStack.size == 1) {
            return LaydrNavListDetailStackState(
                listKey = listKey,
                detailKey = null,
                shape = LaydrNavListDetailStackShape.ListOnly,
            )
        }

        val detailKey = backStack[1]
        if (detailKey.routeId != scene.detailRoute.id ||
            runCatching { scene.detailRoute.buildPath(detailKey.parameters) }.isFailure ||
            !scene.listKeyFor(detailKey).sameRouteIdentityAs(listKey)
        ) {
            return null
        }
        return LaydrNavListDetailStackState(
            listKey = listKey,
            detailKey = detailKey,
            shape = if (backStack.size == 2) {
                LaydrNavListDetailStackShape.ListAndDetail
            } else {
                LaydrNavListDetailStackShape.Nested
            },
        )
    }

    /**
     * Stable route identities used to remember scene support declarations.
     */
    public val stateKey: List<LaydrNavSceneStateKey> =
        scenesByListRouteId.values.map { scene ->
            LaydrNavSceneStateKey(
                listRouteId = scene.listRoute.id,
                detailRouteId = scene.detailRoute.id,
            )
        }

    public companion object {
        /**
         * No-op scene support.
         */
        public val None: LaydrNavSceneSupport =
            LaydrNavSceneSupport(
                scenesByListRouteId = emptyMap(),
                scenesByDetailRouteId = emptyMap(),
            )
    }
}

internal fun LaydrNavEntryKey?.sameRouteIdentityAs(key: LaydrNavEntryKey): Boolean =
    this?.toRouteKey() == key.toRouteKey()
