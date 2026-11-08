package com.example.hello.ui.haptics

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * High-level semantic interaction types that produce tactile haptic feedback.
 */
enum class SemanticHapticType {
    /** Pull-to-action threshold breach (e.g. pulling down to access Archived Contacts). */
    THRESHOLD_ACTIVATION,

    /** Subtle tactile tick when toggling workflow modes or tabs (e.g. Add Contact <-> Log Encounter). */
    MODE_TOGGLE,

    /** Affirmative confirmation pulse upon saving new records, logging encounters, or restoring entities. */
    SUCCESS_CONFIRMATION,

    /** Distinct tactile tap when entering edit screens or activating selection mode. */
    EDIT_ENTRY,

    /** Strong, deliberate warning vibration for destructive actions (archive confirmation, deletion). */
    DESTRUCTIVE_ACTION
}

/**
 * Centralized interface for delivering semantic tactile feedback across Hello CRM.
 */
interface HelloHaptics {
    fun perform(type: SemanticHapticType)
}

/**
 * No-op implementation of [HelloHaptics] for headless testing or disabled vibration environments.
 */
class NoOpHelloHaptics : HelloHaptics {
    override fun perform(type: SemanticHapticType) {
        // No-op
    }
}

/**
 * Standard implementation that uses platform [Vibrator] (with pre-baked [VibrationEffect]s on API 29+)
 * and falls back to Compose [HapticFeedback] when hardware vibrator is unavailable or unprivileged.
 */
class DefaultHelloHaptics(
    private val vibrator: Vibrator? = null,
    private val hapticFeedback: HapticFeedback? = null,
    val isEnabled: Boolean = true
) : HelloHaptics {

    override fun perform(type: SemanticHapticType) {
        if (!isEnabled) return

        val vibrated = tryPerformVibrator(type)
        if (!vibrated) {
            tryPerformComposeHaptic(type)
        }
    }

    private fun tryPerformVibrator(type: SemanticHapticType): Boolean {
        val v = vibrator ?: return false
        if (!v.hasVibrator()) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (type) {
                    SemanticHapticType.THRESHOLD_ACTIVATION ->
                        VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)
                    SemanticHapticType.MODE_TOGGLE ->
                        VibrationEffect.createOneShot(8L, 90)
                    SemanticHapticType.SUCCESS_CONFIRMATION ->
                        VibrationEffect.createWaveform(longArrayOf(0, 12, 40, 18), intArrayOf(0, 180, 0, 220), -1)
                    SemanticHapticType.EDIT_ENTRY ->
                        VibrationEffect.createOneShot(12L, 140)
                    SemanticHapticType.DESTRUCTIVE_ACTION ->
                        VibrationEffect.createWaveform(longArrayOf(0, 35, 20, 45), intArrayOf(0, 255, 0, 255), -1)
                }
                v.vibrate(effect)
                true
            } else {
                // Legacy fallback for pre-API 26
                val pattern = when (type) {
                    SemanticHapticType.THRESHOLD_ACTIVATION -> 15L
                    SemanticHapticType.MODE_TOGGLE -> 8L
                    SemanticHapticType.SUCCESS_CONFIRMATION -> 25L
                    SemanticHapticType.EDIT_ENTRY -> 12L
                    SemanticHapticType.DESTRUCTIVE_ACTION -> 40L
                }
                @Suppress("DEPRECATION")
                v.vibrate(pattern)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun tryPerformComposeHaptic(type: SemanticHapticType): Boolean {
        val haptic = hapticFeedback ?: return false
        return try {
            when (type) {
                SemanticHapticType.THRESHOLD_ACTIVATION,
                SemanticHapticType.DESTRUCTIVE_ACTION -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                SemanticHapticType.MODE_TOGGLE,
                SemanticHapticType.EDIT_ENTRY,
                SemanticHapticType.SUCCESS_CONFIRMATION -> {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * CompositionLocal providing access to the current [HelloHaptics] dispatcher.
 */
val LocalHelloHaptics: ProvidableCompositionLocal<HelloHaptics> = staticCompositionLocalOf {
    NoOpHelloHaptics()
}

/**
 * Remembers a platform [HelloHaptics] instance backed by the ambient [Context] and Compose [HapticFeedback].
 */
@Composable
fun rememberHelloHaptics(isEnabled: Boolean = true): HelloHaptics {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    return remember(context, hapticFeedback, isEnabled) {
        val vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }

        DefaultHelloHaptics(
            vibrator = vibrator,
            hapticFeedback = hapticFeedback,
            isEnabled = isEnabled
        )
    }
}
