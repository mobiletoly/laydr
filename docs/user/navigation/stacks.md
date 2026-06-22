# Stacks And Fullscreen Routes

Use this page when the app needs a single Nav3 stack, a parent stack, or
fullscreen routes outside a sectioned shell.

Sections are best for top-level app areas. Stacks are best for one linear flow
or for parent shells that combine app-owned entries with Laydr entries.

## Single Laydr Stack

Use a single stack when the app does not need tabs or rails:

```kotlin
val stack = LaydrNavRoutes.rememberStack(
    initialDestination = LaydrRoutes.Home.destination(),
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

The app still renders Nav3:

```kotlin
NavDisplay(
    backStack = stack.backStack,
    onBack = { stack.navigator.back() },
    entryProvider = stack.entryProvider,
)
```

The initial destination must be a generated screen destination. Layout-only
routes cannot be stack entries.

## Mixed Parent Stacks

Some app roots include entries that are not Laydr routes, such as a shell
marker, sign-in modal, or platform-specific surface. In that case the app owns
the parent Nav3 stack and lets Laydr manage only the Laydr suffix.

Use the `backStack = ...` overload:

```kotlin
val rootBackStack = remember { mutableStateListOf<Any>(ShellRoot) }

val rootStack = LaydrNavRoutes.rememberStack(
    backStack = rootBackStack,
    notFoundContent = { notFound -> NotFound(notFound) },
)
```

`rootBackStack` is app-owned Nav3 state. Laydr mutates only the trailing Laydr
entries after the last foreign key. Your app remains responsible for rendering
foreign keys in `NavDisplay`.

## Owner-Facing Vs Route-Facing Operations

Route-facing navigators expose actions route content is allowed to perform:
push, replace, Back, and result launches.

Owner-facing objects expose broader shell operations, such as reset and
external target helpers. Keep those in the shell unless a route subtree is
explicitly allowed to use a parent capability.

This split keeps route code from silently taking over root app policy.

## Fullscreen Routes

For tabbed main sections plus chrome-hidden fullscreen routes, keep the
sectioned shell as the main surface and put fullscreen entries in an app-owned
parent layer.

The parent layer decides when chrome is visible. Laydr still provides
generated destinations and route entry content.

If nested route content may launch a parent fullscreen route, provide that
capability explicitly around the allowed subtree:

```kotlin
ProvideLaydrNavStackNavigator(rootStack.navigator) {
    MainSectionShell()
}
```

Nested content can then use `laydrNavStackNavigatorOrNull()` or
`requireLaydrNavStackNavigator()`.

## What Not To Do

Do not create an implicit global root navigator.

Do not pass the full app stack through every route entry.

Do not use route layouts to control whether app-level chrome is visible.
