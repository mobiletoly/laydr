// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

internal val LocalLaydrNavStackNavigator: ProvidableCompositionLocal<LaydrNavStackNavigator?> =
    staticCompositionLocalOf {
        null
    }

/**
 * Provides an app-approved parent or root Laydr Nav stack capability to route
 * content below [content].
 *
 * Laydr never installs this capability implicitly from entry providers. Apps
 * and section shells choose where a parent stack navigator is safe for nested
 * route content to use.
 */
@Composable
public fun ProvideLaydrNavStackNavigator(
    navigator: LaydrNavStackNavigator,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalLaydrNavStackNavigator provides navigator,
        content = content,
    )
}

/**
 * Returns the nearest app-provided Laydr parent stack navigator, or `null`
 * when no parent stack capability is present.
 */
@Composable
public fun laydrNavStackNavigatorOrNull(): LaydrNavStackNavigator? =
    LocalLaydrNavStackNavigator.current

/**
 * Returns the nearest app-provided Laydr parent stack navigator.
 *
 * Throws [IllegalStateException] when no parent stack capability was provided
 * with [ProvideLaydrNavStackNavigator].
 */
@Composable
public fun requireLaydrNavStackNavigator(): LaydrNavStackNavigator =
    laydrNavStackNavigatorOrNull()
        ?: error(
            "No Laydr Nav parent stack capability is present. " +
                "Wrap the route subtree with ProvideLaydrNavStackNavigator " +
                "when route content is allowed to navigate a parent stack.",
        )
