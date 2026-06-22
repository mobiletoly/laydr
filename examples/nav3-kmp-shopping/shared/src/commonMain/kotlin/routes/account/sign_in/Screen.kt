package routes.account.sign_in

import androidx.compose.runtime.Composable
import dev.goquick.laydr.examples.nav3kmpshopping.ActionRow
import dev.goquick.laydr.examples.nav3kmpshopping.ItemCard
import dev.goquick.laydr.examples.nav3kmpshopping.RoutePage
import dev.goquick.laydr.examples.nav3kmpshopping.ShoppingButton
import dev.goquick.laydr.examples.nav3kmpshopping.SignInRoutePayload
import dev.goquick.laydr.examples.nav3kmpshopping.SignInRouteResult
import dev.goquick.laydr.examples.nav3kmpshopping.generated.LaydrRoutes
import dev.goquick.laydr.nav3.kmp.requireLaydrNavPayload
import dev.goquick.laydr.nav3.kmp.requireLaydrNavResultSink
import dev.goquick.laydr.nav3.kmp.requireLaydrNavStackNavigator

@Composable
internal fun Screen(
    route: LaydrRoutes.Account.SignIn.Destination,
) {
    val payload = requireLaydrNavPayload<SignInRoutePayload>()
    val resultSink = requireLaydrNavResultSink<SignInRouteResult>()
    val rootNavigator = requireLaydrNavStackNavigator()

    RoutePage(
        title = "Sign in",
        subtitle = payload.reason,
        routePath = route.path,
    ) {
        ItemCard(
            title = payload.initialEmail ?: "Guest checkout",
            subtitle = "This reusable account route owns its payload and typed result sink.",
            meta = "Root stack entry",
        )
        ActionRow {
            ShoppingButton(
                label = "Cancel",
                onClick = {
                    resultSink.cancel()
                    rootNavigator.back()
                },
            )
            ShoppingButton(
                label = "Sign in",
                onClick = {
                    resultSink.complete(
                        SignInRouteResult(
                            customerId = "customer-demo",
                            displayName = "Demo Shopper",
                        ),
                    )
                    rootNavigator.back()
                },
                primary = true,
            )
        }
    }
}
