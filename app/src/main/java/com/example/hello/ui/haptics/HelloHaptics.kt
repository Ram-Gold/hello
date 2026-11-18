package com.example.hello.ui.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

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
 * Standard implementation that prefers OS-integrated [View.performHapticFeedback] with semantic
 * [HapticFeedbackConstants] on supported API levels, falling back to hardware [Vibrator]
 * (pre-baked [VibrationEffect]s on API 29+ and waveforms on API 26+) and Compose [HapticFeedback].
 */
class DefaultHelloHaptics(
    private val vibrator: Vibrator? = null,
    private val hapticFeedback: HapticFeedback? = null,
    private val view: View? = null,
    val isEnabled: Boolean = true,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val performViewFeedback: (View?, Int) -> Boolean = { targetView, constant ->
        targetView?.performHapticFeedback(constant) == true
    },
    private val hasViewTarget: Boolean = view != null
) : HelloHaptics {

    override fun perform(type: SemanticHapticType) {
        if (!isEnabled) return

        if (tryPerformViewHaptic(type)) return
        if (tryPerformVibrator(type)) return
        tryPerformComposeHaptic(type)
    }

    @ChecksSdkIntAtLeast(parameter = 0)
    private fun isAtLeastApi(apiLevel: Int): Boolean = sdkInt >= apiLevel

    private fun resolveViewHapticConstant(type: SemanticHapticType): Int? {
        return when (type) {
            SemanticHapticType.THRESHOLD_ACTIVATION ->
                if (isAtLeastApi(Build.VERSION_CODES.R)) HapticFeedbackConstants.GESTURE_START else null
            SemanticHapticType.MODE_TOGGLE ->
                HapticFeedbackConstants.CLOCK_TICK
            SemanticHapticType.SUCCESS_CONFIRMATION ->
                if (isAtLeastApi(Build.VERSION_CODES.R)) HapticFeedbackConstants.CONFIRM else null
            SemanticHapticType.EDIT_ENTRY ->
                HapticFeedbackConstants.CONTEXT_CLICK
            SemanticHapticType.DESTRUCTIVE_ACTION ->
                if (isAtLeastApi(Build.VERSION_CODES.R)) HapticFeedbackConstants.REJECT else null
        }
    }

    private fun tryPerformViewHaptic(type: SemanticHapticType): Boolean {
        if (!hasViewTarget) return false
        val constant = resolveViewHapticConstant(type) ?: return false
        return try {
            performViewFeedback(view, constant)
        } catch (_: Exception) {
            false
        }
    }

    private fun tryPerformVibrator(type: SemanticHapticType): Boolean {
        if (view != null && !view.isHapticFeedbackEnabled) return false
        val v = vibrator ?: return false
        if (!v.hasVibrator()) return false

        return try {
            if (isAtLeastApi(Build.VERSION_CODES.Q)) {
                val effect = when (type) {
                    SemanticHapticType.THRESHOLD_ACTIVATION ->
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    SemanticHapticType.MODE_TOGGLE ->
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    SemanticHapticType.SUCCESS_CONFIRMATION ->
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                    SemanticHapticType.EDIT_ENTRY ->
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    SemanticHapticType.DESTRUCTIVE_ACTION ->
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                }
                v.vibrate(effect)
                true
            } else if (isAtLeastApi(Build.VERSION_CODES.O)) {
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
    val view = LocalView.current

    return remember(context, hapticFeedback, view, isEnabled) {
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
            view = view,
            isEnabled = isEnabled
        )
    }
}
