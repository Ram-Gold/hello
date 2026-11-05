package com.example.hello.ui.timeline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import com.example.hello.domain.model.Circle
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
    fun initialState_populatesFutureAndPastNodesWithDefaultAllFilter() {
        val viewModel = TimelineViewModel()
        val state = viewModel.uiState.value

        assertEquals(TimelineFilter.ALL, state.selectedFilter)
        assertTrue("Future nodes must not be empty", state.futureNodes.isNotEmpty())
        assertTrue("Past nodes must not be empty", state.pastNodes.isNotEmpty())
        assertTrue(state.todayLabel.startsWith("TODAY"))
        assertNull(state.userMessage)
    }

    @Test
    fun buildTimelineNodes_distinguishesFirstMetFromMetAgainPerContact() {
        val today = LocalDate.of(2026, 11, 5)
        val now = today.atStartOfDay(ZoneOffset.UTC).toInstant()

        val maya = Contact(
            id = "c-maya",
            name = "Maya Patel",
            circles = listOf(Circle.FRIENDS),
            firstMetAt = now.minus(10, ChronoUnit.DAYS),
            lastMetAt = now.minus(1, ChronoUnit.DAYS)
        )
        val alex = Contact(
            id = "c-alex",
            name = "Alex Rivera",
            circles = listOf(Circle.WORK),
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
            circles = listOf(Circle.FRIENDS),
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
    fun buildTimelineNodes_includesSetInStoneBirthdaysAcrossFullYearHorizon() {
        val today = LocalDate.of(2026, 11, 5)
        val now = today.atStartOfDay(ZoneOffset.UTC).toInstant()

        val summerFriend = Contact(
            id = "c-summer",
            name = "Leo Chen",
            circles = listOf(Circle.FRIENDS),
            birthday = LocalDate.of(1998, 7, 20), // ~257 days away (> 60 days)
            firstMetAt = now.minus(30, ChronoUnit.DAYS),
            lastMetAt = now.minus(10, ChronoUnit.DAYS)
        )

        val (futureNodes, _) = TimelineViewModel.buildTimelineNodes(
            contactsMap = mapOf(summerFriend.id to summerFriend),
            encounters = emptyList(),
            placesMap = emptyMap(),
            followUps = emptyList(),
            today = today,
            zoneId = ZoneOffset.UTC
        )

        assertEquals(1, futureNodes.size)
        val bdayNode = futureNodes.first() as FutureTimelineNode.BirthdayNode
        assertEquals("c-summer", bdayNode.contactId)
        assertTrue(bdayNode.daysUntil > 200L)
        assertTrue(bdayNode.dateLabel.contains("Jul 20"))
    }

    @Test
    fun onSelectFilter_and_inlineFollowUpActions_updateTimelineState() {
        val viewModel = TimelineViewModel()

        viewModel.onEvent(TimelineUiEvent.OnSelectFilter(TimelineFilter.FUTURE))
        assertEquals(TimelineFilter.FUTURE, viewModel.uiState.value.selectedFilter)

        viewModel.onEvent(TimelineUiEvent.OnSelectFilter(TimelineFilter.PAST))
        assertEquals(TimelineFilter.PAST, viewModel.uiState.value.selectedFilter)

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
}
