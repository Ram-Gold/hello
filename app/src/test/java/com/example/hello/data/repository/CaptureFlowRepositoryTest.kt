package com.example.hello.data.repository

import com.example.hello.domain.model.CaptureComponent
import com.example.hello.domain.model.CaptureComponentType
import com.example.hello.domain.model.FlowPreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureFlowRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CaptureFlowRepository

    @Before
    fun setUp() {
        repository = CaptureFlowRepository(
            database = null,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun initialPresets_containFourSystemPresets() {
        val presets = repository.presets.value
        assertEquals(4, presets.size)

        val defaultPreset = presets.find { it.id == FlowPreset.PRESET_DEFAULT }
        assertNotNull(defaultPreset)
        assertTrue(defaultPreset!!.isSystemPreset)
        assertEquals("Default", defaultPreset.name)

        val conferencePreset = presets.find { it.id == FlowPreset.PRESET_CONFERENCE }
        assertNotNull(conferencePreset)
        assertTrue(conferencePreset!!.isSystemPreset)

        val casualPartyPreset = presets.find { it.id == FlowPreset.PRESET_CASUAL_PARTY }
        assertNotNull(casualPartyPreset)
        assertTrue(casualPartyPreset!!.isSystemPreset)

        val campusPreset = presets.find { it.id == FlowPreset.PRESET_CAMPUS }
        assertNotNull(campusPreset)
        assertTrue(campusPreset!!.isSystemPreset)
    }

    @Test
    fun defaultPreset_hasExpectedSequenceOfComponents() {
        val defaultPreset = repository.presets.value.first { it.id == FlowPreset.PRESET_DEFAULT }
        val types = defaultPreset.components.map { it.type }

        assertEquals(
            listOf(
                CaptureComponentType.NAME,
                CaptureComponentType.ROLE,
                CaptureComponentType.COMPANY,
                CaptureComponentType.PLACE,
                CaptureComponentType.NOTE
            ),
            types
        )
    }

    @Test
    fun activeComponents_filtersDisabledAndSortsByStepOrder() {
        val defaultPreset = repository.presets.value.first { it.id == FlowPreset.PRESET_DEFAULT }
        val modifiedComponents = defaultPreset.components.mapIndexed { index, comp ->
            if (comp.type == CaptureComponentType.ROLE) {
                comp.copy(isEnabled = false)
            } else {
                comp.copy(stepOrder = index)
            }
        }

        val presetWithDisabledRole = defaultPreset.copy(components = modifiedComponents)
        val active = presetWithDisabledRole.activeComponents

        assertFalse(active.any { it.type == CaptureComponentType.ROLE })
        assertEquals(4, active.size)
        assertEquals(CaptureComponentType.NAME, active[0].type)
        assertEquals(CaptureComponentType.COMPANY, active[1].type)
        assertEquals(CaptureComponentType.PLACE, active[2].type)
        assertEquals(CaptureComponentType.NOTE, active[3].type)
    }

    @Test
    fun updatePresetComponents_updatesComponentsInStateFlow() = runTest {
        val defaultPreset = repository.presets.value.first { it.id == FlowPreset.PRESET_DEFAULT }
        val reversed = defaultPreset.components.reversed().mapIndexed { index, comp ->
            comp.copy(stepOrder = index)
        }

        repository.updatePresetComponents(defaultPreset.id, reversed)

        val updated = repository.presets.value.first { it.id == defaultPreset.id }
        assertEquals(CaptureComponentType.NOTE, updated.components.first().type)
        assertEquals(CaptureComponentType.NAME, updated.components.last().type)
    }

    @Test
    fun resetPresetToDefault_restoresOriginalComponents() = runTest {
        val defaultPreset = repository.presets.value.first { it.id == FlowPreset.PRESET_DEFAULT }
        val singleComp = listOf(CaptureComponent(id = "comp_single", type = CaptureComponentType.NAME, stepOrder = 0))

        repository.updatePresetComponents(defaultPreset.id, singleComp)
        assertEquals(1, repository.presets.value.first { it.id == defaultPreset.id }.components.size)

        repository.resetPresetToDefault(defaultPreset.id)
        val resetPreset = repository.presets.value.first { it.id == defaultPreset.id }
        assertEquals(5, resetPreset.components.size)
        assertEquals(CaptureComponentType.NAME, resetPreset.components[0].type)
        assertEquals(CaptureComponentType.ROLE, resetPreset.components[1].type)
    }

    @Test
    fun createCustomPreset_addsNewPresetWithCustomFlag() = runTest {
        val customComponents = listOf(
            CaptureComponent(id = "c1", type = CaptureComponentType.NAME, stepOrder = 0, isRequired = true),
            CaptureComponent(id = "c2", type = CaptureComponentType.BIRTHDAY, stepOrder = 1),
            CaptureComponent(id = "c3", type = CaptureComponentType.CUSTOM, label = "Hometown", stepOrder = 2)
        )

        val newPreset = repository.createCustomPreset(
            name = "Family & Relatives",
            description = "Track birthdays and hometowns",
            components = customComponents
        )

        assertFalse(newPreset.isSystemPreset)
        assertEquals("Family & Relatives", newPreset.name)
        assertEquals(5, repository.presets.value.size)
        assertTrue(repository.presets.value.any { it.id == newPreset.id })
    }

    @Test
    fun deleteCustomPreset_removesCustomPresetOnly() = runTest {
        val newPreset = repository.createCustomPreset(
            name = "Temporary",
            description = "Temporary flow",
            components = emptyList()
        )
        assertEquals(5, repository.presets.value.size)

        // Delete custom preset -> succeeds
        repository.deleteCustomPreset(newPreset.id)
        assertEquals(4, repository.presets.value.size)
        assertNull(repository.presets.value.find { it.id == newPreset.id })

        // Attempt to delete system preset -> rejected
        repository.deleteCustomPreset(FlowPreset.PRESET_DEFAULT)
        assertEquals(4, repository.presets.value.size)
        assertNotNull(repository.presets.value.find { it.id == FlowPreset.PRESET_DEFAULT })
    }
}
