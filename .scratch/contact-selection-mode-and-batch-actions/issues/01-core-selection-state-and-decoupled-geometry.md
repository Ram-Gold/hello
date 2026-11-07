# 01: Core Selection State, Long-Press Trigger & Decoupled Card Geometry

**What to build:** Enable long-press on `ContactCard` to enter selection mode with that contact selected. When selected, cards decouple to `RoundedCornerShape(24.dp)` on all four corners, display an inline checkmark beside the contact name, and adopt a theme-aware red surface (`errorContainer`). Single taps on cards toggle selection; deselecting the last contact auto-exits. The top bar switches to `[X selected]` with a circular 'X' cancel button, system Back cancels selection mode, and the bottom dock slides off-screen.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Long-pressing any contact card enters selection mode and selects that contact.
- [x] Selected cards render with dynamic theme-aware red container styling (`errorContainer`) and fully rounded 24dp corners on all 4 sides.
- [x] An inline checkmark icon appears beside the contact name on selected cards.
- [x] Tapping a card during selection mode toggles its selection state on/off.
- [x] Deselecting the last selected card automatically exits selection mode.
- [x] Tapping the top-right 'X' button or pressing system Back exits selection mode and clears all selections.
- [x] Floating Bottom Dock slides down off-screen while in selection mode.
- [x] Unit tests in `ContactsViewModelTest` verify selection state transitions and auto-exit invariants.
