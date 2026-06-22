package routes.contacts.by_id

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.goquick.laydr.compose.LaydrLayoutValues
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.examples.basic.AppButton
import dev.goquick.laydr.examples.basic.Contact
import dev.goquick.laydr.examples.basic.DetailRow
import dev.goquick.laydr.examples.basic.InMemoryAddressBookRepository
import dev.goquick.laydr.examples.basic.LocalComposeBasicContext
import dev.goquick.laydr.examples.basic.border
import dev.goquick.laydr.examples.basic.generated.LaydrRoutes
import dev.goquick.laydr.examples.basic.surface
import dev.goquick.laydr.examples.basic.textMuted
import dev.goquick.laydr.examples.basic.textPrimary
import routes.contacts.ContactsLayoutMode
import routes.contacts.ContactsLayoutState
import routes.contacts.ContactsLayoutStateKey

@Composable
internal fun ContactDetailScreen(
    route: LaydrRoutes.Contacts.ById.Destination,
    repository: InMemoryAddressBookRepository =
        LocalComposeBasicContext.current.repository,
    navigate: (String) -> Unit = LocalComposeBasicContext.current.navigate,
): LaydrScreenContent {
    val contactId = route.id.value
    val contactState by produceState<ContactLookupState>(
        initialValue = ContactLookupState.Loading,
        key1 = repository,
        key2 = contactId,
    ) {
        value = repository.find(contactId)
            ?.let { found -> ContactLookupState.Found(found) }
            ?: ContactLookupState.Missing
    }

    return LaydrScreenContent(
        layoutValues = LaydrLayoutValues.build {
            put(
                ContactsLayoutStateKey,
                contactState.layoutState(routePath = route.path, contactId = contactId),
            )
        },
    ) {
        when (val state = contactState) {
            is ContactLookupState.Found -> ContactDetailContent(
                contact = state.contact,
                navigate = navigate,
            )
            ContactLookupState.Loading -> LoadingContactContent()
            ContactLookupState.Missing -> MissingContactContent(contactId = contactId)
        }
    }
}

private sealed class ContactLookupState {
    data class Found(val contact: Contact) : ContactLookupState()

    data object Loading : ContactLookupState()

    data object Missing : ContactLookupState()
}

private fun ContactLookupState.layoutState(
    routePath: String,
    contactId: String,
): ContactsLayoutState =
    when (this) {
        is ContactLookupState.Found -> ContactsLayoutState(
            title = contact.name,
            subtitle = contact.role,
            activeContactId = contact.id,
            activePath = routePath,
            mode = ContactsLayoutMode.Detail,
        )
        ContactLookupState.Loading -> ContactsLayoutState(
            title = "Loading contact",
            subtitle = "Fetching contact details.",
            activeContactId = contactId,
            activePath = routePath,
            mode = ContactsLayoutMode.Detail,
        )
        ContactLookupState.Missing -> ContactsLayoutState(
            title = "Contact not found",
            subtitle = "No contact exists for id '$contactId'.",
            activeContactId = contactId,
            activePath = routePath,
            mode = ContactsLayoutMode.Missing,
        )
    }

@Composable
internal fun LoadingContactContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(20.dp),
    ) {
        BasicText(
            text = "Loading contact",
            style = TextStyle(color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = "Fetching contact details.",
            style = TextStyle(color = textMuted, fontSize = 14.sp),
        )
    }
}

@Composable
internal fun MissingContactContent(contactId: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(20.dp),
    ) {
        BasicText(
            text = "Contact not found",
            style = TextStyle(color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = "No contact exists for id '$contactId'.",
            style = TextStyle(color = textMuted, fontSize = 14.sp),
        )
    }
}

@Composable
private fun ContactDetailContent(
    contact: Contact,
    navigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = contact.name,
                    style = TextStyle(color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicText(
                    text = contact.role,
                    style = TextStyle(color = textMuted, fontSize = 13.sp),
                )
            }
            AppButton(
                label = "Edit",
                onClick = {
                    navigate(
                        LaydrRoutes.Contacts.ById.Edit.path(
                            id = LaydrRoutes.Contacts.ById.Edit.id(contact.id),
                        ),
                    )
                },
                primary = true,
            )
        }
        DetailRow(label = "Email", value = contact.email)
        DetailRow(label = "Phone", value = contact.phone)
        DetailRow(label = "City", value = contact.city)
        DetailRow(label = "Notes", value = contact.notes)
    }
}
