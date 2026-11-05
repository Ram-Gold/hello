# 0019. Zero-Config Subconscious Check-Ins and Set-in-Stone Timeline Birthdays

Date: 2026-11-06
Status: Accepted

## Context

Manual per-contact and per-group "Cadence" configuration (`7d`, `14d`, `30d`, `90d`), overdue cadence health cards on profiles, and projected cadence nodes on the Living Timeline created cognitive friction and enterprise-CRM guilt. Users do not want to manage manual reconnection SLAs; instead, they want:
1. **Subconscious, zero-configuration check-in nudges** that appear gently from time to time in the Home `"Keep in Touch"` carousel (`"Met X ago — anything new to update?"`) without manual interval pickers.
2. **Set-in-stone Birthdays** anchored prominently on both the Global Living Timeline (`TimelineScreen`) and each individual Contact Profile's `LivingTimeline`.

## Decision

1. **Zero-Config Subconscious Check-In Algorithm (`SubconsciousCheckInCalculator`):**
   - Remove manual cadence interval selectors from Contact Edit (`PersonEditScreen`), Contact Profile (`PropertiesPanel`), Settings (`SettingsPreferencesScreen`), and Capture Flow step pickers (`SettingsFlowsScreen`).
   - Preserve `cadenceDays: Int?` in SQLite (`ContactEntity`) and `ExportBundleGenerator` for 100% backward compatibility with existing database rows and Markdown/JSON export bundles.
   - Compute adaptive check-in intervals automatically from recorded encounter history:
     - For contacts with `2+` recorded encounters on distinct days: `round(1.5 * averageGapDays)` clamped to `14..90` days.
     - For contacts with `0–1` encounters: `30-day` baseline since `lastMetAt` (or `firstMetAt`).
   - Surface at most **2** subconscious check-in cards at a time in the Home `"Keep in Touch"` carousel, ranked by drift ratio (`daysSince / thresholdDays`), with `"Check in"` badge, `"Later"` (14-day snooze), and `"Update"` actions.

2. **Set-in-Stone Birthdays on Living Timelines:**
   - **Global Living Timeline (`TimelineViewModel`, `TimelineScreen`):** Remove `FutureTimelineNode.CadenceNode` so the Future Horizon above `TODAY` is reserved strictly for concrete dated events (**Birthdays** and **Scheduled Follow-Ups**). Expand the `BirthdayNode` window from 60 days to **365 days** so every active contact with a birthday has their next annual birthday anchored on the timeline.
   - **Individual Contact Profile Living Timeline (`LivingTimeline`):** Pin a `BirthdayMilestoneNodeItem` at the top of a contact's `LivingTimeline` when `contact.birthday != null`, connected via a dashed future spine into the solid past encounter spine below.

## Consequences

- **Positive:** Eliminates manual cadence configuration across 5 surfaces while keeping gentle, organic relationship maintenance on the Home screen and elevating birthdays to first-class timeline milestones.
- **Compatibility:** Existing Room tables and Markdown/JSON export schemas remain 100% backward compatible.
