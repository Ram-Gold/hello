package com.example.hello.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hello.domain.model.RelationshipTerminology
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.helloDataStore: DataStore<Preferences> by preferencesDataStore(name = "hello_user_preferences")

enum class ThemeMode(val id: String, val title: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
    }
}

data class UserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val preferredTerminology: RelationshipTerminology = RelationshipTerminology.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val reduceAnimations: Boolean = false,
    val defaultCadenceDays: Int = 30,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val defaultIncludeLocation: Boolean = true
)

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun setOnboardingCompleted(completed: Boolean = true)
    suspend fun setPreferredTerminology(terminology: RelationshipTerminology)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setReduceAnimations(enabled: Boolean)
    suspend fun setDefaultCadenceDays(days: Int)
    suspend fun setNotificationTime(hour: Int, minute: Int)
    suspend fun setDefaultIncludeLocation(enabled: Boolean)
}

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val completed = preferences[KEY_HAS_COMPLETED_ONBOARDING] ?: false
            val terminologyId = preferences[KEY_PREFERRED_TERMINOLOGY]
            val terminology = RelationshipTerminology.fromId(terminologyId)
            val themeModeId = preferences[KEY_THEME_MODE]
            val themeMode = ThemeMode.fromId(themeModeId)
            val dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: false
            val reduceAnimations = preferences[KEY_REDUCE_ANIMATIONS] ?: false
            val cadenceDays = preferences[KEY_DEFAULT_CADENCE_DAYS] ?: 30
            val notifHour = preferences[KEY_NOTIFICATION_HOUR] ?: 9
            val notifMinute = preferences[KEY_NOTIFICATION_MINUTE] ?: 0
            val includeLocation = preferences[KEY_DEFAULT_INCLUDE_LOCATION] ?: true

            UserPreferences(
                hasCompletedOnboarding = completed,
                preferredTerminology = terminology,
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                reduceAnimations = reduceAnimations,
                defaultCadenceDays = cadenceDays,
                notificationHour = notifHour,
                notificationMinute = notifMinute,
                defaultIncludeLocation = includeLocation
            )
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    override suspend fun setPreferredTerminology(terminology: RelationshipTerminology) {
        dataStore.edit { preferences ->
            preferences[KEY_PREFERRED_TERMINOLOGY] = terminology.id
        }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = themeMode.id
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    override suspend fun setReduceAnimations(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_REDUCE_ANIMATIONS] = enabled
        }
    }

    override suspend fun setDefaultCadenceDays(days: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_CADENCE_DAYS] = days
        }
    }

    override suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_HOUR] = hour
            preferences[KEY_NOTIFICATION_MINUTE] = minute
        }
    }

    override suspend fun setDefaultIncludeLocation(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_INCLUDE_LOCATION] = enabled
        }
    }

    companion object {
        val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val KEY_PREFERRED_TERMINOLOGY = stringPreferencesKey("preferred_terminology")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val KEY_DEFAULT_CADENCE_DAYS = intPreferencesKey("default_cadence_days")
        val KEY_NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val KEY_NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val KEY_DEFAULT_INCLUDE_LOCATION = booleanPreferencesKey("default_include_location")
    }
}
