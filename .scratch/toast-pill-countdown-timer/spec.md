# Feature Specification: Toast Pill Countdown Progress Bar and Extended Undo Window

Status: ready-for-agent  
Date: 2026-11-08  
Topic: UI / Transient Feedback / Toast Pill / Data Safety  

---

## Problem Statement

When deleting or archiving contacts, encounters, or other critical relationship records in Hello, users are presented with a transient Toast Pill containing an "Undo" action. However, the default short duration (~4 seconds) expires too quickly. Users often do not have enough time to recognize that a mistake was made, orient their hand, and tap "Undo" before the pill vanishes and the deletion or archive operation becomes permanent or harder to recover.

Furthermore, the existing Toast Pill provides no visual feedback regarding how much time remains before the action expires. Users experience uncertainty and anxiety because the pill disappears abruptly without warning, leading to lost data or unintended modifications when an action is missed.

---

## Solution

1. **Extended Undo Duration (7 Seconds):**
   - For all destructive and state-altering operations that offer an "Undo" action (contact deletion, encounter deletion, contact archiving, mass archiving, and contact restoration), extend the toast visibility window from the standard 4 seconds to an intentional 7 seconds.
   - Standard informational and non-actionable confirmations (e.g., "Contact updated", "Marked as done!") remain brief (3–4 seconds) to avoid lingering visual noise.

2. **Slim Animated Bottom Countdown Progress Bar:**
   - Integrate a sleek, 2–3dp progress track along the entire bottom edge of the Toast Pill capsule.
   - When an actionable Toast Pill is displayed, the progress bar smoothly animates from 100% width down to 0% width over the exact 7-second duration.
   - The progress line is seamlessly clipped to match the pill's rounded capsule shape (`RoundedCornerShape(24.dp)`).
   - The indicator uses dynamic semantic color mapping matching the action context:
     - Error / Delete: Warning/error-tinted progress track.
     - Archive: Secondary terracotta/warm amber-tinted progress track.
     - General Action: Primary terracotta accent.

3. **Smooth Interaction Lifecycle & Interruption Handling:**
   - If the user taps "Undo", the timer is immediately cancelled, the in-memory undo restoration is committed to SQLite, and the pill transitions to a confirmation state ("Contact restored" / "Encounter restored").
   - If the user navigates or dismisses the pill, the timer is safely halted without memory leaks or phantom coroutines.

---

## User Stories

1. As a user deleting an encounter from the living timeline, I want the confirmation Toast Pill to stay visible for 7 seconds, so that I have ample time to read the message and realize if I made an error.
2. As a user deleting a contact from the relationship directory, I want to clearly see a progress bar shrinking along the bottom edge of the Toast Pill, so that I know exactly how much time is left to tap "Undo".
3. As a user who accidentally mass-deleted or mass-archived contacts, I want the bottom progress bar to give me a predictable countdown, so that I can calmly move my thumb to the "Undo" button before it disappears.
4. As a user in a moving environment (e.g., walking or riding transit), I want a longer 7-second undo window, so that minor hand tremors or distractions do not prevent me from tapping "Undo" in time.
5. As a user viewing an undoable Toast Pill, I want the countdown progress bar to animate with smooth continuous motion (linear or gentle ease), so that the countdown feels natural and pleasant rather than jarring.
6. As a user deleting an item, I want the bottom progress bar to be tinted in harmony with the delete status (soft error/red tonal tint), so that the visual hierarchy matches the destructive nature of the action.
7. As a user archiving a contact, I want the bottom progress bar to be tinted in warm terracotta/secondary amber, so that it visually corresponds with archive semantics.
8. As a user receiving a non-destructive informational toast (e.g., "Contact updated" or "Tag created"), I want the toast to dismiss quickly (3–4 seconds) without a countdown bar, so that I am not distracted by unnecessary progress indicators for non-urgent notices.
9. As a user tapping "Undo" on an active Toast Pill, I want the countdown to stop instantly and the item to be restored in SQLite without waiting for the full 7 seconds to lapse, so that I receive instantaneous recovery feedback.
10. As a user who taps "Undo", I want the subsequent confirmation toast ("Contact restored") to appear without a countdown bar, so that I know the restoration was final and successful.
11. As a user using dark theme, I want the bottom countdown bar to have high tonal contrast against the dark elevated pill container, so that the countdown remains clearly legible in low-light environments.
12. As a user using light theme, I want the bottom countdown bar to blend smoothly with the warm earthy pill surface, so that it looks polished and integrated with Hello's design system.
13. As a user viewing a Toast Pill on a compact phone screen, I want the bottom progress bar to span the exact bottom width of the pill within its rounded boundaries, so that it never clips awkwardly or bleeds outside the pill corners.
14. As a user who dismisses the toast or performs an action that triggers a new message, I want the previous countdown to reset cleanly, so that subsequent toasts display their own accurate countdowns.
15. As an accessibility user using screen readers or talkback, I want the extended duration to provide sufficient time to announce both the message text and the action button without truncating speech output.
16. As a user operating Hello completely offline, I want the countdown timer to run entirely on the local device clock, so that timer calculations remain completely deterministic and zero-latency.

