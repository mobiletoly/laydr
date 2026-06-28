# Route-Local Workflow

Use this reference when a KMP Laydr route needs private, headless, multi-step
feature state under an already matched route.

Workflow is app-owned implementation code. Laydr does not generate workflow
nodes, renderers, host glue, DI modules, reducers, ViewModels, repositories,
test skeletons, or navigation policy.

## When To Use

Use workflow when a matched route owns a private flow that should not become
app-addressable destinations, such as review -> confirm inside a checkout
route.

Do not use workflow as a replacement for generated destinations, Nav3 stacks,
tabs, deep links, platform Back, ViewModels, DI, reducers, repositories, or
app-shell state.

## Before Editing

1. Inspect the route root, usually `src/commonMain/kotlin/routes`.
2. Inspect the target route package and its `Route.kt`.
3. Add `laydr-workflow` only if the module does not already depend on it:

```kotlin
commonMain.dependencies {
    implementation(libs.laydr.workflow)
}
```

4. Read `references/dependencies.md` if the workflow needs app services, DI,
   stores, shell callbacks, or previewable default dependencies.

## Implementation Recipe

Keep `Route.kt` as the route declaration:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
```

Add workflow code in the route package or a nearby feature package. A good
route-local shape is:

```text
routes/feature/review/
  Route.kt
  Screen.kt
  Workflow.kt
```

Build the workflow in this order:

1. Define app-owned state, event, and output types.
2. Create headless nodes with `LaydrStatefulWorkflowNode`.
3. Create a `LaydrWorkflow` root and handle node outputs in
   `onNodeOutput(...)`.
4. Mutate only the private workflow stack with `push(...)`, `replaceTop(...)`,
   `replaceAll(...)`, or `back()`.
5. Register node UI with `laydrWorkflowRenderer { register<Node> { ... } }`.
6. Host the workflow from route-owned `Screen(...)` with
   `rememberLaydrWorkflow`, `CollectLaydrWorkflowOutputs`, and
   `LaydrWorkflowHost`.
7. Map workflow outputs to app behavior in the route screen. Use generated
   Laydr destinations for navigation.

Skeleton:

```kotlin
internal data class ReviewState(val canContinue: Boolean)

internal sealed interface ReviewEvent {
    data object Continue : ReviewEvent
}

internal sealed interface ConfirmEvent {
    data object Back : ConfirmEvent
    data object Finish : ConfirmEvent
}

internal sealed interface FeatureOutput {
    data object RequestConfirm : FeatureOutput
    data object CancelConfirm : FeatureOutput
    data object Finished : FeatureOutput
}

internal class ReviewNode(
    parentScope: CoroutineScope,
    initialState: ReviewState,
) : LaydrStatefulWorkflowNode<ReviewState, ReviewEvent, FeatureOutput>(
    parentScope = parentScope,
    initialState = initialState,
) {
    override fun onEvent(event: ReviewEvent) {
        when (event) {
            ReviewEvent.Continue ->
                tryEmitOutput(FeatureOutput.RequestConfirm)
        }
    }
}

internal class ConfirmNode(
    parentScope: CoroutineScope,
    reviewState: ReviewState,
) : LaydrStatefulWorkflowNode<ReviewState, ConfirmEvent, FeatureOutput>(
    parentScope = parentScope,
    initialState = reviewState,
) {
    override fun onEvent(event: ConfirmEvent) {
        when (event) {
            ConfirmEvent.Back ->
                tryEmitOutput(FeatureOutput.CancelConfirm)
            ConfirmEvent.Finish ->
                tryEmitOutput(FeatureOutput.Finished)
        }
    }
}

internal class FeatureWorkflow(
    private val workflowScope: CoroutineScope,
    initialState: ReviewState,
) : LaydrWorkflow<FeatureOutput>(
    scope = workflowScope,
    rootNode = ReviewNode(workflowScope, initialState),
) {
    override fun onNodeOutput(
        node: LaydrWorkflowNode<*, *, FeatureOutput>,
        output: FeatureOutput,
    ) {
        when (output) {
            FeatureOutput.RequestConfirm -> {
                val reviewNode = node as? ReviewNode ?: return
                push(ConfirmNode(workflowScope, reviewNode.state.value))
            }
            FeatureOutput.CancelConfirm -> back()
            FeatureOutput.Finished -> Unit
        }
    }
}

internal val FeatureRenderer: LaydrWorkflowRenderer<FeatureOutput> =
    laydrWorkflowRenderer {
        register<ReviewNode> { node ->
            val state by node.state.collectAsState()
            ReviewScreen(
                state = state,
                onContinue = { node.onEvent(ReviewEvent.Continue) },
            )
        }
        register<ConfirmNode> { node ->
            val state by node.state.collectAsState()
            ConfirmScreen(
                state = state,
                onBack = { node.onEvent(ConfirmEvent.Back) },
                onFinish = { node.onEvent(ConfirmEvent.Finish) },
            )
        }
    }

@Composable
internal fun Screen(route: LaydrRoutes.Feature.Review.Destination) {
    val dependencies = LocalFeatureDependencies.current
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        FeatureWorkflow(
            workflowScope = scope,
            initialState = dependencies.store.reviewState(route),
        )
    }

    CollectLaydrWorkflowOutputs(workflow = workflow) { output ->
        when (output) {
            FeatureOutput.Finished ->
                dependencies.navigator.push(
                    LaydrRoutes.Feature.Done.destination(),
                )
            FeatureOutput.RequestConfirm,
            FeatureOutput.CancelConfirm -> Unit
        }
    }

    LaydrWorkflowHost(workflow = workflow, renderer = FeatureRenderer)
}
```

Stack-only outputs still reach `CollectLaydrWorkflowOutputs`; ignore them in
the route screen if they are only used for private stack transitions.

Platform Back is not automatic. If a route should back through workflow nodes,
wire app-owned Back handling to call `workflow.back()` when
`workflow.canBack()` is true.

## Testing And Validation

Use `LaydrWorkflowScenario` for headless workflow behavior tests:

```kotlin
val workflow = FeatureWorkflow(this, initialState)
val scenario = LaydrWorkflowScenario(workflow, this).start()

scenario
    .updateTopNode<ReviewNode> {
        onEvent(ReviewEvent.Continue)
    }
    .awaitTopNodeIs<ConfirmNode>()
    .assertStackSize(2)
```

Validate the app after workflow edits:

```bash
./gradlew :shared:checkLaydrRoutes
./gradlew :shared:compileKotlinDesktop
```

Use project-specific KMP compile targets when the changed app uses different
module names or platforms. If route validation fails, fix `Route.kt` or the
route tree before changing workflow code. Workflow files do not affect
generated route output.
