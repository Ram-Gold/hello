package com.example.hello.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.Contact
import com.example.hello.platform.avatar.AvatarStorageManager
import com.example.hello.ui.components.PhoneInputUtils
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonEditViewModel(
    val contactId: String,
    val repository: ContactRepository,
    val avatarStorage: AvatarStorageManager? = null
) : ViewModel() {

    private var initialContact: Contact? = repository.getContact(contactId)

    private val _uiState: MutableStateFlow<PersonEditUiState>
    val uiState: StateFlow<PersonEditUiState>

    init {
        val contact = initialContact
        val tags = repository.tags.value
        val initialState = if (contact != null) {
            PersonEditUiState(
                contactId = contactId,
                name = contact.name,
                aliases = contact.aliases,
                role = contact.role.orEmpty(),
                company = contact.company.orEmpty(),
                birthday = contact.birthday,
                phone = contact.phone.orEmpty(),
                email = contact.email.orEmpty(),
                notes = contact.notes,
                photoUri = contact.photoUri,
                notionistConfig = contact.notionistConfig,
                selectedTagIds = contact.tagIds.toSet(),
                availableTags = tags,
                customProperties = contact.customProperties,
                isModified = false,
                canSave = contact.name.isNotBlank()
            )
        } else {
            PersonEditUiState(
                contactId = contactId,
                availableTags = tags
            )
        }
        _uiState = MutableStateFlow(initialState)
        uiState = _uiState.asStateFlow()

        viewModelScope.launch {
            if (initialContact == null) {
                val fetchedContact = repository.getContactFlow(contactId).firstOrNull() ?: repository.getContact(contactId)
                if (fetchedContact != null) {
                    initialContact = fetchedContact
                    _uiState.update { current ->
                        current.copy(
                            name = fetchedContact.name,
                            aliases = fetchedContact.aliases,
                            role = fetchedContact.role.orEmpty(),
                            company = fetchedContact.company.orEmpty(),
                            birthday = fetchedContact.birthday,
                            phone = fetchedContact.phone.orEmpty(),
                            email = fetchedContact.email.orEmpty(),
                            notes = fetchedContact.notes,
                            photoUri = fetchedContact.photoUri,
                            notionistConfig = fetchedContact.notionistConfig,
                            selectedTagIds = fetchedContact.tagIds.toSet(),
                            availableTags = repository.tags.value,
                            customProperties = fetchedContact.customProperties,
                            isModified = false,
                            canSave = fetchedContact.name.isNotBlank()
                        )
                    }
                }
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
                if (clean.isNotEmpty()) {
                    _uiState.update { current ->
                        val updatedAliases = if (clean !in current.aliases) current.aliases + clean else current.aliases
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
            is PersonEditUiEvent.OnRoleChange -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(role = event.role))
                    current.copy(role = event.role, isModified = isMod)
                }
            }
            is PersonEditUiEvent.OnCompanyChange -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(company = event.company))
                    current.copy(company = event.company, isModified = isMod)
                }
            }
            is PersonEditUiEvent.OnBirthdayChange -> {
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(birthday = event.birthday))
                    current.copy(birthday = event.birthday, isModified = isMod)
                }
            }
            is PersonEditUiEvent.OnPhoneChange -> {
                val sanitized = PhoneInputUtils.sanitizeInput(event.phone)
                _uiState.update { current ->
                    val isMod = checkModified(current.copy(phone = sanitized))
                    current.copy(
                        phone = sanitized,
                        phoneError = null,
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
                if (!event.uri.isNullOrBlank()) {
                    _uiState.update { current ->
                        current.copy(pendingCropUri = event.uri)
                    }
                }
            }
            is PersonEditUiEvent.OnPhotoCropped -> {
                _uiState.update { current ->
                    val stagedPath = avatarStorage?.writeStagedBytes(
                        bytes = event.croppedBytes,
                        previousStagedPath = current.photoUri
                    ) ?: current.pendingCropUri
                    val isMod = checkModified(
                        current.copy(photoUri = stagedPath, notionistConfig = null)
                    )
                    current.copy(
                        photoUri = stagedPath,
                        notionistConfig = null,
                        pendingCropUri = null,
                        isModified = isMod
                    )
                }
            }
            is PersonEditUiEvent.OnNotionistAvatarApplied -> {
                _uiState.update { current ->
                    val stagedPath = avatarStorage?.writeStagedBytes(
                        bytes = event.pngBytes,
                        previousStagedPath = current.photoUri
                    ) ?: current.photoUri
                    val isMod = checkModified(
                        current.copy(photoUri = stagedPath, notionistConfig = event.configJson)
                    )
                    current.copy(
                        photoUri = stagedPath,
                        notionistConfig = event.configJson,
                        pendingCropUri = null,
                        isModified = isMod
                    )
                }
            }
            PersonEditUiEvent.OnCancelCrop -> {
                _uiState.update { it.copy(pendingCropUri = null) }
            }
            PersonEditUiEvent.OnPhotoDecodeFailed -> {
                _uiState.update {
                    it.copy(
                        pendingCropUri = null,
                        userMessage = "Could not load selected image"
                    )
                }
            }
            PersonEditUiEvent.OnRemovePhoto -> {
                _uiState.update { current ->
                    avatarStorage?.discardStagedAvatar(
                        stagedPhotoUri = current.photoUri,
                        originalPhotoUri = initialContact?.photoUri
                    )
                    val isMod = checkModified(current.copy(photoUri = null, notionistConfig = null))
                    current.copy(
                        photoUri = null,
                        notionistConfig = null,
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
            PersonEditUiEvent.OnSaveClick -> {
                saveChanges()
            }
            PersonEditUiEvent.OnBackClick -> {
                if (_uiState.value.isModified) {
                    _uiState.update { it.copy(showDiscardDialog = true) }
                }
            }
            PersonEditUiEvent.OnConfirmDiscard -> {
                avatarStorage?.discardStagedAvatar(
                    stagedPhotoUri = _uiState.value.photoUri,
                    originalPhotoUri = initialContact?.photoUri
                )
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
                avatarStorage?.discardStagedAvatar(
                    stagedPhotoUri = _uiState.value.photoUri,
                    originalPhotoUri = initialContact?.photoUri
                )
                avatarStorage?.deleteLocalAvatarFile(initialContact?.photoUri)
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
        val samePhone = PhoneInputUtils.normalizeForStorage(current.phone) == PhoneInputUtils.normalizeForStorage(base.phone)
        val sameEmail = current.email.trim() == (base.email ?: "").trim()
        val sameNotes = current.notes.trim() == base.notes.trim()
        val samePhoto = current.photoUri == base.photoUri
        val sameNotionist = current.notionistConfig == base.notionistConfig
        val sameTags = current.selectedTagIds == base.tagIds.toSet()
        val sameRole = current.role.trim() == base.role.orEmpty()
        val sameCompany = current.company.trim() == base.company.orEmpty()
        val sameBirthday = current.birthday == base.birthday

        return !(sameName && sameAliases && samePhone && sameEmail && sameNotes && samePhoto && sameNotionist && sameTags && sameRole && sameCompany && sameBirthday)
    }

    private fun saveChanges() {
        val current = _uiState.value
        val cleanName = current.name.trim()
        if (cleanName.isBlank()) return

        val phoneValidationError = PhoneInputUtils.validateInput(current.phone)
        if (phoneValidationError != null) {
            _uiState.update { it.copy(phoneError = phoneValidationError) }
            return
        }

        val base = initialContact
        val committedPhotoUri = if (avatarStorage != null) {
            avatarStorage.commitStagedAvatar(
                contactId = contactId,
                currentPhotoUri = current.photoUri,
                originalPhotoUri = base?.photoUri
            )
        } else {
            current.photoUri
        }
        val committedNotionistConfig = if (committedPhotoUri != null) current.notionistConfig else null
        val normalizedPhone = PhoneInputUtils.normalizeForStorage(current.phone)
        val updated = Contact(
            id = contactId,
            name = cleanName,
            aliases = current.aliases,
            role = current.role.trim().ifEmpty { null },
            company = current.company.trim().ifEmpty { null },
            birthday = current.birthday,
            phone = normalizedPhone,
            email = current.email.trim().ifEmpty { null },
            tagIds = current.selectedTagIds.toList(),
            notes = current.notes.trim(),
            photoUri = committedPhotoUri,
            notionistConfig = committedNotionistConfig,
            firstMetAt = base?.firstMetAt,
            lastMetAt = base?.lastMetAt,
            meetCount = base?.meetCount ?: 0,
            isArchived = base?.isArchived ?: false,
            isFavorite = base?.isFavorite ?: false,
            updatedAt = Instant.now(),
            customProperties = current.customProperties
        )

        repository.updateContact(updated)
        initialContact = updated
        _uiState.update {
            it.copy(
                phone = normalizedPhone.orEmpty(),
                phoneError = null,
                photoUri = committedPhotoUri,
                notionistConfig = committedNotionistConfig,
                isModified = false,
                isSaved = true,
                userMessage = "Contact updated"
            )
        }
    }
}
