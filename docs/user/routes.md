# Routes

Use this page when authoring or fixing the filesystem route tree. Read
[Getting Started](getting-started.md) first if you want the same rules in a
complete app.

## Where Routes Live

KMP route root:

```text
src/commonMain/kotlin/routes/
```

Android-only route root:

```text
src/main/kotlin/routes/
```

Each directory under the route root is a path segment. A directory becomes a
declared route only when it contains `Route.kt`.

## Choose The Route Shape

| Need | Directory shape | Route kind | Generates |
| --- | --- | --- | --- |
| A normal screen | `routes/contacts/Route.kt` | `screen` | `/contacts`, `LaydrRoutes.Contacts.destination()` |
| A detail screen with an ID | `routes/contacts/by_id/Route.kt` | `screen` | `/contacts/{id}`, `LaydrRoutes.Contacts.ById.destination(id = ...)` |
| A path group only | `routes/catalog/bundle/` | none | path/object nesting only |
| A layout around child screens | `routes/contacts/Route.kt` plus child routes | `layout` | inherited layout, no `destination()` |
| A parent screen that also wraps children | `routes/settings/Route.kt` plus child routes | `screenAndLayout` | parent destination and inherited layout |
| A screen that passes render data to layouts | child screen `Route.kt` | `screenWithLayoutValues` | screen destination plus layout values |

Use the smallest shape that matches the route. Do not add `Route.kt` to a
directory just to create a path segment.

## Route Kind Decision

Use `screen` when users can navigate to the directory as a screen:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
```

Use `layout` when the directory only wraps declared descendant screens:

```kotlin
internal val Route = LaydrRouteDef.layout(content = ::ContactsLayout)
```

Layout-only routes do not generate `destination()` because they are not screen
endpoints.

Use `screenAndLayout` when the same directory is a screen and also wraps
descendant screens:

```kotlin
internal val Route = LaydrRouteDef.screenAndLayout {
    screen(content = ::Screen)
    layout(content = ::SettingsLayout)
}
```

Use `screenWithLayoutValues` when a child screen must provide runtime render
data to inherited layouts:

```kotlin
internal val Route = LaydrRouteDef.screenWithLayoutValues(content = ::Screen)
```

Read [Compose](compose.md) for layout behavior and layout values.

Core-only generation can use `LaydrRouteDeclaration.screen`,
`LaydrRouteDeclaration.layout`, or
`LaydrRouteDeclaration.screenAndLayout`.

## Static And Dynamic Paths

Directory names must be lowercase snake case.

Static segments:

```text
routes/user_profile -> /user-profile -> LaydrRoutes.UserProfile
```

Dynamic segments start with `by_`:

```text
routes/users/by_user_id -> /users/{user_id}
```

Generated dynamic arguments are lower camel case and route-scoped:

```kotlin
LaydrRoutes.Users.ByUserId.destination(
    userId = LaydrRoutes.Users.ByUserId.userId("ada"),
)
```

Laydr percent-encodes dynamic values as URI path segments when it builds a
path.

## Namespace-Only Segments

A namespace-only segment contributes a path segment and generated object
namespace, but it is not a route:

```text
routes/
  catalog/
    bundle/
      by_activity_bundle_id/
        Route.kt
        Screen.kt
```

Declared route:

- `catalog/bundle/by_activity_bundle_id`

Namespace-only segments:

- `catalog`
- `catalog/bundle`

Namespace-only segment directories may contain child segment directories. They
must not contain direct Kotlin files.

## Metadata

Route declarations may include static descriptor metadata:

```kotlin
internal val Route = LaydrRouteDef.screen(
    name = "Product detail",
    labels = mapOf("area" to "catalog"),
    content = ::ProductDetailScreen,
)
```

Metadata is app-owned descriptor data. Use it for diagnostics or app
decisions. Laydr does not treat it as auth policy, route matching behavior,
screen state, icon policy, or section chrome.

Metadata arguments must be literal values.

## Files Beside A Route

Files beside `Route.kt` are ordinary app implementation files:

```text
routes/products/by_product_id/
  Route.kt
  Screen.kt
  ProductState.kt
  ProductPreview.kt
```

Keep them local when they belong to that route. Move them into a feature
package when they are shared by multiple route packages.

## Generated APIs After Route Changes

If `LaydrRouteDef` or `LaydrRoutes` is unresolved after adding route files,
run validation or generation and refresh the IDE's Gradle model:

```sh
./gradlew :shared:checkLaydrRoutes
./gradlew :shared:generateLaydrRoutes
```

For Android-only apps:

```sh
./gradlew :app:checkLaydrRoutes
```

## Validation Catches

Validation catches:

- missing or invalid route roots
- `Route.kt` files without exactly one route-kind declaration
- Kotlin files under namespace-only segment directories
- layout-only routes without declared descendants
- invalid static or dynamic directory names
- duplicate generated route object names
- duplicate declared route ids

When a generated API is missing, fix the authored route files and rerun
validation or generation. Do not edit generated source.
