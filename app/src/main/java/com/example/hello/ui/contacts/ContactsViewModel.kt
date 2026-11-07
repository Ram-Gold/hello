package com.example.hello.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.repository.ContactRepository
import com.example.hello.data.repository.ContactRepository.Companion.toSummary
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.mock.MockDataProvider
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
                selectedTag = current.selectedTag,
                sortMode = current.sortMode,
                contacts = allContacts,
                selectedContactIds = current.selectedContactIds,
                isDeleteConfirmationVisible = current.isDeleteConfirmationVisible
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
                        selectedTag = current.selectedTag,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible
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
                                    selectedTag = current.selectedTag,
                                    sortMode = current.sortMode,
                                    contacts = matchingSummaries,
                                    selectedContactIds = current.selectedContactIds,
                                    isDeleteConfirmationVisible = current.isDeleteConfirmationVisible
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
                        selectedTag = current.selectedTag,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible
                    )
                }
            }
            is ContactsUiEvent.OnSelectTag -> {
                val normalizedTag = event.tagName?.removePrefix("#")?.trim()?.lowercase()?.ifBlank { null }
                _uiState.update { current ->
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedTag = normalizedTag,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible
                    )
                }
            }
            is ContactsUiEvent.OnToggleSortMode -> {
                _uiState.update { current ->
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedTag = current.selectedTag,
                        sortMode = event.mode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible
                    )
                }
            }
            is ContactsUiEvent.OnContactClick -> {
                _uiState.update { it.copy(userMessage = "Opened profile: ${event.contactId}") }
            }
            is ContactsUiEvent.OnContactLongClick -> {
                _uiState.update { current ->
                    current.copy(selectedContactIds = current.selectedContactIds + event.contactId)
                }
            }
            is ContactsUiEvent.OnToggleSelectContact -> {
                _uiState.update { current ->
                    val updated = if (event.contactId in current.selectedContactIds) {
                        current.selectedContactIds - event.contactId
                    } else {
                        current.selectedContactIds + event.contactId
                    }
                    current.copy(selectedContactIds = updated)
                }
            }
            is ContactsUiEvent.OnClearSelection -> {
                _uiState.update { current ->
                    current.copy(selectedContactIds = emptySet())
                }
            }
            is ContactsUiEvent.OnSelectAllVisibleContacts -> {
                _uiState.update { current ->
                    val allIds = current.contacts.map { it.id }.toSet()
                    current.copy(selectedContactIds = allIds)
                }
            }
            is ContactsUiEvent.OnRequestDeleteSelected -> {
                _uiState.update { it.copy(isDeleteConfirmationVisible = true) }
            }
            is ContactsUiEvent.OnDismissDeleteDialog -> {
                _uiState.update { it.copy(isDeleteConfirmationVisible = false) }
            }
            is ContactsUiEvent.OnConfirmDeleteSelected -> {
                val toDelete = _uiState.value.selectedContactIds
                if (repository != null) {
                    toDelete.forEach { contactId ->
                        repository.deleteContact(contactId)
                    }
                }
                allContacts = allContacts.filterNot { it.id in toDelete }
                val deleteCount = toDelete.size
                val message = "Deleted $deleteCount contact${if (deleteCount > 1) "s" else ""}"
                _uiState.update { current ->
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedTag = current.selectedTag,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = emptySet(),
                        isDeleteConfirmationVisible = false
                    ).copy(userMessage = message)
                }
            }
            is ContactsUiEvent.OnClearFilters -> {
                _uiState.update { current ->
                    computeState(
                        searchQuery = "",
                        selectedTag = null,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible
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
            sortMode: ContactSortMode,
            contacts: List<ContactSummary>,
            selectedTag: String? = null,
            selectedContactIds: Set<String> = emptySet(),
            isDeleteConfirmationVisible: Boolean = false
        ): ContactsUiState {
            val cleanQuery = searchQuery.trim()
            val cleanTag = selectedTag?.removePrefix("#")?.trim()?.lowercase()?.ifBlank { null }

            val filtered = contacts.filter { contact ->
                val matchesQuery = if (cleanQuery.isBlank()) {
                    true
                } else if (cleanQuery.startsWith("#")) {
                    val hashTag = cleanQuery.removePrefix("#")
                    contact.tags.any { it.name.contains(hashTag, ignoreCase = true) } ||
                        contact.contextSubtitle.contains(cleanQuery, ignoreCase = true)
                } else {
                    contact.name.contains(cleanQuery, ignoreCase = true) ||
                        contact.contextSubtitle.contains(cleanQuery, ignoreCase = true) ||
                        contact.notes.contains(cleanQuery, ignoreCase = true) ||
                        contact.tags.any { it.name.contains(cleanQuery, ignoreCase = true) }
                }

                val matchesTag = if (cleanTag == null) {
                    true
                } else {
                    contact.tags.any { it.name.equals(cleanTag, ignoreCase = true) } ||
                        contact.contextSubtitle.contains("#$cleanTag", ignoreCase = true)
                }

                matchesQuery && matchesTag
            }

            val dynamicTags = buildList<String> {
                addAll(ContactsUiState.DEFAULT_SMART_FILTER_TAGS)
                contacts.flatMap { it.tags }.forEach { tag ->
                    val lower = tag.name.lowercase()
                    if (lower !in this) add(lower)
                }
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

            val validContactIds = filtered.map { it.id }.toSet()
            val prunedSelectedIds = selectedContactIds.filter { it in validContactIds }.toSet()

            return ContactsUiState(
                searchQuery = searchQuery,
                selectedTag = cleanTag,
                smartFilterTags = dynamicTags,
                sortMode = sortMode,
                contacts = filtered,
                groupedContacts = grouped,
                totalCount = filtered.size,
                selectedContactIds = prunedSelectedIds,
                isDeleteConfirmationVisible = isDeleteConfirmationVisible
            )
        }

        fun createSampleContacts(): List<ContactSummary> {
            return listOf(
                ContactSummary(
                    id = "c-1",
                    name = "Alex Rivera",
                    contextSubtitle = "Product Designer & Tinkerer",
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
