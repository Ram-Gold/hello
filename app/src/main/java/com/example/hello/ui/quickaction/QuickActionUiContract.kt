package com.example.hello.ui.quickaction

import com.example.hello.domain.model.CaptureComponent
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.FlowPreset
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class QuickActionMode {
    CHOICE,
    FLOW_A_ADD_CONTACT,
    FLOW_B_LOG_ENCOUNTER
}

enum class QuickCaptureDetailKey(val label: String) {
    LOCATION("Location"),
    TIME("Time"),
    TAGS("Tags"),
    REMINDER("Reminder"),
    OCCUPATION("Occupation"),
    CONTACT_INFO("Phone & Email"),
    BIRTHDAY("Birthday"),
    CUSTOM("Custom");

    companion object {
        val DEFAULT_ADD_CONTACT = listOf(LOCATION, TAGS, REMINDER, OCCUPATION)
        val DEFAULT_MET_AGAIN = listOf(LOCATION, TIME, TAGS, REMINDER)
    }
}

enum class FlowAStep(val stepNumber: Int, val totalSteps: Int = 6, val title: String) {
    NAME(1, 6, "Who did you meet?"),
    ROLE(2, 6, "What do they do?"),
    COMPANY(2, 6, "Where do they work or study?"),
    PHONE(3, 6, "Phone number"),
    EMAIL(4, 6, "Email address"),
    BIRTHDAY(4, 6, "When is their birthday?"),
    CUSTOM(4, 6, "Custom property"),
    PLACE(5, 6, "Where did you meet?"),
    TAGS(5, 6, "Tags"),
    NOTE_AND_FOLLOW_UP(6, 6, "Quick Note & Follow-Up")
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
    // Flow A: Modular Capture Engine State (ADR-0012 & ADR-0013)
    val activeFlowPreset: FlowPreset? = null,
    val currentStepIndex: Int = 0,
    val flowAStep: FlowAStep = FlowAStep.NAME,
    val newContactName: String = "",
    val role: String = "",
    val company: String = "",
    val birthday: LocalDate? = null,
    val phone: String = "",
    val email: String = "",
    val selectedPlaceId: String? = null,
    val customPlaceName: String = "",
    val selectedTagIds: Set<String> = emptySet(),
    val customTagInput: String = "",
    val isCustomTagInputExpanded: Boolean = false,
    val encounterNote: String = "",
    val selectedFollowUpDays: Int? = null,
    val customProperties: Map<String, String> = emptyMap(),
    val customPropertyKey: String = "",
    val customPropertyValue: String = "",
    val nameError: Boolean = false,
    val phoneError: String? = null,
    // Flow B: Log Encounter State
    val searchQuery: String = "",
    val selectedContact: ContactSummary? = null,
    val selectedContacts: List<ContactSummary> = emptyList(),
    val isFlowBMultiSelectMode: Boolean = false,
    val isFlowBParticipantPickerOpen: Boolean = false,
    val encounterDate: LocalDate = LocalDate.now(),
    val encounterTime: LocalTime = LocalTime.now().truncatedTo(ChronoUnit.MINUTES),
    val isEncounterTimeCustomized: Boolean = false,
    val encounterPlaceId: String? = null,
    val encounterCustomPlaceName: String = "",
    val encounterNoteFlowB: String = "",
    // Single-surface Quick Capture Composer State (ADR-0040)
    val availableFlowPresets: List<FlowPreset> = emptyList(),
    val pinnedDetailKeys: List<QuickCaptureDetailKey> = QuickCaptureDetailKey.DEFAULT_ADD_CONTACT,
    val activeDetailTray: QuickCaptureDetailKey? = null,
    val isNoteExpanded: Boolean = false,
    val isPlusPopoverOpen: Boolean = false,
    val isCustomizeQuickAddOpen: Boolean = false,
    // Foreground Location Explainer & Captured GPS Fix
    val showLocationExplainer: Boolean = false,
    val isLocationGranted: Boolean = false,
    val isLocatingGps: Boolean = false,
    val capturedLatitude: Double? = null,
    val capturedLongitude: Double? = null,
    val capturedLocationLabel: String? = null,
    // Feedback message
    val userMessage: String? = null
) {
    val currentComponent: CaptureComponent?
        get() = activeFlowPreset?.activeComponents?.getOrNull(currentStepIndex)
}

sealed interface QuickActionUiEvent {
    data object OnOpenChoice : QuickActionUiEvent
    data object OnDismiss : QuickActionUiEvent
    data object OnSelectAddContact : QuickActionUiEvent
    data object OnSelectLogEncounter : QuickActionUiEvent
    data class OnOpenLogEncounterForContact(
        val contact: ContactSummary,
        val initialNote: String? = null
    ) : QuickActionUiEvent
    data object OnBackToChoice : QuickActionUiEvent

