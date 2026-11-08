# 03: Destructive Action Haptics, Restore & Full Test Suite

**What to build:**
Deliver strong, deliberate warning haptic feedback for high-consequence operations (archiving contacts, deleting contacts, and deleting encounters), provide affirmative haptics on restoring contacts and tapping toast undo, and run the full automated test suite to ensure 100% pass rate.

**Blocked by:** 02 (Edit Entry and Completion Success Haptics Wiring)

**Status:** closed

- [x] Connect `DESTRUCTIVE_ACTION` heavy warning pulse upon confirming contact archive actions.
- [x] Connect `DESTRUCTIVE_ACTION` heavy warning pulse upon confirming single or batch contact deletion in alert dialogs.
- [x] Connect `DESTRUCTIVE_ACTION` heavy warning pulse upon confirming encounter deletion.
- [x] Connect `SUCCESS_CONFIRMATION` feedback when tapping "Restore" on an archived contact and when tapping "Undo" on a toast pill.
- [x] Run full project test suite (`./gradlew testDebugUnitTest`) and verify 100% pass rate.

