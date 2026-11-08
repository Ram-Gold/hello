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
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standard display duration constants for Toast Pills (ADR-0027 & countdown timer spec).
 */
const val TOAST_DURATION_ACTIONABLE_MS: Long = 7000L
const val TOAST_DURATION_INFORMATIONAL_MS: Long = 3500L

/**
 * Resolves the toast display duration based on user intent and action recoverability.
 * Actionable messages (or those with non-blank action labels) receive 7000ms to allow
 * ample time to register and tap "Undo". Informational notices receive standard 3500ms.
 */
fun resolveToastDurationMs(message: String, actionLabel: String? = null): Long {
    if (!actionLabel.isNullOrBlank()) {
        return TOAST_DURATION_ACTIONABLE_MS
    }
    val isRecoverableAction = message.contains("deleted", ignoreCase = true) ||
        message.contains("archived", ignoreCase = true) ||
        message.contains("restored", ignoreCase = true)

    return if (isRecoverableAction) {
        TOAST_DURATION_ACTIONABLE_MS
    } else {
        TOAST_DURATION_INFORMATIONAL_MS
    }
}

/**
 * Linearly computes the remaining fraction of the countdown progress bar.
 * Starts at 1.0f when elapsedMs = 0, and linearly drops to 0.0f when elapsedMs >= totalDurationMs.
 * Clamped strictly between 0.0f and 1.0f.
 */
fun calculateCountdownProgress(elapsedMs: Long, totalDurationMs: Long): Float {
    if (totalDurationMs <= 0L) return 0.0f
    val remainingMs = totalDurationMs - elapsedMs
    return (remainingMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0.0f, 1.0f)
}

/**
 * Semantic tone classification for countdown progress tracks.
 */
enum class ToastProgressTone {
    PRIMARY,
    SECONDARY,
    ERROR
}

/**
 * Resolves the progress track semantic tone based on the toast semantic classification:
 * - DELETE / ALERT -> ERROR
 * - ARCHIVE -> SECONDARY
 * - FAVORITE / SUCCESS -> PRIMARY
 */
fun resolveProgressTone(semanticType: ToastSemanticType): ToastProgressTone = when (semanticType) {
    ToastSemanticType.DELETE, ToastSemanticType.ALERT -> ToastProgressTone.ERROR
    ToastSemanticType.ARCHIVE -> ToastProgressTone.SECONDARY
    ToastSemanticType.FAVORITE, ToastSemanticType.SUCCESS -> ToastProgressTone.PRIMARY
}

/**
 * Maps semantic type to concrete theme color given a [ColorScheme].
 */
fun resolveProgressColor(semanticType: ToastSemanticType, colorScheme: ColorScheme): Color =
    when (resolveProgressTone(semanticType)) {
        ToastProgressTone.ERROR -> colorScheme.error
        ToastProgressTone.SECONDARY -> colorScheme.secondary
        ToastProgressTone.PRIMARY -> colorScheme.primary
    }

/**
 * Composable helper resolving progress color from current [MaterialTheme].
 */
@Composable
fun resolveProgressColor(semanticType: ToastSemanticType): Color =
    resolveProgressColor(semanticType, MaterialTheme.colorScheme)

/**
 * Semantic type classification for Toast Pill leading status icons (ADR-0027).
 */
enum class ToastSemanticType {
    ARCHIVE,
    DELETE,
    FAVORITE,
    ALERT,
    SUCCESS
}

fun resolveToastSemanticType(message: String): ToastSemanticType = when {
    message.contains("archive", ignoreCase = true) -> ToastSemanticType.ARCHIVE
    message.contains("favorit", ignoreCase = true) || message.contains("star", ignoreCase = true) -> ToastSemanticType.FAVORITE
    message.contains("delete", ignoreCase = true) || message.contains("remove", ignoreCase = true) -> ToastSemanticType.DELETE
    message.contains("error", ignoreCase = true) ||
        message.contains("fail", ignoreCase = true) ||
        message.contains("no ", ignoreCase = true) -> ToastSemanticType.ALERT
    else -> ToastSemanticType.SUCCESS
}

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
    val semanticType = resolveToastSemanticType(message)
    val leadingIcon = when (semanticType) {
        ToastSemanticType.ARCHIVE -> Icons.Rounded.Archive
        ToastSemanticType.DELETE -> Icons.Rounded.Delete
        ToastSemanticType.FAVORITE -> Icons.Rounded.Star
        ToastSemanticType.ALERT -> Icons.Rounded.Info
        ToastSemanticType.SUCCESS -> Icons.Rounded.CheckCircle
    }
    val iconTint = when (semanticType) {
        ToastSemanticType.DELETE, ToastSemanticType.ALERT -> MaterialTheme.colorScheme.error
        ToastSemanticType.ARCHIVE -> MaterialTheme.colorScheme.secondary
        ToastSemanticType.FAVORITE, ToastSemanticType.SUCCESS -> MaterialTheme.colorScheme.primary
    }

    val pillShape = RoundedCornerShape(24.dp)

    Surface(
        shape = pillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .padding(horizontal = 24.dp)
            .shadow(
                elevation = 4.dp,
                shape = pillShape,
                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)
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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

