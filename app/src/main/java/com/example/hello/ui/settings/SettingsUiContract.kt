package com.example.hello.ui.settings

import com.example.hello.data.preferences.ThemeMode
import com.example.hello.data.preferences.ThemeStyle
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.FlowPreset
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.domain.model.Tag

data class SettingsUiState(
    val userName: String = "Alex",
    val isEditingName: Boolean = false,
    val terminology: RelationshipTerminology = RelationshipTerminology.DEFAULT,
    val tags: List<Tag> = emptyList(),
    val isAddTagDialogOpen: Boolean = false,
    val editingTag: Tag? = null,
    val mergingSourceTag: Tag? = null,
    val isExporting: Boolean = false,
    val userMessage: String? = null,
    val exportSharePayload: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeStyle: ThemeStyle = ThemeStyle.MONOCHROME,
    val dynamicColor: Boolean = false,
    val reduceAnimations: Boolean = false,
    val defaultCadenceDays: Int = 30,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val defaultIncludeLocation: Boolean = true,
    // Capture Flows Settings (ADR-0012)
    val activeFlowPresetId: String = "preset_default",
    val flowPresets: List<FlowPreset> = emptyList(),
    val selectedEditingPresetId: String = "preset_default",
    val isCreatePresetDialogOpen: Boolean = false
) {
    val selectedEditingPreset: FlowPreset?
        get() = flowPresets.firstOrNull { it.id == selectedEditingPresetId } ?: flowPresets.firstOrNull()
}

sealed interface SettingsUiEvent {
    data object OnOpenEditNameDialog : SettingsUiEvent
    data object OnDismissEditNameDialog : SettingsUiEvent
    data class OnSaveUserName(val newName: String) : SettingsUiEvent
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
    data class OnSelectThemeMode(val themeMode: ThemeMode) : SettingsUiEvent
    data class OnSelectThemeStyle(val themeStyle: ThemeStyle) : SettingsUiEvent
    data class OnToggleDynamicColor(val enabled: Boolean) : SettingsUiEvent
    data class OnToggleReduceAnimations(val enabled: Boolean) : SettingsUiEvent
    data class OnSetDefaultCadenceDays(val days: Int) : SettingsUiEvent
    data class OnSetNotificationTime(val hour: Int, val minute: Int) : SettingsUiEvent
    data class OnToggleDefaultIncludeLocation(val enabled: Boolean) : SettingsUiEvent

    // Capture Flows Events
    data class OnSelectActiveFlowPreset(val presetId: String) : SettingsUiEvent
    data class OnSelectPresetToEdit(val presetId: String) : SettingsUiEvent
    data class OnMoveComponentUp(val presetId: String, val componentId: String) : SettingsUiEvent
    data class OnMoveComponentDown(val presetId: String, val componentId: String) : SettingsUiEvent
    data class OnToggleComponentEnabled(val presetId: String, val componentId: String, val isEnabled: Boolean) : SettingsUiEvent
    data class OnResetPresetToDefault(val presetId: String) : SettingsUiEvent
    data class OnDeleteCustomPreset(val presetId: String) : SettingsUiEvent
    data object OnOpenCreatePresetDialog : SettingsUiEvent
    data object OnCloseCreatePresetDialog : SettingsUiEvent
    data class OnCreateCustomPreset(val name: String, val description: String) : SettingsUiEvent
    data class OnAddComponentToPreset(val presetId: String, val type: CaptureComponentType, val label: String) : SettingsUiEvent
}
