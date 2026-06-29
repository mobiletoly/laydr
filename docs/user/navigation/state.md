# Navigation State

Laydr does not own persistent screen state.

Nav3 owns entries and display. Your app owns saved state, ViewModels, retained
state holders, and lifecycle policy.

The rule of thumb is: generated route parameters identify the screen, Nav3
decorators retain entry state, and app-owned state holders do the long-lived
work.

## Stack Restoration

Laydr stack restoration restores destination identity, not screen state.

KMP Nav3 helpers create saved `NavBackStack` state with Laydr's key serializer
registered. AndroidX Nav3 helpers use AndroidX `NavBackStack` with
`rememberNavBackStack(...)`. In both adapters, a restored Laydr entry contains
only the route id and generated route parameters.

Payloads, route-result callbacks, entry tokens, and entry metadata are
transient. Treat a process-restored entry the same way you would treat a direct
destination launch: load durable state from route parameters, ViewModels,
repositories, or another app-owned state boundary.

## Saved State And ViewModels

Nav3 apps still construct `NavDisplay`, so install Navigation3 entry
decorators the same way you would in a non-Laydr app.

If you override entry decorators, keep the saveable-state decorator in the
list:

```kotlin
NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = sections.entryProvider,
)
```

The decorator APIs come from Navigation3 and Lifecycle, not Laydr. Add the app
dependencies for ViewModels or retained state when your app uses them.

## What To Store Where

Use `rememberSaveable` for small UI restoration values.

Use a ViewModel or another retained state holder for loading, subscriptions,
mutation state, or long-running work.

Use generated route parameters for route identity.

Use payloads only for transient launch-time data. Use route results only for a
single answer from a launched entry.

## Plain Compose Host

`LaydrRouteHost` does not provide a Nav3 entry owner. It is only
path-in/content-out Compose hosting.

If a plain `LaydrRouteHost` app needs retained state, the app must provide that
state boundary itself.
