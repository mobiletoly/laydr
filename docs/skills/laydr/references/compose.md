# Compose

Use this reference for `LaydrRouteHost`, route-local screen/layout
definitions, and layout values.

## Enable Compose Generation

```kotlin
laydr {
    compose.set(true)
}
```

Generated route-local helpers are app-context-free. Resolve dependencies from
ordinary app-owned Compose code such as parameters, `CompositionLocal`,
`remember`, or Compose DI.
For DI, feature facades, shell capabilities, and previewable entrypoints, read
`dependencies.md`.

## Route-Local Screen

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::ProductDetailScreen)

@Composable
internal fun ProductDetailScreen(
    route: LaydrRoutes.Products.ByProductId.Destination,
) {
    ProductDetailBody(route.productId)
}
```

Use inline lambdas when the app wants a different local function shape, such
as extra defaulted dependency parameters:

```kotlin
internal val Route = LaydrRouteDef.screen { route ->
    ProductDetailScreen(route = route)
}
```

## Host

```kotlin
LaydrRouteHost(
    currentPath = currentPath,
    routeDefinitions = LaydrComposeRoutes.definitions,
    notFoundContent = { path -> NotFound(path) },
)
```

The app owns current path state. Laydr matches through the generated route map,
renders screen routes, and wraps inherited layouts.

## Layouts And Values

Layout routes use `LaydrRouteDef.layout`. Screen-and-layout routes use
`LaydrRouteDef.screenAndLayout`.

Screens can pass app-owned values to inherited layouts:

```kotlin
internal val Route = LaydrRouteDef.screenWithLayoutValues(content = ::Screen)

internal val TitleKey = LaydrLayoutKey<String>("title")

@Composable
internal fun Screen(route: LaydrRoutes.Contacts.ById.Destination): LaydrScreenContent {
    val title = route.id.value
    return LaydrScreenContent(
        layoutValues = LaydrLayoutValues.build {
            put(TitleKey, title)
        },
    ) {
        Body()
    }
}
```

Layouts read values from `LaydrLayoutContext`. Key lookup uses key object
identity.

## Boundaries

Do not make `LaydrRouteHost` own app navigation state, platform deep links,
query parsing, repositories, ViewModels, auth, or shell chrome. It is a
path-in/content-out Compose host.
