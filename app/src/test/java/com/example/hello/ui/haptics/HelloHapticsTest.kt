package com.example.hello.ui.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelloHapticsTest {

    private class RecordingHapticFeedback : HapticFeedback {
        val performedFeedback = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performedFeedback.add(hapticFeedbackType)
        }
    }

    @Test
    fun semanticHapticType_containsAllRequiredVariants() {
        val types = SemanticHapticType.values().toSet()
        assertTrue(types.contains(SemanticHapticType.THRESHOLD_ACTIVATION))
        assertTrue(types.contains(SemanticHapticType.MODE_TOGGLE))
        assertTrue(types.contains(SemanticHapticType.SUCCESS_CONFIRMATION))
        assertTrue(types.contains(SemanticHapticType.EDIT_ENTRY))
        assertTrue(types.contains(SemanticHapticType.DESTRUCTIVE_ACTION))
    }

    @Test
    fun noOpHelloHaptics_performsCleanlyWithoutException() {
        val noOp = NoOpHelloHaptics()
        SemanticHapticType.values().forEach { type ->
            noOp.perform(type)
        }
    }

    @Test
    fun defaultHelloHaptics_whenDisabled_doesNotTriggerHaptics() {
        val recordingHaptic = RecordingHapticFeedback()
        val haptics = DefaultHelloHaptics(
            vibrator = null,
            hapticFeedback = recordingHaptic,
            isEnabled = false
        )

        SemanticHapticType.values().forEach { type ->
            haptics.perform(type)
        }

        assertTrue("Haptics should not be performed when isEnabled = false", recordingHaptic.performedFeedback.isEmpty())
    }

    @Test
    fun defaultHelloHaptics_fallbackToComposeHapticFeedback_mapsAppropriateTypes() {
        val recordingHaptic = RecordingHapticFeedback()
        val haptics = DefaultHelloHaptics(
            vibrator = null,
            hapticFeedback = recordingHaptic,
            isEnabled = true
        )

        // Threshold activation & destructive action map to deliberate LongPress
        haptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
        assertEquals(listOf(HapticFeedbackType.LongPress), recordingHaptic.performedFeedback)

        haptics.perform(SemanticHapticType.DESTRUCTIVE_ACTION)
        assertEquals(
            listOf(HapticFeedbackType.LongPress, HapticFeedbackType.LongPress),
            recordingHaptic.performedFeedback
        )

        // Mode toggle, edit entry, and success confirmation map to crisp TextHandleMove
        haptics.perform(SemanticHapticType.MODE_TOGGLE)
        haptics.perform(SemanticHapticType.EDIT_ENTRY)
        haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)

        assertEquals(
            listOf(
                HapticFeedbackType.LongPress,
                HapticFeedbackType.LongPress,
                HapticFeedbackType.TextHandleMove,
                HapticFeedbackType.TextHandleMove,
                HapticFeedbackType.TextHandleMove
            ),
            recordingHaptic.performedFeedback
        )
    }

    @Test
    fun recordingHelloHaptics_tracksDistinctSemanticEvents() {
        val testHaptics = object : HelloHaptics {
            val events = mutableListOf<SemanticHapticType>()
            override fun perform(type: SemanticHapticType) {
                events.add(type)
            }
        }

        // Simulate user workflow:
        // 1. Pull down to trigger Archive
        testHaptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
        // 2. Toggle between Add Contact and Log Encounter
        testHaptics.perform(SemanticHapticType.MODE_TOGGLE)
        // 3. Click Edit on a contact profile
        testHaptics.perform(SemanticHapticType.EDIT_ENTRY)
        // 4. Save profile edits
        testHaptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
        // 5. Confirm deletion in dialog
        testHaptics.perform(SemanticHapticType.DESTRUCTIVE_ACTION)
        // 6. Tap Undo to restore
        testHaptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)

        assertEquals(
            listOf(
                SemanticHapticType.THRESHOLD_ACTIVATION,
                SemanticHapticType.MODE_TOGGLE,
                SemanticHapticType.EDIT_ENTRY,
                SemanticHapticType.SUCCESS_CONFIRMATION,
                SemanticHapticType.DESTRUCTIVE_ACTION,
                SemanticHapticType.SUCCESS_CONFIRMATION
            ),
            testHaptics.events
        )
    }

    @Test
    fun defaultHelloHaptics_api30Plus_prefersViewHapticFeedbackConstants() {
        val performedConstants = mutableListOf<Int>()
        val recordingComposeHaptic = RecordingHapticFeedback()
        val haptics = DefaultHelloHaptics(
            vibrator = null,
            hapticFeedback = recordingComposeHaptic,
            view = null,
            isEnabled = true,
            sdkInt = Build.VERSION_CODES.R,
            performViewFeedback = { _, constant ->
                performedConstants.add(constant)
                true
            },
            hasViewTarget = true
        )

        SemanticHapticType.values().forEach { haptics.perform(it) }

        assertEquals(
            listOf(
                HapticFeedbackConstants.GESTURE_START,
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.CONTEXT_CLICK,
                HapticFeedbackConstants.REJECT
            ),
            performedConstants
        )
        assertTrue(
            "Compose fallback should not run when View haptic feedback succeeds",
            recordingComposeHaptic.performedFeedback.isEmpty()
        )
    }

    @Test
    fun defaultHelloHaptics_preApi30_usesViewForAvailableConstantsAndFallsBackOtherwise() {
        val performedConstants = mutableListOf<Int>()
        val recordingComposeHaptic = RecordingHapticFeedback()
        val haptics = DefaultHelloHaptics(
            vibrator = null,
            hapticFeedback = recordingComposeHaptic,
            view = null,
            isEnabled = true,
            sdkInt = Build.VERSION_CODES.Q,
            performViewFeedback = { _, constant ->
                performedConstants.add(constant)
                true
            },
            hasViewTarget = true
        )

        haptics.perform(SemanticHapticType.MODE_TOGGLE)
        haptics.perform(SemanticHapticType.EDIT_ENTRY)
        haptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
        haptics.perform(SemanticHapticType.SUCCESS_CONFIRMATION)
        haptics.perform(SemanticHapticType.DESTRUCTIVE_ACTION)

        assertEquals(
            listOf(
                HapticFeedbackConstants.CLOCK_TICK,
                HapticFeedbackConstants.CONTEXT_CLICK
            ),
            performedConstants
        )
        assertEquals(
            listOf(
                HapticFeedbackType.LongPress,
                HapticFeedbackType.TextHandleMove,
                HapticFeedbackType.LongPress
            ),
            recordingComposeHaptic.performedFeedback
        )
    }

    @Test
    fun defaultHelloHaptics_whenDisabled_suppressesViewAndFallbackHaptics() {
        val performedConstants = mutableListOf<Int>()
        val recordingComposeHaptic = RecordingHapticFeedback()
        val haptics = DefaultHelloHaptics(
            vibrator = null,
            hapticFeedback = recordingComposeHaptic,
            view = null,
            isEnabled = false,
            sdkInt = Build.VERSION_CODES.R,
            performViewFeedback = { _, constant ->
                performedConstants.add(constant)
                true
            },
            hasViewTarget = true
        )

        SemanticHapticType.values().forEach { haptics.perform(it) }

        assertTrue(performedConstants.isEmpty())
        assertTrue(recordingComposeHaptic.performedFeedback.isEmpty())
    }
}

