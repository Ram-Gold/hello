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
import java.time.DayOfWeek

val Context.helloDataStore: DataStore<Preferences> by preferencesDataStore(name = "hello_user_preferences")

enum class ThemeMode(val id: String, val title: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
    }
}

enum class ThemeStyle(val id: String, val title: String, val subtitle: String) {
    MONOCHROME(
        id = "monochrome",
        title = "Minimalist (Mallow & Vellum)",
        subtitle = "High-contrast monochrome palette (#121212), Mallow & Vellum typography, 8dp borders"
    );

    companion object {
        fun fromId(id: String?): ThemeStyle = MONOCHROME
    }
}

enum class WeekStartDay(
    val id: String,
    val title: String,
    val subtitle: String,
    val dayOfWeek: DayOfWeek
) {
    MONDAY(
        id = "monday",
        title = "Monday",
        subtitle = "Weeks run Monday to Sunday (ISO standard)",
        dayOfWeek = DayOfWeek.MONDAY
    ),
    SUNDAY(
        id = "sunday",
        title = "Sunday",
        subtitle = "Weeks run Sunday to Saturday",
        dayOfWeek = DayOfWeek.SUNDAY
    );

    companion object {
        fun fromId(id: String?): WeekStartDay =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MONDAY
    }
}

enum class CheckInDigestFrequency(
    val id: String,
    val title: String,
    val subtitle: String,
    val intervalDays: Long
) {
    OFF(
        id = "off",
        title = "Off",
        subtitle = "Keep reconnect suggestions quiet and in-app only",
        intervalDays = 0L
    ),
    WEEKLY(
        id = "weekly",
        title = "Weekly",
        subtitle = "One gentle notification per week suggesting 2–3 quiet contacts",
        intervalDays = 7L
    ),
    BIWEEKLY(
        id = "biweekly",
        title = "Every 2 weeks",
        subtitle = "One gentle notification every two weeks suggesting 2–3 quiet contacts",
        intervalDays = 14L
    );

    companion object {
        fun fromId(id: String?): CheckInDigestFrequency =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OFF
    }
}

