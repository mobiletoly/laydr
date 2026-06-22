package routes.cart.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.goquick.laydr.compose.LaydrLayoutContext
import dev.goquick.laydr.examples.nav3kmpshopping.*
import org.koin.compose.koinInject

@Composable
internal fun Layout(
    context: LaydrLayoutContext,
    content: @Composable () -> Unit,
) {
    val app: ShoppingContext = koinInject()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ItemCard(
            title = context.route.metadata.name,
            subtitle = "Shared checkout layout wrapping shipping, payment, review, and confirmation.",
            meta = "${app.store.cartItems().size} items",
        )
        content()
    }
}
