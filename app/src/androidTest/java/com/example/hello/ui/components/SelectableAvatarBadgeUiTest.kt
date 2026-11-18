package com.example.hello.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.hello.ui.theme.HelloTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SelectableAvatarBadgeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectedBadge_displaysOpaqueBlackBackgroundAndCheckmark() {
        composeTestRule.setContent {
            HelloTheme(darkTheme = false) {
                SelectableAvatarBadge(
                    isSelected = true,
                    initials = "AB",
                    bgIndex = 0,
                    size = 56.dp,
                    modifier = Modifier.testTag("badge")
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Selected").assertIsDisplayed()

        val pixelMap = composeTestRule.onNodeWithTag("badge").captureToImage().toPixelMap()
        val backgroundPixel = pixelMap[(pixelMap.width * 0.22f).toInt(), pixelMap.height / 2]
        assertEquals(1f, backgroundPixel.alpha, 0.01f)
        assertEquals(0f, backgroundPixel.red, 0.02f)
        assertEquals(0f, backgroundPixel.green, 0.02f)
        assertEquals(0f, backgroundPixel.blue, 0.02f)

        val hasWhiteCheckmarkPixel = (pixelMap.width * 3 / 10 until pixelMap.width * 7 / 10).any { x ->
            (pixelMap.height * 3 / 10 until pixelMap.height * 7 / 10).any { y ->
                val p = pixelMap[x, y]
                p.red > 0.9f && p.green > 0.9f && p.blue > 0.9f
            }
        }
        assertTrue("Expected white checkmark stroke inside badge center", hasWhiteCheckmarkPixel)
    }

    @Test
    fun selectedBadge_darkTheme_displaysOpaqueBlackBackgroundAndWhiteCheckmark() {
        composeTestRule.setContent {
            HelloTheme(darkTheme = true) {
                SelectableAvatarBadge(
                    isSelected = true,
                    initials = "AB",
                    bgIndex = 0,
                    size = 56.dp,
                    modifier = Modifier.testTag("badge_dark")
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Selected").assertIsDisplayed()

        val pixelMap = composeTestRule.onNodeWithTag("badge_dark").captureToImage().toPixelMap()
        val backgroundPixel = pixelMap[(pixelMap.width * 0.22f).toInt(), pixelMap.height / 2]
        assertEquals(1f, backgroundPixel.alpha, 0.01f)
        assertEquals(0f, backgroundPixel.red, 0.02f)
        assertEquals(0f, backgroundPixel.green, 0.02f)
        assertEquals(0f, backgroundPixel.blue, 0.02f)

        val hasWhiteCheckmarkPixel = (pixelMap.width * 3 / 10 until pixelMap.width * 7 / 10).any { x ->
            (pixelMap.height * 3 / 10 until pixelMap.height * 7 / 10).any { y ->
                val p = pixelMap[x, y]
                p.red > 0.9f && p.green > 0.9f && p.blue > 0.9f
            }
        }
        assertTrue("Expected white checkmark stroke inside dark-theme badge center", hasWhiteCheckmarkPixel)
    }

    @Test
    fun unselectedBadge_hidesSelectionCheckmark() {
        composeTestRule.setContent {
            HelloTheme(darkTheme = false) {
                SelectableAvatarBadge(
                    isSelected = false,
                    initials = "AB",
                    bgIndex = 0,
                    size = 56.dp
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Selected").assertDoesNotExist()
    }
}
