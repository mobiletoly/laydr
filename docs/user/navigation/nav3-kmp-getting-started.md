# Nav3 KMP Getting Started

Use this page for the first Nav3 KMP shell in a Compose Multiplatform app.

The goal is not to hide Nav3. The goal is to remove repeated glue between your
generated Laydr route tree and Nav3 entries.

That means the app still constructs `NavDisplay`, but it gets entry providers,
section ownership checks, and navigation destinations from generated Laydr
APIs.

## What You Build

A small sectioned app has four parts:

1. route-local `Route.kt` files that declare screen content
2. generated destinations for navigation
3. generated section helpers that validate entries
4. app-owned `NavDisplay`

Laydr owns the generated route values and validation. The app owns the shell,
visual chrome, and product behavior.

## Setup

Add the runtime dependency in the KMP module that owns routes:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.compose)
            implementation(libs.laydr.nav3.kmp)
        }
    }
}
```

Enable Compose generation and the KMP Nav3 helper target:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

This gives the app three generated objects:

- `LaydrRoutes` for generated routes and destinations
- `LaydrComposeRoutes` for route-local Compose definitions
- `LaydrNavRoutes` for generated Nav3 section and stack helpers

## Declare Route Content

The route declaration stays local to the route directory:

```kotlin
internal val Route = LaydrRouteDef.screen { route ->
    Screen(route = route)
}
```

The neighboring `Screen.kt` is normal app-owned Compose. It can read
repositories, ViewModels, DI, or shell callbacks through app-owned mechanisms.

## Build A Section Shell

Start with generated section helpers:

```kotlin
private data class TabSpec(val label: String)

val sections = LaydrNavRoutes.rememberSections(
    sectionSpecs = listOf(
        LaydrNavRoutes.Contacts.section(TabSpec("Contacts")),
        LaydrNavRoutes.Profile.section(TabSpec("Profile")),
    ),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

Then render Nav3 yourself:

```kotlin
NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    entryProvider = sections.entryProvider,
)
```

This is the key boundary. Laydr creates validated entries and content. Your app
chooses where `NavDisplay` lives, what surrounds it, and how Back is presented.

## Navigate With Destinations

Use generated destinations for ordinary app navigation:

```kotlin
sections.navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
```

Do not hand-build route ids or use raw path strings for normal in-app
navigation. Use path and external-target helpers only at strict app entry
points.

## Next

Read [Sections](sections.md) to add tabs or rails. Read
[Actions And Back](actions-and-back.md) before designing cross-section flows.
Use `examples/nav3-kmp/` as the complete app reference.
