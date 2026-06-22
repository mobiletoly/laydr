# Generated API

Use this reference before naming generated Laydr APIs in downstream app code.

Generated files live under:

```text
build/generated/laydr/commonMain/kotlin/
build/generated/laydr/main/kotlin/
```

Do not edit generated files. Change route source and rerun route validation or
generation.

KMP modules use `commonMain`; Android-only modules use `main`.

## Root Object

`LaydrRoutes` lives in the configured generated package:

```kotlin
import example.app.generated.LaydrRoutes
```

Nested objects mirror the route tree:

```kotlin
LaydrRoutes.Contacts
LaydrRoutes.Contacts.ById
```

## Paths And Destinations

Use `path(...)` when the app intentionally stores path strings:

```kotlin
LaydrRoutes.Contacts.path()
LaydrRoutes.Contacts.ById.path(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

Use `destination(...)` for in-app navigation and adapters:

```kotlin
LaydrRoutes.Contacts.destination()
LaydrRoutes.Contacts.ById.destination(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

Dynamic directory names become lower camel Kotlin arguments:

```text
routes/users/by_user_id -> destination(
    userId = LaydrRoutes.Users.ByUserId.userId("ada"),
)
```

Prefer factory calls over depending on generated destination class names.

## Graph And Map

Use `LaydrRoutes.appGraph` for adapters:

```kotlin
rememberLaydrNavSections(
    LaydrRoutes.appGraph,
    laydrNavSection(LaydrRoutes.Contacts, TabSpec("Contacts")),
)
```

Use `LaydrRoutes.routeMap` only for lower-level route matching, diagnostics,
strict app entry points, or adapter code.

## Compose Generated APIs

When `compose.set(true)` is enabled:

- `LaydrComposeRoutes.definitions` is generated in the generated package.
- route packages receive `LaydrRouteDef`.
- generated definitions are app-context-free.
- `LaydrRouteDef.screen` accepts content-only composables returning `Unit`.
- `LaydrRouteDef.screenWithLayoutValues` is for screens returning
  `LaydrScreenContent` with layout values.

Use the generated definitions for both `LaydrRouteHost` and Nav3 entry
providers.

## Nav3 Generated Helpers

Apps can opt into generated helpers for one Nav3 adapter:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

This generates `LaydrNavRoutes` in the generated package. It is internal and
wraps existing adapter primitives; it does not add dependencies or app-owned
chrome.

For Android-only AndroidX Navigation 3 apps:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
```

Do not enable `nav3Kmp` and `nav3Androidx` for the same route tree.

Use it in app shells:

```kotlin
val wiring = LaydrNavRoutes.rememberSections(
    sectionSpecs = listOf(
        LaydrNavRoutes.Contacts.section(TabSpec("Contacts")),
        LaydrNavRoutes.Profile.section(TabSpec("Profile")),
    ),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

Use `rememberStack` for one-stack apps:

```kotlin
val stack = LaydrNavRoutes.rememberStack(
    initialDestination = LaydrRoutes.Home.destination(),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

`rememberStack` returns `LaydrNavStack`. It also has a `backStack = ...`
overload for app-owned or mixed parent stacks:

```kotlin
val rootStack = LaydrNavRoutes.rememberStack(
    backStack = rootBackStack,
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

The generated API does not declare route-specific payload or result types, and
it does not generate route-specific payload or result helpers.

The initial destination must be a generated screen destination. Layout-only
routes do not generate `destination()`.

Dynamic section roots use explicit root destinations:

```kotlin
LaydrNavRoutes.Workspaces.ById.section(
    rootDestination = LaydrRoutes.Workspaces.ById.destination(
        id = LaydrRoutes.Workspaces.ById.id("alpha"),
    ),
    sectionData = TabSpec("Workspace"),
)
```

Route-local screens still pass generated destinations to runtime navigator
APIs:

```kotlin
navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
navigator.pushWithReturn(LaydrRoutes.Profile.destination())
navigator.replace(LaydrRoutes.Contacts.destination())
navigator.select(LaydrRoutes.Contacts)
```

`LaydrNavRoutes` does not generate route-specific navigator extensions.
Generated route objects expose `matches(key)` and `contains(key)` for typed
route classification without raw route-id comparisons.
