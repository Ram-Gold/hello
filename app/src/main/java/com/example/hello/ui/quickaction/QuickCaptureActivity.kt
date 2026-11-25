package com.example.hello.ui.quickaction

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hello.HelloApplication
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
import com.example.hello.ui.quickaction.components.QuickCaptureComposerCard
import com.example.hello.ui.theme.HelloTheme

/**
 * Dedicated ultra-fast (<50ms cold start) Quick Add activity launched directly from home screen / Glance widget.
 * Uses pre-positioned IME placement (ADR-0040) with stateAlwaysVisible|adjustNothing on API 30+
 * so the keyboard rises without per-frame Compose relayout contention.
 */
class QuickCaptureActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var cachedPortraitImePx: Int = 0

        @Volatile
        private var cachedLandscapeImePx: Int = 0

        private fun getCachedImePx(isLandscape: Boolean): Int =
            if (isLandscape) cachedLandscapeImePx else cachedPortraitImePx

        private fun setCachedImePx(isLandscape: Boolean, heightPx: Int) {
            if (isLandscape) {
                cachedLandscapeImePx = heightPx
            } else {
                cachedPortraitImePx = heightPx
            }
        }
    }

    private lateinit var viewModel: QuickActionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
        enableEdgeToEdge()

        val app = HelloApplication.from(applicationContext)
        val preferencesRepository = app?.userPreferencesRepository
            ?: DataStoreUserPreferencesRepository(applicationContext.helloDataStore)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val repo = app?.contactRepository ?: ContactRepository(
                        database = HelloDatabase.getInstance(applicationContext),
                        alarmScheduler = FollowUpAlarmScheduler(applicationContext)
                    )
                    val locationService = app?.locationService ?: LocationService(applicationContext)
                    return QuickActionViewModel(
                        repository = repo,
                        locationService = locationService,
                        captureFlowRepository = app?.captureFlowRepository,
                        preferencesRepository = preferencesRepository
                    ) as T
                }
            }
        )[QuickActionViewModel::class.java]
        if (savedInstanceState == null) {
            viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        }

        setContent {
            val userPreferences by preferencesRepository.userPreferencesFlow.collectAsStateWithLifecycle(initialValue = null)
            val isDark = when (userPreferences?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            val dynamicColor = userPreferences?.dynamicColor ?: false
            val themeStyle = userPreferences?.themeStyle ?: ThemeStyle.MONOCHROME
            val isReducedMotion = userPreferences?.reduceAnimations == true

            val helloHaptics = rememberHelloHaptics()
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val screenHeightPx = resources.displayMetrics.heightPixels
            val navBarBottomPx = WindowInsets.navigationBars.getBottom(density)
            val imeInsets = WindowInsets.ime
            val thresholdPx = with(density) { WidgetImePlacement.SETTLED_CORRECTION_THRESHOLD_DP.dp.roundToPx() }

            val savedPrefImeHeightPx = if (isLandscape) {
                userPreferences?.widgetImeHeightLandscapePx ?: 0
            } else {
                userPreferences?.widgetImeHeightPortraitPx ?: 0
            }
            val initialSeedImePx = if (savedPrefImeHeightPx > 0) savedPrefImeHeightPx else getCachedImePx(isLandscape)

            var restingBottomPaddingPx by remember(isLandscape) {
                mutableIntStateOf(
                    WidgetImePlacement.resolveInitialBottomPaddingPx(
                        savedImeHeightPx = initialSeedImePx,
                        screenHeightPx = screenHeightPx,
                        navBarBottomPx = navBarBottomPx
                    )
                )
            }

            // Warm in-process cache when DataStore emits (never jumps layout mid-rise)
            LaunchedEffect(savedPrefImeHeightPx, isLandscape) {
                if (savedPrefImeHeightPx > 0) {
                    setCachedImePx(isLandscape, savedPrefImeHeightPx)
                }
            }

            // Observe IME bottom off the composition phase (zero per-frame root recomposition):
            // 1. Dismiss immediately if the keyboard closes after having opened (no exceptions).
            // 2. Apply at most one post-settle correction (>4dp) and persist the learned height.
            LaunchedEffect(isLandscape, navBarBottomPx) {
                var hasSeenImeVisible = false
                var hasAppliedSettledCorrection = false
                snapshotFlow { imeInsets.getBottom(density) }
                    .collectLatest { imeBottomPx ->
                        val isVisibleNow = imeBottomPx > navBarBottomPx
                        if (isVisibleNow) {
                            hasSeenImeVisible = true
                            if (!hasAppliedSettledCorrection) {
                                delay(80L)
                                hasAppliedSettledCorrection = true
                                if (WidgetImePlacement.shouldApplySettledCorrection(restingBottomPaddingPx, imeBottomPx, thresholdPx)) {
                                    restingBottomPaddingPx = imeBottomPx
                                }
                                if (WidgetImePlacement.shouldApplySettledCorrection(getCachedImePx(isLandscape), imeBottomPx, thresholdPx)) {
                                    setCachedImePx(isLandscape, imeBottomPx)
                                    preferencesRepository.setWidgetImeHeightPx(isLandscape = isLandscape, heightPx = imeBottomPx)
                                }
                            }
                        } else if (WidgetImePlacement.shouldDismissOnImeChange(hasSeenImeVisible, isImeVisibleNow = false)) {
                            finishWithoutExitFlash()
                        }
                    }
            }

            HelloTheme(darkTheme = isDark, dynamicColor = dynamicColor, themeStyle = themeStyle) {
                CompositionLocalProvider(
                    LocalHelloHaptics provides helloHaptics,
                    LocalReducedMotion provides isReducedMotion
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    var innerStepBackProgress by remember { mutableFloatStateOf(0f) }
                    var sheetDismissProgress by remember { mutableFloatStateOf(0f) }

                    val hasOpenSubOverlay = uiState.isCustomizeQuickAddOpen ||
                        uiState.isPlusPopoverOpen ||
                        uiState.activeDetailTray != null

                    ScrubbablePredictiveBackHandler(
                        enabled = hasOpenSubOverlay,
                        isReducedMotion = isReducedMotion,
                        onProgressChanged = { innerStepBackProgress = it },
                        finishOnCommit = false,
                        onCommit = {
                            when {
                                uiState.isCustomizeQuickAddOpen -> viewModel.onEvent(QuickActionUiEvent.OnToggleCustomizeQuickAdd(false))
                                uiState.isPlusPopoverOpen -> viewModel.onEvent(QuickActionUiEvent.OnTogglePlusPopover(false))
                                uiState.activeDetailTray != null -> viewModel.onEvent(QuickActionUiEvent.OnToggleDetailTray(null))
                            }
                        }
                    )

                    ScrubbablePredictiveBackHandler(
                        enabled = !hasOpenSubOverlay,
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
                            finishWithoutExitFlash()
                        }
                    }

                // In-App Location Explainer Dialog & Runtime Permission Launcher if triggered
                ForegroundLocationPermissionHandler(
                    showExplainer = uiState.showLocationExplainer,
                    onPermissionResult = { granted ->
                        viewModel.onEvent(QuickActionUiEvent.OnLocationPermissionResult(granted = granted))
                    },
                    onDismissExplainer = {
                        viewModel.onEvent(QuickActionUiEvent.OnDismissLocationExplainer)
                    }
                )

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
                                when {
                                    uiState.isCustomizeQuickAddOpen -> viewModel.onEvent(QuickActionUiEvent.OnToggleCustomizeQuickAdd(false))
                                    uiState.isPlusPopoverOpen -> viewModel.onEvent(QuickActionUiEvent.OnTogglePlusPopover(false))
                                    uiState.activeDetailTray != null -> viewModel.onEvent(QuickActionUiEvent.OnToggleDetailTray(null))
                                    else -> finishWithoutExitFlash()
                                }
                            }
                    )

                    // Single floating Quick Capture card pre-positioned at learned IME height (ADR-0040)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .predictiveSheetDismiss { sheetDismissProgress }
                            .padding(bottom = with(density) { restingBottomPaddingPx.toDp() })
                    ) {
                        QuickCaptureComposerCard(
                            uiState = uiState,
                            onEvent = { event ->
                                when (event) {
                                    is QuickActionUiEvent.OnDismiss -> {
                                        finishWithoutExitFlash()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::viewModel.isInitialized) {
            viewModel.onEvent(QuickActionUiEvent.OnDismiss)
            viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        }
    }

    private fun finishWithoutExitFlash() {
        finishAndRemoveTask()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
