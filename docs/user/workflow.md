# Route-Local Workflow

Use `laydr-workflow` only when an already matched route needs private,
testable, multi-step feature state that should not become app-addressable
destinations.

Workflow is not a router. It does not replace generated destinations, Nav3
stacks, tabs, deep links, platform Back, ViewModels, ordinary screen state, or
app-shell rendering.

## Choose Route, Screen State, Or Workflow

| Need | Use |
| --- | --- |
| User can navigate to a screen or share its path | Laydr route and generated destination |
| One screen has simple UI state | `remember`, `rememberSaveable`, or app-owned state holder |
| Screen state must survive Nav3 entry lifetime | app-owned ViewModel or retained state through `NavDisplay` decorators |
| A matched route owns a private multi-step flow | route-local workflow |
| Several app areas share state or policy | app-owned store, service, or shell code |

Do not use workflow for route parameters, top-level navigation, global app
state, auth policy, or as a ViewModel replacement.

## Add The Runtime

```kotlin
commonMain.dependencies {
    implementation(libs.laydr.workflow)
}
```

`laydr-workflow` owns workflow runtime contracts and Compose host APIs. Laydr
route generation does not generate workflow nodes, renderers, reducers, or
host glue.

## Route Shape

Keep route declaration in `Route.kt`:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
```

Host the workflow from the neighboring screen:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
    dependencies: CheckoutDependencies,
    navigation: CheckoutNavigation,
) {
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        CheckoutReviewWorkflow(
            dependencies = dependencies,
            route = route,
            scope = scope,
        )
    }

    CollectLaydrWorkflowOutputs(workflow) { output ->
        when (output) {
            CheckoutOutput.Confirmed -> navigation.openConfirmation()
        }
    }

    LaydrWorkflowHost(workflow = workflow, renderer = checkoutReviewRenderer)
}
```

The route entry owns dependency lookup, workflow creation, output collection,
and mapping outputs back to app behavior.

## Runtime Model

A workflow owns:

- nodes
- node state
- outputs
- a private node stack

UI sends typed events to nodes. Nodes update state and emit outputs. The route
maps outputs to app behavior, including generated Laydr destinations when the
workflow should trigger app navigation.

Workflow nodes are not route keys or destinations.

## Boundaries

Laydr does not generate:

- workflow nodes
- reducers
- ViewModels
- repositories
- DI wiring
- renderer skeletons
- test skeletons
- business logic
- app navigation policy

Workflow files are ordinary app-owned implementation files inside declared
route packages or feature packages.

## Validation

`checkLaydrRoutes` and `generateLaydrRoutes` validate the route tree and
`Route.kt` declarations. Workflow files do not change generated route output.

If workflow host APIs are missing, add `laydr-workflow`. If outputs do not
trigger app behavior, check the route-local output collection code first.
