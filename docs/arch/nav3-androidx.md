# AndroidX Nav3 Adapter

`laydr-nav3-androidx` adapts generated Laydr route descriptors to Google
AndroidX Navigation 3 for Android-only Compose apps.

The module targets the AndroidX artifact family:

```kotlin
androidx.navigation3:navigation3-runtime
androidx.navigation3:navigation3-ui
```

It does not target JetBrains Compose Multiplatform Navigation3. Use
`laydr-nav3-kmp` for KMP apps.

## Boundary

`laydr-nav-runtime` owns shared Laydr navigation semantics: destination
validation, route-key conversion facts, path and external-target rejection,
stack suffix ownership, push/replace/reset/back behavior, section selection,
return history, payload/result cleanup, not-found classification, and route
placement.

`laydr-nav3-androidx` owns the AndroidX boundary:

- `LaydrNavKey` implements AndroidX `NavKey`
- AndroidX stacks are `NavBackStack<NavKey>`
- entry providers return AndroidX `NavEntry<NavKey>`
- apps render AndroidX `NavDisplay`
- Android Lifecycle, ViewModel decorators, app themes, DI, auth, retained
  state, intent deep links, and chrome remain app-owned

Default AndroidX section and stack helpers create `NavBackStack` state with
AndroidX saved-stack behavior. That restores Laydr destination identity across
configuration changes and Android process death. Restored Laydr entries contain
route id and generated route parameters only; payloads, result callbacks, entry
tokens, and entry metadata remain transient.

The AndroidX adapter must not depend on JetBrains Navigation3 KMP. The KMP
adapter must not depend on AndroidX Navigation 3.

## Generated Helpers

Android-only apps enable the AndroidX helper target:

```kotlin
laydr {
    compose.set(true)
    adapters {
        nav3Androidx.set(true)
    }
}
```

This generates `LaydrNavRoutes`, the same app-facing helper object name used
by the KMP target. Only one target can be enabled for a source set.

The generated helpers expose section and stack setup around
`laydr-nav3-androidx` primitives:

- `LaydrNavRoutes.rememberSections(...)`
- `LaydrNavRoutes.rememberStack(initialDestination = ...)`
- `LaydrNavRoutes.rememberStack(backStack = ...)`
- generated route-local section builders

The helpers do not add dependencies, generate app chrome, or choose Android
presentation policy.

## Source Set

Android-only modules default to:

```text
src/main/kotlin/routes
```

Generated source is wired into:

```text
build/generated/laydr/main/kotlin
```

KMP projects keep using `src/commonMain/kotlin/routes` and the KMP adapter.

## Deferred

AndroidX adaptive scene support is not implemented in this adapter. Do not
document or depend on AndroidX adaptive Laydr APIs until a separate spec adds
and validates them.
