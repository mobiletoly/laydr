package routes.orders.by_order_id.tracking

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Orders.ByOrderId.Tracking.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val routePath = route.path
    val orderId = route.orderId.value
    val order = store.findOrder(orderId)
    if (order == null) {
        MissingRouteState(
            title = "Tracking unavailable",
            message = "No fake order exists for id $orderId.",
            routePath = routePath,
        )
    } else {
        RoutePage(
            title = "Tracking ${order.id}",
            subtitle = "Nested order tracking keeps the order id parameter.",
            routePath = routePath,
        ) {
            DetailRow(label = "Status", value = order.status)
            DetailRow(label = "Tracking", value = order.tracking)
        }
    }
}