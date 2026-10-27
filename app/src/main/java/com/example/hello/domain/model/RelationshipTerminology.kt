package com.example.hello.domain.model

/**
 * Presentation layer preference allowing users to personalize the display nomenclature
 * applied to Contacts across the UI without modifying core domain models (ADR 0005).
 */
enum class RelationshipTerminology(
    val id: String,
    val singularTitle: String,
    val pluralTitle: String,
    val description: String
) {
    PEOPLE(
        id = "people",
        singularTitle = "Person",
        pluralTitle = "People",
        description = "Warm, universal, and grounded"
    ),
    FRIENDS(
        id = "friends",
        singularTitle = "Friend",
        pluralTitle = "Friends",
        description = "Personal, intimate, and informal"
    ),
    CONNECTIONS(
        id = "connections",
        singularTitle = "Connection",
        pluralTitle = "Connections",
        description = "Professional, networking, and exploratory"
    ),
    CONTACTS(
        id = "contacts",
        singularTitle = "Contact",
        pluralTitle = "Contacts",
        description = "Direct, classic, and focused"
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

    companion object {
        val DEFAULT = PEOPLE

        fun fromId(id: String?): RelationshipTerminology {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
