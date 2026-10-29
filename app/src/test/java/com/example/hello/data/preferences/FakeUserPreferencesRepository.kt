package com.example.hello.data.preferences

import com.example.hello.domain.model.RelationshipTerminology
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeUserPreferencesRepository(
    initialPreferences: UserPreferences = UserPreferences()
) : UserPreferencesRepository {

    private val _prefs = MutableStateFlow(initialPreferences)
    override val userPreferencesFlow: Flow<UserPreferences> = _prefs.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _prefs.update { it.copy(hasCompletedOnboarding = completed) }
    }

    override suspend fun setPreferredTerminology(terminology: RelationshipTerminology) {
        _prefs.update { it.copy(preferredTerminology = terminology) }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        _prefs.update { it.copy(themeMode = themeMode) }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        _prefs.update { it.copy(dynamicColor = enabled) }
    }

    override suspend fun setReduceAnimations(enabled: Boolean) {
        _prefs.update { it.copy(reduceAnimations = enabled) }
    }

    override suspend fun setDefaultCadenceDays(days: Int) {
        _prefs.update { it.copy(defaultCadenceDays = days) }
    }

    override suspend fun setNotificationTime(hour: Int, minute: Int) {
        _prefs.update { it.copy(notificationHour = hour, notificationMinute = minute) }
    }

    override suspend fun setDefaultIncludeLocation(enabled: Boolean) {
        _prefs.update { it.copy(defaultIncludeLocation = enabled) }
    }
}
