# Nav3 KMP

Use `laydr-nav3-kmp` when a Compose Multiplatform app wants Nav3 stacks,
sections, app Back, payloads, results, optional adaptive scenes, or
`NavDisplay`.

Laydr does not replace Navigation3. It gives your app generated destinations,
validated entries, route-local Compose content, section helpers, stack helpers,
and structured external-target results. Your app still owns `NavDisplay`,
tabs, labels, icons, adaptive breakpoints, transitions, retained state, deep
links, auth, analytics, and product policy.

## Setup

Add the runtime dependency in the shared KMP module that owns
`src/commonMain/kotlin/routes`:

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

This generates:

- `LaydrRoutes` for destinations and path helpers
- `LaydrComposeRoutes` for route-local Compose definitions
- `LaydrNavRoutes` for Nav3 section and stack helpers

## First App Shape

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

Render Nav3 yourself:

```kotlin
NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    entryProvider = sections.entryProvider,
)
```

Use generated destinations for ordinary app navigation:

```kotlin
sections.navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
```

## Read Next

Use the focused navigation pages when the app needs more detail:

- [Nav3 KMP Getting Started](navigation/nav3-kmp-getting-started.md) for the
  first sectioned KMP shell.
- [Sections](navigation/sections.md) for tabs, section data, selection, and
  app-owned chrome.
- [Actions And Back](navigation/actions-and-back.md) for push, replace,
  return-aware flows, and app Back.
- [Stacks And Fullscreen Routes](navigation/stacks.md) for single stacks,
  mixed parent stacks, and fullscreen routes.
- [Navigation State](navigation/state.md) for saved state, ViewModels, and
  `NavDisplay` decorators.
- [Payloads And Results](navigation/payloads-results.md) for transient launch
  data and one-shot answers.
- [Advanced Navigation Topics](navigation/advanced.md) for entry metadata,
  adaptive scenes, and external targets.

Use `examples/nav3-kmp/` as the complete first app reference and
`examples/nav3-kmp-shopping/` for larger section, result, fullscreen,
adaptive, and workflow patterns.
