# Route-Local Workflow

Use `laydr-workflow` when an already matched route needs private, testable,
multi-step feature state that should not become app-addressable destinations.

Workflow is not a router. It does not replace generated destinations, Nav3
stacks, tabs, deep links, platform Back, ViewModels, ordinary screen state, DI,
repositories, reducers, or app-shell rendering.

## Mental Model

A Laydr route gets the user to a place in the app. A workflow can run inside
that place.

For example, `/cart/checkout/review` is a route because the app can navigate
to it. Inside that route, the user might move between "review order" and
"confirm order" states. Those states are private to the review route, so they
are workflow nodes instead of new route destinations.

The route entry composable owns the workflow:

- it creates the workflow
- it provides dependencies
- it renders the current workflow node
- it collects workflow outputs
- it maps outputs back to app behavior, including generated destinations

## Choose Route, Screen State, Or Workflow

| Need | Use |
| --- | --- |
| User can navigate to a screen or share its path | Laydr route and generated destination |
| One screen has simple local UI state | `remember`, `rememberSaveable`, or app-owned state holder |
| Screen state must survive Nav3 entry lifetime | app-owned ViewModel or retained state through `NavDisplay` decorators |
| A matched route owns a private multi-step flow | route-local workflow |
| Several app areas share state or policy | app-owned store, service, or shell code |

Do not use workflow for route parameters, top-level navigation, global app
state, auth policy, or as a ViewModel replacement.

## Add The Runtime

Add workflow only to modules that host route-local workflow state:

```kotlin
commonMain.dependencies {
    implementation(libs.laydr.workflow)
}
```

`laydr-workflow` owns workflow runtime contracts and Compose host APIs. Laydr
route generation does not generate workflow nodes, renderers, reducers, test
skeletons, or host glue.

## Route-Local Shape

Keep the route declaration in `Route.kt`:

```kotlin
// routes/cart/checkout/review/Route.kt
internal val Route = LaydrRouteDef.screen(content = ::Screen)
```

Put workflow implementation beside the route or in a nearby feature package:

```text
routes/cart/checkout/review/
  Route.kt
  Screen.kt
  Workflow.kt
```

`Workflow.kt` is ordinary app-owned Kotlin. It does not change generated route
output.

## Build A Review Workflow

The smallest useful workflow has:

- state that renderers can observe
- typed UI events
- outputs emitted by nodes
- one or more nodes
- a `LaydrWorkflow` that handles node outputs and mutates its private stack
- a renderer that maps node types to composables
- a route screen that hosts the workflow and maps final outputs to app behavior

### State, Events, And Outputs

Model state, events, and outputs as ordinary Kotlin types:

```kotlin
internal data class ReviewOrderState(
    val items: List<CartItem>,
    val totalCents: Int,
)

internal sealed interface ReviewOrderEvent {
    data object ReturnToCart : ReviewOrderEvent
    data object PlaceOrder : ReviewOrderEvent
}

internal sealed interface ConfirmOrderEvent {
    data object EditReview : ConfirmOrderEvent
    data object SubmitOrder : ConfirmOrderEvent
}

internal sealed interface CheckoutReviewOutput {
    data object ReturnToCart : CheckoutReviewOutput
    data object RequestSubmit : CheckoutReviewOutput
    data object CancelSubmit : CheckoutReviewOutput
    data object OrderPlaced : CheckoutReviewOutput
}
```

Some outputs can be workflow-local stack commands. In this example,
`RequestSubmit` pushes the confirm node and `CancelSubmit` returns to the
review node. Those outputs still reach the route collector, so the route should
ignore stack-only outputs.

### Nodes Own Headless State

Nodes are not routes or screens. They expose state, accept typed events, and
emit outputs:

```kotlin
internal class ReviewOrderNode(
    parentScope: CoroutineScope,
    initialState: ReviewOrderState,
) : LaydrStatefulWorkflowNode<
    ReviewOrderState,
    ReviewOrderEvent,
    CheckoutReviewOutput,
>(
    parentScope = parentScope,
    initialState = initialState,
) {
    override fun onEvent(event: ReviewOrderEvent) {
        when (event) {
            ReviewOrderEvent.ReturnToCart ->
                tryEmitOutput(CheckoutReviewOutput.ReturnToCart)
            ReviewOrderEvent.PlaceOrder -> {
                if (state.value.items.isNotEmpty()) {
                    tryEmitOutput(CheckoutReviewOutput.RequestSubmit)
                }
            }
        }
    }
}

internal class ConfirmOrderNode(
    parentScope: CoroutineScope,
    reviewState: ReviewOrderState,
) : LaydrStatefulWorkflowNode<
    ReviewOrderState,
    ConfirmOrderEvent,
    CheckoutReviewOutput,
>(
    parentScope = parentScope,
    initialState = reviewState,
) {
    override fun onEvent(event: ConfirmOrderEvent) {
        when (event) {
            ConfirmOrderEvent.EditReview ->
                tryEmitOutput(CheckoutReviewOutput.CancelSubmit)
            ConfirmOrderEvent.SubmitOrder ->
                tryEmitOutput(CheckoutReviewOutput.OrderPlaced)
        }
    }
}
```

Use `tryEmitOutput(...)` for immediate UI events. Use `emitOutput(...)` from a
coroutine when emitting should suspend.

### The Workflow Owns The Private Stack

The workflow starts with a root node. It can push, replace, or remove nodes in
response to outputs:

