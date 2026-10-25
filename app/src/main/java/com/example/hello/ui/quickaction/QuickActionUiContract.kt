package com.example.hello.ui.quickaction

import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag
import java.time.LocalDate

enum class QuickActionMode {
    CHOICE,
    FLOW_A_ADD_CONTACT,
    FLOW_B_LOG_ENCOUNTER
}

enum class FlowAStep(val stepNumber: Int, val totalSteps: Int = 4, val title: String) {
    NAME(1, 4, "Who did you meet?"),
    PLACE(2, 4, "Where did you meet?"),
    TAGS_AND_CIRCLES(3, 4, "Circles & Tags"),
    NOTE_AND_FOLLOW_UP(4, 4, "Quick Note & Follow-Up")
}

data class QuickActionUiState(
    val isOpen: Boolean = false,
    val currentMode: QuickActionMode = QuickActionMode.CHOICE,
    // Suggestions from Repository
    val suggestedPlaces: List<Place> = emptyList(),
    val suggestedTags: List<Tag> = emptyList(),
    val allContacts: List<ContactSummary> = emptyList(),
    val recentContacts: List<ContactSummary> = emptyList(),
    val filteredContacts: List<ContactSummary> = emptyList(),
    // Flow A: Add Contact State
    val flowAStep: FlowAStep = FlowAStep.NAME,
    val newContactName: String = "",
    val selectedPlaceId: String? = null,
    val customPlaceName: String = "",
    val selectedCircles: Set<Circle> = setOf(Circle.FRIENDS),
    val selectedTagIds: Set<String> = emptySet(),
    val customTagInput: String = "",
    val encounterNote: String = "",
    val selectedFollowUpDays: Int? = null,
    // Flow B: Log Encounter State
    val searchQuery: String = "",
    val selectedContact: ContactSummary? = null,
    val encounterDate: LocalDate = LocalDate.now(),
    val encounterPlaceId: String? = null,
    val encounterCustomPlaceName: String = "",
    val encounterNoteFlowB: String = "",
    // Foreground Location Explainer
    val showLocationExplainer: Boolean = false,
    val isLocationGranted: Boolean = false,
    // Feedback message
    val userMessage: String? = null
)

sealed interface QuickActionUiEvent {
    data object OnOpenChoice : QuickActionUiEvent
    data object OnDismiss : QuickActionUiEvent
    data object OnSelectAddContact : QuickActionUiEvent
    data object OnSelectLogEncounter : QuickActionUiEvent
    data class OnOpenLogEncounterForContact(val contact: ContactSummary) : QuickActionUiEvent
    data object OnBackToChoice : QuickActionUiEvent

    // Flow A Events
    data class OnNameChange(val name: String) : QuickActionUiEvent
    data class OnSelectPlaceChip(val placeId: String) : QuickActionUiEvent
    data class OnCustomPlaceNameChange(val customName: String) : QuickActionUiEvent
    data class OnToggleCircle(val circle: Circle) : QuickActionUiEvent
    data class OnToggleTag(val tagId: String) : QuickActionUiEvent
    data class OnNoteChange(val note: String) : QuickActionUiEvent
    data class OnSelectFollowUp(val days: Int?) : QuickActionUiEvent
    data object OnNextStep : QuickActionUiEvent
    data object OnPreviousStep : QuickActionUiEvent
    data class OnJumpToStep(val step: FlowAStep) : QuickActionUiEvent
    data object OnQuickSaveFlowA : QuickActionUiEvent

    // Flow B Events
    data class OnSearchQueryChange(val query: String) : QuickActionUiEvent
    data class OnSelectExistingContact(val contact: ContactSummary) : QuickActionUiEvent
    data object OnClearSelectedContact : QuickActionUiEvent
    data class OnEncounterDateChange(val date: LocalDate) : QuickActionUiEvent
    data class OnEncounterPlaceChange(val placeId: String?, val customName: String = "") : QuickActionUiEvent
    data class OnEncounterNoteChange(val note: String) : QuickActionUiEvent
    data object OnSaveFlowBEncounter : QuickActionUiEvent

    // Foreground Location Explainer Events
    data object OnRequestLocation : QuickActionUiEvent
    data object OnDismissLocationExplainer : QuickActionUiEvent
    data class OnLocationPermissionResult(val granted: Boolean) : QuickActionUiEvent

    // User Message
    data object OnDismissUserMessage : QuickActionUiEvent
}
