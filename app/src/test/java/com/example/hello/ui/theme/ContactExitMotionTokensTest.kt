package com.example.hello.ui.theme

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactExitMotionTokensTest {

    @Test
    fun archiveTokens_matchSpatialTuckSpecs() {
        assertEquals(180, ContactExitMotionTokens.ARCHIVE_EXIT_FADE_DURATION_MS)
        assertEquals(-0.20f, ContactExitMotionTokens.ARCHIVE_EXIT_SLIDE_FRACTION_X, 0.001f)
        assertEquals(-0.15f, ContactExitMotionTokens.ARCHIVE_EXIT_SLIDE_FRACTION_Y, 0.001f)
        assertSame(FastOutSlowInEasing, ContactExitMotionTokens.ArchiveExitEasing)
    }

    @Test
    fun deleteTokens_matchPhysicalDissolutionSpecs() {
        assertEquals(150, ContactExitMotionTokens.DELETE_EXIT_FADE_DURATION_MS)
        assertEquals(0.92f, ContactExitMotionTokens.DELETE_EXIT_TARGET_SCALE, 0.001f)
        assertSame(LinearOutSlowInEasing, ContactExitMotionTokens.DeleteExitEasing)
    }

    @Test
    fun restoreTokens_matchHorizontalDepartureSpecs() {
        assertEquals(160, ContactExitMotionTokens.RESTORE_EXIT_FADE_DURATION_MS)
        assertEquals(0.25f, ContactExitMotionTokens.RESTORE_EXIT_SLIDE_FRACTION_X, 0.001f)
        assertSame(FastOutSlowInEasing, ContactExitMotionTokens.RestoreExitEasing)
    }

    @Test
    fun springTokens_provideSmoothReflowPhysics() {
        assertEquals(0.82f, ContactExitMotionTokens.PLACEMENT_SPRING_DAMPING, 0.001f)
        assertEquals(Spring.StiffnessMediumLow, ContactExitMotionTokens.PLACEMENT_SPRING_STIFFNESS, 0.001f)
    }

    @Test
    fun durations_stayWithinPerceptionBudget() {
        assertTrue(
            "Archive duration must be <= 250ms for responsive exit",
            ContactExitMotionTokens.ARCHIVE_EXIT_FADE_DURATION_MS <= 250
        )
        assertTrue(
            "Delete duration must be <= 250ms for responsive dissolution",
            ContactExitMotionTokens.DELETE_EXIT_FADE_DURATION_MS <= 250
        )
        assertTrue(
            "Restore duration must be <= 250ms for crisp removal",
            ContactExitMotionTokens.RESTORE_EXIT_FADE_DURATION_MS <= 250
        )
        assertTrue(
            "Target delete scale (${ContactExitMotionTokens.DELETE_EXIT_TARGET_SCALE}) must be non-zero",
            ContactExitMotionTokens.DELETE_EXIT_TARGET_SCALE > 0.8f && ContactExitMotionTokens.DELETE_EXIT_TARGET_SCALE < 1.0f
        )
    }

    @Test
    fun transitions_honorReducedMotionDisabling() {
        assertEquals(ExitTransition.None, ContactExitMotionTokens.archiveExitTransition(isReducedMotion = true))
        assertEquals(ExitTransition.None, ContactExitMotionTokens.deleteExitTransition(isReducedMotion = true))
        assertEquals(ExitTransition.None, ContactExitMotionTokens.restoreExitTransition(isReducedMotion = true))

        assertNotEquals(ExitTransition.None, ContactExitMotionTokens.archiveExitTransition(isReducedMotion = false))
        assertNotEquals(ExitTransition.None, ContactExitMotionTokens.deleteExitTransition(isReducedMotion = false))
        assertNotEquals(ExitTransition.None, ContactExitMotionTokens.restoreExitTransition(isReducedMotion = false))
    }
}