---

## Implementation Decisions

### 1. Presentation & Component Architecture
- **Pill Container Layout (`ToastPill` / `HelloToastPill`):**
  - Wrap the inner content of the Toast Pill in a vertical container or layered layout where the bottom edge hosts a horizontal `LinearProgressIndicator` or custom drawing canvas of height `2.5.dp`.
  - Apply `Modifier.clip(pillShape)` or embed the indicator directly within the rounded surface so the corners follow the 24dp squircle pill geometry.
- **Progress Track Styling:**
  - Track background: Subdued outline variant or container alpha (~20% opacity).
  - Progress fill: Semantic color derived from the toast's semantic type (`DELETE` -> error tone, `ARCHIVE` -> secondary tone, `DEFAULT` -> primary brand tone).
- **Duration Configuration:**
  - Define an explicit duration policy:
    - Destructive / Undoable actions: `7000L` milliseconds.
    - Informational notices: standard `3500L` milliseconds.
  - When an undo action is present, provide the duration parameter to the toast host and animate progress from `1.0f` down to `0.0f` linearly over the specified duration.

### 2. State & Timer Lifecycle
- Drive the progress value using Compose animation primitives (e.g., `Animatable` or `produceState` tied to the message key / snackbar instance).
- When a new message arrives or the current message changes, the animation restarts from `1.0f`.
- When the user taps the action button, the animation halts and dispatches the action immediately.

### 3. Separation of Concerns
- The progress animation is strictly a visual aid rendered inside the Toast Pill presentation layer; business logic and repository undo caches remain completely decoupled.
- The repository's 5-second in-memory undo window is updated to match the 7-second display window, ensuring that any undo tap during the visible toast lifecycle successfully restores the entity.

---

## Testing Decisions

### Good Test Principles
- Test externally observable behavior, state transitions, duration mapping, and semantic coloring. Do not test internal Compose rendering loops or frame-by-frame canvas redraws.

### Target Test Seams
1. **Primary Seam — Component Contract & State Seam (`HelloToastHostTest`):**
   - **Duration Mapping:** Verify that messages with undo actions (containing "Deleted", "Archived", "Restored") resolve to the extended 7-second duration, while informational messages resolve to the standard duration.
   - **Progress Calculation:** Verify that given elapsed time vs total duration, the remaining progress fraction correctly computes from `1.0f` to `0.0f`.
   - **Semantic Color Resolution:** Verify that delete actions map the progress track to error styling, archive actions map to secondary styling, and generic actions map to primary styling.
2. **Secondary Seam — Repository Undo Window:**
   - Verify that the in-memory undo cache in `ContactRepository` / `EncounterRepository` remains valid and successfully executes rollback for the full 7-second duration.

### Prior Art
- `HelloToastHostTest`: Existing semantic resolution tests for Toast Pill message types.
- `ArchivedContactsContractTest`: Verifies repository undo operations and restoration.

---

## Out of Scope

- **Numeric Digital Countdown Clock:** Displaying text such as "(7s... 6s...)" inside the action button or pill body is excluded to keep the visual design clean and prevent layout shifting as characters change width.
- **Manual Pause on Hover / Touch-and-Hold:** Pausing the countdown when pressing down on the pill is deferred as a future tactile enhancement.
- **Global User Setting for Custom Duration:** A settings toggle to customize snackbar duration in seconds is out of scope for this focused fix.

---

## Further Notes

- Directly aligns with **ADR-0026** (Elevated Toast Pill & Dock Clearance) and **ADR-0027** (Toast Pill Semantic Icons & In-Memory Undo Pipeline).
- Resolves the real-world usability friction where users miss their chance to undo accidental deletions due to aggressive 4-second timeouts.

