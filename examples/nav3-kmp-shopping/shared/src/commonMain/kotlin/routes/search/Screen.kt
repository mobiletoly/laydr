package routes.search

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Search.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    RoutePage(
        title = "Search",
        subtitle = "Search is sectioned separately and can push typed result routes elsewhere.",
        routePath = routePath,
    ) {
        ItemCard(
            title = "Saved search",
            subtitle = "Everyday carry under ${formatPrice(15000)}",
            meta = "Static example query",
        )
        ActionRow {
            ShoppingButton(
                label = "Show results",
                onClick = { navigator.push(LaydrRoutes.Search.Results.destination()) },
                primary = true,
            )
        }
    }
}
