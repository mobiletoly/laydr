// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrScreenDestination

/**
 * Converts adapter stack keys to and from adapter-neutral Laydr entry keys.
 */
public interface LaydrNavKeyAdapter<Key : Any> {
    /**
     * Returns the Laydr entry key represented by [key], or `null` for foreign
     * adapter keys.
     */
    public fun entryKey(key: Key): LaydrNavEntryKey?

    /**
     * Creates an adapter key for [entryKey].
     */
    public fun key(entryKey: LaydrNavEntryKey): Key
}

/**
 * Concrete adapter-neutral stack mutation engine for Laydr navigation.
 */
public class LaydrNavStackEngine<Key : Any> public constructor(
    /**
     * Generated app graph used for destination validation and path lookup.
     */
    public val appGraph: LaydrAppGraph,
    /**
     * Mutable adapter back stack owned by an adapter or app.
     */
    public val backStack: MutableList<Key>,
    private val keyAdapter: LaydrNavKeyAdapter<Key>,
    private val sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
) {
    /**
     * Current adapter key.
     */
    public val currentKey: Key?
        get() = backStack.lastOrNull()

    /**
     * Current Laydr entry key, or `null` when the current adapter key is
     * foreign.
     */
    public val currentEntryKey: LaydrNavEntryKey?
        get() = currentKey?.let(keyAdapter::entryKey)

    /**
     * Current generated Laydr path, or `null`.
     */
    public val currentPath: String?
        get() {
            val key = currentEntryKey ?: return null
            val routeKey = key.toRouteKey()
            appGraph.routeMap.screenRouteFor(routeKey) ?: return null
            return key.path(appGraph.routeMap)
        }

    /**
     * True when Back can remove the current Laydr entry.
     */
    public val canPopLaydrEntry: Boolean
        get() {
            if (backStack.lastOrNull()?.let(keyAdapter::entryKey) == null) {
                return false
            }
            val suffixStart = laydrSuffixStartIndex()
            return suffixStart > 0 || backStack.size > 1
        }

    /**
     * Recognized list-detail route facts for the current Laydr suffix.
     */
    public val listDetailStackState: LaydrNavListDetailStackState?
        get() = sceneSupport.listDetailStackState(backStack.drop(laydrSuffixStartIndex()).toEntryKeys())

    /**
     * Returns all adapter keys converted to Laydr entry keys, or `null` if
     * any stack entry is foreign.
     */
    public fun allEntryKeys(): List<LaydrNavEntryKey>? =
        backStack.map { key -> keyAdapter.entryKey(key) ?: return null }

    /**
     * Replaces the entire adapter stack with [keys].
     */
    public fun replaceWithEntryKeys(keys: List<LaydrNavEntryKey>) {
        backStack.clear()
        keys.forEach { key -> backStack.add(keyAdapter.key(key)) }
    }

    /**
     * Pushes a validated destination.
     */
    public fun push(destination: LaydrScreenDestination) {
        pushKey(validatedKey(destination))
    }

    /**
     * Pushes the screen route represented by [path].
     */
    public fun pushPath(path: String): LaydrNavPathResult {
        val result = laydrNavPathResult(path = path, routeMap = appGraph.routeMap)
        val accepted = result as? LaydrNavPathAccepted ?: return result
        pushKey(accepted.key)
        return accepted
    }

    /**
     * Pushes the screen route represented by URL-like [input].
     */
    public fun pushExternalTarget(input: String): LaydrNavExternalTargetResult {
        val result = laydrNavExternalTargetResult(input = input, routeMap = appGraph.routeMap)
        val accepted = result as? LaydrNavExternalTargetAccepted ?: return result
        pushKey(accepted.key)
        return accepted
    }

    /**
     * Pushes [key] without duplicating the same current entry.
     */
    public fun pushKey(key: LaydrNavEntryKey) {
        val suffixStart = laydrSuffixStartIndex()
        val suffix = backStack.drop(suffixStart).toEntryKeys()
        val detailSelectionIndex = sceneSupport.detailSelectionIndex(
            backStack = suffix,
            key = key,
        )?.let { index -> suffixStart + index }
        if (detailSelectionIndex != null) {
            backStack[detailSelectionIndex] = keyAdapter.key(key)
            while (backStack.lastIndex > detailSelectionIndex) {
                backStack.removeLastOrNull()
            }
            return
        }

        val expandedStack = sceneSupport.expandedStackFor(key)
        if (
            expandedStack != null &&
            !backStack.getOrNull(suffixStart).sameRouteIdentityAs(expandedStack.first())
        ) {
            replaceLaydrSuffix(expandedStack)
            return
        }
        if (backStack.lastOrNull() != keyAdapter.key(key)) {
            backStack.add(keyAdapter.key(key))
        }
    }

    /**
     * Pushes [key] as a distinct entry even when it matches the current top.
     */
    public fun pushNewEntryKey(key: LaydrNavEntryKey) {
        val suffixStart = laydrSuffixStartIndex()
        val expandedStack = sceneSupport.expandedStackFor(key)
        if (
            expandedStack != null &&
            !backStack.getOrNull(suffixStart).sameRouteIdentityAs(expandedStack.first())
        ) {
            replaceLaydrSuffix(expandedStack)
            return
        }
        backStack.add(keyAdapter.key(key))
    }

    /**
     * Fails when the current top is a foreign adapter key and therefore cannot
     * be replaced by Laydr.
     */
    public fun requireCanReplaceCurrentEntry() {
        if (backStack.isNotEmpty() && backStack.lastOrNull()?.let(keyAdapter::entryKey) == null) {
            error(
                "Cannot replace the top Nav3 entry because it is not a LaydrNavKey. " +
                    "Use push(...) or owner-facing reset(...) for mixed stacks.",
            )
        }
    }

    /**
     * Replaces the current Laydr entry with [destination].
     */
    public fun replace(destination: LaydrScreenDestination) {
        replaceKey(validatedKey(destination))
    }

    /**
     * Replaces the current Laydr entry with the screen route represented by
     * [path].
     */
    public fun replacePath(path: String): LaydrNavPathResult {
        val result = laydrNavPathResult(path = path, routeMap = appGraph.routeMap)
        val accepted = result as? LaydrNavPathAccepted ?: return result
        replaceKey(accepted.key)
        return accepted
    }

    /**
     * Replaces the current Laydr entry with the screen route represented by
     * URL-like [input].
     */
    public fun replaceExternalTarget(input: String): LaydrNavExternalTargetResult {
        val result = laydrNavExternalTargetResult(input = input, routeMap = appGraph.routeMap)
        val accepted = result as? LaydrNavExternalTargetAccepted ?: return result
        replaceKey(accepted.key)
        return accepted
    }

    /**
     * Replaces the current Laydr entry with [key].
     */
    public fun replaceKey(key: LaydrNavEntryKey) {
        requireCanReplaceCurrentEntry()
        val expandedStack = sceneSupport.expandedStackFor(key)
        if (expandedStack != null) {
            replaceLaydrSuffix(expandedStack)
            return
        }
        if (backStack.isEmpty()) {
            backStack.add(keyAdapter.key(key))
            return
        }
        backStack[backStack.lastIndex] = keyAdapter.key(key)
    }

    /**
     * Replaces the trailing Laydr suffix with [destination].
     */
    public fun reset(destination: LaydrScreenDestination) {
        resetKey(validatedKey(destination))
    }

    /**
     * Replaces the trailing Laydr suffix with [key].
     */
    public fun resetKey(key: LaydrNavEntryKey) {
        val resetStack = sceneSupport.expandedStackFor(key) ?: listOf(key)
        replaceLaydrSuffix(resetStack)
    }

    /**
     * Pops the current Laydr entry when allowed.
     */
    public fun popLaydrEntry(): Boolean {
        if (!canPopLaydrEntry) {
            return false
        }
        backStack.removeLastOrNull()
        return true
    }

    /**
     * Validates [destination] and returns its entry key.
     */
    public fun validatedKey(destination: LaydrScreenDestination): LaydrNavEntryKey {
        appGraph.requireScreenRoute(destination)
        return destination.routeKey.navEntryKey()
    }

    private fun laydrSuffixStartIndex(): Int {
        val lastForeignIndex = backStack.indexOfLast { key -> keyAdapter.entryKey(key) == null }
        return lastForeignIndex + 1
    }

    private fun replaceLaydrSuffix(keys: List<LaydrNavEntryKey>) {
        val suffixStart = laydrSuffixStartIndex()
        while (backStack.size > suffixStart) {
            backStack.removeLastOrNull()
        }
        backStack.addAll(keys.map(keyAdapter::key))
    }

    private fun List<Key>.toEntryKeys(): List<LaydrNavEntryKey> =
        mapNotNull(keyAdapter::entryKey)

    private fun Key?.sameRouteIdentityAs(key: LaydrNavEntryKey): Boolean =
        this?.let(keyAdapter::entryKey).sameRouteIdentityAs(key)
}
