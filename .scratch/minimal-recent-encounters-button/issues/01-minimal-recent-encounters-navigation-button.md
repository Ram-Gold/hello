# 01: Minimal Recent Encounters Navigation Button

**What to build:**
On the Home screen, replace the filled secondary-container pill button (`"[count] people →"`) in the Recent Encounters section header with a clean, minimal `>` chevron button without a background. The button must keep a comfortable minimum touch target (≥ 48dp), provide clear accessibility semantics for screen readers ("View all people"), and trigger navigation to the People tab on click.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] The "Recent Encounters" section header displays a minimal `>` chevron button with no background shape or container color.
- [x] Tapping the chevron button navigates to the People tab (`onNavigateToPeople`).
- [x] Interactive touch target satisfies Android accessibility guidelines (minimum 48dp × 48dp touch bounds).
- [x] An accessible content description is provided (e.g. "View all [terminology]").
- [x] Existing spring press animation or ripple feedback remains tactile and lightweight.
- [x] All unit tests pass cleanly.

