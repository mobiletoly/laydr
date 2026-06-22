package routes.contacts

import dev.goquick.laydr.compose.LaydrLayoutKey

internal data class ContactsLayoutState(
    val title: String,
    val subtitle: String,
    val activeContactId: String?,
    val activePath: String,
    val mode: ContactsLayoutMode,
)

internal enum class ContactsLayoutMode {
    List,
    Detail,
    Edit,
    Missing,
}

internal val ContactsLayoutStateKey: LaydrLayoutKey<ContactsLayoutState> =
    LaydrLayoutKey("contacts.layout")
