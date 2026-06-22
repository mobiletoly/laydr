// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import dev.goquick.laydr.core.LaydrParameterlessScreenRouteRef
import dev.goquick.laydr.core.LaydrScreenDestination

/**
 * Route-facing capability for navigation inside declared Laydr Nav sections.
 *
 * This interface intentionally excludes reset and external-target helpers.
 * External targets are owner-facing entry-point operations on [LaydrNavSections].
 */
public interface LaydrNavSectionsNavigator {
    /**
     * Selects the section that owns [destination] without mutating stacks.
     */
    public fun select(destination: LaydrScreenDestination)

    /**
     * Selects the section that owns parameterless [route] without mutating stacks.
     */
    public fun select(route: LaydrParameterlessScreenRouteRef)

    /**
     * Pushes [destination] as a no-payload, no-metadata section entry.
     */
    public fun push(destination: LaydrScreenDestination)

    /**
     * Pushes [launch] as a section entry.
     */
    public fun push(launch: LaydrNavLaunch)

    /**
     * Pushes [destination] and records the current selected stack as the next
     * app Back return point.
     */
    public fun pushWithReturn(destination: LaydrScreenDestination)

    /**
     * Pushes [launch] and records the current selected stack as the next app
     * Back return point.
     */
    public fun pushWithReturn(launch: LaydrNavLaunch)

    /**
     * Replaces the current entry with [destination] in its owning section.
     */
    public fun replace(destination: LaydrScreenDestination)

    /**
     * Replaces the current entry with [launch] in its owning section.
     */
    public fun replace(launch: LaydrNavLaunch)

    /**
     * Replaces with [destination] and records the current selected stack as
     * the next app Back return point.
     */
    public fun replaceWithReturn(destination: LaydrScreenDestination)

    /**
     * Replaces with [launch] and records the current selected stack as the
     * next app Back return point.
     */
    public fun replaceWithReturn(launch: LaydrNavLaunch)

    /**
     * Pushes [launch], registers a one-shot typed result callback for the
     * created section entry, and records the current selected stack as the next
     * app Back return point when the launch changes section state.
     *
     * Completing or canceling the result does not navigate. The launched route
     * remains responsible for calling [back] or another navigator operation.
     */
    public fun <Result : Any> pushForResult(
        launch: LaydrNavLaunch,
        onCancel: () -> Unit,
        resultType: kotlin.reflect.KClass<Result>,
        onResult: (Result) -> Unit,
    )

    /**
     * Performs user-facing app Back.
     */
    public fun back(): Boolean
}

/**
 * Pushes [launch], registers a one-shot typed result callback for the created
 * section entry, and records the current selected stack as the next app Back
 * return point when the launch changes section state.
 *
 * Completing or canceling the result does not navigate. The launched route
 * remains responsible for calling [LaydrNavSectionsNavigator.back] or another
 * navigator operation.
 */
public inline fun <reified Result : Any> LaydrNavSectionsNavigator.pushForResult(
    launch: LaydrNavLaunch,
    noinline onCancel: () -> Unit = {},
    noinline onResult: (Result) -> Unit,
) {
    pushForResult(
        launch = launch,
        onCancel = onCancel,
        resultType = Result::class,
        onResult = onResult,
    )
}
