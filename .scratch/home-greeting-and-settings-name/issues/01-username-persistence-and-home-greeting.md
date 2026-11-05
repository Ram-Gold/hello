# 01: User Name Persistence in UserPreferences and Reactive Home Greeting

**What to build:** Add `userName: String = "Alex"` to `UserPreferences` and `UserPreferencesRepository` with local DataStore persistence (`setUserName(name: String)`). Connect `HomeViewModel` to observe `userPreferencesFlow` so `uiState.userName` updates reactively. On `HomeScreen.kt`, clean up the greeting headline: remove the waving hand emoji `👋` and remove the subtitle `"Keep your relationships warm"`, rendering just the clean, bold `"Hello, <name>"`.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] `UserPreferences` includes `userName: String = "Alex"` with DataStore read/write persistence.
- [x] `HomeViewModel` collects `userPreferencesFlow` to update `userName` dynamically.
- [x] `HomeScreen.kt` removes the waving hand emoji `👋` and removes `"Keep your relationships warm"`.
- [x] The greeting displays bold `"Hello, <name>"` cleanly without emoji or subtext.
- [x] Unit tests pass cleanly (`./gradlew testDebugUnitTest`).
