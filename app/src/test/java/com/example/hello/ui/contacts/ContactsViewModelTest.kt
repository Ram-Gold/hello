package com.example.hello.ui.contacts

import com.example.hello.domain.model.ContactSortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ContactsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ContactsViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasExpectedDefaults() {
        val state = viewModel.uiState.value

        assertEquals("", state.searchQuery)
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
    fun clearFilters_resetsState() {
        viewModel.onEvent(ContactsUiEvent.OnSelectTag("design"))
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("NonExistent"))
        assertEquals(0, viewModel.uiState.value.contacts.size)

        viewModel.onEvent(ContactsUiEvent.OnClearFilters)

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertNull(state.selectedTag)
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

    @Test
    fun searchQuery_filtersByNotesAndTags() {
        // Filter by note keyword
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("Ethiopian"))
        assertEquals(1, viewModel.uiState.value.contacts.size)
        assertEquals("Alex Rivera", viewModel.uiState.value.contacts.first().name)

        // Filter by tag name
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("Coffee"))
        val coffeeContacts = viewModel.uiState.value.contacts
        assertTrue(coffeeContacts.isNotEmpty())
        assertTrue(coffeeContacts.any { it.name == "Elena Rostova" || it.name == "Liam Chen" })
    }

    @Test
    fun contactSummary_containsEnrichedNotesAndTags() {
        val alex = viewModel.uiState.value.contacts.first { it.name == "Alex Rivera" }
        assertTrue(alex.notes.isNotBlank())
        assertTrue(alex.tags.isNotEmpty())
        assertTrue(alex.tags.any { it.name == "Design" })
    }

    @Test
    fun onSelectTag_filtersByTopicTags_andClearsCleanly() {
        // Filter by topic tag #design
        viewModel.onEvent(ContactsUiEvent.OnSelectTag("design"))
        val designState = viewModel.uiState.value
        assertEquals("design", designState.selectedTag)
        assertEquals(4, designState.contacts.size)
        assertTrue(designState.contacts.all { c -> c.tags.any { it.name.equals("design", ignoreCase = true) } })

        // Clear filters resets selectedTag
        viewModel.onEvent(ContactsUiEvent.OnClearFilters)
        assertNull(viewModel.uiState.value.selectedTag)
        assertEquals(10, viewModel.uiState.value.totalCount)
    }

    @Test
    fun searchQuery_supportsHashtagTopicQueries() {
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("#design"))
        val designResults = viewModel.uiState.value.contacts
        assertTrue(designResults.isNotEmpty())
        assertTrue(designResults.all { c -> c.tags.any { it.name.equals("design", ignoreCase = true) } })

        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("#ai"))
        val aiResults = viewModel.uiState.value.contacts
        assertEquals(2, aiResults.size)
    }

    @Test
    fun onContactLongClick_entersSelectionModeAndSelectsContact() {
        val targetId = viewModel.uiState.value.contacts.first().id
        assertEquals(emptySet<String>(), viewModel.uiState.value.selectedContactIds)
        assertTrue(!viewModel.uiState.value.isSelectionMode)

        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(targetId))

        val state = viewModel.uiState.value
        assertEquals(setOf(targetId), state.selectedContactIds)
        assertTrue(state.isSelectionMode)
    }

    @Test
    fun onContactLongClick_subsequentContactAddsToSelection() {
        val firstId = viewModel.uiState.value.contacts[0].id
        val secondId = viewModel.uiState.value.contacts[1].id

        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(firstId))
        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(secondId))

        val state = viewModel.uiState.value
        assertEquals(setOf(firstId, secondId), state.selectedContactIds)
        assertTrue(state.isSelectionMode)
    }

    @Test
    fun onToggleSelectContact_togglesSelectionState() {
        val firstId = viewModel.uiState.value.contacts[0].id
        val secondId = viewModel.uiState.value.contacts[1].id

        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(firstId))
        viewModel.onEvent(ContactsUiEvent.OnToggleSelectContact(secondId))

        assertEquals(setOf(firstId, secondId), viewModel.uiState.value.selectedContactIds)

        // Toggle firstId off
        viewModel.onEvent(ContactsUiEvent.OnToggleSelectContact(firstId))
        assertEquals(setOf(secondId), viewModel.uiState.value.selectedContactIds)
        assertTrue(viewModel.uiState.value.isSelectionMode)
    }

    @Test
    fun onToggleSelectContact_deselectingLastContact_autoExitsSelectionMode() {
        val targetId = viewModel.uiState.value.contacts.first().id
        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(targetId))
        assertTrue(viewModel.uiState.value.isSelectionMode)

        // Deselect the last selected contact
        viewModel.onEvent(ContactsUiEvent.OnToggleSelectContact(targetId))

        val state = viewModel.uiState.value
        assertTrue(state.selectedContactIds.isEmpty())
        assertTrue(!state.isSelectionMode)
    }

    @Test
    fun onClearSelection_clearsAllSelectionsAndExitsSelectionMode() {
        val firstId = viewModel.uiState.value.contacts[0].id
        val secondId = viewModel.uiState.value.contacts[1].id

        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(firstId))
        viewModel.onEvent(ContactsUiEvent.OnToggleSelectContact(secondId))
        assertEquals(2, viewModel.uiState.value.selectedContactIds.size)
        assertTrue(viewModel.uiState.value.isSelectionMode)

        viewModel.onEvent(ContactsUiEvent.OnClearSelection)

        val state = viewModel.uiState.value
        assertTrue(state.selectedContactIds.isEmpty())
        assertTrue(!state.isSelectionMode)
    }

    @Test
    fun searchOrFilterChange_prunesSelectedContactIds() {
        val alex = viewModel.uiState.value.contacts.first { it.name == "Alex Rivera" } // WORK
        val elena = viewModel.uiState.value.contacts.first { it.name == "Elena Rostova" } // FRIENDS, FAMILY

        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(alex.id))
        viewModel.onEvent(ContactsUiEvent.OnToggleSelectContact(elena.id))
        assertEquals(setOf(alex.id, elena.id), viewModel.uiState.value.selectedContactIds)

        // Filter by tag "design" - Elena has "craft" not "design", so she must be pruned if not matching
        viewModel.onEvent(ContactsUiEvent.OnSelectTag("design"))
        val stateAfterTag = viewModel.uiState.value
        assertEquals(setOf(alex.id), stateAfterTag.selectedContactIds)
        assertTrue(stateAfterTag.isSelectionMode)

        // Search for a query that matches nobody - Alex must be pruned, auto-exiting selection mode
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("NonExistentNameXYZ"))
        val stateAfterSearch = viewModel.uiState.value
        assertTrue(stateAfterSearch.selectedContactIds.isEmpty())
        assertTrue(!stateAfterSearch.isSelectionMode)
    }

    @Test
    fun onSelectAllVisibleContacts_selectsAllVisibleContacts() {
        val visibleIds = viewModel.uiState.value.contacts.map { it.id }.toSet()
        assertTrue(visibleIds.isNotEmpty())
        assertTrue(viewModel.uiState.value.selectedContactIds.isEmpty())

        viewModel.onEvent(ContactsUiEvent.OnSelectAllVisibleContacts)

        val state = viewModel.uiState.value
        assertEquals(visibleIds, state.selectedContactIds)
        assertEquals(visibleIds.size, state.selectedContactIds.size)
        assertTrue(state.isSelectionMode)
    }

    @Test
    fun onSelectAllVisibleContacts_respectsActiveFilter() {
        viewModel.onEvent(ContactsUiEvent.OnSelectTag("design"))
        val designIds = viewModel.uiState.value.contacts.map { it.id }.toSet()
        assertTrue(designIds.isNotEmpty())

        viewModel.onEvent(ContactsUiEvent.OnSelectAllVisibleContacts)

        val state = viewModel.uiState.value
        assertEquals(designIds, state.selectedContactIds)
        assertTrue(state.isSelectionMode)
    }

    @Test
    fun onRequestDeleteSelected_andDismiss_togglesDeleteConfirmationDialog() {
        assertEquals(false, viewModel.uiState.value.isDeleteConfirmationVisible)

        viewModel.onEvent(ContactsUiEvent.OnRequestDeleteSelected)
        assertEquals(true, viewModel.uiState.value.isDeleteConfirmationVisible)

        viewModel.onEvent(ContactsUiEvent.OnDismissDeleteDialog)
        assertEquals(false, viewModel.uiState.value.isDeleteConfirmationVisible)
    }

    @Test
    fun onConfirmDeleteSelected_removesContacts_clearsSelection_andDismissesDialog() {
        val initialCount = viewModel.uiState.value.contacts.size
        val contact1 = viewModel.uiState.value.contacts[0]
        val contact2 = viewModel.uiState.value.contacts[1]

        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(contact1.id))
        viewModel.onEvent(ContactsUiEvent.OnToggleSelectContact(contact2.id))
        viewModel.onEvent(ContactsUiEvent.OnRequestDeleteSelected)

        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)
        assertEquals(setOf(contact1.id, contact2.id), viewModel.uiState.value.selectedContactIds)

        viewModel.onEvent(ContactsUiEvent.OnConfirmDeleteSelected)

        val state = viewModel.uiState.value
        assertEquals(false, state.isDeleteConfirmationVisible)
        assertTrue(state.selectedContactIds.isEmpty())
        assertTrue(!state.isSelectionMode)
        assertEquals(initialCount - 2, state.contacts.size)
        assertEquals(initialCount - 2, state.totalCount)
        assertTrue(state.contacts.none { it.id == contact1.id || it.id == contact2.id })
        assertEquals("Deleted 2 contacts", state.userMessage)
    }

    @Test
    fun onConfirmDeleteSelected_withRepository_propagatesDeletions() {
        val repo = com.example.hello.data.repository.ContactRepository()
        val repoViewModel = ContactsViewModel(repository = repo)
        val initialRepoCount = repo.contacts.value.size
        val target = repoViewModel.uiState.value.contacts.first()

        repoViewModel.onEvent(ContactsUiEvent.OnContactLongClick(target.id))
        repoViewModel.onEvent(ContactsUiEvent.OnConfirmDeleteSelected)

        val state = repoViewModel.uiState.value
        assertTrue(state.selectedContactIds.isEmpty())
        assertTrue(!state.isSelectionMode)
        assertTrue(state.contacts.none { it.id == target.id })
        assertTrue(repo.contacts.value.none { it.id == target.id })
        assertEquals(initialRepoCount - 1, repo.contacts.value.size)
        assertEquals("Deleted 1 contact", state.userMessage)
    }

    @Test
    fun tabSwitching_preservesSelectionFiltersAndSortState() {
        val target = viewModel.uiState.value.contacts.first()
        viewModel.onEvent(ContactsUiEvent.OnSearchQueryChange("Alex"))
        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(target.id))
        viewModel.onEvent(ContactsUiEvent.OnToggleSortMode(ContactSortMode.RECENTLY_ENCOUNTERED))

        // State remains intact in ViewModel across tab deactivations
        val state = viewModel.uiState.value
        assertEquals("Alex", state.searchQuery)
        assertTrue(state.isSelectionMode)
        assertEquals(setOf(target.id), state.selectedContactIds)
        assertEquals(ContactSortMode.RECENTLY_ENCOUNTERED, state.sortMode)
    }

    @Test
    fun syncTags_updatesAvailableTagsAndDynamicSmartFilters() {
        val customTag = com.example.hello.domain.model.Tag(
            id = "custom-tag-1",
            name = "Ceramics",
            colorHex = "#FF5722"
        )
        viewModel.syncTags(listOf(customTag))

        val state = viewModel.uiState.value
        assertTrue(state.availableTags.any { it.name == "Ceramics" && it.colorHex == "#FF5722" })
        assertTrue(state.smartFilterTags.contains("ceramics"))
    }

    @Test
    fun onSelectTag_supportsMultipleTagSelectionUnionAndToggleDeselect() {
        // Select first tag: design (4 contacts)
        viewModel.onEvent(ContactsUiEvent.OnSelectTag("design"))
        val stateAfterFirst = viewModel.uiState.value
        assertEquals(setOf("design"), stateAfterFirst.selectedTags)
        assertEquals(4, stateAfterFirst.contacts.size)

        // Select second tag: ai (adds to selectedTags and filters by union OR)
        viewModel.onEvent(ContactsUiEvent.OnSelectTag("ai"))
        val stateAfterSecond = viewModel.uiState.value
        assertEquals(setOf("design", "ai"), stateAfterSecond.selectedTags)
        assertTrue(stateAfterSecond.contacts.size > 4)
        assertTrue(
            stateAfterSecond.contacts.all { c ->
                c.tags.any { it.name.equals("design", ignoreCase = true) || it.name.equals("ai", ignoreCase = true) }
            }
        )

        // Tapping an already-selected tag ("design") deselects only that tag, leaving "ai" active
        viewModel.onEvent(ContactsUiEvent.OnSelectTag("design"))
        val stateAfterDeselectFirst = viewModel.uiState.value
        assertEquals(setOf("ai"), stateAfterDeselectFirst.selectedTags)
        assertEquals(2, stateAfterDeselectFirst.contacts.size)

        // Tapping "All" (null) clears all selected tags
        viewModel.onEvent(ContactsUiEvent.OnSelectTag(null))
        val stateAfterReset = viewModel.uiState.value
        assertTrue(stateAfterReset.selectedTags.isEmpty())
        assertNull(stateAfterReset.selectedTag)
        assertEquals(10, stateAfterReset.totalCount)
    }

    @Test
    fun favoriteContacts_collectedFromRepository() = kotlinx.coroutines.test.runTest {
        val repo = com.example.hello.data.repository.ContactRepository()
        val vm = ContactsViewModel(repository = repo)

        assertTrue(vm.uiState.value.favoriteContacts.isEmpty())

        repo.toggleFavorite("c-1")
        vm.syncFavorites(repo.favoriteContacts.value)

        assertEquals(1, vm.uiState.value.favoriteContacts.size)
        assertEquals("c-1", vm.uiState.value.favoriteContacts.first().id)
    }

    @Test
    fun computeState_excludesArchivedContactsFromActiveListAndPopulatesArchived() {
        val sample = ContactsViewModel.createSampleContacts()
        val archivedContact = sample.first().copy(id = "c-archived-1", name = "Zoe Archived", isArchived = true)
        val fullList = sample + archivedContact

        val state = ContactsViewModel.computeState(
            searchQuery = "",
            sortMode = ContactSortMode.ALPHABETICAL,
            contacts = fullList
        )

        // Main list should strictly exclude archived contact
        assertFalse(state.contacts.any { it.id == "c-archived-1" })
        assertEquals(sample.size, state.totalCount)

        // Archived contacts list contains the archived contact
        assertEquals(1, state.archivedContacts.size)
        assertEquals("c-archived-1", state.archivedContacts.first().id)
    }

    @Test
    fun archivedSheet_openAndDismissEvents_updateUiState() {
        assertFalse(viewModel.uiState.value.isArchivedSheetOpen)

        viewModel.onEvent(ContactsUiEvent.OnOpenArchivedSheet)
        assertTrue(viewModel.uiState.value.isArchivedSheetOpen)

        viewModel.onEvent(ContactsUiEvent.OnDismissArchivedSheet)
        assertFalse(viewModel.uiState.value.isArchivedSheetOpen)
    }

    @Test
    fun restoreContact_unarchivesContactAndMovesToActiveList() {
        val sample = ContactsViewModel.createSampleContacts()
        val archived = sample.first().copy(id = "c-archived-2", name = "Liam Restored", isArchived = true)
        viewModel.syncContacts(sample + archived)

        assertTrue(viewModel.uiState.value.archivedContacts.any { it.id == "c-archived-2" })
        assertFalse(viewModel.uiState.value.contacts.any { it.id == "c-archived-2" })

        viewModel.onEvent(ContactsUiEvent.OnRestoreContact("c-archived-2"))

        val state = viewModel.uiState.value
        assertFalse(state.archivedContacts.any { it.id == "c-archived-2" })
        assertTrue(state.contacts.any { it.id == "c-archived-2" })
        assertEquals("Contact restored", state.userMessage)
    }
}
