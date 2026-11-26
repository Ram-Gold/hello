package com.example.hello.ui.timeline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import com.example.hello.domain.model.Contact
import com.example.hello.domain.model.Encounter
import com.example.hello.domain.model.FollowUp
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.Place
import com.example.hello.ui.home.components.DockMotionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class TimelineViewModelTest {

    @Test
    fun initialState_populatesFutureAndPastNodesAnchoredAtToday() {
        val viewModel = TimelineViewModel()
        val state = viewModel.uiState.value

        assertEquals(state.today, state.selectedDate)
        assertTrue("Future nodes must not be empty", state.futureNodes.isNotEmpty())
        assertTrue("Past nodes must not be empty", state.pastNodes.isNotEmpty())
        assertTrue(state.todayLabel.startsWith("TODAY"))
        assertTrue(state.activeEntryDates.isNotEmpty())
        assertNull(state.userMessage)
    }

    @Test
    fun buildTimelineNodes_distinguishesFirstMetFromMetAgainPerContact() {
        val today = LocalDate.of(2026, 11, 5)
        val now = today.atStartOfDay(ZoneOffset.UTC).toInstant()

        val maya = Contact(
            id = "c-maya",
            name = "Maya Patel",
            firstMetAt = now.minus(10, ChronoUnit.DAYS),
            lastMetAt = now.minus(1, ChronoUnit.DAYS)
        )
        val alex = Contact(
            id = "c-alex",
            name = "Alex Rivera",
            firstMetAt = now.minus(2, ChronoUnit.DAYS),
            lastMetAt = now.minus(2, ChronoUnit.DAYS)
        )

        val encounters = listOf(
            // Maya's earliest encounter -> should be FIRST_MET
            Encounter(
                id = "enc-maya-1",
                contactId = "c-maya",
                occurredAt = now.minus(10, ChronoUnit.DAYS),
                placeId = "p-1",
                notes = "Met at the campus hackathon"
            ),
            // Maya's second encounter -> should be MET_AGAIN
            Encounter(
                id = "enc-maya-2",
                contactId = "c-maya",
                occurredAt = now.minus(1, ChronoUnit.DAYS),
                placeId = "p-1",
                notes = "Caught up over coffee about design systems"
            ),
            // Alex's only encounter -> should be FIRST_MET
            Encounter(
                id = "enc-alex-1",
                contactId = "c-alex",
                occurredAt = now.minus(2, ChronoUnit.DAYS),
                placeId = null,
                notes = "Intro call"
            )
        )

        val places = mapOf(
            "p-1" to Place(id = "p-1", label = "Blue Bottle Coffee")
        )

        val (_, pastNodes) = TimelineViewModel.buildTimelineNodes(
            contactsMap = mapOf(maya.id to maya, alex.id to alex),
            encounters = encounters,
            placesMap = places,
            followUps = emptyList(),
            today = today,
            zoneId = ZoneOffset.UTC
        )

        // Past nodes must be ordered newest-first (enc-maya-2 -> enc-alex-1 -> enc-maya-1)
        assertEquals(3, pastNodes.size)
        assertEquals("enc-maya-2", pastNodes[0].id)
        assertEquals(PastEncounterBadge.MET_AGAIN, pastNodes[0].badge)
        assertEquals("Blue Bottle Coffee", pastNodes[0].placeLabel)

        assertEquals("enc-alex-1", pastNodes[1].id)
        assertEquals(PastEncounterBadge.FIRST_MET, pastNodes[1].badge)

        assertEquals("enc-maya-1", pastNodes[2].id)
        assertEquals(PastEncounterBadge.FIRST_MET, pastNodes[2].badge)
    }

    @Test
    fun buildTimelineNodes_ordersFutureNodesSoonestFirstAcrossFollowUpsAndBirthdays() {
        val today = LocalDate.of(2026, 11, 5)
        val now = today.atStartOfDay(ZoneOffset.UTC).toInstant()

        val maya = Contact(
            id = "c-maya",
            name = "Maya Patel",
            birthday = LocalDate.of(2000, 11, 6), // Tomorrow (1 day)
            lastMetAt = now.minus(4, ChronoUnit.DAYS),
            firstMetAt = now.minus(20, ChronoUnit.DAYS)
        )

        val followUps = listOf(
            FollowUp(
                id = "fu-1",
                contactId = "c-maya",
                dueAt = now.plus(3, ChronoUnit.DAYS), // In 3 days
                note = "Send hackathon photos",
                doneAt = null
            )
        )

        val (futureNodes, _) = TimelineViewModel.buildTimelineNodes(
            contactsMap = mapOf(maya.id to maya),
            encounters = emptyList(),
            placesMap = emptyMap(),
            followUps = followUps,
            today = today,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(2, futureNodes.size)
        // 1. Tomorrow (1 day): BirthdayNode
        assertTrue(futureNodes[0] is FutureTimelineNode.BirthdayNode)
        assertEquals(1L, futureNodes[0].daysUntil)
        assertEquals("Tomorrow", futureNodes[0].dateLabel)
        assertEquals(26, (futureNodes[0] as FutureTimelineNode.BirthdayNode).turningAge)

        // 2. In 3 days (3 days): FollowUpNode
        assertTrue(futureNodes[1] is FutureTimelineNode.FollowUpNode)
        assertEquals(3L, futureNodes[1].daysUntil)
        assertEquals("In 3 days", futureNodes[1].dateLabel)
    }

    @Test
    fun buildTimelineNodes_includesBirthdaysWithinOneYearAndBeyondWhenNavigated() {
        val today = LocalDate.of(2026, 11, 5)
        val now = today.atStartOfDay(ZoneOffset.UTC).toInstant()

        val upcomingExactBoundary = Contact(
            id = "c-one-month",
            name = "Maya Lin",
            birthday = LocalDate.of(1998, 12, 5),
            firstMetAt = now.minus(30, ChronoUnit.DAYS),
            lastMetAt = now.minus(10, ChronoUnit.DAYS)
        )
        val summerFriend = Contact(
            id = "c-summer",
            name = "Leo Chen",
            birthday = LocalDate.of(1998, 7, 20), // Next annual occurrence is 2027-07-20 (within 12 months)
            firstMetAt = now.minus(30, ChronoUnit.DAYS),
            lastMetAt = now.minus(10, ChronoUnit.DAYS)
        )

        val (futureNodes, _) = TimelineViewModel.buildTimelineNodes(
            contactsMap = mapOf(
                upcomingExactBoundary.id to upcomingExactBoundary,
                summerFriend.id to summerFriend
            ),
            encounters = emptyList(),
            placesMap = emptyMap(),
            followUps = emptyList(),
            today = today,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(2, futureNodes.size)
        val decNode = futureNodes.first { it.contactId == "c-one-month" } as FutureTimelineNode.BirthdayNode
        assertEquals(30L, decNode.daysUntil)
        assertEquals("In 30 days", decNode.dateLabel)

        val julyNode = futureNodes.first { it.contactId == "c-summer" } as FutureTimelineNode.BirthdayNode
        assertEquals(LocalDate.of(2027, 7, 20), julyNode.targetDate)
    }

    @Test
    fun buildTimelineNodes_birthdayToday_anchorsAsTodayMilestone() {
        val today = LocalDate.of(2026, 11, 8)
        val now = today.atStartOfDay(ZoneOffset.UTC).toInstant()

        val birthdayFriend = Contact(
            id = "c-today",
            name = "Jordan Lee",
            birthday = LocalDate.of(1996, 11, 8),
            firstMetAt = now.minus(100, ChronoUnit.DAYS),
            lastMetAt = now.minus(20, ChronoUnit.DAYS)
        )

        val (futureNodes, _) = TimelineViewModel.buildTimelineNodes(
            contactsMap = mapOf(birthdayFriend.id to birthdayFriend),
            encounters = emptyList(),
            placesMap = emptyMap(),
            followUps = emptyList(),
            today = today,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(1, futureNodes.size)
        val bdayNode = futureNodes.first() as FutureTimelineNode.BirthdayNode
        assertEquals(0L, bdayNode.daysUntil)
        assertEquals("Today!", bdayNode.dateLabel)
        assertEquals(30, bdayNode.turningAge)
    }

    @Test
    fun calendarHeaderEvents_stepDayWeekJumpTodayAndSyncScrollRequests() {
        val viewModel = TimelineViewModel()
        val initialToday = viewModel.uiState.value.today
        assertEquals(initialToday, viewModel.uiState.value.selectedDate)
        assertEquals(0L, viewModel.uiState.value.scrollRequestId)

        // Step forward 1 day -> increments scrollRequestId
        viewModel.onEvent(TimelineUiEvent.OnStepDay(1L))
        assertEquals(initialToday.plusDays(1), viewModel.uiState.value.selectedDate)
        assertEquals(1L, viewModel.uiState.value.scrollRequestId)

        // Step backward 1 week -> keeps weekday, increments scrollRequestId
        viewModel.onEvent(TimelineUiEvent.OnStepWeek(-1L))
        assertEquals(initialToday.plusDays(1).minusWeeks(1), viewModel.uiState.value.selectedDate)
        assertEquals(2L, viewModel.uiState.value.scrollRequestId)

        // Passive scroll sync updates selectedDate without incrementing scrollRequestId
        val scrolledDate = initialToday.minusDays(3)
        viewModel.onEvent(TimelineUiEvent.OnSelectDate(scrolledDate, fromScroll = true))
        assertEquals(scrolledDate, viewModel.uiState.value.selectedDate)
        assertEquals(2L, viewModel.uiState.value.scrollRequestId)

        // Jump to Today resets selectedDate and triggers scroll
        viewModel.onEvent(TimelineUiEvent.OnJumpToToday)
        assertEquals(initialToday, viewModel.uiState.value.selectedDate)
        assertEquals(3L, viewModel.uiState.value.scrollRequestId)

        // Date picker open / dismiss
        viewModel.onEvent(TimelineUiEvent.OnOpenDatePicker)
        assertTrue(viewModel.uiState.value.isDatePickerOpen)
        viewModel.onEvent(TimelineUiEvent.OnDismissDatePicker)
        assertFalse(viewModel.uiState.value.isDatePickerOpen)
    }

    @Test
    fun inlineFollowUpActions_updateTimelineState() {
        val viewModel = TimelineViewModel()

        val followUpNode = viewModel.uiState.value.futureNodes
            .filterIsInstance<FutureTimelineNode.FollowUpNode>()
            .firstOrNull()
        assertNotNull(followUpNode)

        // Snooze shifts due date by +3 days
        val initialDays = followUpNode!!.daysUntil
        viewModel.onEvent(TimelineUiEvent.OnSnoozeFollowUp(followUpNode.followUpId))
        val snoozedNode = viewModel.uiState.value.futureNodes
            .filterIsInstance<FutureTimelineNode.FollowUpNode>()
            .first { it.followUpId == followUpNode.followUpId }
        assertEquals((initialDays + 3).coerceAtLeast(3), snoozedNode.daysUntil)
        assertNotNull(viewModel.uiState.value.userMessage)

        // Mark done removes the follow-up node from Future Horizon
        viewModel.onEvent(TimelineUiEvent.OnMarkFollowUpDone(followUpNode.followUpId))
        assertFalse(
            viewModel.uiState.value.futureNodes.any {
                it is FutureTimelineNode.FollowUpNode && it.followUpId == followUpNode.followUpId
            }
        )
    }

    @Test
    fun threeTabDockGeometry_interpolatesCleanlyAcrossHomeTimelineAndPeople() {
        val dims = DockMotionConfig.resolveThreeTabDockDimensions("People")
        val activeW = dims.activePillWidthDp.toFloat()
        val inactiveW = dims.inactiveSlotWidthDp.toFloat()
        val expectedSum = activeW + 2f * inactiveW

        // Constant pill width and invariant slot width sum across all key frames and mid-transition frames
        listOf(0f, 0.35f, 1f, 1.65f, 2f).forEach { progress ->
            val (left, right) = DockMotionConfig.calculateConstantWidthThreeTabPillBounds(
                activePillWidth = activeW,
                inactiveSlotWidth = inactiveW,
                progress = progress
            )
            assertEquals("Pill width must remain constant at progress=$progress", activeW, right - left, 0.001f)
            assertEquals(progress * inactiveW, left, 0.001f)

            val (s0, s1, s2) = DockMotionConfig.calculateThreeTabSlotWidths(
                activePillWidth = activeW,
                inactiveSlotWidth = inactiveW,
                progress = progress
            )
            assertEquals("Slot widths must sum to total inner dock width at progress=$progress", expectedSum, s0 + s1 + s2, 0.001f)
        }

        // Verify exact slot widths at stationary tab stops
        val (h0, h1, h2) = DockMotionConfig.calculateThreeTabSlotWidths(activeW, inactiveW, 0f)
        assertEquals(activeW, h0, 0.001f)
        assertEquals(inactiveW, h1, 0.001f)
        assertEquals(inactiveW, h2, 0.001f)

        val (t0, t1, t2) = DockMotionConfig.calculateThreeTabSlotWidths(activeW, inactiveW, 1f)
        assertEquals(inactiveW, t0, 0.001f)
        assertEquals(activeW, t1, 0.001f)
        assertEquals(inactiveW, t2, 0.001f)

        val (p0, p1, p2) = DockMotionConfig.calculateThreeTabSlotWidths(activeW, inactiveW, 2f)
        assertEquals(inactiveW, p0, 0.001f)
        assertEquals(inactiveW, p1, 0.001f)
        assertEquals(activeW, p2, 0.001f)
    }

    @Test
    fun resolveTabIcon_switchesBetweenOutlinedAndFilledVectorsPerSelectionState() {
        // Inactive tabs use Outlined icons
        assertEquals(Icons.Outlined.Home, DockMotionConfig.resolveTabIcon(HomeNavTab.HOME, isSelected = false))
        assertEquals(Icons.Outlined.People, DockMotionConfig.resolveTabIcon(HomeNavTab.PEOPLE, isSelected = false))
        assertEquals(Icons.Outlined.CalendarMonth, DockMotionConfig.resolveTabIcon(HomeNavTab.TIMELINE, isSelected = false))

        // Active tabs use Filled icons
        assertEquals(Icons.Filled.Home, DockMotionConfig.resolveTabIcon(HomeNavTab.HOME, isSelected = true))
        assertEquals(Icons.Filled.People, DockMotionConfig.resolveTabIcon(HomeNavTab.PEOPLE, isSelected = true))
        assertEquals(Icons.Filled.CalendarMonth, DockMotionConfig.resolveTabIcon(HomeNavTab.TIMELINE, isSelected = true))
    }

    @Test
    fun onNodeLongClick_entersSelectionModeAndSelectsEncounter() {
        val viewModel = TimelineViewModel()
        assertFalse(viewModel.uiState.value.isSelectionMode)

        val pastNode = viewModel.uiState.value.pastNodes.first()
        viewModel.onEvent(TimelineUiEvent.OnNodeLongClick(pastNode.id))

        val state = viewModel.uiState.value
        assertTrue(state.isSelectionMode)
        assertTrue(state.selectedNodeIds.contains(pastNode.id))
        assertEquals(1, state.selectedNodeIds.size)
    }

    @Test
    fun onNodeLongClick_birthdayNode_isNonSelectableNoOp() {
        val viewModel = TimelineViewModel()
        // Birthday nodes (e.g. future-bday-xyz) are not selectable
        viewModel.onEvent(TimelineUiEvent.OnNodeLongClick("future-bday-test-contact"))

        val state = viewModel.uiState.value
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedNodeIds.isEmpty())
    }

    @Test
    fun onToggleSelectNode_togglesSelectionState_andAutoExitsWhenEmpty() {
        val viewModel = TimelineViewModel()
        val pastNodes = viewModel.uiState.value.pastNodes
        val node1 = pastNodes[0]
        val node2 = pastNodes[1]

        // Select node1 via long click
        viewModel.onEvent(TimelineUiEvent.OnNodeLongClick(node1.id))
        assertTrue(viewModel.uiState.value.isSelectionMode)

        // Toggle node2 on
        viewModel.onEvent(TimelineUiEvent.OnToggleSelectNode(node2.id))
        assertEquals(2, viewModel.uiState.value.selectedNodeIds.size)
        assertTrue(viewModel.uiState.value.selectedNodeIds.contains(node1.id))
        assertTrue(viewModel.uiState.value.selectedNodeIds.contains(node2.id))

        // Toggle node2 off
        viewModel.onEvent(TimelineUiEvent.OnToggleSelectNode(node2.id))
        assertEquals(1, viewModel.uiState.value.selectedNodeIds.size)
        assertTrue(viewModel.uiState.value.selectedNodeIds.contains(node1.id))
        assertTrue(viewModel.uiState.value.isSelectionMode)

        // Toggle node1 off -> auto-exits selection mode
        viewModel.onEvent(TimelineUiEvent.OnToggleSelectNode(node1.id))
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedNodeIds.isEmpty())
    }

    @Test
    fun onClearSelection_clearsAllSelectionsAndExitsSelectionMode() {
        val viewModel = TimelineViewModel()
        val node = viewModel.uiState.value.pastNodes.first()

        viewModel.onEvent(TimelineUiEvent.OnNodeLongClick(node.id))
        assertTrue(viewModel.uiState.value.isSelectionMode)

        viewModel.onEvent(TimelineUiEvent.OnClearSelection)
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedNodeIds.isEmpty())
        assertFalse(viewModel.uiState.value.isDeleteConfirmationVisible)
    }

    @Test
    fun onSelectAllVisibleNodes_selectsAllVisibleEncountersAndFollowUps_excludingBirthdays() {
        val viewModel = TimelineViewModel()
        val followUps = viewModel.uiState.value.futureNodes.filterIsInstance<FutureTimelineNode.FollowUpNode>()
        val past = viewModel.uiState.value.pastNodes

        assertTrue(followUps.isNotEmpty())
        assertTrue(past.isNotEmpty())

        viewModel.onEvent(TimelineUiEvent.OnSelectAllVisibleNodes)
        val state = viewModel.uiState.value
        assertTrue(state.isSelectionMode)

        // All follow-ups and encounters selected
        followUps.forEach { fu ->
            assertTrue(state.selectedNodeIds.contains(fu.id))
        }
        past.forEach { p ->
            assertTrue(state.selectedNodeIds.contains(p.id))
        }

        // None of the selected IDs can be birthday nodes
        state.selectedNodeIds.forEach { id ->
            assertFalse(id.startsWith("future-bday-"))
        }
    }

    @Test
    fun onConfirmDeleteSelected_deletesEncountersAndFollowUps_reconcilesStateAndClearsSelection() {
        val viewModel = TimelineViewModel()
        val encounterNode = viewModel.uiState.value.pastNodes.first { !it.isCompletedReminder }
        val followUpNode = viewModel.uiState.value.futureNodes
            .filterIsInstance<FutureTimelineNode.FollowUpNode>()
            .first()

        viewModel.onEvent(TimelineUiEvent.OnNodeLongClick(encounterNode.id))
        viewModel.onEvent(TimelineUiEvent.OnToggleSelectNode(followUpNode.id))
        assertEquals(2, viewModel.uiState.value.selectedNodeIds.size)

        // Request delete
        viewModel.onEvent(TimelineUiEvent.OnRequestDeleteSelected)
        assertTrue(viewModel.uiState.value.isDeleteConfirmationVisible)

        // Confirm delete
        viewModel.onEvent(TimelineUiEvent.OnConfirmDeleteSelected)
        val state = viewModel.uiState.value

        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedNodeIds.isEmpty())
        assertFalse(state.isDeleteConfirmationVisible)
        assertNotNull(state.userMessage)
        assertTrue(state.userMessage!!.contains("Deleted 1 encounter and 1 follow-up"))

        // Nodes must be gone from the state
        assertFalse(state.pastNodes.any { it.id == encounterNode.id })
        assertFalse(state.futureNodes.any { it.id == followUpNode.id })
    }

    @Test
    fun buildTimelineNodes_consolidatesMultiContactEncountersAndFollowUpsWithParticipants() {
        val today = LocalDate.of(2026, 11, 23)
        val alice = Contact(id = "c-a", name = "Alice")
        val bob = Contact(id = "c-b", name = "Bob")
        val carol = Contact(id = "c-c", name = "Carol")
        val contactsMap = mapOf(alice.id to alice, bob.id to bob, carol.id to carol)

        val sharedEnc = Encounter(
            id = "enc-shared",
            contactId = alice.id,
            participantIds = listOf(alice.id, bob.id, carol.id),
            occurredAt = today.atStartOfDay(ZoneOffset.UTC).toInstant(),
            notes = "Team lunch"
        )
        val sharedFollowUp = FollowUp(
            id = "fu-shared",
            contactId = alice.id,
            participantIds = listOf(alice.id, bob.id),
            dueAt = today.plusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant(),
            note = "Send photos"
        )

        val (future, past) = TimelineViewModel.buildTimelineNodes(
            contactsMap = contactsMap,
            encounters = listOf(sharedEnc),
            placesMap = emptyMap(),
            followUps = listOf(sharedFollowUp),
            today = today,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(1, past.size)
        assertEquals("Alice, Bob & Carol", past.first().contactName)
        assertEquals(3, past.first().participants.size)

        val followUpNodes = future.filterIsInstance<FutureTimelineNode.FollowUpNode>()
        assertEquals(1, followUpNodes.size)
        assertEquals("Alice & Bob", followUpNodes.first().contactName)
        assertEquals(2, followUpNodes.first().participants.size)
    }

    @Test
    fun buildTimelineNodes_projectsCompletedRemindersByDoneAtAndHidesArchivedContacts() {
        val today = LocalDate.of(2026, 11, 24)
        val zone = ZoneOffset.UTC
        val active = Contact(id = "c-active", name = "Alice", firstMetAt = Instant.parse("2026-11-01T10:00:00Z"))
        val archived = Contact(id = "c-archived", name = "Bob", isArchived = true, firstMetAt = Instant.parse("2026-11-01T10:00:00Z"))
        val contactsMap = mapOf(active.id to active, archived.id to archived)

        val earlierEncounter = Encounter(
            id = "enc-earlier",
            contactId = active.id,
            occurredAt = today.atTime(9, 0).toInstant(zone),
            notes = "Morning coffee"
        )
        val laterEncounter = Encounter(
            id = "enc-later",
            contactId = active.id,
            occurredAt = today.atTime(15, 0).toInstant(zone),
            notes = "Afternoon walk"
        )
        val completedActive = FollowUp(
            id = "fu-done-active",
            contactId = active.id,
            dueAt = today.plusDays(5).atStartOfDay(zone).toInstant(),
            note = "Send architecture doc",
            doneAt = today.atTime(12, 0).toInstant(zone)
        )
        val completedArchived = FollowUp(
            id = "fu-done-archived",
            contactId = archived.id,
            dueAt = today.minusDays(2).atStartOfDay(zone).toInstant(),
            note = "Archived task",
            doneAt = today.atTime(13, 0).toInstant(zone)
        )

        val (future, past) = TimelineViewModel.buildTimelineNodes(
            contactsMap = contactsMap,
            encounters = listOf(earlierEncounter, laterEncounter),
            placesMap = emptyMap(),
            followUps = listOf(completedActive, completedArchived),
            today = today,
            zoneId = zone
        )

        assertTrue("Completed reminders should not remain in futureNodes", future.isEmpty())
        assertEquals(
            "Completed reminder for active contact should interleave chronologically by doneAt; archived should be hidden",
            listOf("enc-later", "completed-followup-fu-done-active", "enc-earlier"),
            past.map { it.id }
        )
        val completedNode = past[1]
        assertTrue(completedNode.isCompletedReminder)
        assertEquals(PastEncounterBadge.COMPLETED_REMINDER, completedNode.badge)
        assertEquals("fu-done-active", completedNode.followUpId)
        assertEquals(completedActive.doneAt, completedNode.occurredAt)
    }

    @Test
    fun onMarkFollowUpDone_immediatelyShowsCompletedReminderInTimelineAndSupportsDelete() {
        val viewModel = TimelineViewModel()
        val targetFollowUp = viewModel.uiState.value.futureNodes
            .filterIsInstance<FutureTimelineNode.FollowUpNode>()
            .first()

        viewModel.onEvent(TimelineUiEvent.OnMarkFollowUpDone(targetFollowUp.followUpId))

        val afterDone = viewModel.uiState.value
        assertTrue(
            "Completed reminder must be removed from futureNodes immediately",
            afterDone.futureNodes.filterIsInstance<FutureTimelineNode.FollowUpNode>()
                .none { it.followUpId == targetFollowUp.followUpId }
        )
        val completedNode = afterDone.pastNodes.firstOrNull { it.followUpId == targetFollowUp.followUpId }
        assertNotNull("Completed reminder must appear in pastNodes immediately", completedNode)
        assertEquals(PastEncounterBadge.COMPLETED_REMINDER, completedNode?.badge)
        assertTrue("Today must be marked active after completion", afterDone.today in afterDone.activeEntryDates)
        assertEquals("Reminder completed", afterDone.userMessage)

        // Multi-select delete removes completed reminder node
        viewModel.onEvent(TimelineUiEvent.OnNodeLongClick(completedNode!!.id))
        assertTrue(completedNode.id in viewModel.uiState.value.selectedNodeIds)
        viewModel.onEvent(TimelineUiEvent.OnConfirmDeleteSelected)
        assertTrue(viewModel.uiState.value.pastNodes.none { it.id == completedNode.id })
    }
}
