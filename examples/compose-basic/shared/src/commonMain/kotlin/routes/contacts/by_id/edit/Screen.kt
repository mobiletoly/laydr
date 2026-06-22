package routes.contacts.by_id.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.goquick.laydr.compose.LaydrLayoutValues
import dev.goquick.laydr.compose.LaydrScreenContent
import dev.goquick.laydr.examples.basic.AppButton
import dev.goquick.laydr.examples.basic.Contact
import dev.goquick.laydr.examples.basic.FormField
import dev.goquick.laydr.examples.basic.InMemoryAddressBookRepository
import dev.goquick.laydr.examples.basic.LocalComposeBasicContext
import dev.goquick.laydr.examples.basic.border
import dev.goquick.laydr.examples.basic.generated.LaydrRoutes
import dev.goquick.laydr.examples.basic.surface
import routes.contacts.ContactsLayoutMode
import routes.contacts.ContactsLayoutState
import routes.contacts.ContactsLayoutStateKey
import routes.contacts.by_id.LoadingContactContent
import routes.contacts.by_id.MissingContactContent
import kotlinx.coroutines.launch

@Composable
internal fun ContactEditScreen(
    route: LaydrRoutes.Contacts.ById.Edit.Destination,
    repository: InMemoryAddressBookRepository =
        LocalComposeBasicContext.current.repository,
    navigate: (String) -> Unit = LocalComposeBasicContext.current.navigate,
): LaydrScreenContent {
    val contactId = route.id.value
    val contactState by produceState<ContactEditLookupState>(
        initialValue = ContactEditLookupState.Loading,
        key1 = repository,
        key2 = contactId,
    ) {
        value = repository.find(contactId)
            ?.let { found -> ContactEditLookupState.Found(found) }
            ?: ContactEditLookupState.Missing
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
            is ContactEditLookupState.Found -> ContactEditContent(
                contact = state.contact,
                repository = repository,
                navigate = navigate,
            )
            ContactEditLookupState.Loading -> LoadingContactContent()
            ContactEditLookupState.Missing -> MissingContactContent(contactId = contactId)
        }
    }
}

private sealed class ContactEditLookupState {
    data class Found(val contact: Contact) : ContactEditLookupState()

    data object Loading : ContactEditLookupState()

    data object Missing : ContactEditLookupState()
}

private fun ContactEditLookupState.layoutState(
    routePath: String,
    contactId: String,
): ContactsLayoutState =
    when (this) {
        is ContactEditLookupState.Found -> ContactsLayoutState(
            title = contact.name,
            subtitle = "Editing ${contact.role}",
            activeContactId = contact.id,
            activePath = routePath,
            mode = ContactsLayoutMode.Edit,
        )
        ContactEditLookupState.Loading -> ContactsLayoutState(
            title = "Loading contact",
            subtitle = "Fetching contact details.",
            activeContactId = contactId,
            activePath = routePath,
            mode = ContactsLayoutMode.Edit,
        )
        ContactEditLookupState.Missing -> ContactsLayoutState(
            title = "Contact not found",
            subtitle = "No contact exists for id '$contactId'.",
            activeContactId = contactId,
            activePath = routePath,
            mode = ContactsLayoutMode.Missing,
        )
    }

@Composable
private fun ContactEditContent(
    contact: Contact,
    repository: InMemoryAddressBookRepository,
    navigate: (String) -> Unit,
) {
    var name by remember(contact.id) { mutableStateOf(contact.name) }
    var role by remember(contact.id) { mutableStateOf(contact.role) }
    var email by remember(contact.id) { mutableStateOf(contact.email) }
    var phone by remember(contact.id) { mutableStateOf(contact.phone) }
    var city by remember(contact.id) { mutableStateOf(contact.city) }
    var notes by remember(contact.id) { mutableStateOf(contact.notes) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FormField(label = "Name", value = name, onValueChange = { name = it })
        FormField(label = "Role", value = role, onValueChange = { role = it })
        FormField(label = "Email", value = email, onValueChange = { email = it })
        FormField(label = "Phone", value = phone, onValueChange = { phone = it })
        FormField(label = "City", value = city, onValueChange = { city = it })
        FormField(label = "Notes", value = notes, onValueChange = { notes = it })

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppButton(
                label = "Save",
                onClick = {
                    coroutineScope.launch {
                        repository.update(
                            contact.copy(
                                name = name.trim().ifEmpty { contact.name },
                                role = role.trim().ifEmpty { contact.role },
                                email = email.trim().ifEmpty { contact.email },
                                phone = phone.trim().ifEmpty { contact.phone },
                                city = city.trim().ifEmpty { contact.city },
                                notes = notes.trim().ifEmpty { contact.notes },
                            ),
                        )
                        navigate(
                            LaydrRoutes.Contacts.ById.path(
                                id = LaydrRoutes.Contacts.ById.id(contact.id),
                            ),
                        )
                    }
                },
                primary = true,
            )
            AppButton(
                label = "Cancel",
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
