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
`rememberNavBackStack(...)`. A restored transient KMP entry contains its route
id, generated route parameters, and an opaque entry-instance id. The id is
only a correlation key. Payload objects, callbacks, metadata, ViewModels, and
platform owners are never serialized into saved state.

A fresh owner, including the owner created after process restoration, has no
value for a restored id. Treat that entry the same way you would treat a
direct destination launch: load durable state from route parameters,
ViewModels, repositories, or another app-owned state boundary.

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

## Retaining Transient Entry State Across Configuration Recreation

KMP stacks and sections accept an optional `LaydrNavEntryStateOwner`. Keep the
owner in an app-selected configuration-retained boundary when an open entry's
payload and pending one-shot result must survive Activity recreation:

```kotlin
class NavigationEntryStateViewModel : ViewModel() {
    val owner = LaydrNavEntryStateOwner()

    override fun onCleared() {
        owner.clear()
    }
}

@Composable
fun AppShell(model: NavigationEntryStateViewModel = viewModel()) {
    val stack = LaydrNavRoutes.rememberStack(
        initialDestination = LaydrRoutes.Home.destination(),
        entryStateOwner = model.owner,
        notFoundContent = { notFound -> NotFound(notFound) },
    )

    NavDisplay(
        backStack = stack.backStack,
        onBack = { stack.navigator.back() },
        entryProvider = stack.entryProvider,
    )
}
```

The same parameter is available on `rememberSections`. One owner binding
covers every stack in that section set. A new binding invalidates the previous
stack, sections object, and navigator before mutation. Reacquire those values
from the current composition; never retain a composition-created stack,
sections object, entry provider, or navigator in a ViewModel.

Omitting `entryStateOwner` keeps the default composition-owned lifetime.
Passing a new owner after process restoration intentionally produces missing
payload and result lookups. AndroidX keys preserve the opaque id consistently,
but the AndroidX adapter does not currently expose retained-owner integration.

## What To Store Where

Use `rememberSaveable` for small UI restoration values.

Use a ViewModel or another retained state holder for loading, subscriptions,
mutation state, or long-running work.

Use generated route parameters for route identity.

Use payloads only for transient launch-time data. Use route results only for a
single answer from a launched entry. Configuration retention extends their
in-memory lifetime; it does not make them durable state.

## Plain Compose Host

`LaydrRouteHost` does not provide a Nav3 entry owner. It is only
path-in/content-out Compose hosting.

If a plain `LaydrRouteHost` app needs retained state, the app must provide that
state boundary itself.
