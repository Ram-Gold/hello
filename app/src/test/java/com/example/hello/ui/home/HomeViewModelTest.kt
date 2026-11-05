package com.example.hello.ui.home

import com.example.hello.data.preferences.FakeUserPreferencesRepository
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.CircleGroupConfig
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
        assertFalse(state.isDrawerOpen)
        assertNull(state.selectedCircle)
    }

    @Test
    fun onOpenDrawer_and_onCloseDrawer_updatesDrawerState() {
        assertFalse(viewModel.uiState.value.isDrawerOpen)

        viewModel.onEvent(HomeUiEvent.OnOpenDrawer)
        assertTrue(viewModel.uiState.value.isDrawerOpen)

        viewModel.onEvent(HomeUiEvent.OnCloseDrawer)
        assertFalse(viewModel.uiState.value.isDrawerOpen)
    }

    @Test
    fun onSelectCircle_updatesActiveCircle_and_closesDrawer() {
        assertNull(viewModel.uiState.value.selectedCircle)

        viewModel.onEvent(HomeUiEvent.OnOpenDrawer)
        assertTrue(viewModel.uiState.value.isDrawerOpen)

        viewModel.onEvent(HomeUiEvent.OnSelectCircle(Circle.WORK))
        assertEquals(Circle.WORK, viewModel.uiState.value.selectedCircle)
        assertFalse(viewModel.uiState.value.isDrawerOpen)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Work") == true)

        // Select All Circles (null)
        viewModel.onEvent(HomeUiEvent.OnSelectCircle(null))
        assertNull(viewModel.uiState.value.selectedCircle)
        assertFalse(viewModel.uiState.value.isDrawerOpen)
    }

    @Test
    fun onUpdateCircleConfig_persistsPerGroupConfigurationChanges() {
        val workConfig = viewModel.uiState.value.circleConfigs[Circle.WORK]
        assertNotNull(workConfig)
        assertEquals(7, workConfig?.cadenceDays)
        assertTrue(workConfig?.isCadenceAlertEnabled == true)

        val updatedConfig = workConfig!!.copy(
            cadenceDays = 21,
            isCadenceAlertEnabled = false
        )
        viewModel.onEvent(HomeUiEvent.OnUpdateCircleConfig(updatedConfig))

        val currentWorkConfig = viewModel.uiState.value.circleConfigs[Circle.WORK]
        assertEquals(21, currentWorkConfig?.cadenceDays)
        assertFalse(currentWorkConfig?.isCadenceAlertEnabled == true)
        assertTrue(viewModel.uiState.value.userMessage?.contains("Work configuration") == true)
    }

    @Test
    fun resolveEffectiveCadenceDays_arbitratesExplicitAndShortestCircleCadence() {
        val configs = mapOf(
            Circle.WORK to CircleGroupConfig(circle = Circle.WORK, cadenceDays = 7, isCadenceAlertEnabled = true),
            Circle.FRIENDS to CircleGroupConfig(circle = Circle.FRIENDS, cadenceDays = 30, isCadenceAlertEnabled = true),
            Circle.UNI to CircleGroupConfig(circle = Circle.UNI, cadenceDays = 3, isCadenceAlertEnabled = false)
        )

        // 1. Explicit per-contact cadence wins over circle cadences
        val explicitResult = CircleGroupConfig.resolveEffectiveCadenceDays(
            circles = listOf(Circle.WORK, Circle.FRIENDS),
            explicitCadenceDays = 14,
            circleConfigs = configs
        )
        assertEquals(14, explicitResult)

        // 2. When no explicit cadence is set, shortest enabled circle cadence wins (Work 7d < Friends 30d)
        val shortestCircleResult = CircleGroupConfig.resolveEffectiveCadenceDays(
            circles = listOf(Circle.WORK, Circle.FRIENDS),
            explicitCadenceDays = null,
            circleConfigs = configs
        )
        assertEquals(7, shortestCircleResult)

        // 3. Disabled circle alerts are ignored (Uni @ 3d disabled, Friends @ 30d enabled -> 30d)
        val disabledIgnoredResult = CircleGroupConfig.resolveEffectiveCadenceDays(
            circles = listOf(Circle.UNI, Circle.FRIENDS),
            explicitCadenceDays = null,
            circleConfigs = configs
        )
        assertEquals(30, disabledIgnoredResult)

        // 4. Empty circles or no enabled circles fall back to defaultCadenceDays
        val fallbackResult = CircleGroupConfig.resolveEffectiveCadenceDays(
            circles = emptyList(),
            explicitCadenceDays = null,
            circleConfigs = configs,
            defaultCadenceDays = 21
        )
        assertEquals(21, fallbackResult)
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
                circles = setOf(Circle.WORK, Circle.FRIENDS),
                initialNote = "Met backstage at tour"
            )
        )

        val state = viewModel.uiState.value
        assertEquals(initialCount + 1, state.recentContacts.size)

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
