package routes.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.goquick.laydr.examples.nav3androidx.ContactRow
import dev.goquick.laydr.examples.nav3androidx.InMemoryAddressBookRepository
import dev.goquick.laydr.examples.nav3androidx.LocalAddressBookContext
import dev.goquick.laydr.examples.nav3androidx.accent
import dev.goquick.laydr.examples.nav3androidx.border
import dev.goquick.laydr.examples.nav3androidx.generated.LaydrRoutes
import dev.goquick.laydr.examples.nav3androidx.surface
import dev.goquick.laydr.examples.nav3androidx.textMuted
import dev.goquick.laydr.examples.nav3androidx.textPrimary
import dev.goquick.laydr.nav3.androidx.LaydrNavSectionsNavigator

@Composable
internal fun Screen(
    route: LaydrRoutes.Contacts.Destination,
) {
    val app = LocalAddressBookContext.current
    ContactListContent(
        repository = app.repository,
        routePath = route.path,
        navigator = app.navigator,
    )
}

@Composable
private fun ContactListContent(
    repository: InMemoryAddressBookRepository,
    routePath: String,
    navigator: LaydrNavSectionsNavigator,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(
            text = "Address book",
            style = TextStyle(color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        )
        BasicText(
            text = "These Android-only routes are generated from src/main/kotlin/routes.",
            style = TextStyle(color = textMuted, fontSize = 13.sp),
        )
        BasicText(
            text = routePath,
            style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(modifier = Modifier.height(4.dp))
        repository.contacts.forEach { contact ->
            ContactRow(
                contact = contact,
                selected = false,
                onClick = {
                    navigator.push(
                        LaydrRoutes.Contacts.ById.destination(
                            id = LaydrRoutes.Contacts.ById.id(contact.id),
                        ),
                    )
                },
            )
        }
    }
}
