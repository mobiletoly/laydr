package routes.search.results

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Search.Results.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    RoutePage(
        title = "Search results",
        subtitle = "Results can jump into the Shop section by generated product destination.",
        routePath = routePath,
    ) {
        store.products.filter { product -> product.priceCents < 10000 }.forEach { product ->
            ProductCard(
                product = product,
                onClick = {
                    navigator.pushWithReturn(
                        LaydrRoutes.Shop.Products.ByProductId.destination(
                            productId = LaydrRoutes.Shop.Products.ByProductId.productId(product.id),
                        ),
                    )
                },
            )
        }
    }
}