package com.example.hello.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.hello.domain.model.CaptureComponent
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.FlowPreset

@Entity(tableName = "capture_flow_presets")
data class CaptureFlowPresetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val iconName: String = "Person",
    val isSystemPreset: Boolean = false,
    val displayOrder: Int = 0
)

@Entity(
    tableName = "capture_flow_components",
    foreignKeys = [
        ForeignKey(
            entity = CaptureFlowPresetEntity::class,
            parentColumns = ["id"],
            childColumns = ["presetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["presetId"])]
)
data class CaptureFlowComponentEntity(
    @PrimaryKey
    val id: String,
    val presetId: String,
    val componentType: String,
    val label: String,
    val stepOrder: Int,
    val isRequired: Boolean,
    val customKey: String? = null,
    val isEnabled: Boolean = true
)

data class CaptureFlowPresetWithComponents(
    @Embedded
    val preset: CaptureFlowPresetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "presetId"
    )
    val components: List<CaptureFlowComponentEntity>
)

fun CaptureFlowPresetWithComponents.toDomain(): FlowPreset = FlowPreset(
    id = preset.id,
    name = preset.name,
    description = preset.description,
    iconName = preset.iconName,
    isSystemPreset = preset.isSystemPreset,
    components = components.map { it.toDomain() }.sortedBy { it.stepOrder }
)

fun CaptureFlowComponentEntity.toDomain(): CaptureComponent = CaptureComponent(
    id = id,
    type = try {
        CaptureComponentType.valueOf(componentType)
    } catch (_: Exception) {
        CaptureComponentType.CUSTOM
    },
    label = label,
    stepOrder = stepOrder,
    isRequired = isRequired,
    customKey = customKey,
    isEnabled = isEnabled
)

fun FlowPreset.toEntity(displayOrder: Int = 0): CaptureFlowPresetEntity = CaptureFlowPresetEntity(
    id = id,
    name = name,
    description = description,
    iconName = iconName,
    isSystemPreset = isSystemPreset,
    displayOrder = displayOrder
)

fun CaptureComponent.toEntity(presetId: String): CaptureFlowComponentEntity = CaptureFlowComponentEntity(
    id = id,
    presetId = presetId,
    componentType = type.name,
    label = label,
    stepOrder = stepOrder,
    isRequired = isRequired,
    customKey = customKey,
    isEnabled = isEnabled
)
