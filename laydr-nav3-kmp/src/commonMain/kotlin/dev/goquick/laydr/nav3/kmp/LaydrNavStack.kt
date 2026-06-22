// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SceneStrategy
import androidx.savedstate.serialization.SavedStateConfiguration
import dev.goquick.laydr.compose.LaydrComposeRouteDefinitions
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.nav.runtime.LaydrNavEntryKey
import dev.goquick.laydr.nav.runtime.LaydrNavKeyAdapter
import dev.goquick.laydr.nav.runtime.LaydrNavStackEngine
import kotlin.reflect.KClass

/**
 * Provides app-owned metadata for each resolved Laydr Nav3 entry in a stack.
 */
public fun interface LaydrNavEntryMetadataProvider {
    /**
     * Returns metadata for the entry described by [context].
     */
    public fun metadata(context: LaydrNavEntryContext): LaydrNavEntryMetadata
}

/**
 * Route-facing capability for a Laydr-managed Nav3 stack.
 *
 * This interface intentionally excludes reset and external-target helpers.
 * Those operations belong to stack owners, not arbitrary route content.
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
 * Remembers one pure Laydr-managed Nav3 stack.
 *
 * The stack is initialized from [initialDestination], validates every Laydr
 * destination against [routeDefinitions], installs payload/result entry
 * locals, and returns the pieces an app passes to Nav3 `NavDisplay`.
 */
@Composable
public fun rememberLaydrNavStack(
    routeDefinitions: LaydrComposeRouteDefinitions,
    initialDestination: LaydrScreenDestination,
    sceneSupport: LaydrNavSceneSupport = LaydrNavSceneSupport.None,
    entryMetadata: LaydrNavEntryMetadataProvider = LaydrNavEntryMetadataProvider {
        LaydrNavEntryMetadata.Empty
    },
    savedStateConfiguration: SavedStateConfiguration =
        laydrNavSavedStateConfiguration(),
    notFoundContent: @Composable (notFound: LaydrNavNotFound) -> Unit,
): LaydrNavStack {
    val initialKey = routeDefinitions.appGraph.validatedNavKey(initialDestination)
    val initialStack = sceneSupport.expandedStackFor(initialKey) ?: listOf(initialKey)
    val backStack = rememberNavBackStack(
        savedStateConfiguration,
        *initialStack.toTypedArray(),
    )
    return rememberLaydrNavStack(
        routeDefinitions = routeDefinitions,
        backStack = backStack,
        sceneSupport = sceneSupport,
        entryMetadata = entryMetadata,
        notFoundContent = notFoundContent,
    )
}

/**
 * Remembers Laydr management attached to an app-owned or mixed Nav3 stack.
 *
 * Foreign [NavKey] entries may remain in [backStack]. Laydr navigation mutates
 * only the trailing Laydr suffix after the last foreign key.
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
    savedStateConfiguration: SavedStateConfiguration =
        laydrNavSavedStateConfiguration(),
): LaydrNavStackCoordinator {
    val initialKey = appGraph.validatedNavKey(initialDestination)
    val initialStack = sceneSupport.expandedStackFor(initialKey) ?: listOf(initialKey)
    val backStack = rememberNavBackStack(
        savedStateConfiguration,
        *initialStack.toTypedArray(),
    )
    return remember(appGraph, backStack, sceneSupport) {
        LaydrNavStackCoordinator(
            appGraph = appGraph,
            backStack = backStack,
            sceneSupport = sceneSupport,
        )
    }
}

/**
 * Managed Nav3 stack whose Laydr entries are created, tracked, and rendered
 * through Laydr runtime APIs.
 *
 * The constructor is internal. Apps create stacks with [rememberLaydrNavStack].
 * Reset and external-target helpers are owner-facing operations; route content
 * should receive [navigator] when an app approves stack mutation from nested
 * content.
 */
public class LaydrNavStack internal constructor(
    internal val coordinator: LaydrNavStackCoordinator,
    /**
     * Nav3 entry provider backed by generated route-owned definitions.
     */
    public val entryProvider: (NavKey) -> NavEntry<NavKey>,
    internal val entryStore: LaydrNavEntryStore = LaydrNavEntryStore {
        coordinator.backStack
    },
) {
    /**
     * Mutable Nav3 back stack owned by the app or by this Laydr stack.
     */
    public val backStack: NavBackStack<NavKey>
        get() = coordinator.backStack

    /**
     * Route-facing navigator for this stack.
     */
    public val navigator: LaydrNavStackNavigator = StackNavigator()

    /**
     * Nav3 scene strategies for Laydr entries.
     */
    public val sceneStrategies: List<SceneStrategy<NavKey>>
        get() = coordinator.sceneSupport.sceneStrategies

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
     * Recognized list-detail route facts for the current Laydr suffix, or
     * `null`.
     */
    public val listDetailStackState: LaydrNavListDetailStackState?
        get() = coordinator.listDetailStackState

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
        keyAdapter = KmpLaydrNavKeyAdapter,
        sceneSupport = sceneSupport.runtimeSupport,
    )

    val currentKey: NavKey?
        get() = engine.currentKey

    val currentLaydrKey: LaydrNavKey?
        get() = engine.currentEntryKey?.let(::LaydrNavKey)

    val currentPath: String?
        get() = engine.currentPath

    val canPopLaydrEntry: Boolean
        get() = engine.canPopLaydrEntry

    val listDetailStackState: LaydrNavListDetailStackState?
        get() {
            val state = engine.listDetailStackState ?: return null
            return LaydrNavListDetailStackState(
                listKey = LaydrNavKey(state.listKey),
                detailKey = state.detailKey?.let(::LaydrNavKey),
                shape = state.shape,
            )
        }

    fun push(destination: LaydrScreenDestination) {
        engine.push(destination)
    }

    fun pushPath(path: String): LaydrNavPathResult {
        return engine.pushPath(path).toKmp()
    }

    fun pushExternalTarget(input: String): LaydrNavExternalTargetResult {
        return engine.pushExternalTarget(input).toKmp()
    }

    fun pushKey(key: LaydrNavKey) {
        engine.pushKey(key.entryKey)
    }

    fun pushNewEntryKey(key: LaydrNavKey) {
        engine.pushNewEntryKey(key.entryKey)
    }

    fun replace(destination: LaydrScreenDestination) {
        engine.replace(destination)
    }

    fun replacePath(path: String): LaydrNavPathResult {
        return engine.replacePath(path).toKmp()
    }

    fun replaceExternalTarget(input: String): LaydrNavExternalTargetResult {
        return engine.replaceExternalTarget(input).toKmp()
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

    internal fun allEntryKeys(): List<LaydrNavEntryKey>? =
        engine.allEntryKeys()

    internal fun replaceWithEntryKeys(keys: List<LaydrNavEntryKey>) {
        engine.replaceWithEntryKeys(keys)
    }
}

internal object KmpLaydrNavKeyAdapter : LaydrNavKeyAdapter<NavKey> {
    override fun entryKey(key: NavKey): LaydrNavEntryKey? =
        (key as? LaydrNavKey)?.entryKey

    override fun key(entryKey: LaydrNavEntryKey): NavKey =
        LaydrNavKey(entryKey)
}

internal fun LaydrAppGraph.validatedNavKey(
    destination: LaydrScreenDestination,
): LaydrNavKey {
    requireScreenRoute(destination)
    return destination.navKey()
}
