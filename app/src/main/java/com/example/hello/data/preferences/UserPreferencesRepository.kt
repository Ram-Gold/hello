package com.example.hello.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hello.domain.model.RelationshipTerminology
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.helloDataStore: DataStore<Preferences> by preferencesDataStore(name = "hello_user_preferences")

data class UserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val preferredTerminology: RelationshipTerminology = RelationshipTerminology.DEFAULT
)

interface UserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun setOnboardingCompleted(completed: Boolean = true)
    suspend fun setPreferredTerminology(terminology: RelationshipTerminology)
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
            UserPreferences(
                hasCompletedOnboarding = completed,
                preferredTerminology = terminology
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

    companion object {
        val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val KEY_PREFERRED_TERMINOLOGY = stringPreferencesKey("preferred_terminology")
    }
}
