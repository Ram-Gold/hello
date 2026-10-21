package com.example.hello.ui.contacts

import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactsViewModelTest {

    private lateinit var viewModel: ContactsViewModel

    @Before
    fun setUp() {
        viewModel = ContactsViewModel()
    }

    @Test
    fun initialState_hasExpectedDefaults() {
        val state = viewModel.uiState.value

        assertEquals("", state.searchQuery)
        assertNull(state.selectedCircle)
        assertEquals(ContactSortMode.ALPHABETICAL, state.sortMode)
        assertTrue(state.contacts.isNotEmpty())
        assertEquals(state.contacts.size, state.totalCount)
        assertTrue(state.groupedContacts.isNotEmpty())
    }

    @Test
    fun searchQuery_filtersByName() {
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("Maya"))

        val state = viewModel.uiState.value
        assertEquals(1, state.contacts.size)
        assertEquals("Maya Patel", state.contacts.first().name)
    }

    @Test
    fun searchQuery_filtersByContextOrRole() {
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("Designer"))

        val state = viewModel.uiState.value
        assertTrue(state.contacts.any { it.name == "Alex Rivera" })
    }

    @Test
    fun selectCircle_filtersByCircleCategory() {
        viewModel.onEvent(ContactsUiEvent.OnSelectCircle(Circle.WORK))

        val state = viewModel.uiState.value
        assertTrue(state.contacts.isNotEmpty())
        assertTrue(state.contacts.all { Circle.WORK in it.circles })
    }

    @Test
    fun searchAndCircle_combinesFiltering() {
        viewModel.onEvent(ContactsUiEvent.OnSelectCircle(Circle.WORK))
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("David"))

        val state = viewModel.uiState.value
        assertEquals(1, state.contacts.size)
        assertEquals("David Kim", state.contacts.first().name)
    }

    @Test
    fun clearFilters_resetsState() {
        viewModel.onEvent(ContactsUiEvent.OnSelectCircle(Circle.WORK))
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("NonExistent"))
        assertEquals(0, viewModel.uiState.value.contacts.size)

        viewModel.onEvent(ContactsUiEvent.OnClearFilters)

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertNull(state.selectedCircle)
        assertTrue(state.contacts.size > 1)
    }

    @Test
    fun toggleSortMode_groupsAlphabeticallyOrByRecency() {
        // Alphabetical grouping check
        viewModel.onEvent(ContactsUiEvent.OnToggleSortMode(ContactSortMode.ALPHABETICAL))
        val alphaState = viewModel.uiState.value
        assertTrue(alphaState.groupedContacts.keys.contains("A"))
        assertTrue(alphaState.groupedContacts.keys.contains("M"))

        // Recency grouping check
        viewModel.onEvent(ContactsUiEvent.OnToggleSortMode(ContactSortMode.RECENTLY_ENCOUNTERED))
        val recencyState = viewModel.uiState.value
        assertTrue(
            recencyState.groupedContacts.keys.contains("Recently Met") ||
                    recencyState.groupedContacts.keys.contains("This Month") ||
                    recencyState.groupedContacts.keys.contains("Earlier")
        )
    }

    @Test
    fun onContactClick_setsFeedbackMessage() {
        val targetId = viewModel.uiState.value.contacts.first().id
        viewModel.onEvent(ContactsUiEvent.OnContactClick(targetId))

        assertNotNull(viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.userMessage!!.contains(targetId))
    }
}
