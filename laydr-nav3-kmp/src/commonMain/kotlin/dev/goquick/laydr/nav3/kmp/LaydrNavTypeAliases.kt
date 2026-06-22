// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

/**
 * App-owned launch data for one Laydr Nav3 entry.
 */
public typealias LaydrNavLaunch = dev.goquick.laydr.nav.runtime.LaydrNavLaunch

/**
 * App-owned metadata attached to one resolved Laydr Nav3 entry.
 */
public typealias LaydrNavEntryMetadata = dev.goquick.laydr.nav.runtime.LaydrNavEntryMetadata

/**
 * Typed app-owned key for one value stored in Laydr Nav3 entry metadata.
 */
public typealias LaydrNavEntryMetadataKey<Value> = dev.goquick.laydr.nav.runtime.LaydrNavEntryMetadataKey<Value>

/**
 * Placement of one resolved Laydr screen route inside the generated route tree.
 */
public typealias LaydrNavRoutePlacement = dev.goquick.laydr.nav.runtime.LaydrNavRoutePlacement

/**
 * Reason a Nav3 key could not be rendered by Laydr.
 */
public typealias LaydrNavNotFoundReason = dev.goquick.laydr.nav.runtime.LaydrNavNotFoundReason

/**
 * Creates a typed app-owned metadata key for values of type [Value].
 */
public inline fun <reified Value : Any> laydrNavEntryMetadataKey(
    name: String,
): LaydrNavEntryMetadataKey<Value> =
    dev.goquick.laydr.nav.runtime.laydrNavEntryMetadataKey(name)

/**
 * Reads the value for [key] from a raw Nav3 metadata map when it has the
 * expected type [Value].
 */
public inline operator fun <reified Value : Any> Map<String, Any>.get(
    key: LaydrNavEntryMetadataKey<Value>,
): Value? =
    this[key.name] as? Value
