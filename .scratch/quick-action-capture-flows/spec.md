# Spec: Screen 3 — The `+` Quick Action (Minimal 2-Button Speed-Dial & Material 3 Capture Flows)

**Status:** ready-for-agent

---

## Problem Statement

When logging a new Contact or recording an Encounter on the go, heavy card containers, helper tips, dialog window lag, and mismatched chip borders create visual clutter and slow down capture. Users want a fast, minimal Material Design 3 capture flow: tapping the `[ + ]` button pops out two stacked buttons (`Met Again` and `Add Contact`) above the FAB, and the flow screen itself opens in-hierarchy before the keyboard rises, featuring soft filled inputs, circular `'x'` and `'->'` header buttons flanking minimal progression dots, and Material 3 tonal tags/chips where the selected background color matches the border color with bold, high-contrast typography.

---

## Solution

Deliver a cohesive, minimal Material Design 3 Quick Action experience:

1. **Minimal Stacked 2-Button Speed-Dial Trigger**:
   Tapping the Floating Dock `[ + ]` button rotates the `+` icon 45° into an `×` and springs open two stacked pill buttons in a vertical column right above the FAB (`Met Again` and `Add Contact`).
2. **Zero-Lag In-Hierarchy Sheet & Synchronized Keyboard**:
   Render the Quick Action sheet directly inside the main window hierarchy (eliminating separate `DialogWindow` latency) and sequence focus acquisition after the first frame settles so the input surface is always visible before the software keyboard rises.
3. **Circular Header Controls & Centered Minimal Dots**:
   - Left action (`'x'` close or `'<-'` back): `40.dp` `CircleShape` button in `MaterialTheme.colorScheme.surfaceVariant`.
   - Center indicator: Minimal animated dots (`6.dp` circles expanding to `18.dp` in `primary` on the active step) with zero numeric text clutter.
   - Right action (`'->'` next/commit): `40.dp` `CircleShape` button in `MaterialTheme.colorScheme.primary` with `onPrimary` icon.
4. **Filled Inputs & Material 3 Tonal Tags/Chips**:
   - **Filled Inputs (`MinimalFilledTextField`)**: Soft `surfaceVariant` background fill with `14.dp` rounded corners, bold `titleLarge` input text, and no outer card wrapper.
   - **Material 3 Tonal Chips (`MinimalSelectableChip`)**:
     - **Unselected**: `Color.Transparent` background with a `1.dp` `MaterialTheme.colorScheme.outlineVariant` border and `FontWeight.SemiBold` `labelLarge` text.
     - **Selected**: Matching background and border color (`containerColor == borderColor == MaterialTheme.colorScheme.primaryContainer`) with `FontWeight.Bold` `onPrimaryContainer` `labelLarge` text and `MaterialTheme.shapes.small` corners, adhering strictly to Material Design 3 tonal pairing and shape tokens.

---

## User Stories

1. As a user on the Home or People screen, I want to tap the Floating Dock `[ + ]` button and see two minimal stacked buttons (`Met Again` and `Add Contact`) pop out in a column right above the FAB while `+` rotates to `×`, so that I can trigger either flow in one thumb motion.
2. As a user opening `Add Contact` or `Met Again`, I want the flow sheet and its filled input box to render on screen before the software keyboard slides up, so that the keyboard never appears ahead of the UI.
3. As a user in `Add Contact` or `Met Again`, I want a circular `'x'` / `'<-'` button on the top left, minimal progression dots in the center, and a primary circular `'->'` arrow button on the top right, so that navigation and progression are immediately clear without text clutter.
4. As a user typing in Step 1 (`"Who did you meet?"`), Step 2 (`"Where did you meet?"`), or Step 4 (`"Quick Note & Follow-Up"`), I want a soft filled input container (`surfaceVariant`) with bold, legible typography and no outer card wrapper or `"Tip:"` text.
5. As a user viewing unselected Tags, Circles, Places, Dates, or Follow-Up options, I want each chip to have a clean `1.dp` `outlineVariant` border, transparent fill, and bold `SemiBold` `labelLarge` typography.
6. As a user selecting a Tag (`#tech`, `#design`, `#coffee`, `#ai`), Circle (`Work`, `Friends`, `Family`, `Uni`), Place, Date, or Follow-Up chip, I want its background color and border color to be the exact same Material 3 tonal color (`primaryContainer`) with `FontWeight.Bold` `onPrimaryContainer` text, so that selected tags look unified, tactile, and unmistakably active.
7. As a user tapping a Place chip in Step 2 of `Add Contact`, I want the flow to select that Place and automatically advance to Step 3 (`"Circles & Tags"`).
8. As a user tapping the circular `'->'` arrow on the final step of `Add Contact` (or on the Encounter form of `Met Again`), I want the Contact and Encounter committed in `<2ms`, updating both the Home and People screens and resetting any overdue relationship Cadence.
9. As a privacy-conscious user tapping `📍 Current GPS`, I want an in-app explainer dialog before any system location permission prompt is shown, with graceful fallback if declined.

