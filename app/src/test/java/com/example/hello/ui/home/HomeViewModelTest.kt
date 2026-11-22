package com.example.hello.ui.home

import com.example.hello.data.preferences.FakeUserPreferencesRepository
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.SerendipityPicker
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.domain.model.UpcomingAlert
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
    fun todayBirthdays_filtersOnlyAlertsForToday() {
        val state = viewModel.uiState.value.copy(
            upcomingAlerts = listOf(
                com.example.hello.domain.model.UpcomingAlert.BirthdayAlert(
                    id = "bday-1",
                    contactId = "c-1",
                    contactName = "Sarah",
                    dateText = "Today!"
                ),
                com.example.hello.domain.model.UpcomingAlert.BirthdayAlert(
                    id = "bday-2",
                    contactId = "c-2",
                    contactName = "Leo",
                    dateText = "Tomorrow"
                )
            )
        )
        assertEquals(1, state.todayBirthdays.size)
        assertEquals("Sarah", state.todayBirthdays.first().contactName)
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
    fun onReminderSnooze_forFollowUp_removesAlertAndSetsFeedback() {
        val followUpAlert = viewModel.uiState.value.upcomingAlerts
            .filterIsInstance<UpcomingAlert.FollowUpReminder>()
            .first()
        val initialCount = viewModel.uiState.value.upcomingAlerts.size

        viewModel.onEvent(HomeUiEvent.OnReminderSnooze(followUpAlert.id))

        val state = viewModel.uiState.value
        assertEquals(initialCount - 1, state.upcomingAlerts.size)
        assertTrue(state.upcomingAlerts.none { it.id == followUpAlert.id })
        assertTrue(state.userMessage?.contains("Snoozed") == true)
    }

    @Test
    fun onReminderSnooze_forSerendipityPrompt_skipsQuietlyWithoutToast() {
        val serendipityAlert = viewModel.uiState.value.upcomingAlerts
            .filterIsInstance<UpcomingAlert.SerendipityPrompt>()
            .first()
        val initialCount = viewModel.uiState.value.upcomingAlerts.size

        viewModel.onEvent(HomeUiEvent.OnReminderSnooze(serendipityAlert.id))

        val state = viewModel.uiState.value
        assertEquals(initialCount - 1, state.upcomingAlerts.size)
        assertTrue(state.upcomingAlerts.none { it.id == serendipityAlert.id })
        assertNull(state.userMessage)
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
    fun serendipityPicker_respects14DayFloorWeightingDeterminismAndWarmCopy() {
        val candidates = listOf(
            SerendipityPicker.WeightedCandidate(item = "recent", stableId = "c-recent", daysSinceLastMet = 5),
            SerendipityPicker.WeightedCandidate(item = "two-weeks", stableId = "c-14d", daysSinceLastMet = 14),
            SerendipityPicker.WeightedCandidate(item = "two-months", stableId = "c-60d", daysSinceLastMet = 60),
            SerendipityPicker.WeightedCandidate(item = "six-months", stableId = "c-180d", daysSinceLastMet = 180)
        )

        val day1First = SerendipityPicker.pickWeightedRandom(candidates, todayEpochDay = 20_500L, maxCount = 2)
        val day1Repeat = SerendipityPicker.pickWeightedRandom(candidates.reversed(), todayEpochDay = 20_500L, maxCount = 2)

        assertEquals(2, day1First.size)
        assertTrue("Contacts met <14 days ago must never be picked", day1First.none { it.item == "recent" })
        assertEquals("Daily pick must be deterministic regardless of input ordering", day1First.map { it.stableId }, day1Repeat.map { it.stableId })
        assertEquals("Picked contacts must be unique", day1First.map { it.stableId }.distinct().size, day1First.size)

        // Weight clamps cleanly at 14..365
        assertEquals(14.0, SerendipityPicker.candidateWeight(14), 0.001)
        assertEquals(365.0, SerendipityPicker.candidateWeight(900), 0.001)

        // Warm copy without task/overdue phrasing
        assertEquals("Last met 6 weeks ago", SerendipityPicker.formatSerendipityContext(daysSince = 45, hasMultipleEncounters = true))
        assertEquals("Added 5 weeks ago", SerendipityPicker.formatSerendipityContext(daysSince = 35, hasMultipleEncounters = false))
    }

    @Test
    fun favoriteContacts_collectedFromRepository() = runTest {
        val repo = com.example.hello.data.repository.ContactRepository()
        val vm = HomeViewModel(repository = repo)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.favoriteContacts.isEmpty())

        repo.toggleFavorite("c-1")
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.favoriteContacts.size)
        assertEquals("c-1", vm.uiState.value.favoriteContacts.first().id)
    }
}
