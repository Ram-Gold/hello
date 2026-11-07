package com.example.hello.domain.model

/**
 * Two-Layer Presentation Nomenclature System (ADR 0005):
 * - Layer 1 (Universal Directory Container): Names the top-level directory ("People" or "Contacts")
 *   so the global container never collides with specific relationship spheres.
 * - Layer 2 (Context & Life Spheres): Individual relationships are organized via flat #tags
 *   (#friends, #work, #family, #uni) and closeness without rigid folder lock-in.
 */
enum class RelationshipTerminology(
    val id: String,
    val singularTitle: String,
    val pluralTitle: String,
    val description: String,
    val isPrimaryDirectoryOption: Boolean = false
) {
    PEOPLE(
        id = "people",
        singularTitle = "Person",
        pluralTitle = "People",
        description = "Warm, human, and universal — works naturally with #friends, #work & #family tags",
        isPrimaryDirectoryOption = true
    ),
    CONTACTS(
        id = "contacts",
        singularTitle = "Contact",
        pluralTitle = "Contacts",
        description = "Direct, classic, and neutral container for personal & professional spheres",
        isPrimaryDirectoryOption = true
    ),
    FRIENDS(
        id = "friends",
        singularTitle = "Friend",
        pluralTitle = "Friends",
        description = "Personal, intimate, and informal (Tip: use 'People' + #friends tag to avoid overlap)",
        isPrimaryDirectoryOption = false
    ),
    CONNECTIONS(
        id = "connections",
        singularTitle = "Connection",
        pluralTitle = "Connections",
        description = "Professional, networking, and exploratory",
        isPrimaryDirectoryOption = false
    );

    fun networkCounterText(count: Int): String {
        val noun = if (count == 1) singularTitle.lowercase() else pluralTitle.lowercase()
        return "$count $noun in your network"
    }

    fun recentCounterText(count: Int): String {
        val noun = if (count == 1) singularTitle.lowercase() else pluralTitle.lowercase()
        return "$count $noun"
    }

    fun addActionTitle(): String {
        return "Add $singularTitle"
    }

    fun emptyDirectoryTitle(): String {
        return "Your $pluralTitle directory is empty"
    }

    fun emptyDirectoryPrompt(): String {
        return "Add your first $singularTitle"
    }

    fun searchPlaceholder(): String {
        return "Search ${pluralTitle.lowercase()}, tags, or notes..."
    }

    fun emptyTagFilterMessage(tagName: String): String {
        val cleanTag = tagName.removePrefix("#").lowercase()
        return "No ${pluralTitle.lowercase()} tagged \"$cleanTag\" yet."
    }

    companion object {
        val DEFAULT = PEOPLE

        /**
         * Primary universal container options (Layer 1) recommended during onboarding
         * so the directory name never collides with Layer 2 #tags (#friends, #work, #family).
         */
        val primaryDirectoryOptions: List<RelationshipTerminology> = listOf(PEOPLE, CONTACTS)

        /**
         * Seeded starter tags (Layer 2) representing life spheres.
         */
        val coreSphereTags: List<String> = listOf("friends", "work", "family", "uni")

        fun fromId(id: String?): RelationshipTerminology {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
