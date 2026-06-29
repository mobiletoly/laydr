# AndroidX Nav3

Use `laydr-nav3-androidx` when an Android-only Compose app wants Google
AndroidX Navigation 3 with generated Laydr route contracts.

Use [Nav3 KMP](nav3-kmp.md) instead when the route tree belongs to a shared
Compose Multiplatform module.

The payoff is the same generated route model as KMP, but wired for an
Android-only module: `src/main/kotlin/routes`, AndroidX `NavDisplay`, and no
shared route source set.

## Setup

Android-only Laydr projects put routes here:

```text
src/main/kotlin/routes/
```

Generated source goes here:

```text
build/generated/laydr/main/kotlin/
```

Apply the Laydr plugin to the Android application or library module and add the
AndroidX adapter runtime:

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

## First App Shape

AndroidX apps use the same Laydr route model as KMP apps:

- route-local `Route.kt` declarations
- generated `destination(...)` values for navigation
- generated `LaydrNavRoutes.rememberSections(...)` and `rememberStack(...)`
  helpers
- app-owned AndroidX `NavDisplay`

The generated helpers create AndroidX `NavBackStack` state with
`rememberNavBackStack(...)`, so destination stack identity is restored across
configuration changes and Android process death. Restored Laydr entries contain
route id and route parameters only. Payloads, result callbacks, entry tokens,
and entry metadata remain transient entry-scoped data.

The app still owns Android UI policy, retained state, ViewModels, deep links,
themes, auth, and chrome. AndroidX adaptive scene support is not part of this
adapter.

## Validation

Use Android module tasks:

```sh
./gradlew :app:checkLaydrRoutes
./gradlew :app:compileDebugKotlin
```

Replace `:app` with the path of the Android module that owns `routes/`.

Read [AndroidX Navigation Details](navigation/androidx.md) for the focused
adapter guide, and [Navigation](navigation/README.md) if you are still choosing
between plain Compose hosting, Nav3 KMP, and AndroidX Nav3.

Use `examples/nav3-androidx/` as the complete Android-only reference app.
