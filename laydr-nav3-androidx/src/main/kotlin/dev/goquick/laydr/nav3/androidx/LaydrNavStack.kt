// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.androidx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.nav.runtime.LaydrNavEntryKey
import dev.goquick.laydr.nav.runtime.LaydrNavKeyAdapter
import dev.goquick.laydr.nav.runtime.LaydrNavStackEngine
import kotlin.reflect.KClass

/**
 * Provides app-owned metadata for each resolved Laydr AndroidX Nav3 entry in a
 * stack.
 */
public fun interface LaydrNavEntryMetadataProvider {
    /**
     * Returns metadata for the entry described by [context].
     */
    public fun metadata(context: LaydrNavEntryContext): LaydrNavEntryMetadata
}

/**
 * Route-facing capability for a Laydr-managed AndroidX Nav3 stack.
 */
public interface LaydrNavStackNavigator {
    /**
     * Pushes [destination] as a no-payload, no-metadata Laydr entry.
     */
    public fun push(destination: LaydrScreenDestination)

    /**
     * Pushes [launch] as a Laydr entry.
     */
    public fun push(launch: LaydrNavLaunch)

    /**
     * Replaces the top Laydr entry with [destination].
     */
    public fun replace(destination: LaydrScreenDestination)

    /**
     * Replaces the top Laydr entry with [launch].
     */
    public fun replace(launch: LaydrNavLaunch)

    /**
     * Performs route-facing back for this stack.
     */
    public fun back(): Boolean

    /**
     * Pushes [launch] and registers a one-shot typed result callback for the
     * created entry.
     */
    public fun <Result : Any> pushForResult(
        launch: LaydrNavLaunch,
        onCancel: () -> Unit,
        resultType: KClass<Result>,
        onResult: (Result) -> Unit,
    )
}

/**
 * Pushes [launch] and registers a one-shot typed result callback for the
 * created entry.
 */
public inline fun <reified Result : Any> LaydrNavStackNavigator.pushForResult(
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

/**
 * Remembers one pure Laydr-managed AndroidX Nav3 stack.
 *
 * The created stack is an AndroidX [NavBackStack] remembered with
 * `rememberNavBackStack`, so Laydr route identity restores across Android
 * process death. Restored entries contain route id and route parameters only;
 * payloads, route-result callbacks, entry tokens, and entry metadata are
 * transient.
 */
@Composable
public fun rememberLaydrNavStack(
    routeDefinitions: LaydrComposeRouteDefinitions,
    initialDestination: LaydrScreenDestination,
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: LaydrNavEntryMetadataProvider = LaydrNavEntryMetadataProvider {
        LaydrNavEntryMetadata.Empty
    },
    notFoundContent: @Composable (notFound: LaydrNavNotFound) -> Unit,
): LaydrNavStack {
    val initialKey = routeDefinitions.appGraph.validatedNavKey(initialDestination)
    val backStack = rememberNavBackStack(initialKey)
    return rememberLaydrNavStack(
        routeDefinitions = routeDefinitions,
        backStack = backStack,
        sceneSupport = sceneSupport,
        entryMetadata = entryMetadata,
        notFoundContent = notFoundContent,
    )
}

/**
 * Remembers Laydr management attached to an app-owned or mixed AndroidX Nav3
 * stack.
 *
 * Pass an AndroidX [NavBackStack] whose elements implement [NavKey]. Laydr
 * navigation mutates only the trailing Laydr suffix after the last foreign
 * key. Foreign keys are app-owned and must be AndroidX-serializable when the
 * parent stack needs process-death restoration.
 */
@Composable
public fun rememberLaydrNavStack(
    routeDefinitions: LaydrComposeRouteDefinitions,
    backStack: NavBackStack<NavKey>,
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: LaydrNavEntryMetadataProvider = LaydrNavEntryMetadataProvider {
        LaydrNavEntryMetadata.Empty
    },
    notFoundContent: @Composable (notFound: LaydrNavNotFound) -> Unit,
): LaydrNavStack {
    val coordinator = remember(routeDefinitions.appGraph, backStack, sceneSupport) {
        LaydrNavStackCoordinator(
            appGraph = routeDefinitions.appGraph,
            backStack = backStack,
            sceneSupport = sceneSupport,
        )
    }
    val entryStore = remember(coordinator) {
        LaydrNavEntryStore { coordinator.backStack }
    }
    val entryProvider = laydrNavEntryProvider(
        routeDefinitions = routeDefinitions,
        sceneSupport = sceneSupport,
        entryMetadata = { context -> entryMetadata.metadata(context).values },
        entryStore = entryStore,
        notFoundContent = notFoundContent,
    )
    return remember(coordinator, entryProvider, entryStore) {
        LaydrNavStack(
            coordinator = coordinator,
            entryProvider = entryProvider,
            entryStore = entryStore,
        )
    }
}

@Composable
internal fun rememberLaydrNavStackCoordinator(
    appGraph: LaydrAppGraph,
    initialDestination: LaydrScreenDestination,
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
): LaydrNavStackCoordinator {
    val initialKey = appGraph.validatedNavKey(initialDestination)
    val backStack = rememberNavBackStack(initialKey)
    return remember(appGraph, backStack, sceneSupport) {
        LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = backStack,
            sceneSupport = sceneSupport,
        )
    }
}

/**
 * Managed AndroidX Nav3 stack whose Laydr entries are created, tracked, and
 * rendered through Laydr runtime APIs.
 */
