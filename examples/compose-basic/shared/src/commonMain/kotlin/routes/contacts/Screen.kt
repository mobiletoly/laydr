package routes.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.goquick.laydr.compose.LaydrLayoutValues
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.examples.basic.ContactRow
import dev.goquick.laydr.examples.basic.InMemoryAddressBookRepository
import dev.goquick.laydr.examples.basic.LocalComposeBasicContext
import dev.goquick.laydr.examples.basic.border
import dev.goquick.laydr.examples.basic.generated.LaydrRoutes
import dev.goquick.laydr.examples.basic.surface
import dev.goquick.laydr.examples.basic.textMuted
import dev.goquick.laydr.examples.basic.textPrimary

@Composable
internal fun ContactsScreen(
    route: LaydrRoutes.Contacts.Destination,
    repository: InMemoryAddressBookRepository =
        LocalComposeBasicContext.current.repository,
    navigate: (String) -> Unit = LocalComposeBasicContext.current.navigate,
): LaydrScreenContent =
    LaydrScreenContent(
        layoutValues = LaydrLayoutValues.build {
            put(
                ContactsLayoutStateKey,
                ContactsLayoutState(
                    title = "All contacts",
                    subtitle = "Choose a contact to view details or edit profile information.",
                    activeContactId = null,
                    activePath = route.path,
                    mode = ContactsLayoutMode.List,
                ),
            )
        },
    ) {
        ContactListContent(
            repository = repository,
            navigate = navigate,
        )
    }

@Composable
private fun ContactListContent(
    repository: InMemoryAddressBookRepository,
    navigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(
            text = "Address book",
            style = TextStyle(color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        )
        BasicText(
            text = "These routes are generated from the contacts directory tree.",
            style = TextStyle(color = textMuted, fontSize = 13.sp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        repository.contacts.forEach { contact ->
            ContactRow(
                contact = contact,
                selected = false,
                onClick = {
                    navigate(
                        LaydrRoutes.Contacts.ById.path(
                            id = LaydrRoutes.Contacts.ById.id(contact.id),
                        ),
                    )
                },
            )
        }
    }
}
