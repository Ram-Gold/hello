package com.example.hello.domain.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.random.Random

/**
 * Categories of customizable visual traits for an opt-in Notionist Avatar (ADR-0035).
 */
enum class NotionistTraitCategory(val displayLabel: String) {
    HAIR("Hair"),
    EYES("Eyes"),
    BROWS("Brows"),
    LIPS("Lips"),
    NOSE("Nose"),
    BEARD("Beard"),
    GLASSES("Glasses"),
    BODY("Body"),
    GESTURE("Gesture"),
    BACKGROUND("Background")
}

/**
 * Selectable trait option in the Avatar Playground.
 * A null [id] represents "None" for optional traits (Beard, Glasses, Gesture).
 */
data class NotionistOption(
    val id: String?,
    val label: String
)

/**
 * Curated background swatch for a Notionist Avatar (Minimalist + Hello warm pastels).
 */
data class NotionistBackgroundSwatch(
    val hex: String,
    val label: String,
    val argb: Long
)

/**
 * Verified DiceBear 9.x `notionists` option catalog.
 */
object NotionistCatalog {
    const val DICEBEAR_API_BASE_URL = "https://api.dicebear.com/9.x/notionists/png"
    const val DEFAULT_PNG_SIZE_PX = 256

    private fun numberedVariants(count: Int, prefix: String = "variant"): List<NotionistOption> =
        (1..count).map { index ->
            val id = "$prefix${index.toString().padStart(2, '0')}"
            NotionistOption(id = id, label = "Style $index")
        }

    val hairOptions: List<NotionistOption> =
        listOf(NotionistOption(id = "hat", label = "Hat")) + numberedVariants(63)

    val eyesOptions: List<NotionistOption> = numberedVariants(5)

    val browsOptions: List<NotionistOption> = numberedVariants(13)

    val lipsOptions: List<NotionistOption> = numberedVariants(30)

    val noseOptions: List<NotionistOption> = numberedVariants(20)

    val beardOptions: List<NotionistOption> =
        listOf(NotionistOption(id = null, label = "None")) + numberedVariants(12)

    val glassesOptions: List<NotionistOption> =
        listOf(NotionistOption(id = null, label = "None")) + numberedVariants(11)

    val bodyOptions: List<NotionistOption> = numberedVariants(25)

    val gestureOptions: List<NotionistOption> = listOf(
        NotionistOption(id = null, label = "None"),
        NotionistOption(id = "hand", label = "Hand"),
        NotionistOption(id = "handPhone", label = "Phone"),
        NotionistOption(id = "ok", label = "OK"),
        NotionistOption(id = "okLongArm", label = "Long OK"),
        NotionistOption(id = "point", label = "Point"),
        NotionistOption(id = "pointLongArm", label = "Long Point"),
        NotionistOption(id = "waveLongArm", label = "Wave"),
        NotionistOption(id = "waveLongArms", label = "Double Wave"),
        NotionistOption(id = "waveOkLongArms", label = "Wave + OK"),
        NotionistOption(id = "wavePointLongArms", label = "Wave + Point")
    )

    val backgroundSwatches: List<NotionistBackgroundSwatch> = listOf(
        NotionistBackgroundSwatch(hex = "ffffff", label = "White", argb = 0xFFFFFFFF),
        NotionistBackgroundSwatch(hex = "faf7f2", label = "Warm Cream", argb = 0xFFFAF7F2),
        NotionistBackgroundSwatch(hex = "f1f5f9", label = "Soft Gray", argb = 0xFFF1F5F9),
        NotionistBackgroundSwatch(hex = "ffddd2", label = "Peach", argb = 0xFFFFDDD2),
        NotionistBackgroundSwatch(hex = "ffe8d6", label = "Amber", argb = 0xFFFFE8D6),
        NotionistBackgroundSwatch(hex = "d8e2dc", label = "Sage", argb = 0xFFD8E2DC),
        NotionistBackgroundSwatch(hex = "e8f1f5", label = "Sky", argb = 0xFFE8F1F5),
        NotionistBackgroundSwatch(hex = "ffcad4", label = "Rose", argb = 0xFFFFCAD4),
        NotionistBackgroundSwatch(hex = "e2d4f0", label = "Lavender", argb = 0xFFE2D4F0)
    )

    val backgroundOptions: List<NotionistOption> =
        backgroundSwatches.map { NotionistOption(id = it.hex, label = it.label) }

