package routes.shop

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Shop.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    RoutePage(
        title = "Shop",
        subtitle = "Browse categories and featured products from generated route destinations.",
        routePath = routePath,
    ) {
        ActionRow {
            ShoppingButton(
                label = "All products",
                onClick = { navigator.push(LaydrRoutes.Shop.Products.destination()) },
                primary = true,
            )
            ShoppingButton(
                label = "Categories",
                onClick = { navigator.push(LaydrRoutes.Shop.Categories.destination()) },
            )
        }
        SectionTitle(label = "Featured categories")
        store.categories.forEach { category ->
            ItemCard(
                title = category.name,
                subtitle = category.summary,
                meta = "Category ${category.id}",
                onClick = {
                    navigator.push(
                        LaydrRoutes.Shop.Categories.ById.destination(
                            id = LaydrRoutes.Shop.Categories.ById.id(category.id),
                        ),
                    )
                },
            )
        }
        SectionTitle(label = "Featured products")
        store.featuredProducts().forEach { product ->
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