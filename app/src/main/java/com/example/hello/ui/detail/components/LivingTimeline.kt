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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.Place
import com.example.hello.ui.components.BirthdayInputUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LivingTimeline(
    encounters: List<Encounter>,
    placesMap: Map<String, Place>,
    onLogEncounterClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    birthday: LocalDate? = null,
    selectedEncounterIds: Set<String> = emptySet(),
    isSelectionMode: Boolean = false,
    onEncounterClick: (String) -> Unit = {},
    onEncounterLongClick: (String) -> Unit = {},
    contactName: String = "Contact",
    reminders: List<FollowUp> = emptyList(),
    onReminderClick: (FollowUp) -> Unit = {},
    onMarkReminderDone: (String) -> Unit = {},
    onSnoozeReminder: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Section Header Row (Encounter logging and Reminder creation hosted by bottom split Profile Action Bar per ADR-0034 & RAM-60)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
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
                val encountersLabel = "${encounters.size} ${if (encounters.size == 1) "encounter" else "encounters"} recorded"
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

        Spacer(modifier = Modifier.height(10.dp))

        val today = LocalDate.now()
        if (birthday != null && BirthdayInputUtils.isUpcomingWithinOneMonth(birthday, today)) {
            UpcomingBirthdayCard(
                birthday = birthday,
                today = today
            )
        }

        // Future Horizon: Active & Overdue Reminders on a dashed spine above TODAY divider
        if (reminders.isNotEmpty()) {
            reminders.forEach { reminder ->
                FutureReminderNodeItem(
                    followUp = reminder,
                    today = today,
                    onClick = { onReminderClick(reminder) },
                    onDoneClick = { onMarkReminderDone(reminder.id) },
                    onSnoozeClick = { onSnoozeReminder(reminder.id) }
                )
            }

            TodayTimelineDivider(today = today)
        }

        if (encounters.isEmpty()) {
            EmptyTimelineCard(onLogEncounterClick = onLogEncounterClick)
        } else {
            encounters.forEachIndexed { index, encounter ->
                val isFirst = index == 0
                val isLast = index == encounters.lastIndex
                val isSelected = encounter.id in selectedEncounterIds
                TimelineNodeItem(
                    encounter = encounter,
                    place = encounter.placeId?.let { placesMap[it] },
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
                    }
                )
            }
        }
    }
}

@Composable
private fun TodayTimelineDivider(today: LocalDate) {
    val label = "TODAY, ${today.format(DateTimeFormatter.ofPattern("MMM d")).uppercase()}"
    Row(
        modifier = Modifier
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
    onClick: () -> Unit,
    onDoneClick: () -> Unit,
    onSnoozeClick: () -> Unit
) {
    val zoneId = ZoneId.systemDefault()
    val localDateTime = followUp.dueAt.atZone(zoneId)
    val dueDate = localDateTime.toLocalDate()
    val isOverdue = followUp.dueAt.isBefore(Instant.now())
    val timeLabel = localDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a"))
    val daysDiff = ChronoUnit.DAYS.between(today, dueDate)
    val dueBadgeText = when {
        isOverdue && daysDiff < 0L -> "Overdue (${-daysDiff}d ago)"
        isOverdue -> "Overdue, $timeLabel"
        daysDiff == 0L -> "Today, $timeLabel"
        daysDiff == 1L -> "Tomorrow, $timeLabel"
        daysDiff <= 7L -> "In $daysDiff days, $timeLabel"
        else -> "${dueDate.format(DateTimeFormatter.ofPattern("MMM d"))}, $timeLabel"
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
        modifier = Modifier
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
    today: LocalDate = LocalDate.now()
) {
    val nextBday = BirthdayInputUtils.nextAnnualBirthday(birthday, today)
    val daysUntil = ChronoUnit.DAYS.between(today, nextBday)
    val monthDayLabel = BirthdayInputUtils.formatAbbreviatedMonthDay(nextBday)
    val countdownLabel = BirthdayInputUtils.formatBirthdayCountdown(daysUntil)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineNodeItem(
    encounter: Encounter,
    place: Place?,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null
) {
    val dateText = formatTimelineDate(encounter.occurredAt)
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
        modifier = Modifier
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
private fun EmptyTimelineCard(onLogEncounterClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
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

private fun formatTimelineDate(instant: Instant): String {
    val date = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    val daysBetween = ChronoUnit.DAYS.between(date, today)

    return when {
        daysBetween == 0L -> "Met today"
        daysBetween == 1L -> "Met yesterday"
        daysBetween in 2..6 -> "$daysBetween days ago"
        date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}
