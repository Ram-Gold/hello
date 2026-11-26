package com.example.hello.ui.detail.components

import com.example.hello.R
import com.example.hello.ui.components.EmptyStateIllustration

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.domain.model.formatWithOthersLabel
import com.example.hello.ui.components.BirthdayInputUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val CONTENT_TYPE_SECTION_HEADER = "living_timeline_section_header"
private const val CONTENT_TYPE_UPCOMING_BIRTHDAY = "living_timeline_upcoming_birthday"
private const val CONTENT_TYPE_FUTURE_REMINDER = "living_timeline_future_reminder"
private const val CONTENT_TYPE_TODAY_DIVIDER = "living_timeline_today_divider"
private const val CONTENT_TYPE_EMPTY = "living_timeline_empty"
private const val CONTENT_TYPE_ENCOUNTER = "living_timeline_encounter"
private const val CONTENT_TYPE_COMPLETED_REMINDER = "living_timeline_completed_reminder"

private val MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val MONTH_DAY_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val TIME_OF_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

sealed interface LivingTimelinePastEntry {
    val id: String
    val timestamp: Instant

    data class EncounterEntry(val encounter: Encounter) : LivingTimelinePastEntry {
        override val id: String get() = encounter.id
        override val timestamp: Instant get() = encounter.occurredAt
    }

    data class CompletedReminderEntry(val followUp: FollowUp) : LivingTimelinePastEntry {
        override val id: String get() = "completed-followup-${followUp.id}"
        override val timestamp: Instant get() = followUp.doneAt ?: followUp.dueAt
    }
}

fun buildLivingTimelinePastEntries(
    encounters: List<Encounter>,
    completedReminders: List<FollowUp>
): List<LivingTimelinePastEntry> = buildList(encounters.size + completedReminders.size) {
    encounters.forEach { add(LivingTimelinePastEntry.EncounterEntry(it)) }
    completedReminders.forEach { add(LivingTimelinePastEntry.CompletedReminderEntry(it)) }
}.sortedWith(compareByDescending<LivingTimelinePastEntry> { it.timestamp }.thenBy { it.id })

