package routes.cart

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    val items = store.cartItems()
    RoutePage(
        title = "Cart",
        subtitle = "Cart owns checkout as a nested flow instead of a top-level section.",
        routePath = routePath,
    ) {
        if (items.isEmpty()) {
            ItemCard(
                title = "Your cart is empty",
                subtitle = "Add a product from Shop to exercise cross-section navigation.",
                meta = "0 items",
            )
            ShoppingButton(
                label = "Browse shop",
                onClick = { navigator.replace(LaydrRoutes.Shop.destination()) },
                primary = true,
            )
        } else {
            items.forEach { item ->
                ItemCard(
                    title = item.product.name,
                    subtitle = "${item.quantity} x ${formatPrice(item.product.priceCents)}",
                    meta = item.product.seller,
                )
            }
            DetailRow(label = "Total", value = formatPrice(store.cartTotalCents()))
            ShoppingButton(
                label = "Start checkout",
                onClick = { navigator.push(LaydrRoutes.Cart.Checkout.destination()) },
                primary = true,
            )
        }
    }
}