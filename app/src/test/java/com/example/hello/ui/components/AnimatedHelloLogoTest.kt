package com.example.hello.ui.components

import com.example.hello.ui.theme.MotionTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedHelloLogoTest {

    @Test
    fun totalDurations_matchWritingItDownSpecAndReducedMotion() {
        assertEquals(680, helloMarkTotalDurationMs(HelloMarkAnimationMode.LAUNCH, isReducedMotion = false))
        assertEquals(1670, helloMarkTotalDurationMs(HelloMarkAnimationMode.WELCOME, isReducedMotion = false))
        assertEquals(400, helloMarkTotalDurationMs(HelloMarkAnimationMode.SAVED, isReducedMotion = false))

        assertEquals(120, helloMarkTotalDurationMs(HelloMarkAnimationMode.LAUNCH, isReducedMotion = true))
        assertEquals(120, helloMarkTotalDurationMs(HelloMarkAnimationMode.WELCOME, isReducedMotion = true))
        assertEquals(120, helloMarkTotalDurationMs(HelloMarkAnimationMode.SAVED, isReducedMotion = true))
    }

    @Test
    fun launchTimeline_choreographsBandOutlineAndSquiggleWithOverlap() {
        val start = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.LAUNCH, elapsedMs = 0f, isReducedMotion = false)
        assertEquals(0f, start.bandAlpha, 0.001f)
        assertEquals(-MotionTokens.HelloMarkMotion.BAND_DROP_OFFSET_Y_UNITS, start.bandOffsetYUnits, 0.001f)
        assertEquals(0f, start.outlineProgress, 0.001f)
        assertEquals(0f, start.outlineAlpha, 0.001f)
        assertEquals(0f, start.squiggleProgress, 0.001f)
        assertEquals(0f, start.squiggleAlpha, 0.001f)

        // At 160ms: band has landed; outline started at 100ms and is mid-stroke; squiggle not started yet
        val at160 = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.LAUNCH, elapsedMs = 160f, isReducedMotion = false)
        assertEquals(1f, at160.bandAlpha, 0.001f)
        assertEquals(0f, at160.bandOffsetYUnits, 0.001f)
        assertTrue(at160.outlineProgress > 0f && at160.outlineProgress < 1f)
        assertEquals(1f, at160.outlineAlpha, 0.001f)
        assertEquals(0f, at160.squiggleProgress, 0.001f)
        assertEquals(0f, at160.squiggleAlpha, 0.001f)

        // At 380ms: outline finished; squiggle (started at 260ms) is underway
        val at380 = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.LAUNCH, elapsedMs = 380f, isReducedMotion = false)
        assertEquals(1f, at380.outlineProgress, 0.001f)
        assertTrue(at380.squiggleProgress > 0f && at380.squiggleProgress < 1f)
        assertEquals(1f, at380.squiggleAlpha, 0.001f)

        // At 680ms: all launch elements complete, no wordmark letters active
        val end = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.LAUNCH, elapsedMs = 680f, isReducedMotion = false)
        assertEquals(1f, end.bandAlpha, 0.001f)
        assertEquals(0f, end.bandOffsetYUnits, 0.001f)
        assertEquals(1f, end.outlineProgress, 0.001f)
        assertEquals(1f, end.squiggleProgress, 0.001f)
        assertTrue(end.letterProgress.all { it == 0f })
        assertTrue(end.letterAlpha.all { it == 0f })
    }

    @Test
    fun welcomeTimeline_staggersFiveLettersBy90msStartingAt1050ms() {
        // Before 1050ms: squiggle still finishing, no letters started
        val at1000 = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.WELCOME, elapsedMs = 1000f, isReducedMotion = false)
        assertEquals(1f, at1000.bandAlpha, 0.001f)
        assertEquals(1f, at1000.outlineProgress, 0.001f)
        assertTrue(at1000.squiggleProgress > 0.8f && at1000.squiggleProgress < 1f)
        assertTrue(at1000.letterProgress.all { it == 0f })

        // At 1200ms: letters 0 (1050ms) and 1 (1140ms) are drawing; letters 2 (1230ms), 3, 4 haven't started
        val at1200 = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.WELCOME, elapsedMs = 1200f, isReducedMotion = false)
        assertTrue(at1200.letterProgress[0] > at1200.letterProgress[1])
        assertTrue(at1200.letterProgress[1] > 0f)
        assertEquals(0f, at1200.letterProgress[2], 0.001f)
        assertEquals(0f, at1200.letterProgress[3], 0.001f)
        assertEquals(0f, at1200.letterProgress[4], 0.001f)

        // At 1670ms: all 5 letters fully drawn
        val end = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.WELCOME, elapsedMs = 1670f, isReducedMotion = false)
        assertEquals(1f, end.squiggleProgress, 0.001f)
        assertTrue(end.letterProgress.all { it == 1f })
        assertTrue(end.letterAlpha.all { it == 1f })
    }

    @Test
    fun savedTimeline_keepsBandAndOutlineStaticAndRedrawsSquiggleIn400ms() {
        val start = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.SAVED, elapsedMs = 0f, isReducedMotion = false)
        assertEquals(1f, start.bandAlpha, 0.001f)
        assertEquals(0f, start.bandOffsetYUnits, 0.001f)
        assertEquals(1f, start.outlineProgress, 0.001f)
        assertEquals(1f, start.outlineAlpha, 0.001f)
        assertEquals(0f, start.squiggleProgress, 0.001f)
        assertEquals(0f, start.squiggleAlpha, 0.001f)

        val mid = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.SAVED, elapsedMs = 200f, isReducedMotion = false)
        assertTrue(mid.squiggleProgress > 0.5f && mid.squiggleProgress < 1f)
        assertEquals(1f, mid.squiggleAlpha, 0.001f)

        val end = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.SAVED, elapsedMs = 400f, isReducedMotion = false)
        assertEquals(1f, end.squiggleProgress, 0.001f)
        assertEquals(1f, end.squiggleAlpha, 0.001f)
    }

    @Test
    fun reducedMotion_rendersFullGeometryWith120msOpacityFadeAndZeroDropOffset() {
        val midWelcome = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.WELCOME, elapsedMs = 60f, isReducedMotion = true)
        assertEquals(0.5f, midWelcome.bandAlpha, 0.001f)
        assertEquals(0f, midWelcome.bandOffsetYUnits, 0.001f)
        assertEquals(1f, midWelcome.outlineProgress, 0.001f)
        assertEquals(0.5f, midWelcome.outlineAlpha, 0.001f)
        assertEquals(1f, midWelcome.squiggleProgress, 0.001f)
        assertEquals(0.5f, midWelcome.squiggleAlpha, 0.001f)
        assertTrue(midWelcome.letterProgress.all { it == 1f })
        assertTrue(midWelcome.letterAlpha.all { kotlin.math.abs(it - 0.5f) < 0.001f })

        val endSaved = computeHelloMarkTimelineFrame(HelloMarkAnimationMode.SAVED, elapsedMs = 120f, isReducedMotion = true)
        assertEquals(1f, endSaved.bandAlpha, 0.001f)
        assertEquals(1f, endSaved.squiggleProgress, 0.001f)
        assertEquals(1f, endSaved.squiggleAlpha, 0.001f)
        assertTrue(endSaved.letterProgress.all { it == 0f })
    }

    @Test
    fun shouldPlayLaunchMarkAnimation_respectsAllGatingRules() {
        assertTrue(
            shouldPlayLaunchMarkAnimation(
                hasPlayedInProcess = false,
                hasCompletedOnboarding = true,
                hasInitialDeepLink = false,
                isReducedMotion = false
            )
        )
        assertFalse(
            shouldPlayLaunchMarkAnimation(
                hasPlayedInProcess = true,
                hasCompletedOnboarding = true,
                hasInitialDeepLink = false,
                isReducedMotion = false
            )
        )
        assertFalse(
            shouldPlayLaunchMarkAnimation(
                hasPlayedInProcess = false,
                hasCompletedOnboarding = false,
                hasInitialDeepLink = false,
                isReducedMotion = false
            )
        )
        assertFalse(
            shouldPlayLaunchMarkAnimation(
                hasPlayedInProcess = false,
                hasCompletedOnboarding = true,
                hasInitialDeepLink = true,
                isReducedMotion = false
            )
        )
        assertFalse(
            shouldPlayLaunchMarkAnimation(
                hasPlayedInProcess = false,
                hasCompletedOnboarding = true,
                hasInitialDeepLink = false,
                isReducedMotion = true
            )
        )
    }
}
