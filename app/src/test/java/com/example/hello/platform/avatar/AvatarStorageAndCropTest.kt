package com.example.hello.platform.avatar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AvatarStorageAndCropTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `fill circle mode scales shorter side to viewport and clamps pan so edges never enter circle`() {
        // 1000x500 landscape image inside 500x500 circular viewport
        val baseScale = AvatarCropGeometry.calculateBaseScale(
            imageWidth = 1000f,
            imageHeight = 500f,
            viewportSize = 500f,
            fitWholeImage = false
        )
        // Shorter side (500) matches viewport (500) -> baseScale = 1.0f
        assertEquals(1.0f, baseScale, 0.001f)

        // At zoom = 1f: scaled width = 1000 (max pan X = ±250), scaled height = 500 (max pan Y = 0)
        val (clampedX, clampedY) = AvatarCropGeometry.clampPanOffset(
            offsetX = 400f,
            offsetY = -120f,
            imageWidth = 1000f,
            imageHeight = 500f,
            viewportSize = 500f,
            zoom = 1f,
            fitWholeImage = false
        )
        assertEquals(250f, clampedX, 0.001f)
        assertEquals(0f, clampedY, 0.001f)
    }

    @Test
    fun `fit whole image mode inscribes entire image within circular aperture so all corners fit inside circle`() {
        // 1000x500 landscape sticker inside 500x500 circular viewport
        val baseScale = AvatarCropGeometry.calculateBaseScale(
            imageWidth = 1000f,
            imageHeight = 500f,
            viewportSize = 500f,
            fitWholeImage = true
        )
        // Diagonal = sqrt(1000^2 + 500^2) = ~1118.034f. Base scale = 500 / 1118.034f ~= 0.4472136f
        val expectedScale = 500f / kotlin.math.hypot(1000.0, 500.0).toFloat()
        assertEquals(expectedScale, baseScale, 0.001f)

        // Verify that the corner distance from center is exactly viewportSize / 2 (radius = 250)
        val scaledWidth = 1000f * baseScale
        val scaledHeight = 500f * baseScale
        val cornerDistance = kotlin.math.hypot(scaledWidth.toDouble() / 2, scaledHeight.toDouble() / 2).toFloat()
        assertEquals(250f, cornerDistance, 0.01f)

        // At zoom = 1f, both scaled dimensions are <= 500, so pan is clamped to (0, 0)
        val (clampedX, clampedY) = AvatarCropGeometry.clampPanOffset(
            offsetX = 100f,
            offsetY = 100f,
            imageWidth = 1000f,
            imageHeight = 500f,
            viewportSize = 500f,
            zoom = 1f,
            fitWholeImage = true
        )
        assertEquals(0f, clampedX, 0.001f)
        assertEquals(0f, clampedY, 0.001f)
    }

    @Test
    fun `fit whole image mode on square image inscribes square within circular aperture`() {
        // 400x400 square inside 400x400 circular viewport
        val baseScale = AvatarCropGeometry.calculateBaseScale(
            imageWidth = 400f,
            imageHeight = 400f,
            viewportSize = 400f,
            fitWholeImage = true
        )
        // Diagonal = 400 * sqrt(2). Base scale = 1 / sqrt(2) ~= 0.7071f
        val expectedScale = 1f / kotlin.math.sqrt(2f)
        assertEquals(expectedScale, baseScale, 0.001f)
    }

    @Test
    fun `fit whole image mode on portrait image inscribes portrait within circular aperture`() {
        // 900x1600 portrait image inside 600x600 circular viewport
        val baseScale = AvatarCropGeometry.calculateBaseScale(
            imageWidth = 900f,
            imageHeight = 1600f,
            viewportSize = 600f,
            fitWholeImage = true
        )
        val expectedScale = 600f / kotlin.math.hypot(900.0, 1600.0).toFloat()
        assertEquals(expectedScale, baseScale, 0.001f)

        // Corner distance is exactly radius (300)
        val cornerDist = kotlin.math.hypot((900f * baseScale).toDouble() / 2, (1600f * baseScale).toDouble() / 2).toFloat()
        assertEquals(300f, cornerDist, 0.01f)
    }

    @Test
    fun `staged avatar lifecycle promotes to canonical file on commit and deletes superseded file`() {
        val avatarsDir = File(tempFolder.root, "avatars")
        val storage = AvatarStorageManager(avatarsDir)

        // 1. Initial crop staged and committed for contact-1
        val staged1 = storage.writeStagedBytes(byteArrayOf(1, 2, 3, 4))
        assertTrue(File(staged1).exists())
        assertTrue(File(staged1).name.startsWith("staged_"))

        val canonical1 = storage.commitStagedAvatar(
            contactId = "contact-1",
            currentPhotoUri = staged1,
            originalPhotoUri = null,
            nowMillis = 1000L
        )
        assertNotNull(canonical1)
        assertFalse("Staged file should be moved/promoted", File(staged1).exists())
        assertTrue("Canonical file should exist", File(canonical1!!).exists())
        assertEquals("contact_contact-1_1000.png", File(canonical1).name)

        // 2. User edits contact-1 again, crops twice in one session, then saves
        val staged2a = storage.writeStagedBytes(byteArrayOf(5, 6, 7))
        assertTrue(File(staged2a).exists())
        val staged2b = storage.writeStagedBytes(byteArrayOf(8, 9, 10), previousStagedPath = staged2a)
        assertFalse("Previous uncommitted staged file should be cleaned up", File(staged2a).exists())
        assertTrue(File(staged2b).exists())

        val canonical2 = storage.commitStagedAvatar(
            contactId = "contact-1",
            currentPhotoUri = staged2b,
            originalPhotoUri = canonical1,
            nowMillis = 2000L
        )
        assertNotNull(canonical2)
        assertFalse("Old canonical file should be deleted on save", File(canonical1).exists())
        assertTrue("New canonical file should exist", File(canonical2!!).exists())
        assertEquals("contact_contact-1_2000.png", File(canonical2).name)
    }

    @Test
    fun `discarding staged avatar deletes staged file and leaves original avatar intact`() {
        val avatarsDir = File(tempFolder.root, "avatars")
        val storage = AvatarStorageManager(avatarsDir)

        val initialStaged = storage.writeStagedBytes(byteArrayOf(10, 20, 30))
        val originalCanonical = storage.commitStagedAvatar(
            contactId = "contact-2",
            currentPhotoUri = initialStaged,
            originalPhotoUri = null,
            nowMillis = 5000L
        )!!

        // User crops a new staged photo, then taps Back -> Discard
        val newStaged = storage.writeStagedBytes(byteArrayOf(99, 98, 97))
        assertTrue(File(newStaged).exists())

        storage.discardStagedAvatar(stagedPhotoUri = newStaged, originalPhotoUri = originalCanonical)

        assertFalse("Staged file must be deleted on discard", File(newStaged).exists())
        assertTrue("Original canonical avatar must remain untouched", File(originalCanonical).exists())
    }

    @Test
    fun `resetting photo to null deletes original avatar file when committed`() {
        val avatarsDir = File(tempFolder.root, "avatars")
        val storage = AvatarStorageManager(avatarsDir)

        val staged = storage.writeStagedBytes(byteArrayOf(42, 43))
        val canonical = storage.commitStagedAvatar(
            contactId = "contact-3",
            currentPhotoUri = staged,
            originalPhotoUri = null,
            nowMillis = 7000L
        )!!
        assertTrue(File(canonical).exists())

        val afterReset = storage.commitStagedAvatar(
            contactId = "contact-3",
            currentPhotoUri = null,
            originalPhotoUri = canonical,
            nowMillis = 8000L
        )
        assertNull(afterReset)
        assertFalse("Original avatar file should be deleted when reset is saved", File(canonical).exists())
    }

    @Test
    fun `flattenPixelOverWarmWhite composites transparent and semi-transparent pixels over solid FFFFFF`() {
        // Fully transparent pixel -> solid Warm White (#FFFFFF)
        assertEquals(0xFFFFFFFF.toInt(), AvatarCropGeometry.flattenPixelOverWarmWhite(0x00000000))

        // Fully opaque terracotta pixel (#FFE06D3B) -> unchanged
        assertEquals(0xFFE06D3B.toInt(), AvatarCropGeometry.flattenPixelOverWarmWhite(0xFFE06D3B.toInt()))

        // 50% black pixel (0x80000000) over #FFFFFF -> mid-gray with 100% opacity (0xFF7F7F7F)
        val blended = AvatarCropGeometry.flattenPixelOverWarmWhite(0x80000000.toInt())
        assertEquals(0xFF, (blended ushr 24) and 0xFF)
        assertEquals(127, (blended ushr 16) and 0xFF)
        assertEquals(127, (blended ushr 8) and 0xFF)
        assertEquals(127, blended and 0xFF)
    }
}

