package com.example.hello.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NotionistAvatarConfigTest {

    @Test
    fun toDiceBearPngUrl_omitsOptionalTraitsWithZeroProbabilityWhenNull() {
        val config = NotionistAvatarConfig(
            seed = "abc123def456",
            hair = "variant05",
            eyes = "variant02",
            brows = "variant04",
            lips = "variant10",
            nose = "variant03",
            beard = null,
            glasses = null,
            body = "variant08",
            gesture = null,
            backgroundColor = "faf7f2"
        )

        val url = config.toDiceBearPngUrl()
        assertTrue(url.startsWith("https://api.dicebear.com/9.x/notionists/png?"))
        assertTrue(url.contains("seed=abc123def456"))
        assertTrue(url.contains("size=256"))
        assertTrue(url.contains("backgroundColor=faf7f2"))
        assertTrue(url.contains("hair=variant05"))
        assertTrue(url.contains("beardProbability=0"))
        assertTrue(url.contains("glassesProbability=0"))
        assertTrue(url.contains("gestureProbability=0"))
        assertFalse(url.contains("&beard="))
        assertFalse(url.contains("&glasses="))
        assertFalse(url.contains("&gesture="))
    }

    @Test
    fun toDiceBearPngUrl_includesOptionalTraitsWithFullProbabilityWhenSelected() {
        val config = NotionistAvatarConfig(
            seed = "opaque987654",
            hair = "hat",
            beard = "variant07",
            glasses = "variant03",
            gesture = "handPhone",
            backgroundColor = "ffddd2"
        )

        val url = config.toDiceBearPngUrl()
        assertTrue(url.contains("hair=hat"))
        assertTrue(url.contains("beard=variant07"))
        assertTrue(url.contains("beardProbability=100"))
        assertTrue(url.contains("glasses=variant03"))
        assertTrue(url.contains("glassesProbability=100"))
        assertTrue(url.contains("gesture=handPhone"))
        assertTrue(url.contains("gestureProbability=100"))
        assertTrue(url.contains("backgroundColor=ffddd2"))
    }

    @Test
    fun jsonRoundTrip_preservesAllFieldsAndNullOptionals() {
        val original = NotionistAvatarConfig(
            seed = "seed1234abcd",
            hair = "variant42",
            eyes = "variant04",
            brows = "variant09",
            lips = "variant22",
            nose = "variant15",
            beard = "variant06",
            glasses = null,
            body = "variant19",
            gesture = "waveLongArm",
            backgroundColor = "d8e2dc"
        )

        val json = original.toJson()
        val parsed = NotionistAvatarConfig.fromJson(json)
        assertEquals(original, parsed)
    }

    @Test
    fun fromJson_handlesBlankCorruptOrUnknownVariantGracefully() {
        assertNull(NotionistAvatarConfig.fromJson(null))
        assertNull(NotionistAvatarConfig.fromJson("   "))
        assertNull(NotionistAvatarConfig.fromJson("not-json"))

        val partialWithUnknown = """{"seed":"validseed123","hair":"unknown999","beard":"variant02","backgroundColor":"invalid"}"""
        val parsed = NotionistAvatarConfig.fromJson(partialWithUnknown)
        assertNotNull(parsed)
        assertEquals("validseed123", parsed!!.seed)
        assertEquals("variant01", parsed.hair)
        assertEquals("variant02", parsed.beard)
        assertEquals("ffffff", parsed.backgroundColor)
    }

    @Test
    fun random_generatesOpaqueSeedAndValidCatalogValues() {
        val config = NotionistAvatarConfig.random(Random(42))
        assertTrue(config.seed.matches(Regex("^[a-z0-9]{12}$")))
        assertTrue(config.hair in NotionistCatalog.validHairIds)
        assertTrue(config.eyes in NotionistCatalog.validEyesIds)
        assertTrue(config.brows in NotionistCatalog.validBrowsIds)
        assertTrue(config.lips in NotionistCatalog.validLipsIds)
        assertTrue(config.nose in NotionistCatalog.validNoseIds)
        assertTrue(config.body in NotionistCatalog.validBodyIds)
        assertTrue(config.backgroundColor in NotionistCatalog.validBackgroundHexes)
        if (config.beard != null) assertTrue(config.beard in NotionistCatalog.validBeardIds)
        if (config.glasses != null) assertTrue(config.glasses in NotionistCatalog.validGlassesIds)
        if (config.gesture != null) assertTrue(config.gesture in NotionistCatalog.validGestureIds)
    }

    @Test
    fun stepOption_wrapsCleanlyForwardAndBackward() {
        val base = NotionistAvatarConfig(seed = "stepseed1234", glasses = null)
        // Step backward from None wraps to last glasses variant
        val wrappedBack = base.stepOption(NotionistTraitCategory.GLASSES, -1)
        assertEquals("variant11", wrappedBack.glasses)
        // Step forward wraps back to None
        val wrappedForward = wrappedBack.stepOption(NotionistTraitCategory.GLASSES, 1)
        assertNull(wrappedForward.glasses)
    }

    @Test
    fun thumbnailUrlBuilder_isolatesTraitsAt128pxWithVersionedCacheKeys() {
        val browsUrl = NotionistThumbnailUrlBuilder.buildThumbnailUrl(NotionistTraitCategory.BROWS, "variant05")
        assertTrue(browsUrl.startsWith("https://api.dicebear.com/9.x/notionists-neutral/png?"))
        assertTrue(browsUrl.contains("seed=hellothumb"))
        assertTrue(browsUrl.contains("size=128"))
        assertTrue(browsUrl.contains("brows=variant05"))
        assertTrue(browsUrl.contains("eyes=&"))
        assertTrue(browsUrl.contains("lips=&"))
        assertTrue(browsUrl.contains("nose=&"))
        assertTrue(browsUrl.contains("glassesProbability=0"))

        val hairUrl = NotionistThumbnailUrlBuilder.buildThumbnailUrl(NotionistTraitCategory.HAIR, "hat")
        assertTrue(hairUrl.startsWith("https://api.dicebear.com/9.x/notionists/png?"))
        assertTrue(hairUrl.contains("size=128"))
        assertTrue(hairUrl.contains("base=&"))
        assertTrue(hairUrl.contains("hair=hat"))
        assertTrue(hairUrl.contains("body=&"))
        assertTrue(hairUrl.contains("bodyIconProbability=0"))
        assertTrue(hairUrl.contains("beardProbability=0"))
        assertTrue(hairUrl.contains("glassesProbability=0"))
        assertTrue(hairUrl.contains("gestureProbability=0"))

        val glassesUrl = NotionistThumbnailUrlBuilder.buildThumbnailUrl(NotionistTraitCategory.GLASSES, "variant04")
        assertTrue(glassesUrl.startsWith("https://api.dicebear.com/9.x/notionists-neutral/png?"))
        assertTrue(glassesUrl.contains("glasses=variant04"))
        assertTrue(glassesUrl.contains("glassesProbability=100"))

        assertEquals("v2_brows_variant05", NotionistThumbnailUrlBuilder.cacheKey(NotionistTraitCategory.BROWS, "variant05"))
        assertEquals("v2_hair_hat", NotionistThumbnailUrlBuilder.cacheKey(NotionistTraitCategory.HAIR, "hat"))
    }
}
