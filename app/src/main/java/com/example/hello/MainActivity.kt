package com.example.hello

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.hello.data.preferences.DataStoreUserPreferencesRepository
import com.example.hello.data.preferences.ThemeMode
import com.example.hello.data.preferences.ThemeStyle
import com.example.hello.data.preferences.helloDataStore
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import com.example.hello.platform.notification.FollowUpNotificationHelper
import com.example.hello.ui.MainAppShell
import com.example.hello.ui.theme.HelloTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val deepLinkContactIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        deepLinkContactIdState.value = extractContactDeepLink(intent)

        // Reconcile pending FollowUp alarms off the main thread (handles force-stop / exact-alarm revocation recovery)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                FollowUpAlarmScheduler.rescheduleAllPendingFromDatabase(applicationContext)
            } catch (_: Exception) {
                // Non-critical startup reconciliation
            }
        }

        setContent {
            val preferencesRepository = remember { DataStoreUserPreferencesRepository(applicationContext.helloDataStore) }
            val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(initial = null)
            val isDark = when (userPreferences?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            val dynamicColor = userPreferences?.dynamicColor ?: false
            val themeStyle = userPreferences?.themeStyle ?: ThemeStyle.MONOCHROME

            HelloTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColor,
                themeStyle = themeStyle
            ) {
                MainAppShell(
                    preferencesRepository = preferencesRepository,
                    initialContactId = deepLinkContactIdState.value
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractContactDeepLink(intent)?.let { contactId ->
            deepLinkContactIdState.value = contactId
        }
    }

    private fun extractContactDeepLink(intent: Intent?): String? {
        return if (intent?.action == FollowUpNotificationHelper.ACTION_VIEW_CONTACT) {
            intent.getStringExtra(FollowUpNotificationHelper.EXTRA_CONTACT_ID)
        } else {
            null
        }
    }
}
