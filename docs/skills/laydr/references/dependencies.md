# Route Dependencies

Use this when route content needs repositories, use cases, DI, workflow
dependencies, shell navigation, payloads/results, or previewable entrypoints.

## Contents

- default route entry shape
- dependency rules
- shell capabilities
- payloads and results
- workflow dependencies

## Default Shape

Keep `Route.kt` as route wiring:

```kotlin
internal val Route = LaydrRouteDef.screen { route ->
    Screen(route = route)
}
```

Make the route entry composable a small adapter from generated route value to
app-owned UI:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.Destination,
    profileApi: ProfileApi = koinInject(),
    navigation: ProfileNavigation = LocalProfileNavigation.current,
) {
    ProfileScreen(
        profile = profileApi.loadProfile(),
        onOpenSignIn = { navigation.openSignIn(source = route.path) },
    )
}
```

For previews and tests, pass fake dependencies directly.

## Dependency Rules

- Keep generated route rendering app-context-free.
- Prefer explicit parameters and narrow defaulted providers.
- Use `content = ::Screen` only when `Screen` has exactly the generated route
  parameter shape.
- Use `{ route -> Screen(route = route) }` when `Screen` has defaulted
  dependencies or extra parameters.
- Keep feature facades narrow and feature-owned.
- Use `CompositionLocal` for app shell capabilities or real subtree lifetimes,
  not as a root registry for every feature.
- Do not create root containers such as `MainLaydrContext`.
- Do not add umbrella providers such as `ProvideAllRouteEnvironments` around
  `NavDisplay`.
- Do not make Laydr own DI, repositories, auth, persistence, labels, icons,
  ViewModels, reducers, shell chrome, or platform policy.

## Shell Capabilities

Expose only the action a route needs:

```kotlin
internal interface ProfileNavigation {
    fun openSignIn(source: String)
}
```

For KMP Nav3 parent or fullscreen stack launches, the shell may provide an
approved `LaydrNavStackNavigator` around a subtree:

```kotlin
ProvideLaydrNavStackNavigator(rootStack.navigator) {
    MainSectionShell()
}
```

`ProvideLaydrNavStackNavigator`, `requireLaydrNavStackNavigator`, and
`laydrNavStackNavigatorOrNull` are exported by `laydr-nav3-kmp`. AndroidX Nav3
apps should pass an app-owned parent-stack capability explicitly through their
own shell wiring instead of using those KMP-only locals.

There is no implicit global root navigator in either adapter. Do not pass the
full app stack to every route.

## Payloads And Results

Use payloads for transient launch-time data scoped to one entry:

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
val payload = laydrNavPayloadOrNull<SignInPayload>()
```

Use results when the caller needs one typed answer:

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

Payloads and results are transient. They are not route identity, DI, retained
state, workflow outputs, modal policy, or event buses. Use generated route
parameters when the value identifies the route.

## Workflow Dependencies

Resolve workflow dependencies before creating the workflow:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
    dependencies: CheckoutDependencies = koinInject(),
    navigation: CheckoutNavigation = LocalCheckoutNavigation.current,
) {
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        CheckoutWorkflow(dependencies = dependencies, route = route, scope = scope)
    }

    LaydrWorkflowHost(
        workflow = workflow,
        renderer = checkoutRenderer,
    ) { output ->
        navigation.handle(output)
    }
}
```

Workflow nodes, renderers, output mapping, and dependency lookup remain
app-owned.
