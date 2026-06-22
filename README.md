# Laydr

Laydr is a KMP-first routing layer for Compose Multiplatform apps and
Android-only Compose apps. It turns a visible `routes/` directory into typed
Kotlin destinations, route-local Compose entrypoints, generated route maps,
and validated Nav3 wiring.

Use Laydr when your app has outgrown hand-written route strings, duplicated
graph setup, repeated argument extraction, manual layout wrapping, and stale
navigation glue. Laydr generates the deterministic wiring from your filesystem
route tree while your app keeps Compose, state, data, dependency access,
navigation chrome, and platform policy explicit.

## Why Laydr

Navigation structure is product structure. In many Compose apps, that
structure is split across route constants, graph builders, argument parsers,
screen registries, layout wrappers, and tab setup. The result is code that is
easy to drift and hard for a new maintainer to inspect.

Laydr makes the route tree visible in the project and generates the boring
Kotlin around it:

- typed destinations instead of raw route strings
- typed dynamic parameters instead of repeated argument parsing
- generated route maps and app graphs for adapter validation
- route-local screen and layout declarations beside the route they render
- build-time route checks through the Gradle plugin
- optional Nav3 KMP section, stack, payload, result, and adaptive-scene helpers
- optional AndroidX Navigation 3 section, stack, payload, and result helpers
- optional route-local workflow hosting for private feature flow under a route

## The Route Tree Is The Map

Put route directories under the KMP module that owns app navigation:

```text
src/commonMain/kotlin/routes/
  contacts/
    Route.kt
    Screen.kt
    by_id/
      Route.kt
      Screen.kt
```

This tree gives the app two screen routes:

- `routes/contacts` -> `/contacts` -> `LaydrRoutes.Contacts`
- `routes/contacts/by_id` -> `/contacts/{id}` ->
  `LaydrRoutes.Contacts.ById`

`Route.kt` declares the route kind. Nearby `Screen.kt`, `Layout.kt`,
repositories, state holders, and UI components are ordinary app code.

Route-local rendering stays small:

```kotlin
// routes/contacts/by_id/Route.kt
internal val Route = LaydrRouteDef.screen(content = ::Screen)
```

App code navigates with generated destinations:

```kotlin
navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
```

Apps that deliberately own path state can also use generated path helpers:

```kotlin
LaydrRoutes.Contacts.ById.path(
    id = LaydrRoutes.Contacts.ById.id("ada"),
)
```

## What Laydr Generates

Laydr generated source is readable Kotlin under
`build/generated/laydr/<source-set>/kotlin/`. KMP projects use
`commonMain`; Android-only projects use `main`. You do not edit generated
source by hand; change authored route files and rerun route checks.

The generated API includes:

- `LaydrRoutes` for typed route objects, destinations, path builders,
  `appGraph`, and `routeMap`
- `LaydrComposeRoutes.definitions` for route-local Compose rendering
- route-package `LaydrRouteDef` helpers for `screen`, `layout`,
  `screenAndLayout`, and layout-value screens
- optional `LaydrNavRoutes` helpers for either Nav3 KMP or AndroidX Nav3
  sections and stacks

The Gradle plugin wires generated source into KMP `commonMain` or Android-only
`main`, generates routes, and validates the route tree during `check`.

## Compose And Nav3

Use `LaydrRouteHost` when your app wants the smallest path-in/content-out
Compose host and owns path state directly.

Use `laydr-nav3-kmp` when JetBrains Navigation3 KMP should own back stacks,
sections, app Back, `NavDisplay`, payloads, route results, and optional
adaptive list/detail scenes. Laydr validates generated destinations and gives
Nav3 app code typed route values; the app still renders `NavDisplay` and owns
presentation policy.

Use `laydr-nav3-androidx` when an Android-only Compose app should use Google
AndroidX Navigation 3 without a KMP shared module. AndroidX adaptive scene
support is not part of the current AndroidX adapter.

All paths use the same route-local `Route.kt` declarations.

