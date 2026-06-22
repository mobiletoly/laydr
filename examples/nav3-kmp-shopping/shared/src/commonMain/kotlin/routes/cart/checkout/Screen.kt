package routes.cart.checkout

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    RoutePage(
        title = "Checkout",
        subtitle = "A nested cart flow with shipping, payment, review, and confirmation routes.",
        routePath = routePath,
    ) {
        DetailRow(label = "Items", value = store.cartItems().size.toString())
        DetailRow(label = "Total", value = formatPrice(store.cartTotalCents()))
        ActionRow {
            ShoppingButton(
                label = "Shipping",
                onClick = { navigator.push(LaydrRoutes.Cart.Checkout.Shipping.destination()) },
                primary = true,
            )
            ShoppingButton(
                label = "Payment",
                onClick = { navigator.push(LaydrRoutes.Cart.Checkout.Payment.destination()) },
            )
            ShoppingButton(
                label = "Review",
                onClick = { navigator.push(LaydrRoutes.Cart.Checkout.Review.destination()) },
            )
        }
    }
}