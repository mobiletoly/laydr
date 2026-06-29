# Routes

Use this when editing the filesystem route tree or fixing scanner validation.

## Contents

- route roots
- route kinds
- names and dynamic segments
- namespace-only segments
- metadata
- validation

## Route Roots

```text
src/commonMain/kotlin/routes/  # KMP
src/main/kotlin/routes/        # Android-only
```

Each directory under the route root is a path segment. A directory becomes a
declared route only when it contains `Route.kt`.

## Route Kinds

Compose-enabled routes use generated route-local helpers:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
internal val Route = LaydrRouteDef.screenWithLayoutValues(content = ::Screen)
internal val Route = LaydrRouteDef.layout(content = ::Layout)
internal val Route = LaydrRouteDef.screenAndLayout {
    screen(content = ::Screen)
    layout(content = ::Layout)
}
```

Use:

- `screen` for a navigation target.
- `layout` for inherited rendering around declared descendants.
- `screenAndLayout` when one route is both a target and an inherited layout.
- `screenWithLayoutValues` when a screen returns `LaydrScreenContent` with
  app-owned rendering values for inherited layouts.

Layout-only routes do not generate `destination()` and must have a declared
descendant. Core-only graphs use `LaydrRouteDeclaration.screen`,
`LaydrRouteDeclaration.layout`, or `LaydrRouteDeclaration.screenAndLayout`.

## Names And Dynamic Segments

Directory names are lowercase snake case.

Static underscores become hyphens in paths:

```text
routes/user_profile -> /user-profile -> LaydrRoutes.UserProfile
```

Dynamic segments start with `by_`:

```text
routes/users/by_user_id -> /users/{user_id}
```

Dynamic parameter factories are route-scoped and lower camel case:

```kotlin
LaydrRoutes.Users.ByUserId.destination(
    userId = LaydrRoutes.Users.ByUserId.userId("ada"),
)
```

## Namespace-Only Segments

A directory without `Route.kt` can group child route segments:

```text
routes/
  catalog/
    bundle/
      by_activity_bundle_id/
        Route.kt
        Screen.kt
```

Here `catalog` and `catalog/bundle` contribute path and generated object
nesting, but they are not routes. Namespace-only directories must not contain
direct Kotlin files.

## Metadata

Route metadata is static descriptor data:

```kotlin
internal val Route = LaydrRouteDef.screen(
    name = "Product detail",
    labels = mapOf("area" to "catalog"),
    content = ::ProductDetailScreen,
)
```

Do not treat metadata as auth policy, section chrome, route matching behavior,
state, icons, or labels rendered by the shell.

## Validation

Run the route check for the module that owns routes:

```bash
./gradlew :shared:checkLaydrRoutes
```

Android-only modules commonly use:

```bash
./gradlew :app:checkLaydrRoutes
```

Common scanner failures: missing route root, invalid directory name, bad
dynamic segment, multiple route declarations in `Route.kt`, Kotlin files in a
namespace-only directory, duplicate generated names, or layout-only leaf
routes. Fix authored route source, not generated output.
