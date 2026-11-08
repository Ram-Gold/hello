package com.example.hello.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.preferences.UserPreferencesRepository
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.CardType
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.HomeNavTab
import com.example.hello.domain.model.MyCardData
import com.example.hello.domain.model.SpeedDialAction
import com.example.hello.domain.model.SubconsciousCheckInCalculator
import com.example.hello.domain.model.UpcomingAlert
import com.example.hello.domain.model.extractAlphanumericInitials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(
    val repository: ContactRepository? = null,
    val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        if (repository != null) {
            viewModelScope.launch {
                repository.recentContacts.collect { recents ->
                    _uiState.update { it.copy(recentContacts = recents) }
                }
            }
            viewModelScope.launch {
                repository.favoriteContacts.collect { favorites ->
                    _uiState.update { it.copy(favoriteContacts = favorites) }
                }
            }
            viewModelScope.launch {
                repository.upcomingAlerts.collect { alerts ->
                    _uiState.update { it.copy(upcomingAlerts = alerts) }
                }
            }
        }
        if (preferencesRepository != null) {
            viewModelScope.launch {
                preferencesRepository.userPreferencesFlow.collect { prefs ->
                    _uiState.update { it.copy(userName = prefs.userName) }
                }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.OnCardTypeSelect -> {
                _uiState.update { it.copy(activeCardType = event.cardType) }
            }
            is HomeUiEvent.OnExpandQr -> {
                _uiState.update { it.copy(isQrExpanded = event.expanded) }
            }
            is HomeUiEvent.OnReminderDone -> {
                val targetAlert = _uiState.value.upcomingAlerts.firstOrNull { it.id == event.alertId }
                if (targetAlert is UpcomingAlert.CadenceAlert && targetAlert.contactId.isNotBlank()) {
                    repository?.logEncounterForContact(
                        contactId = targetAlert.contactId,
                        note = "Caught up recently"
                    )
                } else {
                    repository?.markFollowUpDone(event.alertId)
                }
                _uiState.update { state ->
                    val updated = state.upcomingAlerts.filterNot { it.id == event.alertId }
                    state.copy(
                        upcomingAlerts = updated,
                        userMessage = if (targetAlert is UpcomingAlert.CadenceAlert) "Updated!" else "Marked as done!"
                    )
                }
            }
            is HomeUiEvent.OnReminderSnooze -> {
                val targetAlert = _uiState.value.upcomingAlerts.firstOrNull { it.id == event.alertId }
                val isSubconsciousCheckIn = targetAlert is UpcomingAlert.CadenceAlert
                val snoozeDays = if (isSubconsciousCheckIn) SubconsciousCheckInCalculator.SNOOZE_DAYS else 3L
                repository?.snoozeFollowUp(event.alertId, days = snoozeDays)
                _uiState.update { state ->
                    val updated = state.upcomingAlerts.filterNot { it.id == event.alertId }
                    state.copy(
                        upcomingAlerts = updated,
                        userMessage = if (isSubconsciousCheckIn) "Snoozed for 2 weeks" else "Snoozed for 3 days"
                    )
                }
            }
            is HomeUiEvent.OnQuickAddContact -> {
                val addedSummary = if (repository != null) {
                    repository.addContactWithInitialEncounter(
                        name = event.name,
                        tagIds = emptySet(),
                        placeId = null,
                        customPlaceName = null,
                        note = event.initialNote,
                        followUpDays = 14
                    )
                } else {
                    val newContact = ContactSummary(
                        id = UUID.randomUUID().toString(),
                        name = event.name.trim().ifEmpty { "New Friend" },
                        contextSubtitle = event.initialNote.trim().ifEmpty { "Added just now" },
                        avatarInitials = extractAlphanumericInitials(event.name),
                        avatarBgIndex = (_uiState.value.recentContacts.size % 6),
                        lastEncounterText = "Met today"
                    )
                    _uiState.update { state ->
                        state.copy(
                            recentContacts = listOf(newContact) + state.recentContacts
                        )
                    }
                    newContact
                }
                _uiState.update { state ->
                    state.copy(
                        userMessage = "Added ${addedSummary.name} to contacts!"
                    )
                }
            }
            is HomeUiEvent.OnQuickUpdateContact -> {
                if (repository != null) {
                    repository.logEncounterForContact(
                        contactId = event.contactId,
                        note = event.note
                    )
                }
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
                        userMessage = "Contact updated!"
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
                    val actionMessage = when (event.action) {
                        SpeedDialAction.NEW_CONTACT -> "Quick Add Contact"
                        SpeedDialAction.LOG_ENCOUNTER -> "Log Encounter"
                    }
                    state.copy(
                        selectedNavTab = HomeNavTab.HOME,
                        isSpeedDialOpen = false,
                        userMessage = actionMessage
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
        }
    }

    fun syncRepositoryData(
        recentContacts: List<ContactSummary>,
        upcomingAlerts: List<UpcomingAlert>,
        favoriteContacts: List<ContactSummary> = _uiState.value.favoriteContacts
    ) {
        _uiState.update { state ->
            state.copy(
                recentContacts = recentContacts,
                upcomingAlerts = upcomingAlerts,
                favoriteContacts = favoriteContacts
            )
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
                subtitle = "Design Systems • Local-First"
            )
            val socialCard = MyCardData(
                cardType = CardType.SOCIAL,
                userName = "Alex Rivera",
                headline = "Weekend Cyclist & Bookworm",
                email = "alex@hey.com",
                phone = "+1 (415) 890-1234",
                subtitle = "Specialty Coffee • Gravel Bikes"
            )

            val upcoming = listOf(
                UpcomingAlert.BirthdayAlert(
                    id = "alert-1",
                    contactName = "Maya Patel",
                    dateText = "Tomorrow"
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
                    avatarInitials = "MP",
                    avatarBgIndex = 0,
                    lastEncounterText = "Met yesterday"
                ),
                ContactSummary(
                    id = "c-2",
                    name = "Marcus Vance",
                    contextSubtitle = "Founder @ Studio Arc • Met at Coffee Lab",
                    avatarInitials = "MV",
                    avatarBgIndex = 1,
                    lastEncounterText = "4 days ago"
                ),
                ContactSummary(
                    id = "c-3",
                    name = "Elena Rostova",
                    contextSubtitle = "Neighbor & Botanist • Exchanged plants last Sunday",
                    avatarInitials = "ER",
                    avatarBgIndex = 2,
                    lastEncounterText = "Last Sunday"
                ),
                ContactSummary(
                    id = "c-4",
                    name = "David Kim",
                    contextSubtitle = "Mobile Dev • Discussed Compose gestures & spring physics",
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
                isSpeedDialOpen = false,
                selectedNavTab = HomeNavTab.HOME
            )
        }
    }
}
