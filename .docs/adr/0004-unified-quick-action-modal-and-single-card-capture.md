# 4. Two-Button Speed-Dial Quick Capture, Single-Card Persistent Keyboard Flow, and Repository State Sharing

## Context
Capturing a human encounter or new contact in "Hello" must meet the prime architectural invariant of extreme capture speed (<50ms cold start, <2ms commit to timeline) without cluttering the screen or dropping keyboard focus. Users need a minimal, tactile entry point from the Floating Dock `[ + ]` button that preserves the stacked 2-button column (`Met Again` and `Add Contact`) while launching directly into structured capture flows. Furthermore, `HomeViewModel` and `ContactsViewModel` previously managed isolated mock state rather than sharing a single reactive source of truth.

## Decision
1. **Minimal Stacked 2-Button Speed-Dial Trigger**:
   Retain the lightweight, spring-animated 2-button column popping out directly above the Floating Dock `[ + ]` FAB (which rotates 45° to `×` when open, backed by a dismissible scrim). The two stacked pill buttons are `Met Again` (top) and `Add Contact` (bottom), launching Flow B and Flow A directly without an intermediate choice modal sheet.
2. **Flow A (Add Contact) Single-Card Persistent Keyboard Capture**:
   Present a distraction-free single card resting immediately above the IME (`WindowInsets.ime`). The software keyboard retains continuous focus across steps via a single focused input element, avoiding re-layout flicker or dismissal jumps.
   - **Step 1 (Name)**: Instant text entry, advancing on keyboard Next/Continue.
   - **Step 2 (Place)**: Suggested recent `Place` chips and an optional `📍 Near Current GPS` chip. Tapping any chip auto-advances. Manual text entry and 'Skip' remain accessible.
   - **Step 3 (Tags & Circles)**: Multi-selection chips for tags (`#tech`, `#design`, `#coffee`, `#ai`) and primary `Circle` categories (`Work`, `Friends`, `Family`, `Uni`), progressing via Next.
   - **Step 4 (Encounter Note / FollowUp)**: Freeform scratchpad for encounter notes, with quick action chips (`In 3 days`, `Next week`, `In 1 month`) that optionally spawn an actionable `FollowUp` task.
   - **Escape Hatch (Save ✔)**: A persistent header action available at all steps that commits the `Contact` and an initial `Encounter` timestamped to "Now" in under 2ms.
3. **Flow B (Met Again / Log Encounter)**:
   An integrated two-stage bottom sheet. An instant search bar and horizontal recent contact row allow picking an existing `Contact` in one tap. The sheet smoothly morphs into encounter inputs (`When` chip selector: `Today` / `Yesterday` / `Custom...`, `Where` place chips, and `Note` scratchpad). Committing resets the contact's relationship cadence and clears any active cadence alerts.
4. **Graceful Foreground-Only Location**:
   Adhere strictly to zero-background-location discipline. Tapping the GPS chip presents a warm in-app explainer dialog detailing offline-only local storage before triggering Android system runtime permissions. If permission is denied, recent place chips remain fully functional without interruption.
5. **Decoupled State & Repository Sharing**:
   Introduce a lightweight in-memory `ContactRepository` in `data/repository/` exposing reactive Kotlin `StateFlow` streams. A dedicated `QuickActionViewModel` coordinates capture logic at the `MainAppShell` layer, ensuring saves immediately update both Home and People screens reactively.
