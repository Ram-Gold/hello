package com.example.hello.ui.quickaction

import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.Circle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class QuickActionViewModelTest {

    private lateinit var repository: ContactRepository
    private lateinit var viewModel: QuickActionViewModel

    @Before
    fun setUp() {
        repository = ContactRepository()
        viewModel = QuickActionViewModel(repository)
    }

    @Test
    fun initialState_isClosedAndDefaultChoiceMode() {
        val state = viewModel.uiState.value
        assertFalse(state.isOpen)
        assertEquals(QuickActionMode.CHOICE, state.currentMode)
        assertEquals(FlowAStep.NAME, state.flowAStep)
        assertEquals("", state.newContactName)
        assertTrue(state.suggestedPlaces.isNotEmpty())
        assertTrue(state.suggestedTags.isNotEmpty())
    }

    @Test
    fun onOpenChoice_and_onDismiss_togglesVisibilityAndResetsForm() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        assertTrue(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.CHOICE, viewModel.uiState.value.currentMode)

        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Luna Lovegood"))
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        assertEquals("Luna Lovegood", viewModel.uiState.value.newContactName)

        viewModel.onEvent(QuickActionUiEvent.OnDismiss)
        assertFalse(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.CHOICE, viewModel.uiState.value.currentMode)
        assertEquals("", viewModel.uiState.value.newContactName)
    }

    @Test
    fun flowA_stepProgression_movesThroughAllStepsAndBack() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.NAME, viewModel.uiState.value.currentComponent?.type)

        // Step 0: Name
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Cedric Diggory"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.ROLE, viewModel.uiState.value.currentComponent?.type)

        // Step 1: Role -> Step 2: Company
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.COMPANY, viewModel.uiState.value.currentComponent?.type)

        // Step 2: Company -> Step 3: Place
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(3, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PLACE, viewModel.uiState.value.currentComponent?.type)

        // Step 3: Place -> Step 4: Note
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.NOTE, viewModel.uiState.value.currentComponent?.type)

        // Navigate back to Place
        viewModel.onEvent(QuickActionUiEvent.OnPreviousStep)
        assertEquals(3, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PLACE, viewModel.uiState.value.currentComponent?.type)
    }

    @Test
    fun flowA_selectingPlaceChip_autoAdvancesToNextStep() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Cho Chang"))
        // Navigate to Place step (step index 3 in Default preset: Name -> Role -> Company -> Place)
        viewModel.onEvent(QuickActionUiEvent.OnStepIndexChange(3))
        assertEquals(3, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PLACE, viewModel.uiState.value.currentComponent?.type)

        val place = viewModel.uiState.value.suggestedPlaces.first()
        viewModel.onEvent(QuickActionUiEvent.OnSelectPlaceChip(place.id))

        // Auto-advances to step 4 (NOTE in Default preset)
        assertEquals(place.id, viewModel.uiState.value.selectedPlaceId)
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.NOTE, viewModel.uiState.value.currentComponent?.type)
    }

    @Test
    fun flowA_emptyNameSave_blocksSaveAndFocusesNameStep() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        // Jump to Step 2 (Company) with blank name
        viewModel.onEvent(QuickActionUiEvent.OnStepIndexChange(2))
        viewModel.onEvent(QuickActionUiEvent.OnCompanyChange("Ministry of Magic"))

        // Hit Quick Save while Name is empty
        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        val state = viewModel.uiState.value
        // Must remain open, flag error, and jump back to NAME step (index 0)
        assertTrue("Flow card must remain open when name is blank", state.isOpen)
        assertTrue("Name error must be flagged", state.nameError)
        assertEquals("Must focus Name step (index 0)", 0, state.currentStepIndex)
        assertEquals(CaptureComponentType.NAME, state.currentComponent?.type)
    }

    @Test
    fun flowA_escapeHatchSaveAtStep1_commitsContactAndEncounterInTwoSeconds() {
        val initialCount = repository.contacts.value.size

        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Neville Longbottom"))

        // Trigger Escape Hatch Save at Step 1
        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        val state = viewModel.uiState.value
        assertFalse(state.isOpen)
        assertNotNull(state.userMessage)
        assertTrue(state.userMessage!!.contains("Neville Longbottom"))
        assertEquals(initialCount + 1, repository.contacts.value.size)
        assertEquals("Neville Longbottom", repository.recentContacts.value.first().name)
    }

    @Test
    fun flowA_fullStepCompletion_savesAllAttributesAndFollowUp() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)

        // Step 0: Name
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Ginny Weasley"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 1: Role
        viewModel.onEvent(QuickActionUiEvent.OnRoleChange("Senior Quidditch Editor"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 2: Company
        viewModel.onEvent(QuickActionUiEvent.OnCompanyChange("Daily Prophet"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 3: Place chip (auto-advances to Step 4)
        val placeId = viewModel.uiState.value.suggestedPlaces.first().id
        viewModel.onEvent(QuickActionUiEvent.OnSelectPlaceChip(placeId))
        assertEquals(4, viewModel.uiState.value.currentStepIndex)

        // Step 4: Note & FollowUp
        viewModel.onEvent(QuickActionUiEvent.OnNoteChange("Sports journalist at Daily Prophet"))
        viewModel.onEvent(QuickActionUiEvent.OnSelectFollowUp(7))

        // Extended fields: Birthday & Custom Properties
        val bday = LocalDate.of(1981, 8, 11)
        viewModel.onEvent(QuickActionUiEvent.OnBirthdayChange(bday))
        viewModel.onEvent(QuickActionUiEvent.OnCustomPropertyKeyChange("Quidditch Team"))
        viewModel.onEvent(QuickActionUiEvent.OnCustomPropertyValueChange("Holyhead Harpies"))
        viewModel.onEvent(QuickActionUiEvent.OnAddCustomProperty)

        // Additional Circle
        viewModel.onEvent(QuickActionUiEvent.OnToggleCircle(Circle.WORK))

        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        assertFalse(viewModel.uiState.value.isOpen)
        val saved = repository.recentContacts.value.first()
        assertEquals("Ginny Weasley", saved.name)
        assertTrue(Circle.WORK in saved.circles)
        assertTrue(saved.contextSubtitle.contains("Sports journalist at Daily Prophet"))
        assertTrue(repository.followUps.value.any { it.contactId == saved.id })

        val fullContact = repository.fullContacts.value[saved.id]
        assertNotNull(fullContact)
        assertEquals("Senior Quidditch Editor", fullContact!!.role)
        assertEquals("Daily Prophet", fullContact.company)
        assertEquals(bday, fullContact.birthday)
        assertEquals("Holyhead Harpies", fullContact.customProperties["Quidditch Team"])
    }

    @Test
    fun flowB_searchAndSelectContact_morphsToEncounterSheetAndSaves() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, viewModel.uiState.value.currentMode)
        assertNull(viewModel.uiState.value.selectedContact)

        // Instant search
        viewModel.onEvent(QuickActionUiEvent.OnSearchQueryChange("Maya"))
        assertEquals(1, viewModel.uiState.value.filteredContacts.size)
        val maya = viewModel.uiState.value.filteredContacts.first()
        assertEquals("Maya Patel", maya.name)

        // Select contact -> morphs into encounter details stage
        viewModel.onEvent(QuickActionUiEvent.OnSelectExistingContact(maya))
        assertEquals(maya.id, viewModel.uiState.value.selectedContact?.id)

        // Set When, Where, Note
        val yesterday = LocalDate.now().minusDays(1)
        viewModel.onEvent(QuickActionUiEvent.OnEncounterDateChange(yesterday))
        val place = viewModel.uiState.value.suggestedPlaces.first()
        viewModel.onEvent(QuickActionUiEvent.OnEncounterPlaceChange(place.id))
        viewModel.onEvent(QuickActionUiEvent.OnEncounterNoteChange("Caught up about her new job at Figma"))

        // Save
        viewModel.onEvent(QuickActionUiEvent.OnSaveFlowBEncounter)

        assertFalse(viewModel.uiState.value.isOpen)
        assertNotNull(viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.userMessage!!.contains("Maya Patel"))

        val updatedMaya = repository.recentContacts.value.first()
        assertEquals(maya.id, updatedMaya.id)
        assertEquals("Met yesterday", updatedMaya.lastEncounterText)
        assertTrue(updatedMaya.contextSubtitle.contains("Caught up about her new job at Figma"))
    }

    @Test
    fun locationExplainer_togglesGracefullyAndNeverBlocksCapture() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertFalse(viewModel.uiState.value.showLocationExplainer)

        viewModel.onEvent(QuickActionUiEvent.OnRequestLocation)
        assertTrue(viewModel.uiState.value.showLocationExplainer)

        viewModel.onEvent(QuickActionUiEvent.OnDismissLocationExplainer)
        assertFalse(viewModel.uiState.value.showLocationExplainer)

        // Confirm user can also grant location which resolves nearby place chip and advances
        viewModel.onEvent(QuickActionUiEvent.OnRequestLocation)
        viewModel.onEvent(QuickActionUiEvent.OnLocationPermissionResult(granted = true))
        assertFalse(viewModel.uiState.value.showLocationExplainer)
        assertNotNull(viewModel.uiState.value.selectedPlaceId)
    }
}
