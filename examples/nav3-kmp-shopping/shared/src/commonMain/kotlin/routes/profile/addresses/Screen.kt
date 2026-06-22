package routes.profile.addresses

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.Addresses.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val routePath = route.path
    RoutePage(
        title = "Addresses",
        subtitle = "Checkout links here through a generated profile destination.",
        routePath = routePath,
    ) {
        store.customerAddresses.forEach { address ->
            ItemCard(title = address.label, subtitle = address.body, meta = address.id)
        }
    }
}