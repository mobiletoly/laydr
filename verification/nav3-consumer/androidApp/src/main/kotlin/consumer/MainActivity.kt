package consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation3.ui.NavDisplay
import consumer.androidx.generated.LaydrNavRoutes
import consumer.androidx.generated.LaydrRoutes
import dev.goquick.laydr.nav3.androidx.LaydrNavLaunch
import dev.goquick.laydr.nav3.androidx.pushForResult

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val stack = LaydrNavRoutes.rememberStack(
                initialDestination = LaydrRoutes.Home.destination(),
                notFoundContent = { error("Unexpected route: $it") },
            )
            NavDisplay(
                stack.backStack,
                onBack = {
                    stack.navigator.pushForResult<String>(
                        LaydrNavLaunch(LaydrRoutes.Home.destination(), payload = "compile inline API"),
                        onCancel = {},
                        onResult = {},
                    )
                },
                entryProvider = stack.entryProvider,
            )
        }
    }
}
