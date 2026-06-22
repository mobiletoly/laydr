package routes.profile

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.Destination,
) {
    val app: ShoppingContext = koinInject()
    val navigator = app.navigator
    val routePath = route.path
    RoutePage(
        title = "Profile",
        subtitle = "Account routes are regular generated destinations under one section.",
        routePath = routePath,
    ) {
        ItemCard(
            title = "Addresses",
            subtitle = "Shipping addresses used by checkout.",
            meta = "Profile route",
            onClick = { navigator.push(LaydrRoutes.Profile.Addresses.destination()) },
        )
        ItemCard(
            title = "Payment methods",
            subtitle = "Stored cards used by checkout.",
            meta = "Profile route",
            onClick = { navigator.push(LaydrRoutes.Profile.PaymentMethods.destination()) },
        )
        ItemCard(
            title = "Settings",
            subtitle = "Notification and preference placeholders.",
            meta = "Profile route",
            onClick = { navigator.push(LaydrRoutes.Profile.Settings.destination()) },
        )
    }
}