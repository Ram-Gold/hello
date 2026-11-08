package com.example.hello.ui.components

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HelloToastHostTest {

    @Test
    fun resolveToastSemanticType_mapsMessagesToCorrectSemanticTypes() {
        assertEquals(ToastSemanticType.ARCHIVE, resolveToastSemanticType("Contact archived"))
        assertEquals(ToastSemanticType.ARCHIVE, resolveToastSemanticType("Archived 1 contact"))

        assertEquals(ToastSemanticType.DELETE, resolveToastSemanticType("Contact deleted"))
        assertEquals(ToastSemanticType.DELETE, resolveToastSemanticType("Deleted 2 contacts"))
        assertEquals(ToastSemanticType.DELETE, resolveToastSemanticType("Deleted 1 encounter"))
        assertEquals(ToastSemanticType.DELETE, resolveToastSemanticType("Photo removed"))

        assertEquals(ToastSemanticType.FAVORITE, resolveToastSemanticType("Added to favorites"))
        assertEquals(ToastSemanticType.FAVORITE, resolveToastSemanticType("Removed from favorites"))
        assertEquals(ToastSemanticType.FAVORITE, resolveToastSemanticType("Starred contact"))

        assertEquals(ToastSemanticType.ALERT, resolveToastSemanticType("Failed to save contact"))
        assertEquals(ToastSemanticType.ALERT, resolveToastSemanticType("No phone number available"))
        assertEquals(ToastSemanticType.ALERT, resolveToastSemanticType("An error occurred"))

        assertEquals(ToastSemanticType.SUCCESS, resolveToastSemanticType("Contact updated"))
        assertEquals(ToastSemanticType.SUCCESS, resolveToastSemanticType("Contact restored"))
        assertEquals(ToastSemanticType.SUCCESS, resolveToastSemanticType("Marked as done!"))
        assertEquals(ToastSemanticType.SUCCESS, resolveToastSemanticType("Export finished"))
    }

    @Test
    fun toastDurationConstants_matchSpecification() {
        assertEquals(7000L, TOAST_DURATION_ACTIONABLE_MS)
        assertEquals(3500L, TOAST_DURATION_INFORMATIONAL_MS)
    }

    @Test
    fun resolveToastDurationMs_actionableMessagesAndLabelsResolveToExtendedDuration() {
        // Any non-blank action label resolves to 7000ms
        assertEquals(7000L, resolveToastDurationMs("Item deleted", actionLabel = "Undo"))
        assertEquals(7000L, resolveToastDurationMs("Contact updated", actionLabel = "Undo"))
        assertEquals(7000L, resolveToastDurationMs("Custom notice", actionLabel = "Retry"))

        // Messages with recoverable keywords resolve to 7000ms even without action label
        assertEquals(7000L, resolveToastDurationMs("Deleted 1 contact"))
        assertEquals(7000L, resolveToastDurationMs("Contact deleted"))
        assertEquals(7000L, resolveToastDurationMs("Archived 1 contact"))
        assertEquals(7000L, resolveToastDurationMs("Contact archived"))
        assertEquals(7000L, resolveToastDurationMs("Contact restored"))
        assertEquals(7000L, resolveToastDurationMs("Encounter restored"))

        // Case-insensitivity check
        assertEquals(7000L, resolveToastDurationMs("DELETED 2 ITEMS"))
        assertEquals(7000L, resolveToastDurationMs("contact ARCHIVED"))
        assertEquals(7000L, resolveToastDurationMs("RESTORED contact"))
    }

    @Test
    fun resolveToastDurationMs_informationalMessagesResolveToStandardDuration() {
        assertEquals(3500L, resolveToastDurationMs("Contact updated"))
        assertEquals(3500L, resolveToastDurationMs("Marked as done!"))
        assertEquals(3500L, resolveToastDurationMs("Export finished"))
        assertEquals(3500L, resolveToastDurationMs("Added to favorites"))
        assertEquals(3500L, resolveToastDurationMs("Failed to save contact"))

        // Blank or whitespace-only action labels do not trigger actionable duration unless message keywords match
        assertEquals(3500L, resolveToastDurationMs("Contact updated", actionLabel = null))
        assertEquals(3500L, resolveToastDurationMs("Contact updated", actionLabel = ""))
        assertEquals(3500L, resolveToastDurationMs("Contact updated", actionLabel = "   "))
    }

    @Test
    fun calculateCountdownProgress_calculatesAccurateProgressFractions() {
        // At start (0ms elapsed), progress is 1.0f
        assertEquals(1.0f, calculateCountdownProgress(elapsedMs = 0L, totalDurationMs = 7000L), 0.001f)

        // At quarter time
        assertEquals(0.75f, calculateCountdownProgress(elapsedMs = 1750L, totalDurationMs = 7000L), 0.001f)

        // At half time
        assertEquals(0.5f, calculateCountdownProgress(elapsedMs = 3500L, totalDurationMs = 7000L), 0.001f)

        // At three-quarter time
        assertEquals(0.25f, calculateCountdownProgress(elapsedMs = 5250L, totalDurationMs = 7000L), 0.001f)

        // At exact completion
        assertEquals(0.0f, calculateCountdownProgress(elapsedMs = 7000L, totalDurationMs = 7000L), 0.001f)

        // Past completion clamps to 0.0f
        assertEquals(0.0f, calculateCountdownProgress(elapsedMs = 8000L, totalDurationMs = 7000L), 0.001f)
        assertEquals(0.0f, calculateCountdownProgress(elapsedMs = 15000L, totalDurationMs = 7000L), 0.001f)

        // Negative elapsed clamps to 1.0f
        assertEquals(1.0f, calculateCountdownProgress(elapsedMs = -100L, totalDurationMs = 7000L), 0.001f)

        // Zero or negative total duration returns 0.0f
        assertEquals(0.0f, calculateCountdownProgress(elapsedMs = 0L, totalDurationMs = 0L), 0.001f)
        assertEquals(0.0f, calculateCountdownProgress(elapsedMs = 100L, totalDurationMs = -500L), 0.001f)
    }

    @Test
    fun resolveProgressTone_mapsSemanticTypesToAppropriateTones() {
        assertEquals(ToastProgressTone.ERROR, resolveProgressTone(ToastSemanticType.DELETE))
        assertEquals(ToastProgressTone.ERROR, resolveProgressTone(ToastSemanticType.ALERT))
        assertEquals(ToastProgressTone.SECONDARY, resolveProgressTone(ToastSemanticType.ARCHIVE))
        assertEquals(ToastProgressTone.PRIMARY, resolveProgressTone(ToastSemanticType.SUCCESS))
        assertEquals(ToastProgressTone.PRIMARY, resolveProgressTone(ToastSemanticType.FAVORITE))
    }

    @Test
    fun resolveProgressColor_mapsColorSchemeToProgressTones() {
        val testScheme = lightColorScheme(
            primary = Color(0xFFE06D3B),
            secondary = Color(0xFFF3A261),
            error = Color(0xFFBA1A1A)
        )

        assertEquals(testScheme.error, resolveProgressColor(ToastSemanticType.DELETE, testScheme))
        assertEquals(testScheme.error, resolveProgressColor(ToastSemanticType.ALERT, testScheme))
        assertEquals(testScheme.secondary, resolveProgressColor(ToastSemanticType.ARCHIVE, testScheme))
        assertEquals(testScheme.primary, resolveProgressColor(ToastSemanticType.SUCCESS, testScheme))
        assertEquals(testScheme.primary, resolveProgressColor(ToastSemanticType.FAVORITE, testScheme))
    }

    @Test
    fun shouldShowToastCountdown_actionableMessagesAndLabelsReturnTrue() {
        // Any non-blank action label displays countdown progress bar
        assertTrue(shouldShowToastCountdown("Contact updated", actionLabel = "Undo"))
        assertTrue(shouldShowToastCountdown("Encounter saved", actionLabel = "Details"))
        assertTrue(shouldShowToastCountdown("Item deleted", actionLabel = "Undo"))

        // Recoverable messages with default 7000ms duration display countdown progress even without actionLabel
        assertTrue(shouldShowToastCountdown("Contact deleted"))
        assertTrue(shouldShowToastCountdown("Archived 1 contact"))
        assertTrue(shouldShowToastCountdown("Contact restored"))
        assertTrue(shouldShowToastCountdown("Encounter restored"))
    }

    @Test
    fun shouldShowToastCountdown_informationalMessagesWithoutActionReturnFalse() {
        assertFalse(shouldShowToastCountdown("Contact updated"))
        assertFalse(shouldShowToastCountdown("Marked as done!"))
        assertFalse(shouldShowToastCountdown("Export finished"))
        assertFalse(shouldShowToastCountdown("Added to favorites"))
        assertFalse(shouldShowToastCountdown("Failed to save contact"))

        // Blank or whitespace-only action labels on non-recoverable messages return false
        assertFalse(shouldShowToastCountdown("Contact updated", actionLabel = null))
        assertFalse(shouldShowToastCountdown("Contact updated", actionLabel = ""))
        assertFalse(shouldShowToastCountdown("Contact updated", actionLabel = "   "))
    }

    @Test
    fun shouldShowToastCountdown_customDurationsRespectThreshold() {
        // Custom duration > 3500ms shows countdown bar
        assertTrue(shouldShowToastCountdown("Custom message", null, durationMs = 3501L))
        assertTrue(shouldShowToastCountdown("Custom message", null, durationMs = 7000L))

        // Custom duration <= 3500ms does not show countdown bar unless actionLabel is present
        assertFalse(shouldShowToastCountdown("Custom message", null, durationMs = 3500L))
        assertFalse(shouldShowToastCountdown("Custom message", null, durationMs = 2000L))
        assertTrue(shouldShowToastCountdown("Custom message", actionLabel = "Undo", durationMs = 2000L))
    }
}


