package com.example.hello.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.OnNextStep -> {
                _uiState.update {
                    it.copy(currentStep = (it.currentStep + 1).coerceAtMost(2))
                }
            }

            OnboardingUiEvent.OnPreviousStep -> {
                _uiState.update {
                    it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0))
                }
            }

            is OnboardingUiEvent.OnSelectTerminology -> {
                _uiState.update { it.copy(selectedTerminology = event.terminology) }
                viewModelScope.launch {
                    preferencesRepository.setPreferredTerminology(event.terminology)
                }
            }

            is OnboardingUiEvent.OnFinishOnboarding -> {
                viewModelScope.launch {
                    preferencesRepository.setPreferredTerminology(_uiState.value.selectedTerminology)
                    preferencesRepository.setOnboardingCompleted(true)
                    _uiState.update { it.copy(isCompleted = true) }
                    event.onFinished()
                }
            }
        }
    }
}
