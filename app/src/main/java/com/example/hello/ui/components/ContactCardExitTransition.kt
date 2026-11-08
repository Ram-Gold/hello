package com.example.hello.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.hello.ui.theme.ContactExitMotionTokens

/**
 * High-level departure animation types for contact cards.
 */
enum class ContactExitType {
    NONE,
    ARCHIVE,
    DELETE,
    RESTORE
}

/**
 * Animated visibility wrapper that choreographs the spatial tuck, dissolution, or restore
 * transitions when contacts leave a directory or archived listing.
 */
@Composable
fun ContactCardExitTransition(
    exitType: ContactExitType,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val exitTransition = when (exitType) {
        ContactExitType.NONE -> ExitTransition.None
        ContactExitType.ARCHIVE -> ContactExitMotionTokens.archiveExitTransition(isReducedMotion)
        ContactExitType.DELETE -> ContactExitMotionTokens.deleteExitTransition(isReducedMotion)
        ContactExitType.RESTORE -> ContactExitMotionTokens.restoreExitTransition(isReducedMotion)
    }

    AnimatedVisibility(
        visible = exitType == ContactExitType.NONE,
        enter = EnterTransition.None,
        exit = exitTransition,
        modifier = modifier
    ) {
        content()
    }
}

