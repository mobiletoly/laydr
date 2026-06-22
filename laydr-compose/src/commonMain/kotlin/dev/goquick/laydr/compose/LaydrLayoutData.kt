// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.compose

import androidx.compose.runtime.Composable
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteMatch

/**
 * Typed key for screen-owned values consumed by inherited layouts.
 *
 * Key identity is object identity. Two keys with the same [name] are distinct
 * and do not read each other's values.
 *
 * @param T value type stored for this key.
 * @param name readable key name for diagnostics and debugging.
 */
public class LaydrLayoutKey<T : Any> public constructor(
    /**
     * Readable key name for diagnostics and debugging.
     */
    public val name: String,
) {
    init {
        require(name.isNotBlank()) {
            "Layout key name must not be blank"
        }
    }

    override fun toString(): String =
        "LaydrLayoutKey(name=$name)"
}

/**
 * Immutable typed values provided by a matched screen to inherited layouts.
 */
public class LaydrLayoutValues private constructor(
    private val values: Map<LaydrLayoutKey<*>, Any>,
) {
    /**
     * Reads the value stored for [key], or `null` when the screen did not
     * provide a value for that exact key instance.
     */
    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> get(key: LaydrLayoutKey<T>): T? =
        values[key] as T?

    /**
     * Builder for immutable [LaydrLayoutValues].
     */
    public class Builder public constructor() {
        private val values: MutableMap<LaydrLayoutKey<*>, Any> = mutableMapOf()

        /**
         * Stores [value] under [key].
         */
        public fun <T : Any> put(
            key: LaydrLayoutKey<T>,
            value: T,
        ): Builder {
            values[key] = value
            return this
        }

        /**
         * Builds an immutable snapshot of the values stored so far.
         */
        public fun build(): LaydrLayoutValues =
            LaydrLayoutValues(values.toMap())
    }

    public companion object {
        /**
         * Empty layout values.
         */
        public val Empty: LaydrLayoutValues = LaydrLayoutValues(emptyMap())

        /**
         * Builds immutable layout values with [block].
         */
        public fun build(block: Builder.() -> Unit): LaydrLayoutValues =
            Builder().apply(block).build()
    }
}

/**
 * Context passed to an inherited layout while rendering a matched screen.
 *
 * @param route layout route descriptor being rendered.
 * @param match matched screen route and decoded path parameters.
 * @param layoutValues screen-owned values available to inherited layouts.
 */
public class LaydrLayoutContext public constructor(
    /**
     * Layout route descriptor being rendered.
     */
    public val route: LaydrRoute,
    /**
     * Matched screen route and decoded path parameters.
     */
    public val match: LaydrRouteMatch,
    /**
     * Screen-owned values available to inherited layouts.
     */
    public val layoutValues: LaydrLayoutValues,
) {
    /**
     * Reads a screen-owned layout value by [key].
     */
    public operator fun <T : Any> get(key: LaydrLayoutKey<T>): T? =
        layoutValues[key]
}

/**
 * Screen body and layout values returned by app-owned route-to-screen binding.
 *
 * @param layoutValues values made available to inherited layouts.
 * @param content screen body rendered inside inherited layouts.
 */
public class LaydrScreenContent public constructor(
    /**
     * Values made available to inherited layouts.
     */
    public val layoutValues: LaydrLayoutValues = LaydrLayoutValues.Empty,
    /**
     * Screen body rendered inside inherited layouts.
     */
    public val content: @Composable () -> Unit,
)
