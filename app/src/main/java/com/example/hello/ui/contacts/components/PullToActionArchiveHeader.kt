package com.example.hello.ui.contacts.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hello.ui.components.PullToActionState
import com.example.hello.ui.haptics.LocalHelloHaptics
import com.example.hello.ui.haptics.SemanticHapticType

/**
 * Scroll-driven reveal header displayed above the contacts list during overscroll.
 *
 * Scales and rotates the archive indicator dynamically based on pull progress.
 * Transitions state and triggers haptics when crossing the activation threshold.
 */
@Composable
fun PullToActionArchiveHeader(
    state: PullToActionState,
    modifier: Modifier = Modifier
) {
    if (state.pullOffsetPx <= 0f) return

    val density = LocalDensity.current
    val haptics = LocalHelloHaptics.current

    LaunchedEffect(state.isTriggered) {
        if (state.isTriggered) {
            haptics.perform(SemanticHapticType.THRESHOLD_ACTIVATION)
        }
    }

    val heightDp = with(density) { state.pullOffsetPx.toDp() }
    val progress = state.progress
    val isTriggered = state.isTriggered

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val alpha = (progress * 1.5f).coerceIn(0f, 1f)
        val scale = (0.75f + progress * 0.35f).coerceIn(0.75f, 1.15f)
        val rotation = progress * 180f

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isTriggered) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            modifier = Modifier
                .graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = scale
                    this.scaleY = scale
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Archive,
                    contentDescription = null,
                    tint = if (isTriggered) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedContent(
                    targetState = isTriggered,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "PullToActionLabel"
                ) { triggered ->
                    Text(
                        text = if (triggered) "Release for archive" else "Pull for archive",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = if (triggered) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