    // Flow A Events
    data class OnNameChange(val name: String) : QuickActionUiEvent
    data class OnRoleChange(val role: String) : QuickActionUiEvent
    data class OnCompanyChange(val company: String) : QuickActionUiEvent
    data class OnBirthdayChange(val birthday: LocalDate?) : QuickActionUiEvent
    data class OnPhoneChange(val phone: String) : QuickActionUiEvent
    data class OnEmailChange(val email: String) : QuickActionUiEvent
    data class OnSelectPlaceChip(val placeId: String) : QuickActionUiEvent
    data class OnCustomPlaceNameChange(val customName: String) : QuickActionUiEvent
    data class OnToggleTag(val tagId: String) : QuickActionUiEvent
    data object OnToggleCustomTagInput : QuickActionUiEvent
    data class OnCustomTagInputChange(val input: String) : QuickActionUiEvent
    data object OnAddCustomTag : QuickActionUiEvent
    data class OnNoteChange(val note: String) : QuickActionUiEvent
    data class OnSelectFollowUp(val days: Int?) : QuickActionUiEvent
    data class OnCustomPropertyKeyChange(val key: String) : QuickActionUiEvent
    data class OnCustomPropertyValueChange(val value: String) : QuickActionUiEvent
    data object OnAddCustomProperty : QuickActionUiEvent
    data class OnRemoveCustomProperty(val key: String) : QuickActionUiEvent
    data class OnStepIndexChange(val index: Int) : QuickActionUiEvent
    data object OnNextStep : QuickActionUiEvent
    data object OnPreviousStep : QuickActionUiEvent
    data class OnJumpToStep(val step: FlowAStep) : QuickActionUiEvent
    data object OnQuickSaveFlowA : QuickActionUiEvent

    // Flow B Events
    data class OnSearchQueryChange(val query: String) : QuickActionUiEvent
    data class OnSelectExistingContact(val contact: ContactSummary) : QuickActionUiEvent
    data class OnLongPressContactFlowB(val contact: ContactSummary) : QuickActionUiEvent
    data class OnToggleContactFlowB(val contact: ContactSummary) : QuickActionUiEvent
    data object OnConfirmMultiSelectContactsFlowB : QuickActionUiEvent
    data class OnRemoveSelectedContactFlowB(val contactId: String) : QuickActionUiEvent
    data class OnCommitFlowBParticipant(val contact: ContactSummary, val expandNoteAfter: Boolean = false) : QuickActionUiEvent
    data object OnFlowBEnterPressed : QuickActionUiEvent
    data class OnConvertQueryToAddContact(val name: String) : QuickActionUiEvent
    data class OnSetFlowBParticipants(val contacts: List<ContactSummary>) : QuickActionUiEvent
    data class OnToggleFlowBParticipantPicker(val open: Boolean) : QuickActionUiEvent
    data object OnClearSelectedContact : QuickActionUiEvent
    data class OnEncounterDateChange(val date: LocalDate) : QuickActionUiEvent
    data class OnEncounterTimeChange(val time: LocalTime) : QuickActionUiEvent
    data class OnEncounterPlaceChange(val placeId: String?, val customName: String = "") : QuickActionUiEvent
    data class OnEncounterNoteChange(val note: String) : QuickActionUiEvent
    data object OnSaveFlowBEncounter : QuickActionUiEvent

    // Single-surface Quick Capture Composer Events (ADR-0040)
    data class OnToggleDetailTray(val key: QuickCaptureDetailKey?) : QuickActionUiEvent
    data class OnSetNoteExpanded(val expanded: Boolean) : QuickActionUiEvent
    data class OnTogglePlusPopover(val open: Boolean) : QuickActionUiEvent
    data class OnToggleCustomizeQuickAdd(val open: Boolean) : QuickActionUiEvent
    data class OnTogglePinDetailKey(val key: QuickCaptureDetailKey) : QuickActionUiEvent
    data class OnSelectFlowPreset(val presetId: String) : QuickActionUiEvent
    data class OnClearDetailKey(val key: QuickCaptureDetailKey) : QuickActionUiEvent
    data object OnSwitchComposerMode : QuickActionUiEvent

    // Foreground Location Explainer Events
    data object OnRequestLocation : QuickActionUiEvent
    data object OnDismissLocationExplainer : QuickActionUiEvent
    data class OnLocationPermissionResult(val granted: Boolean) : QuickActionUiEvent

    // User Message
    data object OnDismissUserMessage : QuickActionUiEvent
}
