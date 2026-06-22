# Compose Basic Example

This is the first consumer-style Laydr example. It is included in the Laydr
root build for IDE run configurations and uses the root project dependencies
directly.

The shared UI is a small Address Book app. It demonstrates generated route
descriptors, typed path builders, dynamic path params, inherited layouts,
screen-owned layout data, and app-owned path navigation without a navigation
library.

The first wired modules are:

- `shared`: Compose Multiplatform shared UI. It applies the Laydr Gradle
  plugin, imports the generated route graph from common source, and renders
  through `LaydrRouteHost`.
- `androidApp`: Android application entry point that renders the shared
  `ExampleApp`.
- `desktopApp`: desktop entry point for quick local validation.

Generate Laydr routes for the shared example with:

```sh
./gradlew :examples:compose-basic:shared:generateLaydrRoutes
```

Validate the shared example route tree without writing generated source with:

```sh
./gradlew :examples:compose-basic:shared:checkLaydrRoutes
```

The shared app keeps navigation state in app code. Route-to-screen and
route-to-layout declarations live in route-local `Route.kt` files, and
generated `LaydrComposeRoutes.definitions` assembles them. The app provides
its example context with a `CompositionLocal`. Route-local entry functions in
`Screen.kt` and `Layout.kt` read that context through default parameters, while
`Route.kt` remains route wiring. The app passes an app-owned current path and
generated route definitions into `LaydrRouteHost`; Laydr renders the matching
screen and inherited layouts without owning navigation state.

The route tree includes:

```text
/contacts
/contacts/{id}
/contacts/{id}/edit
```

The app-owned `contacts/Layout.kt` implementation wraps the list, detail, and
edit screens. Screens return `LaydrScreenContent` with screen-owned layout
state, and the inherited layout reads that state through `LaydrLayoutContext`.
The layout renders a persistent contacts sidebar on wide screens and a
route-driven single-pane layout on small screens.

From the Laydr root, compile the desktop entry point with:

```sh
./gradlew :examples:compose-basic:desktopApp:compileKotlin
```

Run the desktop example from the Laydr root with:

```sh
./gradlew :examples:compose-basic:desktopApp:run
```

From the Laydr root, assemble the Android debug app with:

```sh
./gradlew :examples:compose-basic:androidApp:assembleDebug
```

Install the Android debug app on a connected device or emulator with:

```sh
./gradlew :examples:compose-basic:androidApp:installDebug
```

`iosApp` is still reserved as a future platform entry point.
