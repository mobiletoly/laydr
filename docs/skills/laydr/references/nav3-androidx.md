# AndroidX Nav3

Use this reference for Android-only Compose apps using Google AndroidX
Navigation 3 with Laydr.

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

Prefer generated destinations:

```kotlin
sections.navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
```

The app renders AndroidX `NavDisplay`:

```kotlin
NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    entryProvider = sections.entryProvider,
)
```

Use generated helpers for repeated section wiring:

```kotlin
val sections = LaydrNavRoutes.rememberSections(
    sectionSpecs = listOf(
        LaydrNavRoutes.Contacts.section(TabSpec("Contacts")),
        LaydrNavRoutes.Profile.section(TabSpec("Profile")),
    ),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

Use `LaydrNavRoutes.rememberStack(initialDestination = ...)` for one-stack
apps, or `LaydrNavRoutes.rememberStack(backStack = ...)` for an app-owned
`SnapshotStateList<NavKey>` parent stack.

## Boundaries

Keep labels, icons, navigation bars, ViewModels, DI, retained-state strategy,
auth, analytics, lifecycle decorators, Android intents, and deep links
app-owned.

Payloads, typed result sinks, and entry metadata are transient entry-scoped
values. Use nullable accessors when route content can recover from
destination-only or process-restored entries.

AndroidX adaptive scene support is not implemented. Do not scaffold
AndroidX adaptive Laydr APIs.

## Validation

Run Android module tasks:

```bash
./gradlew :app:checkLaydrRoutes
./gradlew :app:compileDebugKotlin
```
