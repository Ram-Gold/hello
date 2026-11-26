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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
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
    const val FADE_IN_DURATION_MS: Int = MotionTokens.AVATAR_SELECTION_FADE_IN_DURATION_MS
    const val FADE_OUT_DURATION_MS: Int = MotionTokens.AVATAR_SELECTION_FADE_OUT_DURATION_MS
    const val INITIAL_SCALE: Float = MotionTokens.AVATAR_SELECTION_INITIAL_SCALE
    const val TARGET_EXIT_SCALE: Float = MotionTokens.AVATAR_SELECTION_EXIT_SCALE
    val EasingCurve: Easing = MotionTokens.AvatarSelectionEasing

    fun enterTransition(isReducedMotion: Boolean = false): EnterTransition {
        if (isReducedMotion) return EnterTransition.None
        return scaleIn(
            animationSpec = tween(
                durationMillis = ENTER_DURATION_MS,
                easing = EasingCurve
            ),
            initialScale = INITIAL_SCALE,
            transformOrigin = TransformOrigin.Center
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FADE_IN_DURATION_MS,
                easing = EasingCurve
            )
        )
    }

    fun exitTransition(isReducedMotion: Boolean = false): ExitTransition {
        if (isReducedMotion) return ExitTransition.None
        return scaleOut(
            animationSpec = tween(
                durationMillis = EXIT_DURATION_MS,
                easing = EasingCurve
            ),
            targetScale = TARGET_EXIT_SCALE,
            transformOrigin = TransformOrigin.Center
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = FADE_OUT_DURATION_MS
            )
        )
    }

    val enterTransition: EnterTransition = enterTransition(isReducedMotion = false)
    val exitTransition: ExitTransition = exitTransition(isReducedMotion = false)
}

/**
 * Composable that wraps [DoodleAvatar] with an animated ease-out selection checkmark overlay.
 *
 * When [isSelected] is true, an opaque black badge with [Icons.Rounded.Check] smoothly
 * scales up from the center and fades in. When unselected, it cleanly scales down and fades out.
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
    isReducedMotion: Boolean = false,
    badgeContainerColor: Color = Color.Black,
    badgeContentColor: Color = Color.White,
    contentDescription: String? = if (isSelected) "Selected" else null
) {
    SelectableAvatarBadge(
        isSelected = isSelected,
        modifier = modifier,
        size = size,
        shape = shape,
        isReducedMotion = isReducedMotion,
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
    isReducedMotion: Boolean = false,
    badgeContainerColor: Color = Color.Black,
    badgeContentColor: Color = Color.White,
    contentDescription: String? = if (isSelected) "Selected" else null,
    enterTransition: EnterTransition = SelectableAvatarBadgeDefaults.enterTransition(isReducedMotion),
    exitTransition: ExitTransition = SelectableAvatarBadgeDefaults.exitTransition(isReducedMotion),
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
                    .background(badgeContainerColor),
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

data class ParticipantAvatar(
    val contactId: String,
    val name: String,
    val initials: String,
    val bgIndex: Int,
    val photoUri: String? = null
)

/**
 * Renders up to 3 overlapping avatars (+ overflow count badge) when 2+ participants
 * are linked to an Encounter or FollowUp, falling back to [SelectableAvatarBadge]
 * when single-participant or selected in Timeline Selection Mode.
 */
@Composable
fun StackedAvatarBadges(
    participants: List<ParticipantAvatar>,
    isSelected: Boolean,
    fallbackInitials: String,
    fallbackBgIndex: Int,
    fallbackPhotoUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    ringColor: Color = MaterialTheme.colorScheme.surfaceContainer
) {
    if (participants.size <= 1 || isSelected) {
        val primary = participants.firstOrNull()
        SelectableAvatarBadge(
            isSelected = isSelected,
            initials = primary?.initials ?: fallbackInitials,
            bgIndex = primary?.bgIndex ?: fallbackBgIndex,
            size = size,
            shape = CircleShape,
            photoUri = primary?.photoUri ?: fallbackPhotoUri,
            modifier = modifier
        )
        return
    }

    val visible = participants.take(3)
    val overflow = (participants.size - visible.size).coerceAtLeast(0)
    val itemSize = 28.dp
    val step = 18.dp
    val badgeCount = visible.size + if (overflow > 0) 1 else 0
    val totalWidth = itemSize + step * (badgeCount - 1)

    Box(
        modifier = modifier
            .width(totalWidth)
            .size(width = totalWidth, height = size),
        contentAlignment = Alignment.CenterStart
    ) {
        visible.forEachIndexed { index, person ->
            Box(
                modifier = Modifier
                    .offset(x = step * index)
                    .size(itemSize)
                    .clip(CircleShape)
                    .border(1.5.dp, ringColor, CircleShape)
            ) {
                DoodleAvatar(
                    initials = person.initials,
                    bgIndex = person.bgIndex,
                    size = itemSize,
                    shape = CircleShape,
                    photoUri = person.photoUri
                )
            }
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .offset(x = step * visible.size)
                    .size(itemSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.5.dp, ringColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflow",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
