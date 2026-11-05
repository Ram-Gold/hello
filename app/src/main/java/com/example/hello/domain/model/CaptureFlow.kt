package com.example.hello.domain.model

import java.util.UUID

/**
 * Modular component types available in Hello's Capture Flow engine (ADR-0012).
 */
enum class CaptureComponentType(
    val defaultLabel: String,
    val isSystemMandatory: Boolean = false
) {
    NAME("What's their name?", isSystemMandatory = true),
    ROLE("What do they do?"),
    COMPANY("Where do they work or study?"),
    PLACE("Where did you meet?"),
    CIRCLES("Which circle?"),
    TAGS("Add tags"),
    EMAIL("Email address"),
    PHONE("Phone number"),
    BIRTHDAY("When is their birthday?"),
    CADENCE("How often to keep in touch?"),
    CUSTOM("Custom property"),
    NOTE("Any notes or memories?")
}

/**
 * A self-contained modular step in a capture flow.
 */
data class CaptureComponent(
    val id: String = UUID.randomUUID().toString(),
    val type: CaptureComponentType,
    val label: String = type.defaultLabel,
    val stepOrder: Int,
    val isRequired: Boolean = type.isSystemMandatory,
    val customKey: String? = null,
    val isEnabled: Boolean = true
)

/**
 * A named capture template defining an ordered sequence of components.
 */
data class FlowPreset(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String = "Person",
    val isSystemPreset: Boolean = false,
    val components: List<CaptureComponent> = emptyList()
) {
    /**
     * Active components sorted by step order.
     */
    val activeComponents: List<CaptureComponent>
        get() = components.filter { it.isEnabled }.sortedBy { it.stepOrder }

    companion object {
        const val PRESET_DEFAULT = "preset_default"
        const val PRESET_CONFERENCE = "preset_conference"
        const val PRESET_CASUAL_PARTY = "preset_casual_party"
        const val PRESET_CAMPUS = "preset_campus"

        /**
         * Built-in system presets seeded out-of-the-box (ADR-0012).
         */
        fun createDefaultPresets(): List<FlowPreset> = listOf(
            FlowPreset(
                id = PRESET_DEFAULT,
                name = "Default",
                description = "Name → Role → Company → Place → Note",
                iconName = "Sparkles",
                isSystemPreset = true,
                components = listOf(
                    CaptureComponent(id = "comp_def_1", type = CaptureComponentType.NAME, stepOrder = 0, isRequired = true),
                    CaptureComponent(id = "comp_def_2", type = CaptureComponentType.ROLE, stepOrder = 1),
                    CaptureComponent(id = "comp_def_3", type = CaptureComponentType.COMPANY, stepOrder = 2),
                    CaptureComponent(id = "comp_def_4", type = CaptureComponentType.PLACE, stepOrder = 3),
                    CaptureComponent(id = "comp_def_5", type = CaptureComponentType.NOTE, stepOrder = 4)
                )
            ),
            FlowPreset(
                id = PRESET_CONFERENCE,
                name = "Conference",
                description = "Name → Company → Role → Email → Tags → Note",
                iconName = "BusinessCenter",
                isSystemPreset = true,
                components = listOf(
                    CaptureComponent(id = "comp_conf_1", type = CaptureComponentType.NAME, stepOrder = 0, isRequired = true),
                    CaptureComponent(id = "comp_conf_2", type = CaptureComponentType.COMPANY, stepOrder = 1),
                    CaptureComponent(id = "comp_conf_3", type = CaptureComponentType.ROLE, stepOrder = 2),
                    CaptureComponent(id = "comp_conf_4", type = CaptureComponentType.EMAIL, stepOrder = 3),
                    CaptureComponent(id = "comp_conf_5", type = CaptureComponentType.TAGS, stepOrder = 4),
                    CaptureComponent(id = "comp_conf_6", type = CaptureComponentType.NOTE, stepOrder = 5)
                )
            ),
            FlowPreset(
                id = PRESET_CASUAL_PARTY,
                name = "Casual Party",
                description = "Name → Place → Phone → Tags → Note",
                iconName = "Celebration",
                isSystemPreset = true,
                components = listOf(
                    CaptureComponent(id = "comp_party_1", type = CaptureComponentType.NAME, stepOrder = 0, isRequired = true),
                    CaptureComponent(id = "comp_party_2", type = CaptureComponentType.PLACE, stepOrder = 1),
                    CaptureComponent(id = "comp_party_3", type = CaptureComponentType.PHONE, stepOrder = 2),
                    CaptureComponent(id = "comp_party_4", type = CaptureComponentType.TAGS, stepOrder = 3),
                    CaptureComponent(id = "comp_party_5", type = CaptureComponentType.NOTE, stepOrder = 4)
                )
            ),
            FlowPreset(
                id = PRESET_CAMPUS,
                name = "Campus / Uni",
                description = "Name → University → Circles → Phone → Note",
                iconName = "School",
                isSystemPreset = true,
                components = listOf(
                    CaptureComponent(id = "comp_campus_1", type = CaptureComponentType.NAME, stepOrder = 0, isRequired = true),
                    CaptureComponent(id = "comp_campus_2", type = CaptureComponentType.COMPANY, label = "University / Major", stepOrder = 1),
                    CaptureComponent(id = "comp_campus_3", type = CaptureComponentType.CIRCLES, stepOrder = 2),
                    CaptureComponent(id = "comp_campus_4", type = CaptureComponentType.PHONE, stepOrder = 3),
                    CaptureComponent(id = "comp_campus_5", type = CaptureComponentType.NOTE, stepOrder = 4)
                )
            )
        )
    }
}
