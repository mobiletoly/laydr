# Generated Route Graph

`laydr-codegen` owns generated Kotlin source emission. The generated route
graph is pure Kotlin source that can be added to Kotlin Multiplatform
`commonMain`.

## Current Output

By default, the generator renders only the core route graph. The core file is
named `LaydrRoutes.kt` and has a fixed root object named `LaydrRoutes`. The
generated package name is supplied by the caller. Route object names are
derived from segment directory source names in PascalCase. Static directory
underscores become hyphens only in generated URL path segments, so
`routes/user_profile` produces `LaydrRoutes.UserProfile` and `/user-profile`.

The root object exposes root declared route descriptors and flattened screen and
layout subsets, plus a framework-neutral route map and app graph. A declared
route below root namespace-only segment directories becomes a root descriptor
while keeping its full generated object path.

```kotlin
public val routes: List<LaydrRoute>
public val screenRoutes: List<LaydrRoute>
public val layoutRoutes: List<LaydrRoute>
public val routeMap: LaydrRouteMap
public val appGraph: LaydrAppGraph
```

The app-facing navigation surface is generated from screen route objects:
apps build typed destinations with `destination(...)` and pass those values
through app or adapter APIs. The route map and app graph are generated support
surfaces that keep adapters from rediscovering the route tree.

`LaydrRouteMap` centralizes route-tree lookup, path/key conversion, inherited
layout-chain lookup, top-level route classification, and UI-neutral section
membership. Adapters should consume the route map instead of asking apps to
pass parallel route lists.

Route classification should use generated predicates instead of raw route id
comparisons. `LaydrRoutes.X.matches(key)` checks exact route identity and
valid route parameters. `LaydrRoutes.X.contains(key)` checks valid route-map
membership in a route subtree.

`LaydrAppGraph` wraps the generated `routeMap` as the app-level contract for
runtime adapters. It delegates `routes`, `screenRoutes`, and `layoutRoutes` to
the map and validates generated screen destinations before adapters mutate
navigation state. Apps usually pass this object to adapter controllers, such as
`laydr-nav3-kmp`. It does not own rendering, stack policy, labels,
icons, deep links, or platform behavior.

Apps can group generated screen route refs into UI-neutral route sections with
`laydrRouteSections(routeMap)`. `LaydrRouteSections.sectionFor(key)` returns
the first declared section whose subtree contains a key. Static roots can use
the route alone; dynamic roots use an explicit root `LaydrRouteKey`, and
descendant keys must match the root parameter values to belong to that section.
It does not assign labels, icons, visibility, badges, or navigation-bar
behavior; those remain app-owned rendering policy.

Each declared route object exposes its descriptor:

```kotlin
public val route: LaydrRoute
```

Generated descriptors include static route declaration metadata:

```kotlin
LaydrRouteMetadata(name = "Settings Profile")
```

The generated name is a readable path label derived from segment directory
source names. Dynamic segment directories keep their readable `by` prefix, so
`routes/users/by_id` generates the metadata name `Users By Id`.

Screen route objects also expose typed path builders and app-facing
destinations:

```kotlin
public fun path(...): String
public fun destination(...): Destination
```

The builders take the same dynamic parameters. `path(...)` returns the encoded
URI path. `destination(...)` returns the generated typed value application code
should pass around when it wants to navigate to a screen.

Static screen routes generate an object destination:

```kotlin
public object Home : LaydrParameterlessScreenRouteRef {
    public override val defaultDestination: Destination
}

public object Destination : LaydrScreenDestination {
    public override val routeKey: LaydrRouteKey
    public val path: String
}
```

Static screen route objects implement `LaydrParameterlessScreenRouteRef` and
expose `defaultDestination`. Runtime adapters use that default for app-shell
helpers such as Nav3 section specs, so apps do not repeat
`destination()` for parameterless section roots. `Destination` is the default
nested type name. If a direct child route already generates that object name,
Laydr chooses a deterministic fallback such as `RouteDestination` so child
route objects and destination helper types do not collide. The public factory
remains `destination(...)`.

Dynamic screen routes generate a data class destination. For example,
`routes/users/by_id` generates `LaydrRoutes.Users.ById.Destination.id`, plus
`routeKey` and `path`. A semantic directory such as
`routes/products/by_product_id` keeps the route key parameter
`"product_id"` but generates Kotlin APIs such as:

```kotlin
LaydrRoutes.Products.ByProductId.destination(
    productId = LaydrRoutes.Products.ByProductId.productId("..."),
)
```

`routeKey` is the framework-neutral adapter payload; apps should prefer the
destination object until an adapter boundary needs the key. Dynamic route
objects remain `LaydrScreenRouteRef` because there is no parameterless default
destination to expose.

