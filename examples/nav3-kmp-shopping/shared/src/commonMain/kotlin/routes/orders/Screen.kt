package routes.orders

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Orders.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    RoutePage(
        title = "Orders",
        subtitle = "Orders form a second adaptive list/detail scene.",
        routePath = routePath,
    ) {
        store.orders.forEach { order ->
            ItemCard(
                title = order.id,
                subtitle = order.status,
                meta = formatPrice(order.totalCents),
                onClick = {
                    navigator.push(
                        LaydrRoutes.Orders.ByOrderId.destination(
                            orderId = LaydrRoutes.Orders.ByOrderId.orderId(order.id),
                        ),
                    )
                },
            )
        }
    }
}