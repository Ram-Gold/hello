# 0030: Semantic Tactile Haptic Feedback and Choreographed Contact Departure Motion

**Date:** 2026-11-08  
**Status:** Accepted  
**Context:**  
Interactions across Hello—such as overscroll pull-to-action gestures, toggling between capture workflows, saving new relationships or edits, entering edit modes, and performing irreversible actions like archiving or deleting—previously lacked physical tactile confirmation. When contacts were archived or deleted, cards abruptly vanished from the screen with harsh jump-cuts in adjacent list rows. Users requested immediate tactile physical feedback for state transitions and refined exit motion communicating spatial destinations.

**Decision:**
1. **Centralized Semantic Haptics Architecture (`HelloHaptics`):**
   - High-level interaction types defined in `SemanticHapticType`: `THRESHOLD_ACTIVATION`, `MODE_TOGGLE`, `SUCCESS_CONFIRMATION`, `EDIT_ENTRY`, and `DESTRUCTIVE_ACTION`.
   - Provided ambiently through Compose `LocalHelloHaptics` with standard fallback to Compose `HapticFeedback` and hardware `Vibrator` pre-baked/waveform vibration patterns on Android.
   - Connected across key touchpoints: pull-to-action threshold crossing, Quick Capture workflow toggles, profile edit clicks, contact creation/encounter saves, toast pill undo actions, and deletion/archive confirmations.
2. **Contact Departure Motion Choreography (`ContactCardExitTransition`):**
   - **Archive Spatial Tuck:** $-20\%$ horizontal translation and $-15\%$ upward vertical tuck with $180\text{ms}$ `FastOutSlowInEasing` fade, visually routing cards toward the top archive partition.
   - **Delete Physical Dissolution:** Scales down to $0.92\times$ and dissolves with $150\text{ms}$ `LinearOutSlowInEasing` fade and vertical row collapse.
   - **Restore Departure:** Glides out with $+25\%$ horizontal slide and vertical row collapse in `ArchivedContactsScreen`.
3. **Reactive List Reflow (`Modifier.animateItem`):**
   - Adjacent cards smoothly slide into position using spring physics (`dampingRatio = 0.82f`, `stiffness = Spring.StiffnessMediumLow`), eliminating layout jump-cuts.
4. **Accessibility & Reduced Motion:**
   - Both haptics and motion respect user preferences: `reduceAnimations = true` clamps animation delays to $0\text{ms}$ and bypasses transitions via `ExitTransition.None`.

**Consequences:**
- **Positive:** Tactile physical presence for all consequential actions; seamless visual continuity for list departures without jump-cuts; zero extra dependencies; graceful degradation on devices without vibration actuators or with reduced motion enabled.
- **Negative:** Transient exiting state requires local coroutine coordination prior to database mutations (bounded under $180\text{ms}$).
