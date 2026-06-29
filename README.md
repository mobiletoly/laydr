# Laydr

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Maven Central](https://img.shields.io/maven-central/v/dev.goquick.laydr/dev.goquick.laydr.gradle.plugin?logo=apache-maven&label=Maven%20Central)](https://central.sonatype.com/artifact/dev.goquick.laydr/dev.goquick.laydr.gradle.plugin)
[![Release](https://img.shields.io/github/actions/workflow/status/mobiletoly/laydr/publish.yml?logo=github&label=Release)](https://github.com/mobiletoly/laydr/actions/workflows/publish.yml)
[![License](https://img.shields.io/github/license/mobiletoly/laydr?logo=apache&label=License)](LICENSE)

**If Laydr saves you time, please consider starring the repository - it helps
more Compose Multiplatform and Android Compose developers find it.**

Laydr is file-based, type-safe navigation for Compose Multiplatform and
Android Compose apps. Model your app as a visible `routes/` directory, and
Laydr generates the Kotlin destinations, path builders, route maps,
route-local Compose entrypoints, and Nav3 wiring that usually drifts when
maintained by hand.

Use Laydr when Kotlin navigation has become a web of copied route strings,
duplicated graph setup, repeated argument parsing, layout wrappers, tab
registries, and stale navigation glue. Your routes become inspectable folders;
Laydr generates the deterministic wiring; your app keeps Compose UI, state,
data, dependency access, navigation chrome, and platform policy explicit.

## Supported Platforms

Laydr supports these app targets:

| App target                           | Notes                                                                                            |
|--------------------------------------|--------------------------------------------------------------------------------------------------|
| Compose Multiplatform on Android     | Routes live in the shared KMP module; the Android launcher and platform policy stay app-owned.   |
| Compose Multiplatform on desktop/JVM | Use app-owned path state or Nav3 KMP with generated destinations.                                |
| Compose Multiplatform on iOS         | Published KMP artifacts include iOS device and simulator targets.                                |
| Compose Multiplatform on web/WasmJS  | WasmJS browser support is covered by the KMP route model.                                        |
| Android-only Compose                 | Use `src/main/kotlin/routes` and Google AndroidX Navigation 3 without a shared KMP route module. |

The Gradle plugin and route generator run on JVM 17/Gradle. AndroidX adaptive
scene support is not part of the current AndroidX adapter.

## What Laydr Gives You

Navigation structure is product structure. Laydr makes that structure visible
in source instead of scattering it across constants, graph builders, argument
parsers, screen registries, layout wrappers, and tab setup.

- a `routes/` tree that matches the product screens a maintainer needs to find
- typed destinations instead of raw route strings
- typed dynamic parameters instead of repeated argument parsing
- route-local `Route.kt`, `Screen.kt`, and `Layout.kt` files beside the route
  they define
- generated route maps and app graphs for plain Compose, Nav3 KMP, and
  AndroidX Nav3 adapters
- build-time route checks through the Gradle plugin
- app-owned Compose UI, state, DI, ViewModels, repositories, chrome, and
  platform policy

## Before And After Laydr

Before Laydr, one contact detail screen often needs several matching pieces:

- a route string such as `contacts/{id}`
- a graph entry that must use the same string
- argument extraction that must agree with the placeholder
- layout or section wiring that must be kept in sync by hand

With Laydr, the route starts as a directory, a local `Route.kt`, and ordinary
screen code. The generated API gives app code typed destinations and path
helpers that match the route tree.

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

## Supported App Shapes

Laydr supports the same route-tree model in three common Compose app shapes:

- Compose Multiplatform apps that own a current path and render content through
  `LaydrRouteHost`.
- Compose Multiplatform apps that use Nav3 for back stacks, tabs, app Back,
  payloads, route results, or adaptive scenes.
- Android-only Compose apps that use Google AndroidX Navigation 3 without a KMP
  shared module.

Start with the shape that matches your app's navigation owner. The Gradle docs
show the exact setup when you are ready to wire it in.

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

## Compose And Navigation

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

All runtime paths use the same route-local `Route.kt` declarations.

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

The snippets use `LAYDR_VERSION` as the artifact version placeholder. For this
checkout, the examples are the most reliable way to inspect current behavior
before choosing a published or locally published artifact.

## Examples

- [examples/compose-basic](examples/compose-basic/): app-owned path state
  rendered through `LaydrRouteHost`.
- [examples/nav3-kmp](examples/nav3-kmp/): generated destinations,
  route-local screens, sections, app Back, adaptive scenes, and app-owned
  `NavDisplay`.
- [examples/nav3-kmp-shopping](examples/nav3-kmp-shopping/): a larger app with
  section stacks, checkout layout, route results, transient payloads, adaptive
  list/detail scenes, Koin as app infrastructure, a route-local workflow, and
  Android, desktop, iOS, and WasmJS launchers.
- [examples/nav3-androidx](examples/nav3-androidx/): an Android-only app using
  AndroidX Navigation 3, generated sections, and `src/main/kotlin/routes`.

## Optional Route-Local Workflows

Some routes need private, testable, multi-step feature state after the app has
already matched a Laydr route. `laydr-workflow` handles that case without
turning the private flow into separate app-addressable destinations.

Workflow is optional and route-local. It does not replace generated
destinations, Nav3 stacks, tabs, deep links, platform Back, ViewModels, DI,
repositories, or app-shell policy. Many screens only need generated
destinations plus ordinary Compose state.

Read [Route-Local Workflow](docs/user/workflow.md) when a matched route owns a
private review, confirm, or wizard-style flow.

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
