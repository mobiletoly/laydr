// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.nav.runtime.path

/**
 * Result of resolving an external path into a Nav3 stack operation.
 */
public sealed interface LaydrNavPathResult {
    /**
     * Input path that was evaluated.
     */
    public val path: String

    /**
     * True when [path] resolved to a renderable Laydr Nav key.
     */
    public val accepted: Boolean
}

/**
 * Accepted path result with the renderable Nav3 [key].
 */
public data class LaydrNavPathAccepted(
    override val path: String,
    public val key: LaydrNavKey,
) : LaydrNavPathResult {
    override val accepted: Boolean = true
}

/**
 * Rejected path result with the reason Laydr could determine.
 */
public data class LaydrNavPathRejected(
    override val path: String,
    public val reason: LaydrNavPathRejectionReason,
) : LaydrNavPathResult {
    override val accepted: Boolean = false
}

/**
 * Result of resolving a URL-like external target into a Nav3 stack operation.
 */
public sealed interface LaydrNavExternalTargetResult {
    /**
     * Original target input.
     */
    public val input: String

    /**
     * Normalized app path used for route matching, when one could be read.
     */
    public val path: String?

    /**
     * Raw query text without `?`, or `null` when no query was present.
     */
    public val query: String?

    /**
     * Raw fragment text without `#`, or `null` when no fragment was present.
     */
    public val fragment: String?

    /**
     * True when [input] resolved to a renderable Laydr Nav key.
     */
    public val accepted: Boolean
}

/**
 * Accepted external target result with the renderable Nav3 [key].
 */
public data class LaydrNavExternalTargetAccepted(
    override val input: String,
    override val path: String,
    override val query: String?,
    override val fragment: String?,
    public val key: LaydrNavKey,
) : LaydrNavExternalTargetResult {
    override val accepted: Boolean = true
}

/**
 * Rejected external target result with the reason Laydr could determine.
 */
public data class LaydrNavExternalTargetRejected(
    override val input: String,
    override val path: String?,
    override val query: String?,
    override val fragment: String?,
    public val reason: LaydrNavExternalTargetRejectionReason,
) : LaydrNavExternalTargetResult {
    override val accepted: Boolean = false
}

/**
 * Reason an external path could not be turned into a Laydr Nav entry.
 */
public typealias LaydrNavPathRejectionReason = dev.goquick.laydr.nav.runtime.LaydrNavPathRejectionReason

/**
 * Reason an external target could not be turned into a Laydr Nav entry.
 */
public typealias LaydrNavExternalTargetRejectionReason =
    dev.goquick.laydr.nav.runtime.LaydrNavExternalTargetRejectionReason

/**
 * Resolves [path] to a structured Nav3 path result.
 */
public fun laydrNavPathResult(
    path: String,
    routeMap: LaydrRouteMap,
): LaydrNavPathResult =
    dev.goquick.laydr.nav.runtime.laydrNavPathResult(
        path = path,
        routeMap = routeMap,
    ).toKmp()

/**
 * Resolves a URL-like [input] to a structured Nav3 external target result.
 */
public fun laydrNavExternalTargetResult(
    input: String,
    routeMap: LaydrRouteMap,
): LaydrNavExternalTargetResult =
    dev.goquick.laydr.nav.runtime.laydrNavExternalTargetResult(
        input = input,
        routeMap = routeMap,
    ).toKmp()

/**
 * Resolves [path] to a generic Nav3 key by matching against [routeMap].
 */
public fun laydrNavKeyForPath(
    path: String,
    routeMap: LaydrRouteMap,
): LaydrNavKey? =
    (laydrNavPathResult(path = path, routeMap = routeMap) as? LaydrNavPathAccepted)?.key

/**
 * Builds the concrete Laydr path represented by this Nav3 key.
 */
public fun LaydrNavKey.path(routeMap: LaydrRouteMap): String? =
    entryKey.path(routeMap)

internal fun dev.goquick.laydr.nav.runtime.LaydrNavPathResult.toKmp(): LaydrNavPathResult =
    when (this) {
        is dev.goquick.laydr.nav.runtime.LaydrNavPathAccepted -> LaydrNavPathAccepted(
            path = path,
            key = LaydrNavKey(key),
        )
        is dev.goquick.laydr.nav.runtime.LaydrNavPathRejected -> LaydrNavPathRejected(
            path = path,
            reason = reason,
        )
    }

internal fun dev.goquick.laydr.nav.runtime.LaydrNavExternalTargetResult.toKmp(): LaydrNavExternalTargetResult =
    when (this) {
        is dev.goquick.laydr.nav.runtime.LaydrNavExternalTargetAccepted -> LaydrNavExternalTargetAccepted(
            input = input,
            path = path,
            query = query,
            fragment = fragment,
            key = LaydrNavKey(key),
        )
        is dev.goquick.laydr.nav.runtime.LaydrNavExternalTargetRejected -> LaydrNavExternalTargetRejected(
            input = input,
            path = path,
            query = query,
            fragment = fragment,
            reason = reason,
        )
    }

internal fun dev.goquick.laydr.nav.runtime.LaydrNavPathAccepted.toKmp(): LaydrNavPathAccepted =
    LaydrNavPathAccepted(path = path, key = LaydrNavKey(key))

internal fun dev.goquick.laydr.nav.runtime.LaydrNavExternalTargetAccepted.toKmp(): LaydrNavExternalTargetAccepted =
    LaydrNavExternalTargetAccepted(
        input = input,
        path = path,
        query = query,
        fragment = fragment,
        key = LaydrNavKey(key),
    )
