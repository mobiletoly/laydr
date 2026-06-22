package routes.profile.payment_methods

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.PaymentMethods.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val routePath = route.path
    RoutePage(
        title = "Payment methods",
        subtitle = "Checkout links here without raw route strings.",
        routePath = routePath,
    ) {
        store.customerPaymentMethods.forEach { method ->
            ItemCard(title = method.label, subtitle = method.detail, meta = method.id)
        }
    }
}