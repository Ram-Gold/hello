package com.example.hello.ui.haptics

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
}

