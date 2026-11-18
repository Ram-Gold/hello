package com.example.hello.ui.contacts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.hello.domain.model.ContactSummary
import org.junit.Rule
import org.junit.Test

class ArchivedContactsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyArchivedContacts_displaysZeroWithoutTotalPrefix() {
        composeTestRule.setContent {
            ArchivedContactsScreen(
                contacts = emptyList(),
                onRestoreContact = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total:", substring = true).assertDoesNotExist()
    }

    @Test
    fun nonEmptyArchivedContacts_displaysCountWithoutTotalPrefix() {
        val archivedContacts = listOf(
            ContactSummary(
                id = "c-1",
                name = "Alice",
                contextSubtitle = "Colleague",
                avatarInitials = "AL",
                isArchived = true
            ),
            ContactSummary(
                id = "c-2",
                name = "Bob",
                contextSubtitle = "Friend",
                avatarInitials = "BO",
                isArchived = true
            )
        )

        composeTestRule.setContent {
            ArchivedContactsScreen(
                contacts = archivedContacts,
                onRestoreContact = {},
                onNavigateBack = {}
            )
        }

        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total:", substring = true).assertDoesNotExist()
    }
}
