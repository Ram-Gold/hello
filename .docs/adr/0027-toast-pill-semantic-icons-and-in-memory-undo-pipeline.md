# 0027: Toast Pill Semantic Icons and In-Memory Undo Pipeline

**Date:** 2026-11-08  
**Status:** Accepted  
**Context:**  
The application uses the `ToastPill` component (ADR-0026) for transient feedback across screens. Previously, toasts rendered only basic `Info` or `CheckCircle` icons without semantic distinction for specific operations (e.g. archiving, deleting, favoriting), and lacked a recovery mechanism when destructive operations occurred. Users required clear visual confirmation of the specific action taken, as well as the ability to quickly undo accidental archiving or deletion of Contacts or Encounters.

**Decision:**
1. **Context-Aware Semantic Icons:**
   - Map leading icons based on operation context:
     - `Archive`: `Icons.Rounded.Archive`
     - `Delete`: `Icons.Rounded.Delete`
     - `Star / Favorite`: `Icons.Rounded.Star`
     - `Success / Save`: `Icons.Rounded.CheckCircle`
     - `Alert / Warning`: `Icons.Rounded.Info`
2. **5-Second Transient Undo Mechanism:**
   - Surface an "Undo" text action button on the right side of the Toast Pill during destructive events (archive, delete).
   - Display the action button in bold terracotta primary brand color, separated with subtle spacing.
   - Set the duration to a standard 5 seconds.
3. **In-Memory Restore Pipeline:**
   - Maintain an in-memory repository cache of recently deleted/archived entities (`Contact`, `Encounter`).
   - Tapping "Undo" immediately invokes a zero-latency restoration in Room SQLite (un-archiving or re-inserting the cached entity).
   - Navigation remains on the current screen (destination list or timeline), reactively updating the UI state and displaying a follow-up confirmation toast ("Contact restored" / "Encounter restored") without disruptive screen jumps.
4. **Destination Screen Presentation:**
   - When archiving or deleting from an edit or detail screen that navigates back, trigger the Toast Pill on the destination screen where the user lands, ensuring the toast remains visible and actionable without blocking navigation.

**Consequences:**
- **Positive:** Protects user data sovereignty with zero accidental data loss, provides immediate tactile feedback with rich semantic icons, and adheres to zero-network local-first invariants.
- **Negative:** Requires an in-memory cache in the repository layer to hold temporarily deleted records during the 5-second undo window.

