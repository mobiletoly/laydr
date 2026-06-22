package routes.cart.checkout.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import dev.goquick.laydr.workflow.CollectLaydrWorkflowOutputs
import dev.goquick.laydr.workflow.LaydrWorkflow
import dev.goquick.laydr.workflow.LaydrWorkflowNode
import dev.goquick.laydr.workflow.LaydrStatefulWorkflowNode
import dev.goquick.laydr.workflow.LaydrWorkflowHost
import dev.goquick.laydr.workflow.LaydrWorkflowRenderer
import dev.goquick.laydr.workflow.laydrWorkflowRenderer
import dev.goquick.laydr.workflow.rememberLaydrWorkflow
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.koinInject

internal data class ReviewOrderState(
    val routePath: String,
    val items: List<CartItem>,
    val totalCents: Int,
    val customerName: String,
    val shippingAddress: String,
    val paymentMethod: String,
) {
    val itemCount: Int = items.sumOf { item -> item.quantity }
}

internal sealed interface ReviewWorkflowOutput {
    data object ReturnToCart : ReviewWorkflowOutput
    data object RequestSubmit : ReviewWorkflowOutput
    data object CancelSubmit : ReviewWorkflowOutput
    data object OrderPlaced : ReviewWorkflowOutput
}

internal sealed interface ReviewOrderEvent {
    data object ReturnToCart : ReviewOrderEvent
    data object PlaceOrder : ReviewOrderEvent
}

internal sealed interface ConfirmOrderEvent {
    data object EditReview : ConfirmOrderEvent
    data object SubmitOrder : ConfirmOrderEvent
}

internal class ReviewOrderNode(
    parentScope: CoroutineScope,
    store: ShoppingStore,
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
) : LaydrStatefulWorkflowNode<ReviewOrderState, ReviewOrderEvent, ReviewWorkflowOutput>(
    parentScope = parentScope,
    initialState = store.reviewOrderState(route),
) {
    override fun onEvent(event: ReviewOrderEvent) {
        when (event) {
            ReviewOrderEvent.ReturnToCart -> tryEmitOutput(ReviewWorkflowOutput.ReturnToCart)
            ReviewOrderEvent.PlaceOrder -> {
                if (state.value.items.isNotEmpty()) {
                    tryEmitOutput(ReviewWorkflowOutput.RequestSubmit)
                }
            }
        }
    }
}

internal class ConfirmOrderNode(
    parentScope: CoroutineScope,
    private val store: ShoppingStore,
    reviewState: ReviewOrderState,
) : LaydrStatefulWorkflowNode<ReviewOrderState, ConfirmOrderEvent, ReviewWorkflowOutput>(
    parentScope = parentScope,
    initialState = reviewState,
) {
    override fun onEvent(event: ConfirmOrderEvent) {
        when (event) {
            ConfirmOrderEvent.EditReview -> tryEmitOutput(ReviewWorkflowOutput.CancelSubmit)
            ConfirmOrderEvent.SubmitOrder -> {
                if (state.value.items.isEmpty()) {
                    tryEmitOutput(ReviewWorkflowOutput.ReturnToCart)
                } else {
                    store.placeOrder()
                    tryEmitOutput(ReviewWorkflowOutput.OrderPlaced)
                }
            }
        }
    }
}

internal class ReviewWorkflow(
    private val workflowScope: CoroutineScope,
    private val store: ShoppingStore,
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
) : LaydrWorkflow<ReviewWorkflowOutput>(
    scope = workflowScope,
    rootNode = ReviewOrderNode(
        parentScope = workflowScope,
        store = store,
        route = route,
    ),
) {
    override fun onNodeOutput(
        node: LaydrWorkflowNode<*, *, ReviewWorkflowOutput>,
        output: ReviewWorkflowOutput,
    ) {
        when (output) {
            ReviewWorkflowOutput.RequestSubmit -> {
                val reviewNode = node as? ReviewOrderNode ?: return
                push(
                    ConfirmOrderNode(
                        parentScope = workflowScope,
                        store = store,
                        reviewState = reviewNode.state.value,
                    ),
                )
            }
            ReviewWorkflowOutput.CancelSubmit -> back()
            ReviewWorkflowOutput.ReturnToCart,
            ReviewWorkflowOutput.OrderPlaced -> Unit
        }
    }
}

