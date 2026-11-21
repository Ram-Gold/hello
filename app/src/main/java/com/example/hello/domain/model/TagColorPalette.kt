package com.example.hello.domain.model

/**
 * Mode-specific hex tokens for a Notion-inspired tag color.
 */
data class TagColorToneHex(
    val containerHex: String,
    val contentHex: String,
    val borderHex: String,
    val selectedContainerHex: String,
    val selectedContentHex: String
)

/**
 * Complete light and dark mode specification for a curated tag color.
 */
data class TagColorSpec(
    val name: String,
    val light: TagColorToneHex,
    val dark: TagColorToneHex
)

/**
 * Curated Notion-inspired palette and deterministic auto-assignment of colors for tags.
 *
 * Canonical `Tag.colorHex` stores the light-mode `containerHex` for backward compatibility
 * with Room persistence and JSON/Markdown/CSV exports.
 */
object TagColorPalette {
    val PALETTE: List<TagColorSpec> = listOf(
        TagColorSpec(
            name = "Warm Cream",
            light = TagColorToneHex(
                containerHex = "#FBF3DB",
                contentHex = "#6B5324",
                borderHex = "#EADFB8",
                selectedContainerHex = "#D9B65B",
                selectedContentHex = "#252015"
            ),
            dark = TagColorToneHex(
                containerHex = "#3A3324",
                contentHex = "#F0DEAC",
                borderHex = "#524833",
                selectedContainerHex = "#C9A449",
                selectedContentHex = "#1C1910"
            )
        ),
        TagColorSpec(
            name = "Soft Gray",
            light = TagColorToneHex(
                containerHex = "#F1F1EF",
                contentHex = "#4A4945",
                borderHex = "#DCDCD8",
                selectedContainerHex = "#787774",
                selectedContentHex = "#FFFFFF"
            ),
            dark = TagColorToneHex(
                containerHex = "#2F2F2E",
                contentHex = "#D3D3CF",
                borderHex = "#454543",
                selectedContainerHex = "#9B9A97",
                selectedContentHex = "#181817"
            )
        ),
        TagColorSpec(
            name = "Peach",
            light = TagColorToneHex(
                containerHex = "#FAEBDD",
                contentHex = "#8F4B24",
                borderHex = "#EED3BC",
                selectedContainerHex = "#D9730D",
                selectedContentHex = "#FFFFFF"
            ),
            dark = TagColorToneHex(
                containerHex = "#3D2B1F",
                contentHex = "#F2BA8E",
                borderHex = "#593E2C",
                selectedContainerHex = "#E58A3C",
                selectedContentHex = "#1E140D"
            )
        ),
        TagColorSpec(
            name = "Amber",
            light = TagColorToneHex(
                containerHex = "#FDECC8",
                contentHex = "#7A5616",
                borderHex = "#F0D6A1",
                selectedContainerHex = "#CB912F",
                selectedContentHex = "#FFFFFF"
            ),
            dark = TagColorToneHex(
                containerHex = "#3E321D",
                contentHex = "#F2CA7E",
                borderHex = "#594829",
                selectedContainerHex = "#DFAB4A",
                selectedContentHex = "#1D170B"
            )
        ),
        TagColorSpec(
            name = "Sage",
            light = TagColorToneHex(
                containerHex = "#DBEDDB",
                contentHex = "#2D603B",
                borderHex = "#BEDBBF",
                selectedContainerHex = "#448361",
                selectedContentHex = "#FFFFFF"
            ),
            dark = TagColorToneHex(
                containerHex = "#23372A",
                contentHex = "#9ED3AE",
                borderHex = "#34523E",
                selectedContainerHex = "#5CA37B",
                selectedContentHex = "#0F1B14"
            )
        ),
        TagColorSpec(
            name = "Sky",
            light = TagColorToneHex(
                containerHex = "#D3E5EF",
                contentHex = "#245573",
                borderHex = "#B6D2E2",
                selectedContainerHex = "#337EA9",
                selectedContentHex = "#FFFFFF"
            ),
            dark = TagColorToneHex(
                containerHex = "#1E3240",
                contentHex = "#93C6E4",
                borderHex = "#2D4B5F",
                selectedContainerHex = "#529CCA",
                selectedContentHex = "#0D1922"
            )
        ),
        TagColorSpec(
            name = "Rose",
            light = TagColorToneHex(
                containerHex = "#FFE2DD",
                contentHex = "#8C322A",
                borderHex = "#F2C4BD",
                selectedContainerHex = "#D44C47",
                selectedContentHex = "#FFFFFF"
            ),
            dark = TagColorToneHex(
                containerHex = "#412423",
                contentHex = "#F5A69E",
                borderHex = "#5E3433",
                selectedContainerHex = "#E06B66",
                selectedContentHex = "#1F0F0E"
            )
        ),
        TagColorSpec(
            name = "Lavender",
            light = TagColorToneHex(
                containerHex = "#E8DEEE",
                contentHex = "#5A3973",
                borderHex = "#D3C1DF",
                selectedContainerHex = "#9065B0",
                selectedContentHex = "#FFFFFF"
            ),
            dark = TagColorToneHex(
                containerHex = "#33273E",
                contentHex = "#CCAEE2",
                borderHex = "#4B395B",
                selectedContainerHex = "#A87BC9",
                selectedContentHex = "#17111D"
            )
        )
    )

    val PALETTE_HEX: List<String> = PALETTE.map { it.light.containerHex }

    private val specByHex: Map<String, TagColorSpec> =
        PALETTE.associateBy { it.light.containerHex.uppercase() }

    fun findSpec(colorHex: String): TagColorSpec? =
        specByHex[colorHex.trim().uppercase()]

    fun getDeterministicColorHex(tagName: String): String {
        val clean = tagName.trim().removePrefix("#").lowercase()
        if (clean.isBlank()) return PALETTE_HEX.first()
        val index = (clean.hashCode().toLong() and 0x7FFFFFFF).toInt() % PALETTE_HEX.size
        return PALETTE_HEX[index]
    }
}
