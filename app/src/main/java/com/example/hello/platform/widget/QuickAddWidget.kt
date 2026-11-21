package com.example.hello.platform.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.unit.ColorProvider
import com.example.hello.R
import com.example.hello.ui.quickaction.QuickCaptureActivity

/**
 * Native 1x1 Glance App Widget for the Android home screen.
 * Styled as a crisp Minimalist (#121212) rounded squircle with the Hello name-tag symbol
 * that launches directly into the Add Contact step-by-step flow in <50ms.
 */
class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val minimalistPrimary = Color(0xFF121212)
            val white = Color(0xFFFFFFFF)

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(minimalistPrimary))
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<QuickCaptureActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_hello_mark),
                    contentDescription = "Quick Add Contact",
                    colorFilter = ColorFilter.tint(ColorProvider(white)),
                    modifier = GlanceModifier.size(36.dp)
                )
            }
        }
    }
}

/**
 * AppWidgetProviderReceiver for QuickAddWidget.
 */
class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}