public class LaydrNavStack internal constructor(
    internal val coordinator: LaydrNavStackCoordinator,
    /**
     * AndroidX Nav3 entry provider backed by generated route-owned definitions.
     */
    public val entryProvider: (NavKey) -> NavEntry<NavKey>,
    internal val entryStore: LaydrNavEntryStore = LaydrNavEntryStore {
        coordinator.backStack
    },
) {
    /**
     * Mutable AndroidX Nav3 back stack owned by the app or by this Laydr stack.
     */
    public val backStack: NavBackStack<NavKey>
        get() = coordinator.backStack

    /**
     * Route-facing navigator for this stack.
     */
    public val navigator: LaydrNavStackNavigator = StackNavigator()

    /**
     * Current generated Laydr path for the logical top of [backStack], or
     * `null`.
     */
    public val currentPath: String?
        get() = coordinator.currentPath

    /**
     * True when route-facing Back can remove the current Laydr entry.
     */
    public val canPop: Boolean
        get() = coordinator.canPopLaydrEntry

    /**
     * Replaces the trailing Laydr suffix with [destination].
     */
    public fun reset(destination: LaydrScreenDestination) {
        reset(LaydrNavLaunch(destination = destination))
    }

    /**
     * Replaces the trailing Laydr suffix with [launch].
     */
    public fun reset(launch: LaydrNavLaunch) {
        val key = entryStore.keyForLaunch(
            key = coordinator.validatedKey(launch.destination),
            launch = launch,
        )
        coordinator.resetKey(key)
        entryStore.prune()
    }

    /**
     * Pushes the screen route represented by URL-like [input].
     */
    public fun pushExternalTarget(input: String): LaydrNavExternalTargetResult =
        coordinator.pushExternalTarget(input).also {
            entryStore.prune()
        }

    /**
     * Replaces the top Laydr entry with the screen route represented by
     * URL-like [input].
     */
    public fun replaceExternalTarget(input: String): LaydrNavExternalTargetResult =
        coordinator.replaceExternalTarget(input).also {
            entryStore.prune()
        }

    private inner class StackNavigator : LaydrNavStackNavigator {
        override fun push(destination: LaydrScreenDestination) {
            push(LaydrNavLaunch(destination = destination))
        }

        override fun push(launch: LaydrNavLaunch) {
            val key = entryStore.keyForLaunch(
                key = coordinator.validatedKey(launch.destination),
                launch = launch,
            )
            coordinator.pushNewEntryKey(key)
            entryStore.prune()
        }

        override fun replace(destination: LaydrScreenDestination) {
            replace(LaydrNavLaunch(destination = destination))
        }

        override fun replace(launch: LaydrNavLaunch) {
            val key = entryStore.keyForLaunch(
                key = coordinator.validatedKey(launch.destination),
                launch = launch,
            )
            coordinator.replaceKey(key)
            entryStore.prune()
        }

        override fun back(): Boolean =
            coordinator.popLaydrEntry().also {
                entryStore.prune()
            }

        override fun <Result : Any> pushForResult(
            launch: LaydrNavLaunch,
            onCancel: () -> Unit,
            resultType: KClass<Result>,
            onResult: (Result) -> Unit,
        ) {
            val key = entryStore.keyForLaunchAndResult(
                key = coordinator.validatedKey(launch.destination),
                launch = launch,
                resultType = resultType,
                onCancel = onCancel,
                onResult = { result ->
                    @Suppress("UNCHECKED_CAST")
                    onResult(result as Result)
                },
            )
            coordinator.pushNewEntryKey(key)
            entryStore.prune()
        }
    }
}

internal class LaydrNavStackCoordinator internal constructor(
    val appGraph: LaydrAppGraph,
    val backStack: NavBackStack<NavKey>,
    val sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
) {
    internal val engine = LaydrNavStackEngine(
        appGraph = appGraph,
        backStack = backStack,
        keyAdapter = AndroidxLaydrNavKeyAdapter,
        sceneSupport = sceneSupport.runtimeSupport,
    )

    val currentPath: String?
        get() = engine.currentPath

    val canPopLaydrEntry: Boolean
        get() = engine.canPopLaydrEntry

    fun pushExternalTarget(input: String): LaydrNavExternalTargetResult {
        return engine.pushExternalTarget(input).toAndroidx()
    }

    fun pushNewEntryKey(key: LaydrNavKey) {
        engine.pushNewEntryKey(key.entryKey)
    }

    fun replaceExternalTarget(input: String): LaydrNavExternalTargetResult {
        return engine.replaceExternalTarget(input).toAndroidx()
    }

    fun replaceKey(key: LaydrNavKey) {
        engine.replaceKey(key.entryKey)
    }

    fun reset(destination: LaydrScreenDestination) {
        engine.reset(destination)
    }

    fun resetKey(key: LaydrNavKey) {
        engine.resetKey(key.entryKey)
    }

    fun popLaydrEntry(): Boolean {
        return engine.popLaydrEntry()
    }

    fun validatedKey(destination: LaydrScreenDestination): LaydrNavKey {
        return LaydrNavKey(engine.validatedKey(destination))
    }
}

internal object AndroidxLaydrNavKeyAdapter : LaydrNavKeyAdapter<NavKey> {
    override fun entryKey(key: NavKey): LaydrNavEntryKey? =
        (key as? LaydrNavKey)?.entryKey

    override fun key(entryKey: LaydrNavEntryKey): NavKey =
        LaydrNavKey(entryKey)
}
