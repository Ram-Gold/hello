# ADR-0023: Timeline Selection Mode, Multi-Entity Batch Deletion, and Recency Reconciliation

- **Status:** Accepted
- **Date:** 2026-11-08
- **Context:** Enabling users to hold, select, and batch delete timeline items (Encounters and FollowUps) globally across both the app-wide Living Timeline (Timeline Tab) and Contact Profile Screen, while preserving Contact recency integrity and adhering to Hello's zero-backend local-first principles.

---

## 1. Problem & Decision Drivers

In Hello's Timeline Tab (`ADR-0015`) and Contact Profile Screen (`ADR-0010`), the Living Timeline renders encounters and milestones.
Users need a consistent, global way to prune erroneous or obsolete encounters and follow-ups directly from wherever the timeline is viewed—both in the global chronological feed and within individual contact profiles.
Key design drivers:
1. **Interaction Parity with Contacts (`ADR-0014`):** Long-pressing must activate a consistent selection mode with predictable affordances (dock translation, right-docked action tray, contextual top bar, inline checkmarks).
2. **Entity Heterogeneity & Boundary Safety:** The Living Timeline displays three entity types: `Encounter`, `FollowUp`, and computed `Birthday` milestones. Birthdays are derived from contact attributes and cannot be orphaned or deleted on the timeline.
3. **Relational Recency Invariant:** In Hello, each `Contact` tracks `meetCount` and `lastEncounterText` (e.g. "Met today", "Met 2d ago") to drive the proactive recency engine and Subconscious Check-Ins (`ADR-0019`). Deleting encounters must deterministically recompute the contact's recency state rather than leaving stale metadata.
4. **Permanent Local-First Deletion:** SQLite record deletion is permanent (`ADR-0006`). Accidental deletion must be guarded with explicit confirmation and precise itemized counts.

---

## 2. Decisions

### Decision 1: Long-Press Activation & Selection State Management
- Long-pressing any **Encounter** (Past Log) or **FollowUp** (Future Horizon) activates **Timeline Selection Mode**.
- The long-pressed item is immediately selected.
- Tapping any Encounter or FollowUp while in selection mode toggles its selection state.
- **Birthday Milestone Nodes are non-selectable**: Tapping or long-pressing a birthday node during selection mode is a safe no-op.
- Deselecting the final selected item automatically exits selection mode.
- The system Back button or the contextual top bar's 'X' button cancels selection mode and clears all selections.

### Decision 2: Decoupled Visual Treatment & Theme-Aware Error Container
- Selected Encounter and FollowUp cards switch to theme-aware error container styling (`MaterialTheme.colorScheme.errorContainer` with `onErrorContainer` text and badge tints) matching `ADR-0014`.
- An inline checkmark icon appears beside the contact name in the card header.
- Timeline spine and node dot remain visually stable to preserve chronological continuity.

### Decision 3: Floating Action Tray & Navigation Shell Docking
- **Floating Bottom Dock Dismissal:** While Timeline Selection Mode is active, `MainAppShell` translates the Floating Bottom Dock off-screen using the existing GPU translation pipeline.
- **Floating Vertical Action Tray:** A right-docked floating action tray displays `[ Delete | Kebab (⋮) ]`.
- **Kebab -> Select All:** Tapping "Select all" selects all visible Encounters and FollowUps under the currently active `TimelineFilter` (`ALL`, `FUTURE`, or `PAST`), strictly omitting Birthday nodes.
- **Contextual Top Bar:** Replaces the filter strip with `[X selected]` count on the left and a circular 'X' cancel button on the top right.

### Decision 4: Relational Recency Reconciliation
- When encounters are permanently deleted:
  1. Room deletes the encounter entities (`encounterDao.deleteEncounterById`).
  2. For every affected `contactId`, the repository queries the remaining encounters chronologically.
  3. `meetCount` is updated to `remainingEncounters.size`.
  4. `lastEncounterText` is recomputed from the latest remaining encounter date (or cleared/set to empty if no encounters remain).
- When a FollowUp is deleted:
  1. Any pending Android Alarm notification is cancelled via `FollowUpAlarmScheduler.cancelAlarm(id)`.
  2. Room deletes the follow-up record (`followUpDao.deleteFollowUpById`).

### Decision 5: Confirmation Dialog Breakdown
- Tapping Delete opens a Material 3 `AlertDialog` with itemized counts:
  - If both types are selected: `"Delete X encounters and Y follow-ups? This cannot be undone."`
  - If encounters only: `"Delete X encounter(s)? This cannot be undone."`
  - If follow-ups only: `"Delete Y follow-up(s)? This cannot be undone."`

---

## 3. Consequences

### Positive
- **Complete Behavioral Consistency:** Interaction model matches Contact Selection Mode (`ADR-0014`) identically.
- **Data Integrity:** Contact recency metadata (`meetCount`, `lastEncounterText`) stays perfectly in sync with actual encounter history.
- **Accidental Deletion Protection:** Itemized confirmation dialog and explicit error styling prevent unintentional loss.

### Negative / Tradeoffs
- Tapping an encounter or follow-up in selection mode is repurposed to selection toggling; navigating to the contact requires exiting selection mode first.

