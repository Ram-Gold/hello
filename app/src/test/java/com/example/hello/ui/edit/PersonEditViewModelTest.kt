package com.example.hello.ui.edit

import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Contact
import com.example.hello.platform.avatar.AvatarStorageManager
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PersonEditViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ContactRepository
    private lateinit var avatarStorage: AvatarStorageManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = ContactRepository()
        avatarStorage = AvatarStorageManager(File(tempFolder.root, "avatars"))
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

    @Test
    fun photoPickAndCrop_stagesAvatarEnablesSaveAndPromotesToCanonicalOnSave() = runTest {
        val viewModel = PersonEditViewModel(
            contactId = "c-1",
            repository = repository,
            avatarStorage = avatarStorage
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Picking a photo opens the cropper with pendingCropUri without modifying photoUri yet
        viewModel.onEvent(PersonEditUiEvent.OnPhotoSelected("content://media/picker/101"))
        assertEquals("content://media/picker/101", viewModel.uiState.value.pendingCropUri)
        assertNull(viewModel.uiState.value.photoUri)
        assertFalse(viewModel.uiState.value.isModified)

        // 2. Confirming crop stages the PNG file, clears pendingCropUri, and enables Save (isModified = true)
        viewModel.onEvent(PersonEditUiEvent.OnPhotoCropped(byteArrayOf(1, 2, 3, 4, 5)))
        val stagedPath = viewModel.uiState.value.photoUri
        assertNull(viewModel.uiState.value.pendingCropUri)
        assertNotNull(stagedPath)
        assertTrue(File(stagedPath!!).exists())
        assertTrue(File(stagedPath).name.startsWith("staged_"))
        assertTrue(viewModel.uiState.value.isModified)
        assertTrue(viewModel.uiState.value.canSave)

        // 3. Tapping Save promotes the staged file to canonical contact_c-1_<timestamp>.png
        viewModel.onEvent(PersonEditUiEvent.OnSaveClick)
        testDispatcher.scheduler.advanceUntilIdle()

        val savedContact = repository.getContact("c-1")
        assertNotNull(savedContact?.photoUri)
        assertFalse("Staged file should be promoted", File(stagedPath).exists())
        assertTrue("Canonical file should exist", File(savedContact!!.photoUri!!).exists())
        assertTrue(File(savedContact.photoUri!!).name.startsWith("contact_c-1_"))
    }

    @Test
    fun photoCropThenDiscard_deletesStagedFileAndPreservesOriginalAvatar() = runTest {
        val viewModel = PersonEditViewModel(
            contactId = "c-1",
            repository = repository,
            avatarStorage = avatarStorage
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonEditUiEvent.OnPhotoSelected("content://media/picker/202"))
        viewModel.onEvent(PersonEditUiEvent.OnPhotoCropped(byteArrayOf(9, 8, 7)))
        val stagedPath = viewModel.uiState.value.photoUri!!
        assertTrue(File(stagedPath).exists())

        // User taps Back -> Confirm Discard
        viewModel.onEvent(PersonEditUiEvent.OnBackClick)
        assertTrue(viewModel.uiState.value.showDiscardDialog)
        viewModel.onEvent(PersonEditUiEvent.OnConfirmDiscard)

        assertFalse("Staged file must be deleted on discard", File(stagedPath).exists())
        assertNull("Repository contact photoUri must remain null", repository.getContact("c-1")?.photoUri)
    }

    @Test
    fun photoDecodeFailed_clearsPendingCropAndShowsFriendlySnackbar() = runTest {
        val viewModel = PersonEditViewModel(
            contactId = "c-1",
            repository = repository,
            avatarStorage = avatarStorage
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(PersonEditUiEvent.OnPhotoSelected("content://media/corrupted/999"))
        assertEquals("content://media/corrupted/999", viewModel.uiState.value.pendingCropUri)

        viewModel.onEvent(PersonEditUiEvent.OnPhotoDecodeFailed)
        assertNull(viewModel.uiState.value.pendingCropUri)
        assertEquals("Could not load selected image", viewModel.uiState.value.userMessage)
    }

    @Test
    fun clearingFields_resetsUiStateValues() = runTest {
        val viewModel = PersonEditViewModel(contactId = "c-1", repository = repository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial state has values
        assertEquals("Alex Rivera", viewModel.uiState.value.name)
        assertEquals("+1 (555) 876-5432", viewModel.uiState.value.phone)
        assertEquals("alex.rivera@example.com", viewModel.uiState.value.email)

        // Clear fields using change events with empty strings or null
        viewModel.onEvent(PersonEditUiEvent.OnNameChange(""))
        assertEquals("", viewModel.uiState.value.name)
        assertFalse(viewModel.uiState.value.canSave) // Blank name cannot be saved

        viewModel.onEvent(PersonEditUiEvent.OnPhoneChange(""))
        assertEquals("", viewModel.uiState.value.phone)

        viewModel.onEvent(PersonEditUiEvent.OnEmailChange(""))
        assertEquals("", viewModel.uiState.value.email)

        viewModel.onEvent(PersonEditUiEvent.OnRoleChange(""))
        assertEquals("", viewModel.uiState.value.role)

        viewModel.onEvent(PersonEditUiEvent.OnCompanyChange(""))
        assertEquals("", viewModel.uiState.value.company)

        viewModel.onEvent(PersonEditUiEvent.OnNotesChange(""))
        assertEquals("", viewModel.uiState.value.notes)

        viewModel.onEvent(PersonEditUiEvent.OnBirthdayChange(null))
        assertNull(viewModel.uiState.value.birthday)
        assertTrue(viewModel.uiState.value.isModified)
    }
}
