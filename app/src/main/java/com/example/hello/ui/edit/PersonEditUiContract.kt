package com.example.hello.ui.edit

import com.example.hello.domain.model.Circle
import com.example.hello.domain.model.Tag

data class PersonEditUiState(
    val contactId: String = "",
    val name: String = "",
    val aliases: List<String> = emptyList(),
    val aliasInput: String = "",
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val selectedCircles: Set<Circle> = emptySet(),
    val selectedTagIds: Set<String> = emptySet(),
    val availableTags: List<Tag> = emptyList(),
    val cadenceDays: Int? = 14,
    val isCustomCadence: Boolean = false,
    val customCadenceInput: String = "",
    val isModified: Boolean = false,
    val canSave: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isArchivedCompleted: Boolean = false,
    val isDeletedCompleted: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showArchiveDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showNewTagDialog: Boolean = false,
    val userMessage: String? = null
)

sealed interface PersonEditUiEvent {
    data class OnNameChange(val name: String) : PersonEditUiEvent
    data class OnAliasInputChange(val text: String) : PersonEditUiEvent
    data class OnAddAlias(val alias: String) : PersonEditUiEvent
    data class OnRemoveAlias(val alias: String) : PersonEditUiEvent
    data class OnPhoneChange(val phone: String) : PersonEditUiEvent
    data class OnEmailChange(val email: String) : PersonEditUiEvent
    data class OnNotesChange(val notes: String) : PersonEditUiEvent
    data class OnPhotoSelected(val uri: String?) : PersonEditUiEvent
    data object OnRemovePhoto : PersonEditUiEvent
    data class OnToggleCircle(val circle: Circle) : PersonEditUiEvent
    data class OnToggleTag(val tagId: String) : PersonEditUiEvent
    data class OnCreateTag(val name: String) : PersonEditUiEvent
    data class OnSelectCadence(val days: Int?) : PersonEditUiEvent
    data class OnCustomCadenceChange(val daysString: String) : PersonEditUiEvent
    data object OnSaveClick : PersonEditUiEvent
    data object OnBackClick : PersonEditUiEvent
    data object OnConfirmDiscard : PersonEditUiEvent
    data object OnDismissDiscardDialog : PersonEditUiEvent
    data object OnShowArchiveDialog : PersonEditUiEvent
    data object OnDismissArchiveDialog : PersonEditUiEvent
    data object OnConfirmArchive : PersonEditUiEvent
    data object OnShowDeleteDialog : PersonEditUiEvent
    data object OnDismissDeleteDialog : PersonEditUiEvent
    data object OnConfirmDelete : PersonEditUiEvent
    data class OnShowNewTagDialog(val show: Boolean) : PersonEditUiEvent
    data object OnDismissUserMessage : PersonEditUiEvent
}
