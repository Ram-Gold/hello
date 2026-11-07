package com.example.hello.ui.home

import com.example.hello.data.preferences.FakeUserPreferencesRepository
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.domain.model.SubconsciousCheckInCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = HomeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasExpectedDefaults() {
        val state = viewModel.uiState.value

        assertEquals("Alex", state.userName)
        assertEquals(3, state.upcomingAlerts.size)
        assertEquals(4, state.recentContacts.size)
        assertFalse(state.isSpeedDialOpen)
        assertNull(state.userMessage)
        assertEquals(HomeNavTab.HOME, state.selectedNavTab)
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
    fun onQuickAddContact_prependsNewContact() {
        val initialCount = viewModel.uiState.value.recentContacts.size

        viewModel.onEvent(
            HomeUiEvent.OnQuickAddContact(
                name = "Taylor Swift",
                initialNote = "Met backstage at tour"
            )
        )

        val state = viewModel.uiState.value
        assertEquals(initialCount + 1, state.recentContacts.size)

        val added = state.recentContacts.first()
        assertEquals("Taylor Swift", added.name)
        assertEquals("TS", added.avatarInitials)
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
    fun onSpeedDialAction_closesSpeedDialAndSetsUserMessage() {
        viewModel.onEvent(HomeUiEvent.OnToggleSpeedDial)
        viewModel.onEvent(HomeUiEvent.OnSpeedDialAction(SpeedDialAction.NEW_CONTACT))

        assertFalse(viewModel.uiState.value.isSpeedDialOpen)
        assertEquals("Quick Add Contact", viewModel.uiState.value.userMessage)
        assertEquals(HomeNavTab.HOME, viewModel.uiState.value.selectedNavTab)

        viewModel.onEvent(HomeUiEvent.OnSpeedDialAction(SpeedDialAction.LOG_ENCOUNTER))
        assertFalse(viewModel.uiState.value.isSpeedDialOpen)
        assertEquals("Log Encounter", viewModel.uiState.value.userMessage)
    }

    @Test
    fun onNavTabSelect_updatesSelectedTab() {
        viewModel.onEvent(HomeUiEvent.OnNavTabSelect(HomeNavTab.PEOPLE))
        assertEquals(HomeNavTab.PEOPLE, viewModel.uiState.value.selectedNavTab)

        viewModel.onEvent(HomeUiEvent.OnNavTabSelect(HomeNavTab.HOME))
        assertEquals(HomeNavTab.HOME, viewModel.uiState.value.selectedNavTab)
    }

    @Test
    fun userName_updatesReactivelyFromPreferencesRepository() = runTest {
        val fakePrefsRepo = FakeUserPreferencesRepository()
        val vm = HomeViewModel(preferencesRepository = fakePrefsRepo)

        advanceUntilIdle()
        assertEquals("Alex", vm.uiState.value.userName)

        fakePrefsRepo.setUserName("Taylor")
        advanceUntilIdle()
        assertEquals("Taylor", vm.uiState.value.userName)
    }

    @Test
    fun subconsciousCheckInCalculator_computesAdaptiveIntervalAndPrompt() {
        // 0-1 encounters -> 30-day baseline
        assertEquals(30, SubconsciousCheckInCalculator.computeAdaptiveThresholdDays(emptyList()))
        assertEquals(30, SubconsciousCheckInCalculator.computeAdaptiveThresholdDays(listOf(100L)))

        // 2 encounters 20 days apart -> 1.5 * 20 = 30 days
        assertEquals(30, SubconsciousCheckInCalculator.computeAdaptiveThresholdDays(listOf(100L, 120L)))

        // 3 encounters 4 days apart each -> avg gap 4d -> 1.5 * 4 = 6d -> clamped to min 14d
        assertEquals(14, SubconsciousCheckInCalculator.computeAdaptiveThresholdDays(listOf(100L, 104L, 108L)))

        // 2 encounters 80 days apart -> 1.5 * 80 = 120d -> clamped to max 90d
        assertEquals(90, SubconsciousCheckInCalculator.computeAdaptiveThresholdDays(listOf(100L, 180L)))

        val metPrompt = SubconsciousCheckInCalculator.formatSubconsciousPrompt(daysSince = 45, hasMultipleEncounters = true)
        assertEquals("Met 6 weeks ago — anything new to update?", metPrompt)

        val addedPrompt = SubconsciousCheckInCalculator.formatSubconsciousPrompt(daysSince = 35, hasMultipleEncounters = false)
        assertEquals("Added 5 weeks ago — anything new to update?", addedPrompt)
    }
}
