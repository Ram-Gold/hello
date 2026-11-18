package com.example.hello.ui.quickaction

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.preferences.DataStoreUserPreferencesRepository
import com.example.hello.data.preferences.ThemeMode
import com.example.hello.data.preferences.ThemeStyle
import com.example.hello.data.preferences.helloDataStore
import com.example.hello.data.repository.ContactRepository
import com.example.hello.platform.location.LocationService
import com.example.hello.platform.notification.FollowUpAlarmScheduler
import com.example.hello.ui.components.LocalReducedMotion
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType
import com.example.hello.ui.haptics.rememberHelloHaptics
import com.example.hello.ui.navigation.ScrubbablePredictiveBackHandler
import com.example.hello.ui.navigation.predictiveSheetDismiss
import com.example.hello.ui.navigation.resolveSheetScrimAlpha
import com.example.hello.ui.quickaction.components.FlowAAddContactCard
import com.example.hello.ui.quickaction.components.LocationExplainerDialog
import com.example.hello.ui.theme.HelloTheme

/**
 * Dedicated ultra-fast (<50ms cold start) Quick Add activity launched directly from home screen / Glance widget.
 * Styled with a translucent dialog theme and windowSoftInputMode=stateVisible for instant keyboard presentation.
 */
class QuickCaptureActivity : ComponentActivity() {

    private lateinit var viewModel: QuickActionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = HelloDatabase.getInstance(applicationContext)
        val alarmScheduler = FollowUpAlarmScheduler(applicationContext)
        val repo = ContactRepository(database = db, alarmScheduler = alarmScheduler)
        val locationService = LocationService(applicationContext)
        viewModel = QuickActionViewModel(repository = repo, locationService = locationService)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)

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
            val isReducedMotion = userPreferences?.reduceAnimations == true

            val helloHaptics = rememberHelloHaptics()

            HelloTheme(darkTheme = isDark, dynamicColor = dynamicColor, themeStyle = themeStyle) {
                CompositionLocalProvider(
                    LocalHelloHaptics provides helloHaptics,
                    LocalReducedMotion provides isReducedMotion
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    var innerStepBackProgress by remember { mutableFloatStateOf(0f) }
                    var sheetDismissProgress by remember { mutableFloatStateOf(0f) }

                    // Step back inside Flow A when currentStepIndex > 0
                    ScrubbablePredictiveBackHandler(
                        enabled = uiState.currentStepIndex > 0,
                        isReducedMotion = isReducedMotion,
                        onProgressChanged = { innerStepBackProgress = it },
                        finishOnCommit = false,
                        onCommit = {
                            viewModel.onEvent(QuickActionUiEvent.OnPreviousStep)
                        }
                    )

                    // Dismiss card vertically downward when at step 0
                    ScrubbablePredictiveBackHandler(
                        enabled = uiState.currentStepIndex == 0,
                        isReducedMotion = isReducedMotion,
                        onProgressChanged = { sheetDismissProgress = it },
                        finishOnCommit = true,
                        baseFinishDurationMs = 140,
                        onCommit = {
                            finishWithoutExitFlash()
                        }
                    )

                    // If save completes and flow closes, trigger completion haptic before finishing
                    LaunchedEffect(uiState.isOpen) {
                        if (!uiState.isOpen) {
                            if (uiState.userMessage != null) {
                                helloHaptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
                            }
                            finish()
                        }
                    }

                // In-App Location Explainer Dialog if triggered
                if (uiState.showLocationExplainer) {
                    LocationExplainerDialog(
                        onAllowClick = {
                            viewModel.onEvent(QuickActionUiEvent.OnLocationPermissionResult(granted = true))
                        },
                        onDismissClick = {
                            viewModel.onEvent(QuickActionUiEvent.OnDismissLocationExplainer)
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Translucent scrim backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = resolveSheetScrimAlpha(sheetDismissProgress, maxAlpha = 0.36f)))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                finish()
                            }
                    )

                    // Single card resting directly on top of IME
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.background,
                        tonalElevation = 0.dp,
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .predictiveSheetDismiss { sheetDismissProgress }
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { /* Consume taps on card surface */ }
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        FlowAAddContactCard(
                            uiState = uiState,
                            predictiveBackProgress = innerStepBackProgress,
                            onEvent = { event ->
                                when (event) {
                                    is QuickActionUiEvent.OnDismiss -> {
                                        finish()
                                    }
                                    else -> {
                                        viewModel.onEvent(event)
                                    }
                                }
                            }
                        )
                    }
                }
                }
            }
        }
    }

    private fun finishWithoutExitFlash() {
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
