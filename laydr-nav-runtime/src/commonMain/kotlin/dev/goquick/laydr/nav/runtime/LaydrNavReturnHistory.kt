// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

/**
 * Adapter-neutral return-history entry for section navigation.
 */
public data class LaydrNavReturnEntry(
    /**
     * Source section state id.
     */
    public val sourceSectionStateId: String,
    /**
     * Source section stack before navigation.
     */
    public val sourceBackStack: List<LaydrNavEntryKey>,
    /**
     * Target section state id.
     */
    public val targetSectionStateId: String,
    /**
     * Target section stack before navigation.
     */
    public val targetBackStackBeforeNavigation: List<LaydrNavEntryKey>,
    /**
     * Target key created by the return-aware navigation.
     */
    public val targetKey: LaydrNavEntryKey,
)

/**
 * Serializes return-history entries into saveable strings.
 */
public fun saveLaydrNavReturnHistory(
    history: List<LaydrNavReturnEntry>,
): List<String> =
    buildList {
        add(history.size.toString())
        history.forEach { entry ->
            add(entry.sourceSectionStateId)
            addKeyList(entry.sourceBackStack)
            add(entry.targetSectionStateId)
            addKeyList(entry.targetBackStackBeforeNavigation)
            addKey(entry.targetKey)
        }
    }

/**
 * Restores return-history entries from [saved], or `null` for invalid data.
 */
public fun restoreLaydrNavReturnHistory(saved: List<String>): List<LaydrNavReturnEntry>? {
    val cursor = LaydrNavReturnHistoryCursor(saved)
    val count = cursor.nextNonNegativeInt() ?: return null
    val entries = mutableListOf<LaydrNavReturnEntry>()
    repeat(count) {
        val sourceSectionStateId = cursor.nextString() ?: return null
        val sourceBackStack = cursor.nextKeyList() ?: return null
        val targetSectionStateId = cursor.nextString() ?: return null
        val targetBackStackBeforeNavigation = cursor.nextKeyList() ?: return null
        val targetKey = cursor.nextKey() ?: return null
        entries += LaydrNavReturnEntry(
            sourceSectionStateId = sourceSectionStateId,
            sourceBackStack = sourceBackStack,
            targetSectionStateId = targetSectionStateId,
            targetBackStackBeforeNavigation = targetBackStackBeforeNavigation,
            targetKey = targetKey,
        )
    }
    if (!cursor.isConsumed) {
        return null
    }
    return entries
}

private fun MutableList<String>.addKeyList(keys: List<LaydrNavEntryKey>) {
    add(keys.size.toString())
    keys.forEach { key -> addKey(key) }
}

private fun MutableList<String>.addKey(key: LaydrNavEntryKey) {
    add(key.routeId)
    add(key.parameters.size.toString())
    key.parameters.entries
        .sortedBy { entry -> entry.key }
        .forEach { (name, value) ->
            add(name)
            add(value)
        }
}

private class LaydrNavReturnHistoryCursor(
    private val values: List<String>,
) {
    private var index = 0

    val isConsumed: Boolean
        get() = index == values.size

    fun nextString(): String? =
        values.getOrNull(index++)

    fun nextNonNegativeInt(): Int? =
        nextString()
            ?.toIntOrNull()
            ?.takeIf { value -> value >= 0 }

    fun nextKeyList(): List<LaydrNavEntryKey>? {
        val count = nextNonNegativeInt() ?: return null
        return buildList {
            repeat(count) {
                add(nextKey() ?: return null)
            }
        }
    }

    fun nextKey(): LaydrNavEntryKey? {
        val routeId = nextString() ?: return null
        val parameterCount = nextNonNegativeInt() ?: return null
        val parameters = buildMap {
            repeat(parameterCount) {
                val name = nextString() ?: return null
                val value = nextString() ?: return null
                put(name, value)
            }
        }
        return LaydrNavEntryKey(routeId = routeId, parameters = parameters)
    }
}