internal val ReviewRenderer: LaydrWorkflowRenderer<ReviewWorkflowOutput> =
    laydrWorkflowRenderer {
        register<ReviewOrderNode> { node ->
            val state by node.state.collectAsState()
            ReviewOrderScreen(
                state = state,
                onReturnToCart = { node.onEvent(ReviewOrderEvent.ReturnToCart) },
                onPlaceOrder = { node.onEvent(ReviewOrderEvent.PlaceOrder) },
            )
        }
        register<ConfirmOrderNode> { node ->
            val state by node.state.collectAsState()
            ConfirmOrderScreen(
                state = state,
                onEditReview = { node.onEvent(ConfirmOrderEvent.EditReview) },
                onSubmitOrder = { node.onEvent(ConfirmOrderEvent.SubmitOrder) },
            )
        }
    }

@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
) {
    val app: ShoppingContext = koinInject()
    val workflow = rememberLaydrWorkflow(key = route) { scope ->
        ReviewWorkflow(
            workflowScope = scope,
            store = app.store,
            route = route,
        )
    }
    CollectLaydrWorkflowOutputs(workflow = workflow) { output ->
        when (output) {
            ReviewWorkflowOutput.ReturnToCart ->
                app.navigator.replace(LaydrRoutes.Cart.destination())
            ReviewWorkflowOutput.OrderPlaced ->
                app.navigator.replace(LaydrRoutes.Cart.Checkout.Confirmation.destination())
            ReviewWorkflowOutput.RequestSubmit,
            ReviewWorkflowOutput.CancelSubmit -> Unit
        }
    }
    LaydrWorkflowHost(workflow = workflow, renderer = ReviewRenderer)
}

@Composable
private fun ReviewOrderScreen(
    state: ReviewOrderState,
    onReturnToCart: () -> Unit,
    onPlaceOrder: () -> Unit,
) {
    RoutePage(
        title = "Review order",
        subtitle = "Review cart lines before creating the order.",
        routePath = state.routePath,
    ) {
        if (state.items.isEmpty()) {
            ItemCard(
                title = "Nothing to review",
                subtitle = "The checkout branch is reachable, but this fake cart is empty.",
                meta = "Cart state",
            )
            ShoppingButton(
                label = "Return to cart",
                onClick = onReturnToCart,
            )
        } else {
            state.items.forEach { item ->
                ItemCard(
                    title = item.product.name,
                    subtitle = "${item.quantity} x ${formatPrice(item.product.priceCents)}",
                    meta = item.product.seller,
                )
            }
            DetailRow(label = "Customer", value = state.customerName)
            DetailRow(label = "Ship to", value = state.shippingAddress)
            DetailRow(label = "Pay with", value = state.paymentMethod)
            DetailRow(label = "Total", value = formatPrice(state.totalCents))
            ShoppingButton(
                label = "Place order",
                onClick = onPlaceOrder,
                primary = true,
            )
        }
    }
}

@Composable
private fun ConfirmOrderScreen(
    state: ReviewOrderState,
    onEditReview: () -> Unit,
    onSubmitOrder: () -> Unit,
) {
    RoutePage(
        title = "Confirm order",
        subtitle = "Submit ${state.itemCount} ${itemCountLabel(state.itemCount)} for fulfillment.",
        routePath = state.routePath,
    ) {
        ItemCard(
            title = "Ready to place order",
            subtitle = "Total ${formatPrice(state.totalCents)}",
            meta = "Checkout confirmation",
        )
        ActionRow {
            ShoppingButton(
                label = "Edit review",
                onClick = onEditReview,
            )
            ShoppingButton(
                label = "Submit order",
                onClick = onSubmitOrder,
                primary = true,
                enabled = state.items.isNotEmpty(),
            )
        }
    }
}

private fun ShoppingStore.reviewOrderState(
    route: LaydrRoutes.Cart.Checkout.Review.Destination,
): ReviewOrderState =
    ReviewOrderState(
        routePath = route.path,
        items = cartItems(),
        totalCents = cartTotalCents(),
        customerName = currentCustomer?.displayName ?: "Guest",
        shippingAddress = selectedCheckoutAddress()?.label ?: "No address selected",
        paymentMethod = selectedCheckoutPaymentMethod()?.label ?: "No payment method selected",
    )

private fun itemCountLabel(itemCount: Int): String =
    if (itemCount == 1) {
        "item"
    } else {
        "items"
    }
