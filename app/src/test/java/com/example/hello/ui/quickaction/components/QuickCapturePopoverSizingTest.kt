package com.example.hello.ui.quickaction.components

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickCapturePopoverSizingTest {

    @Test
    fun resolveMaxPopoverHeightDp_fitsStrictlyBelowStatusBarOnCompactOlderPhones() {
        // Older 640dp phone with 260dp keyboard -> 380dp container, 136dp composer, 24dp status bar, 12dp gap
        val result = resolveMaxPopoverHeightDp(
            availableContainerHeightDp = 380f,
            popoverBottomPaddingDp = 136f,
            statusBarTopDp = 24f
        )
        assertEquals(208f, result, 0.01f)
    }

    @Test
    fun resolveMaxPopoverHeightDp_capsComfortablyOnTallPhonesAndRespectsMinFloorOnTinyViewports() {
        // Tall phone with plenty of headroom caps at 360dp
        val tall = resolveMaxPopoverHeightDp(
            availableContainerHeightDp = 620f,
            popoverBottomPaddingDp = 136f,
            statusBarTopDp = 40f
        )
        assertEquals(360f, tall, 0.01f)

        // Extremely short landscape / split-screen viewport clamps at 120dp floor
        val tiny = resolveMaxPopoverHeightDp(
            availableContainerHeightDp = 210f,
            popoverBottomPaddingDp = 136f,
            statusBarTopDp = 24f
        )
        assertEquals(120f, tiny, 0.01f)

        // Unbounded preview container falls back to 280dp
        val unbounded = resolveMaxPopoverHeightDp(
            availableContainerHeightDp = Float.POSITIVE_INFINITY,
            popoverBottomPaddingDp = 136f,
            statusBarTopDp = 24f
        )
        assertEquals(280f, unbounded, 0.01f)
    }

    @Test
    fun resolveDetailChipCornerRadiusDp_morphsFromFullPillToSquircleWhenActive() {
        assertEquals(19f, resolveDetailChipCornerRadiusDp(isSelectedOrActive = false), 0.01f)
        assertEquals(10f, resolveDetailChipCornerRadiusDp(isSelectedOrActive = true), 0.01f)
    }
}
