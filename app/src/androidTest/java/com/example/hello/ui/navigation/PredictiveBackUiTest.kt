package com.example.hello.ui.navigation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hello.domain.model.CaptureComponent
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.FlowPreset
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.ui.contacts.ArchivedContactsScreen
import com.example.hello.ui.home.components.HomeFloatingBottomDock
import com.example.hello.ui.onboarding.OnboardingScreen
import com.example.hello.ui.onboarding.OnboardingUiEvent
import com.example.hello.ui.onboarding.OnboardingUiState
import com.example.hello.ui.quickaction.FlowAStep
import com.example.hello.ui.quickaction.QuickActionMode
import com.example.hello.ui.quickaction.QuickActionSheetHost
import com.example.hello.ui.quickaction.QuickActionUiEvent
import com.example.hello.ui.quickaction.QuickActionUiState
import com.example.hello.ui.theme.HelloTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PredictiveBackUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboardingStep1_predictiveBackCancelKeepsStep_commitRetreatsToStep0() {
        var currentStep by mutableStateOf(1)
        val events = mutableListOf<OnboardingUiEvent>()

        composeTestRule.setContent {
            HelloTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(currentStep = currentStep),
                    onEvent = {
                        events.add(it)
                        if (it is OnboardingUiEvent.OnPreviousStep) {
                            currentStep = (currentStep - 1).coerceAtLeast(0)
                        }
                    },
                    onFinish = {}
                )
            }
        }

        val dispatcher = composeTestRule.activity.onBackPressedDispatcher

        // Cancel partway through predictive swipe: must remain on step 1
        composeTestRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 100f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(80f, 100f, 0.45f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackCancelled()
        }
        composeTestRule.waitForIdle()
        assertEquals(1, currentStep)
        assertTrue(events.none { it is OnboardingUiEvent.OnPreviousStep })

        // Commit predictive swipe: must retreat to step 0
        composeTestRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 100f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(140f, 100f, 0.7f, BackEventCompat.EDGE_LEFT))
            dispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        assertEquals(0, currentStep)
        assertTrue(events.any { it is OnboardingUiEvent.OnPreviousStep })
    }

    @Test
    fun quickActionSheetHost_predictiveBackStepsBackInnerFlowAndDismissesAtStep0() {
        val preset = FlowPreset(
            id = "test",
            name = "Test",
            description = "Test preset",
            components = listOf(
                CaptureComponent(id = "c1", type = CaptureComponentType.NAME, label = "Name", stepOrder = 0, isRequired = true),
                CaptureComponent(id = "c2", type = CaptureComponentType.ROLE, label = "Role", stepOrder = 1, isRequired = false)
            )
        )
        var state by mutableStateOf(
            QuickActionUiState(
                isOpen = true,
                currentMode = QuickActionMode.FLOW_A_ADD_CONTACT,
                flowAStep = FlowAStep.ROLE,
                currentStepIndex = 1,
                activeFlowPreset = preset
            )
        )

        composeTestRule.setContent {
            HelloTheme {
                QuickActionSheetHost(
                    uiState = state,
                    onEvent = { event ->
                        when (event) {
                            QuickActionUiEvent.OnPreviousStep -> {
                                state = state.copy(currentStepIndex = 0, flowAStep = FlowAStep.NAME)
                            }
                            QuickActionUiEvent.OnDismiss -> {
                                state = state.copy(isOpen = false)
                            }
                            else -> Unit
                        }
                    }
                )
            }
        }

        val dispatcher = composeTestRule.activity.onBackPressedDispatcher

        // First commit at step 1 -> steps back to step 0 while staying open
        composeTestRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 100f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(100f, 100f, 0.5f, BackEventCompat.EDGE_LEFT))
            dispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        assertTrue(state.isOpen)
        assertEquals(0, state.currentStepIndex)

        // Second commit at step 0 -> dismisses sheet
        composeTestRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 100f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(120f, 100f, 0.6f, BackEventCompat.EDGE_LEFT))
            dispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        assertFalse(state.isOpen)
    }

    @Test
    fun archivedContactsScreen_doesNotRegisterBlockingBackHandler() {
        var navigateBackCalls = 0
        composeTestRule.setContent {
            HelloTheme {
                ArchivedContactsScreen(
                    contacts = emptyList(),
                    onRestoreContact = {},
                    onNavigateBack = { navigateBackCalls++ }
                )
            }
        }

        // ArchivedContactsScreen delegates system back to NavHost predictive back rather than
        // intercepting with an unconditional BackHandler.
        var hasCallbacks = true
        composeTestRule.runOnUiThread {
            hasCallbacks = composeTestRule.activity.onBackPressedDispatcher.hasEnabledCallbacks()
        }
        assertFalse(hasCallbacks)
        assertEquals(0, navigateBackCalls)
    }

    @Test
    fun deeperRoutePredictiveBackCommit_popsProfileOnceWithoutReplayingExit() {
        var popCount = 0
        var exitTransitionsEvaluated = 0
        var skippedExitEvaluations = 0
        val popHandoff = PredictiveBackPopHandoff()

        composeTestRule.setContent {
            HelloTheme {
                val navController = rememberNavController()
                var backProgress by remember { mutableStateOf(0f) }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            if (popCount == 0 && navController.currentDestination?.route == "home") {
                                navController.navigate("detail")
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text("Home Root")
                        }
                    }
                    composable(
                        route = "detail",
                        popExitTransition = {
                            exitTransitionsEvaluated++
                            if (popHandoff.shouldSkipPopExit(initialState.id)) {
                                skippedExitEvaluations++
                                ExitTransition.None
                            } else {
                                PersonDetailMotionConfig.detailPopExitTransition(false)
                            }
                        }
                    ) { backStackEntry ->
                        DisposableEffect(backStackEntry.id) {
                            onDispose {
                                if (popHandoff.onExitingEntryDisposed(backStackEntry.id)) {
                                    backProgress = 0f
                                }
                            }
                        }
                        ScrubbablePredictiveBackHandler(
                            enabled = !popHandoff.shouldSkipPopExit(backStackEntry.id),
                            isReducedMotion = false,
                            onProgressChanged = { backProgress = it },
                            finishOnCommit = true,
                            onCommit = {
                                popCount++
                                popHandoff.markCommitted(backStackEntry.id)
                                navController.popBackStack()
                            }
                        )
                        Box(modifier = Modifier.fillMaxSize().predictiveDeeperRoutePopExit { backProgress }) {
                            Text("Contact Profile")
                        }
                    }
                }
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Contact Profile")).fetchSemanticsNodes().isNotEmpty()
        }

        val dispatcher = composeTestRule.activity.onBackPressedDispatcher
        composeTestRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 100f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(100f, 100f, 0.5f, BackEventCompat.EDGE_LEFT))
            dispatcher.onBackPressed()
        }

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Home Root")).fetchSemanticsNodes().isNotEmpty() &&
                !popHandoff.hasActiveCommit()
        }
        assertEquals(1, popCount)
        assertTrue(exitTransitionsEvaluated == 0 || skippedExitEvaluations == exitTransitionsEvaluated)
    }

    @Test
    fun floatingBottomDock_overridePillProgressExpandsIncomingLabelAlongsideSwipe() {
        var overrideProgress by mutableStateOf<Float?>(0.5f)
        var selectedTab by mutableStateOf(HomeNavTab.PEOPLE)

        composeTestRule.setContent {
            HelloTheme {
                HomeFloatingBottomDock(
                    selectedTab = selectedTab,
                    onTabSelect = {},
                    isSpeedDialOpen = false,
                    onToggleSpeedDial = {},
                    onSpeedDialAction = {},
                    overridePillProgress = overrideProgress
                )
            }
        }

        composeTestRule.waitForIdle()
        val homeWidthMid = composeTestRule.onNodeWithText("Home", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
        val peopleWidthMid = composeTestRule.onNodeWithText("People", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
        assertTrue("Home label must partially expand during People->Home back swipe", homeWidthMid > 5f)
        assertTrue("People label must partially remain during People->Home back swipe", peopleWidthMid > 5f)

        // When skipping Timeline -> Home at midpoint 1.0f, People label must stay collapsed (0px)
        selectedTab = HomeNavTab.TIMELINE
        overrideProgress = 1.0f
        composeTestRule.waitForIdle()
        val peopleWidthSkip = composeTestRule.onNodeWithText("People", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
        assertEquals(0f, peopleWidthSkip, 0.5f)
    }

    @Test
    fun floatingBottomDock_profileMorphMovesShrinksStretchesAndTriggersEncounterCallback() {
        var morphProgress by mutableStateOf(0f)
        var isProfileMode by mutableStateOf(false)
        var encounterClickCount = 0

        composeTestRule.setContent {
            HelloTheme {
                HomeFloatingBottomDock(
                    selectedTab = HomeNavTab.PEOPLE,
                    onTabSelect = {},
                    isSpeedDialOpen = false,
                    onToggleSpeedDial = {},
                    onSpeedDialAction = {},
                    profileMorphProgressProvider = { morphProgress },
                    isProfileEncounterMode = isProfileMode,
                    onProfileEncounterClick = { encounterClickCount++ }
                )
            }
        }

        // 0.0: Root FAB on right, "Catching up?" collapsed
        composeTestRule.waitForIdle()
        val fabBoundsStart = composeTestRule.onNodeWithContentDescription("Quick capture").fetchSemanticsNode().boundsInRoot
        val labelWidthStart = composeTestRule.onNodeWithText("Catching up?", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
        assertEquals(0f, labelWidthStart, 0.5f)

        // 0.45: Action button stays right-anchored at full 60dp height and sweeps right-to-left
        morphProgress = 0.45f
        isProfileMode = true
        composeTestRule.waitForIdle()
        val midBounds = composeTestRule.onNodeWithContentDescription("Catching up? Log encounter").fetchSemanticsNode().boundsInRoot
        assertEquals(
            "Action button right edge must stay anchored during liquid right-to-left swallow",
            fabBoundsStart.right,
            midBounds.right,
            2f
        )
        assertEquals(
            "Action button height must remain locked at 60dp (no vertical shrink or drop)",
            fabBoundsStart.height,
            midBounds.height,
            2f
        )
        assertTrue(
            "Action button left edge must sweep right-to-left",
            midBounds.left < fabBoundsStart.left
        )
        assertTrue(
            "Action button must expand wider as it absorbs the 3-tab dock",
            midBounds.width > fabBoundsStart.width
        )

        // 1.0: Stretched Profile Encounter Pill with "+ Catching up?" visible and clickable
        morphProgress = 1f
        composeTestRule.waitForIdle()
        val pillBoundsEnd = composeTestRule.onNodeWithContentDescription("Catching up? Log encounter").fetchSemanticsNode().boundsInRoot
        val labelWidthEnd = composeTestRule.onNodeWithText("Catching up?", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width
        assertTrue(
            "Profile Encounter Pill must stretch significantly wider than the root FAB",
            pillBoundsEnd.width > fabBoundsStart.width * 3f
        )
        assertEquals(
            "Profile Encounter Pill height must match root Quick-Add FAB height",
            fabBoundsStart.height,
            pillBoundsEnd.height,
            2f
        )
        assertTrue("Catching up? label must expand at full morph", labelWidthEnd > 30f)

        composeTestRule.onNodeWithContentDescription("Catching up? Log encounter").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, encounterClickCount)
    }
}