---

## Implementation Decisions

### 1. Material Design 3 Tonal Tag & Chip Token Pairing
- Selectable tags and chips use `MaterialTheme.shapes.small` (`8.dp` MD3 chip corner token) and `MaterialTheme.typography.labelLarge`.
- Unselected state pairs `Color.Transparent` fill with a `1.dp` `MaterialTheme.colorScheme.outlineVariant` border and `FontWeight.SemiBold` `onSurfaceVariant` label.
- Selected state sets both `containerColor` and `borderColor` to `MaterialTheme.colorScheme.primaryContainer` (identical background and border color) paired with `FontWeight.Bold` `MaterialTheme.colorScheme.onPrimaryContainer` text.
- Prompt headers use `MaterialTheme.typography.titleMedium` with `FontWeight.Bold`.

### 2. Filled Input Surface & Circular Header Controls
- Text inputs use `MinimalFilledTextField` with `MaterialTheme.colorScheme.surfaceVariant` background fill, `14.dp` corner radius, and `titleLarge` (`FontWeight.Medium`) typography.
- The top bar renders a `40.dp` `CircleShape` `surfaceVariant` close/back button on the left, animated minimal pill dots in the center, and a `40.dp` `CircleShape` `primary` `'->'` arrow button on the right.

### 3. In-Hierarchy Overlay & Frame-Sequenced IME Focus
- `QuickActionSheetHost` renders as an in-hierarchy bottom-anchored `Surface` with `navigationBarsPadding()` and `imePadding()`, avoiding `ModalBottomSheet` window creation delay.
- `FocusRequester.requestFocus()` is sequenced after `withFrameNanos { }` so the sheet renders before the IME animates up.

---

## Testing Decisions

### What Makes a Good Test
- Tests verify observable state transitions and domain side effects at the `QuickActionViewModel` and `ContactRepository` boundary without coupling to internal Composable styling details.

### Modules to Be Tested
- **Single High-Level UDF Seam (`QuickActionViewModel` + `ContactRepository`)**:
  - Verifies direct launch into Flow A (`OnSelectAddContact`) and Flow B (`OnSelectLogEncounter`).
  - Verifies step-by-step progression (`OnNextStep` / `OnPreviousStep`), Place auto-advance (`OnSelectPlaceChip`), Tag/Circle toggling (`OnToggleTag` / `OnToggleCircle`), and final commit (`OnQuickSaveFlowA`).
  - Verifies Flow B contact search (`OnSearchQueryChange`), contact selection (`OnSelectExistingContact`), date/place/note updates, and encounter save (`OnSaveFlowBEncounter`) with Cadence alert reset.

### Prior Art
- Unit test suites in `QuickActionViewModelTest.kt`, `ContactRepositoryTest.kt`, `HomeViewModelTest.kt`, and `ContactsViewModelTest.kt`.

---

## Out of Scope

- Markdown file I/O via Storage Access Framework and Room database migrations.
- Network calls, cloud sync, or reading device system contacts.

---

## Further Notes

- Follows `.agents/skills/material-3/SKILL.md` tonal color pairing, shape tokens (`MaterialTheme.shapes.small`), and emphasized typography guidelines alongside `AGENTS.md` and `GLOSSARY.md`.
