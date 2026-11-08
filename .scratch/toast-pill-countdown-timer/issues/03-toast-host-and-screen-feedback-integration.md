# 03: Toast Host & Screen Feedback Integration with Undo Recovery

**What to build:** Wire the extended duration and countdown-capable Toast Pill to the application's global snackbar collector and screen scaffolds. Ensure that when a user deletes a contact, deletes an encounter, or archives contacts, the Toast Pill remains visible for the full 7-second countdown window with the bottom progress bar active, and that tapping "Undo" at any point during those 7 seconds restores the data in SQLite and surfaces a subsequent confirmation notice without a countdown bar.

**Blocked by:** 02: Animated Slim Countdown Progress Bar Component in Toast Pill

**Status:** closed

- [x] Global feedback collector in `MainAppShell` passes the 7000ms duration for actionable delete/archive messages.
- [x] Sub-screen scaffolds (`PersonDetailScreen`, etc.) pass the 7000ms duration for encounter deletions.
- [x] Deleting a contact or encounter displays the Toast Pill with the active countdown progress bar for 7 seconds.
- [x] Tapping "Undo" at 5–6 seconds into the countdown successfully restores the contact/encounter in SQLite.
- [x] Restoration confirmation toast ("Contact restored" / "Encounter restored") displays as a standard brief toast without a countdown bar.
- [x] Non-actionable toasts continue to display for standard brief durations (3–4s).
- [x] Automated regression tests verify end-to-end event handling and undo restoration contract.

