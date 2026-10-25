package com.example.hello.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonEditViewModel(
    val contactId: String,
    val repository: ContactRepository
) : ViewModel() {

    private var initialContact: Contact? = null

    private val _uiState = MutableStateFlow(PersonEditUiState(contactId = contactId))
    val uiState: StateFlow<PersonEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val contact = repository.getContactFlow(contactId).firstOrNull() ?: repository.getContact(contactId)
            initialContact = contact
            val tags = repository.tags.value

            if (contact != null) {
                val isCustom = contact.cadenceDays != null && contact.cadenceDays !in listOf(7, 14, 30, 90)
                _uiState.update { current ->
                    current.copy(
                        name = contact.name,
                        aliases = contact.aliases,
                        phone = contact.phone.orEmpty(),
                        email = contact.email.orEmpty(),
                        notes = contact.notes,
                        photoUri = contact.photoUri,
                        selectedCircles = contact.circles.toSet(),
                        selectedTagIds = contact.tagIds.toSet(),
                        availableTags = tags,
                        cadenceDays = contact.cadenceDays,
                        isCustomCadence = isCustom,
                        customCadenceInput = if (isCustom) contact.cadenceDays.toString() else "",
                        isModified = false,
                        canSave = contact.name.isNotBlank()
                    )
                }
            } else {
                _uiState.update { it.copy(availableTags = tags) }
            }
        }
    }

    fun onEvent(event: PersonEditUiEvent) {
        when (event) {
            is PersonEditUiEvent.OnNameChange -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(name = event.name))
                    current.copy(
                        name = event.name,
                        isModified = isMod,
                        canSave = event.name.isNotBlank()
                    )
                }
            }
            is PersonEditUiEvent.OnAliasInputChange -> {
                _uiState.update { it.copy(aliasInput = event.text) }
            }
            is PersonEditUiEvent.OnAddAlias -> {
                val clean = event.alias.trim()
                if (clean.isNotEmpty() && clean !in _uiState.value.aliases) {
                    _uiState.update { current ->
                        val updatedAliases = current.aliases + clean
                        val isMod = checkModified(current.copy(aliases = updatedAliases))
                        current.copy(
                            aliases = updatedAliases,
                            aliasInput = "",
                            isModified = isMod
                        )
                    }
                }
            }
            is PersonEditUiEvent.OnRemoveAlias -> {
                _uiState.update { current ->
                    val updatedAliases = current.aliases - event.alias
                    val isMod = checkModified(current.copy(aliases = updatedAliases))
                    current.copy(
                        aliases = updatedAliases,
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnPhoneChange -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(phone = event.phone))
                    current.copy(
                        phone = event.phone,
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnEmailChange -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(email = event.email))
                    current.copy(
                        email = event.email,
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnNotesChange -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(notes = event.notes))
                    current.copy(
                        notes = event.notes,
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnPhotoSelected -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(photoUri = event.uri))
                    current.copy(
                        photoUri = event.uri,
                        isModified = isMod
                    )
                }
            }
            PersonEditUiEvent.OnRemovePhoto -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(photoUri = null))
                    current.copy(
                        photoUri = null,
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnToggleCircle -> {
                _uiState.update { current ->
                    val updated = if (event.circle in current.selectedCircles) {
                        current.selectedCircles - event.circle
                    } else {
                        current.selectedCircles + event.circle
                    }
                    val isMod = checkModified(current.copy(selectedCircles = updated))
                    current.copy(
                        selectedCircles = updated,
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnToggleTag -> {
                _uiState.update { current ->
                    val updated = if (event.tagId in current.selectedTagIds) {
                        current.selectedTagIds - event.tagId
                    } else {
                        current.selectedTagIds + event.tagId
                    }
                    val isMod = checkModified(current.copy(selectedTagIds = updated))
                    current.copy(
                        selectedTagIds = updated,
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnCreateTag -> {
                val created = repository.addTag(event.name)
                _uiState.update { current ->
                    val updatedTags = repository.tags.value
                    val updatedSelection = current.selectedTagIds + created.id
                    val isMod = checkModified(current.copy(selectedTagIds = updatedSelection))
                    current.copy(
                        availableTags = updatedTags,
                        selectedTagIds = updatedSelection,
                        showNewTagDialog = false,
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnSelectCadence -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(cadenceDays = event.days))
                    current.copy(
                        cadenceDays = event.days,
                        isCustomCadence = false,
                        customCadenceInput = "",
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnCustomCadenceChange -> {
                val parsed = event.daysString.filter { it.isDigit() }.toIntOrNull()
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(cadenceDays = parsed))
                    current.copy(
                        customCadenceInput = event.daysString,
                        isCustomCadence = true,
                        cadenceDays = parsed,
                        isModified = isMod
                    )
                }
            }
            PersonEditUiEvent.OnSaveClick -> {
                saveChanges()
            }
            PersonEditUiEvent.OnBackClick -> {
                if (_uiState.value.isModified) {
                    _uiState.update { it.copy(showDiscardDialog = true) }
                }
            }
            PersonEditUiEvent.OnConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }
            PersonEditUiEvent.OnDismissDiscardDialog -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }
            PersonEditUiEvent.OnShowArchiveDialog -> {
                _uiState.update { it.copy(showArchiveDialog = true) }
            }
            PersonEditUiEvent.OnDismissArchiveDialog -> {
                _uiState.update { it.copy(showArchiveDialog = false) }
            }
            PersonEditUiEvent.OnConfirmArchive -> {
                _uiState.update { it.copy(showArchiveDialog = false) }
                repository.archiveContact(contactId, archived = true)
                _uiState.update { it.copy(isArchivedCompleted = true, userMessage = "Contact archived") }
            }
            PersonEditUiEvent.OnShowDeleteDialog -> {
                _uiState.update { it.copy(showDeleteDialog = true) }
            }
            PersonEditUiEvent.OnDismissDeleteDialog -> {
                _uiState.update { it.copy(showDeleteDialog = false) }
            }
            PersonEditUiEvent.OnConfirmDelete -> {
                _uiState.update { it.copy(showDeleteDialog = false) }
                repository.deleteContact(contactId)
                _uiState.update { it.copy(isDeletedCompleted = true, userMessage = "Contact deleted") }
            }
            is PersonEditUiEvent.OnShowNewTagDialog -> {
                _uiState.update { it.copy(showNewTagDialog = event.show) }
            }
            PersonEditUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
        }
    }

    private fun checkModified(current: PersonEditUiState): Boolean {
        val base = initialContact ?: return true
        val sameName = current.name.trim() == base.name.trim()
        val sameAliases = current.aliases == base.aliases
        val samePhone = current.phone.trim() == (base.phone ?: "").trim()
        val sameEmail = current.email.trim() == (base.email ?: "").trim()
        val sameNotes = current.notes.trim() == base.notes.trim()
        val samePhoto = current.photoUri == base.photoUri
        val sameCircles = current.selectedCircles == base.circles.toSet()
        val sameTags = current.selectedTagIds == base.tagIds.toSet()
        val sameCadence = current.cadenceDays == base.cadenceDays

        return !(sameName && sameAliases && samePhone && sameEmail && sameNotes && samePhoto && sameCircles && sameTags && sameCadence)
    }

    private fun saveChanges() {
        val current = _uiState.value
        val cleanName = current.name.trim()
        if (cleanName.isBlank()) return

        val base = initialContact
        val updated = Contact(
            id = contactId,
            name = cleanName,
            aliases = current.aliases,
            phone = current.phone.trim().ifEmpty { null },
            email = current.email.trim().ifEmpty { null },
            circles = current.selectedCircles.toList(),
            tagIds = current.selectedTagIds.toList(),
            notes = current.notes.trim(),
            photoUri = current.photoUri,
            firstMetAt = base?.firstMetAt,
            lastMetAt = base?.lastMetAt,
            meetCount = base?.meetCount ?: 0,
            cadenceDays = current.cadenceDays,
            isArchived = base?.isArchived ?: false
        )

        repository.updateContact(updated)
        initialContact = updated
        _uiState.update {
            it.copy(
                isModified = false,
                isSaved = true,
                userMessage = "Contact updated"
            )
        }
    }
}
