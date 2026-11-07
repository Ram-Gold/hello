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
    fun `fit whole image mode scales longer side inside viewport for full PNG stickers`() {
        // 1000x500 landscape sticker inside 500x500 circular viewport
        val baseScale = AvatarCropGeometry.calculateBaseScale(
            imageWidth = 1000f,
            imageHeight = 500f,
            viewportSize = 500f,
            fitWholeImage = true
        )
        // Longer side (1000) fits inside 500 -> baseScale = 0.5f
        assertEquals(0.5f, baseScale, 0.001f)

        // At zoom = 1f, both scaled dimensions are <= 500, so pan is centered at (0, 0)
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
}