```kotlin
internal class CheckoutReviewWorkflow(
    private val workflowScope: CoroutineScope,
    initialState: ReviewOrderState,
) : LaydrWorkflow<CheckoutReviewOutput>(
    scope = workflowScope,
    rootNode = ReviewOrderNode(
        parentScope = workflowScope,
        initialState = initialState,
    ),
) {
    override fun onNodeOutput(
        node: LaydrWorkflowNode<*, *, CheckoutReviewOutput>,
        output: CheckoutReviewOutput,
    ) {
        when (output) {
            CheckoutReviewOutput.RequestSubmit -> {
                val reviewNode = node as? ReviewOrderNode ?: return
                push(
                    ConfirmOrderNode(
                        parentScope = workflowScope,
                        reviewState = reviewNode.state.value,
                    ),
                )
            }
            CheckoutReviewOutput.CancelSubmit -> back()
            CheckoutReviewOutput.ReturnToCart,
            CheckoutReviewOutput.OrderPlaced -> Unit
        }
    }
}
```

`back()` is workflow-local. Platform Back and app Back remain app-owned. If a
route wants Back to step through workflow nodes first, the route or shell calls
`workflow.back()` explicitly when `workflow.canBack()` is true.

### Render Nodes Explicitly

Register UI for each node type:

```kotlin
internal val CheckoutReviewRenderer:
    LaydrWorkflowRenderer<CheckoutReviewOutput> =
    laydrWorkflowRenderer {
        register<ReviewOrderNode> { node ->
            val state by node.state.collectAsState()
            ReviewOrderScreen(
                state = state,
                onReturnToCart = {
                    node.onEvent(ReviewOrderEvent.ReturnToCart)
                },
                onPlaceOrder = {
                    node.onEvent(ReviewOrderEvent.PlaceOrder)
                },
            )
        }

        register<ConfirmOrderNode> { node ->
            val state by node.state.collectAsState()
            ConfirmOrderScreen(
                state = state,
                onEditReview = {
                    node.onEvent(ConfirmOrderEvent.EditReview)
                },
                onSubmitOrder = {
                    node.onEvent(ConfirmOrderEvent.SubmitOrder)
                },
            )
        }
    }
```

Renderer registration is explicit so node UI stays inspectable app code.
Laydr does not generate renderer skeletons.

### Host From The Route Screen

Create and host the workflow from the route entry composable:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
    dependencies: CheckoutDependencies,
    navigation: CheckoutNavigation,
) {
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        CheckoutReviewWorkflow(
            workflowScope = scope,
            initialState = dependencies.store.reviewOrderState(route),
        )
    }

    CollectLaydrWorkflowOutputs(workflow = workflow) { output ->
        when (output) {
            CheckoutReviewOutput.ReturnToCart ->
                navigation.replace(LaydrRoutes.Cart.destination())
            CheckoutReviewOutput.OrderPlaced ->
                navigation.replace(
                    LaydrRoutes.Cart.Checkout.Confirmation.destination(),
                )
            CheckoutReviewOutput.RequestSubmit,
            CheckoutReviewOutput.CancelSubmit -> Unit
        }
    }

    LaydrWorkflowHost(
        workflow = workflow,
        renderer = CheckoutReviewRenderer,
    )
}
```

`CheckoutDependencies` and `CheckoutNavigation` are app-owned. They can be
parameters, `CompositionLocal` values, Koin injections, or any other app DI
pattern. See [Route Dependencies](route-dependencies.md) for dependency
ownership patterns.

## Test Workflows Headlessly

Workflow nodes are headless, so you can test workflow behavior without Compose:

```kotlin
@Test
fun placeOrderOpensConfirmThenEmitsOrderPlaced() = runTest {
    val workflow = CheckoutReviewWorkflow(
        workflowScope = this,
        initialState = ReviewOrderState(
            items = listOf(CartItem("Bag", quantity = 1)),
            totalCents = 12_900,
        ),
    )
    val scenario = LaydrWorkflowScenario(workflow, this).start()

    scenario
        .updateTopNode<ReviewOrderNode> {
            onEvent(ReviewOrderEvent.PlaceOrder)
        }
        .awaitTopNodeIs<ConfirmOrderNode>()
        .assertStackSize(2)
        .assertNextOutput(CheckoutReviewOutput.RequestSubmit)
        .updateTopNode<ConfirmOrderNode> {
            onEvent(ConfirmOrderEvent.SubmitOrder)
        }
        .assertNextOutput(CheckoutReviewOutput.OrderPlaced)

    scenario.finish()
}
```

The scenario observes all workflow outputs, including stack-only outputs that
the route screen may ignore.

## Build Checklist

- Keep `Route.kt` as the route declaration.
- Add `laydr-workflow` only when a route hosts workflow state.
- Put state, events, outputs, nodes, workflow, and renderer in app-owned code.
- Resolve route dependencies before creating the workflow.
- Use `rememberLaydrWorkflow(key = route)` so a route change creates a new
  workflow instance.
- Collect outputs in the route entry and map them to app behavior.
- Use generated destinations for app navigation triggered by workflow outputs.
- Test node transitions and outputs with `LaydrWorkflowScenario`.
- Run `checkLaydrRoutes` and the relevant KMP compile task.

## Validation

`checkLaydrRoutes` and `generateLaydrRoutes` validate the route tree and
`Route.kt` declarations. Workflow files do not change generated route output.

If workflow host APIs are missing, add `laydr-workflow`. If outputs do not
trigger app behavior, check the route-local output collection code first. If a
node does not render, check that its type is registered in the workflow
renderer.

For a complete working app pattern, see the checkout review workflow in
[`examples/nav3-kmp-shopping`](../../examples/nav3-kmp-shopping/).
