# AndroidX Nav3

Use `laydr-nav3-androidx` when an Android-only Compose app wants Google
AndroidX Navigation 3 with generated Laydr route contracts.

Use the KMP navigation pages when the app has a shared Compose Multiplatform
module.

The useful difference is only the runtime boundary. AndroidX apps keep route
files in the Android source set and render with AndroidX Navigation 3, while
still navigating through generated Laydr destinations.

## Setup

Android-only Laydr projects use:

```text
src/main/kotlin/routes/
```

Generated source goes to:

```text
build/generated/laydr/main/kotlin/
```

Apply the Laydr plugin to the Android application or library module and add
the AndroidX adapter runtime:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.laydr)
}

dependencies {
    implementation(libs.laydr.compose)
    implementation(libs.laydr.nav3.androidx)
}
```

Enable Compose generation and the AndroidX helper target:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
```

Only one Nav3 helper target can be enabled for a route tree.

## Same Model, Android Runtime

AndroidX apps use the same generated route model:

- generated `destination(...)` values for navigation
- route-local `Route.kt` files for rendering declarations
- `LaydrNavRoutes.rememberSections(...)` or `rememberStack(...)` for
  validated AndroidX stacks
- app-owned AndroidX `NavDisplay`

The app still owns Android UI policy, retained state, ViewModels, deep links,
themes, auth, and chrome.

## What Is Different

Android-only route files live in `src/main/kotlin/routes`, not
`src/commonMain/kotlin/routes`.

AndroidX back stacks use AndroidX Navigation 3 types. Use the AndroidX adapter
in Android modules and the KMP adapter in shared KMP modules.

Generated AndroidX helpers use AndroidX `NavBackStack` with
`rememberNavBackStack(...)` for their default section and stack state. That
restores destination stack identity across configuration changes and Android
process death. Restored Laydr entries carry route id and route parameters;
payloads, result callbacks, entry tokens, and entry metadata remain transient.

For mixed parent stacks, pass an app-owned `NavBackStack<NavKey>` to
`LaydrNavRoutes.rememberStack(backStack = ...)`. Foreign keys in that stack are
app-owned; if they need process-death restore, they must be serializable
AndroidX `NavKey`s.

AndroidX adaptive scene support is not currently part of this adapter. Do not
depend on AndroidX adaptive Laydr APIs.

## Validation

Use the Android module tasks:

```sh
./gradlew :app:checkLaydrRoutes
./gradlew :app:compileDebugKotlin
```

Read `examples/nav3-androidx/` for a complete Android-only app.
