# Route-Local Workflow

Use this reference when a route needs private, headless, multi-step feature
state under an already matched route.

## When To Use

Use workflow for route-local flows that should not become app-addressable
destinations.

Do not use workflow as a replacement for app routing, Nav3 stacks, deep links,
tabs, platform Back, ViewModels, repositories, DI, reducers, or app-shell
state.

## Route Shape

`Route.kt` still owns the route declaration:

```kotlin
internal val Route = LaydrRouteDef.screen(content = ::Screen)
```

Host the workflow from app-owned screen content:

```kotlin
@Composable
internal fun Screen(route: LaydrRoutes.Feature.Destination) {
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        FeatureWorkflow(route = route, scope = scope)
    }
    CollectLaydrWorkflowOutputs(workflow = workflow) { output ->
        handleFeatureOutput(route, output)
    }
    LaydrWorkflowHost(workflow = workflow, renderer = featureRenderer)
}
```

Resolve DI or app services with ordinary Compose or app DI before creating the
workflow. Prefer the route dependency pattern from `dependencies.md` when the
screen needs defaulted dependencies for previews. Workflow nodes, renderer
details, output handling, and business logic remain app-owned.

## Outputs

Map workflow outputs back to app behavior in route-local screen code. If an
output should navigate, use generated Laydr destinations through app-owned
navigation code.

## Validation

`checkLaydrRoutes` validates route declarations. Workflow files are ordinary
app-owned Kotlin files inside declared route packages and do not change
generated output.
