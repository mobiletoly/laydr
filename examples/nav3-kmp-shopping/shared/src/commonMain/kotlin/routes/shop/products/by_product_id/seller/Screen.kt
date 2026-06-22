package routes.shop.products.by_product_id.seller

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Shop.Products.ByProductId.Seller.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val routePath = route.path
    val productId = route.productId.value
    val product = store.findProduct(productId)
    if (product == null) {
        MissingRouteState(
            title = "Seller unavailable",
            message = "No fake product exists for id $productId.",
            routePath = routePath,
        )
    } else {
        RoutePage(
            title = product.seller,
            subtitle = "Seller details are a nested route under the product detail branch.",
            routePath = routePath,
        ) {
            DetailRow(label = "Featured product", value = product.name)
            DetailRow(label = "Fulfillment", value = "Ships from regional inventory")
            DetailRow(label = "Support", value = "Replies within one business day")
        }
    }
}