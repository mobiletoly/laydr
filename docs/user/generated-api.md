# Generated API Tour

Use this page when a route exists, but you are unsure which generated object
to call.

Generated files live under:

```text
build/generated/laydr/commonMain/kotlin/  # KMP
build/generated/laydr/main/kotlin/        # Android-only
```

Do not edit generated files. Change `routes/` source, then run
`checkLaydrRoutes` or `generateLaydrRoutes`.

## Task Cheat Sheet

| You want to | Use | Example |
| --- | --- | --- |
| Open a screen in app navigation | generated `destination(...)` | `LaydrRoutes.Contacts.destination()` |
| Open a dynamic detail screen | generated parameter factory plus `destination(...)` | `LaydrRoutes.Contacts.ById.destination(id = LaydrRoutes.Contacts.ById.id("ada"))` |
| Store or compare a path string | generated `path(...)` | `LaydrRoutes.Contacts.path()` |
| Render the current path with Compose | `LaydrComposeRoutes.definitions` | `LaydrRouteHost(routeDefinitions = LaydrComposeRoutes.definitions, ...)` |
| Declare route-local content | route-local `LaydrRouteDef` | `internal val Route = LaydrRouteDef.screen { route -> Screen(route = route) }` |
| Build Nav3 stacks or sections | generated `LaydrNavRoutes` helpers | `LaydrNavRoutes.rememberSections(...)` |
| Diagnose route graph behavior | `appGraph` or `routeMap` | advanced adapter or diagnostics code |

For ordinary in-app navigation, start with generated `destination(...)`
values. Use `path(...)` only when the app deliberately stores or handles path
strings.

## Imports

Most app-level generated APIs come from your configured generated package:

```kotlin
import example.app.generated.LaydrComposeRoutes
import example.app.generated.LaydrNavRoutes
import example.app.generated.LaydrRoutes
```

Route-local `LaydrRouteDef` is generated into each route package, so
`Route.kt` normally uses it without imports.

## `LaydrRoutes`

`LaydrRoutes` is the generated route tree:

```kotlin
LaydrRoutes.Contacts
LaydrRoutes.Contacts.ById
```

Nested objects mirror directories. `routes/contacts/by_id` becomes
`LaydrRoutes.Contacts.ById`.

## Open Screens With `destination(...)`

Screen routes expose typed destination factories:

```kotlin
LaydrRoutes.Contacts.destination()
LaydrRoutes.Contacts.ById.destination(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

Use destinations for normal in-app navigation through `laydr-nav3-kmp`,
`laydr-nav3-androidx`, or app-owned navigation helpers.

Dynamic routes expose route-scoped parameter factories such as `id("ada")`.
Prefer the factory call over depending on generated destination class names.
Use `.value` only when passing a parameter back to app-owned string APIs.

## Build Paths With `path(...)`

Screen routes expose path builders:

```kotlin
LaydrRoutes.Contacts.path()
LaydrRoutes.Contacts.ById.path(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

Use `path(...)` when your app deliberately stores a path string, such as a
plain `LaydrRouteHost` app, browser integration, or strict external target
handling.

## Render Current Path With `LaydrComposeRoutes`

When `compose.set(true)` is enabled, Laydr generates Compose route
definitions:

```kotlin
LaydrRouteHost(
    currentPath = currentPath,
    routeDefinitions = LaydrComposeRoutes.definitions,
    notFoundContent = { path -> NotFound(path) },
)
```

Nav3 entry providers use the same definitions internally.

## Use Nav3 Helpers With `LaydrNavRoutes`

When Compose generation is enabled, one generated Nav3 helper target can be
enabled:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

or:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
```

Both targets generate `LaydrNavRoutes` for that route tree. Enable only one.

`LaydrNavRoutes` exposes generated helpers for section specs, single-stack
setup, mixed parent-stack setup, and dynamic section roots. Read
[Navigation](navigation/README.md) for when to use each shape.

Navigator calls still use generated destinations:

```kotlin
navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
navigator.replace(LaydrRoutes.Contacts.destination())
navigator.select(LaydrRoutes.Contacts)
```

`LaydrNavRoutes` intentionally does not generate route-specific navigator
extensions. Keep app-specific navigation intent in your app code.

## Declare Content With `LaydrRouteDef`

Each declared route package receives a generated `LaydrRouteDef` helper:

```kotlin
internal val Route = LaydrRouteDef.screen { route ->
    Screen(route = route)
}

@Composable
internal fun Screen(route: LaydrRoutes.Products.Destination) {
    ProductsScreen(route.path)
}
```

Route-local helpers include:

- `screen`
- `screenWithLayoutValues`
- `layout`
- `screenAndLayout`

Use `screenWithLayoutValues` only when the screen returns
`LaydrScreenContent` with layout values for inherited layouts.

## Advanced And Diagnostics

`LaydrRoutes.appGraph` is the app-level route contract used by runtime
adapters. Nav3 adapters validate destinations and section ownership through
this graph before mutating stacks.

Most app code does not call `appGraph` directly when it uses generated
`LaydrNavRoutes` helpers.

`LaydrRoutes.routeMap` is lower-level structural routing data. It backs:

- path matching
- route key conversion
- destination conversion
- inherited layout chains
- top-level route classification
- section membership

Use it for diagnostics, strict app entry-point handling, adapter code, or
advanced routing work. Do not use it for ordinary button navigation when a
generated destination exists.

Generated route objects also expose typed checks:

```kotlin
LaydrRoutes.Boot.matches(key)
LaydrRoutes.Main.contains(key)
```

Use `matches` for exact route checks and `contains` for route-subtree checks.
