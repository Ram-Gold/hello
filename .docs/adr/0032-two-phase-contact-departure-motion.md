# 0032: Two-Phase Contact Departure Motion

**Status:** Accepted  
**Refines:** [ADR-0030](0030-semantic-tactile-haptics-and-contact-departure-motion.md)

## Decision

Contact Departure Motion (`ContactExitMotionTokens` and `ContactCardExitTransition`) uses a strict two-phase choreography instead of combining `shrinkVertically` inside `AnimatedVisibility` with `Modifier.animateItem`:

1. **Phase 1 — Departure:** The exiting Contact Card plays its spatial tuck (Archive: `180ms`), physical dissolution (Delete: `150ms`), or horizontal glide (Restore: `160ms`) while still occupying its list slot (`0ms` when reduced motion is enabled).
2. **Phase 2 — Reflow:** Once the departure delay completes, the Contact is removed from the list state and `Modifier.animateItem(placementSpec = ...)` on both Contact Cards and alphabetical `stickyHeader` items smoothly springs neighboring items upward/inward to close the gap.

## Rationale

Previously, `shrinkVertically(animationSpec = placementSpringSpec())` inside `AnimatedVisibility` ran a ~400ms spring that was cut short at 150–180ms when the item was removed from `LazyColumn` state, fighting `Modifier.animateItem` and causing neighboring cards to snap or jump mid-collapse. Removing `shrinkVertically` across Archive, Delete, and Restore transitions lets the departure fade/slide/scale complete cleanly before `Modifier.animateItem` owns 100% of the gap-closing reflow.
