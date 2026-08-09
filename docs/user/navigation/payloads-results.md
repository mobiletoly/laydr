# Payloads And Results

Use payloads and results only for data that belongs to one Nav3 entry.

They are transient. They are not route identity, dependencies, durable state,
auth policy, modal policy, or an event bus. KMP apps may explicitly retain
their in-memory owner across configuration recreation; process-death
restoration still has no payload objects or callbacks.

The useful decision is whether a value identifies the destination or only this
launch. Identity belongs in generated route parameters. One-entry launch
context or one-shot answers can be payloads and results.

## Payloads

Use a payload for launch-time data that should not be part of the route path:

```kotlin
stack.navigator.push(
    LaydrNavLaunch(
        destination = LaydrRoutes.Auth.SignIn.destination(),
        payload = SignInPayload(initialEmail = email),
    ),
)
```

Read it in route-local content:

```kotlin
val payload = laydrNavPayloadOrNull<SignInPayload>()
```

Use `requireLaydrNavPayload<T>()` only when the route cannot render without
that payload.

Because payloads are transient, a process-restored or destination-only entry
may not have one. Use nullable accessors when the route can recover. A KMP
entry recreated with the same app-retained `LaydrNavEntryStateOwner` keeps its
payload; the same restored key paired with a new owner does not.

## Route Parameter Or Payload

Use a route parameter when the value identifies the route. Contact IDs,
workspace IDs, slugs, and other addressable values belong in generated
`destination(...)` or `path(...)` calls.

Use a payload when the value is launch context for this one entry. A draft
object, prefilled form fields, or a one-time handoff from the caller can be a
payload if the route can recover when it is missing.

If a user should be able to refresh, restore, deep link, or share the route
with the value intact, it is not a payload.

## Results

Use a result when the caller needs one typed answer from the entry it launches:

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

The launched route reads a result sink and completes or cancels it.

Completing or canceling a result does not pop the route, switch sections, or
run app policy. If the route should close, call app-owned navigation
separately.

Completion and explicit cancellation deliver at most once. Popping or
replacing an entry removes its payload and cancels a still-pending result once.
Repeated launches of the same destination receive distinct instance ids, so
their values and callbacks cannot collide.

## Section Results

For `LaydrNavSectionsNavigator`, `pushForResult` records a return point when
the launch crosses from one section to another. Same-section launches behave
like ordinary stack pushes with a result sink.

The launched route still owns completion and explicit navigation away from the
entry.

## Recovery

Destination-only navigation, process restoration, or unsupported launch paths
can render a route without the expected payload or result sink.

For Android configuration recreation in the KMP adapter, pass the same
ViewModel-held owner back to the recreated stack or section helper. Retain the
owner only. Capturing an Activity, composition, coroutine scope, navigator, or
entry provider in the retained payload or callbacks can leak the destroyed
configuration. Always use the stack or navigator returned by the current
composition.

The route should choose an app-owned recovery path: render a recovery state,
go Back through an approved navigator, or reset from owner-facing shell code.
Laydr does not auto-close the route.
