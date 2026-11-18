# 0030: Semantic Tactile Haptic Feedback and Choreographed Contact Departure Motion

**Date:** 2026-11-08  
**Status:** Accepted  
**Context:**  
Interactions across Hello—such as overscroll pull-to-action gestures, toggling between capture workflows, saving new relationships or edits, entering edit modes, and performing irreversible actions like archiving or deleting—previously lacked physical tactile confirmation. When contacts were archived or deleted, cards abruptly vanished from the screen with harsh jump-cuts in adjacent list rows. Users requested immediate tactile physical feedback for state transitions and refined exit motion communicating spatial destinations.

**Decision:**
1. **Centralized Semantic Haptics Architecture (`HelloHaptics`):**
   - High-level interaction types defined in `SemanticHapticType`: `THRESHOLD_ACTIVATION`, `MODE_TOGGLE`, `SUCCESS_CONFIRMATION`, `EDIT_ENTRY`, and `DESTRUCTIVE_ACTION`.
   - Provided ambiently through Compose `LocalHelloHaptics` in both `MainAppShell` and `QuickCaptureActivity`, using `View.performHapticFeedback` (`HapticFeedbackConstants.CONFIRM`, `REJECT`, `GESTURE_START`, `CLOCK_TICK`, `CONTEXT_CLICK` on supported API levels) as the primary OS-integrated channel, with `Vibrator` (`VIBRATE` permission + `VibrationEffect` waveforms/predefined effects) and Compose `HapticFeedback` fallbacks.
   - Connected across key touchpoints:
     - **Archived Contacts Screen navigation:** `THRESHOLD_ACTIVATION` once upon crossing the pull-to-action threshold, followed by a lighter `MODE_TOGGLE` upon release to navigate (avoiding duplicate threshold pulses).
     - **Log / Update mode switching:** `MODE_TOGGLE` when selecting Speed Dial actions (`Met Again` / `Add Contact`), choosing Quick Capture modes (`QuickActionChoiceSheet`), navigating Flow B stages, or toggling Contact favorite status.
     - **Completions & updates:** `SUCCESS_CONFIRMATION` on Flow A/B saves (including `QuickCaptureActivity`), `PersonEditScreen` saves, `ArchivedContactsScreen` restores, `Undo Action` restorations, and Home `UpcomingFeedSection` `"Done"` / `"Update"` completions.
     - **Edit state entry:** `EDIT_ENTRY` when opening `PersonEditScreen` or long-pressing into `Contact Selection Mode` / `Timeline Selection Mode` (across `ContactsScreen`, `TimelineScreen`, and `PersonDetailScreen`).
     - **Archive & Delete confirmations:** `DESTRUCTIVE_ACTION` consistently across both single-contact (`PersonEditScreen`) and batch (`ContactsScreen`) Archive and Delete confirmations, plus encounter deletion confirmations.
2. **Contact Departure Motion Choreography (`ContactCardExitTransition`):**
   - **Archive Spatial Tuck:** $-20\%$ horizontal translation and $-15\%$ upward vertical tuck with $180\text{ms}$ `FastOutSlowInEasing` fade, visually routing cards toward the top archive partition.
   - **Delete Physical Dissolution:** Scales down to $0.92\times$ and dissolves with $150\text{ms}$ `LinearOutSlowInEasing` fade and vertical row collapse.
   - **Restore Departure:** Glides out with $+25\%$ horizontal slide and vertical row collapse in `ArchivedContactsScreen`.
3. **Reactive List Reflow (`Modifier.animateItem`):**
   - Adjacent cards smoothly slide into position using spring physics (`dampingRatio = 0.82f`, `stiffness = Spring.StiffnessMediumLow`), eliminating layout jump-cuts.
4. **Accessibility & Reduced Motion:**
   - Visual motion respects `reduceAnimations = true` by clamping animation delays to $0\text{ms}$ and bypassing transitions via `ExitTransition.None`.
   - `HelloHaptics` remains enabled independently of `reduceAnimations` and respects Android's system touch-feedback preferences via `View.performHapticFeedback`.

**Consequences:**
- **Positive:** Tactile physical presence for all consequential actions; seamless visual continuity for list departures without jump-cuts; zero extra dependencies; graceful degradation on devices without vibration actuators or with reduced motion enabled.
- **Negative:** Transient exiting state requires local coroutine coordination prior to database mutations (bounded under $180\text{ms}$).
