package com.example.hello.ui.onboarding

import com.example.hello.data.preferences.FakeUserPreferencesRepository
import com.example.hello.data.preferences.ThemeMode
import com.example.hello.data.preferences.UserPreferences
import com.example.hello.data.preferences.WeekStartDay
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.RelationshipTerminology
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePreferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(hasCompletedOnboarding = false)
        )
        contactRepository = ContactRepository()
        viewModel = OnboardingViewModel(fakePreferencesRepository, contactRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_startsAtStep0AndNotCompleted() {
        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
        assertEquals(6, state.totalSteps)
        assertEquals(RelationshipTerminology.PEOPLE, state.selectedTerminology)
        assertEquals(ThemeMode.SYSTEM, state.selectedThemeMode)
        assertEquals(WeekStartDay.MONDAY, state.selectedFirstDayOfWeek)
        assertFalse(state.isReplay)
        assertFalse(state.isCompleted)
    }

    @Test
    fun stepNavigation_advancesAndRetreatsWithinBounds() {
        repeat(5) { expectedStep ->
            assertEquals(expectedStep, viewModel.uiState.value.currentStep)
            viewModel.onEvent(OnboardingUiEvent.OnNextStep)
        }
        assertEquals(5, viewModel.uiState.value.currentStep)
        assertTrue(viewModel.uiState.value.isLastStep)

        // Cannot advance past 5
        viewModel.onEvent(OnboardingUiEvent.OnNextStep)
        assertEquals(5, viewModel.uiState.value.currentStep)

        // Retreat back to 0
        for (expectedStep in 4 downTo 0) {
            viewModel.onEvent(OnboardingUiEvent.OnPreviousStep)
            assertEquals(expectedStep, viewModel.uiState.value.currentStep)
        }

        // Cannot retreat past 0
        viewModel.onEvent(OnboardingUiEvent.OnPreviousStep)
        assertEquals(0, viewModel.uiState.value.currentStep)
    }

    @Test
    fun onSelectThemeMode_updatesStateAndPersists() = runTest {
        viewModel.onEvent(OnboardingUiEvent.OnSelectThemeMode(ThemeMode.DARK))
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.selectedThemeMode)
        assertEquals(ThemeMode.DARK, fakePreferencesRepository.userPreferencesFlow.first().themeMode)
    }

    @Test
    fun onSelectFirstDayOfWeek_updatesStateAndPersists() = runTest {
        viewModel.onEvent(OnboardingUiEvent.OnSelectFirstDayOfWeek(WeekStartDay.SUNDAY))
        advanceUntilIdle()

        assertEquals(WeekStartDay.SUNDAY, viewModel.uiState.value.selectedFirstDayOfWeek)
        assertEquals(WeekStartDay.SUNDAY, fakePreferencesRepository.userPreferencesFlow.first().firstDayOfWeek)
    }

    @Test
    fun onSelectTerminology_updatesStateAndPersists() = runTest {
        viewModel.onEvent(OnboardingUiEvent.OnSelectTerminology(RelationshipTerminology.FRIENDS))
        advanceUntilIdle()

        assertEquals(RelationshipTerminology.FRIENDS, viewModel.uiState.value.selectedTerminology)
        assertEquals(
            RelationshipTerminology.FRIENDS,
            fakePreferencesRepository.userPreferencesFlow.first().preferredTerminology
        )
    }

    @Test
    fun onFinishOnboarding_savesUserNameAndCreatesFirstConnectionWithTags() = runTest {
        val initialContactCount = contactRepository.contacts.value.size
        var callbackCalled = false

        viewModel.onEvent(OnboardingUiEvent.OnUserNameChange("  Ram  "))
        viewModel.onEvent(OnboardingUiEvent.OnSelectThemeMode(ThemeMode.DARK))
        viewModel.onEvent(OnboardingUiEvent.OnSelectFirstDayOfWeek(WeekStartDay.SUNDAY))
        viewModel.onEvent(OnboardingUiEvent.OnSelectTerminology(RelationshipTerminology.CONNECTIONS))
        viewModel.onEvent(OnboardingUiEvent.OnFirstConnectionNameChange("  Maya Lin  "))
        viewModel.onEvent(OnboardingUiEvent.OnFirstConnectionNoteChange("Met at Kissa Coffee"))
        viewModel.onEvent(OnboardingUiEvent.OnToggleFirstConnectionTag("coffee"))

        viewModel.onEvent(OnboardingUiEvent.OnFinishOnboarding {
            callbackCalled = true
        })
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompleted)
        assertTrue(callbackCalled)

        val savedPrefs = fakePreferencesRepository.userPreferencesFlow.first()
        assertEquals("Ram", savedPrefs.userName)
        assertEquals(ThemeMode.DARK, savedPrefs.themeMode)
        assertEquals(WeekStartDay.SUNDAY, savedPrefs.firstDayOfWeek)
        assertEquals(RelationshipTerminology.CONNECTIONS, savedPrefs.preferredTerminology)
        assertTrue(savedPrefs.hasCompletedOnboarding)

        assertEquals(initialContactCount + 1, contactRepository.contacts.value.size)
        val created = contactRepository.contacts.value.first()
        assertEquals("Maya Lin", created.name)
        assertEquals("Met at Kissa Coffee", created.notes)
        val createdTagNames = created.tags.map { it.name.lowercase() }.toSet()
        assertTrue("friends" in createdTagNames)
        assertTrue("coffee" in createdTagNames)
    }

    @Test
    fun onFinishOnboarding_withBlankName_doesNotOverwriteExistingUserNameAndSkipsEmptyContact() = runTest {
        val customPrefs = FakeUserPreferencesRepository(
            UserPreferences(
                userName = "Sam",
                hasCompletedOnboarding = false
            )
        )
        val customVm = OnboardingViewModel(customPrefs, contactRepository)
        advanceUntilIdle()

        val initialContactCount = contactRepository.contacts.value.size
        customVm.onEvent(OnboardingUiEvent.OnUserNameChange("   "))
        customVm.onEvent(OnboardingUiEvent.OnFirstConnectionNameChange("   "))
        customVm.onEvent(OnboardingUiEvent.OnFinishOnboarding {})
        advanceUntilIdle()

        val savedPrefs = customPrefs.userPreferencesFlow.first()
        assertEquals("Sam", savedPrefs.userName)
        assertTrue(savedPrefs.hasCompletedOnboarding)
        assertEquals(initialContactCount, contactRepository.contacts.value.size)
    }

    @Test
    fun onSkipOnboarding_completesFirstLaunchWithoutCreatingContact() = runTest {
        val initialContactCount = contactRepository.contacts.value.size
        var callbackCalled = false

        viewModel.onEvent(OnboardingUiEvent.OnUserNameChange("Ram"))
        viewModel.onEvent(OnboardingUiEvent.OnFirstConnectionNameChange("Unsaved Person"))
        viewModel.onEvent(OnboardingUiEvent.OnSkipOnboarding {
            callbackCalled = true
        })
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompleted)
        assertTrue(callbackCalled)

        val savedPrefs = fakePreferencesRepository.userPreferencesFlow.first()
        assertEquals("Alex", savedPrefs.userName)
        assertTrue(savedPrefs.hasCompletedOnboarding)
        assertEquals(initialContactCount, contactRepository.contacts.value.size)
    }

    @Test
    fun replayMode_prefillsExistingPreferencesAndSkipDoesNotOverwriteOrResetData() = runTest {
        val existingPrefsRepo = FakeUserPreferencesRepository(
            UserPreferences(
                userName = "Jordan",
                hasCompletedOnboarding = true,
                preferredTerminology = RelationshipTerminology.CONTACTS,
                themeMode = ThemeMode.LIGHT,
                firstDayOfWeek = WeekStartDay.SUNDAY
            )
        )
        val replayVm = OnboardingViewModel(
            preferencesRepository = existingPrefsRepo,
            contactRepository = contactRepository,
            isReplay = true
        )
        advanceUntilIdle()

        val state = replayVm.uiState.value
        assertTrue(state.isReplay)
        assertEquals("Jordan", state.userNameInput)
        assertEquals(ThemeMode.LIGHT, state.selectedThemeMode)
        assertEquals(WeekStartDay.SUNDAY, state.selectedFirstDayOfWeek)
        assertEquals(RelationshipTerminology.CONTACTS, state.selectedTerminology)

        val initialContactCount = contactRepository.contacts.value.size
        var finished = false
        replayVm.onEvent(OnboardingUiEvent.OnSkipOnboarding {
            finished = true
        })
        advanceUntilIdle()

        assertTrue(finished)
        val savedPrefs = existingPrefsRepo.userPreferencesFlow.first()
        assertEquals("Jordan", savedPrefs.userName)
        assertTrue(savedPrefs.hasCompletedOnboarding)
        assertEquals(ThemeMode.LIGHT, savedPrefs.themeMode)
        assertEquals(WeekStartDay.SUNDAY, savedPrefs.firstDayOfWeek)
        assertEquals(RelationshipTerminology.CONTACTS, savedPrefs.preferredTerminology)
        assertEquals(initialContactCount, contactRepository.contacts.value.size)
    }

    @Test
    fun twoLayerTerminology_exposesPrimaryDirectoryOptionsAndCoreSphereTags() {
        assertEquals(
            listOf(RelationshipTerminology.PEOPLE, RelationshipTerminology.CONTACTS),
            RelationshipTerminology.primaryDirectoryOptions
        )
        assertEquals(
            listOf("friends", "work", "family", "uni"),
            RelationshipTerminology.coreSphereTags
        )
        assertEquals(
            "No people tagged \"friends\" yet.",
            RelationshipTerminology.PEOPLE.emptyTagFilterMessage("#friends")
        )
    }
}
