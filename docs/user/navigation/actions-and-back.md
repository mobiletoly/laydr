# Actions And Back

Use this page when deciding how a route should navigate or how the app should
handle Back.

Laydr navigation actions work with generated destinations. Back behavior is
still app policy exposed through the shell.

## Basic Actions

Use generated destinations:

```kotlin
navigator.push(LaydrRoutes.Profile.destination())
navigator.replace(LaydrRoutes.Contacts.destination())
navigator.select(LaydrRoutes.Profile)
```

`push` adds a new entry. `replace` replaces the current Laydr entry. `select`
switches to the section that owns a destination or route ref.

Use a generated destination when parameters are involved. A parameterless
generated route ref, such as `LaydrRoutes.Profile`, is enough when the section
root has no parameters.

Use generated parameter factories for dynamic routes:

```kotlin
navigator.push(
    LaydrRoutes.Contacts.ById.destination(
        id = LaydrRoutes.Contacts.ById.id("ada"),
    ),
)
```

## Return-Aware Navigation

Use return-aware navigation when a cross-section action should let app Back
return to the source stack.

`pushWithReturn` and `replaceWithReturn` record the current selected stack as a
return point. Later, `sections.back()` can restore that point before falling
back to normal selected-stack popping.

Use this for flows such as opening a profile route from a contacts detail
screen when Back should return to the detail screen, not just pop inside the
profile section.

## App Back

In sectioned shells, wire UI Back to:

```kotlin
sections.back()
```

This performs user-facing Back:

- restore a return-aware cross-section point when one matches
- otherwise pop the selected section stack
- keep section roots in place

Use `popSelectedStack()` only when app code intentionally wants the mechanical
selected-section pop and does not want return-aware behavior.

## Back Affordances

Use `canShowBack` when deciding whether to show a Back button:

```kotlin
sections.canShowBack(showingWideListDetail = showingWideListDetail)
```

Pass `true` when a wide list/detail layout already shows both panes and an
ordinary stack-pop button would be misleading.

Use `canReturn` when the UI needs to distinguish return-aware Back from a
normal selected-stack pop.

## Current Path

`sections.currentPath` is logical selected-stack state. It is not proof of the
exact frame Nav3 is currently animating.

Do not use it as a global animated title unless your app also owns transition
state. Route titles are usually better rendered inside route content or through
app-owned metadata.
