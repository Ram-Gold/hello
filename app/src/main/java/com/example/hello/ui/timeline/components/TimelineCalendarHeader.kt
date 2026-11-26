package com.example.hello.ui.timeline.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hello.R
import com.example.hello.data.preferences.WeekStartDay
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.timeline.TimelineViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

internal object TimelineCalendarStripDefaults {
    const val CENTER_PAGE = 5000

    fun weekStartOf(date: LocalDate, firstDayOfWeek: WeekStartDay): LocalDate {
        val targetStart = firstDayOfWeek.dayOfWeek
        val diff = (date.dayOfWeek.value - targetStart.value + 7) % 7
        return date.minusDays(diff.toLong())
    }

    fun pageForDate(
        date: LocalDate,
        today: LocalDate,
        firstDayOfWeek: WeekStartDay
    ): Int {
        val todayWeekStart = weekStartOf(today, firstDayOfWeek)
        val dateWeekStart = weekStartOf(date, firstDayOfWeek)
        val weeksDiff = ChronoUnit.WEEKS.between(todayWeekStart, dateWeekStart).toInt()
        return (CENTER_PAGE + weeksDiff).coerceIn(0, CENTER_PAGE * 2)
    }

    fun weekStartForPage(
        page: Int,
        today: LocalDate,
        firstDayOfWeek: WeekStartDay
    ): LocalDate {
        val todayWeekStart = weekStartOf(today, firstDayOfWeek)
        return todayWeekStart.plusWeeks((page - CENTER_PAGE).toLong())
    }
}

@Composable
fun TimelineCalendarHeader(
    selectedDate: LocalDate,
    today: LocalDate,
    firstDayOfWeek: WeekStartDay,
    activeEntryDates: Set<LocalDate>,
    recurringBirthdayMonthDays: Set<MonthDay>,
    onSelectDate: (LocalDate) -> Unit,
    onStepDay: (Long) -> Unit,
    onStepWeek: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHelloHaptics.current
    val isReducedMotion = LocalReducedMotion.current
    val targetPage = remember(selectedDate, today, firstDayOfWeek) {
        TimelineCalendarStripDefaults.pageForDate(selectedDate, today, firstDayOfWeek)
    }
    val pagerState = rememberPagerState(
        initialPage = targetPage,
        pageCount = { TimelineCalendarStripDefaults.CENTER_PAGE * 2 + 1 }
    )
    var lastProgrammaticPage by remember { mutableIntStateOf(targetPage) }
    val latestOnStepWeek = rememberUpdatedState(onStepWeek)

    // Sync pager when selectedDate moves across week boundaries via tap, arrows, scroll, or picker
    LaunchedEffect(targetPage, isReducedMotion) {
        if (pagerState.currentPage != targetPage) {
            lastProgrammaticPage = targetPage
            if (isReducedMotion) {
                pagerState.scrollToPage(targetPage)
            } else {
                pagerState.animateScrollToPage(targetPage)
            }
        } else {
            lastProgrammaticPage = targetPage
        }
    }

    // Sync selectedDate (same weekday) when user swipes the week strip horizontally
    LaunchedEffect(pagerState.settledPage) {
        val settled = pagerState.settledPage
        if (settled != lastProgrammaticPage && settled != targetPage) {
            val deltaWeeks = (settled - targetPage).toLong()
            lastProgrammaticPage = settled
            if (deltaWeeks != 0L) {
                haptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
                latestOnStepWeek.value(deltaWeeks)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Notion Calendar Minimalist Header — Short Date ("Thu, Nov 25 ▾") + Today pill + Calendar button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            haptics.perform(SemanticHapticType.MODE_TOGGLE)
                            onOpenDatePicker()
                        }
                        .padding(end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = TimelineViewModel.formatCalendarHeaderTitle(selectedDate, today),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Open calendar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = selectedDate != today,
                        enter = if (isReducedMotion) fadeIn(snap()) else fadeIn(tween(150)) + scaleIn(initialScale = 0.92f),
                        exit = if (isReducedMotion) fadeOut(snap()) else fadeOut(tween(120)) + scaleOut(targetScale = 0.92f)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                    onJumpToToday()
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable {
                                haptics.perform(SemanticHapticType.MODE_TOGGLE)
                                onOpenDatePicker()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_feather_calendar),
                            contentDescription = "Open calendar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Row 2: Swipeable 7-Day Week Strip
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val weekStart = remember(page, today, firstDayOfWeek) {
                    TimelineCalendarStripDefaults.weekStartForPage(page, today, firstDayOfWeek)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (offset in 0..6) {
                        val day = weekStart.plusDays(offset.toLong())
                        val isSelected = day == selectedDate
                        val isToday = day == today
                        val hasEntry = remember(day, activeEntryDates, recurringBirthdayMonthDays) {
                            TimelineViewModel.hasTimelineEntryOnDate(
                                date = day,
                                activeEntryDates = activeEntryDates,
                                recurringBirthdays = recurringBirthdayMonthDays
                            )
                        }

                        WeekStripDayCell(
                            day = day,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasEntry = hasEntry,
                            isReducedMotion = isReducedMotion,
                            onClick = {
                                haptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
                                onSelectDate(day)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Ambient Depth Scrim (GLOSSARY.md)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun WeekStripDayCell(
    day: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasEntry: Boolean,
    isReducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = if (isReducedMotion) snap() else tween(160),
        label = "DayCellContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = if (isReducedMotion) snap() else tween(160),
        label = "DayCellContentColor"
    )

    val dayLetter = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
    val accessibilityLabel = buildString {
        append(day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()))
        append(", ")
        append(day.month.getDisplayName(TextStyle.FULL, Locale.getDefault()))
        append(" ")
        append(day.dayOfMonth)
        if (isToday) append(", Today")
        if (hasEntry) append(", has entries")
    }

    val todayRingModifier = if (isToday && !isSelected) {
        Modifier.border(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape
        )
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .semantics {
                selected = isSelected
                contentDescription = accessibilityLabel
            }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = dayLetter,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(containerColor)
                .then(todayRingModifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
            )
        }

        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    when {
                        hasEntry && isSelected -> MaterialTheme.colorScheme.primary
                        hasEntry -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        else -> Color.Transparent
                    }
                )
        )
    }
}
