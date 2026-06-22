# Compose Route Host

`laydr-compose` owns the Compose Multiplatform route host over the
framework-neutral descriptors from `laydr-core`.

## Current Contract

The public host is:

```kotlin
@Composable
public fun LaydrRouteHost(
    currentPath: String,
    routeMap: LaydrRouteMap,
    notFoundContent: @Composable (path: String) -> Unit,
    layoutContent: @Composable (
        context: LaydrLayoutContext,
        content: @Composable () -> Unit,
    ) -> Unit = { _, content -> content() },
    screenContent: @Composable (match: LaydrRouteMatch) -> LaydrScreenContent,
): Unit
```

Generated route definitions can also drive the host:

```kotlin
LaydrRouteHost(
    currentPath = currentPath,
    routeDefinitions = LaydrComposeRoutes.definitions,
    notFoundContent = { path -> NotFound(path) },
)
```

The route-definition overload uses app-context-free
`LaydrComposeRouteDefinitions`. Missing screen or layout definitions fail with
explicit errors.

Apps pass an app-owned current path and generated `LaydrRoutes.routeMap`.
The host matches through the route map and renders only screen routes.

Unmatched route behavior is also app-owned. The host requires
`notFoundContent` and calls it with the current path when no route matches.
Apps can render a not-found screen, update their own navigation state outside
the host, or throw from that lambda.

Route-to-screen rendering is route-owned app code. Apps declare render
functions in route-local `Route.kt` files through generated `LaydrRouteDef`
helpers, and generated `LaydrComposeRoutes.definitions` assembles those
declarations. Those helper calls also declare each route's screen, layout, or
screen-and-layout kind for code generation. Apps may still use the lower-level
`screenContent` lambda when they need a custom host primitive. Static route
declaration metadata is available through the matched descriptor as
`match.route.metadata`.

Route-local helpers receive generated route destinations or layout context:

```kotlin
internal val Route = LaydrRouteDef.screen { route -> ... }
```

When the screen function naturally accepts the route-bound signature, apps can
bind it directly:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::ProductDetailScreen)

@Composable
internal fun ProductDetailScreen(
    route: LaydrRoutes.Products.ByProductId.Destination,
): LaydrScreenContent = ...
```

Inline lambdas remain the right choice when app code wants a different local
function shape. Screen render declarations are composable before they return
`LaydrScreenContent`. The generated `route` value is that screen's typed
destination, with `path` and dynamic parameter properties available to
route-owned code. This lets a screen load Compose state, derive screen-owned
`LaydrLayoutValues` from that state, and then return the content body that
inherited layouts will wrap.

Route-to-layout rendering is also route-owned app code. For a matched screen
route, the host asks `LaydrRouteMap` for the inherited layout chain and wraps
the screen content from outermost to innermost.

Matched screens can provide runtime state to inherited layouts through
`LaydrLayoutValues`. Apps define typed `LaydrLayoutKey<T>` values, return them
from `LaydrScreenContent`, and read them from `LaydrLayoutContext` in
`layoutContent`. Key lookup uses key object identity, not the key name; two
keys with the same name are distinct. Missing values read as `null`.

## Boundaries

`laydr-compose` depends on `laydr-core` and Compose runtime. It owns route
definition contracts and route hosting, but it does not own navigation state,
route metadata policy, query strings, fragments, redirects, or
platform-specific navigation integration. Layout values are app-owned
rendering data and have no Laydr routing semantics.

`laydr-core` remains Compose-free. `laydr-codegen` generates route descriptors,
route subset lists, the route map, path builders, typed screen destinations,
and, when Compose generation is enabled, route-local Compose helpers and
aggregate Compose route definitions.
