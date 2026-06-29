# Route-Local Workflow

Use this when a matched Laydr route owns private, testable, multi-step feature
state. A route answers "where is the user in the app?" A workflow answers
"what private step is this route showing right now?"

Workflow is app-owned implementation code. Laydr does not generate nodes,
renderers, host glue, DI modules, reducers, ViewModels, repositories, test
skeletons, or navigation policy.

## Contents

- when to use workflow
- setup
- route-local shape
- implementation recipe
- testing

## When To Use

Use workflow for private route-local steps, such as review -> submit inside
`/cart/checkout/review`, when those steps should not become app-addressable
destinations.

Do not use workflow as a replacement for generated destinations, Nav3 stacks,
tabs, deep links, platform Back, ViewModels, DI, reducers, repositories, or
app-shell state.

## Setup

Inspect the route root and the target route package before editing. Add
`laydr-workflow` only to modules that host workflow state.

KMP route module:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.laydr.workflow)
        }
    }
}
```

Android-only route module:

```kotlin
dependencies {
    implementation(libs.laydr.workflow)
}
```

Read `dependencies.md` when the workflow needs app services, DI, stores, shell
callbacks, or previewable default dependencies.

## Route-Local Shape

Keep `Route.kt` as the route declaration:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
```

Put workflow code beside the route or in a nearby feature package:

```text
routes/cart/checkout/review/
  Route.kt
  Screen.kt
  Workflow.kt
```

`Workflow.kt` is ordinary app-owned Kotlin. It does not change generated route
output.

## Implementation Recipe

Build the workflow in this order:

1. Define state, events, and outputs.
2. Create headless nodes with `LaydrStatefulWorkflowNode`.
3. Create a `LaydrWorkflow` root and handle node outputs in
   `onNodeOutput(...)`.
4. Mutate only the private workflow stack with `push(...)`, `replaceTop(...)`,
   `replaceAll(...)`, or `back()`.
5. Register node UI with `laydrWorkflowRenderer { register<Node> { ... } }`.
6. Host from route-owned `Screen(...)` with `rememberLaydrWorkflow`,
   `CollectLaydrWorkflowOutputs`, and `LaydrWorkflowHost`.
7. Map workflow outputs to app behavior in the route screen. Use generated
   Laydr destinations for navigation.

Skeleton:

```kotlin
internal data class ReviewState(val canContinue: Boolean)

internal sealed interface ReviewEvent {
    data object Continue : ReviewEvent
}

internal sealed interface SubmitEvent {
    data object EditReview : SubmitEvent
    data object Submit : SubmitEvent
}

internal sealed interface CheckoutOutput {
    data object RequestSubmit : CheckoutOutput
    data object CancelSubmit : CheckoutOutput
    data object Finished : CheckoutOutput
}

internal class ReviewNode(
    parentScope: CoroutineScope,
    initialState: ReviewState,
) : LaydrStatefulWorkflowNode<ReviewState, ReviewEvent, CheckoutOutput>(
    parentScope = parentScope,
    initialState = initialState,
) {
    override fun onEvent(event: ReviewEvent) {
        when (event) {
            ReviewEvent.Continue ->
                tryEmitOutput(CheckoutOutput.RequestSubmit)
        }
    }
}

internal class SubmitNode(
    parentScope: CoroutineScope,
    reviewState: ReviewState,
) : LaydrStatefulWorkflowNode<ReviewState, SubmitEvent, CheckoutOutput>(
    parentScope = parentScope,
    initialState = reviewState,
) {
    override fun onEvent(event: SubmitEvent) {
        when (event) {
            SubmitEvent.EditReview ->
                tryEmitOutput(CheckoutOutput.CancelSubmit)
            SubmitEvent.Submit ->
                tryEmitOutput(CheckoutOutput.Finished)
        }
    }
}

internal class CheckoutWorkflow(
    private val workflowScope: CoroutineScope,
    initialState: ReviewState,
) : LaydrWorkflow<CheckoutOutput>(
    scope = workflowScope,
    rootNode = ReviewNode(workflowScope, initialState),
) {
    override fun onNodeOutput(
        node: LaydrWorkflowNode<*, *, CheckoutOutput>,
        output: CheckoutOutput,
    ) {
        when (output) {
            CheckoutOutput.RequestSubmit -> {
                val reviewNode = node as? ReviewNode ?: return
                push(SubmitNode(workflowScope, reviewNode.state.value))
            }
            CheckoutOutput.CancelSubmit -> back()
            CheckoutOutput.Finished -> Unit
        }
    }
}
```

Renderer:

```kotlin
internal val CheckoutRenderer: LaydrWorkflowRenderer<CheckoutOutput> =
    laydrWorkflowRenderer {
        register<ReviewNode> { node ->
            val state by node.state.collectAsState()
            ReviewScreen(
                state = state,
                onContinue = { node.onEvent(ReviewEvent.Continue) },
            )
        }
        register<SubmitNode> { node ->
            val state by node.state.collectAsState()
            SubmitScreen(
                state = state,
                onEditReview = { node.onEvent(SubmitEvent.EditReview) },
                onSubmit = { node.onEvent(SubmitEvent.Submit) },
            )
        }
    }
```

Route host:

```kotlin
@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
    dependencies: CheckoutDependencies = koinInject(),
    navigation: CheckoutNavigation = LocalCheckoutNavigation.current,
) {
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        CheckoutWorkflow(
            workflowScope = scope,
            initialState = dependencies.store.reviewState(route),
        )
    }

    CollectLaydrWorkflowOutputs(workflow = workflow) { output ->
        when (output) {
            CheckoutOutput.Finished ->
                navigation.replace(
                    LaydrRoutes.Cart.Checkout.Confirmation.destination(),
                )
            CheckoutOutput.RequestSubmit,
            CheckoutOutput.CancelSubmit -> Unit
        }
    }

    LaydrWorkflowHost(workflow = workflow, renderer = CheckoutRenderer)
}
```

Stack-only outputs still reach `CollectLaydrWorkflowOutputs`; ignore them in
the route screen when they only drive private workflow stack transitions.

Platform Back is not automatic. If a route should Back through workflow nodes,
wire app-owned Back handling to call `workflow.back()` when
`workflow.canBack()` is true.

## Testing

Use `LaydrWorkflowScenario` for headless tests:

```kotlin
@Test
fun continueOpensSubmitThenFinishes() = runTest {
    val workflow = CheckoutWorkflow(
        workflowScope = this,
        initialState = ReviewState(canContinue = true),
    )
    val scenario = LaydrWorkflowScenario(workflow, this).start()

    scenario
        .updateTopNode<ReviewNode> {
            onEvent(ReviewEvent.Continue)
        }
        .awaitTopNodeIs<SubmitNode>()
        .assertStackSize(2)
        .assertNextOutput(CheckoutOutput.RequestSubmit)
        .updateTopNode<SubmitNode> {
            onEvent(SubmitEvent.Submit)
        }
        .assertNextOutput(CheckoutOutput.Finished)

    scenario.finish()
}
```

Validate route source and compile the affected module. Workflow files do not
change generated route output, so if route validation fails, fix `Route.kt` or
the route tree before changing workflow code.
