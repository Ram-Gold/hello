# 01: Core IME Insets & Viewport Shrinking in PersonEditScreen

**What to build:** 
Ensure the scrollable container in `PersonEditScreen` dynamically respects the Android soft keyboard insets (`WindowInsets.ime` via `Modifier.imePadding()`). When the keyboard opens, the container's bottom edge shrinks to sit immediately above the keyboard instead of running beneath it, ensuring content can be scrolled all the way to the end without keyboard occlusion.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Apply `Modifier.imePadding()` to `PersonEditScreen`'s content Column / scrollable container.
- [x] Ensure `Scaffold` inner padding and navigation bar insets are cleanly consumed and do not double-pad when the keyboard is raised.
- [x] Verify that scrolling to the very bottom of the screen with the soft keyboard open allows viewing the Save button and all bottom controls.

