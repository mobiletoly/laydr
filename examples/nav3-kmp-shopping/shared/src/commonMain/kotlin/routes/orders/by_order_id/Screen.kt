package routes.orders.by_order_id

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Orders.ByOrderId.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    val orderId = route.orderId.value
    val order = store.findOrder(orderId)
    if (order == null) {
        MissingRouteState(
            title = "Order not found",
            message = "No fake order exists for id $orderId.",
            routePath = routePath,
        )
    } else {
        RoutePage(
            title = order.id,
            subtitle = order.status,
            routePath = routePath,
        ) {
            DetailRow(label = "Total", value = formatPrice(order.totalCents))
            DetailRow(label = "Items", value = orderProductNames(store = store, order = order))
            ShoppingButton(
                label = "Track package",
                onClick = {
                    navigator.push(
                        LaydrRoutes.Orders.ByOrderId.Tracking.destination(
                            orderId = LaydrRoutes.Orders.ByOrderId.Tracking.orderId(order.id),
                        ),
                    )
                },
                primary = true,
            )
        }
    }
}