    fun optionsFor(category: NotionistTraitCategory): List<NotionistOption> = when (category) {
        NotionistTraitCategory.HAIR -> hairOptions
        NotionistTraitCategory.EYES -> eyesOptions
        NotionistTraitCategory.BROWS -> browsOptions
        NotionistTraitCategory.LIPS -> lipsOptions
        NotionistTraitCategory.NOSE -> noseOptions
        NotionistTraitCategory.BEARD -> beardOptions
        NotionistTraitCategory.GLASSES -> glassesOptions
        NotionistTraitCategory.BODY -> bodyOptions
        NotionistTraitCategory.GESTURE -> gestureOptions
        NotionistTraitCategory.BACKGROUND -> backgroundOptions
    }

    val validHairIds: Set<String> = hairOptions.mapNotNull { it.id }.toSet()
    val validEyesIds: Set<String> = eyesOptions.mapNotNull { it.id }.toSet()
    val validBrowsIds: Set<String> = browsOptions.mapNotNull { it.id }.toSet()
    val validLipsIds: Set<String> = lipsOptions.mapNotNull { it.id }.toSet()
    val validNoseIds: Set<String> = noseOptions.mapNotNull { it.id }.toSet()
    val validBeardIds: Set<String> = beardOptions.mapNotNull { it.id }.toSet()
    val validGlassesIds: Set<String> = glassesOptions.mapNotNull { it.id }.toSet()
    val validBodyIds: Set<String> = bodyOptions.mapNotNull { it.id }.toSet()
    val validGestureIds: Set<String> = gestureOptions.mapNotNull { it.id }.toSet()
    val validBackgroundHexes: Set<String> = backgroundSwatches.map { it.hex }.toSet()
}

/**
 * Pure Kotlin recipe representing a Contact's customized Notionist Avatar (ADR-0035).
 * Uses an opaque random [seed] (never Contact PII) alongside explicit DiceBear trait parameters.
 */
