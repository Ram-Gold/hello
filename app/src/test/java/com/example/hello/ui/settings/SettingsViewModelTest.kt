package com.example.hello.ui.settings

import com.example.hello.data.preferences.FakeUserPreferencesRepository
import com.example.hello.data.preferences.ThemeStyle
import com.example.hello.data.preferences.UserPreferences
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.FlowPreset
import com.example.hello.domain.model.RelationshipTerminology
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePreferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(
                userName = "Taylor",
                hasCompletedOnboarding = true,
                preferredTerminology = RelationshipTerminology.PEOPLE
            )
        )
        contactRepository = ContactRepository()
        viewModel = SettingsViewModel(fakePreferencesRepository, contactRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsTerminologyAndTags() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("Taylor", state.userName)
        assertEquals(RelationshipTerminology.PEOPLE, state.terminology)
        assertEquals(ThemeStyle.MONOCHROME, state.themeStyle)
        assertTrue(state.tags.isNotEmpty())
    }

    @Test
    fun onOpenAndDismissEditNameDialog_updatesDialogState() = runTest {
        assertFalse(viewModel.uiState.value.isEditingName)

        viewModel.onEvent(SettingsUiEvent.OnOpenEditNameDialog)
        assertTrue(viewModel.uiState.value.isEditingName)

        viewModel.onEvent(SettingsUiEvent.OnDismissEditNameDialog)
        assertFalse(viewModel.uiState.value.isEditingName)
    }

    @Test
    fun onSaveUserName_updatesRepositoryWhenNonEmpty_andIgnoresEmpty() = runTest {
        advanceUntilIdle()
        assertEquals("Taylor", viewModel.uiState.value.userName)

        // Attempt saving empty or blank string -> should be ignored
        viewModel.onEvent(SettingsUiEvent.OnOpenEditNameDialog)
        viewModel.onEvent(SettingsUiEvent.OnSaveUserName("   "))
        advanceUntilIdle()

        // Name is unchanged, dialog is still open or at least name not updated
        assertEquals("Taylor", viewModel.uiState.value.userName)

        // Save a valid new name
        viewModel.onEvent(SettingsUiEvent.OnSaveUserName("  Jordan  "))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditingName)
        assertEquals("Jordan", viewModel.uiState.value.userName)
        assertEquals("Name updated", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onSelectThemeStyle_switchesToWarmEarth_andCanSwitchBack() = runTest {
        advanceUntilIdle()
        assertEquals(ThemeStyle.MONOCHROME, viewModel.uiState.value.themeStyle)

        // Switch to warm earth
        viewModel.onEvent(SettingsUiEvent.OnSelectThemeStyle(ThemeStyle.WARM_EARTH))
        advanceUntilIdle()

        assertEquals(ThemeStyle.WARM_EARTH, viewModel.uiState.value.themeStyle)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Warm Organic") == true)

        // Switch back to monochrome default
        viewModel.onEvent(SettingsUiEvent.OnSelectThemeStyle(ThemeStyle.MONOCHROME))
        advanceUntilIdle()

        assertEquals(ThemeStyle.MONOCHROME, viewModel.uiState.value.themeStyle)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Minimalist") == true)
    }

    @Test
    fun onSelectTerminology_updatesStateAndPersists() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnSelectTerminology(RelationshipTerminology.FRIENDS))
        advanceUntilIdle()

        assertEquals(RelationshipTerminology.FRIENDS, viewModel.uiState.value.terminology)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Friends") == true)
    }

    @Test
    fun onCreateTag_addsTagAndClosesDialog() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnOpenAddTagDialog)
        assertTrue(viewModel.uiState.value.isAddTagDialogOpen)

        viewModel.onEvent(SettingsUiEvent.OnCreateTag("Photography", "#8A9A5B"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAddTagDialogOpen)
        assertTrue(contactRepository.tags.value.any { it.name == "Photography" })
        assertTrue(viewModel.uiState.value.userMessage?.contains("Photography") == true)
    }

    @Test
    fun onUpdateTag_updatesTagInRepository() = runTest {
        advanceUntilIdle()
        val initialTag = viewModel.uiState.value.tags.first()
        viewModel.onEvent(SettingsUiEvent.OnOpenEditTagDialog(initialTag))
        assertEquals(initialTag, viewModel.uiState.value.editingTag)

        val updated = initialTag.copy(name = "NewName", colorHex = "#D87D7D")
        viewModel.onEvent(SettingsUiEvent.OnUpdateTag(updated))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingTag)
        val retrieved = contactRepository.tags.value.first { it.id == initialTag.id }
        assertEquals("NewName", retrieved.name)
        assertEquals("#D87D7D", retrieved.colorHex)
    }

    @Test
    fun onConfirmMergeTag_mergesTagsAndClosesDialog() = runTest {
        advanceUntilIdle()
        val source = viewModel.uiState.value.tags.first { it.id == "tag-tech" }
        val target = viewModel.uiState.value.tags.first { it.id == "tag-design" }

        viewModel.onEvent(SettingsUiEvent.OnOpenMergeTagDialog(source))
        assertEquals(source, viewModel.uiState.value.mergingSourceTag)

        viewModel.onEvent(SettingsUiEvent.OnConfirmMergeTag(source.id, target.id))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.mergingSourceTag)
        assertNull(contactRepository.tags.value.firstOrNull { it.id == "tag-tech" })
        assertTrue(viewModel.uiState.value.userMessage?.contains("Merged") == true)
    }

    @Test
    fun onDeleteTag_removesTagFromRepository() = runTest {
        advanceUntilIdle()
        val tagToDelete = viewModel.uiState.value.tags.first { it.id == "tag-coffee" }

        viewModel.onEvent(SettingsUiEvent.OnDeleteTag(tagToDelete.id))
        advanceUntilIdle()

        assertNull(contactRepository.tags.value.firstOrNull { it.id == tagToDelete.id })
        assertTrue(viewModel.uiState.value.userMessage?.contains("Deleted tag") == true)
    }

    @Test
    fun onExportMarkdownZip_generatesPayloadAndFeedback() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnExportMarkdownZip)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.exportSharePayload)
        assertTrue(viewModel.uiState.value.exportSharePayload?.contains("Total Contacts:") == true)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Markdown") == true)
    }

    @Test
    fun onExportBackupJson_generatesJsonPayload() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnExportBackupJson)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.exportSharePayload)
        assertTrue(viewModel.uiState.value.exportSharePayload?.contains("\"version\": 1") == true)
    }

    @Test
    fun onRebuildCache_providesFeedback() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnRebuildCache)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.userMessage?.contains("Rebuilt cache") == true)
    }

    @Test
    fun onAppearanceEvents_updatesThemeAndReduceAnimations() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnSelectThemeMode(com.example.hello.data.preferences.ThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(com.example.hello.data.preferences.ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.onEvent(SettingsUiEvent.OnToggleDynamicColor(true))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.dynamicColor)

        viewModel.onEvent(SettingsUiEvent.OnToggleReduceAnimations(true))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.reduceAnimations)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Reduced animations") == true)
    }

    @Test
    fun onPreferencesEvents_updatesCadenceAndNotificationDefaults() = runTest {
        viewModel.onEvent(SettingsUiEvent.OnSetDefaultCadenceDays(60))
        advanceUntilIdle()
        assertEquals(60, viewModel.uiState.value.defaultCadenceDays)

        viewModel.onEvent(SettingsUiEvent.OnSetNotificationTime(10, 30))
        advanceUntilIdle()
        assertEquals(10, viewModel.uiState.value.notificationHour)
        assertEquals(30, viewModel.uiState.value.notificationMinute)

        viewModel.onEvent(SettingsUiEvent.OnToggleDefaultIncludeLocation(false))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.defaultIncludeLocation)
    }

    @Test
    fun onSelectActiveFlowPreset_updatesPreferencesAndState() = runTest {
        advanceUntilIdle()
        viewModel.onEvent(SettingsUiEvent.OnSelectActiveFlowPreset(FlowPreset.PRESET_CONFERENCE))
        advanceUntilIdle()

        assertEquals(FlowPreset.PRESET_CONFERENCE, viewModel.uiState.value.activeFlowPresetId)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Conference") == true)
    }

    @Test
    fun onMoveComponentUpAndDown_updatesComponentOrder() = runTest {
        advanceUntilIdle()
        val defaultPreset = viewModel.uiState.value.flowPresets.first { it.id == FlowPreset.PRESET_DEFAULT }
        val compAt1 = defaultPreset.components[1] // ROLE

        // Move Role up to step 0
        viewModel.onEvent(SettingsUiEvent.OnMoveComponentUp(defaultPreset.id, compAt1.id))
        advanceUntilIdle()

        val updatedPreset = viewModel.uiState.value.flowPresets.first { it.id == FlowPreset.PRESET_DEFAULT }
        assertEquals(compAt1.id, updatedPreset.components[0].id)
        assertEquals(0, updatedPreset.components[0].stepOrder)

        // Move it down back to step 1
        viewModel.onEvent(SettingsUiEvent.OnMoveComponentDown(defaultPreset.id, compAt1.id))
        advanceUntilIdle()

        val restoredPreset = viewModel.uiState.value.flowPresets.first { it.id == FlowPreset.PRESET_DEFAULT }
        assertEquals(compAt1.id, restoredPreset.components[1].id)
    }

    @Test
    fun onToggleComponentEnabled_updatesComponentStatus() = runTest {
        advanceUntilIdle()
        val defaultPreset = viewModel.uiState.value.flowPresets.first { it.id == FlowPreset.PRESET_DEFAULT }
        val compAt1 = defaultPreset.components[1] // ROLE
        assertTrue(compAt1.isEnabled)

        viewModel.onEvent(SettingsUiEvent.OnToggleComponentEnabled(defaultPreset.id, compAt1.id, false))
        advanceUntilIdle()

        val updatedPreset = viewModel.uiState.value.flowPresets.first { it.id == defaultPreset.id }
        val updatedComp = updatedPreset.components.first { it.id == compAt1.id }
        assertFalse(updatedComp.isEnabled)
    }

    @Test
    fun onCreateAndOnDeleteCustomPreset_managesCustomPresets() = runTest {
        advanceUntilIdle()
        val initialCount = viewModel.uiState.value.flowPresets.size

        viewModel.onEvent(SettingsUiEvent.OnCreateCustomPreset(name = "Hackathon", description = "Speed capture for hackathon"))
        advanceUntilIdle()

        assertEquals(initialCount + 1, viewModel.uiState.value.flowPresets.size)
        val created = viewModel.uiState.value.flowPresets.first { it.name == "Hackathon" }
        assertEquals("Hackathon", created.name)
        assertFalse(created.isSystemPreset)

        // Delete custom preset
        viewModel.onEvent(SettingsUiEvent.OnDeleteCustomPreset(created.id))
        advanceUntilIdle()

        assertEquals(initialCount, viewModel.uiState.value.flowPresets.size)
        assertNull(viewModel.uiState.value.flowPresets.firstOrNull { it.id == created.id })
    }

    @Test
    fun onAddComponentToPreset_addsNewComponentToPreset() = runTest {
        advanceUntilIdle()
        val defaultPreset = viewModel.uiState.value.flowPresets.first { it.id == FlowPreset.PRESET_DEFAULT }
        val initialSize = defaultPreset.components.size

        viewModel.onEvent(
            SettingsUiEvent.OnAddComponentToPreset(
                presetId = defaultPreset.id,
                type = CaptureComponentType.BIRTHDAY,
                label = "Birthday"
            )
        )
        advanceUntilIdle()

        val updatedPreset = viewModel.uiState.value.flowPresets.first { it.id == defaultPreset.id }
        assertEquals(initialSize + 1, updatedPreset.components.size)
        assertTrue(updatedPreset.components.any { it.type == CaptureComponentType.BIRTHDAY })
    }
}
