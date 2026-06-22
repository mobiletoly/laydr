// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import androidx.navigation3.runtime.NavKey
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteRef
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.core.isInRouteTree
import dev.goquick.laydr.core.isRoute
import dev.goquick.laydr.nav.runtime.LaydrNavEntryKey
import dev.goquick.laydr.nav.runtime.navEntryKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Generic Nav3 key for a generated Laydr route.
 *
 * The key stores the stable Laydr route id and decoded route parameter values.
 * Apps usually navigate with generated `destination(...)` values and let
 * Laydr app-state or controller APIs convert them at the Nav3 boundary.
 * Use this key directly when app code intentionally owns raw Nav3
 * stacks. Apps own those stacks; this type only provides a serializable key
 * shape that can be resolved back to generated Laydr route descriptors.
 *
 * @param routeId stable Laydr route id.
 * @param parameters decoded dynamic route parameters.
 */
@Serializable(with = LaydrNavKeySerializer::class)
public class LaydrNavKey internal constructor(
    /**
     * Stable Laydr route id.
     */
    public val routeId: String,
    parameters: Map<String, String> = emptyMap(),
    internal val entryToken: String? = null,
    internal val entryMetadata: LaydrNavEntryMetadata = LaydrNavEntryMetadata.Empty,
) : NavKey {
    internal constructor(
        entryKey: LaydrNavEntryKey,
    ) : this(
        routeId = entryKey.routeId,
        parameters = entryKey.parameters,
        entryToken = entryKey.entryToken,
        entryMetadata = entryKey.entryMetadata,
    )

    public constructor(
        routeId: String,
        parameters: Map<String, String> = emptyMap(),
    ) : this(
        routeId = routeId,
        parameters = parameters,
        entryToken = null,
        entryMetadata = LaydrNavEntryMetadata.Empty,
    )

    /**
     * Decoded dynamic route parameters.
     */
    public val parameters: Map<String, String> = parameters.toMap()
    internal val entryKey: LaydrNavEntryKey = LaydrNavEntryKey(
        routeId = routeId,
        parameters = this.parameters,
        entryToken = entryToken,
        entryMetadata = entryMetadata,
    )

    init {
        entryKey.toRouteKey()
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LaydrNavKey &&
            routeId == other.routeId &&
            parameters == other.parameters &&
            entryToken == other.entryToken &&
            entryMetadata.values == other.entryMetadata.values

    override fun hashCode(): Int {
        var result = routeId.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + (entryToken?.hashCode() ?: 0)
        result = 31 * result + entryMetadata.values.hashCode()
        return result
    }

    override fun toString(): String =
        if (entryToken == null && entryMetadata.values.isEmpty()) {
            "LaydrNavKey(routeId=$routeId, parameters=$parameters)"
        } else {
            "LaydrNavKey(routeId=$routeId, parameters=$parameters, entryToken=$entryToken, entryMetadata=${entryMetadata.values})"
        }
}

/**
 * Serializer for [LaydrNavKey].
 *
 * It preserves the public serial shape while letting [LaydrNavKey] copy the
 * caller-provided parameter map during construction.
 */
public object LaydrNavKeySerializer : KSerializer<LaydrNavKey> {
    private val parametersSerializer = MapSerializer(String.serializer(), String.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        "dev.goquick.laydr.nav3.kmp.LaydrNavKey",
    ) {
        element<String>("routeId")
        element<Map<String, String>>("parameters", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: LaydrNavKey): Unit =
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, index = 0, value = value.routeId)
            encodeSerializableElement(
                descriptor = descriptor,
                index = 1,
                serializer = parametersSerializer,
                value = value.parameters,
            )
        }

    override fun deserialize(decoder: Decoder): LaydrNavKey {
        var routeId: String? = null
        var parameters: Map<String, String> = emptyMap()

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> routeId = decodeStringElement(descriptor, index = 0)
                    1 -> parameters = decodeSerializableElement(
                        descriptor = descriptor,
                        index = 1,
                        deserializer = parametersSerializer,
                    )
                    else -> throw SerializationException("Unexpected LaydrNavKey field index: $index")
                }
            }
        }

        return LaydrNavKey(
            routeId = routeId ?: throw SerializationException("Missing LaydrNavKey.routeId"),
            parameters = parameters,
        )
    }
}

/**
 * Converts this Nav3 key back to the framework-neutral Laydr key.
 */
public fun LaydrNavKey.toLaydrRouteKey(): LaydrRouteKey =
    entryKey.toRouteKey()

/**
 * Converts a framework-neutral Laydr route key to a Nav3 key.
 */
public fun LaydrRouteKey.navKey(): LaydrNavKey =
    LaydrNavKey(entryKey = navEntryKey())

