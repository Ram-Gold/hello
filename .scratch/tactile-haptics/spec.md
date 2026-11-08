# Spec: Semantic Tactile Haptic Feedback

## Problem Statement

Interactions across Hello—such as overscroll pull-to-action gestures, switching between logging and updating encounters, saving new relationships or edits, entering edit modes, and performing irreversible actions like archiving or deleting—currently lack immediate, tactile physical feedback. While visual animations communicate transitions, the user's fingers receive little to no physical confirmation when crossing thresholds or committing state changes. This leaves high-consequence operations (like deletion or archiving) feeling lightweight and reduces the tactile delight of the application's physical motion model.

## Solution

Introduce a unified, semantic tactile haptic feedback architecture (`HelloHaptics`) across key physical interactions:
1. **Archive Navigation Trigger:** Physical haptic click when crossing the pull-to-action activation threshold at the top of the Relationship directory.
2. **Log <-> Update Mode Transitions:** Tactile tick feedback when toggling between logging a new encounter and updating existing timeline memories.
3. **Completion of Entries & Updates:** Affirmative success feedback upon completing contact creation, encounter logging, or profile updates.
4. **Entering Edit State:** Distinct transition tap when opening edit views or activating selection mode.
5. **Archiving & Deleting:** Strong, deliberate warning haptic pulse when executing archive operations or confirming deletions.

---

## User Stories

1. As a relationship keeper, I want tactile haptic feedback when pulling down the Relationship directory past the activation threshold, so that I physically feel the exact instant the archive navigation is ready to trigger.
2. As a relationship keeper, I want tactile feedback when releasing a pull-to-action gesture that exceeds the threshold, so that I know the screen transition to Archived Contacts has been initiated.
3. As a user logging an encounter, I want tactile tick feedback when switching between logging a new encounter and updating an existing timeline memory, so that I am confident in which workflow mode is active.
4. As a user adding a new contact, I want affirmative completion haptics upon successfully saving their card, so that I feel immediate physical satisfaction and confidence that the relationship data is persisted.
5. As a user logging a meeting, I want affirmative haptic feedback upon tapping "Save Encounter", so that I physically register that the memory is recorded in the timeline.
6. As a user updating contact details, I want completion haptic feedback upon saving changes in the edit screen, so that I know my edits have been saved to the database.
7. As a relationship keeper reviewing a contact's profile, I want a crisp tactile tap when clicking "Edit", so that the transition into edit mode feels deliberate and responsive.
8. As a user in the contacts directory, I want a distinct haptic pulse when long-pressing a contact to enter multi-selection edit mode, so that I know selection mode is active without relying solely on screen visuals.
9. As a user selecting individual contacts in selection mode, I want subtle tactile clicks on item selection toggles, so that multi-selection feels tactile and precise.
10. As a user archiving an inactive contact, I want a firm haptic pulse when confirming the archive action, so that the gravity of moving someone out of the active directory is physically felt.
11. As a user deleting an encounter or a contact, I want a strong warning haptic pulse upon confirming deletion in the alert dialog, so that irreversible data destruction carries physical weight.
12. As a user restoring an archived contact, I want an affirmative haptic confirmation upon tapping "Restore", so that I feel the relationship returning to the active directory.
13. As a user tapping "Undo" on a toast pill, I want a reassuring tactile pulse confirming that the deletion or archive operation was reversed.
14. As an accessibility-conscious user who prefers reduced device vibration, I want all haptic feedback to automatically respect system-level haptic settings and app preferences, so that vibration is only delivered when desired.

---

## Implementation Decisions

### 1. Unified Semantic Haptic Dispatcher
- Establish a centralized haptic dispatcher interface (`HelloHaptics`) defining semantic interaction types rather than raw low-level vibration durations:
  - `ThresholdActivation`: Light to medium crisp feedback for overscroll and gesture threshold crossing.
  - `ModeToggle`: Subtle selector click for toggling between Log and Update workflows.
  - `SuccessConfirmation`: Affirmative double-pulse or solid confirmation click when completing input, creation, or update commits.
  - `EditEntry`: Sharp, tactile transition tap when entering edit screens or selection mode.
  - `DestructiveAction`: Heavy, deliberate warning pulse when confirming archive or delete operations.
- Map semantic types to Compose `LocalHapticFeedback` (`HapticFeedbackType.LongPress`, `HapticFeedbackType.TextHandleMove`) or Android platform `VibrationEffect` where enhanced pre-baked effects (e.g. `EFFECT_CLICK`, `EFFECT_HEAVY_CLICK`, `EFFECT_DOUBLE_CLICK`) are supported on API 29+.

### 2. Interaction Points & Wiring
- **Archive Navigation:** Integrated into `PullToActionState` / `PullToActionArchiveHeader` to fire on threshold breach and gesture trigger.
- **Log <-> Update Mode Switch:** Integrated into `FlowBLogEncounterSheet` and Quick Action sheets when switching stages or encounter modes.
- **Entry & Update Completion:** Wired into the completion callbacks of `PersonEditViewModel`, `QuickActionViewModel`, and `ContactsViewModel` on successful persistence.
- **Entering Edit State:** Wired into the `onEditClick` button in `PersonDetailScreen`, card long-press handlers in `ContactsScreen`, and note edit triggers.
- **Archiving & Deleting:** Wired into confirmation dialog confirm buttons in `PersonEditScreen`, `ContactsScreen` batch deletion, and `ArchivedContactsScreen` restore flows.

### 3. Graceful Fallback & Zero Noise
- Operations degrade gracefully on devices without vibration actuators or when system haptics are disabled.
- Unit tests remain fully decoupled from Android hardware via the dispatcher abstraction.

---

## Testing Decisions

### What Makes a Good Test
- Tests must verify behavior through public boundaries (the `HelloHaptics` semantic dispatcher), asserting that appropriate semantic events are emitted when specific user actions occur, rather than asserting on Android OS hardware drivers.

### Modules Tested
- **Dispatcher Unit Tests:** Verifies semantic pattern mapping and fallback behavior.
- **Workflow Seam Tests:**
  - Pull-to-action threshold event emission.
  - Quick action log vs update toggle emission.
  - Contact/encounter creation and update completion emission.
  - Archive and deletion confirmation emission.

### Prior Art
- Existing `PullToActionStateTest` and `ContactsViewModelTest` which validate gesture math and state transitions deterministically without hardware dependencies.

---

## Out of Scope

- Custom hardware waveform synthesizers or third-party haptic engine libraries.
- Continuous vibration effects or game-style audio-haptic synthesizers.
- Custom haptic intensity tuning sliders (system haptic intensity settings are respected automatically).

---

## Further Notes

- Haptic feedback will follow Google's Material Design 3 guidelines for tactile feedback, ensuring subtlety and avoiding cognitive or sensory fatigue during rapid interactions.

