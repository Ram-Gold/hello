# 01: Core Exit Motion Tokens and Spatial Archive Tuck Animation

**What to build:**
Create the motion tokens and departure state coordinator for contact card exits. Implement the archive departure animation: card elevates with a subtle tilt, glides out with horizontal translation and an upward tuck toward the top archived boundary, followed by a spring collapse of the vacated row.

**Blocked by:** None (can start immediately)

**Status:** closed

- [x] Define `ContactExitMotionTokens` specifying physics springs, slide offsets, and fade curves for archive and delete exits.
- [x] Implement `ContactCardExitTransition` composable wrapper or modifier driving the archive exit tuck.
- [x] Wire archive exit motion into `ContactsScreen` and `PersonEditScreen` archive confirmation flows.
- [x] Support reduced-motion fallback bypassing animation delays when enabled.

