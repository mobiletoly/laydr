# Route-Local Workflow

`laydr-workflow` provides runtime and Compose host APIs for private feature
workflows under an already matched Laydr route. A workflow owns nodes, node
state, outputs, and a private node stack. It does not own app routes, tabs,
deep links, Nav3 state, platform Back, or app-shell rendering.

Use workflow when a route needs local multi-step state that should be headless
and testable but should not become app-addressable destinations.

## Modules

- `laydr-workflow` owns runtime contracts, headless testing helpers, and
  Compose host APIs. Workflow nodes remain headless, but the artifact exports
  Compose runtime for public composable APIs.
- `laydr-codegen` generates route and Compose route-definition helpers only.
  It does not generate workflow nodes, renderers, or workflow host glue.
- `laydr-gradle-plugin` delegates route validation and generation to
  `laydr-codegen`.

## App Shape

Workflow hosting is ordinary app-owned Compose code under a generated route:

```kotlin
package routes.cart.checkout.review

internal val Route = LaydrRouteDef.screen(content = ::Screen)

@Composable
internal fun Screen(route: LaydrRoutes.Cart.Checkout.Review.Destination) {
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        CheckoutReviewWorkflow(route = route, scope = scope)
    }
    LaydrWorkflowHost(
        workflow = workflow,
        renderer = checkoutReviewRenderer,
    ) { output ->
        handleCheckoutReviewOutput(route, output)
    }
}
```

## Validation Rules

`checkLaydrRoutes` and `generateLaydrRoutes` validate the route tree and route
declarations. Workflow files are ordinary app-owned implementation files inside
declared route packages. They do not change generated output.

## Runtime Boundaries

Workflow nodes are runtime objects, not route keys or destinations. UI code
sends typed events to nodes. Nodes update state and emit outputs. Workflows may
mutate their own private stack in response to outputs. App navigation remains
an app-owned mapping from app-facing workflow outputs to generated Laydr
destinations.

Laydr intentionally does not generate workflow nodes, reducers, ViewModels,
repositories, DI wiring, renderer skeletons, test skeletons, or business
logic.
