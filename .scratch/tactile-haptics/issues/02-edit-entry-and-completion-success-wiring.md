# 02: Edit Entry and Completion Success Haptics Wiring

**What to build:**
Connect tactile feedback when transitioning into edit states and upon successfully completing entries or updates. Users receive a crisp transition tap when entering the edit screen (`PersonDetailScreen` -> `PersonEditScreen`) or entering multi-selection mode in the contacts directory, and an affirmative success confirmation pulse upon saving new contacts (Flow A), logging encounters (Flow B), or saving profile edits.

**Blocked by:** 01 (Core Semantic Haptics Dispatcher and Navigation / Mode Toggle Wiring)

**Status:** ready-for-agent

- [ ] Connect `EDIT_ENTRY` tactile tap when clicking "Edit" on a contact profile and when long-pressing a contact to enter multi-selection mode.
- [ ] Connect `SUCCESS_CONFIRMATION` affirmative haptic feedback upon saving a new contact in Flow A.
- [ ] Connect `SUCCESS_CONFIRMATION` affirmative haptic feedback upon tapping "Save Encounter" in Flow B.
- [ ] Connect `SUCCESS_CONFIRMATION` affirmative haptic feedback upon successfully saving profile updates in `PersonEditScreen`.

