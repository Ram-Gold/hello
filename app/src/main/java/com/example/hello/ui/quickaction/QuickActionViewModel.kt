package com.example.hello.ui.quickaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.preferences.UserPreferencesRepository
import com.example.hello.data.repository.CaptureFlowRepository
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.CaptureComponent
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.FlowPreset
import com.example.hello.platform.location.LocationService
import com.example.hello.platform.location.LocationState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class QuickActionViewModel(
    val repository: ContactRepository = ContactRepository(),
    val locationService: LocationService? = null,
    val captureFlowRepository: CaptureFlowRepository = CaptureFlowRepository(repository.database),
    val preferencesRepository: UserPreferencesRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val coroutineScope: CoroutineScope = externalScope ?: CoroutineScope(ioDispatcher + SupervisorJob())

    private val _uiState = MutableStateFlow(createDefaultState())
    val uiState: StateFlow<QuickActionUiState> = _uiState.asStateFlow()

    private var currentActivePresetId: String = FlowPreset.PRESET_DEFAULT

    init {
        if (preferencesRepository != null) {
            coroutineScope.launch {
                preferencesRepository.userPreferencesFlow.collectLatest { prefs ->
                    currentActivePresetId = prefs.activeFlowPresetId
                    updateActivePreset()
                }
            }
        }

        coroutineScope.launch {
            captureFlowRepository.presets.collectLatest {
                updateActivePreset()
            }
        }
    }

    private fun updateActivePreset() {
        val presets = captureFlowRepository.presets.value
        val active = presets.firstOrNull { it.id == currentActivePresetId }
            ?: presets.firstOrNull()
            ?: FlowPreset.createDefaultPresets().first()

        _uiState.update { state ->
            val safeIndex = state.currentStepIndex.coerceIn(0, (active.activeComponents.size - 1).coerceAtLeast(0))
            val currentComp = active.activeComponents.getOrNull(safeIndex)
            state.copy(
                activeFlowPreset = active,
                currentStepIndex = safeIndex,
                flowAStep = mapComponentToFlowAStep(currentComp)
            )
        }
    }

    private fun mapComponentToFlowAStep(component: CaptureComponent?): FlowAStep {
        return when (component?.type) {
            CaptureComponentType.NAME -> FlowAStep.NAME
            CaptureComponentType.ROLE -> FlowAStep.ROLE
            CaptureComponentType.COMPANY -> FlowAStep.COMPANY
            CaptureComponentType.PHONE -> FlowAStep.PHONE
            CaptureComponentType.EMAIL -> FlowAStep.EMAIL
            CaptureComponentType.BIRTHDAY -> FlowAStep.BIRTHDAY
            CaptureComponentType.CUSTOM -> FlowAStep.CUSTOM
            CaptureComponentType.PLACE -> FlowAStep.PLACE
            CaptureComponentType.TAGS -> FlowAStep.TAGS
            CaptureComponentType.NOTE, CaptureComponentType.CADENCE -> FlowAStep.NOTE_AND_FOLLOW_UP
            null -> FlowAStep.NAME
        }
    }

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
                    state.copy(
                        isOpen = false,
                        showLocationExplainer = false
                    )
                }
            }

            is QuickActionUiEvent.OnSelectAddContact -> {
                refreshSuggestionsFromRepository()
                updateActivePreset()
                _uiState.update { state ->
                    resetDraftState(state).copy(
                        isOpen = true,
                        currentMode = QuickActionMode.FLOW_A_ADD_CONTACT,
                        currentStepIndex = 0,
                        flowAStep = mapComponentToFlowAStep(state.activeFlowPreset?.activeComponents?.firstOrNull())
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
                        encounterNote = event.initialNote.orEmpty(),
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

            is QuickActionUiEvent.OnNameChange -> {
                _uiState.update { it.copy(newContactName = event.name, nameError = false) }
            }

            is QuickActionUiEvent.OnRoleChange -> {
                _uiState.update { it.copy(role = event.role) }
            }

            is QuickActionUiEvent.OnCompanyChange -> {
                _uiState.update { it.copy(company = event.company) }
            }

            is QuickActionUiEvent.OnBirthdayChange -> {
                _uiState.update { it.copy(birthday = event.birthday) }
            }

            is QuickActionUiEvent.OnPhoneChange -> {
                _uiState.update { it.copy(phone = event.phone) }
            }

            is QuickActionUiEvent.OnEmailChange -> {
                _uiState.update { it.copy(email = event.email) }
            }

            is QuickActionUiEvent.OnSelectPlaceChip -> {
                val current = _uiState.value
                val activeComponents = current.activeFlowPreset?.activeComponents.orEmpty()
                val nextIndex = (current.currentStepIndex + 1).coerceAtMost((activeComponents.size - 1).coerceAtLeast(0))
                val nextComp = activeComponents.getOrNull(nextIndex)

                _uiState.update { state ->
                    state.copy(
                        selectedPlaceId = event.placeId,
                        customPlaceName = "",
                        currentStepIndex = nextIndex,
                        flowAStep = mapComponentToFlowAStep(nextComp)
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

            is QuickActionUiEvent.OnToggleCustomTagInput -> {
                _uiState.update { state ->
                    state.copy(
                        isCustomTagInputExpanded = !state.isCustomTagInputExpanded,
                        customTagInput = if (state.isCustomTagInputExpanded) "" else state.customTagInput
                    )
                }
            }

            is QuickActionUiEvent.OnCustomTagInputChange -> {
                _uiState.update { it.copy(customTagInput = event.input) }
            }

            is QuickActionUiEvent.OnAddCustomTag -> {
                val tagText = _uiState.value.customTagInput.trim().removePrefix("#")
                if (tagText.isNotBlank()) {
                    val addedTag = repository.addTag(name = tagText)
                    _uiState.update { state ->
                        state.copy(
                            selectedTagIds = state.selectedTagIds + addedTag.id,
                            customTagInput = "",
                            isCustomTagInputExpanded = false
                        )
                    }
                    refreshSuggestionsFromRepository()
                } else {
                    _uiState.update { state ->
                        state.copy(
                            customTagInput = "",
                            isCustomTagInputExpanded = false
                        )
                    }
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

            is QuickActionUiEvent.OnCustomPropertyKeyChange -> {
                _uiState.update { it.copy(customPropertyKey = event.key) }
            }

            is QuickActionUiEvent.OnCustomPropertyValueChange -> {
                _uiState.update { it.copy(customPropertyValue = event.value) }
            }

            is QuickActionUiEvent.OnAddCustomProperty -> {
                val current = _uiState.value
                if (current.customPropertyKey.isNotBlank() && current.customPropertyValue.isNotBlank()) {
                    _uiState.update { state ->
                        state.copy(
                            customProperties = state.customProperties + (state.customPropertyKey.trim() to state.customPropertyValue.trim()),
                            customPropertyKey = "",
                            customPropertyValue = ""
                        )
                    }
                }
            }

            is QuickActionUiEvent.OnRemoveCustomProperty -> {
                _uiState.update { state ->
                    state.copy(customProperties = state.customProperties - event.key)
                }
            }

            is QuickActionUiEvent.OnStepIndexChange -> {
                val activeComponents = _uiState.value.activeFlowPreset?.activeComponents.orEmpty()
                val safeIndex = event.index.coerceIn(0, (activeComponents.size - 1).coerceAtLeast(0))
                val comp = activeComponents.getOrNull(safeIndex)
                _uiState.update { it.copy(currentStepIndex = safeIndex, flowAStep = mapComponentToFlowAStep(comp)) }
            }

            is QuickActionUiEvent.OnNextStep -> {
                val current = _uiState.value
                val activeComponents = current.activeFlowPreset?.activeComponents.orEmpty()
                if (current.currentStepIndex >= activeComponents.size - 1) {
                    onEvent(QuickActionUiEvent.OnQuickSaveFlowA)
                } else {
                    val nextIndex = current.currentStepIndex + 1
                    val nextComp = activeComponents.getOrNull(nextIndex)
                    _uiState.update {
                        it.copy(
                            currentStepIndex = nextIndex,
                            flowAStep = mapComponentToFlowAStep(nextComp)
                        )
                    }
                }
            }

            is QuickActionUiEvent.OnPreviousStep -> {
                val current = _uiState.value
                val activeComponents = current.activeFlowPreset?.activeComponents.orEmpty()
                if (current.currentStepIndex > 0) {
                    val prevIndex = current.currentStepIndex - 1
                    val prevComp = activeComponents.getOrNull(prevIndex)
                    _uiState.update {
                        it.copy(
                            currentStepIndex = prevIndex,
                            flowAStep = mapComponentToFlowAStep(prevComp)
                        )
                    }
                }
            }

            is QuickActionUiEvent.OnJumpToStep -> {
                val activeComponents = _uiState.value.activeFlowPreset?.activeComponents.orEmpty()
                val targetIndex = activeComponents.indexOfFirst { mapComponentToFlowAStep(it) == event.step }
                if (targetIndex >= 0) {
                    _uiState.update {
                        it.copy(
                            currentStepIndex = targetIndex,
                            flowAStep = event.step
                        )
                    }
                } else {
                    _uiState.update { it.copy(flowAStep = event.step) }
                }
            }

            is QuickActionUiEvent.OnQuickSaveFlowA -> {
                val current = _uiState.value
                if (current.newContactName.isBlank()) {
                    val activeComponents = current.activeFlowPreset?.activeComponents.orEmpty()
                    val nameIndex = activeComponents.indexOfFirst { it.type == CaptureComponentType.NAME }
                    val targetIndex = if (nameIndex >= 0) nameIndex else 0
                    val comp = activeComponents.getOrNull(targetIndex)
                    _uiState.update {
                        it.copy(
                            currentStepIndex = targetIndex,
                            flowAStep = mapComponentToFlowAStep(comp),
                            nameError = true,
                            userMessage = "Please enter a name first"
                        )
                    }
                    return
                }

                val added = repository.addContactWithInitialEncounter(
                    name = current.newContactName,
                    tagIds = current.selectedTagIds,
                    placeId = current.selectedPlaceId,
                    customPlaceName = current.customPlaceName.takeIf { it.isNotBlank() },
                    note = current.encounterNote,
                    followUpDays = current.selectedFollowUpDays,
                    role = current.role.takeIf { it.isNotBlank() },
                    company = current.company.takeIf { it.isNotBlank() },
                    birthday = current.birthday,
                    phone = current.phone.takeIf { it.isNotBlank() },
                    email = current.email.takeIf { it.isNotBlank() },
                    customProperties = current.customProperties
                )
                refreshSuggestionsFromRepository()
                _uiState.update { state ->
                    state.copy(
                        isOpen = false,
                        userMessage = "Saved ${added.name} & logged encounter!"
                    )
                }
            }

            // Flow B Events
            is QuickActionUiEvent.OnSearchQueryChange -> {
                _uiState.update {
                    it.copy(
                        searchQuery = event.query,
                        filteredContacts = filterContacts(it.allContacts, event.query)
                    )
                }
            }

            is QuickActionUiEvent.OnSelectExistingContact -> {
                _uiState.update { it.copy(selectedContact = event.contact) }
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
                    state.copy(
                        isOpen = false,
                        userMessage = "Logged encounter with ${targetContact.name}!"
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
                val currentPlaces = _uiState.value.suggestedPlaces
                if (event.granted && locationService != null) {
                    coroutineScope.launch {
                        locationService.getLocationFlow(currentPlaces).collect { locState ->
                            if (locState is LocationState.Success && locState.nearbyPlaces.isNotEmpty()) {
                                val nearest = locState.nearbyPlaces.first().place
                                _uiState.update { st ->
                                    st.copy(
                                        selectedPlaceId = if (st.currentMode == QuickActionMode.FLOW_A_ADD_CONTACT) nearest.id else st.selectedPlaceId,
                                        encounterPlaceId = if (st.currentMode == QuickActionMode.FLOW_B_LOG_ENCOUNTER) nearest.id else st.encounterPlaceId
                                    )
                                }
                            }
                        }
                    }
                }
                _uiState.update { state ->
                    val nearestPlaceId = if (event.granted) {
                        state.suggestedPlaces.firstOrNull()?.id
                    } else {
                        state.selectedPlaceId
                    }
                    val nextStep = if (event.granted && state.currentMode == QuickActionMode.FLOW_A_ADD_CONTACT && state.flowAStep == FlowAStep.PLACE) {
                        FlowAStep.TAGS
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
        val active = state.activeFlowPreset ?: FlowPreset.createDefaultPresets().first()
        val firstComp = active.activeComponents.firstOrNull()
        return state.copy(
            activeFlowPreset = active,
            currentStepIndex = 0,
            flowAStep = mapComponentToFlowAStep(firstComp),
            newContactName = "",
            role = "",
            company = "",
            birthday = null,
            phone = "",
            email = "",
            selectedPlaceId = null,
            customPlaceName = "",
            selectedTagIds = emptySet(),
            customTagInput = "",
            isCustomTagInputExpanded = false,
            encounterNote = "",
            selectedFollowUpDays = null,
            customProperties = emptyMap(),
            customPropertyKey = "",
            customPropertyValue = "",
            nameError = false,
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
        val defaultPreset = FlowPreset.createDefaultPresets().first()
        return QuickActionUiState(
            suggestedPlaces = places,
            suggestedTags = tags,
            allContacts = allContacts,
            recentContacts = recentContacts,
            filteredContacts = allContacts,
            activeFlowPreset = defaultPreset,
            currentStepIndex = 0,
            flowAStep = FlowAStep.NAME
        )
    }
}
