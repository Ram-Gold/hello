package com.example.hello

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.hello.data.preferences.DataStoreUserPreferencesRepository
import com.example.hello.data.preferences.ThemeMode
import com.example.hello.data.preferences.helloDataStore
import com.example.hello.platform.notification.FollowUpNotificationHelper
import com.example.hello.ui.MainAppShell
import com.example.hello.ui.theme.HelloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialContactId = if (intent?.action == FollowUpNotificationHelper.ACTION_VIEW_CONTACT) {
            intent.getStringExtra(FollowUpNotificationHelper.EXTRA_CONTACT_ID)
        } else null

        setContent {
            val preferencesRepository = remember { DataStoreUserPreferencesRepository(applicationContext.helloDataStore) }
            val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(initial = null)
            val isDark = when (userPreferences?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            val dynamicColor = userPreferences?.dynamicColor ?: false

            HelloTheme(darkTheme = isDark, dynamicColor = dynamicColor) {
                MainAppShell(
                    preferencesRepository = preferencesRepository,
                    initialContactId = initialContactId
                )
            }
        }
    }
}
