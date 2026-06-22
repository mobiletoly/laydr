package dev.goquick.laydr.examples.basic

import androidx.compose.runtime.mutableStateListOf

internal data class Contact(
    val id: String,
    val name: String,
    val role: String,
    val email: String,
    val phone: String,
    val city: String,
    val notes: String,
)

internal class InMemoryAddressBookRepository(
    initialContacts: List<Contact>,
) {
    private val contactState = mutableStateListOf<Contact>().apply {
        addAll(initialContacts)
    }

    val contacts: List<Contact>
        get() = contactState

    suspend fun find(id: String): Contact? {
        return contactState.firstOrNull { contact -> contact.id == id }
    }

    suspend fun update(contact: Contact): Boolean {
        val index = contactState.indexOfFirst { existing -> existing.id == contact.id }
        if (index == -1) {
            return false
        }
        contactState[index] = contact
        return true
    }
}

internal fun seedContacts(): List<Contact> =
    listOf(
        Contact(
            id = "ada",
            name = "Ada Lovelace",
            role = "Systems Analyst",
            email = "ada@example.com",
            phone = "+1 555 0101",
            city = "London",
            notes = "Prefers concise project updates and early technical drafts.",
        ),
        Contact(
            id = "grace",
            name = "Grace Hopper",
            role = "Compiler Engineer",
            email = "grace@example.com",
            phone = "+1 555 0102",
            city = "New York",
            notes = "Keeps notes on language tooling, runtime behavior, and developer education.",
        ),
        Contact(
            id = "katherine",
            name = "Katherine Johnson",
            role = "Flight Dynamics Lead",
            email = "katherine@example.com",
            phone = "+1 555 0103",
            city = "Hampton",
            notes = "Good contact for verification-heavy work and launch readiness reviews.",
        ),
        Contact(
            id = "margaret",
            name = "Margaret Hamilton",
            role = "Software Director",
            email = "margaret@example.com",
            phone = "+1 555 0104",
            city = "Cambridge",
            notes = "Focuses on operational reliability, interface contracts, and fault handling.",
        ),
    )
