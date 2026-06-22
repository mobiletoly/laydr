package routes.shop.products.by_product_id

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Shop.Products.ByProductId.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    val productId = route.productId.value
    val product = store.findProduct(productId)
    if (product == null) {
        MissingRouteState(
            title = "Product not found",
            message = "No fake product exists for id $productId.",
            routePath = routePath,
        )
    } else {
        RoutePage(
            title = product.name,
            subtitle = product.summary,
            routePath = routePath,
        ) {
            DetailRow(label = "Price", value = formatPrice(product.priceCents))
            DetailRow(label = "Seller", value = product.seller)
            DetailRow(label = "Rating", value = product.rating)
            DetailRow(label = "Stock", value = product.stockLabel)
            ActionRow {
                ShoppingButton(
                    label = "Add and view cart",
                    onClick = {
                        store.addToCart(product.id)
                        navigator.replace(LaydrRoutes.Cart.destination())
                    },
                    primary = true,
                )
                ShoppingButton(
                    label = "Reviews",
                    onClick = {
                        navigator.push(
                            LaydrRoutes.Shop.Products.ByProductId.Reviews.destination(
                                productId = LaydrRoutes.Shop.Products.ByProductId.Reviews.productId(product.id),
                            ),
                        )
                    },
                )
                ShoppingButton(
                    label = "Seller",
                    onClick = {
                        navigator.push(
                            LaydrRoutes.Shop.Products.ByProductId.Seller.destination(
                                productId = LaydrRoutes.Shop.Products.ByProductId.Seller.productId(product.id),
                            ),
                        )
                    },
                )
            }
        }
    }
}