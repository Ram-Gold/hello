package com.example.hello.ui.quickaction

import com.example.hello.data.repository.ContactRepository
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
    fun flowA_stepProgression_movesThroughAllFourStepsAndBack() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertEquals(FlowAStep.NAME, viewModel.uiState.value.flowAStep)

        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Cedric Diggory"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(FlowAStep.PLACE, viewModel.uiState.value.flowAStep)

        // Skip place
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(FlowAStep.TAGS_AND_CIRCLES, viewModel.uiState.value.flowAStep)

        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(FlowAStep.NOTE_AND_FOLLOW_UP, viewModel.uiState.value.flowAStep)

        // Navigate back
        viewModel.onEvent(QuickActionUiEvent.OnPreviousStep)
        assertEquals(FlowAStep.TAGS_AND_CIRCLES, viewModel.uiState.value.flowAStep)
    }

    @Test
    fun flowA_selectingPlaceChip_autoAdvancesToTagsAndCirclesStep() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Cho Chang"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(FlowAStep.PLACE, viewModel.uiState.value.flowAStep)

        val place = viewModel.uiState.value.suggestedPlaces.first()
        viewModel.onEvent(QuickActionUiEvent.OnSelectPlaceChip(place.id))

        // Auto-advances to TAGS_AND_CIRCLES
        assertEquals(place.id, viewModel.uiState.value.selectedPlaceId)
        assertEquals(FlowAStep.TAGS_AND_CIRCLES, viewModel.uiState.value.flowAStep)
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

        // Step 1: Name
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Ginny Weasley"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 2: Place chip (auto-advances)
        val placeId = viewModel.uiState.value.suggestedPlaces.first().id
        viewModel.onEvent(QuickActionUiEvent.OnSelectPlaceChip(placeId))
        assertEquals(FlowAStep.TAGS_AND_CIRCLES, viewModel.uiState.value.flowAStep)

        // Step 3: Circles & Tags
        viewModel.onEvent(QuickActionUiEvent.OnToggleCircle(Circle.WORK))
        val tagId = viewModel.uiState.value.suggestedTags.first().id
        viewModel.onEvent(QuickActionUiEvent.OnToggleTag(tagId))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(FlowAStep.NOTE_AND_FOLLOW_UP, viewModel.uiState.value.flowAStep)

        // Step 4: Note & FollowUp
        viewModel.onEvent(QuickActionUiEvent.OnNoteChange("Sports journalist at Daily Prophet"))
        viewModel.onEvent(QuickActionUiEvent.OnSelectFollowUp(7))
        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        assertFalse(viewModel.uiState.value.isOpen)
        val saved = repository.recentContacts.value.first()
        assertEquals("Ginny Weasley", saved.name)
        assertTrue(Circle.WORK in saved.circles)
        assertTrue(saved.contextSubtitle.contains("Sports journalist at Daily Prophet"))
        assertTrue(repository.followUps.value.any { it.contactId == saved.id })
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
