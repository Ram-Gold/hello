package com.example.hello.ui.detail

import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Circle
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
}
