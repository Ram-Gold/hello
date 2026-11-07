package com.example.hello.domain.model

/**
 * Curated palette and deterministic auto-assignment of colors for tags.
 */
object TagColorPalette {
    val PALETTE_HEX = listOf(
        "#E06D3B", // Terracotta
        "#F3A261", // Amber
        "#7DAA82", // Sage
        "#D87D7D", // Dusty Rose
        "#6B8BA4", // Slate
        "#D4A373", // Ochre
        "#8A9A5B", // Olive
        "#B07D62"  // Clay
    )

    fun getDeterministicColorHex(tagName: String): String {
        val clean = tagName.trim().removePrefix("#").lowercase()
        if (clean.isBlank()) return PALETTE_HEX.first()
        val index = (clean.hashCode().toLong() and 0x7FFFFFFF).toInt() % PALETTE_HEX.size
        return PALETTE_HEX[index]
    }
}

