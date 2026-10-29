package com.example.hello.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.repository.ContactRepository
import com.example.hello.data.repository.ContactRepository.Companion.toSummary
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSortMode
import com.example.hello.domain.model.ContactSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactsViewModel(
    val repository: ContactRepository? = null
) : ViewModel() {

    private var allContacts: List<ContactSummary> = createSampleContacts()
    private var searchJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(
        computeState(
            searchQuery = "",
            selectedCircle = null,
            sortMode = ContactSortMode.ALPHABETICAL,
            contacts = allContacts
        )
    )
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    init {
        if (repository != null) {
            viewModelScope.launch {
                repository.contacts.collect { contacts ->
                    syncContacts(contacts)
                }
            }
        }
    }

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
                val query = event.query
                _uiState.update { current ->
                    computeState(
                        searchQuery = query,
                        selectedCircle = current.selectedCircle,
                        sortMode = current.sortMode,
                        contacts = allContacts
                    )
                }
                if (repository != null && query.isNotBlank()) {
                    searchJob?.cancel()
                    searchJob = viewModelScope.launch {
                        repository.searchContacts(query).collect { matchingContacts ->
                            val matchingSummaries = matchingContacts.map { it.toEntity().toSummary() }
                            _uiState.update { current ->
                                computeState(
                                    searchQuery = query,
                                    selectedCircle = current.selectedCircle,
                                    sortMode = current.sortMode,
                                    contacts = matchingSummaries
                                )
                            }
                        }
                    }
                }
            }
            is ContactsUiEvent.OnClearSearch -> {
                searchJob?.cancel()
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
                            contact.contextSubtitle.contains(searchQuery, ignoreCase = true) ||
                            contact.notes.contains(searchQuery, ignoreCase = true) ||
                            contact.tags.any { it.name.contains(searchQuery, ignoreCase = true) }
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
                    lastEncounterText = "Met today",
                    notes = "Working on design system tokens and micro-interactions. Loves pour-over Ethiopian roast.",
                    tags = listOf(MockDataProvider.tagDesign, MockDataProvider.tagTech)
                ),
                ContactSummary(
                    id = "c-2",
                    name = "David Kim",
                    contextSubtitle = "Mobile Dev • Discussed Compose gestures",
                    circles = listOf(Circle.WORK, Circle.UNI),
                    avatarInitials = "DK",
                    avatarBgIndex = 1,
                    lastEncounterText = "10 days ago",
                    notes = "Discussed Compose gestures and physics-based spring transitions.",
                    tags = listOf(MockDataProvider.tagTech)
                ),
                ContactSummary(
                    id = "c-3",
                    name = "Elena Rostova",
                    contextSubtitle = "Neighbor & Botanist • Exchanged plants",
                    circles = listOf(Circle.FRIENDS, Circle.FAMILY),
                    avatarInitials = "ER",
                    avatarBgIndex = 2,
                    lastEncounterText = "Last Sunday",
                    notes = "Exchanged monstera cuttings and discussed balcony gardening setup.",
                    tags = listOf(MockDataProvider.tagCoffee)
                ),
                ContactSummary(
                    id = "c-4",
                    name = "Liam Chen",
                    contextSubtitle = "Architect • Coffee and urban photography",
                    circles = listOf(Circle.FRIENDS),
                    avatarInitials = "LC",
                    avatarBgIndex = 3,
                    lastEncounterText = "2 weeks ago",
                    notes = "Architect exploring urban geometry and brutalist structures.",
                    tags = listOf(MockDataProvider.tagDesign, MockDataProvider.tagCoffee)
                ),
                ContactSummary(
                    id = "c-5",
                    name = "Marcus Vance",
                    contextSubtitle = "Founder @ Studio Arc • Met at Coffee Lab",
                    circles = listOf(Circle.WORK),
                    avatarInitials = "MV",
                    avatarBgIndex = 4,
                    lastEncounterText = "4 days ago",
                    notes = "Founder at Studio Arc. Brainstormed local-first privacy models.",
                    tags = listOf(MockDataProvider.tagTech, MockDataProvider.tagAi)
                ),
                ContactSummary(
                    id = "c-6",
                    name = "Maya Patel",
                    contextSubtitle = "CS '27 • Met yesterday at Hackathon",
                    circles = listOf(Circle.UNI, Circle.FRIENDS),
                    avatarInitials = "MP",
                    avatarBgIndex = 5,
                    lastEncounterText = "Met yesterday",
                    notes = "Hackathon project on on-device vector search without cloud sync.",
                    tags = listOf(MockDataProvider.tagAi, MockDataProvider.tagTech)
                ),
                ContactSummary(
                    id = "c-7",
                    name = "Noah Albright",
                    contextSubtitle = "High School Friend • Catch up quarterly",
                    circles = listOf(Circle.FRIENDS),
                    avatarInitials = "NA",
                    avatarBgIndex = 0,
                    lastEncounterText = "1 month ago",
                    notes = "High School Friend catching up quarterly over dinner.",
                    tags = emptyList()
                ),
                ContactSummary(
                    id = "c-8",
                    name = "Sarah Lin",
                    contextSubtitle = "Biotech Researcher • Tea tasting buddy",
                    circles = listOf(Circle.FAMILY, Circle.FRIENDS),
                    avatarInitials = "SL",
                    avatarBgIndex = 1,
                    lastEncounterText = "45 days ago",
                    notes = "Biotech Researcher and tea tasting buddy. Exploring fermented oolong varieties.",
                    tags = listOf(MockDataProvider.tagCoffee)
                ),
                ContactSummary(
                    id = "c-9",
                    name = "Sophia Martinez",
                    contextSubtitle = "UX Researcher • Working on accessibility tooling",
                    circles = listOf(Circle.WORK),
                    avatarInitials = "SM",
                    avatarBgIndex = 2,
                    lastEncounterText = "Yesterday",
                    notes = "UX Researcher working on tactile accessibility tooling.",
                    tags = listOf(MockDataProvider.tagDesign)
                ),
                ContactSummary(
                    id = "c-10",
                    name = "Zoe Washington",
                    contextSubtitle = "Ceramics artist & Studio host",
                    circles = listOf(Circle.FRIENDS),
                    avatarInitials = "ZW",
                    avatarBgIndex = 3,
                    lastEncounterText = "3 weeks ago",
                    notes = "Ceramics artist and studio host. Firing stoneware pottery.",
                    tags = listOf(MockDataProvider.tagCoffee, MockDataProvider.tagDesign)
                )
            )
        }
    }
}
