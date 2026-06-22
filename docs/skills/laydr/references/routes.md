# Routes

Use this reference for route tree edits, route declaration validation, and
generated destination APIs.

## Filesystem Route Tree

Routes live under a configured route root, usually:

```text
src/commonMain/kotlin/routes/
```

Each route directory owns `Route.kt`. Nearby `Screen.kt`, `Layout.kt`,
`Workflow.kt`, and helper files are app-owned implementation files.

## Route Kinds

Compose-enabled route declarations use generated helpers:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
internal val Route = LaydrRouteDef.screenWithLayoutValues(content = ::Screen)
internal val Route = LaydrRouteDef.layout(content = ::Layout)
internal val Route = LaydrRouteDef.screenAndLayout {
    screen(content = ::Screen)
    layout(content = ::Layout)
}
```

Core-only route graphs use `LaydrRouteDeclaration.screen`,
`LaydrRouteDeclaration.layout`, or
`LaydrRouteDeclaration.screenAndLayout`.

Layout-only routes must have at least one child and do not generate
destinations.

Use `screen` for a route that is a navigation target, even when the directory
also has child routes. Use `screenAndLayout` only when that route should also
wrap descendant content with inherited Laydr layout behavior.

## Names And Dynamic Segments

Directory names must be lowercase snake case.

Static underscores become hyphens in path segments:

```text
routes/user_profile -> /user-profile -> LaydrRoutes.UserProfile
```

Dynamic directories use `by_<name>`:

```text
routes/users/by_user_id -> /users/{user_id}
```

Generated Kotlin APIs use lower camel arguments:

```kotlin
LaydrRoutes.Users.ByUserId.destination(
    userId = LaydrRoutes.Users.ByUserId.userId("ada"),
)
```

## Generated Destinations

Prefer generated destinations for app navigation:

```kotlin
LaydrRoutes.Contacts.destination()
LaydrRoutes.Contacts.ById.destination(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

The destination contains `routeKey`, `path`, and dynamic parameter values.
Use route keys only at adapter boundaries or for lower-level route-map work.
Read `generated-api.md` for the broader generated API map.

## Metadata

Route metadata must be static literal data:

```kotlin
internal val Route = LaydrRouteDef.screen(
    name = "Product detail",
    labels = mapOf("area" to "catalog"),
    content = ::ProductDetailScreen,
)
```

Do not treat metadata as auth policy, section chrome, route matching behavior,
or screen state.

## Validation

Run:

```bash
./gradlew :shared:checkLaydrRoutes
```

Common failures include missing `Route.kt`, multiple or missing route-kind
declarations, invalid directory names, invalid dynamic directories, duplicate
generated names, and layout-only leaf routes.

Read `troubleshooting.md` before broad rewrites when validation fails.
