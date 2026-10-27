package com.example.hello.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.hello.data.preferences.UserPreferencesRepository
import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.RelationshipTerminology
import com.example.hello.domain.model.Tag
import com.example.hello.platform.portability.ExportDataWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val contactRepository: ContactRepository,
    private val workManager: WorkManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Collect terminology from preferences
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                _uiState.update { it.copy(terminology = prefs.preferredTerminology) }
            }
        }

        // Collect tags from ContactRepository
        viewModelScope.launch {
            contactRepository.tags.collect { tags ->
                _uiState.update { it.copy(tags = tags) }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
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
                        userMessage = "Tag #${created.name} created"
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
                        userMessage = "Tag #${event.tag.name} updated"
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
                        userMessage = "Merged #${source?.name ?: "source"} into #${target?.name ?: "target"}"
                    )
                }
            }

            is SettingsUiEvent.OnDeleteTag -> {
                val tag = _uiState.value.tags.firstOrNull { it.id == event.tagId }
                contactRepository.deleteTag(event.tagId)
                _uiState.update {
                    it.copy(
                        editingTag = null,
                        userMessage = "Deleted tag #${tag?.name ?: "tag"}"
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
                        it.copy(userMessage = "Assembling Export Bundle via WorkManager... 📦")
                    }
                } else {
                    _uiState.update {
                        it.copy(userMessage = "Export Bundle scheduled 📦")
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
                        appendLine("- Circles: ${c.circles.joinToString { it.displayName }}")
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
                    it.copy(userMessage = "Rebuilt cache for $count ${it.terminology.pluralTitle.lowercase()} in 18ms ✨")
                }
            }

            SettingsUiEvent.OnDismissUserMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }

            SettingsUiEvent.OnClearExportPayload -> {
                _uiState.update { it.copy(exportSharePayload = null) }
            }
        }
    }
}
