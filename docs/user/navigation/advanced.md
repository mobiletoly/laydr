# Advanced Navigation Topics

Read this after the common navigation pages. These features are useful, but
they should not be part of a first Laydr Nav3 shell.

The common thread is explicit ownership: Laydr can carry structured metadata,
adaptive scene hooks, and external-target results, but the app interprets
those values and decides product behavior.

## Entry Metadata

Entry metadata lets the app attach data to resolved Nav3 entries while keeping
presentation policy app-owned.

Use it for app-owned transition keys, analytics tags, route-depth
classification, or chrome hints read by Nav3 scenes and transitions.

```kotlin
val sections = LaydrNavRoutes.rememberSections(
    sectionSpecs = sectionSpecs,
    entryMetadata = { context ->
        mapOf(ChromeModeKey to chromeModeFor(context.match))
    },
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

Laydr transports metadata into `NavEntry.metadata`. It does not interpret
modal, dialog, sheet, fullscreen, transition, analytics, or chrome policy.

Typed metadata keys are optional app-owned helpers over Nav3's raw
`Map<String, Any>`.

## Adaptive Scenes

Adaptive list/detail scenes are optional KMP Nav3 helpers.

Add the adaptive artifact only when the app uses them:

```kotlin
implementation(libs.laydr.nav3.kmp.adaptive)
```

On Android, this artifact inherits Material adaptive's `compileSdk 37`
requirement.

Use adaptive scene support when a list route and a detail route should render
together on wide screens and behave like normal stack entries on compact
screens.

The app still owns breakpoints, shell layout, Back affordances, and the
placeholder UI.

## External Targets

External targets are for strict app entry points, not ordinary in-app button
navigation.

```kotlin
sections.pushExternalTarget("/contacts/ada?source=link#notes")
```

Accepted results mutate state. Rejected results preserve state and report a
structured reason, such as unknown route, layout-only route, invalid
parameters, unsupported path, or outside a declared section.

Query strings and fragments are preserved for app-owned policy. They do not
participate in Laydr route matching.

For normal in-app navigation, use generated destinations.
