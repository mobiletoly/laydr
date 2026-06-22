// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav.runtime

import dev.goquick.laydr.core.LaydrRouteMap
import dev.goquick.laydr.core.LaydrRouteMatch

/**
 * Result of resolving an external path into a Laydr navigation operation.
 */
public sealed interface LaydrNavPathResult {
    /**
     * Input path that was evaluated.
     */
    public val path: String

    /**
     * True when [path] resolved to a renderable Laydr entry key.
     */
    public val accepted: Boolean
}

/**
 * Accepted path result with the renderable Laydr entry [key].
 */
public data class LaydrNavPathAccepted(
    override val path: String,
    public val key: LaydrNavEntryKey,
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
 * Result of resolving a URL-like external target into a Laydr navigation
 * operation.
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
     * True when [input] resolved to a renderable Laydr entry key.
     */
    public val accepted: Boolean
}

/**
 * Accepted external target result with the renderable Laydr entry [key].
 */
public data class LaydrNavExternalTargetAccepted(
    override val input: String,
    override val path: String,
    override val query: String?,
    override val fragment: String?,
    public val key: LaydrNavEntryKey,
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
 * Reason an external path could not be turned into a Laydr navigation entry.
 */
public enum class LaydrNavPathRejectionReason {
    /**
     * The path is not in the supported Laydr path shape.
     */
    UnsupportedPath,

    /**
     * No generated route matches the path.
     */
    UnknownRoute,

    /**
     * The matched route is layout-only and cannot render as a stack entry.
     */
    LayoutOnlyRoute,

    /**
     * The matched route key cannot produce a valid route path.
     */
    InvalidParameters,

    /**
     * The matched route is valid but outside the declared app section set.
     */
    OutsideDeclaredSection,
}

/**
 * Reason an external target could not be turned into a Laydr navigation entry.
 */
public enum class LaydrNavExternalTargetRejectionReason {
    /**
     * The input does not contain a supported app path target.
     */
    UnsupportedTarget,

    /**
     * The target path is not in the supported Laydr path shape.
     */
    UnsupportedPath,

    /**
     * No generated route matches the target path.
     */
    UnknownRoute,

    /**
     * The matched route is layout-only and cannot render as a stack entry.
     */
    LayoutOnlyRoute,

    /**
     * The matched route key cannot produce a valid route path.
     */
    InvalidParameters,

    /**
     * The matched route is valid but outside the declared app section set.
     */
    OutsideDeclaredSection,
}

/**
 * Resolves [path] to a structured Laydr navigation path result.
 */
public fun laydrNavPathResult(
    path: String,
    routeMap: LaydrRouteMap,
): LaydrNavPathResult {
    if (!path.hasSupportedLaydrNavPathShape()) {
        return LaydrNavPathRejected(
            path = path,
            reason = LaydrNavPathRejectionReason.UnsupportedPath,
        )
    }

    val routeKey = routeMap.keyForPath(path)
        ?: return LaydrNavPathRejected(
            path = path,
            reason = LaydrNavPathRejectionReason.UnknownRoute,
        )

    if (routeMap.screenRouteFor(routeKey) == null) {
        return LaydrNavPathRejected(
            path = path,
            reason = LaydrNavPathRejectionReason.LayoutOnlyRoute,
        )
    }

    routeMap.pathFor(routeKey)
        ?: return LaydrNavPathRejected(
            path = path,
            reason = LaydrNavPathRejectionReason.InvalidParameters,
        )

    return LaydrNavPathAccepted(
        path = path,
        key = routeKey.navEntryKey(),
    )
}

/**
 * Resolves a URL-like [input] to a structured Laydr external target result.
 */
public fun laydrNavExternalTargetResult(
    input: String,
    routeMap: LaydrRouteMap,
): LaydrNavExternalTargetResult {
    val parts = input.externalTargetParts()
    val path = parts.path
        ?: return LaydrNavExternalTargetRejected(
            input = input,
            path = null,
            query = parts.query,
            fragment = parts.fragment,
            reason = LaydrNavExternalTargetRejectionReason.UnsupportedTarget,
        )

    return when (val result = laydrNavPathResult(path = path, routeMap = routeMap)) {
        is LaydrNavPathAccepted -> LaydrNavExternalTargetAccepted(
            input = input,
            path = path,
            query = parts.query,
            fragment = parts.fragment,
            key = result.key,
        )
        is LaydrNavPathRejected -> LaydrNavExternalTargetRejected(
            input = input,
            path = path,
            query = parts.query,
            fragment = parts.fragment,
            reason = result.reason.externalTargetReason(),
        )
    }
}

/**
 * Resolves [path] to an adapter-neutral Laydr entry key.
 */
public fun laydrNavEntryKeyForPath(
    path: String,
    routeMap: LaydrRouteMap,
): LaydrNavEntryKey? =
    (laydrNavPathResult(path = path, routeMap = routeMap) as? LaydrNavPathAccepted)?.key

/**
 * Builds the concrete Laydr path represented by this entry key.
 */
public fun LaydrNavEntryKey.path(routeMap: LaydrRouteMap): String? =
    routeMap.pathFor(toRouteKey())

/**
 * Returns the renderable route match for this entry key.
 */
public fun LaydrRouteMap.laydrMatchForKey(key: LaydrNavEntryKey): LaydrRouteMatch? {
    val routeKey = key.toRouteKey()
    val route = screenRouteFor(routeKey) ?: return null
    val path = pathFor(routeKey) ?: return null
    return route.match(path)
}

private fun String.hasSupportedLaydrNavPathShape(): Boolean {
    if (!startsWith("/") || contains("?") || contains("#")) {
        return false
    }
    if (this != "/" && (endsWith("/") || contains("//"))) {
        return false
    }
    if (containsInvalidPercentEncoding() || any { char -> char.code > 0x7F }) {
        return false
    }
    return true
}

private data class LaydrNavExternalTargetParts(
    val path: String?,
    val query: String?,
    val fragment: String?,
)

private fun String.externalTargetParts(): LaydrNavExternalTargetParts {
    val fragmentDelimiter = indexOf('#')
    val withoutFragment = if (fragmentDelimiter >= 0) substring(0, fragmentDelimiter) else this
    val fragment = if (fragmentDelimiter >= 0) substring(fragmentDelimiter + 1) else null
    val queryDelimiter = withoutFragment.indexOf('?')
    val target = if (queryDelimiter >= 0) withoutFragment.substring(0, queryDelimiter) else withoutFragment
    val query = if (queryDelimiter >= 0) withoutFragment.substring(queryDelimiter + 1) else null

    return LaydrNavExternalTargetParts(
        path = target.externalTargetPath(),
        query = query,
        fragment = fragment,
    )
}

private fun String.externalTargetPath(): String? {
    if (isEmpty()) {
        return null
    }

    val authoritySeparator = indexOf("://")
    if (authoritySeparator >= 0) {
        val authorityStart = authoritySeparator + 3
        val pathStart = indexOf('/', startIndex = authorityStart)
        if (pathStart < 0) {
            return null
        }
        return substring(pathStart).takeIf { path -> path.isNotEmpty() }
    }

    val schemeSeparator = indexOf(':')
    if (schemeSeparator > 0 && substring(0, schemeSeparator).isUriScheme()) {
        return substring(schemeSeparator + 1).takeIf { path -> path.isNotEmpty() }
    }

    return this
}

private fun String.isUriScheme(): Boolean {
    if (isEmpty() || first() !in 'A'..'Z' && first() !in 'a'..'z') {
        return false
    }
    return drop(1).all { char ->
        char in 'A'..'Z' ||
            char in 'a'..'z' ||
            char in '0'..'9' ||
            char == '+' ||
            char == '-' ||
            char == '.'
    }
}

/**
 * Converts a path rejection reason to an external-target rejection reason.
 */
public fun LaydrNavPathRejectionReason.externalTargetReason(): LaydrNavExternalTargetRejectionReason =
    when (this) {
        LaydrNavPathRejectionReason.UnsupportedPath ->
            LaydrNavExternalTargetRejectionReason.UnsupportedPath
        LaydrNavPathRejectionReason.UnknownRoute ->
            LaydrNavExternalTargetRejectionReason.UnknownRoute
        LaydrNavPathRejectionReason.LayoutOnlyRoute ->
            LaydrNavExternalTargetRejectionReason.LayoutOnlyRoute
        LaydrNavPathRejectionReason.InvalidParameters ->
            LaydrNavExternalTargetRejectionReason.InvalidParameters
        LaydrNavPathRejectionReason.OutsideDeclaredSection ->
            LaydrNavExternalTargetRejectionReason.OutsideDeclaredSection
    }

private fun String.containsInvalidPercentEncoding(): Boolean {
    var index = 0
    while (index < length) {
        if (this[index] != '%') {
            index += 1
            continue
        }
        if (index + 2 >= length || !this[index + 1].isHexDigit() || !this[index + 2].isHexDigit()) {
            return true
        }
        index += 3
    }
    return false
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
