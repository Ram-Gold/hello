# ADR-0040: Single-Surface Floating Quick Capture & FAB-to-Choice Morph

- **Status:** Accepted
- **Date:** 2026-11-24
- **Supersedes (UI portion):** ADR-0004 (Unified Quick Action Modal), ADR-0012 (Step-by-Step Modular Capture Flow UI)
- **Origin:** `RAM-91` ("Reevaluate the flow for quickly adding and logging people")

## Context

Flow A (`FlowAAddContactCard`) implemented `ADR-0012` as a 4–6 step sequential wizard. In daily use, Flow B (`FlowBLogEncounterSheet` Stage 2) felt dramatically faster because all encounter inputs were visible on one flat surface with non-focus-stealing chips. In Flow A, adding just a Name + Place + Note required stepping past unused fields (`Role`, `Company`), caused repeated focus/IME transitions, and produced vertical layout jumps when `Save <Name> Now` appeared.

## Decision

1. **Retire `QuickActionChoiceSheet` ("Quick Capture" Choice Card) in Favor of Direct Entry:**
   - The Floating Bottom Dock's `+` Speed Dial (`Add Person` / `Met Again`) already serves as the chooser. Removing the intermediate 2-tile `"Quick Capture"` choice card (`QuickActionChoiceSheet` / `QuickActionMode.CHOICE`) eliminates redundant navigation.
   - Dismissing or cancelling from the composer (via the top-right circular `×` close button, system Back, or tapping the scrim) cancels and closes the modal directly instead of routing back to a choice sheet.
   - `QuickCaptureActivity` (external launcher/widget entry) continues to open directly into the **Add Contact** floating card to preserve `<50ms` cold start.
   - **Widget-Only Pre-Positioned IME Placement:** `QuickCaptureActivity` intentionally does **not** use per-frame `.imePadding()` (and uses `windowSoftInputMode="stateAlwaysVisible|adjustNothing"`). Instead, it pre-positions the floating card on frame 0 at the device's learned per-orientation IME height (persisted asynchronously via `UserPreferences`, falling back to `35%` of screen height on first launch) so the keyboard rises without Compose relayout contention. Once the IME settles, if the real IME height differs by `> 4.dp`, the card makes a single correction and updates the saved height. If the keyboard subsequently closes after having opened, `QuickCaptureActivity` cancels and dismisses immediately (no exceptions). In-app `QuickActionSheetHost` remains unchanged.
2. **Unify Add Contact & Met Again into a Todoist-Style Single-Surface Floating Card:**
   - Hosted inside `QuickActionSheetHost`'s in-hierarchy `Surface` with `.navigationBarsPadding().imePadding()` and `ScrubbablePredictiveBackHandler`.
   - Anatomy: top drag handle (expands/collapses `Notes`), a clean clickable editorial header (`titleMedium` / `titleSmall` SemiBold in `onSurface` + subtle `16.dp` muted swap/unfold icon, no pill container and no `"· switch"` text) that toggles between Add Contact and Met Again on tap alongside a subtle circular `×` close button on the right, auto-focused primary input (`Name` or participant filter), and a bottom action bar (`[+]` overflow popover + horizontal scrollable ADR-0038 detail buttons + right-thumb primary Save button).
   - **Floating Detail Popovers Above Composer (No Inline Container Expansion):** Tapping any detail button (`Location`, `Tags`, `Reminder`, `Time`, `Occupation`, `Phone & Email`, `Custom`) or `+` opens a separate elevated floating popover card hovering directly above the composer card, keeping the composer stationary and compact above the keyboard. Single-select choices (`Location` preset, `Reminder` interval, `Today`/`Yesterday`) apply and auto-dismiss in 1 tap; multi-select (`Tags`) and text inputs (`Occupation`, `Phone & Email`, `Custom`, custom `Location`) stay open until tap-outside or `Done`.
   - Populated detail buttons morph to their filled squircle state displaying the live value with a `×` clear affordance.
   - **Met Again Inline Participant Chips & Predictive Autofill:** `Notes` (`"Caught up about..."`) starts collapsed by default on `Met Again` and expands/focuses on `Enter` (or drag handle / `+ → Notes`). The floating suggestion strip above the card is hidden until the user types a non-empty query, matching both `Contact.name` and `Nickname` (`Contact.aliases`), showing `Name (Nickname)` for nickname matches and a 1-tap `Add "[query]" as new contact` pill when unmatched. Confirmed Contacts render inline in a wrapping `FlowRow` beside the live cursor as rounded chips with their Contact Avatar and name; typing `,` commits the top predictive match and clears the query for the next person, while pressing `Enter` commits any matching query and expands/focuses `Notes`.
3. **Repurpose `Flow Preset` (ADR-0012) as Quick Add Button Presets:**
   - Retire the sequential step wizard. Existing `FlowPreset` definitions (`Default`, `Conference`, `Casual Party`, `Campus`) now control which detail buttons are pinned in the bottom bar and in what order, editable via `+ → Customize Quick Add` and Settings.
   - Default pinned buttons: **Add Contact** → `Location`, `Tags`, `Reminder`, `Occupation`; **Met Again** → `Location`, `Time`, `Tags`, `Reminder` (with `Notes` expanded).
4. **Explicitly Out of Scope:**
   - Natural-language inline text parsing (`#tag`, `@place`, `"tomorrow"`) and Room v9 schema changes.

## Consequences

- **Positive:** Adding a Contact with 0, 1, or 3+ details now requires zero wizard-step traversal and keeps the IME stable; `Add Contact` and `Met Again` share one cohesive visual language; `FlowPreset` customization remains valuable without wizard friction.
- **Trade-offs:** Existing step-index unit tests in `QuickActionViewModelTest` will be updated to test single-surface composer state and preset button pinning; `HomeFloatingBottomDock` and `QuickActionSheetHost` must coordinate the FAB container-transform bounds cleanly while respecting `LocalReducedMotion`.
