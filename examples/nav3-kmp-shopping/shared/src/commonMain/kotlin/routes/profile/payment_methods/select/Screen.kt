package routes.profile.payment_methods.select

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.ActionRow
import dev.goquick.laydr.examples.nav3kmpshopping.ItemCard
import dev.goquick.laydr.examples.nav3kmpshopping.PaymentMethodSelectionResult
import dev.goquick.laydr.examples.nav3kmpshopping.RoutePage
import dev.goquick.laydr.examples.nav3kmpshopping.ShoppingButton
import dev.goquick.laydr.examples.nav3kmpshopping.ShoppingContext
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import dev.goquick.laydr.nav3.kmp.requireLaydrNavResultSink
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Profile.PaymentMethods.Select.Destination,
) {
    val app: ShoppingContext = koinInject()
    val resultSink = requireLaydrNavResultSink<PaymentMethodSelectionResult>()

    RoutePage(
        title = "Select payment",
        subtitle = "Choose the checkout payment method.",
        routePath = route.path,
    ) {
        app.store.customerPaymentMethods.forEach { method ->
            ItemCard(
                title = method.label,
                subtitle = method.detail,
                meta = method.id,
                selected = method.id == app.store.selectedCheckoutPaymentMethodId,
                onClick = {
                    resultSink.complete(PaymentMethodSelectionResult(paymentMethodId = method.id))
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
