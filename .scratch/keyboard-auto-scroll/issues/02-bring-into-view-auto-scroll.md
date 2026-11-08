# 02: BringIntoView Auto-Scroll on Input Field Focus

**What to build:**
Attach `BringIntoViewRequester` and focus change listeners to all text input fields in `PersonEditScreen` (Full Name, Nickname input, Role, Company, Phone, Email, Notes, and Custom Cadence). When any field gains focus or the keyboard transitions to visible, automatically and smoothly animate the scroll offset (`scrollState.animateScrollTo(...)` or `bringIntoViewRequester.bringIntoView()`), bringing the active field into view with comfortable vertical breathing room above the keyboard.

**Blocked by:** 01 (Core IME Insets & Viewport Shrinking in PersonEditScreen)

**Status:** done

- [x] Attach `BringIntoViewRequester` to every text field composable in `PersonEditScreen`.
- [x] On focus change (`onFocusChanged { if (it.isFocused) ... }`), trigger `bringIntoView()` with an extra margin or animate scroll offset.
- [x] Add a LaunchedEffect observing IME visibility transitions (`WindowInsets.isImeVisible`) so opening the keyboard while a field is focused automatically re-aligns the field above the keyboard.
- [x] Ensure `Notes` multi-line field centers or exposes its cursor area clearly without being cut off by the keyboard's top edge.

