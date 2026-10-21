package com.example.hello.domain.model

enum class CardType(val title: String, val iconEmoji: String) {
    WORK("WORK CARD", "💼"),
    SOCIAL("SOCIAL CARD", "☕")
}

enum class Circle(val displayName: String) {
    WORK("Work"),
    FRIENDS("Friends"),
    FAMILY("Family"),
    UNI("Uni")
}

data class CircleGroupConfig(
    val circle: Circle,
    val cadenceDays: Int = 14,
    val defaultCardType: CardType = CardType.WORK,
    val isCadenceAlertEnabled: Boolean = true
)

sealed interface UpcomingAlert {
    val id: String
    val contactName: String

    data class BirthdayAlert(
        override val id: String,
        override val contactName: String,
        val dateText: String,
        val circle: Circle? = null
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
        val promptText: String
    ) : UpcomingAlert
}

data class ContactSummary(
    val id: String,
    val name: String,
    val contextSubtitle: String,
    val circles: List<Circle>,
    val avatarInitials: String,
    val avatarBgIndex: Int = 0,
    val lastEncounterText: String? = null
)

data class MyCardData(
    val cardType: CardType,
    val userName: String,
    val headline: String,
    val email: String,
    val phone: String,
    val circleLabel: String
)

enum class HomeNavTab(val label: String) {
    HOME("Home"),
    PEOPLE("People")
}

enum class AccordionType {
    ADD_CONTACT,
    UPDATE_CONTACT
}

enum class SpeedDialAction {
    NEW_CONTACT,
    LOG_ENCOUNTER
}

enum class ContactSortMode(val label: String) {
    ALPHABETICAL("A–Z"),
    RECENTLY_ENCOUNTERED("Recent")
}
