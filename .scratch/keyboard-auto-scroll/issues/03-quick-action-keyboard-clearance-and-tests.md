# 03: Quick Action Sheet Keyboard Clearance & End-to-End Verification

**What to build:**
Extend the keyboard auto-clearance and scroll-into-view behavior to Quick Action sheets (`FlowAAddContactCard`, `FlowBLogEncounterCard`, custom tag/property inputs) and write automated unit/Compose tests that verify the scroll state reactively adjusts when IME insets and text focus change.

**Blocked by:** 02 (BringIntoView Auto-Scroll on Input Field Focus)

**Status:** done

- [x] Check `FlowAAddContactCard` and `FlowBLogEncounterCard` modal sheets to verify that custom property and note text fields auto-scroll above the keyboard when focused.
- [x] Add unit/Compose tests validating that focusing a field inside a scrollable container requests bring-into-view and scrolls the parent state.
- [x] Verify cold-start (<50ms) performance is maintained with Baseline Profile integrity.

