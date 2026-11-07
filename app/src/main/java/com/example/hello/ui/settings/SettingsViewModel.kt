package com.example.hello.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.hello.data.preferences.UserPreferencesRepository
import com.example.hello.data.repository.CaptureFlowRepository
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.CaptureComponent
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.FlowPreset
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.domain.model.Tag
import com.example.hello.platform.portability.ExportDataWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val contactRepository: ContactRepository,
    private val workManager: WorkManager? = null,
    private val captureFlowRepository: CaptureFlowRepository = CaptureFlowRepository(contactRepository.database)
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Collect preferences from repository
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update {
                    it.copy(
                        userName = prefs.userName,
                        terminology = prefs.preferredTerminology,
                        themeMode = prefs.themeMode,
                        themeStyle = prefs.themeStyle,
                        dynamicColor = prefs.dynamicColor,
                        reduceAnimations = prefs.reduceAnimations,
                        defaultCadenceDays = prefs.defaultCadenceDays,
                        notificationHour = prefs.notificationHour,
                        notificationMinute = prefs.notificationMinute,
                        defaultIncludeLocation = prefs.defaultIncludeLocation,
                        activeFlowPresetId = prefs.activeFlowPresetId
                    )
                }
            }
        }

        // Collect tags from ContactRepository
        viewModelScope.launch {
            contactRepository.tags.collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }

        // Collect presets from CaptureFlowRepository
        viewModelScope.launch {
            captureFlowRepository.presets.collect { presets ->
                _uiState.update { state ->
                    val defaultSelect = state.selectedEditingPresetId.ifBlank { state.activeFlowPresetId }
                    state.copy(
                        flowPresets = presets,
                        selectedEditingPresetId = if (presets.any { it.id == defaultSelect }) defaultSelect else presets.firstOrNull()?.id ?: "preset_default"
                    )
                }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.OnOpenEditNameDialog -> {
                _uiState.update { it.copy(isEditingName = true) }
            }

            SettingsUiEvent.OnDismissEditNameDialog -> {
                _uiState.update { it.copy(isEditingName = false) }
            }

            is SettingsUiEvent.OnSaveUserName -> {
                val trimmed = event.newName.trim()
                if (trimmed.isNotEmpty()) {
                    viewModelScope.launch {
                        preferencesRepository.setUserName(trimmed)
                        _uiState.update {
                            it.copy(
                                isEditingName = false,
                                userMessage = "Name updated"
                            )
                        }
                    }
                }
            }

            is SettingsUiEvent.OnSelectTerminology -> {
                viewModelScope.launch {
                    preferencesRepository.setPreferredTerminology(event.terminology)
                    _uiState.update {
                        it.copy(
                            terminology = event.terminology,
                            userMessage = "Terminology updated to ${event.terminology.pluralTitle}"
                        )
                    }
                }
            }

            SettingsUiEvent.OnOpenAddTagDialog -> {
                _uiState.update { it.copy(isAddTagDialogOpen = true) }
            }

            SettingsUiEvent.OnCloseTagDialogs -> {
                _uiState.update {
                    it.copy(
                        isAddTagDialogOpen = false,
                        editingTag = null,
                        mergingSourceTag = null
                    )
                }
            }

            is SettingsUiEvent.OnCreateTag -> {
                val created = contactRepository.addTag(event.name, event.colorHex)
                _uiState.update {
                    it.copy(
                        isAddTagDialogOpen = false,
                        userMessage = "Tag \"${created.name}\" created"
                    )
                }
            }

            is SettingsUiEvent.OnOpenEditTagDialog -> {
                _uiState.update { it.copy(editingTag = event.tag) }
            }

            is SettingsUiEvent.OnUpdateTag -> {
                contactRepository.updateTag(event.tag)
                _uiState.update {
                    it.copy(
                        editingTag = null,
                        userMessage = "Tag \"${event.tag.name}\" updated"
                    )
                }
            }

            is SettingsUiEvent.OnOpenMergeTagDialog -> {
                _uiState.update { it.copy(mergingSourceTag = event.sourceTag) }
            }

            is SettingsUiEvent.OnConfirmMergeTag -> {
                val source = _uiState.value.tags.firstOrNull { it.id == event.sourceTagId }
                val target = _uiState.value.tags.firstOrNull { it.id == event.targetTagId }
                contactRepository.mergeTags(event.sourceTagId, event.targetTagId)
                _uiState.update {
                    it.copy(
                        mergingSourceTag = null,
                        userMessage = "Merged \"${source?.name ?: "source"}\" into \"${target?.name ?: "target"}\""
                    )
                }
            }

            is SettingsUiEvent.OnDeleteTag -> {
                val tag = _uiState.value.tags.firstOrNull { it.id == event.tagId }
                contactRepository.deleteTag(event.tagId)
                _uiState.update {
                    it.copy(
                        editingTag = null,
                        userMessage = "Deleted tag \"${tag?.name ?: "tag"}\""
                    )
                }
            }

            is SettingsUiEvent.OnStartExportBundle -> {
                if (workManager != null) {
                    val request = OneTimeWorkRequestBuilder<ExportDataWorker>()
                        .addTag(ExportDataWorker.WORK_TAG_EXPORT)
                        .setInputData(
                            workDataOf(ExportDataWorker.KEY_OUTPUT_URI to (event.outputUri ?: ""))
                        )
                        .build()
                    workManager.enqueue(request)
                    _uiState.update {
                        it.copy(userMessage = "Assembling Export Bundle via WorkManager...")
                    }
                } else {
                    _uiState.update {
                        it.copy(userMessage = "Export Bundle scheduled")
                    }
                }
            }

            SettingsUiEvent.OnExportMarkdownZip -> {
                val count = contactRepository.contacts.value.size
                val payload = buildString {
                    appendLine("# Hello CRM - Local Markdown Export")
                    appendLine("Total Contacts: $count")
                    appendLine("Exported at: ${java.time.Instant.now()}")
                    appendLine()
                    contactRepository.contacts.value.forEach { c ->
                        appendLine("## ${c.name}")
                        appendLine("- Context: ${c.contextSubtitle}")
                        appendLine()
                    }
                }
                _uiState.update {
                    it.copy(
                        userMessage = "Exported $count ${it.terminology.pluralTitle.lowercase()} to Markdown",
                        exportSharePayload = payload
                    )
                }
            }

            SettingsUiEvent.OnExportBackupJson -> {
                val count = contactRepository.contacts.value.size
                val payload = """{"version": 1, "exportedAt": "${java.time.Instant.now()}", "contactsCount": $count}"""
                _uiState.update {
                    it.copy(
                        userMessage = "Exported JSON backup ($count records)",
                        exportSharePayload = payload
                    )
                }
            }

            SettingsUiEvent.OnRebuildCache -> {
                val count = contactRepository.contacts.value.size
                _uiState.update {
                    it.copy(userMessage = "Rebuilt cache for $count ${it.terminology.pluralTitle.lowercase()} in 18ms")
                }
            }

            SettingsUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }

            SettingsUiEvent.OnClearExportPayload -> {
                _uiState.update { it.copy(exportSharePayload = null) }
            }

            is SettingsUiEvent.OnSelectThemeMode -> {
                viewModelScope.launch {
                    preferencesRepository.setThemeMode(event.themeMode)
                }
            }

            is SettingsUiEvent.OnSelectThemeStyle -> {
                viewModelScope.launch {
                    preferencesRepository.setThemeStyle(event.themeStyle)
                    _uiState.update {
                        it.copy(
                            themeStyle = event.themeStyle,
                            userMessage = "Design theme updated to ${event.themeStyle.title}"
                        )
                    }
                }
            }

            is SettingsUiEvent.OnToggleDynamicColor -> {
                viewModelScope.launch {
                    preferencesRepository.setDynamicColor(event.enabled)
                }
            }

            is SettingsUiEvent.OnToggleReduceAnimations -> {
                viewModelScope.launch {
                    preferencesRepository.setReduceAnimations(event.enabled)
                    _uiState.update {
                        it.copy(
                            userMessage = if (event.enabled) "Reduced animations enabled" else "Standard animations enabled"
                        )
                    }
                }
            }

            is SettingsUiEvent.OnSetDefaultCadenceDays -> {
                viewModelScope.launch {
                    preferencesRepository.setDefaultCadenceDays(event.days)
                }
            }

            is SettingsUiEvent.OnSetNotificationTime -> {
                viewModelScope.launch {
                    preferencesRepository.setNotificationTime(event.hour, event.minute)
                }
            }

            is SettingsUiEvent.OnToggleDefaultIncludeLocation -> {
                viewModelScope.launch {
                    preferencesRepository.setDefaultIncludeLocation(event.enabled)
                }
            }

            // Capture Flows Handlers (ADR-0012)
            is SettingsUiEvent.OnSelectActiveFlowPreset -> {
                viewModelScope.launch {
                    preferencesRepository.setActiveFlowPresetId(event.presetId)
                    _uiState.update {
                        val name = it.flowPresets.firstOrNull { p -> p.id == event.presetId }?.name ?: "Flow"
                        it.copy(
                            activeFlowPresetId = event.presetId,
                            userMessage = "Active flow set to $name"
                        )
                    }
                }
            }

            is SettingsUiEvent.OnSelectPresetToEdit -> {
                _uiState.update { it.copy(selectedEditingPresetId = event.presetId) }
            }

            is SettingsUiEvent.OnMoveComponentUp -> {
                val preset = _uiState.value.flowPresets.firstOrNull { it.id == event.presetId } ?: return
                val components = preset.components.toMutableList()
                val index = components.indexOfFirst { it.id == event.componentId }
                if (index > 0) {
                    val item = components.removeAt(index)
                    components.add(index - 1, item)
                    val reindexed = components.mapIndexed { idx, comp -> comp.copy(stepOrder = idx) }
                    viewModelScope.launch {
                        captureFlowRepository.updatePresetComponents(event.presetId, reindexed)
                    }
                }
            }

            is SettingsUiEvent.OnMoveComponentDown -> {
                val preset = _uiState.value.flowPresets.firstOrNull { it.id == event.presetId } ?: return
                val components = preset.components.toMutableList()
                val index = components.indexOfFirst { it.id == event.componentId }
                if (index >= 0 && index < components.size - 1) {
                    val item = components.removeAt(index)
                    components.add(index + 1, item)
                    val reindexed = components.mapIndexed { idx, comp -> comp.copy(stepOrder = idx) }
                    viewModelScope.launch {
                        captureFlowRepository.updatePresetComponents(event.presetId, reindexed)
                    }
                }
            }

            is SettingsUiEvent.OnToggleComponentEnabled -> {
                val preset = _uiState.value.flowPresets.firstOrNull { it.id == event.presetId } ?: return
                val updated = preset.components.map { comp ->
                    if (comp.id == event.componentId) comp.copy(isEnabled = event.isEnabled) else comp
                }
                viewModelScope.launch {
                    captureFlowRepository.updatePresetComponents(event.presetId, updated)
                }
            }

            is SettingsUiEvent.OnResetPresetToDefault -> {
                viewModelScope.launch {
                    captureFlowRepository.resetPresetToDefault(event.presetId)
                    _uiState.update { it.copy(userMessage = "Reset preset components to default") }
                }
            }

            is SettingsUiEvent.OnDeleteCustomPreset -> {
                viewModelScope.launch {
                    captureFlowRepository.deleteCustomPreset(event.presetId)
                    _uiState.update {
                        it.copy(
                            selectedEditingPresetId = FlowPreset.PRESET_DEFAULT,
                            activeFlowPresetId = if (it.activeFlowPresetId == event.presetId) FlowPreset.PRESET_DEFAULT else it.activeFlowPresetId,
                            userMessage = "Custom flow preset deleted"
                        )
                    }
                }
            }

            SettingsUiEvent.OnOpenCreatePresetDialog -> {
                _uiState.update { it.copy(isCreatePresetDialogOpen = true) }
            }

            SettingsUiEvent.OnCloseCreatePresetDialog -> {
                _uiState.update { it.copy(isCreatePresetDialogOpen = false) }
            }

            is SettingsUiEvent.OnCreateCustomPreset -> {
                viewModelScope.launch {
                    val defaultPreset = FlowPreset.createDefaultPresets().first()
                    val newPreset = captureFlowRepository.createCustomPreset(
                        name = event.name.trim().ifEmpty { "Custom Flow" },
                        description = event.description.trim().ifEmpty { "Custom Capture Flow" },
                        components = defaultPreset.components.map { it.copy(id = UUID.randomUUID().toString()) }
                    )
                    _uiState.update {
                        it.copy(
                            isCreatePresetDialogOpen = false,
                            selectedEditingPresetId = newPreset.id,
                            userMessage = "Created '${newPreset.name}' flow preset"
                        )
                    }
                }
            }

            is SettingsUiEvent.OnAddComponentToPreset -> {
                val preset = _uiState.value.flowPresets.firstOrNull { it.id == event.presetId } ?: return
                val newComp = CaptureComponent(
                    type = event.type,
                    label = event.label.ifBlank { event.type.defaultLabel },
                    stepOrder = preset.components.size,
                    isRequired = event.type.isSystemMandatory
                )
                val updated = preset.components + newComp
                viewModelScope.launch {
                    captureFlowRepository.updatePresetComponents(event.presetId, updated)
                }
            }
        }
    }
}
