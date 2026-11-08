# Specification: Pull-to-Action Hidden Archived Contacts

**Status:** Approved  
**Related ADR:** [ADR-0028: Archived Contacts Pull-Up Sheet and Restore Flow](file:///home/ram/Projects/Hello/.docs/adr/0028-archived-contacts-pull-up-sheet-and-restore-flow.md)  
**Domain Model:** [GLOSSARY.md](file:///home/ram/Projects/Hello/GLOSSARY.md) (`Archived Contacts Sheet`, `Restore Action`)

---

## 1. Overview & Motion Mechanics

Archived contacts are completely hidden from the standard relationship directory. To view archived contacts, users employ the **Pull-to-action** mobile pattern at the top of the contacts list on the Relationship Tab:
- **Zero Idle Footprint:** The idle screen has zero footer bars or archive buttons cluttering the view.
- **Rubber-banding:** Dragging down when at the top of the list introduces tactile exponential resistance (iOS overscroll feel).
- **Scroll-driven Animation:** Pull progress (0.0 to 1.0) directly rotates/scales an Archive icon and scales an indicator message above the list.
- **Activation Threshold & Haptics:** Reaching the threshold (~72dp) transitions the message from `"Pull for archive"` to `"Release for archive"`.
- **Release Action:** Releasing past the threshold smoothly triggers opening the Archived Contacts Sheet while content snaps back via spring physics. Releasing before the threshold snaps back to rest without triggering.

---

## 2. Technical Architecture

1. **`PullToActionNestedScrollConnection`**:
   - A Compose `NestedScrollConnection` that intercepts pre-scroll and post-scroll delta `dy` at the top boundary (`LazyListState.firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0`).
   - Calculates rubber-band pull distance using diminishing returns resistance:
     `pullDistance += delta * (1f - (currentOffset / maxOffset).coerceIn(0f, 0.85f))`.
   - Tracks `pullOffset: Animatable<Float, AnimationVector1D>`.
2. **Scroll-Driven Header (`PullToActionArchiveHeader`)**:
   - Positioned above the contacts list / search bar, clipped when offset is 0.
   - Dynamic height driven by `pullOffset.value`.
   - Displays `Icons.Rounded.Archive` rotating from 0 to 180 degrees (or scaling up) as threshold approaches.
   - Dynamic text label switching at threshold with subtle haptic feedback.
3. **Trigger & Restore Pipeline**:
   - On release past threshold, invokes `onTriggerAction()`, opening `ArchivedContactsSheet`.
   - Removing the previous bottom pull-up footer to keep the contacts screen completely minimal.

