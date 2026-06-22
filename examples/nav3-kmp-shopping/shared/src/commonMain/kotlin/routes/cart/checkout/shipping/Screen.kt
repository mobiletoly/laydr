package routes.cart.checkout.shipping

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import dev.goquick.laydr.nav3.kmp.LaydrNavLaunch
import dev.goquick.laydr.nav3.kmp.pushForResult
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Shipping.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val routePath = route.path
    RoutePage(
        title = "Shipping",
        subtitle = "Checkout selects an address through a reusable Profile route result.",
        routePath = routePath,
    ) {
        store.selectedCheckoutAddress()?.let { address ->
            ItemCard(
                title = address.label,
                subtitle = address.body,
                meta = "Selected shipping address",
                selected = true,
            )
        }
        store.checkoutFeedbackText?.let { feedback ->
            ItemCard(
                title = "Checkout update",
                subtitle = feedback,
                meta = "App-owned state",
            )
        }
        ActionRow {
            ShoppingButton(
                label = "Select address",
                onClick = {
                    navigator.pushForResult<AddressSelectionResult>(
                        launch = LaydrNavLaunch(
                            destination = LaydrRoutes.Profile.Addresses.Select.destination(),
                        ),
                        onCancel = {
                            store.showCheckoutFeedback("Address selection canceled.")
                        },
                    ) { result ->
                        store.selectCheckoutAddress(result.addressId)
                    }
                },
            )
            ShoppingButton(
                label = "Continue",
                onClick = { navigator.push(LaydrRoutes.Cart.Checkout.Payment.destination()) },
                primary = true,
            )
        }
    }
}
