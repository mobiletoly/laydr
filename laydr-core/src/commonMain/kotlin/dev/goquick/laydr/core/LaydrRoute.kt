// Copyright 2026 Toly Pochkin
// SPDX-License-Identifier: Apache-2.0

package dev.goquick.laydr.core

/**
 * Immutable description of one route in a Laydr route tree.
 *
 * A route is framework-neutral: it describes a URI path template and optional
 * child routes, but it does not own Compose navigation, HTTP handling, screen
 * rendering, or application policy.
 *
 * @param id stable generated or app-defined identity used for diagnostics and lookup.
 * @param segments absolute path segments for this route.
 * @param children child routes declared below this route.
 * @param metadata static app-owned metadata for this route declaration.
 */
public class LaydrRoute public constructor(
    /**
     * Stable generated or app-defined identity used for diagnostics and lookup.
     */
    public val id: String,
    segments: List<LaydrRouteSegment>,
    children: List<LaydrRoute> = emptyList(),
    /**
     * Static app-owned metadata for this route declaration.
     */
    public val metadata: LaydrRouteMetadata = LaydrRouteMetadata(name = id),
) {
    /**
     * Absolute path segments for this route.
     */
    public val segments: List<LaydrRouteSegment> = segments.toList()

    /**
     * Child routes declared below this route.
     */
    public val children: List<LaydrRoute> = children.toList()

    /**
     * URI path template for this route, such as `/users/{id}`.
     */
    public val pathTemplate: String =
        segments.joinToString(separator = "/", prefix = "/") { it.templatePart }

    /**
     * Dynamic path parameter names in segment order.
     */
    public val parameterNames: List<String> =
        segments.mapNotNull { segment ->
            (segment as? LaydrRouteSegment.Dynamic)?.parameterName
        }

    init {
        require(laydrRouteIdRegex.matches(id)) {
            "Route id must be dot-separated Kotlin-style identifiers: $id"
        }
        require(segments.isNotEmpty()) {
            "Route must contain at least one path segment: $id"
        }

        val duplicateParameter = parameterNames
            .groupingBy { it }
            .eachCount()
            .firstNotNullOfOrNull { (parameterName, count) ->
                parameterName.takeIf { count > 1 }
            }
        require(duplicateParameter == null) {
            "Route dynamic parameter names must be unique: $duplicateParameter"
        }

        val duplicateChildId = children
            .groupingBy { it.id }
            .eachCount()
            .firstNotNullOfOrNull { (childId, count) ->
                childId.takeIf { count > 1 }
            }
        require(duplicateChildId == null) {
            "Child route ids must be unique under $id: $duplicateChildId"
        }

        val duplicateChildTemplate = children
            .groupingBy { it.pathTemplate }
            .eachCount()
            .firstNotNullOfOrNull { (pathTemplate, count) ->
                pathTemplate.takeIf { count > 1 }
            }
        require(duplicateChildTemplate == null) {
            "Child route templates must be unique under $id: $duplicateChildTemplate"
        }

        val invalidChild = children.firstOrNull { child ->
            child.segments.size <= segments.size ||
                child.segments.take(segments.size) != segments
        }
        require(invalidChild == null) {
            "Child route ${invalidChild?.id} must extend parent route $id"
        }
    }

    /**
     * Builds a concrete URI path by substituting dynamic segment [parameters].
     *
     * Dynamic values are encoded as URI path segments. The provided map must
     * contain exactly this route's [parameterNames].
     */
    public fun buildPath(parameters: Map<String, String> = emptyMap()): String {
        validateParameters(parameters)

        return segments.joinToString(separator = "/", prefix = "/") { segment ->
            when (segment) {
                is LaydrRouteSegment.Static -> segment.value
                is LaydrRouteSegment.Dynamic -> {
                    val value = parameters.getValue(segment.parameterName)
                    require(value.isNotBlank()) {
                        "Route parameter ${segment.parameterName} must not be blank"
                    }
                    encodePathSegment(value)
                }
            }
        }
    }

    /**
     * Creates a framework-neutral key for this route after validating [parameters].
     *
     * Validation uses [buildPath], so missing, extra, or blank dynamic
     * parameters fail the same way they fail when building a path.
     */
    public fun key(parameters: Map<String, String> = emptyMap()): LaydrRouteKey {
        buildPath(parameters)
        return LaydrRouteKey(routeId = id, parameters = parameters)
    }

    /**
     * Matches [path] against this route's own path template.
     *
     * Returns `null` when [path] has a query, fragment, malformed
     * percent-encoding, static segment mismatch, or a different segment count.
     */
    public fun match(path: String): LaydrRouteMatch? {
        val pathSegments = splitPath(path) ?: return null
        if (pathSegments.size != segments.size) {
            return null
        }

        val parameters = mutableMapOf<String, String>()
        for ((segment, rawValue) in segments.zip(pathSegments)) {
            when (segment) {
                is LaydrRouteSegment.Static -> if (segment.value != rawValue) {
                    return null
                }
                is LaydrRouteSegment.Dynamic -> {
                    if (rawValue.isBlank()) {
                        return null
                    }
                    val decodedValue = decodePathSegment(rawValue) ?: return null
                    if (decodedValue.isBlank()) {
                        return null
                    }
                    parameters[segment.parameterName] = decodedValue
                }
            }
        }

        return LaydrRouteMatch(route = this, parameters = parameters)
    }

    /**
     * Matches [path] against this route and descendants in route-specificity
     * order, with static siblings preferred over dynamic siblings.
     */
    public fun findMatch(path: String): LaydrRouteMatch? =
        match(path) ?: children.sortedForMatching().firstNotNullOfOrNull { child ->
            child.findMatch(path)
        }

    private fun validateParameters(parameters: Map<String, String>) {
        val expectedNames = parameterNames.toSet()
        val actualNames = parameters.keys

        val missingNames = expectedNames - actualNames
        require(missingNames.isEmpty()) {
            "Route parameters missing for $id: ${missingNames.sorted()}"
        }

        val extraNames = actualNames - expectedNames
        require(extraNames.isEmpty()) {
            "Route parameters not used by $id: ${extraNames.sorted()}"
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LaydrRoute &&
            id == other.id &&
            segments == other.segments &&
            children == other.children &&
            metadata == other.metadata

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + segments.hashCode()
        result = 31 * result + children.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String =
        "LaydrRoute(id=$id, segments=$segments, children=$children, metadata=$metadata)"

}

/**
 * Framework-neutral key for one concrete Laydr route destination.
 *
 * The key stores the stable route identity plus decoded dynamic parameter
 * values. Runtime adapters may adapt this value into their own key types.
 *
 * @param routeId stable Laydr route id.
 * @param parameters decoded dynamic route parameter values.
 */
public class LaydrRouteKey public constructor(
    /**
     * Stable Laydr route id.
     */
    public val routeId: String,
    parameters: Map<String, String> = emptyMap(),
) {
    /**
     * Decoded dynamic route parameter values.
     */
    public val parameters: Map<String, String> = parameters.toMap()

    init {
        require(laydrRouteIdRegex.matches(routeId)) {
            "Route id must be dot-separated Kotlin-style identifiers: $routeId"
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LaydrRouteKey &&
            routeId == other.routeId &&
            parameters == other.parameters

    override fun hashCode(): Int {
        var result = routeId.hashCode()
        result = 31 * result + parameters.hashCode()
        return result
    }

    override fun toString(): String =
        "LaydrRouteKey(routeId=$routeId, parameters=$parameters)"
}

/**
 * App-facing reference to one concrete generated Laydr route destination.
 *
 * A destination is the value application code should pass around when it wants
 * to navigate to a route. Runtime adapters may convert [routeKey] into their
 * own stack-key shape, but destinations themselves stay framework-neutral.
 */
public interface LaydrDestination {
    /**
     * Framework-neutral route key represented by this destination.
     */
    public val routeKey: LaydrRouteKey
}

/**
 * App-facing destination for a generated Laydr screen route.
 *
 * Layout-only routes do not produce screen destinations because they cannot be
 * rendered as stack entries on their own.
 */
public interface LaydrScreenDestination : LaydrDestination

/**
 * Reference to a generated or app-owned Laydr route descriptor.
 */
public interface LaydrRouteRef {
    /**
     * Route descriptor represented by this reference.
     */
    public val route: LaydrRoute
}

/**
 * Returns true when [key] targets this route and carries valid parameters for
 * the route path template.
 */
public fun LaydrRouteRef.matches(key: LaydrRouteKey): Boolean =
    route.id == key.routeId &&
        runCatching { route.buildPath(key.parameters) }.isSuccess

/**
 * Returns true when this key targets [route] and carries valid route
 * parameters.
 */
public fun LaydrRouteKey.isRoute(route: LaydrRouteRef): Boolean =
    route.matches(this)

/**
 * Returns true when this key is valid in [routeMap] and belongs to
 * [parentRoute]'s route subtree.
 */
public fun LaydrRouteKey.isInRouteTree(
    routeMap: LaydrRouteMap,
    parentRoute: LaydrRouteRef,
): Boolean =
    routeMap.contains(parentRoute = parentRoute, key = this)

/**
 * Reference to a Laydr route that is renderable as a screen endpoint.
 */
public interface LaydrScreenRouteRef : LaydrRouteRef

/**
 * Reference to a parameterless generated screen route.
 *
 * Generated static screen route objects implement this contract so runtime
 * adapters can initialize app-owned navigation structures without requiring
 * apps to spell the root destination manually.
 */
public interface LaydrParameterlessScreenRouteRef : LaydrScreenRouteRef {
    /**
     * Default destination for this parameterless screen route.
     */
    public val defaultDestination: LaydrScreenDestination
}

/**
 * Reference to a Laydr route that can wrap matched descendant content.
 */
public interface LaydrLayoutRouteRef : LaydrRouteRef

/**
 * Route kinds supported by Laydr route declarations.
 *
 * Code generation uses this kind to decide which generated route references,
 * destination helpers, and layout hooks a route should expose.
 */
public enum class LaydrRouteKind {
    /**
     * A route that can render a screen endpoint.
     */
    SCREEN,

    /**
     * A route that wraps matched descendant content without rendering a screen
     * endpoint on its own.
     */
    LAYOUT,

    /**
     * A route that both renders a screen endpoint and wraps matched descendant
     * content.
     */
    SCREEN_AND_LAYOUT,
}

/**
 * Compose-free route declaration for core-only Laydr route trees.
 *
 * Apps that enable Compose generation normally declare route rendering with
 * generated route-local `LaydrRouteDef` helpers. Core-only generators use this
 * declaration when they need a route kind without depending on Compose.
 *
 * @param kind the structural kind declared by a route `Route.kt` file.
 * @param name optional static readable name declared by the route.
 * @param labels optional static app-owned key/value labels declared by the
 * route.
 */
public class LaydrRouteDeclaration private constructor(
    /**
     * The structural kind declared by a route `Route.kt` file.
     */
    public val kind: LaydrRouteKind,
    /**
     * Optional static readable name declared by the route.
     */
    public val name: String?,
    labels: Map<String, String>,
) {
    /**
     * Optional static app-owned key/value labels declared by the route.
     */
    public val labels: Map<String, String> = labels.toMap()

    init {
        if (name != null) {
            require(name.isNotBlank()) {
                "Route declaration metadata name must not be blank"
            }
        }
        val blankLabelKey = this.labels.keys.firstOrNull { key -> key.isBlank() }
        require(blankLabelKey == null) {
            "Route declaration metadata label keys must not be blank"
        }
    }

    public companion object {
        /**
         * Declares a route that can render a screen endpoint.
         *
         * [name] and [labels] are static route metadata. They do not affect
         * route matching, navigation policy, or rendering.
         */
        public fun screen(
            name: String? = null,
            labels: Map<String, String> = emptyMap(),
        ): LaydrRouteDeclaration =
            LaydrRouteDeclaration(
                kind = LaydrRouteKind.SCREEN,
                name = name,
                labels = labels,
            )

        /**
         * Declares a route that wraps matched descendant content without
         * rendering a screen endpoint on its own.
         *
         * [name] and [labels] are static route metadata. They do not affect
         * route matching, navigation policy, or rendering.
         */
        public fun layout(
            name: String? = null,
            labels: Map<String, String> = emptyMap(),
        ): LaydrRouteDeclaration =
            LaydrRouteDeclaration(
                kind = LaydrRouteKind.LAYOUT,
                name = name,
                labels = labels,
            )

        /**
         * Declares a route that both renders a screen endpoint and wraps matched
         * descendant content.
         *
         * [name] and [labels] are static route metadata. They do not affect
         * route matching, navigation policy, or rendering.
         */
        public fun screenAndLayout(
            name: String? = null,
            labels: Map<String, String> = emptyMap(),
        ): LaydrRouteDeclaration =
            LaydrRouteDeclaration(
                kind = LaydrRouteKind.SCREEN_AND_LAYOUT,
                name = name,
                labels = labels,
            )
    }
}

/**
 * Framework-neutral lookup map for a generated Laydr route tree.
 *
 * The map owns structural route lookup, path/key conversion, section
 * membership, and inherited layout-chain lookup. It does not own rendering,
 * navigation state, labels, icons, resources, or application policy.
 *
 * @param routes top-level route descriptors.
 * @param screenRoutes flattened descriptors for routes that render screens.
 * @param layoutRoutes flattened descriptors for routes that render layouts.
 */
public class LaydrRouteMap public constructor(
    routes: List<LaydrRoute>,
    screenRoutes: List<LaydrRoute>,
    layoutRoutes: List<LaydrRoute>,
) {
    /**
     * Top-level route descriptors.
     */
    public val routes: List<LaydrRoute> = routes.toList()

    /**
     * Flattened descriptors for routes that render screens.
     */
    public val screenRoutes: List<LaydrRoute> = screenRoutes.toList()

    /**
     * Flattened descriptors for routes that render layouts.
     */
    public val layoutRoutes: List<LaydrRoute> = layoutRoutes.toList()

    private val allRoutes: List<LaydrRoute> = this.routes.flattenRoutes()
    private val allRouteSet: Set<LaydrRoute> = allRoutes.toSet()
    private val screenRouteSet: Set<LaydrRoute> = this.screenRoutes.toSet()
    private val layoutRouteSet: Set<LaydrRoute> = this.layoutRoutes.toSet()

    init {
        val missingScreenRoute = this.screenRoutes.firstOrNull { route -> route !in allRouteSet }
        require(missingScreenRoute == null) {
            "Screen route ${missingScreenRoute?.id} is not present in the Laydr route tree"
        }

        val missingLayoutRoute = this.layoutRoutes.firstOrNull { route -> route !in allRouteSet }
        require(missingLayoutRoute == null) {
            "Layout route ${missingLayoutRoute?.id} is not present in the Laydr route tree"
        }
    }

    /**
     * Resolves [path] to a framework-neutral route key by matching the route
     * tree roots and descendants in route-specificity order.
     */
    public fun keyForPath(path: String): LaydrRouteKey? =
        routes.sortedForMatching().firstNotNullOfOrNull { route ->
            route.findMatch(path)?.let { match ->
                LaydrRouteKey(
                    routeId = match.route.id,
                    parameters = match.parameters,
                )
            }
        }

    /**
     * Resolves [key] to its route descriptor, or returns `null` when the route
     * id is unknown.
     */
    public fun routeFor(key: LaydrRouteKey): LaydrRoute? =
        routes.firstNotNullOfOrNull { route -> route.findRouteById(key.routeId) }

    /**
     * Resolves [key] to a screen route descriptor, or returns `null` for
     * unknown or layout-only keys.
     */
    public fun screenRouteFor(key: LaydrRouteKey): LaydrRoute? =
        routeFor(key)?.takeIf { route -> route in screenRouteSet }

    /**
     * Builds the concrete path represented by [key].
     *
     * Returns `null` when [key] names an unknown route or carries invalid
     * parameters for the matched route.
     */
    public fun pathFor(key: LaydrRouteKey): String? {
        val route = routeFor(key) ?: return null
        return runCatching { route.buildPath(key.parameters) }.getOrNull()
    }

    /**
     * Returns route ancestry from the generated root to [route], including
     * [route], or an empty list when [route] is not part of this route map.
     */
    public fun routeChainFor(route: LaydrRoute): List<LaydrRoute> =
        routes.firstNotNullOfOrNull { root ->
            root.routeChainTo(route)
        }.orEmpty()

    /**
     * Returns inherited layout routes from outermost to innermost for [route].
     */
    public fun layoutChainFor(route: LaydrRoute): List<LaydrRoute> =
        routeChainFor(route).filter { chainedRoute -> chainedRoute in layoutRouteSet }

    /**
     * Returns the top-level generated route that contains [key], or `null`
     * when [key] is unknown.
     */
    public fun topLevelRouteFor(key: LaydrRouteKey): LaydrRoute? =
        routes.firstOrNull { route -> route.findRouteById(key.routeId) != null }

    /**
     * Returns true when [key] is valid in this route map and belongs to
     * [parentRoute]'s route subtree.
     */
    public fun contains(parentRoute: LaydrRouteRef, key: LaydrRouteKey): Boolean =
        parentRoute.route in allRouteSet &&
            pathFor(key) != null &&
            parentRoute.route.findRouteById(key.routeId) != null
}

/**
 * Framework-neutral generated app graph facade.
 *
 * The app graph is the stable app-level object adapters can consume when they
 * need the generated route map plus the renderable route subset. It validates
 * app-facing screen destinations without owning navigation state, rendering,
 * labels, icons, or platform policy.
 *
 * @param routeMap generated structural route map for this app.
 */
public class LaydrAppGraph public constructor(
    /**
     * Generated structural route map for this app.
     */
    public val routeMap: LaydrRouteMap,
) {
    /**
     * Top-level route descriptors.
     */
    public val routes: List<LaydrRoute>
        get() = routeMap.routes

    /**
     * Flattened descriptors for routes that render screens.
     */
    public val screenRoutes: List<LaydrRoute>
        get() = routeMap.screenRoutes

    /**
     * Flattened descriptors for routes that render layouts.
     */
    public val layoutRoutes: List<LaydrRoute>
        get() = routeMap.layoutRoutes

    /**
     * Resolves [destination] to a screen route descriptor in this app graph.
     *
     * Returns `null` when the destination points at an unknown route,
     * layout-only route, or carries invalid route parameters.
     */
    public fun screenRouteFor(destination: LaydrScreenDestination): LaydrRoute? {
        val route = routeMap.screenRouteFor(destination.routeKey) ?: return null
        routeMap.pathFor(destination.routeKey) ?: return null
        return route
    }

    /**
     * Builds the concrete path represented by [destination].
     *
     * Returns `null` when [destination] is not a valid screen destination for
     * this app graph.
     */
    public fun pathFor(destination: LaydrScreenDestination): String? {
        screenRouteFor(destination) ?: return null
        return routeMap.pathFor(destination.routeKey)
    }

    /**
     * Resolves [destination] or throws when it is not a valid screen
     * destination for this app graph.
     */
    public fun requireScreenRoute(destination: LaydrScreenDestination): LaydrRoute =
        screenRouteFor(destination)
            ?: throw IllegalArgumentException(
                "Destination is not a valid Laydr screen route in this app graph: ${destination.routeKey}",
            )
}

/**
 * One UI-neutral route section declared by app code from generated route refs.
 *
 * Laydr sections expose structural membership only. Apps own labels, icons,
 * resources, visibility, tab ordering, and rendering.
 */
public class LaydrRouteSection internal constructor(
    /**
     * Generated screen route ref that acts as this section's root.
     */
    public val route: LaydrScreenRouteRef,
    /**
     * Key for the concrete section root destination.
     */
    public val rootKey: LaydrRouteKey,
    private val routeMap: LaydrRouteMap,
) {
    /**
     * Returns true when [key] belongs to this section's route subtree and
     * matches the root destination parameters.
     */
    public fun matches(key: LaydrRouteKey): Boolean =
        routeMap.contains(route, key) && key.matchesRootScope(rootKey)
}

/**
 * Ordered app-declared route sections for a Laydr route map.
 */
public class LaydrRouteSections internal constructor(
    /**
     * Route map used to validate and match these sections.
     */
    public val routeMap: LaydrRouteMap,
    sections: List<LaydrRouteSection>,
) {
    /**
     * Ordered section entries.
     */
    public val items: List<LaydrRouteSection> = sections.toList()

    /**
     * Returns the first section whose route subtree contains [key], or `null`.
     */
    public fun sectionFor(key: LaydrRouteKey): LaydrRouteSection? =
        items.firstOrNull { section -> section.matches(key) }
}

/**
 * Builder for UI-neutral route sections.
 */
public class LaydrRouteSectionsBuilder internal constructor(
    private val routeMap: LaydrRouteMap,
) {
    private val sections = mutableListOf<LaydrRouteSection>()

    /**
     * Adds [route] as a section root.
     *
     * The route must be a generated screen route in [routeMap] and must not
     * require dynamic parameters.
     */
    public fun section(route: LaydrScreenRouteRef) {
        require(route.route.parameterNames.isEmpty()) {
            "Laydr route section root must not require dynamic parameters: ${route.route.id}"
        }
        section(route = route, rootKey = route.route.key())
    }

    /**
     * Adds [route] as a concrete section root scoped by [rootKey].
     *
     * The route must be a generated screen route in [routeMap]. The [rootKey]
     * must name the same route and contain valid route parameters. Descendant
     * matches belong to this section only when they keep the same root
     * parameter values.
     */
    public fun section(route: LaydrScreenRouteRef, rootKey: LaydrRouteKey) {
        require(routeMap.screenRoutes.contains(route.route)) {
            "Laydr route section root must be a screen route in the route map: ${route.route.id}"
        }
        require(rootKey.routeId == route.route.id) {
            "Laydr route section root key must target ${route.route.id}: ${rootKey.routeId}"
        }
        require(routeMap.pathFor(rootKey) != null) {
            "Laydr route section root key must contain valid parameters: $rootKey"
        }
        sections += LaydrRouteSection(route = route, rootKey = rootKey, routeMap = routeMap)
    }

    internal fun build(): LaydrRouteSections =
        LaydrRouteSections(routeMap = routeMap, sections = sections)
}

private fun LaydrRouteKey.matchesRootScope(rootKey: LaydrRouteKey): Boolean =
    rootKey.parameters.all { (name, value) -> parameters[name] == value }

/**
 * Builds ordered UI-neutral route sections for [routeMap].
 */
public fun laydrRouteSections(
    routeMap: LaydrRouteMap,
    block: LaydrRouteSectionsBuilder.() -> Unit,
): LaydrRouteSections =
    LaydrRouteSectionsBuilder(routeMap)
        .apply(block)
        .build()

/**
 * Static metadata attached to a Laydr route declaration.
 *
 * Metadata is framework-neutral and app-owned. Laydr exposes it for
 * diagnostics, inspection, and app rendering decisions, but it does not assign
 * behavior to labels or use metadata for routing policy.
 *
 * @param name readable route name suitable for diagnostics and app-owned
 * display decisions.
 * @param labels opaque app-owned key/value labels with no Laydr semantics.
 */
public class LaydrRouteMetadata public constructor(
    /**
     * Readable route name suitable for diagnostics and app-owned display
     * decisions.
     */
    public val name: String,
    labels: Map<String, String> = emptyMap(),
) {
    /**
     * Opaque app-owned key/value labels with no Laydr semantics.
     */
    public val labels: Map<String, String> = labels.toMap()

    init {
        require(name.isNotBlank()) {
            "Route metadata name must not be blank"
        }
        val blankLabelKey = this.labels.keys.firstOrNull { it.isBlank() }
        require(blankLabelKey == null) {
            "Route metadata label keys must not be blank"
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LaydrRouteMetadata &&
            name == other.name &&
            labels == other.labels

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + labels.hashCode()
        return result
    }

    override fun toString(): String =
        "LaydrRouteMetadata(name=$name, labels=$labels)"
}

/**
 * One segment in a Laydr route path template.
 */
public sealed interface LaydrRouteSegment {
    /**
     * Segment text as it appears in a path template.
     */
    public val templatePart: String

    /**
     * Static URI path segment.
     */
    public data class Static(
        /**
         * Literal segment value.
         */
        public val value: String,
    ) : LaydrRouteSegment {
        init {
            require(unreservedPathSegmentRegex.matches(value)) {
                "Static route segment must contain only unreserved URI path characters: $value"
            }
        }

        override val templatePart: String = value
    }

    /**
     * Dynamic URI path segment captured by [parameterName].
     */
    public data class Dynamic(
        /**
         * Lowercase snake case parameter name.
         */
        public val parameterName: String,
    ) : LaydrRouteSegment {
        init {
            require(parameterNameRegex.matches(parameterName)) {
                "Dynamic route parameter must be lowercase snake_case: $parameterName"
            }
        }

        override val templatePart: String = "{$parameterName}"
    }
}

/**
 * Successful match between a concrete path and a [LaydrRoute].
 */
public class LaydrRouteMatch public constructor(
    /**
     * Route whose template matched.
     */
    public val route: LaydrRoute,
    parameters: Map<String, String>,
) {
    /**
     * Decoded dynamic parameter values captured from the path.
     */
    public val parameters: Map<String, String> = parameters.toMap()

    override fun equals(other: Any?): Boolean =
        this === other ||
            other is LaydrRouteMatch &&
            route == other.route &&
            parameters == other.parameters

    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + parameters.hashCode()
        return result
    }

    override fun toString(): String =
        "LaydrRouteMatch(route=$route, parameters=$parameters)"
}

private val unreservedPathSegmentRegex = Regex("[A-Za-z0-9._~-]+")
private val laydrRouteIdRegex = Regex("[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*")
private val parameterNameRegex = Regex("[a-z][a-z0-9]*(?:_[a-z0-9]+)*")

private fun LaydrRoute.findRouteById(routeId: String): LaydrRoute? {
    if (id == routeId) {
        return this
    }

    return children.firstNotNullOfOrNull { child ->
        child.findRouteById(routeId)
    }
}

private fun List<LaydrRoute>.flattenRoutes(): List<LaydrRoute> =
    flatMap { route ->
        listOf(route) + route.children.flattenRoutes()
    }

private fun List<LaydrRoute>.sortedForMatching(): List<LaydrRoute> =
    withIndex()
        .sortedWith { left, right ->
            val specificity = compareRouteMatchSpecificity(left.value, right.value)
            if (specificity != 0) {
                specificity
            } else {
                left.index.compareTo(right.index)
            }
        }
        .map { indexedRoute -> indexedRoute.value }

private fun compareRouteMatchSpecificity(
    left: LaydrRoute,
    right: LaydrRoute,
): Int {
    val maxSegmentCount = maxOf(left.segments.size, right.segments.size)
    for (index in 0 until maxSegmentCount) {
        val leftPriority = left.segments.getOrNull(index).matchPriority
        val rightPriority = right.segments.getOrNull(index).matchPriority
        if (leftPriority != rightPriority) {
            return rightPriority - leftPriority
        }
    }
    return 0
}

private val LaydrRouteSegment?.matchPriority: Int
    get() = when (this) {
        is LaydrRouteSegment.Static -> 2
        is LaydrRouteSegment.Dynamic -> 1
        null -> 0
    }

private fun LaydrRoute.routeChainTo(target: LaydrRoute): List<LaydrRoute>? {
    if (this == target) {
        return listOf(this)
    }

    return children.firstNotNullOfOrNull { child ->
        child.routeChainTo(target)?.let { childChain -> listOf(this) + childChain }
    }
}

private fun splitPath(path: String): List<String>? {
    if (!path.startsWith("/") || path.contains("?") || path.contains("#")) {
        return null
    }
    if (path == "/") {
        return emptyList()
    }
    if (path.endsWith("/")) {
        return null
    }

    return path
        .removePrefix("/")
        .split("/")
        .takeIf { segments -> segments.none { it.isEmpty() } }
}

private fun encodePathSegment(value: String): String =
    buildString {
        for (byte in value.encodeToByteArray()) {
            val unsignedByte = byte.toInt() and 0xFF
            if (unsignedByte.isUnreserved()) {
                append(unsignedByte.toChar())
            } else {
                append('%')
                append(hexDigits[unsignedByte shr 4])
                append(hexDigits[unsignedByte and 0x0F])
            }
        }
    }

private fun decodePathSegment(value: String): String? {
    val bytes = mutableListOf<Byte>()
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char == '%') {
            if (index + 2 >= value.length) {
                return null
            }
            val high = value[index + 1].hexValue() ?: return null
            val low = value[index + 2].hexValue() ?: return null
            bytes += ((high shl 4) or low).toByte()
            index += 3
        } else {
            if (char.code > 0x7F) {
                return null
            }
            bytes += char.code.toByte()
            index += 1
        }
    }

    return decodeUtf8(bytes.toByteArray())
}

private fun decodeUtf8(bytes: ByteArray): String? =
    buildString {
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index].unsigned()
            when {
                first <= 0x7F -> {
                    append(first.toChar())
                    index += 1
                }
                first in 0xC2..0xDF -> {
                    if (index + 1 >= bytes.size) {
                        return null
                    }
                    val second = bytes[index + 1].unsigned()
                    if (!second.isUtf8Continuation()) {
                        return null
                    }
                    append((((first and 0x1F) shl 6) or (second and 0x3F)).toChar())
                    index += 2
                }
                first in 0xE0..0xEF -> {
                    if (index + 2 >= bytes.size) {
                        return null
                    }
                    val second = bytes[index + 1].unsigned()
                    val third = bytes[index + 2].unsigned()
                    if (!second.isUtf8Continuation() || !third.isUtf8Continuation()) {
                        return null
                    }
                    if (first == 0xE0 && second < 0xA0) {
                        return null
                    }
                    if (first == 0xED && second >= 0xA0) {
                        return null
                    }
                    append(
                        (((first and 0x0F) shl 12) or
                            ((second and 0x3F) shl 6) or
                            (third and 0x3F)).toChar(),
                    )
                    index += 3
                }
                first in 0xF0..0xF4 -> {
                    if (index + 3 >= bytes.size) {
                        return null
                    }
                    val second = bytes[index + 1].unsigned()
                    val third = bytes[index + 2].unsigned()
                    val fourth = bytes[index + 3].unsigned()
                    if (
                        !second.isUtf8Continuation() ||
                        !third.isUtf8Continuation() ||
                        !fourth.isUtf8Continuation()
                    ) {
                        return null
                    }
                    if (first == 0xF0 && second < 0x90) {
                        return null
                    }
                    if (first == 0xF4 && second > 0x8F) {
                        return null
                    }
                    appendCodePoint(
                        ((first and 0x07) shl 18) or
                            ((second and 0x3F) shl 12) or
                            ((third and 0x3F) shl 6) or
                            (fourth and 0x3F),
                    )
                    index += 4
                }
                else -> return null
            }
        }
    }

private fun Byte.unsigned(): Int =
    toInt() and 0xFF

private fun Int.isUtf8Continuation(): Boolean =
    this in 0x80..0xBF

private fun StringBuilder.appendCodePoint(codePoint: Int) {
    val surrogateValue = codePoint - 0x10000
    append(((surrogateValue shr 10) + 0xD800).toChar())
    append(((surrogateValue and 0x03FF) + 0xDC00).toChar())
}

private fun Int.isUnreserved(): Boolean =
    this in 'A'.code..'Z'.code ||
        this in 'a'.code..'z'.code ||
        this in '0'.code..'9'.code ||
        this == '-'.code ||
        this == '.'.code ||
        this == '_'.code ||
        this == '~'.code

private fun Char.hexValue(): Int? =
    when (this) {
        in '0'..'9' -> this - '0'
        in 'A'..'F' -> this - 'A' + 10
        in 'a'..'f' -> this - 'a' + 10
        else -> null
    }

private const val hexDigits = "0123456789ABCDEF"
