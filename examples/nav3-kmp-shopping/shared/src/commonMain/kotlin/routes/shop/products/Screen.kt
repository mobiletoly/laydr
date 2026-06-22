package routes.shop.products

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Shop.Products.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    RoutePage(
        title = "All products",
        subtitle = "A route-backed product list that can feed adaptive detail panes.",
        routePath = routePath,
    ) {
        store.products.forEach { product ->
            ProductCard(
                product = product,
                onClick = {
                    navigator.replaceWithReturn(
                        LaydrRoutes.Shop.Products.ByProductId.destination(
                            productId = LaydrRoutes.Shop.Products.ByProductId.productId(product.id),
                        ),
                    )
                },
            )
        }
    }
}