package com.example.hello.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.preferences.UserPreferencesRepository
import com.example.hello.data.repository.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val contactRepository: ContactRepository? = null,
    private val isReplay: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(isReplay = isReplay))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var hasEditedName = false
    private var hasEditedTheme = false
    private var hasEditedWeekStart = false
    private var hasEditedTerminology = false
    private var hasFinished = false

    init {
        viewModelScope.launch {
            val initial = preferencesRepository.userPreferencesFlow.first()
            _uiState.update { state ->
                state.copy(
                    userNameInput = if (hasEditedName) {
                        state.userNameInput
                    } else if (isReplay) {
                        initial.userName
                    } else {
                        initial.userName.takeIf { !it.equals("Alex", ignoreCase = true) }.orEmpty()
                    },
                    selectedThemeMode = if (hasEditedTheme) state.selectedThemeMode else initial.themeMode,
                    selectedFirstDayOfWeek = if (hasEditedWeekStart) state.selectedFirstDayOfWeek else initial.firstDayOfWeek,
                    selectedTerminology = if (hasEditedTerminology) state.selectedTerminology else initial.preferredTerminology
                )
            }
        }
    }

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            OnboardingUiEvent.OnNextStep -> {
                _uiState.update {
                    it.copy(currentStep = (it.currentStep + 1).coerceAtMost(ONBOARDING_LAST_STEP))
                }
            }

            OnboardingUiEvent.OnPreviousStep -> {
                _uiState.update {
                    it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0))
                }
            }

            is OnboardingUiEvent.OnUserNameChange -> {
                hasEditedName = true
                _uiState.update { it.copy(userNameInput = event.name) }
            }

            is OnboardingUiEvent.OnSelectThemeMode -> {
                hasEditedTheme = true
                _uiState.update { it.copy(selectedThemeMode = event.themeMode) }
                viewModelScope.launch {
                    preferencesRepository.setThemeMode(event.themeMode)
                }
            }

            is OnboardingUiEvent.OnSelectFirstDayOfWeek -> {
                hasEditedWeekStart = true
                _uiState.update { it.copy(selectedFirstDayOfWeek = event.firstDayOfWeek) }
                viewModelScope.launch {
                    preferencesRepository.setFirstDayOfWeek(event.firstDayOfWeek)
                }
            }

            is OnboardingUiEvent.OnSelectTerminology -> {
                hasEditedTerminology = true
                _uiState.update { it.copy(selectedTerminology = event.terminology) }
                viewModelScope.launch {
                    preferencesRepository.setPreferredTerminology(event.terminology)
                }
            }

            is OnboardingUiEvent.OnFirstConnectionNameChange -> {
                _uiState.update { it.copy(firstConnectionName = event.name) }
            }

            is OnboardingUiEvent.OnFirstConnectionNoteChange -> {
                _uiState.update { it.copy(firstConnectionNote = event.note) }
            }

            is OnboardingUiEvent.OnToggleFirstConnectionTag -> {
                val clean = event.tagName.trim().lowercase()
                if (clean.isNotEmpty()) {
                    _uiState.update { state ->
                        val updatedTags = if (clean in state.firstConnectionTagNames) {
                            state.firstConnectionTagNames - clean
                        } else {
                            state.firstConnectionTagNames + clean
                        }
                        state.copy(firstConnectionTagNames = updatedTags)
                    }
                }
            }

            is OnboardingUiEvent.OnSkipOnboarding -> {
                if (hasFinished) return
                hasFinished = true
                viewModelScope.launch {
                    if (!isReplay) {
                        preferencesRepository.setOnboardingCompleted(true)
                    }
                    _uiState.update { it.copy(isCompleted = true) }
                    event.onFinished()
                }
            }

            is OnboardingUiEvent.OnFinishOnboarding -> {
                if (hasFinished) return
                hasFinished = true
                val snapshot = _uiState.value
                viewModelScope.launch {
                    val trimmedName = snapshot.userNameInput.trim()
                    if (trimmedName.isNotEmpty()) {
                        preferencesRepository.setUserName(trimmedName)
                    }
                    preferencesRepository.setThemeMode(snapshot.selectedThemeMode)
                    preferencesRepository.setFirstDayOfWeek(snapshot.selectedFirstDayOfWeek)
                    preferencesRepository.setPreferredTerminology(snapshot.selectedTerminology)
                    if (!isReplay) {
                        preferencesRepository.setOnboardingCompleted(true)
                    }
                    if (event.createFirstConnection && snapshot.firstConnectionName.isNotBlank() && contactRepository != null) {
                        val resolvedTagIds = snapshot.firstConnectionTagNames
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .map { contactRepository.addTag(it).id }
                            .toSet()
                        contactRepository.addContactWithInitialEncounter(
                            name = snapshot.firstConnectionName.trim(),
                            tagIds = resolvedTagIds,
                            placeId = null,
                            customPlaceName = null,
                            note = snapshot.firstConnectionNote.trim(),
                            followUpDays = null
                        )
                    }
                    _uiState.update { it.copy(isCompleted = true) }
                    event.onFinished()
                }
            }
        }
    }
}
