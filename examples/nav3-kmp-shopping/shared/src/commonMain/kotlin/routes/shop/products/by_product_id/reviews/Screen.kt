package routes.shop.products.by_product_id.reviews

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Shop.Products.ByProductId.Reviews.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val routePath = route.path
    val productId = route.productId.value
    val product = store.findProduct(productId)
    if (product == null) {
        MissingRouteState(
            title = "Reviews unavailable",
            message = "No fake product exists for id $productId.",
            routePath = routePath,
        )
    } else {
        RoutePage(
            title = "${product.name} reviews",
            subtitle = "Nested detail route below a dynamic product destination.",
            routePath = routePath,
        ) {
            ItemCard(
                title = "Useful for daily carry",
                subtitle = "The route keeps the selected product id while adding a reviews child.",
                meta = "Rating ${product.rating}",
            )
            ItemCard(
                title = "Good materials",
                subtitle = "Fake review content keeps the example focused on navigation.",
                meta = "Verified buyer",
            )
        }
    }
}