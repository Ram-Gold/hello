package com.example.hello.ui.detail

import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class PersonDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ContactRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = ContactRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_whenContactInMemory_isPopulatedImmediatelyWithoutLoading() {
        // Must be populated synchronously on frame 0 to avoid spinner flash & mid-transition layout thrashing
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        val state = viewModel.uiState.value

        assertEquals(false, state.isLoading)
        assertNotNull(state.contact)
        assertEquals("Alex Rivera", state.contact?.name)
        assertNotNull(state.contactSummary)
        assertTrue(state.encounters.isNotEmpty())
    }

    @Test
    fun loadContact_populatesStateWithContactAndEncounters() = runTest {
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.contact)
        assertEquals("Alex Rivera", state.contact?.name)
        assertTrue(state.encounters.isNotEmpty())
    }

    @Test
    fun onToggleFavorite_togglesFavoriteStatus() = runTest {
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isFavorite)

        viewModel.onEvent(PersonDetailUiEvent.OnToggleFavorite)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.isFavorite)
        assertTrue(repository.getContact("c-1")?.isFavorite == true)

        viewModel.onEvent(PersonDetailUiEvent.OnToggleFavorite)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isFavorite)
        assertTrue(repository.getContact("c-1")?.isFavorite == false)
    }

    @Test
    fun phoneClick_whenPhoneMissing_setsUserMessage() = runTest {
        val contact = Contact(
            id = "test-nophone",
            name = "No Phone Contact",
            phone = null
        )
        repository.updateContact(contact)

        val viewModel = PersonDetailViewModel(contactId = "test-nophone", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonDetailUiEvent.OnPhoneClick)
        assertEquals("No phone number available", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onEncounterLongClick_entersSelectionModeAndSelectsEncounter() = runTest {
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val encounter = viewModel.uiState.value.encounters.first()
        viewModel.onEvent(PersonDetailUiEvent.OnEncounterLongClick(encounter.id))

        val state = viewModel.uiState.value
        assertTrue(state.isSelectionMode)
        assertTrue(state.selectedEncounterIds.contains(encounter.id))
        assertEquals(1, state.selectedEncounterIds.size)
    }

    @Test
    fun onToggleSelectEncounter_togglesAndClearsSelection() = runTest {
        repository.logEncounterForContact(
            contactId = "c-1",
            date = java.time.LocalDate.now(),
            placeId = null,
            customPlaceName = null,
            note = "Second meeting"
        )
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val encounters = viewModel.uiState.value.encounters
        assertTrue(encounters.size >= 2)
        val enc1 = encounters[0]
        val enc2 = encounters[1]

        viewModel.onEvent(PersonDetailUiEvent.OnEncounterLongClick(enc1.id))
        assertTrue(viewModel.uiState.value.isSelectionMode)

        viewModel.onEvent(PersonDetailUiEvent.OnToggleSelectEncounter(enc2.id))
        assertEquals(2, viewModel.uiState.value.selectedEncounterIds.size)

        viewModel.onEvent(PersonDetailUiEvent.OnToggleSelectEncounter(enc2.id))
        assertEquals(1, viewModel.uiState.value.selectedEncounterIds.size)

        viewModel.onEvent(PersonDetailUiEvent.OnToggleSelectEncounter(enc1.id))
        assertTrue(!viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedEncounterIds.isEmpty())
    }

    @Test
    fun onSelectAllEncounters_selectsAllContactEncounters() = runTest {
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val allIds = viewModel.uiState.value.encounters.map { it.id }.toSet()
        viewModel.onEvent(PersonDetailUiEvent.OnSelectAllEncounters)

        val state = viewModel.uiState.value
        assertTrue(state.isSelectionMode)
        assertEquals(allIds, state.selectedEncounterIds)
    }

    @Test
    fun onConfirmDeleteSelected_deletesEncountersAndReconcilesContact() = runTest {
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialEncounters = viewModel.uiState.value.encounters
        val toDelete = initialEncounters.first()

        viewModel.onEvent(PersonDetailUiEvent.OnEncounterLongClick(toDelete.id))
        viewModel.onEvent(PersonDetailUiEvent.OnRequestDeleteSelected)
        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)

        viewModel.onEvent(PersonDetailUiEvent.OnConfirmDeleteSelected)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(!state.isSelectionMode)
        assertTrue(!state.isDeleteConfirmationVisible)
        assertTrue(state.selectedEncounterIds.isEmpty())
        assertNotNull(state.userMessage)
        assertTrue(state.userMessage!!.contains("Deleted 1 encounter"))
        assertTrue(state.encounters.none { it.id == toDelete.id })
    }

    @Test
    fun onUndoDeleteEncounter_restoresDeletedEncounterAndShowsRestorationMessage() = runTest {
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialEncounters = viewModel.uiState.value.encounters
        val toDelete = initialEncounters.first()

        // Select and delete encounter
        viewModel.onEvent(PersonDetailUiEvent.OnEncounterLongClick(toDelete.id))
        viewModel.onEvent(PersonDetailUiEvent.OnConfirmDeleteSelected)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Deleted 1 encounter", viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.encounters.none { it.id == toDelete.id })

        // Trigger undo
        viewModel.onEvent(PersonDetailUiEvent.OnUndoDeleteEncounter)
        testDispatcher.scheduler.advanceUntilIdle()

        val restoredState = viewModel.uiState.value
        assertEquals("Encounter restored", restoredState.userMessage)
        assertTrue(restoredState.encounters.any { it.id == toDelete.id })
    }

    @Test
    fun profileReminders_createEditDoneSnoozeDeleteAndUndoFlow() = runTest {
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Create via bottom-sheet reminder flow with blank note -> default fallback
        viewModel.onEvent(PersonDetailUiEvent.OnOpenCreateReminderSheet)
        assertTrue(viewModel.uiState.value.isCreateReminderSheetOpen)

        viewModel.onEvent(
            PersonDetailUiEvent.OnCreateReminder(
                dueDate = java.time.LocalDate.now().plusDays(2),
                note = "",
                hasExplicitTime = false
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(!viewModel.uiState.value.isCreateReminderSheetOpen)
        assertEquals(1L, viewModel.uiState.value.reminderSavedEventId)
        assertEquals("Reminder saved", viewModel.uiState.value.userMessage)
        assertEquals(PersonDetailUndoKind.NONE, viewModel.uiState.value.undoKind)
        val created = viewModel.uiState.value.reminders.firstOrNull { it.note == "Follow up with Alex Rivera" }
        assertNotNull(created)

        // Dismiss the toast message before editing
        viewModel.onEvent(PersonDetailUiEvent.OnDismissUserMessage)
        assertNull(viewModel.uiState.value.userMessage)

        // Edit reminder
        viewModel.onEvent(PersonDetailUiEvent.OnOpenEditReminderSheet(created!!))
        assertEquals(created.id, viewModel.uiState.value.editingReminder?.id)
        viewModel.onEvent(
            PersonDetailUiEvent.OnUpdateReminder(
                followUpId = created.id,
                dueDate = java.time.LocalDate.now().plusDays(4),
                note = "Send portfolio link",
                hasExplicitTime = true,
                explicitTime = java.time.LocalTime.of(11, 15)
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2L, viewModel.uiState.value.reminderSavedEventId)
        assertEquals("Reminder saved", viewModel.uiState.value.userMessage)
        assertEquals(PersonDetailUndoKind.NONE, viewModel.uiState.value.undoKind)
        val edited = viewModel.uiState.value.reminders.first { it.id == created.id }
        assertEquals("Send portfolio link", edited.note)
        assertTrue(edited.hasExplicitTime)

        // Snooze reminder keeps toast and does not trigger "Saved" pill confirmation
        val beforeSnooze = edited.dueAt
        viewModel.onEvent(PersonDetailUiEvent.OnSnoozeReminder(created.id))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.reminderSavedEventId)
        assertEquals("Snoozed for 3 days", viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.reminders.first { it.id == created.id }.dueAt.isAfter(beforeSnooze))

        // Mark Done + Undo keeps toast and does not increment reminderSavedEventId
        viewModel.onEvent(PersonDetailUiEvent.OnMarkReminderDone(created.id))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.reminderSavedEventId)
        assertEquals(PersonDetailUndoKind.REMINDER, viewModel.uiState.value.undoKind)
        assertTrue(viewModel.uiState.value.reminders.none { it.id == created.id })

        viewModel.onEvent(PersonDetailUiEvent.OnUndoReminderMutation)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.reminderSavedEventId)
        assertEquals("Reminder restored", viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.reminders.any { it.id == created.id })

        // Delete + Undo keeps toast and does not increment reminderSavedEventId
        viewModel.onEvent(PersonDetailUiEvent.OnDeleteReminder(created.id))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.reminderSavedEventId)
        assertEquals(PersonDetailUndoKind.REMINDER, viewModel.uiState.value.undoKind)
        assertTrue(viewModel.uiState.value.reminders.none { it.id == created.id })

        viewModel.onEvent(PersonDetailUiEvent.OnUndoReminderMutation)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.reminders.any { it.id == created.id })
    }

    @Test
    fun profileReminders_openAndDismissCreateOrEditOverlayClearsFlagsWithoutSavePulse() = runTest {
        val viewModel = PersonDetailViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonDetailUiEvent.OnOpenCreateReminderSheet)
        assertTrue(viewModel.uiState.value.isCreateReminderSheetOpen)
        assertNull(viewModel.uiState.value.editingReminder)

        viewModel.onEvent(PersonDetailUiEvent.OnDismissReminderSheet)
        assertTrue(!viewModel.uiState.value.isCreateReminderSheetOpen)
        assertNull(viewModel.uiState.value.editingReminder)
        assertEquals(0L, viewModel.uiState.value.reminderSavedEventId)

        // Create one reminder, open edit overlay, and dismiss without saving
        viewModel.onEvent(
            PersonDetailUiEvent.OnCreateReminder(
                dueDate = java.time.LocalDate.now().plusDays(1),
                note = "Ping about weekend",
                hasExplicitTime = false
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        val created = viewModel.uiState.value.reminders.first { it.note == "Ping about weekend" }
        assertEquals(1L, viewModel.uiState.value.reminderSavedEventId)

        viewModel.onEvent(PersonDetailUiEvent.OnOpenEditReminderSheet(created))
        assertEquals(created.id, viewModel.uiState.value.editingReminder?.id)
        assertTrue(!viewModel.uiState.value.isCreateReminderSheetOpen)

        viewModel.onEvent(PersonDetailUiEvent.OnDismissReminderSheet)
        assertNull(viewModel.uiState.value.editingReminder)
        assertTrue(!viewModel.uiState.value.isCreateReminderSheetOpen)
        assertEquals(1L, viewModel.uiState.value.reminderSavedEventId)
    }
}
