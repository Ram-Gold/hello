package com.example.hello.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable HelloToastHost component implementing the Toast Pill canonical design (ADR-0026):
 * - Elevated tactile pill capsule (RoundedCornerShape(24.dp))
 * - Warm earthy border and subtle shadow for physical depth
 * - Subtle leading status icon paired with concise message text
 * - Dynamic adaptive clearance preventing overlap with FloatingBottomDock or system navigation bars
 */
@Composable
fun HelloToastHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
    ) { snackbarData ->
        HelloToastPill(
            message = snackbarData.visuals.message,
            actionLabel = snackbarData.visuals.actionLabel,
            onAction = { snackbarData.performAction() },
            onDismiss = { snackbarData.dismiss() }
        )
    }
}

@Composable
fun HelloToastPill(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val isAlert = message.contains("error", ignoreCase = true) ||
        message.contains("fail", ignoreCase = true) ||
        message.contains("no ", ignoreCase = true)

    val leadingIcon = if (isAlert) Icons.Rounded.Info else Icons.Rounded.CheckCircle
    val iconTint = if (isAlert) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val pillShape = RoundedCornerShape(24.dp)

    Surface(
        shape = pillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .padding(horizontal = 24.dp)
            .shadow(
                elevation = 6.dp,
                shape = pillShape,
                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f),
                spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.18f)
            )
            .widthIn(max = 380.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!actionLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = onAction,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

