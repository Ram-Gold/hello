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
import com.example.hello.domain.model.formatParticipantNames
import com.example.hello.platform.location.LocationService
import com.example.hello.platform.location.LocationState
import com.example.hello.ui.components.PhoneInputUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class QuickActionViewModel(
    val repository: ContactRepository = ContactRepository(),
    val locationService: LocationService? = null,
    captureFlowRepository: CaptureFlowRepository? = null,
    val preferencesRepository: UserPreferencesRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val externalScope: CoroutineScope? = null,
    private val nowTimeProvider: () -> LocalTime = { LocalTime.now().truncatedTo(ChronoUnit.MINUTES) }
) : ViewModel() {

    val captureFlowRepository: CaptureFlowRepository =
        captureFlowRepository ?: CaptureFlowRepository(repository.database)

    private val ownedScope: CoroutineScope? = if (externalScope == null) {
        CoroutineScope(ioDispatcher + SupervisorJob())
    } else {
        null
    }
    private val coroutineScope: CoroutineScope = externalScope ?: ownedScope!!

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
            this@QuickActionViewModel.captureFlowRepository.presets.collectLatest {
                updateActivePreset()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ownedScope?.cancel()
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
                availableFlowPresets = presets,
                pinnedDetailKeys = resolvePinnedDetailKeys(active, state.currentMode),
                currentStepIndex = safeIndex,
                flowAStep = mapComponentToFlowAStep(currentComp)
            )
        }
    }

    private fun resolvePinnedDetailKeys(
        preset: FlowPreset?,
        mode: QuickActionMode
    ): List<QuickCaptureDetailKey> {
        if (mode == QuickActionMode.FLOW_B_LOG_ENCOUNTER) {
            return QuickCaptureDetailKey.DEFAULT_MET_AGAIN
        }
        val active = preset ?: return QuickCaptureDetailKey.DEFAULT_ADD_CONTACT
        val seededDefault = FlowPreset.createDefaultPresets().first()
        if (active.id == FlowPreset.PRESET_DEFAULT && active.components == seededDefault.components) {
            return QuickCaptureDetailKey.DEFAULT_ADD_CONTACT
        }
        val mapped = active.activeComponents.mapNotNull { comp ->
            when (comp.type) {
                CaptureComponentType.PLACE -> QuickCaptureDetailKey.LOCATION
                CaptureComponentType.TAGS -> QuickCaptureDetailKey.TAGS
                CaptureComponentType.CADENCE -> QuickCaptureDetailKey.REMINDER
                CaptureComponentType.ROLE, CaptureComponentType.COMPANY -> QuickCaptureDetailKey.OCCUPATION
                CaptureComponentType.PHONE, CaptureComponentType.EMAIL -> QuickCaptureDetailKey.CONTACT_INFO
                CaptureComponentType.BIRTHDAY -> QuickCaptureDetailKey.BIRTHDAY
                CaptureComponentType.CUSTOM -> QuickCaptureDetailKey.CUSTOM
                CaptureComponentType.NAME, CaptureComponentType.NOTE -> null
            }
        }.distinct()
        return mapped.ifEmpty { QuickCaptureDetailKey.DEFAULT_ADD_CONTACT }
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
                        pinnedDetailKeys = resolvePinnedDetailKeys(state.activeFlowPreset, QuickActionMode.FLOW_A_ADD_CONTACT),
                        isNoteExpanded = false,
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
                        pinnedDetailKeys = QuickCaptureDetailKey.DEFAULT_MET_AGAIN,
                        isNoteExpanded = false,
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
                        pinnedDetailKeys = QuickCaptureDetailKey.DEFAULT_MET_AGAIN,
                        isNoteExpanded = true,
                        selectedContact = event.contact,
                        selectedContacts = listOf(event.contact),
                        encounterNoteFlowB = event.initialNote.orEmpty(),
                        searchQuery = ""
                    )
                }
            }

            is QuickActionUiEvent.OnBackToChoice -> {
                _uiState.update { state ->
                    state.copy(
                        isOpen = false,
                        showLocationExplainer = false,
                        selectedContact = null,
                        selectedContacts = emptyList(),
                        isFlowBMultiSelectMode = false,
                        isFlowBParticipantPickerOpen = false
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
                _uiState.update { it.copy(phone = PhoneInputUtils.sanitizeInput(event.phone), phoneError = null) }
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
                        capturedLatitude = null,
                        capturedLongitude = null,
                        capturedLocationLabel = null,
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
                if (current.currentComponent?.type == CaptureComponentType.PHONE) {
                    val phoneValidationError = PhoneInputUtils.validateInput(current.phone)
                    if (phoneValidationError != null) {
                        _uiState.update { it.copy(phoneError = phoneValidationError) }
                        return
                    }
                }
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

                val phoneValidationError = PhoneInputUtils.validateInput(current.phone)
                if (phoneValidationError != null) {
                    val activeComponents = current.activeFlowPreset?.activeComponents.orEmpty()
                    val phoneIndex = activeComponents.indexOfFirst { it.type == CaptureComponentType.PHONE }
                    val targetIndex = if (phoneIndex >= 0) phoneIndex else current.currentStepIndex
                    val comp = activeComponents.getOrNull(targetIndex)
                    _uiState.update {
                        it.copy(
                            currentStepIndex = targetIndex,
                            flowAStep = if (phoneIndex >= 0) mapComponentToFlowAStep(comp) else it.flowAStep,
                            phoneError = phoneValidationError
                        )
                    }
                    return
                }

                val resolvedFlowACustomPlace = current.customPlaceName.takeIf { it.isNotBlank() }
                    ?: current.capturedLocationLabel.takeIf { current.selectedPlaceId == null }
                val added = repository.addContactWithInitialEncounter(
                    name = current.newContactName,
                    tagIds = current.selectedTagIds,
                    placeId = current.selectedPlaceId,
                    customPlaceName = resolvedFlowACustomPlace,
                    note = current.encounterNote,
                    followUpDays = current.selectedFollowUpDays,
                    role = current.role.takeIf { it.isNotBlank() },
                    company = current.company.takeIf { it.isNotBlank() },
                    birthday = current.birthday,
                    phone = PhoneInputUtils.normalizeForStorage(current.phone),
                    email = current.email.takeIf { it.isNotBlank() },
                    customProperties = current.customProperties,
                    latitude = current.capturedLatitude,
                    longitude = current.capturedLongitude
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
                _uiState.update { state ->
                    if (state.currentMode == QuickActionMode.FLOW_B_LOG_ENCOUNTER && event.query.contains(',')) {
                        val token = event.query.substringBefore(',').trim()
                        val remainder = event.query.substringAfter(',').trimStart()
                        val topMatch = findTopUnselectedMatch(
                            contacts = state.allContacts,
                            selectedIds = state.selectedContacts.map { it.id }.toSet(),
                            query = token
                        )
                        if (topMatch != null) {
                            val updated = (state.selectedContacts + topMatch).distinctBy { it.id }
                            state.copy(
                                selectedContacts = updated,
                                selectedContact = updated.firstOrNull(),
                                searchQuery = remainder,
                                filteredContacts = filterContacts(state.allContacts, remainder)
                            )
                        } else {
                            // Unmatched comma: keep the typed text without the comma so user can tap "Add as new contact"
                            state.copy(
                                searchQuery = token,
                                filteredContacts = filterContacts(state.allContacts, token)
                            )
                        }
                    } else {
                        state.copy(
                            searchQuery = event.query,
                            filteredContacts = filterContacts(state.allContacts, event.query)
                        )
                    }
                }
            }

            is QuickActionUiEvent.OnSelectExistingContact -> {
                _uiState.update {
                    it.copy(
                        selectedContact = event.contact,
                        selectedContacts = listOf(event.contact),
                        isFlowBMultiSelectMode = false
                    )
                }
            }

            is QuickActionUiEvent.OnLongPressContactFlowB -> {
                _uiState.update { state ->
                    val updated = (state.selectedContacts + event.contact).distinctBy { it.id }
                    state.copy(
                        isFlowBMultiSelectMode = true,
                        selectedContacts = updated
                    )
                }
            }

            is QuickActionUiEvent.OnToggleContactFlowB -> {
                _uiState.update { state ->
                    val exists = state.selectedContacts.any { it.id == event.contact.id }
                    val updated = if (exists) {
                        state.selectedContacts.filterNot { it.id == event.contact.id }
                    } else {
                        state.selectedContacts + event.contact
                    }
                    state.copy(
                        selectedContacts = updated,
                        isFlowBMultiSelectMode = updated.isNotEmpty()
                    )
                }
            }

            is QuickActionUiEvent.OnConfirmMultiSelectContactsFlowB -> {
                _uiState.update { state ->
                    val first = state.selectedContacts.firstOrNull() ?: return@update state
                    state.copy(
                        selectedContact = first,
                        isFlowBMultiSelectMode = false
                    )
                }
            }

            is QuickActionUiEvent.OnRemoveSelectedContactFlowB -> {
                _uiState.update { state ->
                    val updated = state.selectedContacts.filterNot { it.id == event.contactId }
                    state.copy(
                        selectedContacts = updated,
                        selectedContact = updated.firstOrNull()
                    )
                }
            }

            is QuickActionUiEvent.OnCommitFlowBParticipant -> {
                _uiState.update { state ->
                    val updated = (state.selectedContacts + event.contact).distinctBy { it.id }
                    state.copy(
                        selectedContacts = updated,
                        selectedContact = updated.firstOrNull(),
                        searchQuery = "",
                        filteredContacts = state.allContacts,
                        isNoteExpanded = if (event.expandNoteAfter) true else state.isNoteExpanded
                    )
                }
            }

            is QuickActionUiEvent.OnFlowBEnterPressed -> {
                _uiState.update { state ->
                    val trimmed = state.searchQuery.trim()
                    if (trimmed.isNotEmpty()) {
                        val topMatch = findTopUnselectedMatch(
                            contacts = state.allContacts,
                            selectedIds = state.selectedContacts.map { it.id }.toSet(),
                            query = trimmed
                        )
                        if (topMatch != null) {
                            val updated = (state.selectedContacts + topMatch).distinctBy { it.id }
                            state.copy(
                                selectedContacts = updated,
                                selectedContact = updated.firstOrNull(),
                                searchQuery = "",
                                filteredContacts = state.allContacts,
                                isNoteExpanded = true
                            )
                        } else if (state.selectedContacts.isNotEmpty()) {
                            state.copy(isNoteExpanded = true)
                        } else {
                            state
                        }
                    } else if (state.selectedContacts.isNotEmpty()) {
                        state.copy(isNoteExpanded = true)
                    } else {
                        state
                    }
                }
            }

            is QuickActionUiEvent.OnConvertQueryToAddContact -> {
                _uiState.update { state ->
                    state.copy(
                        currentMode = QuickActionMode.FLOW_A_ADD_CONTACT,
                        newContactName = event.name.trim(),
                        nameError = false,
                        searchQuery = "",
                        selectedContact = null,
                        selectedContacts = emptyList(),
                        pinnedDetailKeys = resolvePinnedDetailKeys(state.activeFlowPreset, QuickActionMode.FLOW_A_ADD_CONTACT),
                        isNoteExpanded = false,
                        activeDetailTray = null,
                        isPlusPopoverOpen = false,
                        isCustomizeQuickAddOpen = false
                    )
                }
            }

            is QuickActionUiEvent.OnSetFlowBParticipants -> {
                if (event.contacts.isEmpty()) return
                val distinct = event.contacts.distinctBy { it.id }
                _uiState.update {
                    it.copy(
                        selectedContacts = distinct,
                        selectedContact = distinct.firstOrNull(),
                        isFlowBParticipantPickerOpen = false
                    )
                }
            }

            is QuickActionUiEvent.OnToggleFlowBParticipantPicker -> {
                _uiState.update { it.copy(isFlowBParticipantPickerOpen = event.open) }
            }

            is QuickActionUiEvent.OnClearSelectedContact -> {
                _uiState.update {
                    it.copy(
                        selectedContact = null,
                        selectedContacts = emptyList(),
                        isFlowBMultiSelectMode = false,
                        isFlowBParticipantPickerOpen = false
                    )
                }
            }

            is QuickActionUiEvent.OnEncounterDateChange -> {
                _uiState.update { it.copy(encounterDate = event.date) }
            }

            is QuickActionUiEvent.OnEncounterTimeChange -> {
                _uiState.update {
                    it.copy(
                        encounterTime = event.time.truncatedTo(ChronoUnit.MINUTES),
                        isEncounterTimeCustomized = true
                    )
                }
            }

            is QuickActionUiEvent.OnEncounterPlaceChange -> {
                _uiState.update {
                    it.copy(
                        encounterPlaceId = event.placeId,
                        encounterCustomPlaceName = event.customName,
                        capturedLatitude = null,
                        capturedLongitude = null,
                        capturedLocationLabel = null
                    )
                }
            }

            is QuickActionUiEvent.OnEncounterNoteChange -> {
                _uiState.update { it.copy(encounterNoteFlowB = event.note) }
            }

            is QuickActionUiEvent.OnSaveFlowBEncounter -> {
                val current = _uiState.value
                val participants = current.selectedContacts.ifEmpty {
                    listOfNotNull(current.selectedContact)
                }
                val primary = participants.firstOrNull() ?: return
                val resolvedFlowBCustomPlace = current.encounterCustomPlaceName.takeIf { it.isNotBlank() }
                    ?: current.capturedLocationLabel.takeIf { current.encounterPlaceId == null }
                repository.logEncounterForContact(
                    contactId = primary.id,
                    date = current.encounterDate,
                    placeId = current.encounterPlaceId,
                    customPlaceName = resolvedFlowBCustomPlace,
                    note = current.encounterNoteFlowB,
                    additionalContactIds = participants.drop(1).map { it.id },
                    latitude = current.capturedLatitude,
                    longitude = current.capturedLongitude,
                    time = current.encounterTime
                )
                current.selectedFollowUpDays?.let { days ->
                    repository.addFollowUp(
                        contactId = primary.id,
                        dueDate = LocalDate.now().plusDays(days.toLong()),
                        note = current.encounterNoteFlowB.ifBlank {
                            "Follow up with ${formatParticipantNames(participants.map { it.name })}"
                        },
                        participantIds = participants.map { it.id }
                    )
                }
                refreshSuggestionsFromRepository()
                _uiState.update { state ->
                    state.copy(
                        isOpen = false,
                        userMessage = "Logged encounter with ${formatParticipantNames(participants.map { it.name })}!"
                    )
                }
            }

            // Single-surface Quick Capture Composer Events (ADR-0040)
            is QuickActionUiEvent.OnToggleDetailTray -> {
                _uiState.update { state ->
                    val next = if (state.activeDetailTray == event.key) null else event.key
                    state.copy(
                        activeDetailTray = next,
                        isPlusPopoverOpen = false,
                        isCustomizeQuickAddOpen = false
                    )
                }
            }

            is QuickActionUiEvent.OnSetNoteExpanded -> {
                _uiState.update {
                    it.copy(
                        isNoteExpanded = event.expanded,
                        isPlusPopoverOpen = false
                    )
                }
            }

            is QuickActionUiEvent.OnTogglePlusPopover -> {
                _uiState.update {
                    it.copy(
                        isPlusPopoverOpen = event.open,
                        isCustomizeQuickAddOpen = false
                    )
                }
            }

            is QuickActionUiEvent.OnToggleCustomizeQuickAdd -> {
                _uiState.update {
                    it.copy(
                        isCustomizeQuickAddOpen = event.open,
                        isPlusPopoverOpen = false,
                        activeDetailTray = if (event.open) null else it.activeDetailTray
                    )
                }
            }

            is QuickActionUiEvent.OnTogglePinDetailKey -> {
                val current = _uiState.value
                val nextPinned = if (event.key in current.pinnedDetailKeys) {
                    current.pinnedDetailKeys - event.key
                } else {
                    current.pinnedDetailKeys + event.key
                }
                _uiState.update { it.copy(pinnedDetailKeys = nextPinned) }

                val activePreset = current.activeFlowPreset
                if (activePreset != null && current.currentMode == QuickActionMode.FLOW_A_ADD_CONTACT) {
                    val updatedComponents = buildPresetComponentsForPinnedKeys(activePreset, nextPinned)
                    coroutineScope.launch {
                        captureFlowRepository.updatePresetComponents(activePreset.id, updatedComponents)
                    }
                }
            }

            is QuickActionUiEvent.OnSelectFlowPreset -> {
                currentActivePresetId = event.presetId
                updateActivePreset()
                if (preferencesRepository != null) {
                    coroutineScope.launch {
                        preferencesRepository.setActiveFlowPresetId(event.presetId)
                    }
                }
            }

            is QuickActionUiEvent.OnClearDetailKey -> {
                _uiState.update { state ->
                    when (event.key) {
                        QuickCaptureDetailKey.LOCATION -> state.copy(
                            selectedPlaceId = null,
                            customPlaceName = "",
                            encounterPlaceId = null,
                            encounterCustomPlaceName = "",
                            capturedLatitude = null,
                            capturedLongitude = null,
                            capturedLocationLabel = null
                        )
                        QuickCaptureDetailKey.TIME -> state.copy(
                            encounterDate = LocalDate.now(),
                            encounterTime = nowTimeProvider(),
                            isEncounterTimeCustomized = false
                        )
                        QuickCaptureDetailKey.TAGS -> state.copy(selectedTagIds = emptySet())
                        QuickCaptureDetailKey.REMINDER -> state.copy(selectedFollowUpDays = null)
                        QuickCaptureDetailKey.OCCUPATION -> state.copy(role = "", company = "")
                        QuickCaptureDetailKey.CONTACT_INFO -> state.copy(phone = "", email = "", phoneError = null)
                        QuickCaptureDetailKey.BIRTHDAY -> state.copy(birthday = null)
                        QuickCaptureDetailKey.CUSTOM -> state.copy(
                            customProperties = emptyMap(),
                            customPropertyKey = "",
                            customPropertyValue = ""
                        )
                    }
                }
            }

            is QuickActionUiEvent.OnSwitchComposerMode -> {
                _uiState.update { state ->
                    val nextMode = if (state.currentMode == QuickActionMode.FLOW_A_ADD_CONTACT) {
                        QuickActionMode.FLOW_B_LOG_ENCOUNTER
                    } else {
                        QuickActionMode.FLOW_A_ADD_CONTACT
                    }
                    state.copy(
                        currentMode = nextMode,
                        pinnedDetailKeys = resolvePinnedDetailKeys(state.activeFlowPreset, nextMode),
                        isNoteExpanded = false,
                        activeDetailTray = null,
                        isPlusPopoverOpen = false,
                        isCustomizeQuickAddOpen = false
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
                if (!event.granted) {
                    _uiState.update { state ->
                        state.copy(
                            showLocationExplainer = false,
                            isLocationGranted = false,
                            isLocatingGps = false,
                            userMessage = "Location permission denied — pick or type a place"
                        )
                    }
                    return
                }

                val service = locationService
                if (service == null) {
                    _uiState.update { state ->
                        state.copy(
                            showLocationExplainer = false,
                            isLocationGranted = true,
                            isLocatingGps = false,
                            userMessage = "GPS unavailable — pick or type a place"
                        )
                    }
                    return
                }

                val currentPlaces = _uiState.value.suggestedPlaces
                _uiState.update { state ->
                    state.copy(
                        showLocationExplainer = false,
                        isLocationGranted = true,
                        isLocatingGps = true
                    )
                }

                coroutineScope.launch {
                    service.getLocationFlow(currentPlaces).collect { locState ->
                        when (locState) {
                            is LocationState.Searching -> {
                                _uiState.update { it.copy(isLocatingGps = true) }
                            }
                            is LocationState.Success -> {
                                val nearest = locState.nearbyPlaces.firstOrNull()?.place
                                val resolvedLabel = nearest?.label
                                    ?: locState.resolvedLabel
                                    ?: LocationService.formatCoordinates(locState.latitude, locState.longitude)
                                _uiState.update { st ->
                                    if (st.currentMode == QuickActionMode.FLOW_A_ADD_CONTACT) {
                                        st.copy(
                                            isLocatingGps = false,
                                            capturedLatitude = locState.latitude,
                                            capturedLongitude = locState.longitude,
                                            capturedLocationLabel = resolvedLabel,
                                            selectedPlaceId = nearest?.id,
                                            customPlaceName = if (nearest != null) {
                                                ""
                                            } else {
                                                st.customPlaceName.ifBlank { resolvedLabel }
                                            }
                                        )
                                    } else {
                                        st.copy(
                                            isLocatingGps = false,
                                            capturedLatitude = locState.latitude,
                                            capturedLongitude = locState.longitude,
                                            capturedLocationLabel = resolvedLabel,
                                            encounterPlaceId = nearest?.id,
                                            encounterCustomPlaceName = if (nearest != null) {
                                                ""
                                            } else {
                                                st.encounterCustomPlaceName.ifBlank { resolvedLabel }
                                            }
                                        )
                                    }
                                }
                            }
                            is LocationState.TimedOut,
                            is LocationState.ProviderDisabled,
                            is LocationState.PermissionDenied -> {
                                _uiState.update { st ->
                                    st.copy(
                                        isLocatingGps = false,
                                        userMessage = "GPS unavailable — pick or type a place"
                                    )
                                }
                            }
                            is LocationState.Idle -> Unit
                        }
                    }
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
            phoneError = null,
            searchQuery = "",
            selectedContact = null,
            selectedContacts = emptyList(),
            isFlowBMultiSelectMode = false,
            isFlowBParticipantPickerOpen = false,
            filteredContacts = allContacts,
            encounterDate = LocalDate.now(),
            encounterTime = nowTimeProvider(),
            isEncounterTimeCustomized = false,
            encounterPlaceId = null,
            encounterCustomPlaceName = "",
            encounterNoteFlowB = "",
            showLocationExplainer = false,
            isLocatingGps = false,
            capturedLatitude = null,
            capturedLongitude = null,
            capturedLocationLabel = null,
            activeDetailTray = null,
            isNoteExpanded = false,
            isPlusPopoverOpen = false,
            isCustomizeQuickAddOpen = false
        )
    }

    private fun buildPresetComponentsForPinnedKeys(
        preset: FlowPreset,
        pinnedKeys: List<QuickCaptureDetailKey>
    ): List<CaptureComponent> {
        val result = mutableListOf<CaptureComponent>()
        val nameComp = preset.components.firstOrNull { it.type == CaptureComponentType.NAME }
            ?: CaptureComponent(type = CaptureComponentType.NAME, stepOrder = 0, isRequired = true)
        result += nameComp.copy(stepOrder = 0, isEnabled = true)

        var order = 1
        pinnedKeys.forEach { key ->
            val types = when (key) {
                QuickCaptureDetailKey.LOCATION -> listOf(CaptureComponentType.PLACE)
                QuickCaptureDetailKey.TAGS -> listOf(CaptureComponentType.TAGS)
                QuickCaptureDetailKey.REMINDER -> listOf(CaptureComponentType.CADENCE)
                QuickCaptureDetailKey.OCCUPATION -> listOf(CaptureComponentType.ROLE, CaptureComponentType.COMPANY)
                QuickCaptureDetailKey.CONTACT_INFO -> listOf(CaptureComponentType.PHONE, CaptureComponentType.EMAIL)
                QuickCaptureDetailKey.BIRTHDAY -> listOf(CaptureComponentType.BIRTHDAY)
                QuickCaptureDetailKey.CUSTOM -> listOf(CaptureComponentType.CUSTOM)
                QuickCaptureDetailKey.TIME -> emptyList()
            }
            types.forEach { type ->
                val existing = preset.components.firstOrNull { it.type == type }
                result += (existing ?: CaptureComponent(type = type, stepOrder = order))
                    .copy(stepOrder = order++, isEnabled = true)
            }
        }
        val noteComp = preset.components.firstOrNull { it.type == CaptureComponentType.NOTE }
            ?: CaptureComponent(type = CaptureComponentType.NOTE, stepOrder = order)
        result += noteComp.copy(stepOrder = order, isEnabled = true)
        return result
    }

    private fun findTopUnselectedMatch(
        contacts: List<ContactSummary>,
        selectedIds: Set<String>,
        query: String
    ): ContactSummary? {
        val clean = query.trim()
        if (clean.isEmpty()) return null
        val unselected = contacts.filterNot { it.id in selectedIds }
        return unselected.firstOrNull { it.name.startsWith(clean, ignoreCase = true) }
            ?: unselected.firstOrNull { c -> c.nicknames.any { it.startsWith(clean, ignoreCase = true) } }
            ?: unselected.firstOrNull { it.name.contains(clean, ignoreCase = true) }
            ?: unselected.firstOrNull { c -> c.nicknames.any { it.contains(clean, ignoreCase = true) } }
    }

    private fun filterContacts(
        contacts: List<ContactSummary>,
        query: String
    ): List<ContactSummary> {
        val clean = query.trim()
        if (clean.isBlank()) return contacts
        val matched = contacts.filter { c ->
            c.name.contains(clean, ignoreCase = true) ||
                c.nicknames.any { it.contains(clean, ignoreCase = true) } ||
                c.contextSubtitle.contains(clean, ignoreCase = true)
        }
        return matched.sortedBy { c ->
            when {
                c.name.startsWith(clean, ignoreCase = true) -> 0
                c.nicknames.any { it.startsWith(clean, ignoreCase = true) } -> 1
                c.name.contains(clean, ignoreCase = true) -> 2
                c.nicknames.any { it.contains(clean, ignoreCase = true) } -> 3
                else -> 4
            }
        }
    }

    private fun createDefaultState(): QuickActionUiState {
        val allContacts = repository.contacts.value
        val recentContacts = repository.recentContacts.value
        val places = repository.places.value.sortedByDescending { it.useCount }
        val tags = repository.tags.value.sortedByDescending { it.useCount }
        val defaultPresets = FlowPreset.createDefaultPresets()
        val defaultPreset = defaultPresets.first()
        return QuickActionUiState(
            suggestedPlaces = places,
            suggestedTags = tags,
            allContacts = allContacts,
            recentContacts = recentContacts,
            filteredContacts = allContacts,
            activeFlowPreset = defaultPreset,
            availableFlowPresets = defaultPresets,
            pinnedDetailKeys = QuickCaptureDetailKey.DEFAULT_ADD_CONTACT,
            currentStepIndex = 0,
            flowAStep = FlowAStep.NAME,
            encounterTime = nowTimeProvider()
        )
    }
}