/**
 * Converts a generated Laydr screen destination to a Nav3 key.
 *
 * Most app code should pass destinations directly to Laydr app-state or
 * controller APIs. Use this conversion when managing Nav3 keys or
 * stacks directly.
 */
public fun LaydrScreenDestination.navKey(): LaydrNavKey =
    routeKey.navKey()

internal fun LaydrNavKey.withEntryToken(entryToken: String): LaydrNavKey =
    LaydrNavKey(entryKey = entryKey.withEntryToken(entryToken))

internal fun LaydrNavKey.withEntryMetadata(entryMetadata: LaydrNavEntryMetadata): LaydrNavKey =
    LaydrNavKey(entryKey = entryKey.withEntryMetadata(entryMetadata))

/**
 * Returns true when this Nav3 key is a Laydr key for [route] with valid route
 * parameters.
 *
 * Foreign Nav3 keys return false.
 */
public fun NavKey.isLaydrRoute(route: LaydrRouteRef): Boolean =
    (this as? LaydrNavKey)
        ?.toLaydrRouteKey()
        ?.isRoute(route)
        ?: false

/**
 * Returns true when this Nav3 key is a valid Laydr key in [routeMap] and
 * belongs to [parentRoute]'s route subtree.
 *
 * Foreign Nav3 keys return false.
 */
public fun NavKey.isInLaydrRouteTree(
    routeMap: LaydrRouteMap,
    parentRoute: LaydrRouteRef,
): Boolean =
    (this as? LaydrNavKey)
        ?.toLaydrRouteKey()
        ?.isInRouteTree(routeMap = routeMap, parentRoute = parentRoute)
        ?: false

/**
 * Returns true when the top Nav3 stack entry is a Laydr key for [route].
 */
public fun List<NavKey>.topIsLaydrRoute(route: LaydrRouteRef): Boolean =
    lastOrNull()?.isLaydrRoute(route) == true

/**
 * Returns true when the top Nav3 stack entry is a valid Laydr key in
 * [routeMap] and belongs to [parentRoute]'s route subtree.
 */
public fun List<NavKey>.topIsInLaydrRouteTree(
    routeMap: LaydrRouteMap,
    parentRoute: LaydrRouteRef,
): Boolean =
    lastOrNull()?.isInLaydrRouteTree(routeMap = routeMap, parentRoute = parentRoute) == true

/**
 * Validates [destination], appends it to this app-owned Nav3 stack, and
 * returns the appended Laydr key.
 */
public fun MutableList<NavKey>.pushLaydr(
    appGraph: LaydrAppGraph,
    destination: LaydrScreenDestination,
): LaydrNavKey {
    val key = destination.validatedNavKey(appGraph)
    add(key)
    return key
}

/**
 * Validates [destination], replaces the top entry of this app-owned Nav3 stack,
 * and returns the replacement Laydr key.
 *
 * Throws [IllegalStateException] when the stack is empty.
 */
public fun MutableList<NavKey>.replaceTopWithLaydr(
    appGraph: LaydrAppGraph,
    destination: LaydrScreenDestination,
): LaydrNavKey {
    check(isNotEmpty()) {
        "Cannot replace the top Laydr Nav entry in an empty stack"
    }
    val key = destination.validatedNavKey(appGraph)
    this[lastIndex] = key
    return key
}

/**
 * Replaces the top entry with [destination] only when the current top entry is
 * a valid Laydr key for [route].
 *
 * Returns false without mutating the stack when the top entry is missing,
 * foreign, invalid, or belongs to another route.
 */
public fun MutableList<NavKey>.replaceTopLaydrIf(
    appGraph: LaydrAppGraph,
    route: LaydrRouteRef,
    destination: LaydrScreenDestination,
): Boolean {
    if (!topIsLaydrRoute(route)) {
        return false
    }
    replaceTopWithLaydr(appGraph = appGraph, destination = destination)
    return true
}

/**
 * Removes entries above the last stack entry that satisfies [matching].
 *
 * Returns the number of removed entries. If no entry matches, or the matching
 * entry is already the top entry, the stack is unchanged and zero is returned.
 */
public fun MutableList<NavKey>.removeEntriesAboveLast(matching: (NavKey) -> Boolean): Int {
    val markerIndex = indexOfLast(matching)
    if (markerIndex < 0 || markerIndex == lastIndex) {
        return 0
    }
    val removedCount = lastIndex - markerIndex
    repeat(removedCount) {
        removeAt(lastIndex)
    }
    return removedCount
}

/**
 * Removes entries above the last occurrence of [key].
 *
 * Returns the number of removed entries.
 */
public fun MutableList<NavKey>.removeEntriesAboveLast(key: NavKey): Int =
    removeEntriesAboveLast { stackKey -> stackKey == key }

private fun LaydrScreenDestination.validatedNavKey(appGraph: LaydrAppGraph): LaydrNavKey {
    appGraph.requireScreenRoute(this)
    return navKey()
}
