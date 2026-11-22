package com.example.hello.domain.model

import java.time.Instant

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
 * Weighted-random serendipity selector for quiet, in-app-only Home reconnect prompts (ADR-0036).
 * - Eligible contacts: active, no pending FollowUp, not skipped today, and at least [MIN_GAP_DAYS]
 *   since their last recorded encounter (or creation anchor).
 * - Weighting: proportional to days since last met, clamped to [MIN_GAP_DAYS]..[MAX_WEIGHT_DAYS]
 *   so longer gaps are favored without letting multi-year gaps monopolize selection.
 * - Deterministic daily sampling seeded by `todayEpochDay`, keeping picks stable all day until skipped.
 */
object SerendipityPicker {
    const val MIN_GAP_DAYS = 14
    const val MAX_WEIGHT_DAYS = 365
    const val MAX_SURFACED_PROMPTS = 2

    data class WeightedCandidate<T>(
        val item: T,
        val stableId: String,
        val daysSinceLastMet: Int
    )

    fun <T> pickWeightedRandom(
        candidates: List<WeightedCandidate<T>>,
        todayEpochDay: Long,
        maxCount: Int = MAX_SURFACED_PROMPTS
    ): List<WeightedCandidate<T>> {
        if (candidates.isEmpty() || maxCount <= 0) return emptyList()

        val eligible = candidates
            .filter { it.daysSinceLastMet >= MIN_GAP_DAYS }
            .sortedBy { it.stableId }
            .toMutableList()
        if (eligible.isEmpty()) return emptyList()

        val rng = kotlin.random.Random(todayEpochDay * 6364136223846793005L + 1442695040888963407L)
        val picked = mutableListOf<WeightedCandidate<T>>()
        val targetCount = minOf(maxCount, eligible.size)

        repeat(targetCount) {
            val totalWeight = eligible.sumOf { candidateWeight(it.daysSinceLastMet) }
            if (totalWeight <= 0.0) return@repeat
            var roll = rng.nextDouble() * totalWeight
            var selectedIndex = eligible.lastIndex
            for (i in eligible.indices) {
                roll -= candidateWeight(eligible[i].daysSinceLastMet)
                if (roll <= 0.0) {
                    selectedIndex = i
                    break
                }
            }
            picked += eligible.removeAt(selectedIndex)
        }

        return picked
    }

    fun candidateWeight(daysSinceLastMet: Int): Double =
        daysSinceLastMet.coerceIn(MIN_GAP_DAYS, MAX_WEIGHT_DAYS).toDouble()

    fun formatSerendipityContext(daysSince: Int, hasMultipleEncounters: Boolean): String {
        val durationLabel = when {
            daysSince >= 60 -> "${daysSince / 30} months ago"
            daysSince >= 14 -> "${daysSince / 7} weeks ago"
            daysSince == 1 -> "yesterday"
            else -> "$daysSince days ago"
        }
        val prefix = if (hasMultipleEncounters) "Last met" else "Added"
        return "$prefix $durationLabel"
    }
}

sealed interface UpcomingAlert {
    val id: String
    val contactName: String

    data class BirthdayAlert(
        override val id: String,
        override val contactName: String,
        val dateText: String,
        val contactId: String = ""
    ) : UpcomingAlert

    data class FollowUpReminder(
        override val id: String,
        override val contactName: String,
        val taskDescription: String,
        val dueText: String
    ) : UpcomingAlert

    data class SerendipityPrompt(
        override val id: String,
        override val contactName: String,
        val daysSinceLastMet: Int,
        val contextText: String,
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
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val updatedAt: Instant? = null,
    val photoUri: String? = null
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
    ALPHABETICAL("A-Z"),
    RECENTLY_ENCOUNTERED("Recent")
}
