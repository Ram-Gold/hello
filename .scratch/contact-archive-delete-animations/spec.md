# Spec: Contact Archive and Deletion Animations

## Problem Statement

When archiving or deleting a contact in Hello, the contact card abruptly vanishes from the screen. Surrounding contact cards in the directory jump instantly into the vacant space with a harsh, jarring cut. In addition, when executing an archive or delete operation from the edit screen or batch action tray, there is no spatial or visual transition communicating where the card went (i.e. moved to the hidden archive partition versus permanently erased). This lack of visual continuity breaks the tactile, physical character of the app and makes high-stakes actions feel sudden and unrefined.

## Solution

Introduce choreographed exit motion when archiving or deleting contacts across both the primary directory and the archived contacts screen:
1. **Archive Motion Choreography (Spatial Tuck):**
   - When a contact is archived, the card elevates with a subtle physical tilt, glides out with a horizontal slide and upward tuck towards the top boundary where archived contacts reside, followed by a smooth spring collapse of the vacant vertical space.
2. **Delete Motion Choreography (Physical Dissolution):**
   - When a contact is deleted, the card scales down subtly ($1.0\times \rightarrow 0.92\times$), fades in opacity, and collapses vertically, signaling permanent destruction.
3. **Reactive List Reflow (`Modifier.animateItem`):**
   - Remaining adjacent cards in the `LazyColumn` smoothly slide into their new positions using spring physics (`dampingRatio = 0.8f`, `stiffness = Spring.StiffnessMediumLow`), eliminating layout jump-cuts.
4. **Restore Motion Choreography:**
   - In `ArchivedContactsScreen`, tapping "Restore" smoothly glides the card away and collapses its row, while re-entering the active directory with an entrance fade and spring expand.
5. **Reduced Motion Respect:**
   - Automatically degrades to instant cuts or zero-duration transitions when the user enables "Reduce Animations" in Settings or system accessibility.

---

## User Stories

1. As a user archiving an inactive relationship, I want the contact card to animate smoothly out of view instead of disappearing instantly, so that the action feels controlled and physically grounded.
2. As a user archiving a contact, I want the card to slide and tuck upwards, so that I have a clear mental model that the contact now lives in the top archived partition.
3. As a user viewing my contacts list, I want remaining contact cards to smoothly slide up into the vacant slot when a card leaves, so that the list reflow looks natural and avoids jarring visual jump-cuts.
4. As a user deleting a contact permanently, I want the card to scale down and dissolve with a fade, so that the visual effect clearly signals irreversible destruction rather than temporary shelving.
5. As a user deleting multiple contacts via the batch selection tray, I want all selected cards to animate out simultaneously with staggered vertical collapse, so that batch operations feel cohesive and responsive.
6. As a user confirming deletion in an alert dialog, I want the dialog dismissal to seamlessly lead into the card exit animation on the underlying list, so that the transition feels continuous.
7. As a user archiving a contact from `PersonEditScreen`, I want the edit screen to pop back smoothly while the contact list reflects the departure animation, so that the relationship directory stays in sync.
8. As a user on `ArchivedContactsScreen`, I want tapping "Restore" to animate the restored card out of the archived list with smooth row collapse, so that I see immediate physical feedback before returning to the main list.
9. As a user who restored a contact, I want the restored card to smoothly appear in the active relationship directory with an entrance animation, so that I can easily spot where it returned.
10. As a user tapping "Undo" on a deletion or archive toast pill, I want the affected card to spring back into its original slot, so that reversing an action feels delightful and immediate.
11. As a user with motion sensitivity who enabled "Reduce Animations" in Settings, I want archive and delete operations to complete immediately without movement or scaling, so that the interface remains comfortable to use.
12. As a rapid user performing multiple quick actions, I want exit animations to be interruptible and non-blocking, so that my ability to scroll, tap, or search is never held hostage by motion.

---

## Implementation Decisions

### 1. Motion Tokens & Physics Specifications
- **Archive Exit:**
  - Slide offset: $-20\%$ horizontal translation combined with slight negative Y translation (upward tuck).
  - Fade: $1.0 \rightarrow 0.0$ alpha over $180\text{ms}$ with `FastOutSlowInEasing`.
  - Row collapse: Vertical height collapses via `AnimatedVisibility` / `Modifier.animateItem` with spring physics (`dampingRatio = 0.82f`, `stiffness = Spring.StiffnessMediumLow`).
- **Delete Exit:**
  - Scale: $1.0\times \rightarrow 0.92\times$ scale reduction.
  - Fade: $1.0 \rightarrow 0.0$ alpha over $150\text{ms}$ with `LinearOutSlowInEasing`.
  - Row collapse: Immediate vertical collapse following scale with spring physics.
- **Restore Exit (from Archive Screen):**
  - Slide out horizontally towards the right edge with vertical collapse.

### 2. State & Component Integration
- Utilize Compose `LazyColumn` item animation capabilities (`Modifier.animateItem(fadeInSpec, fadeOutSpec, placementSpec)`):
  - Ensures items have stable identity keys (`key = { _, contact -> contact.id }`).
  - Animates item removal and subsequent item placement without blocking Room persistence or coroutine scopes.
- Maintain an in-memory exiting item registry or transient transition state in ViewModels / Composables where custom exit choreographies (e.g. upward tuck for archive) are needed before Room data removal propagates.

### 3. Graceful Fallback
- Check `userPreferences.reduceAnimations` centrally. If true, motion duration is clamped to $0\text{ms}$ / `snap()`, bypassing animation delays.

---

## Testing Decisions

### What Makes a Good Test
- Tests should verify that archive and delete departure requests trigger the appropriate animation state machine transitions, that list item keys remain stable, and that reduced-motion preferences bypass animation delays.

### Modules Tested
- **ContactsScreen / ContactCard Motion Seam:** Verifies exit spec mapping (archive vs delete).
- **ArchivedContactsScreen Motion Seam:** Verifies restore exit collapse.
- **Reduced Motion Configuration:** Verifies instantaneous state completion when reduced motion is enabled.

### Prior Art
- Existing `PullToActionStateTest` and `NavigationTransitionsTest` validating Compose animation specs and spring parameter stability.

---

## Out of Scope

- Ragdoll physics, confetti particles, or complex 3D skeletal animations.
- Custom sound effects on card deletion (tactile haptic feedback handles physical sensation).
- Animation on typing or keyboard filtering (search filtering remains instant).

---

## Further Notes

- Animation durations strictly follow Emil Kowalski's guidelines: exit animations start fast (`ease-out`), total duration stays under $250\text{ms}$, and no interaction is blocked while an exit is underway.

