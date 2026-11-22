package com.example.hello.platform.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import com.example.hello.R
import com.example.hello.ui.quickaction.QuickCaptureActivity

internal val QuickAddFallbackLightColors = lightColorScheme(
    primaryContainer = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF121212)
)

internal val QuickAddFallbackDarkColors = darkColorScheme(
    primaryContainer = Color(0xFF121212),
    onPrimaryContainer = Color(0xFFFFFFFF)
)

internal val QuickAddFallbackColorProviders = ColorProviders(
    light = QuickAddFallbackLightColors,
    dark = QuickAddFallbackDarkColors
)

/**
 * Native 1x1 Glance App Widget for the Android home screen.
 * Uses Material 3 dynamic colors (`primaryContainer` / `onPrimaryContainer`) on Android 12+
 * with a Day/Night monochrome fallback (#FFFFFF/#121212 in Light, #121212/#FFFFFF in Dark)
 * on Android 7-11, launching directly into QuickCaptureActivity in <50ms.
 */
class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(
                colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    GlanceTheme.colors
                } else {
                    QuickAddFallbackColorProviders
                }
            ) {
                QuickAddWidgetContent()
            }
        }
    }
}

@Composable
private fun QuickAddWidgetContent() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<QuickCaptureActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_hello_mark),
            contentDescription = "Quick Add Contact",
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
            modifier = GlanceModifier.size(36.dp)
        )
    }
}

/**
 * AppWidgetProviderReceiver for QuickAddWidget.
 */
class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}
