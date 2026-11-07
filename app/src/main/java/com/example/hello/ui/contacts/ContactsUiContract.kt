package com.example.hello.ui.contacts

import com.example.hello.domain.model.ContactSortMode
import com.example.hello.domain.model.ContactSummary

data class ContactsUiState(
    val searchQuery: String = "",
    val selectedTag: String? = null,
    val smartFilterTags: List<String> = DEFAULT_SMART_FILTER_TAGS,
    val sortMode: ContactSortMode = ContactSortMode.ALPHABETICAL,
    val contacts: List<ContactSummary> = emptyList(),
    val groupedContacts: Map<String, List<ContactSummary>> = emptyMap(),
    val totalCount: Int = 0,
    val userMessage: String? = null,
    val selectedContactIds: Set<String> = emptySet(),
    val isDeleteConfirmationVisible: Boolean = false
) {
    val isSelectionMode: Boolean get() = selectedContactIds.isNotEmpty()

    companion object {
        val DEFAULT_SMART_FILTER_TAGS = listOf(
            "friends",
            "work",
            "family",
            "uni",
            "design",
            "tech",
            "coffee",
            "ai"
        )
    }
}

sealed interface ContactsUiEvent {
    data class OnSearchQueryChange(val query: String) : ContactsUiEvent
    data object OnClearSearch : ContactsUiEvent
    data class OnSelectTag(val tagName: String?) : ContactsUiEvent
    data class OnToggleSortMode(val mode: ContactSortMode) : ContactsUiEvent
    data class OnContactClick(val contactId: String) : ContactsUiEvent
    data class OnContactLongClick(val contactId: String) : ContactsUiEvent
    data class OnToggleSelectContact(val contactId: String) : ContactsUiEvent
    data object OnSelectAllVisibleContacts : ContactsUiEvent
    data object OnRequestDeleteSelected : ContactsUiEvent
    data object OnConfirmDeleteSelected : ContactsUiEvent
    data object OnDismissDeleteDialog : ContactsUiEvent
    data object OnClearSelection : ContactsUiEvent
    data object OnClearFilters : ContactsUiEvent
    data object OnDismissUserMessage : ContactsUiEvent
}
