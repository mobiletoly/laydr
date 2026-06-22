// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.nav3.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.goquick.laydr.core.LaydrAppGraph
import dev.goquick.laydr.core.LaydrParameterlessScreenRouteRef
import dev.goquick.laydr.core.LaydrRoute
import dev.goquick.laydr.core.LaydrRouteKey
import dev.goquick.laydr.core.LaydrRouteSection
import dev.goquick.laydr.core.LaydrRouteSections
import dev.goquick.laydr.core.LaydrScreenDestination
import dev.goquick.laydr.core.LaydrScreenRouteRef
import dev.goquick.laydr.core.laydrRouteSections

/**
 * Remembers ordered Nav3 sections declared from generated Laydr routes.
 *
 * Prefer this overload for app shells: static section routes can provide their
 * own default destinations through [laydrNavSection], while [Data]
 * carries app-owned labels, icons, visibility, or other UI policy.
 */
@Composable
public fun <Data : Any> rememberLaydrNavSectionSet(
    appGraph: LaydrAppGraph,
    vararg sectionSpecs: LaydrNavSectionSpec<Data>,
): LaydrNavSectionSet<Data> {
    val sectionKeys = sectionSpecs.map { spec ->
        SectionSpecRememberKey(
            routeId = spec.route.route.id,
            rootKey = spec.rootDestination.routeKey,
            data = spec.data,
        )
    }
    return remember(appGraph, sectionKeys) {
        laydrNavSectionSet(
            appGraph,
            *sectionSpecs,
        )
    }
}

/**
 * Builds ordered Nav3 sections from generated Laydr route specs.
 */
public fun <Data : Any> laydrNavSectionSet(
    appGraph: LaydrAppGraph,
    vararg sectionSpecs: LaydrNavSectionSpec<Data>,
): LaydrNavSectionSet<Data> =
    buildLaydrNavSectionSet(
        appGraph = appGraph,
        sectionSpecs = sectionSpecs.asList(),
    )

/**
 * Remembers ordered Nav3 sections declared with explicit root
 * destinations and no app-owned section data.
 *
 * This overload is intended for unusual section roots that cannot use the
 * parameterless route shortcut.
 */
@Composable
public fun rememberLaydrNavSectionSet(
    appGraph: LaydrAppGraph,
    block: LaydrNavSectionSetBuilder.() -> Unit,
): LaydrNavSectionSet<Unit> =
    remember(appGraph) {
        LaydrNavSectionSetBuilder(appGraph)
            .apply(block)
            .build()
    }

/**
 * Declarative section specification used to build [LaydrNavSectionSet].
 *
 * @param Data app-owned data attached to the section.
 * @param route generated screen route ref that owns the section subtree.
 * @param rootDestination destination used to initialize the section stack.
 * @param data app-owned section data.
 */
public class LaydrNavSectionSpec<Data : Any> public constructor(
    route: LaydrScreenRouteRef,
    rootDestination: LaydrScreenDestination,
    data: Data,
) {
    /**
     * Generated screen route ref that owns the section subtree.
     */
    public val route: LaydrScreenRouteRef = route

    /**
     * Destination used to initialize this section stack.
     */
    public val rootDestination: LaydrScreenDestination = rootDestination

    /**
     * App-owned section data such as labels, icons, or visibility policy.
     */
    public val data: Data = data
}

/**
 * Creates a section spec for a parameterless generated screen route.
 */
public fun <Data : Any> laydrNavSection(
    route: LaydrParameterlessScreenRouteRef,
    data: Data,
): LaydrNavSectionSpec<Data> =
    LaydrNavSectionSpec(
        route = route,
        rootDestination = route.defaultDestination,
        data = data,
    )

/**
 * Creates a no-data section spec for a parameterless generated screen route.
 */
public fun laydrNavSection(
    route: LaydrParameterlessScreenRouteRef,
): LaydrNavSectionSpec<Unit> =
    laydrNavSection(route = route, data = Unit)

/**
 * Creates a section spec with an explicit root destination.
 *
 * Use this for screen route refs that need an app-chosen root destination.
 */
public fun <Data : Any> laydrNavSection(
    route: LaydrScreenRouteRef,
    rootDestination: LaydrScreenDestination,
    data: Data,
): LaydrNavSectionSpec<Data> =
    LaydrNavSectionSpec(
        route = route,
        rootDestination = rootDestination,
        data = data,
    )

/**
 * Creates a no-data section spec with an explicit root destination.
 */
public fun laydrNavSection(
    route: LaydrScreenRouteRef,
    rootDestination: LaydrScreenDestination,
): LaydrNavSectionSpec<Unit> =
    laydrNavSection(
        route = route,
        rootDestination = rootDestination,
        data = Unit,
    )


/**
 * Ordered Nav3 sections backed by a generated Laydr app graph.
 *
 * Sections expose membership, root destinations, and app-owned section data.
 * Apps still decide how sections are displayed and when they are visible.
 */
