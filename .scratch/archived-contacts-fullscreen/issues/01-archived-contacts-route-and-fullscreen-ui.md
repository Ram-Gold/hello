# 01: Archived Contacts Route and Full-Screen UI

**What to build:**
Create `ArchivedContactsRoute` and develop `ArchivedContactsScreen` as a dedicated full-screen destination. Includes a Material 3 Expressive top bar with back button, terminology title (`Archived ${terminology.pluralTitle}`), total count pill, full-height scrollable list of archived contacts with avatars, context details, and terracotta `"Restore"` action buttons, plus an empty state when all contacts are restored.

**Blocked by:** None (can start immediately)

**Status:** closed

- [x] Define `ArchivedContactsRoute` in `AppNavigation.kt` with route path `contacts/archived`.
- [x] Build `ArchivedContactsScreen` composable with edge-to-edge support, top app bar, and system back handler.
- [x] Implement full-height list displaying archived contact cards with avatar, name, subtitle, and instant restore action.
- [x] Provide empty state view displaying when no archived contacts remain.

