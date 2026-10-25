package com.example.hello.ui.edit

import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
class PersonEditViewModelTest {

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
    fun loadContact_initializesFormFieldsCorrectly() = runTest {
        val viewModel = PersonEditViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Alex Rivera", state.name)
        assertEquals("+1 (555) 876-5432", state.phone)
        assertEquals("alex.rivera@example.com", state.email)
        assertEquals(14, state.cadenceDays)
        assertTrue(state.aliases.contains("Rivera"))
        assertFalse(state.isModified)
        assertTrue(state.canSave)
    }

    @Test
    fun onNameChange_marksModifiedAndUpdatesSaveState() = runTest {
        val viewModel = PersonEditViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonEditUiEvent.OnNameChange("Alex Rivera Jr."))
        assertTrue(viewModel.uiState.value.isModified)
        assertTrue(viewModel.uiState.value.canSave)

        viewModel.onEvent(PersonEditUiEvent.OnNameChange(""))
        assertFalse(viewModel.uiState.value.canSave)
    }

    @Test
    fun addAndRemoveAlias_updatesAliasesList() = runTest {
        val viewModel = PersonEditViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonEditUiEvent.OnAddAlias("Al"))
        assertTrue(viewModel.uiState.value.aliases.contains("Al"))
        assertTrue(viewModel.uiState.value.isModified)

        viewModel.onEvent(PersonEditUiEvent.OnRemoveAlias("Al"))
        assertFalse(viewModel.uiState.value.aliases.contains("Al"))
    }

    @Test
    fun cadenceIntervalSelection_updatesCadenceDays() = runTest {
        val viewModel = PersonEditViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonEditUiEvent.OnSelectCadence(30))
        assertEquals(30, viewModel.uiState.value.cadenceDays)
        assertFalse(viewModel.uiState.value.isCustomCadence)

        viewModel.onEvent(PersonEditUiEvent.OnSelectCadence(null))
        assertNull(viewModel.uiState.value.cadenceDays)

        viewModel.onEvent(PersonEditUiEvent.OnCustomCadenceChange("45"))
        assertEquals(45, viewModel.uiState.value.cadenceDays)
        assertTrue(viewModel.uiState.value.isCustomCadence)
    }

    @Test
    fun saveChanges_persistsToRepository() = runTest {
        val viewModel = PersonEditViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonEditUiEvent.OnNameChange("Alex Updated"))
        viewModel.onEvent(PersonEditUiEvent.OnPhoneChange("+1 (555) 000-1111"))
        viewModel.onEvent(PersonEditUiEvent.OnSaveClick)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = repository.getContact("c-1")
        assertNotNull(updated)
        assertEquals("Alex Updated", updated?.name)
        assertEquals("+1 (555) 000-1111", updated?.phone)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun archiveContact_updatesRepositoryArchiveStatus() = runTest {
        val viewModel = PersonEditViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonEditUiEvent.OnConfirmArchive)
        testDispatcher.scheduler.advanceUntilIdle()

        val contact = repository.getContact("c-1")
        assertTrue(contact?.isArchived == true)
        assertTrue(viewModel.uiState.value.isArchivedCompleted)
    }

    @Test
    fun deleteContact_removesFromRepository() = runTest {
        val viewModel = PersonEditViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonEditUiEvent.OnConfirmDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        val contact = repository.getContact("c-1")
        assertNull(contact)
        assertTrue(viewModel.uiState.value.isDeletedCompleted)
    }
}
