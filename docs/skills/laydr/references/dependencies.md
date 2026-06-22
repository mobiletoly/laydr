# Route Dependencies

Use this reference when editing a Laydr app route that needs repositories,
use cases, DI, workflow dependencies, shell navigation, or previewable
entrypoints.

## Default Pattern

Keep `Route.kt` as route wiring:

```kotlin
internal val Route = LaydrRouteDef.screen { route ->
    Screen(route = route)
}
```

Acquire dependencies in the route entry composable:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.Destination,
    profileApi: ProfileApi = koinInject(),
    shellNavigation: ProfileShellNavigation = LocalProfileShellNavigation.current,
) {
    ProfileScreen(
        profileApi = profileApi,
        onOpenSignIn = { shellNavigation.openSignIn(source = route.path) },
    )
}
```

Use explicit arguments in previews and tests.

## Checklist

- Keep generated route rendering app-context-free.
- Keep dependencies app-owned.
- Prefer local default parameters for ordinary route dependencies.
- Use `content = ::Screen` only when `Screen` has exactly the generated route
  parameter shape.
- Use `{ route -> Screen(route) }` when `Screen` has extra defaulted
  parameters.
- Keep feature facades narrow and feature-owned.
- Keep shell navigation capabilities narrow and separate from feature
  repositories or services.
- Use `CompositionLocal` for app shell capabilities or explicit subtree
  lifetimes, not as a root registry for every feature.
- Do not create root app contexts such as `MainLaydrContext`.
- Do not add umbrella providers such as `ProvideAllRouteEnvironments` around
  `NavDisplay`.
- Do not make Laydr own DI, repositories, auth, persistence, labels, icons,
  ViewModels, reducers, or shell chrome.

## Route Payloads

Use Nav3 managed-stack payloads for transient launch data scoped to one entry,
such as an initial form value, launch source, or one-entry request object:

```kotlin
stack.navigator.push(
    LaydrNavLaunch(
        destination = LaydrRoutes.Auth.SignIn.destination(),
        payload = SignInPayload(initialEmail = email),
    ),
)
```

Read payloads in route-local Compose code:

```kotlin
@Composable
internal fun Screen(route: LaydrRoutes.Auth.SignIn.Destination) {
    val payload = laydrNavPayloadOrNull<SignInPayload>()
    SignInScreen(initialEmail = payload?.initialEmail.orEmpty())
}
```

Do not use payloads for DI, retained state, auth policy, result handling,
modal metadata, or process-restored state. Use route-local dependency lookup
or narrow feature providers for long-lived services and shared feature state.

Use Nav3 route results for one-shot answers from a launched entry:

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

Route results are transient callback transport. They are not DI, retained
state, workflow outputs, an event bus, automatic pop behavior, modal policy,
or post-result app policy.

## Provider Escape Hatch

A provider is acceptable when it owns a real feature lifetime, such as a
tab-level cache, feature-scoped state holder, or platform integration that
must wrap a specific subtree.

If the provider would only exist because Laydr lacks a route entry wrapping
hook or lifecycle affordance, stop and report framework friction instead of
normalizing a broad provider cascade.

## Workflow Routes

Resolve workflow dependencies before creating the workflow:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
    dependencies: CheckoutDependencies = koinInject(),
    navigation: CheckoutNavigation = LocalCheckoutNavigation.current,
) {
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        CheckoutWorkflow(dependencies, route, scope)
    }
    CollectLaydrWorkflowOutputs(workflow) { output ->
        navigation.handle(output)
    }
    LaydrWorkflowHost(workflow = workflow, renderer = checkoutRenderer)
}
```

Workflow nodes, renderer registration, output handling, and DI wiring remain
app-owned.
