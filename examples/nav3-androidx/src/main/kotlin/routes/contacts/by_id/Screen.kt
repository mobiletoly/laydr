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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.goquick.laydr.examples.nav3androidx.AppButton
import dev.goquick.laydr.examples.nav3androidx.Contact
import dev.goquick.laydr.examples.nav3androidx.DetailRow
import dev.goquick.laydr.examples.nav3androidx.LocalAddressBookContext
import dev.goquick.laydr.examples.nav3androidx.border
import dev.goquick.laydr.examples.nav3androidx.generated.LaydrRoutes
import dev.goquick.laydr.examples.nav3androidx.surface
import dev.goquick.laydr.examples.nav3androidx.textMuted
import dev.goquick.laydr.examples.nav3androidx.textPrimary
import dev.goquick.laydr.nav3.androidx.LaydrNavSectionsNavigator

@Composable
internal fun Screen(
    route: LaydrRoutes.Contacts.ById.Destination,
) {
    val app = LocalAddressBookContext.current
    val contactState by produceState<ContactLookupState>(
        initialValue = ContactLookupState.Loading,
        key1 = app.repository,
        key2 = route.id.value,
    ) {
        value = app.repository.find(route.id.value)
            ?.let { found -> ContactLookupState.Found(found) }
            ?: ContactLookupState.Missing
    }

    when (val state = contactState) {
        is ContactLookupState.Found -> ContactDetailContent(
            contact = state.contact,
            routePath = route.path,
            navigator = app.navigator,
        )
        ContactLookupState.Loading -> LoadingContactContent()
        ContactLookupState.Missing -> MissingContactContent(contactId = route.id.value)
    }
}

private sealed class ContactLookupState {
    data class Found(val contact: Contact) : ContactLookupState()

    data object Loading : ContactLookupState()

    data object Missing : ContactLookupState()
}

@Composable
internal fun LoadingContactContent() {
    ContactMessageCard(
        title = "Loading contact",
        message = "Fetching contact details.",
    )
}

@Composable
internal fun MissingContactContent(contactId: String) {
    ContactMessageCard(
        title = "Contact not found",
        message = "No contact exists for id '$contactId'.",
    )
}

@Composable
private fun ContactMessageCard(
    title: String,
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        BasicText(
            text = title,
            style = TextStyle(color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicText(
            text = message,
            style = TextStyle(color = textMuted, fontSize = 14.sp),
        )
    }
}

@Composable
private fun ContactDetailContent(
    contact: Contact,
    routePath: String,
    navigator: LaydrNavSectionsNavigator,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .verticalScroll(rememberScrollState())
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
                    navigator.push(
                        LaydrRoutes.Contacts.ById.Edit.destination(
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
        DetailRow(label = "Path", value = routePath)
        DetailRow(label = "Notes", value = contact.notes)
    }
}