public class LaydrNavSectionSet<Data : Any> internal constructor(
    /**
     * Generated app graph used for destination validation and membership.
     */
    public val appGraph: LaydrAppGraph,
    /**
     * UI-neutral section membership derived from the generated route map.
     */
    public val routeSections: LaydrRouteSections,
    sectionSpecsByRootKey: Map<LaydrRouteKey, LaydrNavSectionSpec<Data>>,
) {
    private val sectionsByRootKey: Map<LaydrRouteKey, LaydrNavSection<Data>>
    internal val selectedSectionStateKey: List<LaydrNavSectionStateKey>

    /**
     * Ordered section entries in app declaration order.
     */
    public val items: List<LaydrNavSection<Data>>

    init {
        items = routeSections.items.map { routeSection ->
            val spec = sectionSpecsByRootKey.getValue(routeSection.rootKey)
            LaydrNavSection(
                routeSection = routeSection,
                rootDestination = spec.rootDestination,
                data = spec.data,
            )
        }
        sectionsByRootKey = items.associateBy { section -> section.rootKey }
        selectedSectionStateKey = items.map { section ->
            LaydrNavSectionStateKey(
                routeId = section.route.route.id,
                rootKey = section.rootKey,
            )
        }
    }

    /**
     * Returns the first section for [routeId], or `null` when no section has
     * that id.
     */
    public fun sectionForRouteId(routeId: String): LaydrNavSection<Data>? =
        items.firstOrNull { section -> section.route.route.id == routeId }

    /**
     * Returns the section for [rootKey], or `null` when no section has that
     * exact scoped root destination.
     */
    public fun sectionForRootKey(rootKey: LaydrRouteKey): LaydrNavSection<Data>? =
        sectionsByRootKey[rootKey]

    /**
     * Returns the section that owns [destination], or `null` when the
     * destination is invalid for [appGraph] or outside every declared section.
     */
    public fun sectionFor(destination: LaydrScreenDestination): LaydrNavSection<Data>? {
        appGraph.screenRouteFor(destination) ?: return null
        return sectionFor(destination.routeKey)
    }

    /**
     * Returns the section that owns [path], or `null` when the path is
     * malformed, unknown, layout-only, invalid, or outside every declared
     * section.
     */
    public fun sectionForPath(path: String): LaydrNavSection<Data>? {
        val result = laydrNavPathResult(path = path, routeMap = appGraph.routeMap)
        val accepted = result as? LaydrNavPathAccepted ?: return null
        return sectionFor(accepted.key.toLaydrRouteKey())
    }

    /**
     * Returns the section that owns URL-like [input], or `null` when the target
     * is malformed, unknown, layout-only, invalid, or outside every declared
     * section.
     */
    public fun sectionForExternalTarget(input: String): LaydrNavSection<Data>? {
        val result = laydrNavExternalTargetResult(input = input, routeMap = appGraph.routeMap)
        val accepted = result as? LaydrNavExternalTargetAccepted ?: return null
        return sectionFor(accepted.key.toLaydrRouteKey())
    }

    /**
     * Returns the section that owns [key], or `null` when the key is invalid,
     * layout-only, unknown, or outside every declared section.
     */
    public fun sectionFor(key: LaydrRouteKey): LaydrNavSection<Data>? {
        appGraph.routeMap.screenRouteFor(key) ?: return null
        appGraph.routeMap.pathFor(key) ?: return null
        val routeSection = routeSections.sectionFor(key) ?: return null
        return sectionsByRootKey[routeSection.rootKey]
    }

    /**
     * Returns section placement for [destination], or `null` when the
     * destination is invalid for [appGraph] or outside every declared section.
     */
    public fun placementFor(destination: LaydrScreenDestination): LaydrNavSectionPlacement<Data>? {
        val route = appGraph.screenRouteFor(destination) ?: return null
        return placementFor(routeKey = destination.routeKey, route = route)
    }

    /**
     * Returns section placement for [key], or `null` when the key is invalid,
     * layout-only, unknown, or outside every declared section.
     */
    public fun placementFor(key: LaydrRouteKey): LaydrNavSectionPlacement<Data>? {
        val route = appGraph.routeMap.screenRouteFor(key) ?: return null
        appGraph.routeMap.pathFor(key) ?: return null
        return placementFor(routeKey = key, route = route)
    }

    internal fun sectionForStateId(stateId: String): LaydrNavSection<Data>? =
        items.firstOrNull { section -> section.stateId == stateId }

    private fun placementFor(
        routeKey: LaydrRouteKey,
        route: LaydrRoute,
    ): LaydrNavSectionPlacement<Data>? {
        val section = sectionFor(routeKey) ?: return null
        val routeChain = appGraph.routeMap.routeChainFor(route)
        val sectionRootIndex = routeChain.indexOf(section.route.route)
        if (sectionRootIndex < 0) {
            return null
        }
        val screenDepth = routeChain
            .drop(sectionRootIndex + 1)
            .count { chainedRoute -> chainedRoute in appGraph.routeMap.screenRoutes }
        return LaydrNavSectionPlacement(
            section = section,
            isSectionRoot = routeKey == section.rootKey,
            depthFromSectionRoot = screenDepth,
        )
    }
}

