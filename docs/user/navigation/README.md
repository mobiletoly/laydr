# Navigation

Use these pages when an app needs navigation behavior beyond plain path state.

Laydr does not replace Navigation3. It gives your app generated route values,
validated entries, route-local Compose content, and helper APIs around the
navigation runtime you choose.

Your app still owns the shell: `NavDisplay`, tabs, labels, icons, Back
affordances, adaptive breakpoints, transitions, retained state, platform deep
links, analytics, auth, and any product-specific navigation policy.

## Choose A Runtime

First decide whether the surface needs a navigation stack. If it only needs
to render the content for one current path, use `LaydrRouteHost`. If users can
push, pop, switch sections, return across tabs, pass launch payloads, or wait
for results, use a Nav3 adapter.

Use `LaydrRouteHost` when the app owns a path string and only needs
path-in/content-out rendering. Read [Compose](../compose.md).

Use `laydr-nav3-kmp` when a Compose Multiplatform app wants Nav3 stacks,
sections, app Back, optional adaptive scenes, payloads, results, or
`NavDisplay`.

Use `laydr-nav3-androidx` when an Android-only Compose app wants Google
AndroidX Navigation 3. Read [AndroidX Nav3](androidx.md).

## How Laydr Fits Nav3

Think of the boundary this way:

- route files declare what routes exist and how they render
- generated destinations are the values your app navigates with
- Laydr validates destinations before they become Nav3 entries
- Laydr builds an entry provider from route-local Compose definitions
- Nav3 owns the back stack and display runtime
- the app owns the shell and product policy around that runtime

That means route code can stay local:

```kotlin
internal val Route = LaydrRouteDef.screen { route ->
    Screen(route = route)
}
```

And shell code can stay explicit:

```kotlin
NavDisplay(
    backStack = sections.selectedBackStack,
    onBack = { sections.back() },
    entryProvider = sections.entryProvider,
)
```

## Read Order

1. [Nav3 KMP Getting Started](nav3-kmp-getting-started.md) - the first
   sectioned KMP shell.
2. [Sections](sections.md) - tabs, section data, selection, and app-owned
   chrome.
3. [Actions And Back](actions-and-back.md) - push, replace, select,
   return-aware flows, and app Back.
4. [Stacks](stacks.md) - single stacks, mixed parent stacks, and fullscreen
   routes.
5. [State](state.md) - saved state, ViewModels, and `NavDisplay` decorators.
6. [Payloads And Results](payloads-results.md) - transient launch data and
   one-shot answers.
7. [Advanced](advanced.md) - entry metadata, adaptive scenes, and external
   targets.

Read [AndroidX Nav3](androidx.md) instead of the KMP pages when the app is
Android-only.

## Examples

Use `examples/nav3-kmp/` for the smallest complete KMP Nav3 app.

Use `examples/nav3-kmp-shopping/` after that when you need larger section,
result, fullscreen, adaptive, and workflow patterns.

Use `examples/nav3-androidx/` for Android-only Navigation 3.
