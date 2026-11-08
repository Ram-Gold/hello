package com.example.hello.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull

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
    // Informational notices (including restoration confirmations without an action label)
    // dismiss quickly (3500ms) without an extended countdown window.
    val isRecoverableDeletionOrArchive = (message.contains("deleted", ignoreCase = true) ||
        message.contains("archived", ignoreCase = true)) &&
        !message.contains("restored", ignoreCase = true)

    return if (isRecoverableDeletionOrArchive) {
        TOAST_DURATION_ACTIONABLE_MS
    } else {
        TOAST_DURATION_INFORMATIONAL_MS
    }
}

/**
 * Determines whether the slim animated countdown progress bar should be displayed.
 * Only actionable toasts (with a non-blank [actionLabel] or [durationMs] > [TOAST_DURATION_INFORMATIONAL_MS])
 * display the bottom countdown progress line; non-actionable toasts remain clean without any countdown line.
 * Restoration confirmations without an action button never display a countdown progress bar.
 */
fun shouldShowToastCountdown(
    message: String,
    actionLabel: String? = null,
    durationMs: Long = resolveToastDurationMs(message, actionLabel)
): Boolean {
    if (actionLabel.isNullOrBlank() && message.contains("restored", ignoreCase = true)) {
        return false
    }
    return !actionLabel.isNullOrBlank() || durationMs > TOAST_DURATION_INFORMATIONAL_MS
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

const val TOAST_PILL_TEST_TAG = "HelloToastPill"
const val TOAST_COUNTDOWN_BAR_TEST_TAG = "HelloToastCountdownBar"

/**
 * Custom [SnackbarVisuals] carrying the explicit countdown display duration.
 */
class HelloToastVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    val durationMs: Long = resolveToastDurationMs(message, actionLabel),
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
) : SnackbarVisuals

/**
 * Displays a Toast Pill with an intentional display duration matching the countdown progress bar.
 * Uses [withTimeoutOrNull] around [SnackbarHostState.showSnackbar] so that actionable messages
 * stay visible for the full [durationMs] (e.g. 7000ms), and cancels cleanly when timeout elapses
 * if no user action was performed.
 */
suspend fun SnackbarHostState.showHelloToast(
    message: String,
    actionLabel: String? = null,
    durationMs: Long = resolveToastDurationMs(message, actionLabel)
): SnackbarResult {
    val visuals = HelloToastVisuals(
        message = message,
        actionLabel = actionLabel,
        durationMs = durationMs,
        duration = SnackbarDuration.Indefinite
    )
    val result = withTimeoutOrNull(durationMs) {
        showSnackbar(visuals)
    }
    return result ?: run {
        currentSnackbarData?.dismiss()
        SnackbarResult.Dismissed
    }
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
        val message = snackbarData.visuals.message
        val actionLabel = snackbarData.visuals.actionLabel
        val durationMs = (snackbarData.visuals as? HelloToastVisuals)?.durationMs
            ?: resolveToastDurationMs(message, actionLabel)
        HelloToastPill(
            message = message,
            actionLabel = actionLabel,
            durationMs = durationMs,
            onAction = { snackbarData.performAction() }
        )
    }
}

/**
 * Slim animated countdown progress track rendered along the bottom edge of the Toast Pill capsule.
 * Height defaults to 2.5dp and is cleanly clipped to the pill capsule shape.
 */
@Composable
fun HelloToastCountdownBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = color.copy(alpha = 0.15f),
    height: Dp = 2.5.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .testTag(TOAST_COUNTDOWN_BAR_TEST_TAG)
    ) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        // Background track
        drawRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size
        )
        // Active countdown line shrinking from full width down to 0
        if (clampedProgress > 0f) {
            drawRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(width = size.width * clampedProgress, height = size.height)
            )
        }
    }
}

@Composable
fun HelloToastPill(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    durationMs: Long = resolveToastDurationMs(message, actionLabel),
    progressOverride: Float? = null,
    onAction: () -> Unit = {}
) {
    val semanticType = resolveToastSemanticType(message)
    val leadingIcon = when (semanticType) {
        ToastSemanticType.ARCHIVE -> Icons.Rounded.Archive
        ToastSemanticType.DELETE -> Icons.Rounded.Delete
        ToastSemanticType.FAVORITE -> Icons.Rounded.Star
        ToastSemanticType.ALERT -> Icons.Rounded.Info
        ToastSemanticType.SUCCESS -> Icons.Rounded.CheckCircle
    }
    val iconTint = resolveProgressColor(semanticType)
    val progressColor = iconTint
    val showCountdown = shouldShowToastCountdown(message, actionLabel, durationMs)

    val progressAnimatable = remember(message) { Animatable(1.0f) }

    LaunchedEffect(message, durationMs, showCountdown) {
        if (showCountdown && progressOverride == null) {
            progressAnimatable.snapTo(1.0f)
            progressAnimatable.animateTo(
                targetValue = 0.0f,
                animationSpec = tween(
                    durationMillis = durationMs.toInt().coerceAtLeast(0),
                    easing = LinearEasing
                )
            )
        }
    }

    val currentProgress = progressOverride ?: progressAnimatable.value
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
            .clip(pillShape)
            .widthIn(max = 380.dp)
            .testTag(TOAST_PILL_TEST_TAG)
    ) {
        Box(
            modifier = Modifier.clip(pillShape)
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

            if (showCountdown) {
                HelloToastCountdownBar(
                    progress = currentProgress,
                    color = progressColor,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
        }
    }
}

