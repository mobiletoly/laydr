package routes.cart.checkout.confirmation

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Confirmation.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    val orderId = store.latestOrderId
    RoutePage(
        title = "Order confirmed",
        subtitle = "Confirmation can replace into an Orders detail destination.",
        routePath = routePath,
    ) {
        ItemCard(
            title = orderId ?: "No order yet",
            subtitle = "The latest order id is stored in app-owned example state.",
            meta = "Checkout complete",
        )
        ShoppingButton(
            label = "View order",
            onClick = {
                if (orderId != null) {
                    navigator.pushWithReturn(
                        LaydrRoutes.Orders.ByOrderId.destination(
                            orderId = LaydrRoutes.Orders.ByOrderId.orderId(orderId),
                        ),
                    )
                }
            },
            primary = true,
            enabled = orderId != null,
        )
    }
}