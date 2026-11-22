# 0036. Weighted-Random Serendipity and Full Removal of Cadences

Date: 2026-11-22
Status: Accepted (Supersedes ADR-0019 check-in and cadence compatibility sections)

## Context

Manual per-contact cadence selectors (`7d`, `14d`, `30d`, `90d`, custom) still remained in Contact Edit (`PersonEditScreen`), along with legacy `cadenceDays` fields across `Contact`, `ContactEntity`, `ExportBundleGenerator`, `PersonDetailViewModel`, and `UserPreferencesRepository`. Meanwhile, even deterministic "overdue drift" check-in phrasing (`"Anything new to update?"` + one-tap `"Update"` logging a synthetic `"Caught up recently"` encounter) felt mechanical and CRM-like rather than organic.

Users want:
1. **Zero manual cadences** anywhere in the app, domain model, Room schema, or exports.
2. **Zero OS push notifications for ambient reconnect prompts** (Android notifications fire strictly for explicit user-scheduled FollowUps).
3. **Quiet, weighted-random serendipity** on the Home screen that surfaces 1-2 friends you haven't seen in a while without overdue guilt, letting you skip to roll another friend or view their profile to reach out organically.

## Decision

1. **Full Cadence Purge & Room `MIGRATION_6_7`:**
   - Remove `cadenceDays` from `Contact`, `ContactEntity`, `ExportBundleGenerator` (JSON + YAML frontmatter), and `MockDataProvider`.
   - Bump `HelloDatabase` from version `6` to `7` with `MIGRATION_6_7` rebuilding the `contacts` table without `cadenceDays` while preserving all existing contact rows.
   - Remove `CadenceIntervalSelector` and cadence state/events from `PersonEditScreen`, `PersonEditViewModel`, and `PersonEditUiContract`.
   - Remove unused `CadenceHealth`, `CadenceStatusInfo`, `CadenceHealthCard`, and `defaultCadenceDays` across Contact Profile, Settings, and `UserPreferencesRepository`.
   - Update `FollowUpNotificationHelper` channel description and fallback copy to refer strictly to scheduled follow-up reminders.

2. **Weighted-Random Serendipity (`SerendipityPicker` + `UpcomingAlert.SerendipityPrompt`):**
   - Replace `SubconsciousCheckInCalculator` with `SerendipityPicker` and `UpcomingAlert.CadenceAlert` with `UpcomingAlert.SerendipityPrompt`.
   - Eligibility: active (non-archived) Contacts without a pending `FollowUp`, not skipped today, and with `daysSince >= 14` since `lastMetAt ?: firstMetAt`.
   - Weighting: proportional to `daysSince.coerceIn(14, 365)` so longer gaps are favored without letting multi-year gaps monopolize draws.
   - Daily stability + Skip: deterministic daily weighted sampling without replacement seeded by `today.toEpochDay()`, capped at 2 cards. Tapping `"Skip"` adds the Contact to today's skipped set in `ContactRepository` and immediately rolls the next eligible weighted-random pick.
   - Warm low-pressure card UX: `"Say hi"` badge (`Icons.Rounded.AutoAwesome`), light memory context (`"Last met 3 weeks ago"` / `"Added 2 months ago"`), `"Skip"` outlined button, and `"View"` tonal button that opens the Contact Profile Screen without auto-logging synthetic encounters.

## Consequences

- **Positive:** Completely removes the transactional "cadence / overdue" mental model from both UI and schema, keeps Home fresh and warm through daily weighted-random serendipity, and ensures OS notifications remain strictly user-initiated.
- **Migration:** Existing v6 SQLite databases migrate cleanly to v7 via `MIGRATION_6_7` with zero data loss.
