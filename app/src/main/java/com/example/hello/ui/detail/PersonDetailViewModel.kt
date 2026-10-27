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

    private val _uiState = MutableStateFlow(PersonDetailUiState(contactId = contactId, isLoading = true))
    val uiState: StateFlow<PersonDetailUiState> = _uiState.asStateFlow()

    init {
        combine(
            repository.getContactFlow(contactId),
            repository.getEncountersForContact(contactId),
            repository.places,
            repository.tags
        ) { contact, encounters, places, tags ->
            val placesMap = places.associateBy { it.id }
            val cadenceStatus = calculateCadenceStatus(contact, encounters)
            val summary = repository.contacts.value.firstOrNull { it.id == contactId }

            _uiState.update { current ->
                current.copy(
                    contact = contact,
                    contactSummary = summary,
                    encounters = encounters,
                    placesMap = placesMap,
                    tagsList = tags,
                    cadenceStatus = cadenceStatus,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: PersonDetailUiEvent) {
        when (event) {
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
