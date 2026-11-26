# CODING_STANDARDS.md

Review-stage judgement calls for the Hello Android codebase. Mechanical checks (unit tests, banned permissions) belong in `.git/hooks/pre-commit` and `./gradlew lintDebug`.

## Compose Focus & Soft Keyboard (IME) Continuity

- **Keep one persistent `BasicTextField` across mode toggles**: Never swap between two separate `BasicTextField` branches (`if (modeA) BasicTextField(...) else BasicTextField(...)`) on a live capture surface. Disposing a focused `BasicTextField` tears down the `InputConnection`, drops the keyboard, and causes a full hide/re-show bounce.
- **Hand off focus before unmounting secondary inputs**: When toggling modes or dismissing overlays while focus is in a secondary field (such as `Notes` or a floating popover text input), transfer focus to the persistent primary field and wait 2 frames (`withFrameNanos`) before unmounting the secondary field.
- **Observe IME insets off the composition phase**: Read `WindowInsets.ime` and `WindowInsets.imeAnimationTarget` inside `snapshotFlow` (within `LaunchedEffect`) or via layout/draw modifiers—never directly in a root `setContent` body—so keyboard frames do not recompose the root tree.

## Floating Overlays, Motion & State Isolation

- **Floating popovers never expand inside the composer card**: Render Quick Capture detail popovers (`Location`, `Tags`, `Reminder`, `+`, etc.) as `zIndex` overlays anchored above the composer top rim so the primary input stays sharp and stationary.
- **Key mode-specific animated blocks**: Wrap mode-dependent `AnimatedVisibility` regions (such as Notes) in `key(uiState.currentMode)` so switching modes does not flash outgoing content.
- **Purposeful motion (`.agents/DESIGN.md`)**: Use `MotionTokens.StrongEaseOut` for entrances/exits and `MotionTokens.StrongEaseInOut` for on-screen morphs, asymmetric faster exits (`130ms` exit vs `180ms` enter), origin-aware `0.94f` scale popovers (never scale from `0`), GPU-only `graphicsLayer` transforms (including draw-phase pill-to-squircle shape morphs—never per-frame `.clip(RoundedCornerShape(animatedDp))` or per-frame `BlurEffect` shader reallocation), and gentle opacity-only fallbacks under `LocalReducedMotion`.

## Architecture & State Consistency

- **Unidirectional Data Flow (UDF)**: Keep screens on immutable `UiState`, sealed `UiEvent`, and `StateFlow` exposed from `ViewModel`. Keep `ui/` free of direct Room or filesystem calls.
- **Keep `ContactSummary` projections in sync**: Any field added to `ContactSummary` (e.g., `nicknames`) must stay synchronized across Room mapping (`ContactEntity.toSummary`), domain mapping (`Contact.toSummary`), in-memory mutations (`ContactRepository.updateContact`), and sample seeds (`SeedDataFactory.createSampleContactSummaries()`).
- **Deterministic tests**: Pass `Dispatchers.Unconfined` (or a `TestDispatcher`) when unit-testing ViewModels that collect repository flows on `ioDispatcher` to avoid `Dispatchers.IO` races.

## UI Copy & Terminology

- **Use `GLOSSARY.md` terms** (`Contact`, `Nickname`, `Encounter`, `Place`, `Tag`, `Reminder`).
- **Write plain, human UI copy**: Avoid em dashes and overly formal AI-style phrasing in user-facing labels, placeholders, and helper text.
