# 02: Animated Slim Countdown Progress Bar Component in Toast Pill

**What to build:** Integrate a 2–3dp animated progress bar along the bottom edge of the `HelloToastPill` capsule. When an actionable toast with an "Undo" label appears, the bar starts at 100% width and smoothly shrinks to 0% width over the duration (7s), clipped precisely to the pill's 24dp squircle shape. The bar uses theme-aware and semantic-aware coloring (error container / red tint for delete, warm secondary amber for archive, primary terracotta for generic). When the action button is tapped or the message is dismissed, the animation stops immediately without leaking coroutines.

**Blocked by:** 01: Duration Policy, Semantic Progress Contract, and 7-Second Repository Undo Window

**Status:** closed

- [x] Progress line is rendered along the bottom edge of `HelloToastPill` with a height of 2–3dp.
- [x] Progress line is strictly clipped within the pill's 24dp rounded corners, never overflowing or bleeding outside the capsule.
- [x] Only actionable toasts with an action label (or duration > 4000ms) display the bottom countdown progress line; non-actionable toasts remain clean without a countdown line.
- [x] The progress line animates continuously and smoothly from full width to empty width across the duration.
- [x] Semantic colors match the action context (error tint for delete, secondary amber for archive, primary for other actions).
- [x] Tapping the action button immediately terminates the countdown animation.
- [x] Switching or updating messages resets the progress bar cleanly from 100%.
- [x] Automated tests verify progress bar visibility rules and lifecycle cleanup.