Nested objects mirror the filesystem segment hierarchy. Route descriptor ids use
the generated object path, such as `Home` or `Users.ById`. Parent descriptors
include nearest declared descendant descriptors in `children`; namespace-only
segment directories are skipped in descriptor children.

Namespace-only segment objects expose nested child objects only. They do not
expose `route`, `path(...)`, destinations, defaults, metadata, Compose
definitions, or route-local helper source.

Layout-only route objects expose `route` and nested child objects but do not
expose `path(...)` or destinations, because they are not
screen endpoints.

Dynamic segment directories use scanner parameter names for path templates and
route keys, and lower-camel names for generated Kotlin identifiers. For
example, `routes/users/by_user_id` maps to `/users/{user_id}` and generates
`UserIdParam`, `LaydrRoutes.Users.ByUserId.userId(rawValue: String)`,
`LaydrRoutes.Users.ByUserId.path(userId: UserIdParam)`, and
`LaydrRoutes.Users.ByUserId.destination(userId: UserIdParam)`. These functions
delegate to `LaydrRoute.buildPath` and `LaydrRoute.key` internally with
`"user_id"` as the key, so URI path-segment percent encoding and parameter
validation stay in `laydr-core`.

Route declarations may provide static descriptor metadata:

```kotlin
internal val Route = LaydrRouteDef.screen(
    name = "Product detail",
    content = ::ProductDetailScreen,
)
```

Metadata is static app-owned descriptor data for diagnostics, inspection, and
apps that choose to read it. It is not section chrome, icon policy, auth
policy, route matching behavior, or screen state. Routes may also attach
opaque string `labels` when app code has its own descriptor tags, but Laydr
does not assign UI meaning to those labels. Code generation accepts only
literal metadata arguments and rejects dynamic expressions.

Compose-facing output is explicit opt-in. Gradle consumers enable it with:

```kotlin
laydr {
    compose.set(true)
}
```

When Compose generation is enabled, the generator also emits
`LaydrComposeRoutes.kt` in the configured generated package. It exposes:

```kotlin
internal object LaydrComposeRoutes {
    public val definitions: LaydrComposeRouteDefinitions
}
```

Generated Compose definitions are app-context-free. Apps provide feature
dependencies from ordinary app-owned Compose code rather than through a
generated global app context.

Each declared route package receives a generated `LaydrRouteDef.kt` helper.
App-authored `Route.kt` files use that helper to declare route-owned screen and
layout rendering without naming global generated match types:

```kotlin
internal val Route = LaydrRouteDef.screen { route -> ... }
```

Route helpers also accept direct function references when the target function
uses the generated route-bound signature:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::ProductDetailScreen)
```

The same `Route.kt` declaration is the route-kind source used by the scanner.
Core-only route graph generation can declare kind with
`LaydrRouteDeclaration.screen`, `LaydrRouteDeclaration.layout`, or
`LaydrRouteDeclaration.screenAndLayout` without enabling Compose generation.

For screen routes, the helper gives `route` the generated typed destination
inside a composable render lambda, so route definitions can read path and
dynamic parameters and load Compose state. The simple `screen` helper wraps
content-only composables in `LaydrScreenContent`. Screens that need layout
values use `screenWithLayoutValues`.
For routes that are both screen and layout, `screenAndLayout` returns one
definition containing both render functions. Generated helpers do not import a
route app type or expose app parameters.

## Boundaries

KotlinPoet owns rendering so generated Kotlin stays structured as the output
grows. `laydr-codegen` exposes a small public facade for generating route
graph source text from a routes directory and generated package. The scanner
model and KotlinPoet implementation remain internal.

`laydr-gradle-plugin` owns the user-facing Gradle tasks. The
`generateLaydrRoutes` task writes every enabled generated Laydr source under
`build/generated/laydr/commonMain/kotlin`, and the plugin adds that output
directory to Kotlin Multiplatform `commonMain` when the KMP plugin is applied.
The `checkLaydrRoutes` task validates the configured route tree and all
enabled generated source in memory without writing files, and Gradle `check`
depends on it.
Because Laydr is KMP-first, the Gradle plugin depends on the pinned Kotlin
Gradle Plugin API and uses typed KMP APIs instead of reflection. Consumers
configure the package with `laydr.generatedPackage`.

Generated core route source imports `laydr-core` only. Generated Compose
definition source imports `laydr-compose` and Compose runtime types, and is
emitted only when `compose.set(true)` is configured. Consumers must have those
dependencies available directly or transitively; the Gradle plugin does not
add them automatically.

This layer generates route-definition wiring, but app-owned `Route.kt` files
still call app composables or route-local workflow content directly. It does not
generate app screens, workflow nodes, parse app-authored metadata, query
strings, fragments, redirects, or HTTP method behavior.
