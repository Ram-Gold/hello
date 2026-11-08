# 01: Core Semantic Haptics Dispatcher and Navigation / Mode Toggle Wiring

**What to build:**
Create the centralized `HelloHaptics` interface and default implementation mapping high-level semantic user interactions to physical tactile feedback patterns (`THRESHOLD_ACTIVATION`, `MODE_TOGGLE`, `SUCCESS_CONFIRMATION`, `EDIT_ENTRY`, `DESTRUCTIVE_ACTION`). Wire `THRESHOLD_ACTIVATION` into pulling down past the threshold on the contacts directory to access archived contacts, and wire `MODE_TOGGLE` into switching between logging new encounters and updating existing timeline memories in Quick Action sheets.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Create `HelloHaptics` interface and default Compose provider defining semantic interaction feedback types.
- [ ] Connect `THRESHOLD_ACTIVATION` haptic feedback to the overscroll pull-to-action threshold breach and trigger when accessing Archived Contacts.
- [ ] Connect `MODE_TOGGLE` tactile tick feedback when switching between logging a new encounter and updating timeline memories in Quick Action sheets.
- [ ] Write unit tests for `HelloHaptics` verifying semantic event mapping and fallback execution.

