# Compose

Use this for `LaydrRouteHost`, route-local screens/layouts, inherited layouts,
and layout values.

## Contents

- enable Compose generation
- declare route-local screens
- host current path
- use layouts and layout values
- keep app-owned boundaries

## Enable Compose Generation

```kotlin
laydr {
    compose.set(true)
}
```

Add `laydr-compose` in the route-owning module. For KMP:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.compose)
        }
    }
}
```

Android-only modules use normal `dependencies { ... }`.

## Route-Local Screen

`Route.kt` binds generated route wiring to app-owned UI:

```kotlin
internal val Route = LaydrRouteDef.screen { route ->
    Screen(route = route)
}
```

The neighboring screen is ordinary Compose:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Products.ByProductId.Destination,
    dependencies: ProductRouteDependencies =
        LocalProductRouteDependencies.current,
) {
    ProductDetailBody(
        productId = route.productId.value,
        repository = dependencies.repository,
    )
}
```

Use `content = ::Screen` only when the function shape exactly matches the
generated callback. Use the lambda form when the entry composable has defaulted
dependencies or extra parameters.

## Host Current Path

Use `LaydrRouteHost` when the app owns current path state directly:

```kotlin
LaydrRouteHost(
    currentPath = currentPath,
    routeDefinitions = LaydrComposeRoutes.definitions,
    notFoundContent = { path -> NotFound(path) },
)
```

Laydr matches the path, renders the screen route, wraps inherited layouts, and
calls `notFoundContent` when no screen route matches. The app owns path state,
browser/platform entry points, shell chrome, and retained state.

## Layouts And Values

Use a layout route for shared rendering around child screens:

```kotlin
internal val Route = LaydrRouteDef.layout(content = ::ContactsLayout)
```

Use `screenAndLayout` when the parent is both addressable and an inherited
layout:

```kotlin
internal val Route = LaydrRouteDef.screenAndLayout {
    screen(content = ::ContactsScreen)
    layout(content = ::ContactsLayout)
}
```

Use `screenWithLayoutValues` when a screen passes runtime rendering values to
inherited layouts:

```kotlin
internal val Route = LaydrRouteDef.screenWithLayoutValues(content = ::Screen)

internal val TitleKey = LaydrLayoutKey<String>("title")

@Composable
internal fun Screen(route: LaydrRoutes.Contacts.ById.Destination): LaydrScreenContent {
    return LaydrScreenContent(
        layoutValues = LaydrLayoutValues.build {
            put(TitleKey, route.id.value)
        },
    ) {
        ContactDetailBody()
    }
}
```

Layouts read values from `LaydrLayoutContext`. Key lookup uses key object
identity. Layout values are rendering data; they do not affect route matching,
navigation, auth, or persistence.

## Boundary

Do not make `LaydrRouteHost` own navigation stacks, query parsing, browser
history, deep links, repositories, DI, ViewModels, auth, retained state, or
app shell policy. Use a Nav3 adapter when the app needs stacks, sections,
Back, payloads, or results.
