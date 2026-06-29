# Route Dependencies

Laydr generates route wiring, not app architecture.

Route-local screens receive generated destinations. Layouts receive layout
context and child content. Your app decides how repositories, services, state
holders, DI, ViewModels, shell callbacks, and platform dependencies reach
those composables.

The useful pattern is to treat a route entrypoint as a small adapter: generated
route value in, narrow app dependencies in, ordinary screen UI out. That keeps
routes local without turning Laydr into a service locator.

## Start With Explicit Screen Parameters

Keep the route-owned screen easy to preview and test. The route declaration can
bridge a narrow provider into explicit parameters:

```kotlin
// routes/profile/Route.kt
internal val Route = LaydrRouteDef.screen { route ->
    val dependencies = LocalProfileRouteDependencies.current
    Screen(
        route = route,
        profileApi = dependencies.profileApi,
        navigation = dependencies.navigation,
    )
}
```

```kotlin
// routes/profile/Screen.kt
@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.Destination,
    profileApi: ProfileApi,
    navigation: ProfileNavigation,
) {
    ProfileScreen(
        profile = profileApi.loadProfile(),
        onOpenSignIn = { navigation.openSignIn(source = route.path) },
    )
}
```

For previews, pass fake values directly:

```kotlin
@Preview
@Composable
private fun ProfilePreview() {
    Screen(
        route = LaydrRoutes.Profile.destination(),
        profileApi = FakeProfileApi(),
        navigation = FakeProfileNavigation,
    )
}
```

This is the clearest dependency shape. Use it whenever the host or parent
feature can pass the values naturally, or when a small route provider can
bridge values into the screen.

## Add A Narrow Provider When Needed

Generated `Route.kt` callbacks usually call `Screen(route = route)`. If many
routes in one feature need the same dependencies, use a narrow feature
provider with default parameters:

```kotlin
internal data class ProfileRouteDependencies(
    val profileApi: ProfileApi,
    val navigation: ProfileNavigation,
)

internal val LocalProfileRouteDependencies =
    staticCompositionLocalOf<ProfileRouteDependencies> {
        error("ProfileRouteDependencies was not provided")
    }

@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.Destination,
    dependencies: ProfileRouteDependencies = LocalProfileRouteDependencies.current,
) {
    ProfileScreen(
        profile = dependencies.profileApi.loadProfile(),
        onOpenSignIn = {
            dependencies.navigation.openSignIn(source = route.path)
        },
    )
}
```

Keep the provider feature-owned. Do not turn it into a root object for every
route in the app.

## DI Is App-Owned

Laydr does not require Koin, Dagger, Hilt, manual factories, or any other DI
tool. If your app already uses Compose-aware DI, a route entry composable can
read it like ordinary Compose code:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Program.ById.Destination,
    dependencies: ProgramRouteDependencies = koinInject(),
) {
    ProgramScreen(
        programId = route.id.value,
        repository = dependencies.repository,
        formatter = dependencies.formatter,
    )
}
```

This is still app-owned dependency lookup. Laydr does not generate DI modules,
scopes, components, or test bindings.

## Supported Dependency Sources

Route entry composables can use ordinary app-owned Compose tools:

- explicit parameters
- `CompositionLocal` values from the app shell or feature owner
- `remember` for route-local UI state
- `rememberSaveable` when the host provides a saveable state owner
- ViewModels or retained state holders when the app provides the owner
- Compose DI such as Koin's `koinInject()`
- narrow feature-owned facades

## Feature Dependencies

Feature dependencies are services the route uses to render or perform local
work: repositories, use cases, APIs, stores, workflow factories, analytics
clients, purchase helpers, and formatters.

Keep them local or behind a narrow feature facade. If a dependency is used by
only one route, prefer a direct parameter or direct app-owned lookup in that
route entry. If several related child routes share it, use a feature provider.

## Shell Capabilities

Shell capabilities are actions owned by the surrounding app surface: opening a
fullscreen route, switching sections, showing sign-in, closing a parent entry,
or changing global chrome.

Expose only the capability the route needs:

```kotlin
internal interface ProgramShellNavigation {
    fun openActivityEditor(activityId: String)
    fun closeFullscreen()
}
```

For generated parent or root stack launches, an app may provide
`LaydrNavStackNavigator` around the route subtree with
`ProvideLaydrNavStackNavigator`. There is no implicit global root navigator.

## What To Avoid

Avoid broad root containers:

```kotlin
internal class MainLaydrContext(
    val profileApi: ProfileApi,
    val catalogRepository: CatalogRepository,
    val planningStore: PlanningStore,
    val reportEngine: ReportEngine,
)
```

Avoid umbrella providers around a broad route subtree:

```kotlin
ProvideAllRouteEnvironments {
    NavDisplay(...)
}
```

Those shapes hide route ownership, connect unrelated features, and make one
route harder to preview or test.

## When A Provider Is Reasonable

A route or feature provider is reasonable when it represents a real lifetime
or subtree boundary:

- feature state must survive child route swaps
- a tab-level cache outlives individual entry compositions
- child routes share one feature-scoped workflow owner
- a platform integration must be installed once around a specific subtree

Keep that provider feature-owned and narrow. If it becomes a registry for
unrelated routes, move dependencies back to route entry composables or DI.

## Persistent Screen State

Laydr does not own persistent screen state.

In Nav3 KMP shells, install Navigation3 saved-state or ViewModel entry
decorators on the app-owned `NavDisplay`; see
[Navigation State](navigation/state.md).

Plain `LaydrRouteHost` is path/content hosting. Apps that use it directly
bring their own retained or saved-state boundary.

## Payloads And Results

Route payloads and results are navigation features. Read
[Payloads And Results](navigation/payloads-results.md) for the full model.

Payloads and results are not DI, retained state, auth policy, modal policy, or
event buses. They are transient. Use nullable accessors when the route can be
restored or opened without the expected value.

## Workflows

Workflow hosting follows the same rule: the route entry composable owns
dependency lookup, creates the workflow, collects outputs, and maps outputs to
app behavior.

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
    dependencies: CheckoutDependencies,
    navigation: CheckoutNavigation,
) {
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        CheckoutWorkflow(dependencies = dependencies, route = route, scope = scope)
    }

    CollectLaydrWorkflowOutputs(workflow) { output ->
        when (output) {
            CheckoutOutput.Confirmed -> navigation.openConfirmation()
        }
    }

    LaydrWorkflowHost(workflow = workflow, renderer = checkoutRenderer)
}
```

Laydr does not generate workflow nodes, renderers, DI modules, test skeletons,
or workflow environment providers.
