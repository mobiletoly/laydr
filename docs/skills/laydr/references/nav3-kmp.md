# Nav3 KMP

Use this for KMP apps using JetBrains Navigation3 KMP with Laydr.
Use `nav3-androidx.md` for Android-only Google AndroidX Navigation 3 apps.

## Contents

- golden path
- sections
- stacks
- actions and Back
- payloads and results
- parent stack capability
- adaptive scenes
- external targets
- lower-level APIs

## Golden Path

Enable generated helpers:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Kmp.set(true)
    }
}
```

Start sectioned shells with generated `LaydrNavRoutes`:

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

The app still renders Nav3:

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

Keep labels, icons, tabs, rails, breakpoints, retained state, deep links, auth,
analytics, and visual shell policy app-owned.

## Sections

Use sections for top-level tabs, rails, or product areas that need independent
stacks. Section data is app-owned:

```kotlin
LaydrNavRoutes.Contacts.section(TabSpec("Contacts"))
```

For dynamic section roots, supply the root destination:

```kotlin
LaydrNavRoutes.Workspaces.ById.section(
    rootDestination = LaydrRoutes.Workspaces.ById.destination(
        id = LaydrRoutes.Workspaces.ById.id("alpha"),
    ),
    sectionData = TabSpec("Alpha"),
)
```

`sections.select(section)` changes the selected section without mutating stack
entries. `sections.navigator.select(LaydrRoutes.Profile)` selects the section
that owns a generated route ref.

## Stacks

Use one stack when the app does not need top-level sections:

```kotlin
val stack = LaydrNavRoutes.rememberStack(
    initialDestination = LaydrRoutes.Home.destination(),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

Render it with Nav3:

```kotlin
NavDisplay(
    backStack = stack.backStack,
    onBack = { stack.navigator.back() },
    entryProvider = stack.entryProvider,
)
```

For app-owned parent stacks, use the `backStack = ...` overload. Laydr mutates
only the trailing Laydr suffix after the last foreign key; the app renders
foreign keys in its own entry provider. When the parent stack must restore
after process death, create it with `rememberNavBackStack(...)` and a
`laydrNavSavedStateConfiguration(serializersModule = ...)` that includes
serializers for every app-owned foreign key.

Owner-facing operations such as `reset(...)`, `pushExternalTarget(...)`, and
`replaceExternalTarget(...)` stay on the stack or sections owner. Route-facing
navigators expose bounded push, replace, Back, and result-launch operations.

## Actions And Back

Use generated destinations. Return-aware operations are section-navigator
operations; do not call them on a one-stack `LaydrNavStackNavigator`.

```kotlin
sections.navigator.push(LaydrRoutes.Profile.destination())
sections.navigator.replace(LaydrRoutes.Contacts.destination())
sections.navigator.pushWithReturn(LaydrRoutes.Profile.destination())
```

Use return-aware operations when a cross-section action should let app Back
return to the source stack.

For sectioned shells, wire user-facing Back to:

```kotlin
sections.back()
```

Use `sections.canShowBack(showingWideListDetail = showingWideDetail)` for Back
button visibility. Use `canReturn` only when UI must distinguish return-aware
Back from ordinary selected-stack popping.

## Payloads And Results

Use `LaydrNavLaunch` for transient launch data:

```kotlin
stack.navigator.push(
    LaydrNavLaunch(
        destination = LaydrRoutes.Auth.SignIn.destination(),
        payload = SignInPayload(initialEmail = email),
    ),
)
```

Route content can read:

```kotlin
val payload = laydrNavPayloadOrNull<SignInPayload>()
```

Use `requireLaydrNavPayload<T>()` only when the route cannot render without
the value. Process-restored or destination-only entries may not have payloads.

Use `pushForResult` for one-shot answers:

```kotlin
stack.navigator.pushForResult<SignInResult>(
    launch = LaydrNavLaunch(
        destination = LaydrRoutes.Auth.SignIn.destination(),
    ),
    onCancel = { signInCanceled() },
) { result ->
    handleSignInResult(result)
}
```

The launched route completes or cancels the sink:

```kotlin
val resultSink = requireLaydrNavResultSink<SignInResult>()
resultSink.complete(SignInResult.SignedIn(userId))
```

Completing a result does not pop the route. Call app-owned navigation
separately when the route should close.

## Parent Stack Capability

When route content may launch onto an approved parent or fullscreen stack,
provide that capability around the allowed subtree:

```kotlin
ProvideLaydrNavStackNavigator(rootStack.navigator) {
    MainSectionShell()
}
```

Inside the subtree, use `requireLaydrNavStackNavigator()` for required parent
stack workflows or `laydrNavStackNavigatorOrNull()` for optional behavior.
Do not invent a global root navigator.

## Adaptive Scenes

KMP adaptive scenes are optional and require `laydr-nav3-kmp-adaptive`:

```kotlin
laydrNavListDetailScene(
    list = LaydrRoutes.Contacts,
    detail = LaydrRoutes.Contacts.ById,
    detailPlaceholder = { EmptyContactDetail() },
)
```

Pass scene strategies to app-owned `NavDisplay`. The app owns breakpoints,
placeholder UI, and Back affordances. AndroidX adaptive scene support is not
current behavior.

## External Targets

Use path and external-target helpers for strict app entry points, not normal
button navigation:

```kotlin
sections.pushExternalTarget("/contacts/ada?source=link#notes")
```

Accepted results mutate state. Rejected results preserve state and report a
structured reason such as unknown route, layout-only route, invalid
parameters, unsupported path, or outside-section target.

## Lower-Level APIs

Low-level adapter primitives exist for framework work and advanced shell code.
Do not start app code there when generated `LaydrNavRoutes` helpers fit.
