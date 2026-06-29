# AndroidX Nav3

Use this for Android-only Compose apps using Google AndroidX Navigation 3 with
Laydr. Use `nav3-kmp.md` when routes live in a shared KMP module.

## Setup

Android-only apps use:

```text
src/main/kotlin/routes
build/generated/laydr/main/kotlin
```

Enable Compose generation and AndroidX helpers:

```kotlin
laydr {
    generatedPackage.set("example.app.generated")
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
```

Add app dependencies:

```kotlin
dependencies {
    implementation(libs.laydr.compose)
    implementation(libs.laydr.nav3.androidx)
}
```

Do not enable `nav3Kmp` for the same route tree.

## Golden Path

Use generated `LaydrNavRoutes` helpers:

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

Render AndroidX `NavDisplay` yourself:

```kotlin
NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    entryProvider = sections.entryProvider,
)
```

Navigate with generated destinations:

```kotlin
sections.navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
```

Single-stack apps use `LaydrNavRoutes.rememberStack(initialDestination = ...)`.
Mixed parent stacks use `LaydrNavRoutes.rememberStack(backStack = ...)`.
For mixed parent stacks, pass an AndroidX `NavBackStack<NavKey>`.

Default AndroidX helpers create stack state with AndroidX
`rememberNavBackStack(...)`, so route identity restores across configuration
changes and Android process death. Restored Laydr entries contain route id and
route parameters only.

## Boundaries

The app owns Android UI policy, `NavDisplay`, labels, icons, navigation bars,
ViewModels, DI, retained-state strategy, lifecycle decorators, Android intents,
deep links, auth, analytics, and themes.

Payloads, typed result sinks, and entry metadata are transient entry-scoped
values. Use nullable accessors when route content can recover from
destination-only or process-restored entries.

Foreign keys in app-owned mixed stacks are app-owned. If they must restore
after process death, make them serializable AndroidX `NavKey`s.

AndroidX adaptive scene support is not implemented. Do not scaffold AndroidX
adaptive Laydr APIs.

## Validation

Run Android module tasks:

```bash
./gradlew :app:checkLaydrRoutes
./gradlew :app:compileDebugKotlin
```

Use the app's broader Android build task when route changes affect resources,
manifests, packaging, or platform wiring.
