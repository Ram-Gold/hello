# 0029: Archived Contacts Full-Screen Destination and Restore Flow

**Date:** 2026-11-08  
**Status:** Accepted  
**Supersedes:** ADR-0028  
**Context:**  
ADR-0028 introduced archived contact access via an ephemeral modal bottom sheet. However, presenting relationships within a modal bottom sheet constrained vertical viewing area, introduced nested scrolling compromises, and felt transient rather than a full, dignified directory view. Users requested that archived contacts be elevated to a dedicated full-screen destination while preserving the hidden top pull-to-action gesture.

**Decision:**
1. **Dedicated Full-Screen Destination (`ArchivedContactsScreen`):**
   - Registered as a full-screen NavHost route `contacts/archived` in `AppNavigation.kt` (`ArchivedContactsRoute`).
   - Driven with shared-axis slide and fade transitions.
   - Includes a Material 3 Expressive top bar with system back navigation, terminology-aware title (`Archived ${terminology.pluralTitle}`), and total count badge (`Total: N`).
2. **Dock Visibility Management:**
   - The Floating Bottom Dock is automatically hidden on this destination via `shouldHideFloatingDock`, maximizing vertical real estate for viewing contacts.
3. **Pull-to-Action Navigation Trigger:**
   - Releasing the top boundary overscroll gesture past the threshold navigates directly to `ArchivedContactsRoute` via `NavController.navigateSafely`.
4. **Instant Multi-Restore & Bi-Directional Undo:**
   - Restoring a contact updates Room SQLite (`isArchived = false`), reactively removing the card from the full-screen view and restoring it to the active directory.
   - Surfaces a semantic Toast Pill with `"Undo"`.
   - Tapping `"Undo"` executes a reverse archive operation via `ContactRepository.undoLastArchive`, seamlessly returning the contact to the archive state.
5. **Zero Idle Clutter:**
   - When idle, the Relationship Tab maintains a zero-footprint presentation with no bottom sheets or permanent archive chrome.

**Consequences:**
- **Positive:** Unconfined full-screen experience with native top bar and back navigation; preserves zero idle clutter; supports bi-directional undo on restore; adheres to Android Jetpack Navigation standards.
- **Negative:** Full-screen navigation adds a new back stack entry (mitigated by instant 0ms back-press pop).

