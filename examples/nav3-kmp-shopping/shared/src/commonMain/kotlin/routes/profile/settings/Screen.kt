package routes.profile.settings

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.Settings.Destination,
) {
    val app: ShoppingContext = koinInject()
    val routePath = route.path
    RoutePage(
        title = "Settings",
        subtitle = "A simple profile child route that rounds out the account branch.",
        routePath = routePath,
    ) {
        DetailRow(label = "Notifications", value = "Order and delivery updates enabled")
        DetailRow(label = "Currency", value = "USD")
        DetailRow(label = "Region", value = "United States")
    }
}