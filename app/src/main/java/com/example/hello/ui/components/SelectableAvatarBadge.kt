package com.example.hello.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hello.ui.home.components.DoodleAvatar
import com.example.hello.ui.theme.MotionTokens

/**
 * Motion specifications and defaults for [SelectableAvatarBadge].
 *
 * Implements an ease-out enter transition (FastOutSlowInEasing, initial scale 0.45x, 180ms)
 * and exit transition (FastOutSlowInEasing, target scale 0.45x, 140ms) for responsive,
 * tactile multi-selection feedback.
 */
object SelectableAvatarBadgeDefaults {
    const val ENTER_DURATION_MS: Int = MotionTokens.AVATAR_SELECTION_ENTER_DURATION_MS
    const val EXIT_DURATION_MS: Int = MotionTokens.AVATAR_SELECTION_EXIT_DURATION_MS
    const val INITIAL_SCALE: Float = MotionTokens.AVATAR_SELECTION_INITIAL_SCALE
    const val TARGET_EXIT_SCALE: Float = MotionTokens.AVATAR_SELECTION_EXIT_SCALE
    val EasingCurve: Easing = MotionTokens.AvatarSelectionEasing

    val enterTransition: EnterTransition = scaleIn(
        animationSpec = tween(
            durationMillis = ENTER_DURATION_MS,
            easing = EasingCurve
        ),
        initialScale = INITIAL_SCALE
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = ENTER_DURATION_MS,
            easing = EasingCurve
        )
    )

    val exitTransition: ExitTransition = scaleOut(
        animationSpec = tween(
            durationMillis = EXIT_DURATION_MS,
            easing = EasingCurve
        ),
        targetScale = TARGET_EXIT_SCALE
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = EXIT_DURATION_MS,
            easing = EasingCurve
        )
    )
}

/**
 * Composable that wraps [DoodleAvatar] with an animated ease-out selection checkmark overlay.
 *
 * When [isSelected] is true, an accent-colored badge with [Icons.Rounded.Check] smoothly
 * scales and fades in. When unselected, it cleanly scales down and fades out.
 */
@Composable
fun SelectableAvatarBadge(
    isSelected: Boolean,
    initials: String,
    bgIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = CircleShape,
    photoUri: String? = null,
    badgeContainerColor: Color = MaterialTheme.colorScheme.primary,
    badgeContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentDescription: String? = if (isSelected) "Selected" else null
) {
    SelectableAvatarBadge(
        isSelected = isSelected,
        modifier = modifier,
        size = size,
        shape = shape,
        badgeContainerColor = badgeContainerColor,
        badgeContentColor = badgeContentColor,
        contentDescription = contentDescription
    ) {
        DoodleAvatar(
            initials = initials,
            bgIndex = bgIndex,
            size = size,
            shape = shape,
            photoUri = photoUri
        )
    }
}

/**
 * Composable container overload for [SelectableAvatarBadge] that overlays an animated
 * ease-out selection checkmark on top of any arbitrary [avatar] composable.
 */
@Composable
fun SelectableAvatarBadge(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = CircleShape,
    badgeContainerColor: Color = MaterialTheme.colorScheme.primary,
    badgeContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentDescription: String? = if (isSelected) "Selected" else null,
    enterTransition: EnterTransition = SelectableAvatarBadgeDefaults.enterTransition,
    exitTransition: ExitTransition = SelectableAvatarBadgeDefaults.exitTransition,
    avatar: @Composable () -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        avatar()

        AnimatedVisibility(
            visible = isSelected,
            enter = enterTransition,
            exit = exitTransition
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(badgeContainerColor)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = contentDescription,
                    tint = badgeContentColor,
                    modifier = Modifier.size(size * 0.48f)
                )
            }
        }
    }
}
