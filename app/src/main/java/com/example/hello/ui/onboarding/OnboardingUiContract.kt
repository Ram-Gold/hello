package com.example.hello.ui.onboarding

import com.example.hello.domain.model.RelationshipTerminology

data class OnboardingUiState(
    val currentStep: Int = 0,
    val selectedTerminology: RelationshipTerminology = RelationshipTerminology.DEFAULT,
    val isCompleted: Boolean = false
)

sealed interface OnboardingUiEvent {
    data object OnNextStep : OnboardingUiEvent
    data object OnPreviousStep : OnboardingUiEvent
    data class OnSelectTerminology(val terminology: RelationshipTerminology) : OnboardingUiEvent
    data class OnFinishOnboarding(val onFinished: () -> Unit) : OnboardingUiEvent
}
