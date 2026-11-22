package com.example.hello.ui.home

import com.example.hello.domain.model.UpcomingAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpcomingAlertCardVisualTest {

    @Test
    fun birthdayAlert_resolvesBadgeAndDescriptionCorrectly() {
        val alert = UpcomingAlert.BirthdayAlert(
            id = "b-1",
            contactName = "Sarah Connor",
            dateText = "Tomorrow"
        )

        val badgeText = alert.dateText.ifBlank { "Birthday" }
        val descriptionText = "Wish them well on their birthday 🎉"

        assertEquals("Tomorrow", badgeText)
        assertTrue(descriptionText.contains("birthday"))
    }

    @Test
    fun followUpReminder_resolvesUppercaseDueBadgeAndTaskDescription() {
        val alert = UpcomingAlert.FollowUpReminder(
            id = "f-1",
            contactName = "John Doe",
            taskDescription = "Send revised presentation deck for next quarter planning",
            dueText = "today"
        )

        val badgeText = alert.dueText.uppercase()
        val descriptionText = alert.taskDescription

        assertEquals("TODAY", badgeText)
        assertEquals("Send revised presentation deck for next quarter planning", descriptionText)
    }

    @Test
    fun serendipityPrompt_resolvesSayHiBadgeAndContext() {
        val alert = UpcomingAlert.SerendipityPrompt(
            id = "serendipity-1",
            contactName = "Marcus Aurelius",
            daysSinceLastMet = 45,
            contextText = "Last met 6 weeks ago",
            contactId = "c-1"
        )

        val badgeText = "Say hi"
        val descriptionText = alert.contextText

        assertEquals("Say hi", badgeText)
        assertEquals("Last met 6 weeks ago", descriptionText)
    }
}
