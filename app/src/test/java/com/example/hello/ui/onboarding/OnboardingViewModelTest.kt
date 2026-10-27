package com.example.hello.ui.onboarding

import com.example.hello.data.preferences.FakeUserPreferencesRepository
import com.example.hello.data.preferences.UserPreferences
import com.example.hello.domain.model.RelationshipTerminology
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakePreferencesRepository = FakeUserPreferencesRepository(
            UserPreferences(hasCompletedOnboarding = false)
        )
        viewModel = OnboardingViewModel(fakePreferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_startsAtStep0AndNotCompleted() {
        val state = viewModel.uiState.value
        assertEquals(0, state.currentStep)
        assertEquals(RelationshipTerminology.PEOPLE, state.selectedTerminology)
        assertFalse(state.isCompleted)
    }

    @Test
    fun stepNavigation_advancesAndRetreatsWithinBounds() {
        viewModel.onEvent(OnboardingUiEvent.OnNextStep)
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.onEvent(OnboardingUiEvent.OnNextStep)
        assertEquals(2, viewModel.uiState.value.currentStep)

        // Cannot advance past 2
        viewModel.onEvent(OnboardingUiEvent.OnNextStep)
        assertEquals(2, viewModel.uiState.value.currentStep)

        // Retreat
        viewModel.onEvent(OnboardingUiEvent.OnPreviousStep)
        assertEquals(1, viewModel.uiState.value.currentStep)

        viewModel.onEvent(OnboardingUiEvent.OnPreviousStep)
        assertEquals(0, viewModel.uiState.value.currentStep)

        // Cannot retreat past 0
        viewModel.onEvent(OnboardingUiEvent.OnPreviousStep)
        assertEquals(0, viewModel.uiState.value.currentStep)
    }

    @Test
    fun onSelectTerminology_updatesStateAndPersists() = runTest {
        viewModel.onEvent(OnboardingUiEvent.OnSelectTerminology(RelationshipTerminology.FRIENDS))
        advanceUntilIdle()

        assertEquals(RelationshipTerminology.FRIENDS, viewModel.uiState.value.selectedTerminology)
    }

    @Test
    fun onFinishOnboarding_persistsAndCallsCallback() = runTest {
        viewModel.onEvent(OnboardingUiEvent.OnSelectTerminology(RelationshipTerminology.CONNECTIONS))
        var callbackCalled = false

        viewModel.onEvent(OnboardingUiEvent.OnFinishOnboarding {
            callbackCalled = true
        })
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isCompleted)
        assertTrue(callbackCalled)
    }
}
