# 02: Settings Name Editor with Non-Empty Validation

**What to build:** Add a "Profile" / "Your Name" card in `SettingsScreen.kt` displaying the current user name. Tapping it triggers an edit dialog with an `OutlinedTextField` allowing the user to update their name. The Save button is strictly disabled if the text is empty or blank, guaranteeing the name cannot be cleared. Saving commits the new name to `UserPreferencesRepository`, instantly updating Settings and the Homepage greeting.

**Blocked by:** 01-username-persistence-and-home-greeting

**Status:** done

- [x] Settings screen includes a Profile / Name card displaying the current user name.
- [x] Tapping the card opens a Material 3 edit dialog with text input.
- [x] Validation prevents saving blank or empty text (Save button disabled if `text.trim().isEmpty()`).
- [x] Saving updates `UserPreferences` and dismisses the dialog.
- [x] The updated name reflects immediately in Settings and on the Home screen.
- [x] Unit tests pass cleanly (`./gradlew testDebugUnitTest`).
