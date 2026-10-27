package com.example.hello

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.hello.platform.notification.FollowUpNotificationHelper
import com.example.hello.ui.MainAppShell
import com.example.hello.ui.theme.HelloTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialContactId = if (intent?.action == FollowUpNotificationHelper.ACTION_VIEW_CONTACT) {
            intent.getStringExtra(FollowUpNotificationHelper.EXTRA_CONTACT_ID)
        } else null

        setContent {
            HelloTheme {
                MainAppShell(initialContactId = initialContactId)
            }
        }
    }
}
