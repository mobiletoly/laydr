package routes.shop.categories

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Shop.Categories.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    RoutePage(
        title = "Categories",
        subtitle = "A grouping route kept explicit so the filesystem stays the navigation map.",
        routePath = routePath,
    ) {
        store.categories.forEach { category ->
            ItemCard(
                title = category.name,
                subtitle = category.summary,
                meta = "${store.productsForCategory(category.id).size} products",
                onClick = {
                    navigator.push(
                        LaydrRoutes.Shop.Categories.ById.destination(
                            id = LaydrRoutes.Shop.Categories.ById.id(category.id),
                        ),
                    )
                },
            )
        }
    }
}