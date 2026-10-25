package com.example.hello.ui.quickaction

import androidx.lifecycle.ViewModel
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

class QuickActionViewModel(
    val repository: ContactRepository = ContactRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(createDefaultState())
    val uiState: StateFlow<QuickActionUiState> = _uiState.asStateFlow()

    fun onEvent(event: QuickActionUiEvent) {
        when (event) {
            is QuickActionUiEvent.OnOpenChoice -> {
                refreshSuggestionsFromRepository()
                _uiState.update { state ->
                    resetDraftState(state).copy(
                        isOpen = true,
                        currentMode = QuickActionMode.CHOICE
                    )
                }
            }

            is QuickActionUiEvent.OnDismiss -> {
                _uiState.update { state ->
                    resetDraftState(state).copy(
                        isOpen = false,
                        currentMode = QuickActionMode.CHOICE,
                        showLocationExplainer = false
                    )
                }
            }

            is QuickActionUiEvent.OnSelectAddContact -> {
                refreshSuggestionsFromRepository()
                _uiState.update { state ->
                    resetDraftState(state).copy(
                        isOpen = true,
                        currentMode = QuickActionMode.FLOW_A_ADD_CONTACT,
                        flowAStep = FlowAStep.NAME
                    )
                }
            }

            is QuickActionUiEvent.OnSelectLogEncounter -> {
                refreshSuggestionsFromRepository()
                _uiState.update { state ->
                    resetDraftState(state).copy(
                        isOpen = true,
                        currentMode = QuickActionMode.FLOW_B_LOG_ENCOUNTER,
                        selectedContact = null,
                        searchQuery = ""
                    )
                }
            }

            is QuickActionUiEvent.OnOpenLogEncounterForContact -> {
                refreshSuggestionsFromRepository()
                _uiState.update { state ->
                    resetDraftState(state).copy(
                        isOpen = true,
                        currentMode = QuickActionMode.FLOW_B_LOG_ENCOUNTER,
                        selectedContact = event.contact,
                        searchQuery = ""
                    )
                }
            }

            is QuickActionUiEvent.OnBackToChoice -> {
                _uiState.update { state ->
                    state.copy(
                        currentMode = QuickActionMode.CHOICE,
                        selectedContact = null
                    )
                }
            }

            // Flow A: Add Contact Events
            is QuickActionUiEvent.OnNameChange -> {
                _uiState.update { it.copy(newContactName = event.name) }
            }

            is QuickActionUiEvent.OnSelectPlaceChip -> {
                _uiState.update { state ->
                    val nextStep = if (state.flowAStep == FlowAStep.PLACE) {
                        FlowAStep.TAGS_AND_CIRCLES
                    } else {
                        state.flowAStep
                    }
                    state.copy(
                        selectedPlaceId = event.placeId,
                        customPlaceName = "",
                        flowAStep = nextStep
                    )
                }
            }

            is QuickActionUiEvent.OnCustomPlaceNameChange -> {
                _uiState.update {
                    it.copy(
                        customPlaceName = event.customName,
                        selectedPlaceId = if (event.customName.isNotBlank()) null else it.selectedPlaceId
                    )
                }
            }

            is QuickActionUiEvent.OnToggleCircle -> {
                _uiState.update { state ->
                    val updated = if (event.circle in state.selectedCircles) {
                        if (state.selectedCircles.size > 1) state.selectedCircles - event.circle else state.selectedCircles
                    } else {
                        state.selectedCircles + event.circle
                    }
                    state.copy(selectedCircles = updated)
                }
            }

            is QuickActionUiEvent.OnToggleTag -> {
                _uiState.update { state ->
                    val updated = if (event.tagId in state.selectedTagIds) {
                        state.selectedTagIds - event.tagId
                    } else {
                        state.selectedTagIds + event.tagId
                    }
                    state.copy(selectedTagIds = updated)
                }
            }

            is QuickActionUiEvent.OnNoteChange -> {
                _uiState.update { it.copy(encounterNote = event.note) }
            }

            is QuickActionUiEvent.OnSelectFollowUp -> {
                _uiState.update { state ->
                    val nextDays = if (state.selectedFollowUpDays == event.days) null else event.days
                    state.copy(selectedFollowUpDays = nextDays)
                }
            }

            is QuickActionUiEvent.OnNextStep -> {
                _uiState.update { state ->
                    val next = when (state.flowAStep) {
                        FlowAStep.NAME -> FlowAStep.PLACE
                        FlowAStep.PLACE -> FlowAStep.TAGS_AND_CIRCLES
                        FlowAStep.TAGS_AND_CIRCLES -> FlowAStep.NOTE_AND_FOLLOW_UP
                        FlowAStep.NOTE_AND_FOLLOW_UP -> FlowAStep.NOTE_AND_FOLLOW_UP
                    }
                    state.copy(flowAStep = next)
                }
            }

            is QuickActionUiEvent.OnPreviousStep -> {
                _uiState.update { state ->
                    val prev = when (state.flowAStep) {
                        FlowAStep.NAME -> FlowAStep.NAME
                        FlowAStep.PLACE -> FlowAStep.NAME
                        FlowAStep.TAGS_AND_CIRCLES -> FlowAStep.PLACE
                        FlowAStep.NOTE_AND_FOLLOW_UP -> FlowAStep.TAGS_AND_CIRCLES
                    }
                    state.copy(flowAStep = prev)
                }
            }

            is QuickActionUiEvent.OnJumpToStep -> {
                _uiState.update { it.copy(flowAStep = event.step) }
            }

            is QuickActionUiEvent.OnQuickSaveFlowA -> {
                val current = _uiState.value
                val added = repository.addContactWithInitialEncounter(
                    name = current.newContactName,
                    circles = current.selectedCircles,
                    tagIds = current.selectedTagIds,
                    placeId = current.selectedPlaceId,
                    customPlaceName = current.customPlaceName.takeIf { it.isNotBlank() },
                    note = current.encounterNote,
                    followUpDays = current.selectedFollowUpDays
                )
                refreshSuggestionsFromRepository()
                _uiState.update { state ->
                    resetDraftState(state).copy(
                        isOpen = false,
                        currentMode = QuickActionMode.CHOICE,
                        userMessage = "Saved ${added.name} & logged encounter! ✨"
                    )
                }
            }

            // Flow B: Log Encounter / Met Again Events
            is QuickActionUiEvent.OnSearchQueryChange -> {
                val query = event.query
                _uiState.update { state ->
                    state.copy(
                        searchQuery = query,
                        filteredContacts = filterContacts(state.allContacts, query)
                    )
                }
            }

            is QuickActionUiEvent.OnSelectExistingContact -> {
                _uiState.update { state ->
                    state.copy(
                        selectedContact = event.contact,
                        encounterDate = LocalDate.now(),
                        encounterPlaceId = null,
                        encounterCustomPlaceName = "",
                        encounterNoteFlowB = ""
                    )
                }
            }

            is QuickActionUiEvent.OnClearSelectedContact -> {
                _uiState.update { it.copy(selectedContact = null) }
            }

            is QuickActionUiEvent.OnEncounterDateChange -> {
                _uiState.update { it.copy(encounterDate = event.date) }
            }

            is QuickActionUiEvent.OnEncounterPlaceChange -> {
                _uiState.update {
                    it.copy(
                        encounterPlaceId = event.placeId,
                        encounterCustomPlaceName = event.customName
                    )
                }
            }

            is QuickActionUiEvent.OnEncounterNoteChange -> {
                _uiState.update { it.copy(encounterNoteFlowB = event.note) }
            }

            is QuickActionUiEvent.OnSaveFlowBEncounter -> {
                val current = _uiState.value
                val targetContact = current.selectedContact ?: return
                repository.logEncounterForContact(
                    contactId = targetContact.id,
                    date = current.encounterDate,
                    placeId = current.encounterPlaceId,
                    customPlaceName = current.encounterCustomPlaceName.takeIf { it.isNotBlank() },
                    note = current.encounterNoteFlowB
                )
                refreshSuggestionsFromRepository()
                _uiState.update { state ->
                    resetDraftState(state).copy(
                        isOpen = false,
                        currentMode = QuickActionMode.CHOICE,
                        userMessage = "Logged encounter with ${targetContact.name}! 🔄"
                    )
                }
            }

            // Location Explainer Events
            is QuickActionUiEvent.OnRequestLocation -> {
                _uiState.update { it.copy(showLocationExplainer = true) }
            }

            is QuickActionUiEvent.OnDismissLocationExplainer -> {
                _uiState.update { it.copy(showLocationExplainer = false) }
            }

            is QuickActionUiEvent.OnLocationPermissionResult -> {
                _uiState.update { state ->
                    val nearestPlaceId = if (event.granted) {
                        state.suggestedPlaces.firstOrNull()?.id
                    } else {
                        state.selectedPlaceId
                    }
                    val nextStep = if (event.granted && state.currentMode == QuickActionMode.FLOW_A_ADD_CONTACT && state.flowAStep == FlowAStep.PLACE) {
                        FlowAStep.TAGS_AND_CIRCLES
                    } else {
                        state.flowAStep
                    }
                    state.copy(
                        showLocationExplainer = false,
                        isLocationGranted = event.granted,
                        selectedPlaceId = if (state.currentMode == QuickActionMode.FLOW_A_ADD_CONTACT) nearestPlaceId else state.selectedPlaceId,
                        encounterPlaceId = if (state.currentMode == QuickActionMode.FLOW_B_LOG_ENCOUNTER && event.granted) nearestPlaceId else state.encounterPlaceId,
                        flowAStep = nextStep
                    )
                }
            }

            is QuickActionUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    private fun refreshSuggestionsFromRepository() {
        val allContacts = repository.contacts.value
        val recentContacts = repository.recentContacts.value
        val places = repository.places.value.sortedByDescending { it.useCount }
        val tags = repository.tags.value.sortedByDescending { it.useCount }
        _uiState.update { state ->
            state.copy(
                allContacts = allContacts,
                recentContacts = recentContacts,
                filteredContacts = filterContacts(allContacts, state.searchQuery),
                suggestedPlaces = places,
                suggestedTags = tags
            )
        }
    }

    private fun resetDraftState(state: QuickActionUiState): QuickActionUiState {
        val allContacts = repository.contacts.value
        return state.copy(
            flowAStep = FlowAStep.NAME,
            newContactName = "",
            selectedPlaceId = null,
            customPlaceName = "",
            selectedCircles = setOf(Circle.FRIENDS),
            selectedTagIds = emptySet(),
            customTagInput = "",
            encounterNote = "",
            selectedFollowUpDays = null,
            searchQuery = "",
            selectedContact = null,
            filteredContacts = allContacts,
            encounterDate = LocalDate.now(),
            encounterPlaceId = null,
            encounterCustomPlaceName = "",
            encounterNoteFlowB = "",
            showLocationExplainer = false
        )
    }

    private fun filterContacts(
        contacts: List<ContactSummary>,
        query: String
    ): List<ContactSummary> {
        if (query.isBlank()) return contacts
        return contacts.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.contextSubtitle.contains(query, ignoreCase = true)
        }
    }

    private fun createDefaultState(): QuickActionUiState {
        val allContacts = repository.contacts.value
        val recentContacts = repository.recentContacts.value
        val places = repository.places.value.sortedByDescending { it.useCount }
        val tags = repository.tags.value.sortedByDescending { it.useCount }
        return QuickActionUiState(
            suggestedPlaces = places,
            suggestedTags = tags,
            allContacts = allContacts,
            recentContacts = recentContacts,
            filteredContacts = allContacts
        )
    }
}
