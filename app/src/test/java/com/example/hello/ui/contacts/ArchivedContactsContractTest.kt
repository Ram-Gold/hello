package com.example.hello.ui.contacts

import com.example.hello.domain.model.ContactSummary
import com.example.hello.ui.navigation.ArchivedContactsRoute
import com.example.hello.ui.navigation.shouldHideFloatingDock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Seams 1, 2 & 3: Route contract, dock hiding policy, and restore state partitioning for full-screen archived contacts.
 */
class ArchivedContactsContractTest {

    @Test
    fun archivedContactsRoute_hasCorrectRouteName() {
        assertEquals("contacts/archived", ArchivedContactsRoute.ROUTE_NAME)
    }

    @Test
    fun archivedContactsRoute_hidesFloatingBottomDock() {
        // Floating bottom dock must be hidden to give the full-screen list complete vertical real estate
        assertTrue(shouldHideFloatingDock(ArchivedContactsRoute.ROUTE_NAME))
    }

    @Test
    fun restoreContact_removesFromArchivedAndReinsertsIntoActiveDirectory() {
        val archived = ContactSummary(
            id = "c-arc-1",
            name = "Archived Contact",
            contextSubtitle = "Former colleague",
            avatarInitials = "AC",
            isArchived = true
        )
        val active = ContactSummary(
            id = "c-act-1",
            name = "Active Contact",
            contextSubtitle = "Coworker",
            avatarInitials = "AC",
            isArchived = false
        )

        val viewModel = ContactsViewModel()
        viewModel.syncContacts(listOf(archived, active))

        // Initially partitioned: active directory excludes archived contact
        assertEquals(1, viewModel.uiState.value.contacts.size)
        assertEquals("c-act-1", viewModel.uiState.value.contacts.first().id)
        assertEquals(1, viewModel.uiState.value.archivedContacts.size)
        assertEquals("c-arc-1", viewModel.uiState.value.archivedContacts.first().id)

        // Trigger restore event
        viewModel.onEvent(ContactsUiEvent.OnRestoreContact("c-arc-1"))

        // After restoration: archived list is empty, active list has both
        assertEquals(0, viewModel.uiState.value.archivedContacts.size)
        assertEquals(2, viewModel.uiState.value.contacts.size)
        assertTrue(viewModel.uiState.value.contacts.any { it.id == "c-arc-1" })
        assertFalse(viewModel.uiState.value.contacts.first { it.id == "c-arc-1" }.isArchived)
    }
}

