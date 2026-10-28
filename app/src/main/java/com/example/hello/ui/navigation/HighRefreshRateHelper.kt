package com.example.hello.ui.navigation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Finds the nearest Activity from a Context.
 */
fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Dual-layer High Refresh Rate effect (120Hz).
 * When active (e.g. while pager is animating between tabs), sets window.attributes.preferredRefreshRate = 120f
 * on Android 11+ (API 30+) devices, forcing LTPO/VRR displays to ramp up to 120Hz even for non-touch programmatic tab clicks.
 * When the animation settles, resets to original rate (or 0f) to restore system adaptive power savings.
 */
@Composable
fun HighRefreshRateEffect(isActive: Boolean) {
    val context = LocalContext.current
    val window: Window? = context.findActivity()?.window

    DisposableEffect(isActive, window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && window != null && isActive) {
            val originalRate = window.attributes.preferredRefreshRate
            window.attributes = window.attributes.apply {
                preferredRefreshRate = 120f
            }
            onDispose {
                try {
                    window.attributes = window.attributes.apply {
                        preferredRefreshRate = originalRate
                    }
                } catch (_: Exception) {}
            }
        } else {
            onDispose { }
        }
    }
}
