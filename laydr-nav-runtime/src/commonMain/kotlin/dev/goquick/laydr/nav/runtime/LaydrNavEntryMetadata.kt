// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

/**
 * App-owned metadata attached to one resolved Laydr navigation entry.
 *
 * Laydr stores and transports these values through adapter entry metadata, but
 * it does not interpret presentation, transition, analytics, auth, or chrome
 * policy.
 */
public class LaydrNavEntryMetadata public constructor(
    /**
     * Raw app-owned metadata values keyed by adapter metadata name.
     */
    public val values: Map<String, Any>,
) {
    /**
     * Creates metadata from [values].
     */
    public constructor(vararg values: Pair<String, Any>) : this(mapOf(*values))

    public companion object {
        /**
         * Empty entry metadata.
         */
        public val Empty: LaydrNavEntryMetadata = LaydrNavEntryMetadata(emptyMap())
    }
}

/**
 * Typed app-owned key for one value stored in Laydr navigation entry metadata.
 */
public class LaydrNavEntryMetadataKey<Value : Any> public constructor(
    /**
     * App-owned string key used in the raw adapter metadata map.
     */
    public val name: String,
) {
    init {
        require(name.isNotBlank()) {
            "Laydr Nav entry metadata key name must not be blank."
        }
    }

    /**
     * Creates the raw metadata pair consumed by [LaydrNavEntryMetadata].
     */
    public infix fun to(value: Value): Pair<String, Any> =
        name to value
}

/**
 * Creates a typed app-owned metadata key for values of type [Value].
 */
public inline fun <reified Value : Any> laydrNavEntryMetadataKey(
    name: String,
): LaydrNavEntryMetadataKey<Value> =
    LaydrNavEntryMetadataKey(name = name)

/**
 * Reads the value for [key] when it has the expected type [Value].
 */
public inline operator fun <reified Value : Any> Map<String, Any>.get(
    key: LaydrNavEntryMetadataKey<Value>,
): Value? =
    this[key.name] as? Value
