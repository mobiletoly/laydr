// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

import dev.goquick.laydr.core.LaydrRouteKey

/**
 * Adapter-neutral identity for one Laydr navigation entry.
 *
 * [routeId] and [parameters] are stable route identity. [entryInstanceId] and
 * [entryMetadata] are transient entry-scoped runtime data used by adapters to
 * distinguish repeated launches, payloads, result sinks, and app-owned
 * metadata without changing route matching.
 */
public class LaydrNavEntryKey public constructor(
    /**
     * Stable generated Laydr route id.
     */
    public val routeId: String,
    parameters: Map<String, String> = emptyMap(),
    /**
     * Opaque saved identity for one distinct navigation entry instance.
     */
    public val entryInstanceId: String? = null,
    /**
     * App-owned metadata attached to this runtime entry.
     */
    public val entryMetadata: LaydrNavEntryMetadata = LaydrNavEntryMetadata.Empty,
) {
    /**
     * Decoded dynamic route parameters.
     */
    public val parameters: Map<String, String> = parameters.toMap()

    init {
        LaydrRouteKey(routeId = routeId, parameters = this.parameters)
    }

    /**
     * Converts this entry identity to the framework-neutral route key.
     */
    public fun toRouteKey(): LaydrRouteKey =
        LaydrRouteKey(routeId = routeId, parameters = parameters)

    /**
     * Returns a copy with [entryInstanceId] while preserving route identity
     * and metadata.
     */
    public fun withEntryInstanceId(entryInstanceId: String): LaydrNavEntryKey =
        LaydrNavEntryKey(
            routeId = routeId,
            parameters = parameters,
            entryInstanceId = entryInstanceId,
            entryMetadata = entryMetadata,
        )

    /**
     * Returns a copy with [entryMetadata] while preserving route identity and
     * instance identity.
     */
    public fun withEntryMetadata(entryMetadata: LaydrNavEntryMetadata): LaydrNavEntryKey =
        LaydrNavEntryKey(
            routeId = routeId,
            parameters = parameters,
            entryInstanceId = entryInstanceId,
            entryMetadata = entryMetadata,
        )

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LaydrNavEntryKey &&
            routeId == other.routeId &&
            parameters == other.parameters &&
            entryInstanceId == other.entryInstanceId &&
            entryMetadata.values == other.entryMetadata.values

    override fun hashCode(): Int {
        var result = routeId.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + (entryInstanceId?.hashCode() ?: 0)
        result = 31 * result + entryMetadata.values.hashCode()
        return result
    }

    override fun toString(): String =
        if (entryInstanceId == null && entryMetadata.values.isEmpty()) {
            "LaydrNavEntryKey(routeId=$routeId, parameters=$parameters)"
        } else {
            "LaydrNavEntryKey(routeId=$routeId, parameters=$parameters, entryInstanceId=$entryInstanceId, entryMetadata=${entryMetadata.values})"
        }
}

/**
 * Converts a framework-neutral route key to a navigation entry key.
 */
public fun LaydrRouteKey.navEntryKey(): LaydrNavEntryKey =
    LaydrNavEntryKey(routeId = routeId, parameters = parameters)
