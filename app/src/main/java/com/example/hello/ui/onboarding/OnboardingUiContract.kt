package com.example.hello.ui.onboarding

import com.example.hello.data.preferences.ThemeMode
import com.example.hello.data.preferences.WeekStartDay
import com.example.hello.domain.model.RelationshipTerminology

const val ONBOARDING_TOTAL_STEPS = 7
const val ONBOARDING_LAST_STEP = ONBOARDING_TOTAL_STEPS - 1

data class OnboardingUiState(
    val currentStep: Int = 0,
    val userNameInput: String = "",
    val selectedThemeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedFirstDayOfWeek: WeekStartDay = WeekStartDay.MONDAY,
    val selectedTerminology: RelationshipTerminology = RelationshipTerminology.DEFAULT,
    val isLocationPermissionGranted: Boolean = false,
    val hasRequestedLocationPermission: Boolean = false,
    val firstConnectionName: String = "",
    val firstConnectionNote: String = "",
    val firstConnectionTagNames: Set<String> = setOf("friends"),
    val isReplay: Boolean = false,
    val isCompleted: Boolean = false
) {
    val totalSteps: Int get() = ONBOARDING_TOTAL_STEPS
    val isLastStep: Boolean get() = currentStep >= ONBOARDING_LAST_STEP
}

sealed interface OnboardingUiEvent {
    data object OnNextStep : OnboardingUiEvent
    data object OnPreviousStep : OnboardingUiEvent
    data class OnUserNameChange(val name: String) : OnboardingUiEvent
    data class OnSelectThemeMode(val themeMode: ThemeMode) : OnboardingUiEvent
    data class OnSelectFirstDayOfWeek(val firstDayOfWeek: WeekStartDay) : OnboardingUiEvent
    data class OnSelectTerminology(val terminology: RelationshipTerminology) : OnboardingUiEvent
    data class OnSyncLocationPermission(val granted: Boolean) : OnboardingUiEvent
    data class OnLocationPermissionResult(val granted: Boolean) : OnboardingUiEvent
    data class OnFirstConnectionNameChange(val name: String) : OnboardingUiEvent
    data class OnFirstConnectionNoteChange(val note: String) : OnboardingUiEvent
    data class OnToggleFirstConnectionTag(val tagName: String) : OnboardingUiEvent
    data class OnSkipOnboarding(val onFinished: () -> Unit) : OnboardingUiEvent
    data class OnFinishOnboarding(
        val createFirstConnection: Boolean = true,
        val onFinished: () -> Unit
    ) : OnboardingUiEvent
}
