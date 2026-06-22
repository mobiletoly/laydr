package routes.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.goquick.laydr.compose.LaydrLayoutContext
import dev.goquick.laydr.examples.basic.AppButton
import dev.goquick.laydr.examples.basic.ContactRow
import dev.goquick.laydr.examples.basic.InMemoryAddressBookRepository
import dev.goquick.laydr.examples.basic.LocalComposeBasicContext
import dev.goquick.laydr.examples.basic.accent
import dev.goquick.laydr.examples.basic.border
import dev.goquick.laydr.examples.basic.generated.LaydrRoutes
import dev.goquick.laydr.examples.basic.surface
import dev.goquick.laydr.examples.basic.surfaceMuted
import dev.goquick.laydr.examples.basic.textMuted
import dev.goquick.laydr.examples.basic.textPrimary

@Composable
internal fun ContactsLayout(
    context: LaydrLayoutContext,
    content: @Composable () -> Unit,
    repository: InMemoryAddressBookRepository =
        LocalComposeBasicContext.current.repository,
    navigate: (String) -> Unit = LocalComposeBasicContext.current.navigate,
) {
    val layoutState = context[ContactsLayoutStateKey] ?: ContactsLayoutState(
        title = context.match.route.metadata.name,
        subtitle = context.match.route.pathTemplate,
        activeContactId = null,
        activePath = context.match.route.pathTemplate,
        mode = ContactsLayoutMode.List,
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(surface, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(16.dp),
    ) {
        if (maxWidth < 720.dp) {
            CompactContactsLayout(
                layoutState = layoutState,
                navigate = navigate,
                content = content,
            )
        } else {
            WideContactsLayout(
                layoutState = layoutState,
                repository = repository,
                navigate = navigate,
                content = content,
            )
        }
    }
}

@Composable
private fun WideContactsLayout(
    layoutState: ContactsLayoutState,
    repository: InMemoryAddressBookRepository,
    navigate: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ContactSidebar(
            repository = repository,
            activeContactId = layoutState.activeContactId,
            navigate = navigate,
            modifier = Modifier.width(280.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(surfaceMuted, RoundedCornerShape(8.dp))
                .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
                .padding(20.dp),
        ) {
            LayoutHeader(layoutState = layoutState)
            Spacer(modifier = Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
private fun CompactContactsLayout(
    layoutState: ContactsLayoutState,
    navigate: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                LayoutHeader(layoutState = layoutState)
            }
            if (layoutState.mode != ContactsLayoutMode.List) {
                AppButton(
                    label = "Contacts",
                    onClick = { navigate(LaydrRoutes.Contacts.path()) },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun ContactSidebar(
    repository: InMemoryAddressBookRepository,
    activeContactId: String?,
    navigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceMuted, RoundedCornerShape(8.dp))
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(8.dp))
            .padding(14.dp),
    ) {
        BasicText(
            text = "Contacts",
            style = TextStyle(color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = "${repository.contacts.size} people",
            style = TextStyle(color = textMuted, fontSize = 12.sp),
        )
        Spacer(modifier = Modifier.height(14.dp))
        repository.contacts.forEach { contact ->
            ContactRow(
                contact = contact,
                selected = contact.id == activeContactId,
                onClick = {
                    navigate(
                        LaydrRoutes.Contacts.ById.path(
                            id = LaydrRoutes.Contacts.ById.id(contact.id),
                        ),
                    )
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LayoutHeader(layoutState: ContactsLayoutState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = layoutState.title,
            style = TextStyle(color = textPrimary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = layoutState.subtitle,
            style = TextStyle(color = textMuted, fontSize = 13.sp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicText(
            text = layoutState.activePath,
            style = TextStyle(color = accent, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
    }
}
