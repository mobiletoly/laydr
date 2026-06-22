package routes.profile.addresses.select

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.ActionRow
import dev.goquick.laydr.examples.nav3kmpshopping.AddressSelectionResult
import dev.goquick.laydr.examples.nav3kmpshopping.ItemCard
import dev.goquick.laydr.examples.nav3kmpshopping.RoutePage
import dev.goquick.laydr.examples.nav3kmpshopping.ShoppingButton
import dev.goquick.laydr.examples.nav3kmpshopping.ShoppingContext
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import dev.goquick.laydr.nav3.kmp.requireLaydrNavResultSink
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.Addresses.Select.Destination,
) {
    val app: ShoppingContext = koinInject()
    val resultSink = requireLaydrNavResultSink<AddressSelectionResult>()

    RoutePage(
        title = "Select address",
        subtitle = "Choose a checkout shipping address.",
        routePath = route.path,
    ) {
        app.store.customerAddresses.forEach { address ->
            ItemCard(
                title = address.label,
                subtitle = address.body,
                meta = address.id,
                selected = address.id == app.store.selectedCheckoutAddressId,
                onClick = {
                    resultSink.complete(AddressSelectionResult(addressId = address.id))
                    app.navigator.back()
                },
            )
        }
        ActionRow {
            ShoppingButton(
                label = "Cancel",
                onClick = {
                    resultSink.cancel()
                    app.navigator.back()
                },
            )
        }
    }
}
