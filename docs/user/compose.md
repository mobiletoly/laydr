# Compose

Use `laydr-compose` when you want generated route declarations rendered as
ordinary Compose content.

`LaydrRouteHost` is the simplest runtime: the app passes a path string in and
Laydr renders route content out.

## Enable Compose Generation

In the module that owns routes:

```kotlin
laydr {
    compose.set(true)
}
```

Add the runtime dependency:

```kotlin
commonMain.dependencies {
    implementation(libs.laydr.compose)
}
```

Android-only modules use normal Android dependencies:

```kotlin
dependencies {
    implementation(libs.laydr.compose)
}
```

## Declare A Screen

Route-local `Route.kt` binds generated route wiring to app-owned UI:

```kotlin
package routes.products.by_product_id

internal val Route = LaydrRouteDef.screen { route ->
    val dependencies = LocalProductRouteDependencies.current
    Screen(route = route, repository = dependencies.repository)
}
```

The neighboring screen is normal Compose. Start with explicit parameters for
the values the screen needs:

```kotlin
package routes.products.by_product_id

import androidx.compose.runtime.Composable
import example.app.generated.LaydrRoutes

@Composable
internal fun Screen(
    route: LaydrRoutes.Products.ByProductId.Destination,
    repository: ProductRepository,
) {
    ProductDetailBody(productId = route.productId.value, repository = repository)
}
```

When generated `Route.kt` needs to call `Screen(route = route)`, use a narrow
feature provider or app-owned DI as a default parameter instead. Laydr does
not care which dependency tool the app uses:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Products.ByProductId.Destination,
    dependencies: ProductRouteDependencies = LocalProductRouteDependencies.current,
) {
    ProductDetailBody(
        productId = route.productId.value,
        repository = dependencies.repository,
    )
}
```

Use `content = ::Screen` only when the function shape exactly matches the
generated callback. Use the lambda form when the entry composable has
additional defaulted parameters.

## Host Routes

Use the generated definitions object:

```kotlin
LaydrRouteHost(
    currentPath = currentPath,
    routeDefinitions = LaydrComposeRoutes.definitions,
    notFoundContent = { path -> NotFound(path) },
)
```

The app owns `currentPath`. Laydr matches it, renders the screen route, wraps
inherited layouts, and calls `notFoundContent` when no screen route matches.

## When To Use Layout Routes

Use a Laydr layout route when a route subtree needs shared rendering around
its child screens:

```text
routes/
  contacts/
    Route.kt      # layout
    Layout.kt
    by_id/
      Route.kt    # screen
      Screen.kt
```

A matched child screen renders inside the inherited layout:

```kotlin
internal val Route = LaydrRouteDef.layout(content = ::ContactsLayout)
```

For a matched screen, Laydr reads the inherited layout chain from the generated
route map and wraps content from outermost layout to innermost layout.

Use `screenAndLayout` when the parent route is both addressable and wraps
descendants:

```kotlin
internal val Route = LaydrRouteDef.screenAndLayout {
    screen(content = ::ContactsScreen)
    layout(content = ::ContactsLayout)
}
```

Do not use a Laydr layout route for app-level tabs, rails, auth policy,
selected-section state, Nav3 stack state, or platform deep-link policy. Those
belong to the app shell.

## Layout Values

Use `screenWithLayoutValues` when a child screen needs to provide runtime
rendering data to inherited layouts:

```kotlin
internal val Route = LaydrRouteDef.screenWithLayoutValues(content = ::Screen)

internal val ContactTitleKey = LaydrLayoutKey<String>("contactTitle")

@Composable
internal fun Screen(
    route: LaydrRoutes.Contacts.ById.Destination,
): LaydrScreenContent {
    val contact = rememberContact(route.id.value)
    return LaydrScreenContent(
        layoutValues = LaydrLayoutValues.build {
            put(ContactTitleKey, contact.name)
        },
    ) {
        ContactDetail(contact)
    }
}
```

Layouts read those values from `LaydrLayoutContext`. Key lookup uses key object
identity, not the key name.

Layout values are rendering data. They do not affect route matching,
destinations, navigation, auth, or persistence.

## Boundaries

`LaydrRouteHost` does not own:

- navigation stacks
- browser history
- query strings or fragments
- redirects
- platform deep links
- repositories or DI
- retained screen state
- ViewModels
- app shell policy

If you need Nav3 stacks, sections, app Back, adaptive scenes, route payloads,
or route results, use [Navigation](navigation/README.md) or
[AndroidX Nav3](navigation/androidx.md).
