package com.example.hello.ui.quickaction

import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.CaptureComponentType
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
    fun onOpenChoice_and_onDismiss_preservesActiveModeDuringExit_andResetsOnReopen() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        assertTrue(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.CHOICE, viewModel.uiState.value.currentMode)

        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Luna Lovegood"))
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        assertEquals("Luna Lovegood", viewModel.uiState.value.newContactName)

        // Dismissing must keep FLOW_A_ADD_CONTACT and input fields intact during exit animation
        viewModel.onEvent(QuickActionUiEvent.OnDismiss)
        assertFalse(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        assertEquals("Luna Lovegood", viewModel.uiState.value.newContactName)

        // Opening a new flow resets draft state cleanly
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertTrue(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        assertEquals("", viewModel.uiState.value.newContactName)
    }

    @Test
    fun onQuickSaveFlowA_preservesActiveModeAndDraftDuringExit_andResetsOnReopen() {
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Harry Potter"))
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("+1234567890"))

        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        val stateAfterSave = viewModel.uiState.value
        assertFalse(stateAfterSave.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, stateAfterSave.currentMode)
        assertEquals("Harry Potter", stateAfterSave.newContactName)
        assertEquals("+1234567890", stateAfterSave.phone)
        assertNotNull(stateAfterSave.userMessage)

        // Starting another capture session resets previous draft inputs cleanly
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        val stateAfterReopen = viewModel.uiState.value
        assertTrue(stateAfterReopen.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, stateAfterReopen.currentMode)
        assertEquals("", stateAfterReopen.newContactName)
        assertEquals("", stateAfterReopen.phone)
    }

    @Test
    fun onSaveFlowBEncounter_preservesActiveModeAndDraftDuringExit_andResetsOnReopen() {
        viewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
        val sample = viewModel.uiState.value.allContacts.first()
        viewModel.onEvent(QuickActionUiEvent.OnSelectExistingContact(sample))
        viewModel.onEvent(QuickActionUiEvent.OnEncounterNoteChange("Catch up over tea"))

        viewModel.onEvent(QuickActionUiEvent.OnSaveFlowBEncounter)

        val stateAfterSave = viewModel.uiState.value
        assertFalse(stateAfterSave.isOpen)
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, stateAfterSave.currentMode)
        assertEquals("Catch up over tea", stateAfterSave.encounterNoteFlowB)
        assertEquals(sample.id, stateAfterSave.selectedContact?.id)
        assertNotNull(stateAfterSave.userMessage)

        // Starting another Flow B session resets draft state cleanly
        viewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
        val stateAfterReopen = viewModel.uiState.value
        assertTrue(stateAfterReopen.isOpen)
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, stateAfterReopen.currentMode)
        assertNull(stateAfterReopen.selectedContact)
        assertEquals("", stateAfterReopen.encounterNoteFlowB)
    }

    @Test
    fun flowA_stepProgression_movesThroughAllSixStepsAndBack() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.NAME, viewModel.uiState.value.currentComponent?.type)

        // Step 0: Name -> Step 1: Role
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Cedric Diggory"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.ROLE, viewModel.uiState.value.currentComponent?.type)

        // Step 1: Role -> Step 2: Phone
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PHONE, viewModel.uiState.value.currentComponent?.type)

        // Step 2: Phone -> Step 3: Email
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(3, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.EMAIL, viewModel.uiState.value.currentComponent?.type)

        // Step 3: Email -> Step 4: Place
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PLACE, viewModel.uiState.value.currentComponent?.type)

        // Step 4: Place -> Step 5: Note
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(5, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.NOTE, viewModel.uiState.value.currentComponent?.type)

        // Navigate back to Place
        viewModel.onEvent(QuickActionUiEvent.OnPreviousStep)
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PLACE, viewModel.uiState.value.currentComponent?.type)
    }

    @Test
    fun flowA_selectingPlaceChip_autoAdvancesToNextStep() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Cho Chang"))
        // Navigate to Place step (step index 4 in Default preset: Name -> Role -> Phone -> Email -> Place -> Note)
        viewModel.onEvent(QuickActionUiEvent.OnStepIndexChange(4))
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PLACE, viewModel.uiState.value.currentComponent?.type)

        val place = viewModel.uiState.value.suggestedPlaces.first()
        viewModel.onEvent(QuickActionUiEvent.OnSelectPlaceChip(place.id))

        // Auto-advances to step 5 (NOTE in Default preset)
        assertEquals(place.id, viewModel.uiState.value.selectedPlaceId)
        assertEquals(5, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.NOTE, viewModel.uiState.value.currentComponent?.type)
    }

    @Test
    fun flowA_emptyNameSave_blocksSaveAndFocusesNameStep() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        // Jump to Step 2 (Phone) with blank name
        viewModel.onEvent(QuickActionUiEvent.OnStepIndexChange(2))
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("+1 555 000 1111"))

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
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, state.currentMode)
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

        // Step 2: Phone
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("+44 20 7946 0958"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 3: Email
        viewModel.onEvent(QuickActionUiEvent.OnEmailChange("ginny@dailyprophet.co.uk"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 4: Place chip (auto-advances to Step 5)
        val placeId = viewModel.uiState.value.suggestedPlaces.first().id
        viewModel.onEvent(QuickActionUiEvent.OnSelectPlaceChip(placeId))
        assertEquals(5, viewModel.uiState.value.currentStepIndex)

        // Step 5: Note & FollowUp
        viewModel.onEvent(QuickActionUiEvent.OnNoteChange("Sports journalist at Daily Prophet"))
        viewModel.onEvent(QuickActionUiEvent.OnSelectFollowUp(7))

        // Extended fields: Company, Birthday & Custom Properties
        viewModel.onEvent(QuickActionUiEvent.OnCompanyChange("Daily Prophet"))
        val bday = LocalDate.of(1981, 8, 11)
        viewModel.onEvent(QuickActionUiEvent.OnBirthdayChange(bday))
        viewModel.onEvent(QuickActionUiEvent.OnCustomPropertyKeyChange("Quidditch Team"))
        viewModel.onEvent(QuickActionUiEvent.OnCustomPropertyValueChange("Holyhead Harpies"))
        viewModel.onEvent(QuickActionUiEvent.OnAddCustomProperty)

        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        assertFalse(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        val saved = repository.recentContacts.value.first()
        assertEquals("Ginny Weasley", saved.name)
        assertTrue(saved.contextSubtitle.contains("Sports journalist at Daily Prophet"))
        assertTrue(repository.followUps.value.any { it.contactId == saved.id })

        val fullContact = repository.fullContacts.value[saved.id]
        assertNotNull(fullContact)
        assertEquals("Senior Quidditch Editor", fullContact!!.role)
        assertEquals("+44 20 7946 0958", fullContact.phone)
        assertEquals("ginny@dailyprophet.co.uk", fullContact.email)
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
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, viewModel.uiState.value.currentMode)
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

    @Test
    fun pinnedTagChipStrip_togglesTagsAndCustomTagInput() {
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertFalse(viewModel.uiState.value.isCustomTagInputExpanded)

        // Expand "+ tag" inline custom tag input, add a custom tag, and verify it auto-collapses and selects
        viewModel.onEvent(QuickActionUiEvent.OnToggleCustomTagInput)
        assertTrue(viewModel.uiState.value.isCustomTagInputExpanded)
        viewModel.onEvent(QuickActionUiEvent.OnCustomTagInputChange("#hackathon"))
        viewModel.onEvent(QuickActionUiEvent.OnAddCustomTag)
        assertFalse(viewModel.uiState.value.isCustomTagInputExpanded)
        assertEquals("", viewModel.uiState.value.customTagInput)
        val addedTag = viewModel.uiState.value.suggestedTags.first { it.name.equals("hackathon", ignoreCase = true) }
        assertTrue(addedTag.id in viewModel.uiState.value.selectedTagIds)

        // Save in 1 step from step 0 with Name + Role + Phone + custom tag
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Jordan Lee"))
        viewModel.onEvent(QuickActionUiEvent.OnRoleChange("Staff Designer"))
        viewModel.onEvent(QuickActionUiEvent.OnCompanyChange("Linear"))
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("+1 555 999 8888"))

        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        assertFalse(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        val saved = repository.contacts.value.first()
        assertEquals("Jordan Lee", saved.name)
        assertTrue(saved.tags.any { it.id == addedTag.id })
        assertTrue(saved.contextSubtitle.contains("Staff Designer at Linear"))
        assertEquals("+1 555 999 8888", repository.getContact(saved.id)?.phone)
    }
}
