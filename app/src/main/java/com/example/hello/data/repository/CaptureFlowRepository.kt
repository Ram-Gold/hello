package com.example.hello.data.repository

import com.example.hello.data.local.HelloDatabase
import com.example.hello.data.local.entity.toDomain
import com.example.hello.data.local.entity.toEntity
import com.example.hello.domain.model.CaptureComponent
import com.example.hello.domain.model.FlowPreset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Repository coordinating Capture Flow presets and components in Room and in-memory caches.
 */
class CaptureFlowRepository(
    val database: HelloDatabase? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coroutineScope: CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())
) {
    private val _presets = MutableStateFlow<List<FlowPreset>>(FlowPreset.createDefaultPresets())
    val presets: StateFlow<List<FlowPreset>> = _presets.asStateFlow()

    init {
        if (database != null) {
            coroutineScope.launch {
                seedPresetsIfEmpty()
                observePresetsStream()
            }
        }
    }

    suspend fun seedPresetsIfEmpty() {
        val db = database ?: return
        val existing = db.captureFlowDao().getAllPresetEntities()
        if (existing.isEmpty()) {
            val defaults = FlowPreset.createDefaultPresets()
            defaults.forEachIndexed { index, preset ->
                db.captureFlowDao().insertPresets(listOf(preset.toEntity(displayOrder = index)))
                db.captureFlowDao().insertComponents(preset.components.map { it.toEntity(preset.id) })
            }
        }
    }

    private fun observePresetsStream() {
        val db = database ?: return
        coroutineScope.launch {
            db.captureFlowDao().getAllPresetsWithComponentsFlow().collect { entities ->
                if (entities.isNotEmpty()) {
                    _presets.value = entities.map { it.toDomain() }
                }
            }
        }
    }

    suspend fun updatePresetComponents(presetId: String, components: List<CaptureComponent>) {
        val updatedList = _presets.value.map { preset ->
            if (preset.id == presetId) {
                preset.copy(components = components)
            } else {
                preset
            }
        }
        _presets.value = updatedList

        val db = database ?: return
        db.captureFlowDao().deleteComponentsForPreset(presetId)
        db.captureFlowDao().insertComponents(components.map { it.toEntity(presetId) })
    }

    suspend fun resetPresetToDefault(presetId: String) {
        val defaultPreset = FlowPreset.createDefaultPresets().firstOrNull { it.id == presetId } ?: return
        updatePresetComponents(presetId, defaultPreset.components)
    }

    suspend fun createCustomPreset(
        name: String,
        description: String,
        iconName: String = "Person",
        components: List<CaptureComponent>
    ): FlowPreset {
        val newPreset = FlowPreset(
            id = "preset_custom_${UUID.randomUUID()}",
            name = name,
            description = description,
            iconName = iconName,
            isSystemPreset = false,
            components = components
        )
        val updated = _presets.value + newPreset
        _presets.value = updated

        val db = database
        if (db != null) {
            db.captureFlowDao().insertPresets(listOf(newPreset.toEntity(displayOrder = updated.size - 1)))
            db.captureFlowDao().insertComponents(newPreset.components.map { it.toEntity(newPreset.id) })
        }
        return newPreset
    }

    suspend fun deleteCustomPreset(presetId: String) {
        val preset = _presets.value.firstOrNull { it.id == presetId } ?: return
        if (preset.isSystemPreset) return // System presets cannot be deleted

        _presets.value = _presets.value.filterNot { it.id == presetId }
        val db = database ?: return
        db.captureFlowDao().deleteCustomPreset(presetId)
    }
}
