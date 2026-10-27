package com.example.hello.ui.detail

import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag

enum class CadenceHealth {
    ON_TRACK,
    DUE_SOON,
    OVERDUE,
    NO_CADENCE
}

data class CadenceStatusInfo(
    val health: CadenceHealth = CadenceHealth.NO_CADENCE,
    val daysSinceLastMet: Int? = null,
    val cadenceDays: Int? = null,
    val daysRemaining: Int? = null,
    val displayText: String = "No cadence set",
    val subText: String = "Set a stay-in-touch cadence to get reminders"
)

data class PersonDetailUiState(
    val contactId: String = "",
    val contact: Contact? = null,
    val contactSummary: ContactSummary? = null,
    val encounters: List<Encounter> = emptyList(),
    val placesMap: Map<String, Place> = emptyMap(),
    val tagsList: List<Tag> = emptyList(),
    val cadenceStatus: CadenceStatusInfo = CadenceStatusInfo(),
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

sealed interface PersonDetailUiEvent {
    data object OnPhoneClick : PersonDetailUiEvent
    data object OnMessageClick : PersonDetailUiEvent
    data object OnEmailClick : PersonDetailUiEvent
    data object OnDismissUserMessage : PersonDetailUiEvent
}
