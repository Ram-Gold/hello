package com.example.hello.ui.contacts

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.hello.ui.theme.HelloTheme
import org.junit.Rule
import org.junit.Test

class ContactsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selectionMode_archiveAction_showsConfirmationAndArchivesSelectedContact() {
        val viewModel = ContactsViewModel()
        val contacts = ContactsViewModel.createSampleContacts()
        viewModel.syncContacts(contacts)
        val targetContact = contacts.first()
        val remainingContact = contacts[1]

        viewModel.onEvent(ContactsUiEvent.OnContactLongClick(targetContact.id))

        composeTestRule.setContent {
            HelloTheme {
                val uiState by viewModel.uiState.collectAsState()
                ContactsScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Archive contacts").performClick()
        composeTestRule.onNodeWithText("Archive 1 contact?").assertIsDisplayed()

        composeTestRule.onNodeWithText("Archive").performClick()
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            viewModel.uiState.value.contacts.none { it.id == targetContact.id }
        }

        composeTestRule.onNodeWithText(targetContact.name).assertDoesNotExist()
        composeTestRule.onNodeWithText(remainingContact.name).assertIsDisplayed()
    }
}
