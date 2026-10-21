package com.example.hello.ui.contacts

import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.ContactSortMode
import com.example.hello.domain.model.ContactSummary

data class ContactsUiState(
    val searchQuery: String = "",
    val selectedCircle: Circle? = null,
    val sortMode: ContactSortMode = ContactSortMode.ALPHABETICAL,
    val contacts: List<ContactSummary> = emptyList(),
    val groupedContacts: Map<String, List<ContactSummary>> = emptyMap(),
    val totalCount: Int = 0,
    val userMessage: String? = null
)

sealed interface ContactsUiEvent {
    data class OnSearchQueryChange(val query: String) : ContactsUiEvent
    data object OnClearSearch : ContactsUiEvent
    data class OnSelectCircle(val circle: Circle?) : ContactsUiEvent
    data class OnToggleSortMode(val mode: ContactSortMode) : ContactsUiEvent
    data class OnContactClick(val contactId: String) : ContactsUiEvent
    data object OnClearFilters : ContactsUiEvent
    data object OnDismissUserMessage : ContactsUiEvent
}
