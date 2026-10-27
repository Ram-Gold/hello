package com.example.hello.ui.settings

import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.domain.model.Tag

data class SettingsUiState(
    val terminology: RelationshipTerminology = RelationshipTerminology.DEFAULT,
    val tags: List<Tag> = emptyList(),
    val isAddTagDialogOpen: Boolean = false,
    val editingTag: Tag? = null,
    val mergingSourceTag: Tag? = null,
    val isExporting: Boolean = false,
    val userMessage: String? = null,
    val exportSharePayload: String? = null
)

sealed interface SettingsUiEvent {
    data class OnSelectTerminology(val terminology: RelationshipTerminology) : SettingsUiEvent
    data object OnOpenAddTagDialog : SettingsUiEvent
    data object OnCloseTagDialogs : SettingsUiEvent
    data class OnCreateTag(val name: String, val colorHex: String) : SettingsUiEvent
    data class OnOpenEditTagDialog(val tag: Tag) : SettingsUiEvent
    data class OnUpdateTag(val tag: Tag) : SettingsUiEvent
    data class OnOpenMergeTagDialog(val sourceTag: Tag) : SettingsUiEvent
    data class OnConfirmMergeTag(val sourceTagId: String, val targetTagId: String) : SettingsUiEvent
    data class OnDeleteTag(val tagId: String) : SettingsUiEvent
    data class OnStartExportBundle(val outputUri: String? = null) : SettingsUiEvent
    data object OnExportMarkdownZip : SettingsUiEvent
    data object OnExportBackupJson : SettingsUiEvent
    data object OnRebuildCache : SettingsUiEvent
    data object OnDismissUserMessage : SettingsUiEvent
    data object OnClearExportPayload : SettingsUiEvent
}
