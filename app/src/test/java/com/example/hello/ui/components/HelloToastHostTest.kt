package com.example.hello.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

        // Messages with recoverable deletion/archive keywords resolve to 7000ms even without action label
        assertEquals(7000L, resolveToastDurationMs("Deleted 1 contact"))
        assertEquals(7000L, resolveToastDurationMs("Contact deleted"))
        assertEquals(7000L, resolveToastDurationMs("Archived 1 contact"))
        assertEquals(7000L, resolveToastDurationMs("Contact archived"))

        // Case-insensitivity check
        assertEquals(7000L, resolveToastDurationMs("DELETED 2 ITEMS"))
        assertEquals(7000L, resolveToastDurationMs("contact ARCHIVED"))
    }

    @Test
    fun resolveToastDurationMs_informationalMessagesResolveToStandardDuration() {
        assertEquals(3500L, resolveToastDurationMs("Contact updated"))
        assertEquals(3500L, resolveToastDurationMs("Marked as done!"))
        assertEquals(3500L, resolveToastDurationMs("Export finished"))
        assertEquals(3500L, resolveToastDurationMs("Added to favorites"))
        assertEquals(3500L, resolveToastDurationMs("Failed to save contact"))

        // Restoration confirmations without an action label resolve to brief informational duration
        assertEquals(3500L, resolveToastDurationMs("Contact restored"))
        assertEquals(3500L, resolveToastDurationMs("Encounter restored"))
        assertEquals(3500L, resolveToastDurationMs("RESTORED contact"))

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
        assertTrue(shouldShowToastCountdown("Contact restored", actionLabel = "Undo"))

        // Recoverable deletion/archive messages with default 7000ms duration display countdown progress
        assertTrue(shouldShowToastCountdown("Contact deleted"))
        assertTrue(shouldShowToastCountdown("Archived 1 contact"))
    }

    @Test
    fun shouldShowToastCountdown_informationalMessagesWithoutActionReturnFalse() {
        assertFalse(shouldShowToastCountdown("Contact updated"))
        assertFalse(shouldShowToastCountdown("Marked as done!"))
        assertFalse(shouldShowToastCountdown("Export finished"))
        assertFalse(shouldShowToastCountdown("Added to favorites"))
        assertFalse(shouldShowToastCountdown("Failed to save contact"))

        // Subsequent restoration confirmations without action button omit countdown bar (User Story 10)
        assertFalse(shouldShowToastCountdown("Contact restored"))
        assertFalse(shouldShowToastCountdown("Encounter restored"))

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

    @Test
    fun helloToastVisuals_defaultsAndCustomProperties() {
        val actionableVisuals = HelloToastVisuals("Contact deleted", "Undo")
        assertEquals("Contact deleted", actionableVisuals.message)
        assertEquals("Undo", actionableVisuals.actionLabel)
        assertEquals(false, actionableVisuals.withDismissAction)
        assertEquals(TOAST_DURATION_ACTIONABLE_MS, actionableVisuals.durationMs)
        assertEquals(SnackbarDuration.Indefinite, actionableVisuals.duration)

        val infoVisuals = HelloToastVisuals("Contact updated")
        assertEquals("Contact updated", infoVisuals.message)
        assertNull(infoVisuals.actionLabel)
        assertEquals(false, infoVisuals.withDismissAction)
        assertEquals(TOAST_DURATION_INFORMATIONAL_MS, infoVisuals.durationMs)
        assertEquals(SnackbarDuration.Indefinite, infoVisuals.duration)

        val customVisuals = HelloToastVisuals(
            message = "Custom alert",
            actionLabel = "Retry",
            withDismissAction = true,
            durationMs = 9000L,
            duration = SnackbarDuration.Long
        )
        assertEquals("Custom alert", customVisuals.message)
        assertEquals("Retry", customVisuals.actionLabel)
        assertEquals(true, customVisuals.withDismissAction)
        assertEquals(9000L, customVisuals.durationMs)
        assertEquals(SnackbarDuration.Long, customVisuals.duration)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun showHelloToast_actionableToastTimesOutAfterFullDuration() = runTest {
        val hostState = SnackbarHostState()
        var result: SnackbarResult? = null

        val job = launch {
            result = hostState.showHelloToast(
                message = "Contact deleted",
                actionLabel = "Undo",
                durationMs = TOAST_DURATION_ACTIONABLE_MS
            )
        }

        // Advance 6000ms: toast remains visible and active
        advanceTimeBy(6000L)
        assertTrue(job.isActive)
        assertNull(result)
        assertEquals("Contact deleted", hostState.currentSnackbarData?.visuals?.message)
        assertEquals("Undo", hostState.currentSnackbarData?.visuals?.actionLabel)
        val visuals = hostState.currentSnackbarData?.visuals as? HelloToastVisuals
        assertEquals(7000L, visuals?.durationMs)

        // Advance another 1001ms (total 7001ms): timeout occurs, dismisses toast
        advanceTimeBy(1001L)
        assertFalse(job.isActive)
        assertEquals(SnackbarResult.Dismissed, result)
        assertNull(hostState.currentSnackbarData)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun showHelloToast_actionPerformedResolvesImmediately() = runTest {
        val hostState = SnackbarHostState()
        var result: SnackbarResult? = null

        val job = launch {
            result = hostState.showHelloToast(
                message = "Contact deleted",
                actionLabel = "Undo",
                durationMs = TOAST_DURATION_ACTIONABLE_MS
            )
        }

        advanceTimeBy(3000L)
        assertTrue(job.isActive)

        // User taps Undo
        hostState.currentSnackbarData?.performAction()

        advanceTimeBy(100L)
        assertEquals(SnackbarResult.ActionPerformed, result)
        assertFalse(job.isActive)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun showHelloToast_subsequentRestorationToastHasBriefDurationAndNoCountdown() = runTest {
        val hostState = SnackbarHostState()
        var result: SnackbarResult? = null

        val job = launch {
            result = hostState.showHelloToast(
                message = "Contact restored",
                durationMs = TOAST_DURATION_INFORMATIONAL_MS
            )
        }

        advanceTimeBy(1000L)
        assertTrue(job.isActive)
        val currentVisuals = hostState.currentSnackbarData?.visuals as? HelloToastVisuals
        assertEquals(TOAST_DURATION_INFORMATIONAL_MS, currentVisuals?.durationMs)
        assertNull(currentVisuals?.actionLabel)

        // Confirmation toast does NOT show countdown bar
        assertFalse(
            shouldShowToastCountdown(
                message = "Contact restored",
                actionLabel = null,
                durationMs = TOAST_DURATION_INFORMATIONAL_MS
            )
        )

        // Completes after 3500ms
        advanceTimeBy(2600L)
        assertEquals(SnackbarResult.Dismissed, result)
        assertFalse(job.isActive)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun showHelloToast_undoRestoresEntityInRepositoryWithin7Seconds() = runTest {
        val repo = com.example.hello.data.repository.ContactRepository()
        val initialContact = repo.contacts.value.first()
        val contactId = initialContact.id

        // Delete contact
        repo.deleteContact(contactId)
        assertFalse(repo.contacts.value.any { it.id == contactId })

        val hostState = SnackbarHostState()
        var actionResult: SnackbarResult? = null

        val toastJob = launch {
            actionResult = hostState.showHelloToast(
                message = "Contact deleted",
                actionLabel = "Undo",
                durationMs = TOAST_DURATION_ACTIONABLE_MS
            )
        }

        // Advance 5500ms (5.5s into the 7-second window)
        advanceTimeBy(5500L)
        assertTrue(toastJob.isActive)
        val activeVisuals = hostState.currentSnackbarData?.visuals as? HelloToastVisuals
        assertEquals(7000L, activeVisuals?.durationMs)
        assertTrue(shouldShowToastCountdown("Contact deleted", "Undo", 7000L))

        // User taps Undo
        hostState.currentSnackbarData?.performAction()
        advanceTimeBy(50L)
        assertEquals(SnackbarResult.ActionPerformed, actionResult)

        // Undo in repository
        val restored = repo.undoLastDeleteContact()
        assertTrue("Repository rollback must succeed at 5.5s within the 7s window", restored)
        assertTrue(repo.contacts.value.any { it.id == contactId })

        // Surface subsequent restoration notice
        var restoreResult: SnackbarResult? = null
        val restoreToastJob = launch {
            restoreResult = hostState.showHelloToast(
                message = "Contact restored",
                durationMs = TOAST_DURATION_INFORMATIONAL_MS
            )
        }

        advanceTimeBy(1000L)
        assertTrue(restoreToastJob.isActive)
        // Verify subsequent notice does NOT have countdown bar
        assertFalse(shouldShowToastCountdown("Contact restored", null, TOAST_DURATION_INFORMATIONAL_MS))

        // Finishes at 3500ms
        advanceTimeBy(2600L)
        assertEquals(SnackbarResult.Dismissed, restoreResult)
        assertFalse(restoreToastJob.isActive)
    }
}


