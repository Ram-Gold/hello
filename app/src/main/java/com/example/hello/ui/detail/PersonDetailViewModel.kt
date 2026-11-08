package com.example.hello.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.Duration
import java.time.Instant

class PersonDetailViewModel(
    val contactId: String,
    val repository: ContactRepository
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
        val initialCadence = calculateCadenceStatus(initialContact, initialEncounters)
        val initialLoading = initialContact == null && initialSummary == null
        val initialFavorite = initialContact?.isFavorite ?: (initialSummary?.isFavorite ?: false)

        _uiState = MutableStateFlow(
            PersonDetailUiState(
                contactId = contactId,
                contact = initialContact,
                contactSummary = initialSummary,
                encounters = initialEncounters,
                placesMap = initialPlaces,
                tagsList = initialTags,
                cadenceStatus = initialCadence,
                isLoading = initialLoading,
                isFavorite = initialFavorite
            )
        )
        uiState = _uiState.asStateFlow()

        combine(
            repository.getContactFlow(contactId),
            repository.getEncountersForContact(contactId),
            repository.places,
            repository.tags
        ) { contact, encounters, places, tags ->
            val placesMap = places.associateBy { it.id }
            val cadenceStatus = calculateCadenceStatus(contact, encounters)
            val summary = repository.contacts.value.firstOrNull { it.id == contactId }
            val isFavorite = contact?.isFavorite ?: (summary?.isFavorite ?: false)

            _uiState.update { current ->
                val allEncounterIds = encounters.map { it.id }.toSet()
                val prunedSelected = current.selectedEncounterIds.filter { it in allEncounterIds }.toSet()
                current.copy(
                    contact = contact,
                    contactSummary = summary,
                    encounters = encounters,
                    placesMap = placesMap,
                    tagsList = tags,
                    cadenceStatus = cadenceStatus,
                    selectedEncounterIds = prunedSelected,
                    isLoading = false,
                    isFavorite = isFavorite
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: PersonDetailUiEvent) {
        when (event) {
            PersonDetailUiEvent.OnToggleFavorite -> {
                repository.toggleFavorite(contactId)
            }
            PersonDetailUiEvent.OnPhoneClick -> {
                val phone = _uiState.value.contact?.phone
                if (phone.isNullOrBlank()) {
                    _uiState.update { it.copy(userMessage = "No phone number available") }
                }
            }
            PersonDetailUiEvent.OnMessageClick -> {
                val phone = _uiState.value.contact?.phone
                if (phone.isNullOrBlank()) {
                    _uiState.update { it.copy(userMessage = "No phone number available to message") }
                }
            }
            PersonDetailUiEvent.OnEmailClick -> {
                val email = _uiState.value.contact?.email
                if (email.isNullOrBlank()) {
                    _uiState.update { it.copy(userMessage = "No email address available") }
                }
            }
            PersonDetailUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
            is PersonDetailUiEvent.OnEncounterLongClick -> {
                _uiState.update { state ->
                    if (state.encounters.any { it.id == event.encounterId }) {
                        state.copy(selectedEncounterIds = state.selectedEncounterIds + event.encounterId)
                    } else {
                        state
                    }
                }
            }
            is PersonDetailUiEvent.OnToggleSelectEncounter -> {
                _uiState.update { state ->
                    if (state.encounters.any { it.id == event.encounterId }) {
                        val next = if (event.encounterId in state.selectedEncounterIds) {
                            state.selectedEncounterIds - event.encounterId
                        } else {
                            state.selectedEncounterIds + event.encounterId
                        }
                        state.copy(selectedEncounterIds = next)
                    } else {
                        state
                    }
                }
            }
            PersonDetailUiEvent.OnClearSelection -> {
                _uiState.update { it.copy(selectedEncounterIds = emptySet(), isDeleteConfirmationVisible = false) }
            }
            PersonDetailUiEvent.OnSelectAllEncounters -> {
                _uiState.update { state ->
                    state.copy(selectedEncounterIds = state.encounters.map { it.id }.toSet())
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
                    _uiState.update {
                        it.copy(
                            selectedEncounterIds = emptySet(),
                            isDeleteConfirmationVisible = false,
                            userMessage = message
                        )
                    }
                }
            }
            PersonDetailUiEvent.OnUndoDeleteEncounter -> {
                if (repository.undoLastDeleteEncounters()) {
                    _uiState.update { it.copy(userMessage = "Encounter restored") }
                }
            }
        }
    }

    companion object {
        fun calculateCadenceStatus(contact: Contact?, encounters: List<Encounter>): CadenceStatusInfo {
            if (contact == null || contact.cadenceDays == null || contact.cadenceDays <= 0) {
                return CadenceStatusInfo(
                    health = CadenceHealth.NO_CADENCE,
                    displayText = "No cadence set",
                    subText = "Set a stay-in-touch cadence in edit"
                )
            }

            val targetDays = contact.cadenceDays
            val lastEncounterInstant = encounters.firstOrNull()?.occurredAt ?: contact.lastMetAt
            if (lastEncounterInstant == null) {
                return CadenceStatusInfo(
                    health = CadenceHealth.DUE_SOON,
                    cadenceDays = targetDays,
                    displayText = "Never met yet",
                    subText = "Target cadence: Every $targetDays days"
                )
            }

            val now = Instant.now()
            val daysSince = Duration.between(lastEncounterInstant, now).toDays().toInt().coerceAtLeast(0)
            val daysDiff = targetDays - daysSince

            return when {
                daysDiff < 0 -> {
                    val daysOverdue = -daysDiff
                    CadenceStatusInfo(
                        health = CadenceHealth.OVERDUE,
                        daysSinceLastMet = daysSince,
                        cadenceDays = targetDays,
                        daysRemaining = daysDiff,
                        displayText = "Overdue by $daysOverdue ${if (daysOverdue == 1) "day" else "days"}",
                        subText = "Cadence: Every $targetDays days • Last met $daysSince days ago"
                    )
                }
                daysDiff <= 3 -> {
                    CadenceStatusInfo(
                        health = CadenceHealth.DUE_SOON,
                        daysSinceLastMet = daysSince,
                        cadenceDays = targetDays,
                        daysRemaining = daysDiff,
                        displayText = "Due in $daysDiff ${if (daysDiff == 1) "day" else "days"}",
                        subText = "Cadence: Every $targetDays days • Last met $daysSince days ago"
                    )
                }
                else -> {
                    CadenceStatusInfo(
                        health = CadenceHealth.ON_TRACK,
                        daysSinceLastMet = daysSince,
                        cadenceDays = targetDays,
                        daysRemaining = daysDiff,
                        displayText = "On track • Due in $daysDiff days",
                        subText = "Cadence: Every $targetDays days • Last met $daysSince days ago"
                    )
                }
            }
        }
    }
}
