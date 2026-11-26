package com.example.hello.ui.detail

import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.Tag
import java.time.LocalDate
import java.time.LocalTime

enum class PersonDetailUndoKind {
    NONE,
    ENCOUNTER,
    REMINDER
}

data class PersonDetailUiState(
    val contactId: String = "",
    val contact: Contact? = null,
    val contactSummary: ContactSummary? = null,
    val encounters: List<Encounter> = emptyList(),
    val reminders: List<FollowUp> = emptyList(),
    val completedReminders: List<FollowUp> = emptyList(),
    val placesMap: Map<String, Place> = emptyMap(),
    val tagsList: List<Tag> = emptyList(),
    val allContacts: List<ContactSummary> = emptyList(),
    val defaultNotificationHour: Int = 9,
    val defaultNotificationMinute: Int = 0,
    val isCreateReminderSheetOpen: Boolean = false,
    val editingReminder: FollowUp? = null,
    val reminderSavedEventId: Long = 0L,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val undoKind: PersonDetailUndoKind = PersonDetailUndoKind.NONE,
    val selectedEncounterIds: Set<String> = emptySet(),
    val isDeleteConfirmationVisible: Boolean = false,
    val isFavorite: Boolean = false
) {
    val isSelectionMode: Boolean get() = selectedEncounterIds.isNotEmpty()
}

sealed interface PersonDetailUiEvent {
    data object OnToggleFavorite : PersonDetailUiEvent
    data object OnPhoneClick : PersonDetailUiEvent
    data object OnMessageClick : PersonDetailUiEvent
    data object OnEmailClick : PersonDetailUiEvent
    data object OnDismissUserMessage : PersonDetailUiEvent
    data class OnEncounterLongClick(val encounterId: String) : PersonDetailUiEvent
    data class OnToggleSelectEncounter(val encounterId: String) : PersonDetailUiEvent
    data object OnClearSelection : PersonDetailUiEvent
    data object OnSelectAllEncounters : PersonDetailUiEvent
    data object OnRequestDeleteSelected : PersonDetailUiEvent
    data object OnConfirmDeleteSelected : PersonDetailUiEvent
    data object OnDismissDeleteDialog : PersonDetailUiEvent
    data object OnUndoDeleteEncounter : PersonDetailUiEvent

    // Profile Reminder Events (ADR-0037, RAM-60)
    data object OnOpenCreateReminderSheet : PersonDetailUiEvent
    data class OnOpenEditReminderSheet(val followUp: FollowUp) : PersonDetailUiEvent
    data object OnDismissReminderSheet : PersonDetailUiEvent
    data class OnCreateReminder(
        val dueDate: LocalDate,
        val note: String,
        val hasExplicitTime: Boolean = false,
        val explicitTime: LocalTime? = null,
        val participantIds: List<String> = emptyList()
    ) : PersonDetailUiEvent
    data class OnUpdateReminder(
        val followUpId: String,
        val dueDate: LocalDate,
        val note: String,
        val hasExplicitTime: Boolean = false,
        val explicitTime: LocalTime? = null,
        val participantIds: List<String> = emptyList()
    ) : PersonDetailUiEvent
    data class OnMarkReminderDone(val followUpId: String) : PersonDetailUiEvent
    data class OnReopenReminder(val followUpId: String) : PersonDetailUiEvent
    data class OnSnoozeReminder(val followUpId: String) : PersonDetailUiEvent
    data class OnDeleteReminder(val followUpId: String) : PersonDetailUiEvent
    data object OnUndoReminderMutation : PersonDetailUiEvent
}
