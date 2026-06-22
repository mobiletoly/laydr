# Navigation State

Laydr does not own persistent screen state.

Nav3 owns entries and display. Your app owns saved state, ViewModels, retained
state holders, and lifecycle policy.

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
