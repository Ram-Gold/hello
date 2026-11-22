package com.example.hello.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.hello.R
import com.example.hello.ui.theme.HelloTheme

data class NotificationExplainerCopy(
    val title: String,
    val body: String,
    val confirmLabel: String,
    val dismissLabel: String = "Not Now"
)

fun resolveNotificationExplainerCopy(openSystemSettings: Boolean): NotificationExplainerCopy {
    return if (openSystemSettings) {
        NotificationExplainerCopy(
            title = "Enable Reminder Alerts",
            body = "Your reminder is saved in Hello, but notifications are currently turned off in Android settings. Enable them to receive on-device alerts when reminders are due.",
            confirmLabel = "Open Settings"
        )
    } else {
        NotificationExplainerCopy(
            title = "Never Miss a Reminder",
            body = "Hello sends local notifications strictly for reminders you schedule. Everything runs 100% on your device with no servers or tracking.",
            confirmLabel = "Allow"
        )
    }
}

/**
 * In-app explainer dialog shown prior to triggering the Android POST_NOTIFICATIONS
 * runtime permission dialog or system Notification Settings (Prime Directive 3, ADR-0037).
 */
@Composable
fun NotificationExplainerDialog(
    openSystemSettings: Boolean = false,
    onAllowClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismissClick) {
        NotificationExplainerCard(
            openSystemSettings = openSystemSettings,
            onAllowClick = onAllowClick,
            onDismissClick = onDismissClick
        )
    }
}

@Composable
internal fun NotificationExplainerCard(
    openSystemSettings: Boolean = false,
    onAllowClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = resolveNotificationExplainerCopy(openSystemSettings)
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyStateIllustration(
                drawableRes = R.drawable.ic_due_date_reminder,
                height = 88.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = copy.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = copy.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismissClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = copy.dismissLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAllowClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = copy.confirmLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Preview(name = "Never Miss a Reminder Light", showBackground = true, widthDp = 360)
@Composable
private fun NotificationExplainerDialogLightPreview() {
    HelloTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(16.dp)) {
            NotificationExplainerCard(
                openSystemSettings = false,
                onAllowClick = {},
                onDismissClick = {}
            )
        }
    }
}

@Preview(
    name = "Enable Reminder Alerts Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 360
)
@Composable
private fun NotificationExplainerDialogDarkPreview() {
    HelloTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(16.dp)) {
            NotificationExplainerCard(
                openSystemSettings = true,
                onAllowClick = {},
                onDismissClick = {}
            )
        }
    }
}