data class NotionistAvatarConfig(
    val seed: String = generateOpaqueSeed(),
    val hair: String = "variant01",
    val eyes: String = "variant01",
    val brows: String = "variant01",
    val lips: String = "variant01",
    val nose: String = "variant01",
    val beard: String? = null,
    val glasses: String? = null,
    val body: String = "variant01",
    val gesture: String? = null,
    val backgroundColor: String = "ffffff"
) {
    fun selectedOptionId(category: NotionistTraitCategory): String? = when (category) {
        NotionistTraitCategory.HAIR -> hair
        NotionistTraitCategory.EYES -> eyes
        NotionistTraitCategory.BROWS -> brows
        NotionistTraitCategory.LIPS -> lips
        NotionistTraitCategory.NOSE -> nose
        NotionistTraitCategory.BEARD -> beard
        NotionistTraitCategory.GLASSES -> glasses
        NotionistTraitCategory.BODY -> body
        NotionistTraitCategory.GESTURE -> gesture
        NotionistTraitCategory.BACKGROUND -> backgroundColor
    }

    fun selectedOptionLabel(category: NotionistTraitCategory): String {
        val currentId = selectedOptionId(category)
        return NotionistCatalog.optionsFor(category)
            .firstOrNull { it.id == currentId }
            ?.label
            ?: currentId
            ?: "None"
    }

    fun withOption(category: NotionistTraitCategory, optionId: String?): NotionistAvatarConfig = when (category) {
        NotionistTraitCategory.HAIR -> copy(hair = optionId?.takeIf { it in NotionistCatalog.validHairIds } ?: hair)
        NotionistTraitCategory.EYES -> copy(eyes = optionId?.takeIf { it in NotionistCatalog.validEyesIds } ?: eyes)
        NotionistTraitCategory.BROWS -> copy(brows = optionId?.takeIf { it in NotionistCatalog.validBrowsIds } ?: brows)
        NotionistTraitCategory.LIPS -> copy(lips = optionId?.takeIf { it in NotionistCatalog.validLipsIds } ?: lips)
        NotionistTraitCategory.NOSE -> copy(nose = optionId?.takeIf { it in NotionistCatalog.validNoseIds } ?: nose)
        NotionistTraitCategory.BEARD -> copy(beard = optionId?.takeIf { it in NotionistCatalog.validBeardIds })
        NotionistTraitCategory.GLASSES -> copy(glasses = optionId?.takeIf { it in NotionistCatalog.validGlassesIds })
        NotionistTraitCategory.BODY -> copy(body = optionId?.takeIf { it in NotionistCatalog.validBodyIds } ?: body)
        NotionistTraitCategory.GESTURE -> copy(gesture = optionId?.takeIf { it in NotionistCatalog.validGestureIds })
        NotionistTraitCategory.BACKGROUND -> copy(
            backgroundColor = optionId?.lowercase()?.takeIf { it in NotionistCatalog.validBackgroundHexes } ?: backgroundColor
        )
    }

    fun stepOption(category: NotionistTraitCategory, delta: Int): NotionistAvatarConfig {
        val options = NotionistCatalog.optionsFor(category)
        if (options.isEmpty()) return this
        val currentId = selectedOptionId(category)
        val currentIndex = options.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        val nextIndex = Math.floorMod(currentIndex + delta, options.size)
        return withOption(category, options[nextIndex].id)
    }

    /**
     * Builds the deterministic DiceBear 9.x `notionists` PNG URL.
     * Strictly uses opaque [seed] and explicit trait query parameters (never Contact PII).
     */
    fun toDiceBearPngUrl(sizePx: Int = NotionistCatalog.DEFAULT_PNG_SIZE_PX): String {
        val clampedSize = sizePx.coerceIn(32, 256)
        val params = linkedMapOf(
            "seed" to seed,
            "size" to clampedSize.toString(),
            "backgroundColor" to backgroundColor.lowercase(),
            "hair" to hair,
            "eyes" to eyes,
            "brows" to brows,
            "lips" to lips,
            "nose" to nose,
            "body" to body
        )

        if (beard != null) {
            params["beard"] = beard
            params["beardProbability"] = "100"
        } else {
            params["beardProbability"] = "0"
        }

        if (glasses != null) {
            params["glasses"] = glasses
            params["glassesProbability"] = "100"
        } else {
            params["glassesProbability"] = "0"
        }

        if (gesture != null) {
            params["gesture"] = gesture
            params["gestureProbability"] = "100"
        } else {
            params["gestureProbability"] = "0"
        }

        val query = params.entries.joinToString("&") { (k, v) ->
            "${encodeQueryComponent(k)}=${encodeQueryComponent(v)}"
        }
        return "${NotionistCatalog.DICEBEAR_API_BASE_URL}?$query"
    }

    /**
     * Serializes this recipe into a compact, dependency-free JSON string for Room and Export Bundle storage.
     */
    fun toJson(): String = buildString {
        append("{")
        append("\"seed\":${quoteJsonString(seed)},")
        append("\"hair\":${quoteJsonString(hair)},")
        append("\"eyes\":${quoteJsonString(eyes)},")
        append("\"brows\":${quoteJsonString(brows)},")
        append("\"lips\":${quoteJsonString(lips)},")
        append("\"nose\":${quoteJsonString(nose)},")
        append("\"beard\":${if (beard != null) quoteJsonString(beard) else "null"},")
        append("\"glasses\":${if (glasses != null) quoteJsonString(glasses) else "null"},")
        append("\"body\":${quoteJsonString(body)},")
        append("\"gesture\":${if (gesture != null) quoteJsonString(gesture) else "null"},")
        append("\"backgroundColor\":${quoteJsonString(backgroundColor.lowercase())}")
        append("}")
    }

    companion object {
        private val OPAQUE_SEED_PATTERN = Regex("^[a-zA-Z0-9_-]{4,64}$")
        private val JSON_FIELD_REGEX = Regex("\"([a-zA-Z0-9_]+)\"\\s*:\\s*(null|\"((?:\\\\.|[^\"\\\\])*)\")")

        /**
         * Generates a random opaque seed token containing zero Contact PII.
         */
        fun generateOpaqueSeed(): String =
            UUID.randomUUID().toString().replace("-", "").take(12)

        /**
         * Creates a randomized Notionist Avatar recipe with an opaque seed and balanced accessory probabilities.
         */
        fun random(random: Random = Random.Default): NotionistAvatarConfig {
            val hair = NotionistCatalog.hairOptions[random.nextInt(NotionistCatalog.hairOptions.size)].id!!
            val eyes = NotionistCatalog.eyesOptions[random.nextInt(NotionistCatalog.eyesOptions.size)].id!!
            val brows = NotionistCatalog.browsOptions[random.nextInt(NotionistCatalog.browsOptions.size)].id!!
            val lips = NotionistCatalog.lipsOptions[random.nextInt(NotionistCatalog.lipsOptions.size)].id!!
            val nose = NotionistCatalog.noseOptions[random.nextInt(NotionistCatalog.noseOptions.size)].id!!
            val body = NotionistCatalog.bodyOptions[random.nextInt(NotionistCatalog.bodyOptions.size)].id!!
            val background = NotionistCatalog.backgroundSwatches[random.nextInt(NotionistCatalog.backgroundSwatches.size)].hex

            val nonNullBeards = NotionistCatalog.beardOptions.mapNotNull { it.id }
            val beard = if (random.nextFloat() < 0.2f) nonNullBeards[random.nextInt(nonNullBeards.size)] else null

            val nonNullGlasses = NotionistCatalog.glassesOptions.mapNotNull { it.id }
            val glasses = if (random.nextFloat() < 0.3f) nonNullGlasses[random.nextInt(nonNullGlasses.size)] else null

            val nonNullGestures = NotionistCatalog.gestureOptions.mapNotNull { it.id }
            val gesture = if (random.nextFloat() < 0.25f) nonNullGestures[random.nextInt(nonNullGestures.size)] else null

            val opaqueSeed = buildString(12) {
                val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
                repeat(12) { append(alphabet[random.nextInt(alphabet.length)]) }
            }

            return NotionistAvatarConfig(
                seed = opaqueSeed,
                hair = hair,
                eyes = eyes,
                brows = brows,
                lips = lips,
                nose = nose,
                beard = beard,
                glasses = glasses,
                body = body,
                gesture = gesture,
                backgroundColor = background
            )
        }

        /**
         * Parses a persisted JSON recipe back into [NotionistAvatarConfig], returning null if blank or invalid.
         */
        fun fromJson(raw: String?): NotionistAvatarConfig? {
            val trimmed = raw?.trim()
            if (trimmed.isNullOrEmpty() || !trimmed.startsWith("{") || !trimmed.endsWith("}")) {
                return null
            }

            val map = mutableMapOf<String, String?>()
            JSON_FIELD_REGEX.findAll(trimmed).forEach { match ->
                val key = match.groupValues[1]
                val rawValue = match.groupValues[2]
                map[key] = if (rawValue == "null") null else unescapeJsonString(match.groupValues[3])
            }

            if (map.isEmpty()) return null

            val defaults = NotionistAvatarConfig()
            val seed = map["seed"]?.takeIf { OPAQUE_SEED_PATTERN.matches(it) } ?: generateOpaqueSeed()
            val hair = map["hair"]?.takeIf { it in NotionistCatalog.validHairIds } ?: defaults.hair
            val eyes = map["eyes"]?.takeIf { it in NotionistCatalog.validEyesIds } ?: defaults.eyes
            val brows = map["brows"]?.takeIf { it in NotionistCatalog.validBrowsIds } ?: defaults.brows
            val lips = map["lips"]?.takeIf { it in NotionistCatalog.validLipsIds } ?: defaults.lips
            val nose = map["nose"]?.takeIf { it in NotionistCatalog.validNoseIds } ?: defaults.nose
            val beard = map["beard"]?.takeIf { it in NotionistCatalog.validBeardIds }
            val glasses = map["glasses"]?.takeIf { it in NotionistCatalog.validGlassesIds }
            val body = map["body"]?.takeIf { it in NotionistCatalog.validBodyIds } ?: defaults.body
            val gesture = map["gesture"]?.takeIf { it in NotionistCatalog.validGestureIds }
            val background = map["backgroundColor"]?.lowercase()
                ?.takeIf { it in NotionistCatalog.validBackgroundHexes }
                ?: defaults.backgroundColor

            return NotionistAvatarConfig(
                seed = seed,
                hair = hair,
                eyes = eyes,
                brows = brows,
                lips = lips,
                nose = nose,
                beard = beard,
                glasses = glasses,
                body = body,
                gesture = gesture,
                backgroundColor = background
            )
        }

        private fun encodeQueryComponent(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8.name())

        private fun quoteJsonString(value: String): String {
            val escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
            return "\"$escaped\""
        }

        private fun unescapeJsonString(value: String): String = value
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}

/**
 * Builds 100% isolated, centered 128x128px PNG thumbnail URLs and versioned disk cache keys
 * for visual trait tiles in the Avatar Playground.
 *
 * Uses `notionists-neutral` for facial features (Brows, Eyes, Lips, Nose, Glasses) and
 * empty-layer isolation on `notionists` for full-canvas features (Hair, Beard, Body, Gesture),
 * keeping every tile crisp (~2.5-5 KB) and shared across all Contacts with zero Contact PII.
 */
object NotionistThumbnailUrlBuilder {
    const val THUMBNAIL_SIZE_PX = 128
    const val CANONICAL_THUMB_SEED = "hellothumb"
    const val CACHE_VERSION_PREFIX = "v2"
    const val DICEBEAR_NEUTRAL_API_BASE_URL = "https://api.dicebear.com/9.x/notionists-neutral/png"

    private data class Framing(
        val scale: Int,
        val translateX: Int,
        val translateY: Int
    )

    private fun framingFor(category: NotionistTraitCategory): Framing = when (category) {
        NotionistTraitCategory.BROWS -> Framing(scale = 135, translateX = 4, translateY = 32)
        NotionistTraitCategory.EYES -> Framing(scale = 135, translateX = -2, translateY = 18)
        NotionistTraitCategory.LIPS -> Framing(scale = 145, translateX = 10, translateY = -28)
        NotionistTraitCategory.NOSE -> Framing(scale = 165, translateX = -6, translateY = 4)
        NotionistTraitCategory.GLASSES -> Framing(scale = 100, translateX = 4, translateY = 12)
        NotionistTraitCategory.HAIR -> Framing(scale = 145, translateX = 4, translateY = 26)
        NotionistTraitCategory.BEARD -> Framing(scale = 200, translateX = -4, translateY = -10)
        NotionistTraitCategory.BODY -> Framing(scale = 115, translateX = 0, translateY = -28)
        NotionistTraitCategory.GESTURE -> Framing(scale = 115, translateX = -36, translateY = -20)
        NotionistTraitCategory.BACKGROUND -> Framing(scale = 100, translateX = 0, translateY = 0)
    }

    fun isNeutralIsolatedCategory(category: NotionistTraitCategory): Boolean = when (category) {
        NotionistTraitCategory.BROWS,
        NotionistTraitCategory.EYES,
        NotionistTraitCategory.LIPS,
        NotionistTraitCategory.NOSE,
        NotionistTraitCategory.GLASSES -> true
        NotionistTraitCategory.HAIR,
        NotionistTraitCategory.BEARD,
        NotionistTraitCategory.BODY,
        NotionistTraitCategory.GESTURE,
        NotionistTraitCategory.BACKGROUND -> false
    }

    fun cacheKey(category: NotionistTraitCategory, optionId: String): String {
        val sanitized = optionId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "${CACHE_VERSION_PREFIX}_${category.name.lowercase()}_$sanitized"
    }

    fun buildThumbnailUrl(category: NotionistTraitCategory, optionId: String): String {
        val framing = framingFor(category)
        val encodedOption = URLEncoder.encode(optionId, StandardCharsets.UTF_8.name())

        return if (isNeutralIsolatedCategory(category)) {
            val brows = if (category == NotionistTraitCategory.BROWS) encodedOption else ""
            val eyes = if (category == NotionistTraitCategory.EYES) encodedOption else ""
            val lips = if (category == NotionistTraitCategory.LIPS) encodedOption else ""
            val nose = if (category == NotionistTraitCategory.NOSE) encodedOption else ""
            val glassesPart = if (category == NotionistTraitCategory.GLASSES) {
                "glasses=$encodedOption&glassesProbability=100"
            } else {
                "glassesProbability=0"
            }
            "$DICEBEAR_NEUTRAL_API_BASE_URL?" +
                "seed=$CANONICAL_THUMB_SEED" +
                "&size=$THUMBNAIL_SIZE_PX" +
                "&backgroundColor=ffffff" +
                "&brows=$brows" +
                "&eyes=$eyes" +
                "&lips=$lips" +
                "&nose=$nose" +
                "&$glassesPart" +
                "&scale=${framing.scale}" +
                "&translateX=${framing.translateX}" +
                "&translateY=${framing.translateY}"
        } else {
            val hair = if (category == NotionistTraitCategory.HAIR) encodedOption else ""
            val body = if (category == NotionistTraitCategory.BODY) encodedOption else ""
            val beardPart = if (category == NotionistTraitCategory.BEARD) {
                "beard=$encodedOption&beardProbability=100"
            } else {
                "beardProbability=0"
            }
            val gesturePart = if (category == NotionistTraitCategory.GESTURE) {
                "gesture=$encodedOption&gestureProbability=100"
            } else {
                "gestureProbability=0"
            }
            "${NotionistCatalog.DICEBEAR_API_BASE_URL}?" +
                "seed=$CANONICAL_THUMB_SEED" +
                "&size=$THUMBNAIL_SIZE_PX" +
                "&backgroundColor=ffffff" +
                "&base=" +
                "&hair=$hair" +
                "&body=$body" +
                "&bodyIconProbability=0" +
                "&eyes=" +
                "&brows=" +
                "&lips=" +
                "&nose=" +
                "&$beardPart" +
                "&glassesProbability=0" +
                "&$gesturePart" +
                "&scale=${framing.scale}" +
                "&translateX=${framing.translateX}" +
                "&translateY=${framing.translateY}"
        }
    }
}