/**
 * Placement of one resolved Laydr screen route inside declared app sections.
 *
 * [depthFromSectionRoot] counts renderable screen routes between the section
 * root and the target, excluding the root and ignoring layout-only or
 * namespace-only routes.
 */
public class LaydrNavSectionPlacement<Data : Any> public constructor(
    /**
     * Section that owns the resolved route.
     */
    public val section: LaydrNavSection<Data>,
    /**
     * True when the resolved key is the concrete section root destination.
     */
    public val isSectionRoot: Boolean,
    /**
     * Number of renderable screen routes between the section root and target.
     */
    public val depthFromSectionRoot: Int,
)

/**
 * One Nav3 section declared from a generated screen route ref.
 *
 * The [rootDestination] initializes that section's Nav3 stack. The
 * [route], [rootKey], and [data] expose app-shell information without moving
 * app-owned UI policy into Laydr.
 */
public class LaydrNavSection<Data : Any> internal constructor(
    /**
     * UI-neutral route section used for subtree membership.
     */
    public val routeSection: LaydrRouteSection,
    /**
     * Generated screen destination used as this section stack's root.
     */
    public val rootDestination: LaydrScreenDestination,
    /**
     * App-owned section data.
     */
    public val data: Data,
) {
    /**
     * Generated screen route ref that acts as this section's root.
     */
    public val route: LaydrScreenRouteRef = routeSection.route

    /**
     * Framework-neutral key for the section root route.
     */
    public val rootKey: LaydrRouteKey = routeSection.rootKey

    internal val stateId: String = rootKey.nav3SectionStateId()
}

/**
 * Builder for explicit-root Nav3 section declarations.
 */
public class LaydrNavSectionSetBuilder internal constructor(
    /**
     * Generated app graph that declared sections must belong to.
     */
    public val appGraph: LaydrAppGraph,
) {
    private val declarations = mutableListOf<LaydrNavSectionSpec<Unit>>()

    /**
     * Declares [route] as a section root initialized by [rootDestination].
     *
     * The route must be a generated screen route in [appGraph], and
     * [rootDestination] must resolve to that exact route. When the route has
     * dynamic parameters, [rootDestination] scopes this section to those root
     * parameter values.
     */
    public fun section(
        route: LaydrScreenRouteRef,
        rootDestination: LaydrScreenDestination,
    ) {
        declarations += laydrNavSection(
            route = route,
            rootDestination = rootDestination,
            data = Unit,
        )
    }

    internal fun build(): LaydrNavSectionSet<Unit> =
        buildLaydrNavSectionSet(
            appGraph = appGraph,
            sectionSpecs = declarations,
        )
}

private fun <Data : Any> buildLaydrNavSectionSet(
    appGraph: LaydrAppGraph,
    sectionSpecs: List<LaydrNavSectionSpec<Data>>,
): LaydrNavSectionSet<Data> {
    require(sectionSpecs.isNotEmpty()) {
        "Laydr Nav sections must declare at least one section"
    }

    val rootKeys = mutableSetOf<LaydrRouteKey>()
    sectionSpecs.forEach { spec ->
        validateSectionSpec(appGraph = appGraph, spec = spec)
        require(rootKeys.add(spec.rootDestination.routeKey)) {
            "Duplicate Laydr Nav section root: ${spec.rootDestination.routeKey}"
        }
    }

    val routeSections = laydrRouteSections(appGraph.routeMap) {
        sectionSpecs.forEach { spec ->
            section(route = spec.route, rootKey = spec.rootDestination.routeKey)
        }
    }
    val specsByRootKey = sectionSpecs.associateBy { spec -> spec.rootDestination.routeKey }
    return LaydrNavSectionSet(
        appGraph = appGraph,
        routeSections = routeSections,
        sectionSpecsByRootKey = specsByRootKey,
    )
}

private fun <Data : Any> validateSectionSpec(
    appGraph: LaydrAppGraph,
    spec: LaydrNavSectionSpec<Data>,
) {
    val route = spec.route
    require(appGraph.routeMap.screenRoutes.contains(route.route)) {
        "Laydr Nav section root must be a screen route in this app graph: ${route.route.id}"
    }
    val rootRoute = appGraph.screenRouteFor(spec.rootDestination)
        ?: throw IllegalArgumentException(
            "Laydr Nav section root destination is not valid for this app graph: " +
                "${spec.rootDestination.routeKey}",
        )
    require(rootRoute == route.route) {
        "Laydr Nav section root destination must resolve to ${route.route.id}: " +
            "${spec.rootDestination.routeKey}"
    }
}

private data class SectionSpecRememberKey(
    val routeId: String,
    val rootKey: LaydrRouteKey,
    val data: Any,
)

internal data class LaydrNavSectionStateKey(
    val routeId: String,
    val rootKey: LaydrRouteKey,
)

private fun LaydrRouteKey.nav3SectionStateId(): String =
    buildString {
        append(routeId.length)
        append(':')
        append(routeId)
        parameters.entries
            .sortedBy { entry -> entry.key }
            .forEach { (name, value) ->
                append('|')
                append(name.length)
                append(':')
                append(name)
                append('=')
                append(value.length)
                append(':')
                append(value)
            }
    }
