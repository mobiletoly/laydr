# Concepts

Laydr turns a visible filesystem route tree into typed Kotlin route APIs.

The short version:

1. You create route directories.
2. Directories with `Route.kt` become screen, layout, or screen-and-layout
   routes.
3. The Gradle plugin validates the tree and generates Kotlin source.
4. Your app renders generated route definitions through plain Compose, Nav3
   KMP, or AndroidX Nav3.
5. Your app still owns UI, data, state, shell, and platform policy.

## The Route Tree Is The Map

KMP apps normally keep routes here:

```text
src/commonMain/kotlin/routes/
```

Android-only apps normally keep routes here:

```text
src/main/kotlin/routes/
```

A route tree might look like this:

```text
routes/
  contacts/
    Route.kt
    Screen.kt
    by_id/
      Route.kt
      Screen.kt
  catalog/
    bundle/
      by_activity_bundle_id/
        Route.kt
        Screen.kt
```

The filesystem is part of the product model:

- `routes/contacts` becomes `/contacts`.
- `routes/contacts/by_id` becomes `/contacts/{id}`.
- `routes/catalog/bundle` contributes path and generated object nesting, but
  it is not a route because it has no `Route.kt`.
- Static underscores become hyphens in paths.
- Dynamic segment directories start with `by_`.

## `Route.kt` Declares Behavior

A directory is declared only when it contains `Route.kt`.

Compose-enabled routes use generated `LaydrRouteDef` helpers:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
```

Route kinds:

- `screen`: renderable navigation target.
- `layout`: inherited layout wrapper for child screen routes.
- `screenAndLayout`: a screen that also wraps descendants.
- `screenWithLayoutValues`: a screen that sends app-owned rendering values to
  inherited layouts.

Laydr does not discover routes from annotations, runtime scanners, file names
such as `Screen.kt`, or hidden registration.

## Generated Destinations Are The Default

Screen routes generate typed destinations:

```kotlin
LaydrRoutes.Contacts.ById.destination(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

Use generated destinations for normal in-app navigation through Nav3 KMP,
AndroidX Nav3, or app-owned navigation helpers.

Use generated paths only when the app deliberately stores path strings:

```kotlin
LaydrRoutes.Contacts.ById.path(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

Dynamic parameters are route-scoped value classes. Pass `.value` only when
crossing back into app-owned APIs that expect strings.

## Generated Source Is Build Output

Generated source lives under:

```text
build/generated/laydr/commonMain/kotlin/  # KMP
build/generated/laydr/main/kotlin/        # Android-only
```

Never edit generated files by hand. Change `routes/` source and rerun
`checkLaydrRoutes` or `generateLaydrRoutes`.

## Runtime Choices

Use `LaydrRouteHost` when the app owns path state directly:

```kotlin
LaydrRouteHost(
    currentPath = currentPath,
    routeDefinitions = LaydrComposeRoutes.definitions,
    notFoundContent = { path -> NotFound(path) },
)
```

Use `laydr-nav3-kmp` when a KMP app wants Nav3 stacks, sections, app Back,
optional adaptive scenes, external target handling, or `NavDisplay`.

Use `laydr-nav3-androidx` when an Android-only app wants Google AndroidX
Navigation 3.

All three paths use the same route-local `Route.kt` declarations.

## App-Owned Policy

Laydr owns deterministic route wiring. Your app owns:

- Compose UI and design
- navigation chrome, labels, icons, ordering, visibility, and breakpoints
- repositories, services, DI, ViewModels, reducers, and state holders
- auth, permissions, analytics, persistence, and platform lifecycle policy
- browser history, Android intents, iOS universal links, query parsing, and
  fragment routing

Keep route behavior visible in `routes/`, but keep product decisions in app
code.

## Common Mistakes

- Do not navigate with raw route ids when a generated `destination(...)`
  exists.
- Do not put Kotlin files in a namespace-only segment directory.
- Do not use `layout` for a route that should be pushed onto a stack.
- Do not create one root app context that holds every feature dependency.
- Do not treat payloads or route results as retained state.
- Do not edit generated source to fix a route problem.
