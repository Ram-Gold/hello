package com.example.hello.domain.model

enum class HelloIconKey(val label: String) {
    GLOBE("All Groups"),
    BRIEFCASE("Work"),
    COFFEE("Coffee & Friends"),
    HOME("Family & Home"),
    GRADUATION_CAP("Uni & Study"),
    HEART("Close Circle"),
    STAR("Favorites"),
    USERS("Community"),
    COMPASS("Travel & Outdoors"),
    CODE("Tech & Builders"),
    MUSIC("Arts & Music"),
    BOOK("Reading & Ideas"),
    CAKE("Birthday"),
    CALENDAR("Calendar"),
    CLOCK("Reminder"),
    BELL("Reminders"),
    MAP_PIN("Location"),
    PHONE("Phone"),
    MAIL("Email"),
    SPARKLES("Highlight"),
    SEARCH("Search"),
    USER("Contact"),
    USER_PLUS("Add Contact"),
    REFRESH("Reconnect"),
    CHECK("Done"),
    TRASH("Delete"),
    PACKAGE("Export Bundle"),
    TAG("Tag");
}

fun extractAlphanumericInitials(name: String): String {
    return name.trim()
        .split(Regex("\\s+"))
        .mapNotNull { token -> token.firstOrNull { it.isLetterOrDigit() }?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
}

enum class CardType(val title: String, val iconKey: HelloIconKey) {
    WORK("WORK CARD", HelloIconKey.BRIEFCASE),
    SOCIAL("SOCIAL CARD", HelloIconKey.COFFEE)
}

/**
 * Zero-configuration subconscious check-in interval calculator.
 * Computes an adaptive threshold from past encounter history:
 * - For 2+ encounters: 1.5x average gap between recorded encounters, clamped to 14..90 days.
 * - For 0-1 encounters: 30-day baseline.
 */
object SubconsciousCheckInCalculator {
    const val MIN_ADAPTIVE_DAYS = 14
    const val MAX_ADAPTIVE_DAYS = 90
    const val BASELINE_DAYS = 30
    const val SNOOZE_DAYS = 14L
    const val MAX_SURFACED_PROMPTS = 2

    fun computeAdaptiveThresholdDays(encounterEpochDaysSortedAsc: List<Long>): Int {
        val distinctDays = encounterEpochDaysSortedAsc.distinct().sorted()
        if (distinctDays.size < 2) {
            return BASELINE_DAYS
        }
        val totalSpan = (distinctDays.last() - distinctDays.first()).coerceAtLeast(1L)
        val intervalsCount = distinctDays.size - 1
        val avgGap = totalSpan.toDouble() / intervalsCount.toDouble()
        val adaptive = kotlin.math.round(avgGap * 1.5).toInt()
        return adaptive.coerceIn(MIN_ADAPTIVE_DAYS, MAX_ADAPTIVE_DAYS)
    }

    fun formatSubconsciousPrompt(daysSince: Int, hasMultipleEncounters: Boolean): String {
        val durationLabel = when {
            daysSince >= 60 -> "${daysSince / 30} months ago"
            daysSince >= 14 -> "${daysSince / 7} weeks ago"
            daysSince == 1 -> "yesterday"
            else -> "$daysSince days ago"
        }
        val prefix = if (hasMultipleEncounters) "Met" else "Added"
        return "$prefix $durationLabel — anything new to update?"
    }
}

sealed interface UpcomingAlert {
    val id: String
    val contactName: String

    data class BirthdayAlert(
        override val id: String,
        override val contactName: String,
        val dateText: String
    ) : UpcomingAlert

    data class FollowUpReminder(
        override val id: String,
        override val contactName: String,
        val taskDescription: String,
        val dueText: String
    ) : UpcomingAlert

    data class CadenceAlert(
        override val id: String,
        override val contactName: String,
        val daysSinceLastMet: Int,
        val promptText: String,
        val contactId: String = ""
    ) : UpcomingAlert
}

data class ContactSummary(
    val id: String,
    val name: String,
    val contextSubtitle: String,
    val avatarInitials: String,
    val avatarBgIndex: Int = 0,
    val lastEncounterText: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val notes: String = "",
    val tags: List<Tag> = emptyList(),
    val isArchived: Boolean = false
)

data class MyCardData(
    val cardType: CardType,
    val userName: String,
    val headline: String,
    val email: String,
    val phone: String,
    val subtitle: String = ""
)

enum class HomeNavTab(val label: String) {
    HOME("Home"),
    PEOPLE("People"),
    TIMELINE("Timeline")
}

enum class SpeedDialAction {
    NEW_CONTACT,
    LOG_ENCOUNTER
}

enum class ContactSortMode(val label: String) {
    ALPHABETICAL("A–Z"),
    RECENTLY_ENCOUNTERED("Recent")
}
