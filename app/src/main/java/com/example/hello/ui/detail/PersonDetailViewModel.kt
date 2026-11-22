package com.example.hello.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.preferences.UserPreferencesRepository
import com.example.hello.data.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonDetailViewModel(
    val contactId: String,
    val repository: ContactRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState: MutableStateFlow<PersonDetailUiState>
    val uiState: StateFlow<PersonDetailUiState>

    init {
        val initialContact = repository.getContact(contactId)
        val initialSummary = repository.contacts.value.firstOrNull { it.id == contactId }
        val initialPlaces = repository.places.value.associateBy { it.id }
        val initialTags = repository.tags.value
        val initialEncounters = repository.encounters.value
            .filter { it.contactId == contactId }
            .sortedByDescending { it.occurredAt }
        val initialReminders = repository.followUps.value
            .filter { it.contactId == contactId && !it.isDone }
            .sortedBy { it.dueAt }
        val initialLoading = initialContact == null && initialSummary == null
        val initialFavorite = initialContact?.isFavorite ?: (initialSummary?.isFavorite ?: false)

        _uiState = MutableStateFlow(
            PersonDetailUiState(
                contactId = contactId,
                contact = initialContact,
                contactSummary = initialSummary,
                encounters = initialEncounters,
                reminders = initialReminders,
                placesMap = initialPlaces,
                tagsList = initialTags,
                isLoading = initialLoading,
                isFavorite = initialFavorite
            )
        )
        uiState = _uiState.asStateFlow()

        combine(
            repository.getContactFlow(contactId),
            repository.getEncountersForContact(contactId),
            repository.getFollowUpsForContact(contactId),
            repository.places,
            repository.tags
        ) { contact, encounters, reminders, places, tags ->
            val placesMap = places.associateBy { it.id }
            val summary = repository.contacts.value.firstOrNull { it.id == contactId }
            val isFavorite = contact?.isFavorite ?: (summary?.isFavorite ?: false)

            _uiState.update { current ->
                val allEncounterIds = encounters.map { it.id }.toSet()
                val prunedSelected = current.selectedEncounterIds.filter { it in allEncounterIds }.toSet()
                current.copy(
                    contact = contact,
                    contactSummary = summary,
                    encounters = encounters,
                    reminders = reminders,
                    placesMap = placesMap,
                    tagsList = tags,
                    selectedEncounterIds = prunedSelected,
                    isLoading = false,
                    isFavorite = isFavorite
                )
            }
        }.launchIn(viewModelScope)

        if (preferencesRepository != null) {
            viewModelScope.launch {
                preferencesRepository.userPreferencesFlow.collect { prefs ->
                    repository.setDefaultNotificationTime(prefs.notificationHour, prefs.notificationMinute)
                    _uiState.update {
                        it.copy(
                            defaultNotificationHour = prefs.notificationHour,
                            defaultNotificationMinute = prefs.notificationMinute
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: PersonDetailUiEvent) {
        when (event) {
            PersonDetailUiEvent.OnToggleFavorite -> {
                repository.toggleFavorite(contactId)
            }
            PersonDetailUiEvent.OnPhoneClick -> {
                val phone = _uiState.value.contact?.phone
                if (phone.isNullOrBlank()) {
                    _uiState.update { it.copy(userMessage = "No phone number available", undoKind = PersonDetailUndoKind.NONE) }
                }
            }
            PersonDetailUiEvent.OnMessageClick -> {
                val phone = _uiState.value.contact?.phone
                if (phone.isNullOrBlank()) {
                    _uiState.update { it.copy(userMessage = "No phone number available to message", undoKind = PersonDetailUndoKind.NONE) }
                }
            }
            PersonDetailUiEvent.OnEmailClick -> {
                val email = _uiState.value.contact?.email
                if (email.isNullOrBlank()) {
                    _uiState.update { it.copy(userMessage = "No email address available", undoKind = PersonDetailUndoKind.NONE) }
                }
            }
            PersonDetailUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null, undoKind = PersonDetailUndoKind.NONE) }
            }
            is PersonDetailUiEvent.OnEncounterLongClick -> {
                _uiState.update { current ->
                    current.copy(selectedEncounterIds = current.selectedEncounterIds + event.encounterId)
                }
            }
            is PersonDetailUiEvent.OnToggleSelectEncounter -> {
                _uiState.update { current ->
                    val updated = if (event.encounterId in current.selectedEncounterIds) {
                        current.selectedEncounterIds - event.encounterId
                    } else {
                        current.selectedEncounterIds + event.encounterId
                    }
                    current.copy(selectedEncounterIds = updated)
                }
            }
            PersonDetailUiEvent.OnClearSelection -> {
                _uiState.update { current ->
                    current.copy(
                        selectedEncounterIds = emptySet(),
                        isDeleteConfirmationVisible = false
                    )
                }
            }
            PersonDetailUiEvent.OnSelectAllEncounters -> {
                _uiState.update { current ->
                    val allIds = current.encounters.map { it.id }.toSet()
                    current.copy(selectedEncounterIds = allIds)
                }
            }
            PersonDetailUiEvent.OnRequestDeleteSelected -> {
                if (_uiState.value.selectedEncounterIds.isNotEmpty()) {
                    _uiState.update { it.copy(isDeleteConfirmationVisible = true) }
                }
            }
            PersonDetailUiEvent.OnDismissDeleteDialog -> {
                _uiState.update { it.copy(isDeleteConfirmationVisible = false) }
            }
            PersonDetailUiEvent.OnConfirmDeleteSelected -> {
                val toDelete = _uiState.value.selectedEncounterIds
                if (toDelete.isNotEmpty()) {
                    repository.deleteEncounters(toDelete)
                    val count = toDelete.size
                    val message = "Deleted $count encounter${if (count > 1) "s" else ""}"
                    _uiState.update { current ->
                        current.copy(
                            selectedEncounterIds = emptySet(),
                            isDeleteConfirmationVisible = false,
                            userMessage = message,
                            undoKind = PersonDetailUndoKind.ENCOUNTER
                        )
                    }
                }
            }
            PersonDetailUiEvent.OnUndoDeleteEncounter -> {
                if (repository.undoLastDeleteEncounters()) {
                    _uiState.update { it.copy(userMessage = "Encounter restored", undoKind = PersonDetailUndoKind.NONE) }
                }
            }
            PersonDetailUiEvent.OnOpenCreateReminderSheet -> {
                _uiState.update {
                    it.copy(
                        isCreateReminderSheetOpen = true,
                        editingReminder = null
                    )
                }
            }
            is PersonDetailUiEvent.OnOpenEditReminderSheet -> {
                _uiState.update {
                    it.copy(
                        editingReminder = event.followUp,
                        isCreateReminderSheetOpen = false
                    )
                }
            }
            PersonDetailUiEvent.OnDismissReminderSheet -> {
                _uiState.update {
                    it.copy(
                        isCreateReminderSheetOpen = false,
                        editingReminder = null
                    )
                }
            }
            is PersonDetailUiEvent.OnCreateReminder -> {
                val state = _uiState.value
                repository.addFollowUp(
                    contactId = contactId,
                    dueDate = event.dueDate,
                    note = event.note,
                    hasExplicitTime = event.hasExplicitTime,
                    explicitTime = event.explicitTime,
                    defaultHour = state.defaultNotificationHour,
                    defaultMinute = state.defaultNotificationMinute
                )
                _uiState.update {
                    it.copy(
                        isCreateReminderSheetOpen = false,
                        editingReminder = null,
                        reminderSavedEventId = it.reminderSavedEventId + 1L,
                        userMessage = null,
                        undoKind = PersonDetailUndoKind.NONE
                    )
                }
            }
            is PersonDetailUiEvent.OnUpdateReminder -> {
                val state = _uiState.value
                repository.updateFollowUp(
                    followUpId = event.followUpId,
                    dueDate = event.dueDate,
                    note = event.note,
                    hasExplicitTime = event.hasExplicitTime,
                    explicitTime = event.explicitTime,
                    defaultHour = state.defaultNotificationHour,
                    defaultMinute = state.defaultNotificationMinute
                )
                _uiState.update {
                    it.copy(
                        editingReminder = null,
                        reminderSavedEventId = it.reminderSavedEventId + 1L,
                        userMessage = null,
                        undoKind = PersonDetailUndoKind.NONE
                    )
                }
            }
            is PersonDetailUiEvent.OnMarkReminderDone -> {
                repository.markFollowUpDone(event.followUpId)
                _uiState.update {
                    it.copy(
                        editingReminder = if (it.editingReminder?.id == event.followUpId) null else it.editingReminder,
                        userMessage = "Reminder completed",
                        undoKind = PersonDetailUndoKind.REMINDER
                    )
                }
            }
            is PersonDetailUiEvent.OnSnoozeReminder -> {
                val state = _uiState.value
                repository.snoozeFollowUp(
                    alertId = event.followUpId,
                    days = 3L,
                    defaultHour = state.defaultNotificationHour,
                    defaultMinute = state.defaultNotificationMinute
                )
                _uiState.update {
                    it.copy(
                        userMessage = "Snoozed for 3 days",
                        undoKind = PersonDetailUndoKind.NONE
                    )
                }
            }
            is PersonDetailUiEvent.OnDeleteReminder -> {
                repository.deleteFollowUp(event.followUpId)
                _uiState.update {
                    it.copy(
                        editingReminder = null,
                        userMessage = "Deleted reminder",
                        undoKind = PersonDetailUndoKind.REMINDER
                    )
                }
            }
            PersonDetailUiEvent.OnUndoReminderMutation -> {
                if (repository.undoLastFollowUpMutation()) {
                    _uiState.update {
                        it.copy(
                            userMessage = "Reminder restored",
                            undoKind = PersonDetailUndoKind.NONE
                        )
                    }
                }
            }
        }
    }
}
