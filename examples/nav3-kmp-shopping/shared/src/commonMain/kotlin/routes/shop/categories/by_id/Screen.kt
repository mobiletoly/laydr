package routes.shop.categories.by_id

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Shop.Categories.ById.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    val categoryId = route.id.value
    val category = store.findCategory(categoryId)
    if (category == null) {
        MissingRouteState(
            title = "Category not found",
            message = "No fake category exists for id $categoryId.",
            routePath = routePath,
        )
    } else {
        RoutePage(
            title = category.name,
            subtitle = category.summary,
            routePath = routePath,
        ) {
            store.productsForCategory(category.id).forEach { product ->
                ProductCard(
                    product = product,
                    onClick = {
                        navigator.push(
                            LaydrRoutes.Shop.Products.ByProductId.destination(
                                productId = LaydrRoutes.Shop.Products.ByProductId.productId(product.id),
                            ),
                        )
                    },
                )
            }
        }
    }
}