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
    fun cadenceCalculation_whenOverdue_marksOverdue() = runTest {
        val pastDate = Instant.now().minus(20, ChronoUnit.DAYS)
        val contact = Contact(
            id = "test-overdue",
            name = "Overdue Contact",
            lastMetAt = pastDate,
            cadenceDays = 14
        )
        repository.updateContact(contact)

        val viewModel = PersonDetailViewModel(contactId = "test-overdue", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val status = viewModel.uiState.value.cadenceStatus
        assertEquals(CadenceHealth.OVERDUE, status.health)
        assertTrue(status.displayText.contains("Overdue"))
    }

    @Test
    fun cadenceCalculation_whenOnTrack_marksOnTrack() = runTest {
        val recentDate = Instant.now().minus(2, ChronoUnit.DAYS)
        val contact = Contact(
            id = "test-ontrack",
            name = "On Track Contact",
            lastMetAt = recentDate,
            cadenceDays = 14
        )
        repository.updateContact(contact)

        val viewModel = PersonDetailViewModel(contactId = "test-ontrack", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val status = viewModel.uiState.value.cadenceStatus
        assertEquals(CadenceHealth.ON_TRACK, status.health)
        assertTrue(status.displayText.contains("On track"))
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
    fun cadenceCalculation_whenNoCadence_marksNoCadence() = runTest {
        val contact = Contact(
            id = "test-nocadence",
            name = "No Cadence Contact",
            cadenceDays = null
        )
        repository.updateContact(contact)

        val viewModel = PersonDetailViewModel(contactId = "test-nocadence", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val status = viewModel.uiState.value.cadenceStatus
        assertEquals(CadenceHealth.NO_CADENCE, status.health)
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
}
