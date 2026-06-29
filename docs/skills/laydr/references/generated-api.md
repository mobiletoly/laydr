# Generated API

Use this before naming generated Laydr APIs in app code.

## Contents

- generated source locations
- task map
- routes, paths, and destinations
- Compose definitions
- generated Nav3 helpers
- graph and map

Generated source lives under:

```text
build/generated/laydr/commonMain/kotlin/  # KMP
build/generated/laydr/main/kotlin/        # Android-only
```

Never edit generated files. Change `routes/` source and rerun validation or
generation.

## Task Map

| Need | Use |
| --- | --- |
| Navigate inside the app | `LaydrRoutes.*.destination(...)` |
| Store or compare a path string | `LaydrRoutes.*.path(...)` |
| Render current path with Compose | `LaydrComposeRoutes.definitions` |
| Declare route-local content | route-local `LaydrRouteDef` |
| Build Nav3 sections or stacks | generated `LaydrNavRoutes` helpers |
| Diagnose adapter behavior | `LaydrRoutes.appGraph` or `routeMap` |

## Routes, Paths, And Destinations

Import app-level generated APIs from the configured generated package:

```kotlin
import example.app.generated.LaydrComposeRoutes
import example.app.generated.LaydrNavRoutes
import example.app.generated.LaydrRoutes
```

Nested route objects mirror directories:

```text
routes/contacts/by_id -> LaydrRoutes.Contacts.ById
```

Prefer destinations for in-app navigation:

```kotlin
LaydrRoutes.Contacts.ById.destination(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

Use paths only when the app deliberately stores or handles path strings:

```kotlin
LaydrRoutes.Contacts.ById.path(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

Dynamic values are wrapped by route-scoped factories. Use `.value` only when
passing back into app-owned APIs that expect plain strings.

## Compose Definitions

When `compose.set(true)` is enabled:

- `LaydrComposeRoutes.definitions` is generated in the generated package.
- each declared route package receives `LaydrRouteDef`.
- generated definitions are app-context-free.

Use definitions with `LaydrRouteHost`:

```kotlin
LaydrRouteHost(
    currentPath = currentPath,
    routeDefinitions = LaydrComposeRoutes.definitions,
    notFoundContent = { path -> NotFound(path) },
)
```

Nav3 entry providers use the same definitions internally.

## Generated Nav3 Helpers

Enable exactly one Nav3 helper target:

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

This generates an app-module `LaydrNavRoutes` object. Use it for repeated
section and stack setup:

```kotlin
val sections = LaydrNavRoutes.rememberSections(
    sectionSpecs = listOf(
        LaydrNavRoutes.Contacts.section(TabSpec("Contacts")),
        LaydrNavRoutes.Profile.section(TabSpec("Profile")),
    ),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

Single-stack apps use:

```kotlin
val stack = LaydrNavRoutes.rememberStack(
    initialDestination = LaydrRoutes.Home.destination(),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

Mixed parent stacks use the `backStack = ...` overload:

```kotlin
val rootStack = LaydrNavRoutes.rememberStack(
    backStack = rootBackStack,
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

The app-owned stack type comes from the enabled adapter. AndroidX helpers
expect AndroidX `NavBackStack<NavKey>`. KMP helpers expect the KMP Nav3
`NavBackStack<NavKey>`. Do not use `SnapshotStateList<NavKey>` for AndroidX
generated stack helpers.

Layout-only routes do not generate `destination()`. Dynamic section roots pass
an explicit root destination:

```kotlin
LaydrNavRoutes.Workspaces.ById.section(
    rootDestination = LaydrRoutes.Workspaces.ById.destination(
        id = LaydrRoutes.Workspaces.ById.id("alpha"),
    ),
    sectionData = TabSpec("Alpha"),
)
```

`LaydrNavRoutes` does not generate route-specific navigator extensions,
payload types, or result types. Keep app-specific navigation intent in app
code.

## Graph And Map

Use `LaydrRoutes.appGraph` for adapter validation and advanced shell code.
Use `LaydrRoutes.routeMap` for lower-level path matching, diagnostics, strict
entry-point handling, or adapter work.

Generated route objects expose typed checks:

```kotlin
LaydrRoutes.Profile.matches(key)
LaydrRoutes.Contacts.contains(key)
```

Use generated destinations for ordinary buttons and menu actions instead of
raw route ids or hand-built path strings.