fun LazyListScope.livingTimelineItems(
    encountersCount: Int,
    pastEntries: List<LivingTimelinePastEntry>,
    placesMap: Map<String, Place>,
    today: LocalDate,
    now: Instant,
    zoneId: ZoneId,
    onLogEncounterClick: () -> Unit = {},
    birthday: LocalDate? = null,
    selectedEncounterIds: Set<String> = emptySet(),
    isSelectionMode: Boolean = false,
    onEncounterClick: (String) -> Unit = {},
    onEncounterLongClick: (String) -> Unit = {},
    reminders: List<FollowUp> = emptyList(),
    onReminderClick: (FollowUp) -> Unit = {},
    onMarkReminderDone: (String) -> Unit = {},
    onReopenReminder: (String) -> Unit = {},
    onSnoozeReminder: (String) -> Unit = {},
    currentContactId: String = "",
    contactNamesById: Map<String, String> = emptyMap()
) {
    // Section Header Row (Encounter logging and Reminder creation hosted by bottom split Profile Action Bar per ADR-0034 & RAM-60)
    item(key = "timeline_header", contentType = CONTENT_TYPE_SECTION_HEADER) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "LIVING TIMELINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val encountersLabel = "$encountersCount ${if (encountersCount == 1) "encounter" else "encounters"} recorded"
                val subtitle = if (reminders.isNotEmpty()) {
                    "$encountersLabel, ${reminders.size} upcoming"
                } else {
                    encountersLabel
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (birthday != null && BirthdayInputUtils.isUpcomingWithinOneMonth(birthday, today)) {
        item(key = "timeline_upcoming_birthday", contentType = CONTENT_TYPE_UPCOMING_BIRTHDAY) {
            UpcomingBirthdayCard(
                birthday = birthday,
                today = today,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }

    // Future Horizon: Active & Overdue Reminders on a dashed spine above TODAY divider
    if (reminders.isNotEmpty()) {
        items(
            items = reminders,
            key = { "timeline_reminder_${it.id}" },
            contentType = { CONTENT_TYPE_FUTURE_REMINDER }
        ) { reminder ->
            val withOthersText = formatWithOthersLabel(
                reminder.resolvedParticipantIds()
                    .filterNot { it == currentContactId }
                    .mapNotNull { contactNamesById[it] }
            )
            FutureReminderNodeItem(
                followUp = reminder,
                today = today,
                now = now,
                zoneId = zoneId,
                withOthersText = withOthersText,
                onClick = { onReminderClick(reminder) },
                onDoneClick = { onMarkReminderDone(reminder.id) },
                onSnoozeClick = { onSnoozeReminder(reminder.id) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item(key = "timeline_today_divider", contentType = CONTENT_TYPE_TODAY_DIVIDER) {
            TodayTimelineDivider(
                today = today,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }

    if (pastEntries.isEmpty()) {
        item(key = "timeline_empty", contentType = CONTENT_TYPE_EMPTY) {
            EmptyTimelineCard(
                onLogEncounterClick = onLogEncounterClick,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
            )
        }
    } else {
        itemsIndexed(
            items = pastEntries,
            key = { _, entry -> "timeline_past_${entry.id}" },
            contentType = { _, entry ->
                when (entry) {
                    is LivingTimelinePastEntry.EncounterEntry -> CONTENT_TYPE_ENCOUNTER
                    is LivingTimelinePastEntry.CompletedReminderEntry -> CONTENT_TYPE_COMPLETED_REMINDER
                }
            }
        ) { index, entry ->
            val isFirst = index == 0
            val isLast = index == pastEntries.lastIndex
            val itemModifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                bottom = if (isLast) 8.dp else 0.dp
            )
            when (entry) {
                is LivingTimelinePastEntry.EncounterEntry -> {
                    val encounter = entry.encounter
                    val isSelected = encounter.id in selectedEncounterIds
                    val withOthersText = formatWithOthersLabel(
                        encounter.resolvedParticipantIds()
                            .filterNot { it == currentContactId }
                            .mapNotNull { contactNamesById[it] }
                    )
                    TimelineNodeItem(
                        encounter = encounter,
                        place = encounter.placeId?.let { placesMap[it] },
                        today = today,
                        zoneId = zoneId,
                        withOthersText = withOthersText,
                        isFirst = isFirst,
                        isLast = isLast,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                onEncounterClick(encounter.id)
                            }
                        },
                        onLongClick = {
                            onEncounterLongClick(encounter.id)
                        },
                        modifier = itemModifier
                    )
                }
                is LivingTimelinePastEntry.CompletedReminderEntry -> {
                    val reminder = entry.followUp
                    val withOthersText = formatWithOthersLabel(
                        reminder.resolvedParticipantIds()
                            .filterNot { it == currentContactId }
                            .mapNotNull { contactNamesById[it] }
                    )
                    CompletedReminderNodeItem(
                        followUp = reminder,
                        today = today,
                        zoneId = zoneId,
                        withOthersText = withOthersText,
                        isFirst = isFirst,
                        isLast = isLast,
                        onReopenClick = { onReopenReminder(reminder.id) },
                        modifier = itemModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayTimelineDivider(
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val label = "TODAY, ${today.format(MONTH_DAY_FORMATTER).uppercase()}"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun FutureReminderNodeItem(
    followUp: FollowUp,
    today: LocalDate,
    now: Instant,
    zoneId: ZoneId,
    withOthersText: String? = null,
    onClick: () -> Unit,
    onDoneClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localDateTime = followUp.dueAt.atZone(zoneId)
    val dueDate = localDateTime.toLocalDate()
    val isOverdue = followUp.dueAt.isBefore(now)
    val timeLabel = localDateTime.toLocalTime().format(TIME_OF_DAY_FORMATTER)
    val daysDiff = ChronoUnit.DAYS.between(today, dueDate)
    val dueBadgeText = when {
        isOverdue && daysDiff < 0L -> "Overdue (${-daysDiff}d ago)"
        isOverdue -> "Overdue, $timeLabel"
        daysDiff == 0L -> "Today, $timeLabel"
        daysDiff == 1L -> "Tomorrow, $timeLabel"
        daysDiff <= 7L -> "In $daysDiff days, $timeLabel"
        else -> "${dueDate.format(MONTH_DAY_FORMATTER)}, $timeLabel"
    }

    val spineColor = if (isOverdue) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    }
    val cardBg = if (isOverdue) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val cardBorder = if (isOverdue) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Dashed vertical future spine + node dot
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Canvas(modifier = Modifier.fillMaxHeight().width(2.dp)) {
                drawLine(
                    color = spineColor,
                    start = Offset(size.width / 2f, 20.dp.toPx()),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surface)
                    .border(
                        width = 2.dp,
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = dueBadgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isOverdue) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = followUp.note.ifBlank { "Scheduled reminder" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!withOthersText.isNullOrBlank()) {
                        WithOthersChip(label = withOthersText)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            .clickable(onClick = onDoneClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Mark reminder done",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onSnoozeClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = "Snooze 3 days",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingBirthdayCard(
    birthday: LocalDate,
    today: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier
) {
    val nextBday = BirthdayInputUtils.nextAnnualBirthday(birthday, today)
    val daysUntil = ChronoUnit.DAYS.between(today, nextBday)
    val monthDayLabel = BirthdayInputUtils.formatAbbreviatedMonthDay(nextBday)
    val countdownLabel = BirthdayInputUtils.formatBirthdayCountdown(daysUntil)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Birthday on $monthDayLabel",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = countdownLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WithOthersChip(
    label: String,
    isSelected: Boolean = false
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Group,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineNodeItem(
    encounter: Encounter,
    place: Place?,
    today: LocalDate,
    zoneId: ZoneId,
    withOthersText: String? = null,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateText = formatTimelineDate(encounter.occurredAt, today, zoneId)
    val placeLabel = encounter.eventLabel ?: place?.label
    val spineColor = MaterialTheme.colorScheme.onSurface
    val dotFillColor = MaterialTheme.colorScheme.surface

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val secondaryContentColor = if (isSelected) {
        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // High-Contrast Vertical Timeline Spine & Centered Hollow Node Dot
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Spine Column: Top half down to center, bottom half down from center
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val topBrush = if (isFirst) {
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, spineColor)
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(spineColor, spineColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(topBrush)
                )

                val bottomBrush = if (isLast) {
                    Brush.verticalGradient(
                        colors = listOf(spineColor, Color.Transparent)
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(spineColor, spineColor)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(bottomBrush)
                )
            }

            // Hollow Center Node Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotFillColor)
                    .border(
                        width = 2.dp,
                        color = spineColor,
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Encounter Content Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            contentColor = contentColor,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    }

                    if (!placeLabel.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = placeLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = secondaryContentColor
                                )
                            }
                        }
                    }
                }

                if (!withOthersText.isNullOrBlank()) {
                    WithOthersChip(label = withOthersText, isSelected = isSelected)
                }

                if (encounter.notes.isNotBlank()) {
                    Text(
                        text = encounter.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryContentColor,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTimelineCard(
    onLogEncounterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmptyStateIllustration(
                drawableRes = R.drawable.ic_empty_profile_timeline,
                height = 88.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No encounters logged yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Log moments, coffees, and catch-ups to cultivate this relationship.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompletedReminderNodeItem(
    followUp: FollowUp,
    today: LocalDate,
    zoneId: ZoneId,
    withOthersText: String? = null,
    isFirst: Boolean,
    isLast: Boolean,
    onReopenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val completedAt = followUp.doneAt ?: followUp.dueAt
    val dateText = formatCompletedTimelineDate(completedAt, today, zoneId)
    val spineColor = MaterialTheme.colorScheme.onSurface
    val dotFillColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val topBrush = if (isFirst) {
                    Brush.verticalGradient(colors = listOf(Color.Transparent, spineColor))
                } else {
                    Brush.verticalGradient(colors = listOf(spineColor, spineColor))
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(topBrush)
                )
                val bottomBrush = if (isLast) {
                    Brush.verticalGradient(colors = listOf(spineColor, Color.Transparent))
                } else {
                    Brush.verticalGradient(colors = listOf(spineColor, spineColor))
                }
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(bottomBrush)
                )
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotFillColor)
                    .border(width = 2.dp, color = spineColor, shape = CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!withOthersText.isNullOrBlank()) {
                    WithOthersChip(label = withOthersText)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = followUp.note.ifBlank { "Scheduled reminder" },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onReopenClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Replay,
                                contentDescription = "Reopen reminder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Reopen",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompletedTimelineDate(
    instant: Instant,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val date = instant.atZone(zoneId).toLocalDate()
    val daysBetween = ChronoUnit.DAYS.between(date, today)

    return when {
        daysBetween <= 0L -> "Completed today"
        daysBetween == 1L -> "Completed yesterday"
        daysBetween in 2..6 -> "Completed $daysBetween days ago"
        date.year == today.year -> "Completed ${date.format(MONTH_DAY_FORMATTER)}"
        else -> "Completed ${date.format(MONTH_DAY_YEAR_FORMATTER)}"
    }
}

private fun formatTimelineDate(
    instant: Instant,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val date = instant.atZone(zoneId).toLocalDate()
    val daysBetween = ChronoUnit.DAYS.between(date, today)

    return when {
        daysBetween == 0L -> "Met today"
        daysBetween == 1L -> "Met yesterday"
        daysBetween in 2..6 -> "$daysBetween days ago"
        date.year == today.year -> date.format(MONTH_DAY_FORMATTER)
        else -> date.format(MONTH_DAY_YEAR_FORMATTER)
    }
}
