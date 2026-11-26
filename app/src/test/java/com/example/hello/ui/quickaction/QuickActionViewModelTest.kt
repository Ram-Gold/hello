package com.example.hello.ui.quickaction

import com.example.hello.data.repository.ContactRepository
import com.example.hello.domain.model.CaptureComponentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class QuickActionViewModelTest {

    private lateinit var repository: ContactRepository
    private lateinit var viewModel: QuickActionViewModel

    @Before
    fun setUp() {
        repository = ContactRepository()
        viewModel = QuickActionViewModel(repository)
    }

    @Test
    fun initialState_isClosedAndDefaultChoiceMode() {
        val state = viewModel.uiState.value
        assertFalse(state.isOpen)
        assertEquals(QuickActionMode.CHOICE, state.currentMode)
        assertEquals(FlowAStep.NAME, state.flowAStep)
        assertEquals("", state.newContactName)
        assertTrue(state.suggestedPlaces.isNotEmpty())
        assertTrue(state.suggestedTags.isNotEmpty())
    }

    @Test
    fun onOpenChoice_and_onDismiss_preservesActiveModeDuringExit_andResetsOnReopen() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        assertTrue(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.CHOICE, viewModel.uiState.value.currentMode)

        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Luna Lovegood"))
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        assertEquals("Luna Lovegood", viewModel.uiState.value.newContactName)

        // Dismissing must keep FLOW_A_ADD_CONTACT and input fields intact during exit animation
        viewModel.onEvent(QuickActionUiEvent.OnDismiss)
        assertFalse(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        assertEquals("Luna Lovegood", viewModel.uiState.value.newContactName)

        // Opening a new flow resets draft state cleanly
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertTrue(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        assertEquals("", viewModel.uiState.value.newContactName)
    }

    @Test
    fun onQuickSaveFlowA_preservesActiveModeAndDraftDuringExit_andResetsOnReopen() {
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Harry Potter"))
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("+1234567890"))

        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        val stateAfterSave = viewModel.uiState.value
        assertFalse(stateAfterSave.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, stateAfterSave.currentMode)
        assertEquals("Harry Potter", stateAfterSave.newContactName)
        assertEquals("+1234567890", stateAfterSave.phone)
        assertNotNull(stateAfterSave.userMessage)

        // Starting another capture session resets previous draft inputs cleanly
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        val stateAfterReopen = viewModel.uiState.value
        assertTrue(stateAfterReopen.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, stateAfterReopen.currentMode)
        assertEquals("", stateAfterReopen.newContactName)
        assertEquals("", stateAfterReopen.phone)
    }

    @Test
    fun onSaveFlowBEncounter_preservesActiveModeAndDraftDuringExit_andResetsOnReopen() {
        viewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
        val sample = viewModel.uiState.value.allContacts.first()
        viewModel.onEvent(QuickActionUiEvent.OnSelectExistingContact(sample))
        viewModel.onEvent(QuickActionUiEvent.OnEncounterNoteChange("Catch up over tea"))

        viewModel.onEvent(QuickActionUiEvent.OnSaveFlowBEncounter)

        val stateAfterSave = viewModel.uiState.value
        assertFalse(stateAfterSave.isOpen)
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, stateAfterSave.currentMode)
        assertEquals("Catch up over tea", stateAfterSave.encounterNoteFlowB)
        assertEquals(sample.id, stateAfterSave.selectedContact?.id)
        assertNotNull(stateAfterSave.userMessage)

        // Starting another Flow B session resets draft state cleanly
        viewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
        val stateAfterReopen = viewModel.uiState.value
        assertTrue(stateAfterReopen.isOpen)
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, stateAfterReopen.currentMode)
        assertNull(stateAfterReopen.selectedContact)
        assertEquals("", stateAfterReopen.encounterNoteFlowB)
    }

    @Test
    fun flowA_stepProgression_movesThroughAllSixStepsAndBack() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.NAME, viewModel.uiState.value.currentComponent?.type)

        // Step 0: Name -> Step 1: Role
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Cedric Diggory"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.ROLE, viewModel.uiState.value.currentComponent?.type)

        // Step 1: Role -> Step 2: Phone
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PHONE, viewModel.uiState.value.currentComponent?.type)

        // Step 2: Phone -> Step 3: Email
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(3, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.EMAIL, viewModel.uiState.value.currentComponent?.type)

        // Step 3: Email -> Step 4: Place
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PLACE, viewModel.uiState.value.currentComponent?.type)

        // Step 4: Place -> Step 5: Note
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(5, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.NOTE, viewModel.uiState.value.currentComponent?.type)

        // Navigate back to Place
        viewModel.onEvent(QuickActionUiEvent.OnPreviousStep)
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PLACE, viewModel.uiState.value.currentComponent?.type)
    }

    @Test
    fun flowA_selectingPlaceChip_autoAdvancesToNextStep() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Cho Chang"))
        // Navigate to Place step (step index 4 in Default preset: Name -> Role -> Phone -> Email -> Place -> Note)
        viewModel.onEvent(QuickActionUiEvent.OnStepIndexChange(4))
        assertEquals(4, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PLACE, viewModel.uiState.value.currentComponent?.type)

        val place = viewModel.uiState.value.suggestedPlaces.first()
        viewModel.onEvent(QuickActionUiEvent.OnSelectPlaceChip(place.id))

        // Auto-advances to step 5 (NOTE in Default preset)
        assertEquals(place.id, viewModel.uiState.value.selectedPlaceId)
        assertEquals(5, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.NOTE, viewModel.uiState.value.currentComponent?.type)
    }

    @Test
    fun flowA_emptyNameSave_blocksSaveAndFocusesNameStep() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        // Jump to Step 2 (Phone) with blank name
        viewModel.onEvent(QuickActionUiEvent.OnStepIndexChange(2))
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("+1 555 000 1111"))

        // Hit Quick Save while Name is empty
        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        val state = viewModel.uiState.value
        // Must remain open, flag error, and jump back to NAME step (index 0)
        assertTrue("Flow card must remain open when name is blank", state.isOpen)
        assertTrue("Name error must be flagged", state.nameError)
        assertEquals("Must focus Name step (index 0)", 0, state.currentStepIndex)
        assertEquals(CaptureComponentType.NAME, state.currentComponent?.type)
    }

    @Test
    fun flowA_escapeHatchSaveAtStep1_commitsContactAndEncounterInTwoSeconds() {
        val initialCount = repository.contacts.value.size

        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Neville Longbottom"))

        // Trigger Escape Hatch Save at Step 1
        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        val state = viewModel.uiState.value
        assertFalse(state.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, state.currentMode)
        assertNotNull(state.userMessage)
        assertTrue(state.userMessage!!.contains("Neville Longbottom"))
        assertEquals(initialCount + 1, repository.contacts.value.size)
        assertEquals("Neville Longbottom", repository.recentContacts.value.first().name)
    }

    @Test
    fun flowA_fullStepCompletion_savesAllAttributesAndFollowUp() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)

        // Step 0: Name
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Ginny Weasley"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 1: Role
        viewModel.onEvent(QuickActionUiEvent.OnRoleChange("Senior Quidditch Editor"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 2: Phone
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("+44 20 7946 0958"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 3: Email
        viewModel.onEvent(QuickActionUiEvent.OnEmailChange("ginny@dailyprophet.co.uk"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)

        // Step 4: Place chip (auto-advances to Step 5)
        val placeId = viewModel.uiState.value.suggestedPlaces.first().id
        viewModel.onEvent(QuickActionUiEvent.OnSelectPlaceChip(placeId))
        assertEquals(5, viewModel.uiState.value.currentStepIndex)

        // Step 5: Note & FollowUp
        viewModel.onEvent(QuickActionUiEvent.OnNoteChange("Sports journalist at Daily Prophet"))
        viewModel.onEvent(QuickActionUiEvent.OnSelectFollowUp(7))

        // Extended fields: Company, Birthday & Custom Properties
        viewModel.onEvent(QuickActionUiEvent.OnCompanyChange("Daily Prophet"))
        val bday = LocalDate.of(1981, 8, 11)
        viewModel.onEvent(QuickActionUiEvent.OnBirthdayChange(bday))
        viewModel.onEvent(QuickActionUiEvent.OnCustomPropertyKeyChange("Quidditch Team"))
        viewModel.onEvent(QuickActionUiEvent.OnCustomPropertyValueChange("Holyhead Harpies"))
        viewModel.onEvent(QuickActionUiEvent.OnAddCustomProperty)

        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        assertFalse(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        val saved = repository.recentContacts.value.first()
        assertEquals("Ginny Weasley", saved.name)
        assertTrue(saved.contextSubtitle.contains("Sports journalist at Daily Prophet"))
        assertTrue(repository.followUps.value.any { it.contactId == saved.id })

        val fullContact = repository.fullContacts.value[saved.id]
        assertNotNull(fullContact)
        assertEquals("Senior Quidditch Editor", fullContact!!.role)
        assertEquals("+442079460958", fullContact.phone)
        assertEquals("ginny@dailyprophet.co.uk", fullContact.email)
        assertEquals("Daily Prophet", fullContact.company)
        assertEquals(bday, fullContact.birthday)
        assertEquals("Holyhead Harpies", fullContact.customProperties["Quidditch Team"])
    }

    @Test
    fun flowB_searchAndSelectContact_morphsToEncounterSheetAndSaves() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, viewModel.uiState.value.currentMode)
        assertNull(viewModel.uiState.value.selectedContact)

        // Instant search
        viewModel.onEvent(QuickActionUiEvent.OnSearchQueryChange("Maya"))
        assertEquals(1, viewModel.uiState.value.filteredContacts.size)
        val maya = viewModel.uiState.value.filteredContacts.first()
        assertEquals("Maya Patel", maya.name)

        // Select contact -> morphs into encounter details stage
        viewModel.onEvent(QuickActionUiEvent.OnSelectExistingContact(maya))
        assertEquals(maya.id, viewModel.uiState.value.selectedContact?.id)

        // Set When, Where, Note
        val yesterday = LocalDate.now().minusDays(1)
        viewModel.onEvent(QuickActionUiEvent.OnEncounterDateChange(yesterday))
        val place = viewModel.uiState.value.suggestedPlaces.first()
        viewModel.onEvent(QuickActionUiEvent.OnEncounterPlaceChange(place.id))
        viewModel.onEvent(QuickActionUiEvent.OnEncounterNoteChange("Caught up about her new job at Figma"))

        // Save
        viewModel.onEvent(QuickActionUiEvent.OnSaveFlowBEncounter)

        assertFalse(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, viewModel.uiState.value.currentMode)
        assertNotNull(viewModel.uiState.value.userMessage)
        assertTrue(viewModel.uiState.value.userMessage!!.contains("Maya Patel"))

        val updatedMaya = repository.recentContacts.value.first()
        assertEquals(maya.id, updatedMaya.id)
        assertEquals("Met yesterday", updatedMaya.lastEncounterText)
        assertTrue(updatedMaya.contextSubtitle.contains("Caught up about her new job at Figma"))
    }

    @Test
    fun locationExplainer_togglesGracefullyAndNeverDefaultsToSeededPlaceWhenGpsUnavailable() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertFalse(viewModel.uiState.value.showLocationExplainer)

        viewModel.onEvent(QuickActionUiEvent.OnRequestLocation)
        assertTrue(viewModel.uiState.value.showLocationExplainer)

        viewModel.onEvent(QuickActionUiEvent.OnDismissLocationExplainer)
        assertFalse(viewModel.uiState.value.showLocationExplainer)

        // When GPS service is unavailable, granting permission must NOT silently default to any place
        viewModel.onEvent(QuickActionUiEvent.OnRequestLocation)
        viewModel.onEvent(QuickActionUiEvent.OnLocationPermissionResult(granted = true))
        assertFalse(viewModel.uiState.value.showLocationExplainer)
        assertNull(viewModel.uiState.value.selectedPlaceId)
        assertTrue(viewModel.uiState.value.userMessage?.contains("GPS unavailable") == true)

        // Permission denied leaves place unselected without blocking capture
        viewModel.onEvent(QuickActionUiEvent.OnLocationPermissionResult(granted = false))
        assertFalse(viewModel.uiState.value.isLocationGranted)
        assertNull(viewModel.uiState.value.selectedPlaceId)

        // Blue Bottle Coffee must never appear in seeded places
        assertTrue(viewModel.uiState.value.suggestedPlaces.none { it.label.contains("Blue Bottle", ignoreCase = true) })
    }

    @Test
    fun gpsSuccess_withoutNearbyPlace_savesRealCoordinatesAndResolvedLabelOnEnter() {
        val fakeScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val fakeLocationService = object : com.example.hello.platform.location.LocationService(
            context = null,
            locationManager = null
        ) {
            override fun getLocationFlow(
                places: List<com.example.hello.domain.model.Place>,
                timeoutMillis: Long,
                radiusMeters: Double
            ) = kotlinx.coroutines.flow.flowOf(
                com.example.hello.platform.location.LocationState.Success(
                    latitude = 14.5995,
                    longitude = 120.9842,
                    accuracy = 12f,
                    nearbyPlaces = emptyList(),
                    resolvedLabel = "Makati, Metro Manila"
                )
            )
        }
        val vmWithGps = QuickActionViewModel(
            repository = repository,
            locationService = fakeLocationService,
            externalScope = fakeScope
        )

        vmWithGps.onEvent(QuickActionUiEvent.OnSelectAddContact)
        vmWithGps.onEvent(QuickActionUiEvent.OnNameChange("Tessa Cruz"))
        vmWithGps.onEvent(QuickActionUiEvent.OnLocationPermissionResult(granted = true))

        assertNull(vmWithGps.uiState.value.selectedPlaceId)
        assertEquals(14.5995, vmWithGps.uiState.value.capturedLatitude!!, 0.0001)
        assertEquals(120.9842, vmWithGps.uiState.value.capturedLongitude!!, 0.0001)
        assertEquals("Makati, Metro Manila", vmWithGps.uiState.value.customPlaceName)

        // Pressing Enter / saving persists exact coordinates and resolved location
        vmWithGps.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)
        val savedEncounter = repository.encounters.value.first()
        assertEquals("Makati, Metro Manila", savedEncounter.eventLabel)
        assertEquals(14.5995, savedEncounter.latitude!!, 0.0001)
        assertEquals(120.9842, savedEncounter.longitude!!, 0.0001)
    }

    @Test
    fun flowB_longPressMultiSelect_logsSingleEncounterForMultipleParticipants() {
        viewModel.onEvent(QuickActionUiEvent.OnOpenChoice)
        viewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)

        val contacts = viewModel.uiState.value.allContacts
        assertTrue(contacts.size >= 2)
        val first = contacts[0]
        val second = contacts[1]

        // Long-press first contact enters multi-select mode in Stage 1
        viewModel.onEvent(QuickActionUiEvent.OnLongPressContactFlowB(first))
        assertTrue(viewModel.uiState.value.isFlowBMultiSelectMode)
        assertNull(viewModel.uiState.value.selectedContact)
        assertEquals(listOf(first.id), viewModel.uiState.value.selectedContacts.map { it.id })

        // Tap second contact toggles it in
        viewModel.onEvent(QuickActionUiEvent.OnToggleContactFlowB(second))
        assertEquals(listOf(first.id, second.id), viewModel.uiState.value.selectedContacts.map { it.id })

        // Confirm selection advances to Stage 2
        viewModel.onEvent(QuickActionUiEvent.OnConfirmMultiSelectContactsFlowB)
        assertFalse(viewModel.uiState.value.isFlowBMultiSelectMode)
        assertEquals(first.id, viewModel.uiState.value.selectedContact?.id)

        val initialCount = repository.encounters.value.size
        viewModel.onEvent(QuickActionUiEvent.OnEncounterNoteChange("Group dinner"))
        viewModel.onEvent(QuickActionUiEvent.OnSaveFlowBEncounter)

        assertFalse(viewModel.uiState.value.isOpen)
        assertEquals(initialCount + 1, repository.encounters.value.size)
        val saved = repository.encounters.value.first()
        assertEquals(listOf(first.id, second.id), saved.resolvedParticipantIds())
        assertTrue(viewModel.uiState.value.userMessage!!.contains(first.name))
        assertTrue(viewModel.uiState.value.userMessage!!.contains(second.name))
    }

    @Test
    fun pinnedTagChipStrip_togglesTagsAndCustomTagInput() {
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertFalse(viewModel.uiState.value.isCustomTagInputExpanded)

        // Expand "+ tag" inline custom tag input, add a custom tag, and verify it auto-collapses and selects
        viewModel.onEvent(QuickActionUiEvent.OnToggleCustomTagInput)
        assertTrue(viewModel.uiState.value.isCustomTagInputExpanded)
        viewModel.onEvent(QuickActionUiEvent.OnCustomTagInputChange("#hackathon"))
        viewModel.onEvent(QuickActionUiEvent.OnAddCustomTag)
        assertFalse(viewModel.uiState.value.isCustomTagInputExpanded)
        assertEquals("", viewModel.uiState.value.customTagInput)
        val addedTag = viewModel.uiState.value.suggestedTags.first { it.name.equals("hackathon", ignoreCase = true) }
        assertTrue(addedTag.id in viewModel.uiState.value.selectedTagIds)

        // Save in 1 step from step 0 with Name + Role + Phone + custom tag
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Jordan Lee"))
        viewModel.onEvent(QuickActionUiEvent.OnRoleChange("Staff Designer"))
        viewModel.onEvent(QuickActionUiEvent.OnCompanyChange("Linear"))
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("+1 555 999 8888"))

        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)

        assertFalse(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        val saved = repository.contacts.value.first()
        assertEquals("Jordan Lee", saved.name)
        assertTrue(saved.tags.any { it.id == addedTag.id })
        assertTrue(saved.contextSubtitle.contains("Staff Designer at Linear"))
        assertEquals("+15559998888", repository.getContact(saved.id)?.phone)
    }

    @Test
    fun flowA_messyPhoneInput_sanitizesOnChangeAndNormalizesOrDropsPunctuationOnSave() {
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Luna Phone"))

        val messy = "\u200E\uFF0B\uFF14\uFF14\u00A0\uFF12\uFF10\u2013\uFF17\uFF19\uFF14\uFF16\u2014\uFF10\uFF19\uFF15\uFF18 abc"
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange(messy))
        assertEquals("+44 20-7946-0958 ", viewModel.uiState.value.phone)

        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)
        val savedId = repository.recentContacts.value.first().id
        assertEquals("+442079460958", repository.getContact(savedId)?.phone)

        // Too-short phone blocks Next on Phone step and clears when edited
        viewModel.onEvent(QuickActionUiEvent.OnSelectAddContact)
        viewModel.onEvent(QuickActionUiEvent.OnNameChange("Short Phone"))
        viewModel.onEvent(QuickActionUiEvent.OnStepIndexChange(2)) // Phone step
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("12345"))
        viewModel.onEvent(QuickActionUiEvent.OnNextStep)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        assertEquals("Phone number is too short (at least 7 digits)", viewModel.uiState.value.phoneError)

        // Symbol-only phone blocks Quick Save from another step and jumps back to Phone step
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange("+--- (.) "))
        assertNull(viewModel.uiState.value.phoneError)
        viewModel.onEvent(QuickActionUiEvent.OnStepIndexChange(5)) // Note step
        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)
        assertTrue(viewModel.uiState.value.isOpen)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)
        assertEquals(CaptureComponentType.PHONE, viewModel.uiState.value.currentComponent?.type)
        assertEquals("Enter a valid phone number", viewModel.uiState.value.phoneError)

        // Clearing phone allows save and persists null
        viewModel.onEvent(QuickActionUiEvent.OnPhoneChange(""))
        assertNull(viewModel.uiState.value.phoneError)
        viewModel.onEvent(QuickActionUiEvent.OnQuickSaveFlowA)
        assertFalse(viewModel.uiState.value.isOpen)
        val blankSavedId = repository.recentContacts.value.first().id
        assertNull(repository.getContact(blankSavedId)?.phone)
    }

    @Test
    fun flowB_defaultsToCurrentTime_allowsEditing_andPersistsForSingleAndMultiPerson() {
        var fakeNow = LocalTime.of(14, 35)
        val timedVm = QuickActionViewModel(
            repository = repository,
            nowTimeProvider = { fakeNow }
        )

        val contacts = timedVm.uiState.value.allContacts
        val alice = contacts[0]
        val bob = contacts[1]

        // Opening Flow B captures current time and marks it uncustomized
        timedVm.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
        assertEquals(LocalTime.of(14, 35), timedVm.uiState.value.encounterTime)
        assertFalse(timedVm.uiState.value.isEncounterTimeCustomized)

        // Single-person save uses the captured default time
        timedVm.onEvent(QuickActionUiEvent.OnSelectExistingContact(alice))
        val targetDate = LocalDate.of(2026, 5, 10)
        timedVm.onEvent(QuickActionUiEvent.OnEncounterDateChange(targetDate))
        assertEquals(LocalTime.of(14, 35), timedVm.uiState.value.encounterTime)
        timedVm.onEvent(QuickActionUiEvent.OnSaveFlowBEncounter)

        val savedSingle = repository.encounters.value.first()
        val expectedSingleInstant = targetDate.atTime(14, 35).atZone(ZoneId.systemDefault()).toInstant()
        assertEquals(expectedSingleInstant, savedSingle.occurredAt)

        // Reopening refreshes default time; editing time persists for multi-person catch-up
        fakeNow = LocalTime.of(19, 10)
        timedVm.onEvent(QuickActionUiEvent.OnOpenLogEncounterForContact(alice))
        assertEquals(LocalTime.of(19, 10), timedVm.uiState.value.encounterTime)
        assertFalse(timedVm.uiState.value.isEncounterTimeCustomized)

        timedVm.onEvent(QuickActionUiEvent.OnSetFlowBParticipants(listOf(alice, bob)))
        timedVm.onEvent(QuickActionUiEvent.OnEncounterDateChange(targetDate.minusDays(1)))
        timedVm.onEvent(QuickActionUiEvent.OnEncounterTimeChange(LocalTime.of(21, 5)))
        assertTrue(timedVm.uiState.value.isEncounterTimeCustomized)
        assertEquals(LocalTime.of(21, 5), timedVm.uiState.value.encounterTime)

        timedVm.onEvent(QuickActionUiEvent.OnSaveFlowBEncounter)
        val savedMulti = repository.encounters.value.first()
        val expectedMultiInstant = targetDate.minusDays(1).atTime(21, 5).atZone(ZoneId.systemDefault()).toInstant()
        assertEquals(expectedMultiInstant, savedMulti.occurredAt)
        assertEquals(listOf(alice.id, bob.id), savedMulti.resolvedParticipantIds())
    }

    @Test
    fun singleSurfaceComposer_pinnedDefaults_trayPopoverMutuallyExclusive_andCustomizePinning() {
        val syncVm = QuickActionViewModel(
            repository = repository,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        )
        syncVm.onEvent(QuickActionUiEvent.OnSelectAddContact)
        assertEquals(QuickCaptureDetailKey.DEFAULT_ADD_CONTACT, syncVm.uiState.value.pinnedDetailKeys)
        assertFalse(syncVm.uiState.value.isNoteExpanded)

        // Toggling '+' popover, Customize, and inline detail tray are mutually exclusive
        syncVm.onEvent(QuickActionUiEvent.OnTogglePlusPopover(true))
        assertTrue(syncVm.uiState.value.isPlusPopoverOpen)
        syncVm.onEvent(QuickActionUiEvent.OnToggleDetailTray(QuickCaptureDetailKey.OCCUPATION))
        assertFalse(syncVm.uiState.value.isPlusPopoverOpen)
        assertEquals(QuickCaptureDetailKey.OCCUPATION, syncVm.uiState.value.activeDetailTray)

        // Populating and clearing Occupation resets both role and company
        syncVm.onEvent(QuickActionUiEvent.OnRoleChange("Designer"))
        syncVm.onEvent(QuickActionUiEvent.OnCompanyChange("Acme"))
        syncVm.onEvent(QuickActionUiEvent.OnClearDetailKey(QuickCaptureDetailKey.OCCUPATION))
        assertEquals("", syncVm.uiState.value.role)
        assertEquals("", syncVm.uiState.value.company)

        // Pinning/unpinning detail keys updates pinnedDetailKeys immediately
        syncVm.onEvent(QuickActionUiEvent.OnToggleCustomizeQuickAdd(true))
        assertTrue(syncVm.uiState.value.isCustomizeQuickAddOpen)
        assertNull(syncVm.uiState.value.activeDetailTray)
        syncVm.onEvent(QuickActionUiEvent.OnTogglePinDetailKey(QuickCaptureDetailKey.BIRTHDAY))
        assertTrue(QuickCaptureDetailKey.BIRTHDAY in syncVm.uiState.value.pinnedDetailKeys)
        syncVm.onEvent(QuickActionUiEvent.OnTogglePinDetailKey(QuickCaptureDetailKey.OCCUPATION))
        assertFalse(QuickCaptureDetailKey.OCCUPATION in syncVm.uiState.value.pinnedDetailKeys)

        // Switching to Met Again keeps Notes collapsed until Enter and uses Met Again default pinned buttons
        syncVm.onEvent(QuickActionUiEvent.OnSwitchComposerMode)
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, syncVm.uiState.value.currentMode)
        assertFalse(syncVm.uiState.value.isNoteExpanded)
        assertEquals(QuickCaptureDetailKey.DEFAULT_MET_AGAIN, syncVm.uiState.value.pinnedDetailKeys)
    }

    @Test
    fun flowB_inlineChips_commaCommit_nicknameAutofill_enterExpandsNote_andUnmatchedConvertToAddContact() {
        viewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
        assertFalse(viewModel.uiState.value.isNoteExpanded)
        assertTrue(viewModel.uiState.value.selectedContacts.isEmpty())

        // Match by nickname ("S-Lin" -> Sarah Lin) and commit via comma
        val sarah = viewModel.uiState.value.allContacts.first { it.nicknames.any { n -> n.equals("S-Lin", ignoreCase = true) } }
        viewModel.onEvent(QuickActionUiEvent.OnSearchQueryChange("S-L"))
        assertEquals(sarah.id, viewModel.uiState.value.filteredContacts.first().id)

        viewModel.onEvent(QuickActionUiEvent.OnSearchQueryChange("S-L,"))
        assertEquals(listOf(sarah.id), viewModel.uiState.value.selectedContacts.map { it.id })
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertFalse(viewModel.uiState.value.isNoteExpanded)

        // Type second person and press Enter -> commits second person AND expands Notes
        val second = viewModel.uiState.value.allContacts.first { it.id != sarah.id }
        viewModel.onEvent(QuickActionUiEvent.OnSearchQueryChange(second.name.take(3)))
        viewModel.onEvent(QuickActionUiEvent.OnFlowBEnterPressed)
        assertEquals(listOf(sarah.id, second.id), viewModel.uiState.value.selectedContacts.map { it.id })
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.isNoteExpanded)

        // Removing all chips works cleanly
        viewModel.onEvent(QuickActionUiEvent.OnRemoveSelectedContactFlowB(sarah.id))
        viewModel.onEvent(QuickActionUiEvent.OnRemoveSelectedContactFlowB(second.id))
        assertTrue(viewModel.uiState.value.selectedContacts.isEmpty())

        // Unmatched query converts cleanly into Add Contact with the typed name preserved
        viewModel.onEvent(QuickActionUiEvent.OnSearchQueryChange("Brand New Person,"))
        assertEquals("Brand New Person", viewModel.uiState.value.searchQuery)
        viewModel.onEvent(QuickActionUiEvent.OnConvertQueryToAddContact("Brand New Person"))
        assertEquals(QuickActionMode.FLOW_A_ADD_CONTACT, viewModel.uiState.value.currentMode)
        assertEquals("Brand New Person", viewModel.uiState.value.newContactName)
    }

    @Test
    fun onBackToChoice_dismissesComposerDirectlyWithoutOpeningChoiceCard() {
        viewModel.onEvent(QuickActionUiEvent.OnSelectLogEncounter)
        assertTrue(viewModel.uiState.value.isOpen)
        assertEquals(QuickActionMode.FLOW_B_LOG_ENCOUNTER, viewModel.uiState.value.currentMode)

        viewModel.onEvent(QuickActionUiEvent.OnBackToChoice)
        assertFalse("Back/cancel must dismiss the composer directly instead of showing Quick Capture choice", viewModel.uiState.value.isOpen)
        assertTrue(viewModel.uiState.value.selectedContacts.isEmpty())
    }
}
