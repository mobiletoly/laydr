package routes.contacts.by_id.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.goquick.laydr.examples.nav3kmp.AppButton
import dev.goquick.laydr.examples.nav3kmp.Contact
import dev.goquick.laydr.examples.nav3kmp.DetailRow
import dev.goquick.laydr.examples.nav3kmp.FormField
import dev.goquick.laydr.examples.nav3kmp.InMemoryAddressBookRepository
import dev.goquick.laydr.examples.nav3kmp.LocalAddressBookContext
import dev.goquick.laydr.examples.nav3kmp.border
import dev.goquick.laydr.examples.nav3kmp.generated.LaydrRoutes
import dev.goquick.laydr.examples.nav3kmp.surface
import dev.goquick.laydr.nav3.kmp.LaydrNavSectionsNavigator
import routes.contacts.by_id.LoadingContactContent
import routes.contacts.by_id.MissingContactContent
import kotlinx.coroutines.launch

@Composable
internal fun Screen(
    route: LaydrRoutes.Contacts.ById.Edit.Destination,
) {
    val app = LocalAddressBookContext.current
    val contactState by produceState<ContactEditLookupState>(
        initialValue = ContactEditLookupState.Loading,
        key1 = app.repository,
        key2 = route.id.value,
    ) {
        value = app.repository.find(route.id.value)
            ?.let { found -> ContactEditLookupState.Found(found) }
            ?: ContactEditLookupState.Missing
    }

    when (val state = contactState) {
        is ContactEditLookupState.Found -> ContactEditContent(
            contact = state.contact,
            routePath = route.path,
            repository = app.repository,
            navigator = app.navigator,
        )
        ContactEditLookupState.Loading -> LoadingContactContent()
        ContactEditLookupState.Missing -> MissingContactEditContent(
            contactId = route.id.value,
            navigator = app.navigator,
        )
    }

}

private sealed class ContactEditLookupState {
    data class Found(val contact: Contact) : ContactEditLookupState()

    data object Loading : ContactEditLookupState()

    data object Missing : ContactEditLookupState()
}

@Composable
private fun MissingContactEditContent(
    contactId: String,
    navigator: LaydrNavSectionsNavigator,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MissingContactContent(contactId = contactId)
        AppButton(
            label = "Cancel",
            onClick = { navigator.replace(LaydrRoutes.Contacts.destination()) },
        )
    }
}

@Composable
private fun ContactEditContent(
    contact: Contact,
    routePath: String,
    repository: InMemoryAddressBookRepository,
    navigator: LaydrNavSectionsNavigator,
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
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DetailRow(label = "Path", value = routePath)
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
                        val updated = contact.copy(
                            name = name.trim().ifEmpty { contact.name },
                            role = role.trim().ifEmpty { contact.role },
                            email = email.trim().ifEmpty { contact.email },
                            phone = phone.trim().ifEmpty { contact.phone },
                            city = city.trim().ifEmpty { contact.city },
                            notes = notes.trim().ifEmpty { contact.notes },
                        )
                        if (repository.update(updated)) {
                            navigator.replace(
                                LaydrRoutes.Contacts.ById.destination(
                                    id = LaydrRoutes.Contacts.ById.id(updated.id),
                                ),
                            )
                        } else {
                            navigator.replace(LaydrRoutes.Contacts.destination())
                        }
                    }
                },
                primary = true,
            )
            AppButton(
                label = "Cancel",
                onClick = {
                    navigator.replace(
                        LaydrRoutes.Contacts.ById.destination(
                            id = LaydrRoutes.Contacts.ById.id(contact.id),
                        ),
                    )
                },
            )
        }
    }
}
