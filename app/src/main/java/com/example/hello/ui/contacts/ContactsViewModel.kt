package com.example.hello.ui.contacts

import androidx.lifecycle.ViewModel
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSortMode
import com.example.hello.domain.model.ContactSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ContactsViewModel : ViewModel() {

    private var allContacts: List<ContactSummary> = createSampleContacts()

    private val _uiState = MutableStateFlow(
        computeState(
            searchQuery = "",
            selectedCircle = null,
            sortMode = ContactSortMode.ALPHABETICAL,
            contacts = allContacts
        )
    )
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    fun syncContacts(updatedContacts: List<ContactSummary>) {
        allContacts = updatedContacts
        _uiState.update { current ->
            computeState(
                searchQuery = current.searchQuery,
                selectedCircle = current.selectedCircle,
                sortMode = current.sortMode,
                contacts = allContacts
            )
        }
    }

    fun onEvent(event: ContactsUiEvent) {
        when (event) {
            is ContactsUiEvent.OnSearchQueryChange -> {
                _uiState.update { current ->
                    computeState(
                        searchQuery = event.query,
                        selectedCircle = current.selectedCircle,
                        sortMode = current.sortMode,
                        contacts = allContacts
                    )
                }
            }
            is ContactsUiEvent.OnClearSearch -> {
                _uiState.update { current ->
                    computeState(
                        searchQuery = "",
                        selectedCircle = current.selectedCircle,
                        sortMode = current.sortMode,
                        contacts = allContacts
                    )
                }
            }
            is ContactsUiEvent.OnSelectCircle -> {
                _uiState.update { current ->
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedCircle = event.circle,
                        sortMode = current.sortMode,
                        contacts = allContacts
                    )
                }
            }
            is ContactsUiEvent.OnToggleSortMode -> {
                _uiState.update { current ->
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedCircle = current.selectedCircle,
                        sortMode = event.mode,
                        contacts = allContacts
                    )
                }
            }
            is ContactsUiEvent.OnContactClick -> {
                _uiState.update { it.copy(userMessage = "Opened profile: ${event.contactId}") }
            }
            is ContactsUiEvent.OnClearFilters -> {
                _uiState.update { current ->
                    computeState(
                        searchQuery = "",
                        selectedCircle = null,
                        sortMode = current.sortMode,
                        contacts = allContacts
                    )
                }
            }
            is ContactsUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    companion object {
        fun computeState(
            searchQuery: String,
            selectedCircle: Circle?,
            sortMode: ContactSortMode,
            contacts: List<ContactSummary>
        ): ContactsUiState {
            val filtered = contacts.filter { contact ->
                val matchesQuery = if (searchQuery.isBlank()) {
                    true
                } else {
                    contact.name.contains(searchQuery, ignoreCase = true) ||
                            contact.contextSubtitle.contains(searchQuery, ignoreCase = true)
                }
                val matchesCircle = if (selectedCircle == null) {
                    true
                } else {
                    selectedCircle in contact.circles
                }
                matchesQuery && matchesCircle
            }

            val grouped = when (sortMode) {
                ContactSortMode.ALPHABETICAL -> {
                    filtered
                        .sortedBy { it.name }
                        .groupBy { it.name.firstOrNull()?.uppercase() ?: "#" }
                }
                ContactSortMode.RECENTLY_ENCOUNTERED -> {
                    filtered.groupBy { contact ->
                        val text = contact.lastEncounterText ?: "Earlier"
                        when {
                            text.contains("today", ignoreCase = true) || text.contains("yesterday", ignoreCase = true) -> "Recently Met"
                            text.contains("days", ignoreCase = true) || text.contains("Sunday", ignoreCase = true) -> "This Month"
                            else -> "Earlier"
                        }
                    }
                }
            }

            return ContactsUiState(
                searchQuery = searchQuery,
                selectedCircle = selectedCircle,
                sortMode = sortMode,
                contacts = filtered,
                groupedContacts = grouped,
                totalCount = filtered.size
            )
        }

        fun createSampleContacts(): List<ContactSummary> {
            return listOf(
                ContactSummary(
                    id = "c-1",
                    name = "Alex Rivera",
                    contextSubtitle = "Product Designer & Tinkerer",
                    circles = listOf(Circle.WORK),
                    avatarInitials = "AR",
                    avatarBgIndex = 0,
                    lastEncounterText = "Met today"
                ),
                ContactSummary(
                    id = "c-2",
                    name = "David Kim",
                    contextSubtitle = "Mobile Dev • Discussed Compose gestures",
                    circles = listOf(Circle.WORK, Circle.UNI),
                    avatarInitials = "DK",
                    avatarBgIndex = 1,
                    lastEncounterText = "10 days ago"
                ),
                ContactSummary(
                    id = "c-3",
                    name = "Elena Rostova",
                    contextSubtitle = "Neighbor & Botanist • Exchanged plants",
                    circles = listOf(Circle.FRIENDS, Circle.FAMILY),
                    avatarInitials = "ER",
                    avatarBgIndex = 2,
                    lastEncounterText = "Last Sunday"
                ),
                ContactSummary(
                    id = "c-4",
                    name = "Liam Chen",
                    contextSubtitle = "Architect • Coffee and urban photography",
                    circles = listOf(Circle.FRIENDS),
                    avatarInitials = "LC",
                    avatarBgIndex = 3,
                    lastEncounterText = "2 weeks ago"
                ),
                ContactSummary(
                    id = "c-5",
                    name = "Marcus Vance",
                    contextSubtitle = "Founder @ Studio Arc • Met at Coffee Lab",
                    circles = listOf(Circle.WORK),
                    avatarInitials = "MV",
                    avatarBgIndex = 4,
                    lastEncounterText = "4 days ago"
                ),
                ContactSummary(
                    id = "c-6",
                    name = "Maya Patel",
                    contextSubtitle = "CS '27 • Met yesterday at Hackathon",
                    circles = listOf(Circle.UNI, Circle.FRIENDS),
                    avatarInitials = "MP",
                    avatarBgIndex = 5,
                    lastEncounterText = "Met yesterday"
                ),
                ContactSummary(
                    id = "c-7",
                    name = "Noah Albright",
                    contextSubtitle = "High School Friend • Catch up quarterly",
                    circles = listOf(Circle.FRIENDS),
                    avatarInitials = "NA",
                    avatarBgIndex = 0,
                    lastEncounterText = "1 month ago"
                ),
                ContactSummary(
                    id = "c-8",
                    name = "Sarah Lin",
                    contextSubtitle = "Biotech Researcher • Tea tasting buddy",
                    circles = listOf(Circle.FAMILY, Circle.FRIENDS),
                    avatarInitials = "SL",
                    avatarBgIndex = 1,
                    lastEncounterText = "45 days ago"
                ),
                ContactSummary(
                    id = "c-9",
                    name = "Sophia Martinez",
                    contextSubtitle = "UX Researcher • Working on accessibility tooling",
                    circles = listOf(Circle.WORK),
                    avatarInitials = "SM",
                    avatarBgIndex = 2,
                    lastEncounterText = "Yesterday"
                ),
                ContactSummary(
                    id = "c-10",
                    name = "Zoe Washington",
                    contextSubtitle = "Ceramics artist & Studio host",
                    circles = listOf(Circle.FRIENDS),
                    avatarInitials = "ZW",
                    avatarBgIndex = 3,
                    lastEncounterText = "3 weeks ago"
                )
            )
        }
    }
}
