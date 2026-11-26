package com.example.hello.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.repository.ContactRepository
import com.example.hello.data.repository.ContactRepository.Companion.toSummary
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.mock.MockDataProvider
import com.example.hello.domain.model.ContactSortMode
import com.example.hello.domain.model.ContactSummary
import com.example.hello.domain.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactsViewModel(
    val repository: ContactRepository? = null
) : ViewModel() {

    private var allContacts: List<ContactSummary> = createSampleContacts()
    private var availableTags: List<Tag> = MockDataProvider.tags
    private var favoriteContacts: List<ContactSummary> = emptyList()
    private var searchJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(
        computeState(
            searchQuery = "",
            sortMode = ContactSortMode.ALPHABETICAL,
            contacts = allContacts,
            availableTags = availableTags,
            favoriteContacts = favoriteContacts
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
            viewModelScope.launch {
                repository.tags.collect { tags ->
                    syncTags(tags)
                }
            }
            viewModelScope.launch {
                repository.favoriteContacts.collect { favorites ->
                    syncFavorites(favorites)
                }
            }
        }
    }

    fun syncFavorites(favorites: List<ContactSummary>) {
        this.favoriteContacts = favorites
        _uiState.update { current ->
            current.copy(favoriteContacts = favorites)
        }
    }

    fun syncContacts(updatedContacts: List<ContactSummary>) {
        allContacts = updatedContacts
        _uiState.update { current ->
            computeState(
                searchQuery = current.searchQuery,
                selectedTags = current.selectedTags,
                sortMode = current.sortMode,
                contacts = allContacts,
                selectedContactIds = current.selectedContactIds,
                isDeleteConfirmationVisible = current.isDeleteConfirmationVisible,
                isArchiveConfirmationVisible = current.isArchiveConfirmationVisible,
                availableTags = availableTags,
                favoriteContacts = favoriteContacts,
                isArchivedSheetOpen = current.isArchivedSheetOpen,
                userMessage = current.userMessage
            )
        }
    }

    fun syncTags(tags: List<Tag>) {
        availableTags = tags
        _uiState.update { current ->
            computeState(
                searchQuery = current.searchQuery,
                selectedTags = current.selectedTags,
                sortMode = current.sortMode,
                contacts = allContacts,
                selectedContactIds = current.selectedContactIds,
                isDeleteConfirmationVisible = current.isDeleteConfirmationVisible,
                isArchiveConfirmationVisible = current.isArchiveConfirmationVisible,
                availableTags = availableTags,
                favoriteContacts = favoriteContacts,
                isArchivedSheetOpen = current.isArchivedSheetOpen,
                userMessage = current.userMessage
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
                        selectedTags = current.selectedTags,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible,
                        isArchiveConfirmationVisible = current.isArchiveConfirmationVisible,
                        availableTags = availableTags,
                        favoriteContacts = favoriteContacts,
                        isArchivedSheetOpen = current.isArchivedSheetOpen,
                        userMessage = current.userMessage
                    )
                }
                if (repository != null && query.isNotBlank()) {
                    searchJob?.cancel()
                    searchJob = viewModelScope.launch {
                        repository.searchContacts(query).collect { matchingContacts ->
                            val matchingSummaries = matchingContacts.map { it.toSummary(availableTags) }
                            _uiState.update { current ->
                                computeState(
                                    searchQuery = query,
                                    selectedTags = current.selectedTags,
                                    sortMode = current.sortMode,
                                    contacts = matchingSummaries,
                                    selectedContactIds = current.selectedContactIds,
                                    isDeleteConfirmationVisible = current.isDeleteConfirmationVisible,
                                    isArchiveConfirmationVisible = current.isArchiveConfirmationVisible,
                                    availableTags = availableTags,
                                    favoriteContacts = favoriteContacts,
                                    isArchivedSheetOpen = current.isArchivedSheetOpen,
                                    userMessage = current.userMessage
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
                        selectedTags = current.selectedTags,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible,
                        isArchiveConfirmationVisible = current.isArchiveConfirmationVisible,
                        availableTags = availableTags,
                        favoriteContacts = favoriteContacts,
                        isArchivedSheetOpen = current.isArchivedSheetOpen,
                        userMessage = current.userMessage
                    )
                }
            }
            is ContactsUiEvent.OnSelectTag -> {
                val normalizedTag = event.tagName?.removePrefix("#")?.trim()?.lowercase()?.ifBlank { null }
                _uiState.update { current ->
                    val updatedTags = if (normalizedTag == null) {
                        emptySet()
                    } else if (normalizedTag in current.selectedTags) {
                        current.selectedTags - normalizedTag
                    } else {
                        current.selectedTags + normalizedTag
                    }
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedTags = updatedTags,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible,
                        isArchiveConfirmationVisible = current.isArchiveConfirmationVisible,
                        availableTags = availableTags,
                        favoriteContacts = favoriteContacts,
                        isArchivedSheetOpen = current.isArchivedSheetOpen,
                        userMessage = current.userMessage
                    )
                }
            }
            is ContactsUiEvent.OnToggleSortMode -> {
                _uiState.update { current ->
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedTags = current.selectedTags,
                        sortMode = event.mode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible,
                        isArchiveConfirmationVisible = current.isArchiveConfirmationVisible,
                        availableTags = availableTags,
                        favoriteContacts = favoriteContacts,
                        isArchivedSheetOpen = current.isArchivedSheetOpen,
                        userMessage = current.userMessage
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
            is ContactsUiEvent.OnClearSelection,
            is ContactsUiEvent.OnTabDeparture -> {
                _uiState.update { current ->
                    current.copy(
                        selectedContactIds = emptySet(),
                        isDeleteConfirmationVisible = false,
                        isArchiveConfirmationVisible = false
                    )
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
            is ContactsUiEvent.OnRequestArchiveSelected -> {
                _uiState.update { it.copy(isArchiveConfirmationVisible = true) }
            }
            is ContactsUiEvent.OnDismissArchiveDialog -> {
                _uiState.update { it.copy(isArchiveConfirmationVisible = false) }
            }
            is ContactsUiEvent.OnConfirmArchiveSelected -> {
                val toArchive = _uiState.value.selectedContactIds
                if (repository != null) {
                    repository.archiveContacts(toArchive, archived = true)
                }
                allContacts = allContacts.map { c ->
                    if (c.id in toArchive) c.copy(isArchived = true) else c
                }
                val archiveCount = toArchive.size
                val message = "Archived $archiveCount contact${if (archiveCount > 1) "s" else ""}"
                _uiState.update { current ->
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedTags = current.selectedTags,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = emptySet(),
                        isDeleteConfirmationVisible = false,
                        isArchiveConfirmationVisible = false,
                        availableTags = availableTags,
                        favoriteContacts = favoriteContacts,
                        isArchivedSheetOpen = current.isArchivedSheetOpen,
                        userMessage = message
                    )
                }
            }
            is ContactsUiEvent.OnConfirmDeleteSelected -> {
                val toDelete = _uiState.value.selectedContactIds
                if (repository != null) {
                    repository.deleteContacts(toDelete)
                }
                allContacts = allContacts.filterNot { it.id in toDelete }
                val deleteCount = toDelete.size
                val message = "Deleted $deleteCount contact${if (deleteCount > 1) "s" else ""}"
                _uiState.update { current ->
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedTags = current.selectedTags,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = emptySet(),
                        isDeleteConfirmationVisible = false,
                        availableTags = availableTags,
                        favoriteContacts = favoriteContacts,
                        isArchivedSheetOpen = current.isArchivedSheetOpen,
                        userMessage = message
                    )
                }
            }
            is ContactsUiEvent.OnClearFilters -> {
                _uiState.update { current ->
                    computeState(
                        searchQuery = "",
                        selectedTags = emptySet(),
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible,
                        isArchiveConfirmationVisible = current.isArchiveConfirmationVisible,
                        availableTags = availableTags,
                        favoriteContacts = favoriteContacts,
                        isArchivedSheetOpen = current.isArchivedSheetOpen,
                        userMessage = current.userMessage
                    )
                }
            }
            is ContactsUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
            is ContactsUiEvent.OnOpenArchivedSheet -> {
                _uiState.update { it.copy(isArchivedSheetOpen = true) }
            }
            is ContactsUiEvent.OnDismissArchivedSheet -> {
                _uiState.update { it.copy(isArchivedSheetOpen = false) }
            }
            is ContactsUiEvent.OnRestoreContact -> {
                repository?.archiveContact(event.contactId, archived = false)
                allContacts = allContacts.map { c ->
                    if (c.id == event.contactId) c.copy(isArchived = false) else c
                }
                _uiState.update { current ->
                    computeState(
                        searchQuery = current.searchQuery,
                        selectedTags = current.selectedTags,
                        sortMode = current.sortMode,
                        contacts = allContacts,
                        selectedContactIds = current.selectedContactIds,
                        isDeleteConfirmationVisible = current.isDeleteConfirmationVisible,
                        availableTags = availableTags,
                        favoriteContacts = favoriteContacts,
                        isArchivedSheetOpen = current.isArchivedSheetOpen
                    ).copy(userMessage = "Contact restored")
                }
            }
        }
    }

    companion object {
        fun computeState(
            searchQuery: String,
            sortMode: ContactSortMode,
            contacts: List<ContactSummary>,
            selectedTag: String? = null,
            selectedTags: Set<String> = if (selectedTag != null) setOf(selectedTag) else emptySet(),
            selectedContactIds: Set<String> = emptySet(),
            isDeleteConfirmationVisible: Boolean = false,
            isArchiveConfirmationVisible: Boolean = false,
            availableTags: List<Tag> = emptyList(),
            favoriteContacts: List<ContactSummary> = emptyList(),
            isArchivedSheetOpen: Boolean = false,
            userMessage: String? = null
        ): ContactsUiState {
            val cleanQuery = searchQuery.trim()
            val cleanSelectedTags = selectedTags
                .mapNotNull { it.removePrefix("#").trim().lowercase().ifBlank { null } }
                .toSet()

            val (archivedList, activeList) = contacts.partition { it.isArchived }

            val filtered = activeList.filter { contact ->
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

                val matchesTag = if (cleanSelectedTags.isEmpty()) {
                    true
                } else {
                    cleanSelectedTags.any { cleanTag ->
                        contact.tags.any { it.name.equals(cleanTag, ignoreCase = true) } ||
                            contact.contextSubtitle.contains(cleanTag, ignoreCase = true)
                    }
                }

                matchesQuery && matchesTag
            }

            val sortedArchived = archivedList.sortedBy { it.name }

            val dynamicTags = buildList<String> {
                addAll(ContactsUiState.DEFAULT_SMART_FILTER_TAGS)
                availableTags.forEach { tag ->
                    val lower = tag.name.lowercase()
                    if (lower !in this) add(lower)
                }
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
                selectedTags = cleanSelectedTags,
                selectedTag = cleanSelectedTags.firstOrNull(),
                smartFilterTags = dynamicTags,
                availableTags = availableTags,
                sortMode = sortMode,
                contacts = filtered,
                groupedContacts = grouped,
                totalCount = filtered.size,
                selectedContactIds = prunedSelectedIds,
                isDeleteConfirmationVisible = isDeleteConfirmationVisible,
                isArchiveConfirmationVisible = isArchiveConfirmationVisible,
                favoriteContacts = favoriteContacts,
                archivedContacts = sortedArchived,
                isArchivedSheetOpen = isArchivedSheetOpen,
                userMessage = userMessage
            )
        }

        fun createSampleContacts(): List<ContactSummary> =
            com.example.hello.domain.mock.SeedDataFactory.createSampleContactSummaries()
    }
}
