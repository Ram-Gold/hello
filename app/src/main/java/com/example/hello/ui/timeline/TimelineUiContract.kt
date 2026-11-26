package com.example.hello.ui.timeline

import com.example.hello.data.preferences.WeekStartDay
import com.example.hello.ui.components.ParticipantAvatar
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay

enum class PastEncounterBadge(val label: String) {
    FIRST_MET("First Met"),
    MET_AGAIN("Met Again"),
    COMPLETED_REMINDER("Completed")
}

sealed interface FutureTimelineNode {
    val id: String
    val contactId: String
    val contactName: String
    val avatarInitials: String
    val avatarBgIndex: Int
    val daysUntil: Long
    val dateLabel: String
    val photoUri: String?
    val targetDate: LocalDate

    data class FollowUpNode(
        override val id: String,
        override val contactId: String,
        override val contactName: String,
        override val avatarInitials: String,
        override val avatarBgIndex: Int,
        override val daysUntil: Long,
        override val dateLabel: String,
        val followUpId: String,
        val note: String,
        override val photoUri: String? = null,
        override val targetDate: LocalDate = LocalDate.now().plusDays(daysUntil.coerceAtLeast(0L)),
        val participants: List<ParticipantAvatar> = emptyList()
    ) : FutureTimelineNode

    data class BirthdayNode(
        override val id: String,
        override val contactId: String,
        override val contactName: String,
        override val avatarInitials: String,
        override val avatarBgIndex: Int,
        override val daysUntil: Long,
        override val dateLabel: String,
        val turningAge: Int? = null,
        override val photoUri: String? = null,
        override val targetDate: LocalDate = LocalDate.now().plusDays(daysUntil.coerceAtLeast(0L))
    ) : FutureTimelineNode
}

data class PastTimelineNode(
    val id: String,
    val contactId: String,
    val contactName: String,
    val avatarInitials: String,
    val avatarBgIndex: Int,
    val occurredAt: Instant,
    val dateLabel: String,
    val placeLabel: String?,
    val notes: String,
    val badge: PastEncounterBadge,
    val photoUri: String? = null,
    val timeLabel: String = "",
    val participants: List<ParticipantAvatar> = emptyList(),
    val followUpId: String? = null
) {
    val isCompletedReminder: Boolean get() = followUpId != null
}

data class TimelineDayGroup<T>(
    val date: LocalDate,
    val headerLabel: String,
    val items: List<T>
)

data class TimelineUiState(
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = today,
    val firstDayOfWeek: WeekStartDay = WeekStartDay.MONDAY,
    val isDatePickerOpen: Boolean = false,
    val scrollRequestId: Long = 0L,
    val futureNodes: List<FutureTimelineNode> = emptyList(),
    val pastNodes: List<PastTimelineNode> = emptyList(),
    val futureDayGroups: List<TimelineDayGroup<FutureTimelineNode>> = emptyList(),
    val pastDayGroups: List<TimelineDayGroup<PastTimelineNode>> = emptyList(),
    val pastBirthdaysByDate: Map<LocalDate, List<FutureTimelineNode.BirthdayNode>> = emptyMap(),
    val activeEntryDates: Set<LocalDate> = emptySet(),
    val recurringBirthdayMonthDays: Set<MonthDay> = emptySet(),
    val todayLabel: String = "TODAY",
    val userMessage: String? = null,
    val selectedNodeIds: Set<String> = emptySet(),
    val isDeleteConfirmationVisible: Boolean = false
) {
    val isSelectionMode: Boolean get() = selectedNodeIds.isNotEmpty()
}

sealed interface TimelineUiEvent {
    data class OnSelectDate(val date: LocalDate, val fromScroll: Boolean = false) : TimelineUiEvent
    data class OnStepDay(val deltaDays: Long) : TimelineUiEvent
    data class OnStepWeek(val deltaWeeks: Long) : TimelineUiEvent
    data object OnJumpToToday : TimelineUiEvent
    data object OnOpenDatePicker : TimelineUiEvent
    data object OnDismissDatePicker : TimelineUiEvent
    data class OnMarkFollowUpDone(val followUpId: String) : TimelineUiEvent
    data class OnReopenFollowUp(val followUpId: String) : TimelineUiEvent
    data class OnSnoozeFollowUp(val followUpId: String) : TimelineUiEvent
    data object OnDismissUserMessage : TimelineUiEvent
    data class OnNodeLongClick(val nodeId: String) : TimelineUiEvent
    data class OnToggleSelectNode(val nodeId: String) : TimelineUiEvent
    data object OnClearSelection : TimelineUiEvent
    data object OnSelectAllVisibleNodes : TimelineUiEvent
    data object OnRequestDeleteSelected : TimelineUiEvent
    data object OnConfirmDeleteSelected : TimelineUiEvent
    data object OnDismissDeleteDialog : TimelineUiEvent
}