## What Your App Still Owns

Laydr is routing infrastructure, not a client architecture framework. Your app
still owns:

- Compose UI and design
- navigation chrome, labels, icons, ordering, visibility, and transitions
- data loading, repositories, DI, ViewModels, reducers, and retained state
- auth, permissions, analytics, persistence, and platform lifecycle policy
- browser history, Android intents, iOS universal links, query parsing, and
  fragment routing

Laydr gives those app-owned pieces stable generated route values to use.

## Quick Start

Define the Laydr version once:

```toml
[versions]
laydr = "LAYDR_VERSION"

[plugins]
laydr = { id = "dev.goquick.laydr", version.ref = "laydr" }

[libraries]
laydr-compose = { module = "dev.goquick.laydr:laydr-compose", version.ref = "laydr" }
laydr-nav3-kmp = { module = "dev.goquick.laydr:laydr-nav3-kmp", version.ref = "laydr" }
laydr-nav3-androidx = { module = "dev.goquick.laydr:laydr-nav3-androidx", version.ref = "laydr" }
laydr-workflow = { module = "dev.goquick.laydr:laydr-workflow", version.ref = "laydr" }
```

Apply the plugin in the KMP module that owns `src/commonMain/kotlin/routes`:

```kotlin
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.laydr)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.compose)
            implementation(libs.laydr.nav3.kmp)
        }
    }
}

laydr {
    generatedPackage.set("example.app.generated")
    compose.set(true)
}
```

Then add route-local `Route.kt` files and validate them:

```sh
./gradlew :shared:checkLaydrRoutes
```

Read [Getting Started](docs/user/getting-started.md) for a complete first app,
and [Gradle](docs/user/gradle.md) for artifact coordinates, composite-build
setup, task details, and generated-source locations.

## Examples

- [examples/compose-basic](examples/compose-basic/): app-owned path state
  rendered through `LaydrRouteHost`.
- [examples/nav3-kmp](examples/nav3-kmp/): generated destinations,
  route-local screens, sections, app Back, adaptive scenes, and app-owned
  `NavDisplay`.
- [examples/nav3-kmp-shopping](examples/nav3-kmp-shopping/): a larger app with
  section stacks, checkout layout, route results, transient payloads, adaptive
  list/detail scenes, Koin as app infrastructure, and a route-local workflow.
- [examples/nav3-androidx](examples/nav3-androidx/): an Android-only app using
  AndroidX Navigation 3, generated sections, and `src/main/kotlin/routes`.

## Learn More

Start with the [user documentation](docs/user/README.md):

- [Getting Started](docs/user/getting-started.md): build one route tree and
  render it with plain Compose, Nav3 KMP, or AndroidX Nav3.
- [Concepts](docs/user/concepts.md): learn the route tree, generated source,
  runtime adapter, and app-owned policy model.
- [Generated API Tour](docs/user/generated-api.md): learn `LaydrRoutes`,
  destinations, route maps, Compose definitions, and route-local helpers.
- [Routes](docs/user/routes.md): filesystem conventions and validation rules.
- [Route Dependencies](docs/user/route-dependencies.md): keep dependency
  access near route entrypoints without generated app contexts.
- [Compose](docs/user/compose.md): route host, route-local render definitions,
  layouts, and layout values.
- [Nav3 KMP](docs/user/nav3-kmp.md): sections, stacks, app Back, payloads,
  route results, adaptive scenes, and external targets.
- [AndroidX Nav3](docs/user/nav3-androidx.md): Android-only sections, stacks,
  app Back, payloads, route results, and generated helpers.
- [Route-Local Workflow](docs/user/workflow.md): private route workflows with
  app-owned workflow host code.
- [Troubleshooting](docs/user/troubleshooting.md): common scanner, generated
  API, Compose, Nav3 KMP, and workflow failures.

## Current Scope

Laydr is in v0 development. The docs describe current supported behavior in
this checkout and avoid planned adapters, migration history, deprecated
alternatives, and maintainer-only workflows.