data class UserPreferences(
    val userName: String = "Alex",
    val hasCompletedOnboarding: Boolean = false,
    val preferredTerminology: RelationshipTerminology = RelationshipTerminology.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.MONOCHROME,
    val firstDayOfWeek: WeekStartDay = WeekStartDay.MONDAY,
    val dynamicColor: Boolean = false,
    val reduceAnimations: Boolean = false,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val checkInDigestFrequency: CheckInDigestFrequency = CheckInDigestFrequency.OFF,
    val activeFlowPresetId: String = "preset_default",
    val widgetImeHeightPortraitPx: Int = 0,
    val widgetImeHeightLandscapePx: Int = 0
)

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun setUserName(name: String)
    suspend fun setOnboardingCompleted(completed: Boolean = true)
    suspend fun setPreferredTerminology(terminology: RelationshipTerminology)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setThemeStyle(themeStyle: ThemeStyle)
    suspend fun setFirstDayOfWeek(firstDayOfWeek: WeekStartDay)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setReduceAnimations(enabled: Boolean)
    suspend fun setNotificationTime(hour: Int, minute: Int)
    suspend fun setCheckInDigestFrequency(frequency: CheckInDigestFrequency)
    suspend fun setActiveFlowPresetId(presetId: String)
    suspend fun setWidgetImeHeightPx(isLandscape: Boolean, heightPx: Int) {}
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
            val userName = preferences[KEY_USER_NAME] ?: "Alex"
            val completed = preferences[KEY_HAS_COMPLETED_ONBOARDING] ?: false
            val terminologyId = preferences[KEY_PREFERRED_TERMINOLOGY]
            val terminology = RelationshipTerminology.fromId(terminologyId)
            val themeModeId = preferences[KEY_THEME_MODE]
            val themeMode = ThemeMode.fromId(themeModeId)
            val themeStyleId = preferences[KEY_THEME_STYLE]
            val themeStyle = ThemeStyle.fromId(themeStyleId)
            val firstDayOfWeek = WeekStartDay.fromId(preferences[KEY_FIRST_DAY_OF_WEEK])
            val dynamicColor = preferences[KEY_DYNAMIC_COLOR] ?: false
            val reduceAnimations = preferences[KEY_REDUCE_ANIMATIONS] ?: false
            val notifHour = preferences[KEY_NOTIFICATION_HOUR] ?: 9
            val notifMinute = preferences[KEY_NOTIFICATION_MINUTE] ?: 0
            val checkInDigestFrequency = CheckInDigestFrequency.fromId(preferences[KEY_CHECK_IN_DIGEST_FREQUENCY])
            val activeFlowPresetId = preferences[KEY_ACTIVE_FLOW_PRESET_ID] ?: "preset_default"
            val widgetImeHeightPortraitPx = preferences[KEY_WIDGET_IME_HEIGHT_PORTRAIT_PX] ?: 0
            val widgetImeHeightLandscapePx = preferences[KEY_WIDGET_IME_HEIGHT_LANDSCAPE_PX] ?: 0

            UserPreferences(
                userName = userName,
                hasCompletedOnboarding = completed,
                preferredTerminology = terminology,
                themeMode = themeMode,
                themeStyle = themeStyle,
                firstDayOfWeek = firstDayOfWeek,
                dynamicColor = dynamicColor,
                reduceAnimations = reduceAnimations,
                notificationHour = notifHour,
                notificationMinute = notifMinute,
                checkInDigestFrequency = checkInDigestFrequency,
                activeFlowPresetId = activeFlowPresetId,
                widgetImeHeightPortraitPx = widgetImeHeightPortraitPx,
                widgetImeHeightLandscapePx = widgetImeHeightLandscapePx
            )
        }

    override suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name.trim()
        }
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

    override suspend fun setThemeStyle(themeStyle: ThemeStyle) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_STYLE] = themeStyle.id
        }
    }

    override suspend fun setFirstDayOfWeek(firstDayOfWeek: WeekStartDay) {
        dataStore.edit { preferences ->
            preferences[KEY_FIRST_DAY_OF_WEEK] = firstDayOfWeek.id
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

    override suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_HOUR] = hour
            preferences[KEY_NOTIFICATION_MINUTE] = minute
        }
    }

    override suspend fun setCheckInDigestFrequency(frequency: CheckInDigestFrequency) {
        dataStore.edit { preferences ->
            preferences[KEY_CHECK_IN_DIGEST_FREQUENCY] = frequency.id
        }
    }

    override suspend fun setActiveFlowPresetId(presetId: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ACTIVE_FLOW_PRESET_ID] = presetId
        }
    }

    override suspend fun setWidgetImeHeightPx(isLandscape: Boolean, heightPx: Int) {
        val safeHeight = heightPx.coerceAtLeast(0)
        dataStore.edit { preferences ->
            val key = if (isLandscape) KEY_WIDGET_IME_HEIGHT_LANDSCAPE_PX else KEY_WIDGET_IME_HEIGHT_PORTRAIT_PX
            preferences[key] = safeHeight
        }
    }

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val KEY_PREFERRED_TERMINOLOGY = stringPreferencesKey("preferred_terminology")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_THEME_STYLE = stringPreferencesKey("theme_style")
        val KEY_FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val KEY_NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val KEY_NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val KEY_CHECK_IN_DIGEST_FREQUENCY = stringPreferencesKey("check_in_digest_frequency")
        val KEY_ACTIVE_FLOW_PRESET_ID = stringPreferencesKey("active_flow_preset_id")
        val KEY_WIDGET_IME_HEIGHT_PORTRAIT_PX = intPreferencesKey("widget_ime_height_portrait_px")
        val KEY_WIDGET_IME_HEIGHT_LANDSCAPE_PX = intPreferencesKey("widget_ime_height_landscape_px")
    }
}
