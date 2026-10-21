package com.example.hello.ui.home

import com.example.hello.domain.model.AccordionType
import com.example.hello.domain.model.CardType
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.SpeedDialAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        viewModel = HomeViewModel()
    }

    @Test
    fun initialState_hasExpectedDefaults() {
        val state = viewModel.uiState.value

        assertEquals("Alex", state.userName)
        assertEquals(CardType.WORK, state.activeCardType)
        assertTrue(state.myCards.containsKey(CardType.WORK))
        assertTrue(state.myCards.containsKey(CardType.SOCIAL))
        assertEquals(3, state.upcomingAlerts.size)
        assertEquals(4, state.recentContacts.size)
        assertFalse(state.isQrExpanded)
        assertFalse(state.isSpeedDialOpen)
        assertNull(state.expandedAccordion)
        assertEquals(HomeNavTab.HOME, state.selectedNavTab)
    }

    @Test
    fun onCardTypeSelect_switchesActiveCard() {
        viewModel.onEvent(HomeUiEvent.OnCardTypeSelect(CardType.SOCIAL))
        assertEquals(CardType.SOCIAL, viewModel.uiState.value.activeCardType)

        viewModel.onEvent(HomeUiEvent.OnCardTypeSelect(CardType.WORK))
        assertEquals(CardType.WORK, viewModel.uiState.value.activeCardType)
    }

    @Test
    fun onExpandQr_togglesModalVisibility() {
        viewModel.onEvent(HomeUiEvent.OnExpandQr(true))
        assertTrue(viewModel.uiState.value.isQrExpanded)

        viewModel.onEvent(HomeUiEvent.OnExpandQr(false))
        assertFalse(viewModel.uiState.value.isQrExpanded)
    }

    @Test
    fun onReminderDone_removesAlertAndSetsFeedback() {
        val alertId = viewModel.uiState.value.upcomingAlerts.first().id
        val initialCount = viewModel.uiState.value.upcomingAlerts.size

        viewModel.onEvent(HomeUiEvent.OnReminderDone(alertId))

        val state = viewModel.uiState.value
        assertEquals(initialCount - 1, state.upcomingAlerts.size)
        assertTrue(state.upcomingAlerts.none { it.id == alertId })
        assertNotNull(state.userMessage)
    }

    @Test
    fun onReminderSnooze_removesAlertAndSetsFeedback() {
        val alertId = viewModel.uiState.value.upcomingAlerts.last().id
        val initialCount = viewModel.uiState.value.upcomingAlerts.size

        viewModel.onEvent(HomeUiEvent.OnReminderSnooze(alertId))

        val state = viewModel.uiState.value
        assertEquals(initialCount - 1, state.upcomingAlerts.size)
        assertTrue(state.upcomingAlerts.none { it.id == alertId })
        assertTrue(state.userMessage?.contains("Snoozed") == true)
    }

    @Test
    fun onToggleAccordion_switchesAndCollapses() {
        viewModel.onEvent(HomeUiEvent.OnToggleAccordion(AccordionType.ADD_CONTACT))
        assertEquals(AccordionType.ADD_CONTACT, viewModel.uiState.value.expandedAccordion)

        // Switching to UPDATE_CONTACT
        viewModel.onEvent(HomeUiEvent.OnToggleAccordion(AccordionType.UPDATE_CONTACT))
        assertEquals(AccordionType.UPDATE_CONTACT, viewModel.uiState.value.expandedAccordion)

        // Tapping same accordion closes it
        viewModel.onEvent(HomeUiEvent.OnToggleAccordion(AccordionType.UPDATE_CONTACT))
        assertNull(viewModel.uiState.value.expandedAccordion)
    }

    @Test
    fun onQuickAddContact_prependsNewContactAndClosesAccordion() {
        val initialCount = viewModel.uiState.value.recentContacts.size

        viewModel.onEvent(HomeUiEvent.OnToggleAccordion(AccordionType.ADD_CONTACT))
        viewModel.onEvent(
            HomeUiEvent.OnQuickAddContact(
                name = "Taylor Swift",
                circles = setOf(Circle.WORK, Circle.FRIENDS),
                initialNote = "Met backstage at tour"
            )
        )

        val state = viewModel.uiState.value
        assertEquals(initialCount + 1, state.recentContacts.size)
        assertNull(state.expandedAccordion)

        val added = state.recentContacts.first()
        assertEquals("Taylor Swift", added.name)
        assertEquals("TS", added.avatarInitials)
        assertEquals(2, added.circles.size)
        assertEquals("Met backstage at tour", added.contextSubtitle)
    }

    @Test
    fun onQuickUpdateContact_updatesEncounterNote() {
        val target = viewModel.uiState.value.recentContacts.first()

        viewModel.onEvent(
            HomeUiEvent.OnQuickUpdateContact(
                contactId = target.id,
                note = "Had lunch together downtown"
            )
        )

        val updated = viewModel.uiState.value.recentContacts.first { it.id == target.id }
        assertEquals("Had lunch together downtown", updated.contextSubtitle)
        assertEquals("Updated just now", updated.lastEncounterText)
    }

    @Test
    fun onToggleSpeedDial_togglesState() {
        assertFalse(viewModel.uiState.value.isSpeedDialOpen)

        viewModel.onEvent(HomeUiEvent.OnToggleSpeedDial)
        assertTrue(viewModel.uiState.value.isSpeedDialOpen)

        viewModel.onEvent(HomeUiEvent.OnCloseSpeedDial)
        assertFalse(viewModel.uiState.value.isSpeedDialOpen)
    }

    @Test
    fun onSpeedDialAction_opensCorrespondingAccordionAndClosesSpeedDial() {
        viewModel.onEvent(HomeUiEvent.OnToggleSpeedDial)
        viewModel.onEvent(HomeUiEvent.OnSpeedDialAction(SpeedDialAction.NEW_CONTACT))

        assertFalse(viewModel.uiState.value.isSpeedDialOpen)
        assertEquals(AccordionType.ADD_CONTACT, viewModel.uiState.value.expandedAccordion)

        viewModel.onEvent(HomeUiEvent.OnSpeedDialAction(SpeedDialAction.LOG_ENCOUNTER))
        assertFalse(viewModel.uiState.value.isSpeedDialOpen)
        assertEquals(AccordionType.UPDATE_CONTACT, viewModel.uiState.value.expandedAccordion)
    }

    @Test
    fun onNavTabSelect_updatesSelectedTab() {
        viewModel.onEvent(HomeUiEvent.OnNavTabSelect(HomeNavTab.PEOPLE))
        assertEquals(HomeNavTab.PEOPLE, viewModel.uiState.value.selectedNavTab)

        viewModel.onEvent(HomeUiEvent.OnNavTabSelect(HomeNavTab.HOME))
        assertEquals(HomeNavTab.HOME, viewModel.uiState.value.selectedNavTab)
    }
}
