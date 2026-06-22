package routes.cart.checkout.payment

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.*
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import dev.goquick.laydr.nav3.kmp.LaydrNavEntryMetadata
import dev.goquick.laydr.nav3.kmp.LaydrNavLaunch
import dev.goquick.laydr.nav3.kmp.pushForResult
import dev.goquick.laydr.nav3.kmp.requireLaydrNavStackNavigator
import org.koin.compose.koinInject

@Composable
internal fun Screen(
    route: LaydrRoutes.Cart.Checkout.Payment.Destination,
) {
    val app: ShoppingContext = koinInject()
    val store = app.store
    val navigator = app.navigator
    val rootNavigator = requireLaydrNavStackNavigator()
    val routePath = route.path
    RoutePage(
        title = "Payment",
        subtitle = "Checkout selects payment and launches root sign-in when the customer is a guest.",
        routePath = routePath,
    ) {
        store.selectedCheckoutPaymentMethod()?.let { method ->
            ItemCard(
                title = method.label,
                subtitle = method.detail,
                meta = "Selected payment method",
                selected = true,
            )
        }
        val customer = store.currentCustomer
        ItemCard(
            title = customer?.displayName ?: "Guest checkout",
            subtitle = if (customer == null) {
                "Sign in before review to attach the order to a fake account."
            } else {
                "Signed in customer id ${customer.id}."
            },
            meta = "Account state",
        )
        store.checkoutFeedbackText?.let { feedback ->
            ItemCard(
                title = "Checkout update",
                subtitle = feedback,
                meta = "App-owned state",
            )
        }
        ActionRow {
            ShoppingButton(
                label = "Select method",
                onClick = {
                    navigator.pushForResult<PaymentMethodSelectionResult>(
                        launch = LaydrNavLaunch(
                            destination = LaydrRoutes.Profile.PaymentMethods.Select.destination(),
                        ),
                        onCancel = {
                            store.showCheckoutFeedback("Payment selection canceled.")
                        },
                    ) { result ->
                        store.selectCheckoutPaymentMethod(result.paymentMethodId)
                    }
                },
            )
            ShoppingButton(
                label = if (store.isGuest) "Sign in to continue" else "Continue",
                onClick = {
                    if (store.isGuest) {
                        rootNavigator.pushForResult<SignInRouteResult>(
                            launch = LaydrNavLaunch(
                                destination = LaydrRoutes.Account.SignIn.destination(),
                                payload = SignInRoutePayload(
                                    initialEmail = null,
                                    reason = "Sign in to attach this checkout to a fake account.",
                                ),
                                entryMetadata = LaydrNavEntryMetadata(
                                    ShoppingEntryPresentationKey to ShoppingEntryPresentation.Overlay,
                                ),
                            ),
                            onCancel = {
                                store.showCheckoutFeedback("Sign-in canceled. Review stays blocked.")
                            },
                        ) { result ->
                            store.applySignInResult(result)
                            navigator.push(LaydrRoutes.Cart.Checkout.Review.destination())
                        }
                    } else {
                        navigator.push(LaydrRoutes.Cart.Checkout.Review.destination())
                    }
                },
                primary = true,
            )
        }
    }
}
