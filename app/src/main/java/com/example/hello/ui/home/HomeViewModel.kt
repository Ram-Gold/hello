package com.example.hello.ui.home

import androidx.lifecycle.ViewModel
import com.example.hello.domain.model.AccordionType
import com.example.hello.domain.model.CardType
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.MyCardData
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.domain.model.UpcomingAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnCardTypeSelect -> {
                _uiState.update { it.copy(activeCardType = event.cardType) }
            }
            is HomeUiEvent.OnExpandQr -> {
                _uiState.update { it.copy(isQrExpanded = event.expanded) }
            }
            is HomeUiEvent.OnReminderDone -> {
                _uiState.update { state ->
                    val updated = state.upcomingAlerts.filterNot { it.id == event.alertId }
                    state.copy(
                        upcomingAlerts = updated,
                        userMessage = "Marked as done! ✨"
                    )
                }
            }
            is HomeUiEvent.OnReminderSnooze -> {
                _uiState.update { state ->
                    val updated = state.upcomingAlerts.filterNot { it.id == event.alertId }
                    state.copy(
                        upcomingAlerts = updated,
                        userMessage = "Snoozed for 3 days ⏰"
                    )
                }
            }
            is HomeUiEvent.OnToggleAccordion -> {
                _uiState.update { state ->
                    val nextAccordion = if (state.expandedAccordion == event.type) null else event.type
                    state.copy(
                        expandedAccordion = nextAccordion,
                        isSpeedDialOpen = false
                    )
                }
            }
            is HomeUiEvent.OnCloseAccordion -> {
                _uiState.update { it.copy(expandedAccordion = null) }
            }
            is HomeUiEvent.OnQuickAddContact -> {
                val newContact = ContactSummary(
                    id = UUID.randomUUID().toString(),
                    name = event.name.trim().ifEmpty { "New Friend" },
                    contextSubtitle = event.initialNote.trim().ifEmpty { "Added just now" },
                    circles = event.circles.toList().ifEmpty { listOf(Circle.FRIENDS) },
                    avatarInitials = event.name.trim().split(" ")
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()
                        .ifEmpty { "👋" },
                    avatarBgIndex = (_uiState.value.recentContacts.size % 6),
                    lastEncounterText = "Met today"
                )
                _uiState.update { state ->
                    state.copy(
                        recentContacts = listOf(newContact) + state.recentContacts,
                        expandedAccordion = null,
                        userMessage = "Added ${newContact.name} to contacts! 🤝"
                    )
                }
            }
            is HomeUiEvent.OnQuickUpdateContact -> {
                _uiState.update { state ->
                    val updatedList = state.recentContacts.map { contact ->
                        if (contact.id == event.contactId) {
                            contact.copy(
                                contextSubtitle = event.note.trim().ifEmpty { contact.contextSubtitle },
                                lastEncounterText = "Updated just now"
                            )
                        } else {
                            contact
                        }
                    }
                    state.copy(
                        recentContacts = updatedList,
                        expandedAccordion = null,
                        userMessage = "Contact updated! 📝"
                    )
                }
            }
            is HomeUiEvent.OnContactClick -> {
                _uiState.update {
                    it.copy(userMessage = "Opened profile: ${event.contactId}")
                }
            }
            is HomeUiEvent.OnToggleSpeedDial -> {
                _uiState.update { it.copy(isSpeedDialOpen = !it.isSpeedDialOpen) }
            }
            is HomeUiEvent.OnCloseSpeedDial -> {
                _uiState.update { it.copy(isSpeedDialOpen = false) }
            }
            is HomeUiEvent.OnSpeedDialAction -> {
                _uiState.update { state ->
                    val targetAccordion = when (event.action) {
                        SpeedDialAction.NEW_CONTACT -> AccordionType.ADD_CONTACT
                        SpeedDialAction.LOG_ENCOUNTER -> AccordionType.UPDATE_CONTACT
                    }
                    state.copy(
                        selectedNavTab = HomeNavTab.HOME,
                        isSpeedDialOpen = false,
                        expandedAccordion = targetAccordion
                    )
                }
            }
            is HomeUiEvent.OnNavTabSelect -> {
                _uiState.update { it.copy(selectedNavTab = event.tab) }
            }
            is HomeUiEvent.OnSettingsClick -> {
                _uiState.update { it.copy(userMessage = "Settings & Local Markdown Export") }
            }
            is HomeUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
            is HomeUiEvent.OnOpenDrawer -> {
                _uiState.update { it.copy(isDrawerOpen = true) }
            }
            is HomeUiEvent.OnCloseDrawer -> {
                _uiState.update { it.copy(isDrawerOpen = false) }
            }
        }
    }

    companion object {
        fun createInitialState(): HomeUiState {
            val workCard = MyCardData(
                cardType = CardType.WORK,
                userName = "Alex Rivera",
                headline = "Product Designer & Tinkerer",
                email = "alex.rivera@craft.design",
                phone = "+1 (415) 890-1234",
                circleLabel = "Design Systems • Local-First"
            )
            val socialCard = MyCardData(
                cardType = CardType.SOCIAL,
                userName = "Alex Rivera",
                headline = "Weekend Cyclist & Bookworm",
                email = "alex@hey.com",
                phone = "+1 (415) 890-1234",
                circleLabel = "Specialty Coffee • Gravel Bikes"
            )

            val upcoming = listOf(
                UpcomingAlert.BirthdayAlert(
                    id = "alert-1",
                    contactName = "Maya Patel",
                    dateText = "Tomorrow",
                    circle = Circle.FRIENDS
                ),
                UpcomingAlert.FollowUpReminder(
                    id = "alert-2",
                    contactName = "Alex R.",
                    taskDescription = "Send design feedback from hackathon",
                    dueText = "In 3 days"
                ),
                UpcomingAlert.CadenceAlert(
                    id = "alert-3",
                    contactName = "Sarah L.",
                    daysSinceLastMet = 45,
                    promptText = "45 days since coffee chat"
                )
            )

            val contacts = listOf(
                ContactSummary(
                    id = "c-1",
                    name = "Maya Patel",
                    contextSubtitle = "CS '27 • Met yesterday at Hackathon",
                    circles = listOf(Circle.UNI, Circle.FRIENDS),
                    avatarInitials = "MP",
                    avatarBgIndex = 0,
                    lastEncounterText = "Met yesterday"
                ),
                ContactSummary(
                    id = "c-2",
                    name = "Marcus Vance",
                    contextSubtitle = "Founder @ Studio Arc • Met at Coffee Lab",
                    circles = listOf(Circle.WORK),
                    avatarInitials = "MV",
                    avatarBgIndex = 1,
                    lastEncounterText = "4 days ago"
                ),
                ContactSummary(
                    id = "c-3",
                    name = "Elena Rostova",
                    contextSubtitle = "Neighbor & Botanist • Exchanged plants last Sunday",
                    circles = listOf(Circle.FRIENDS, Circle.FAMILY),
                    avatarInitials = "ER",
                    avatarBgIndex = 2,
                    lastEncounterText = "Last Sunday"
                ),
                ContactSummary(
                    id = "c-4",
                    name = "David Kim",
                    contextSubtitle = "Mobile Dev • Discussed Compose gestures & spring physics",
                    circles = listOf(Circle.WORK, Circle.UNI),
                    avatarInitials = "DK",
                    avatarBgIndex = 3,
                    lastEncounterText = "10 days ago"
                )
            )

            return HomeUiState(
                userName = "Alex",
                activeCardType = CardType.WORK,
                myCards = mapOf(
                    CardType.WORK to workCard,
                    CardType.SOCIAL to socialCard
                ),
                isQrExpanded = false,
                upcomingAlerts = upcoming,
                recentContacts = contacts,
                expandedAccordion = null,
                isSpeedDialOpen = false,
                selectedNavTab = HomeNavTab.HOME
            )
        }
    }
}
