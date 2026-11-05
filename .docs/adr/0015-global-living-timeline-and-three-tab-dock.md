# ADR-0015: Three-Tab Floating Bottom Dock and App-Wide Living Timeline

- **Status:** Accepted
- **Date:** 2026-11-05
- **Context:** Introducing an app-wide chronological Living Timeline screen (`Timeline Tab`) that unifies future relationship horizons and past encounter logs across all Contacts.

---

## 1. Problem & Decision Drivers

Previously, Hello's **Living Timeline** existed only inside an individual `Contact Profile Screen` (`PersonDetailScreen`), showing past encounters for a single person. Users lacked a single chronological stream across their entire network to see:
1. Everyone they recently added ("First Met") or caught up with ("Met Again") in chronological order.
2. What lies ahead in the **Future**—scheduled `FollowUp` notes, upcoming `Birthdays`, and projected next `Cadence` reconnections—on the same living timeline spine.
3. Fast top-level navigation to this stream from the `Floating Bottom Dock`.

---

## 2. Decisions

### Decision 1: Three-Tab Floating Bottom Dock (`Home` | `Timeline` | `People`) with Persistent Icons & Constant Pill Width
- `HomeNavTab` expands from two tabs to three primary root destinations:
  1. `HOME` ("Home") — Slot 0 (Left): `Icons.Outlined.Home` (inactive) → `Icons.Filled.Home` (active)
  2. `TIMELINE` ("Timeline") — Slot 1 (Center): `Icons.Outlined.Schedule` (inactive) → `Icons.Filled.Schedule` (active)
  3. `PEOPLE` (Dynamic `RelationshipTerminology` label: People / Friends / Connections / Contacts) — Slot 2 (Right): `Icons.Outlined.People` (inactive) → `Icons.Filled.People` (active)
- **Icon/Text Inversion & Constant-Width Pill Invariant:**
  - Icons are **always visible** on all three tabs: inactive tabs display only their **Outlined** icon inside a compact `1:1` slot (`inactiveSlotWidthDp`), while the active tab displays its **Filled** icon + expanding **Text Label** (`[Filled Icon + Label]`).
  - Both the **outer Floating Bottom Dock container** (`activePillWidth + 2 * inactiveSlotWidth + padding`) and the **inner sliding Selection Pill** (`activePillWidth`) maintain a **100% constant width** across all states (`0f`, `1f`, `2f`). At any progress `p` in `0f..2f`, the pill's left offset is `p * inactiveSlotWidth` and its width is `activePillWidth`.

### Decision 2: Unified Vertical Spine with `Future` → `TODAY` → `Past`
- The **Timeline Tab** renders a continuous vertical Living Timeline spine divided by a prominent **`TODAY`** anchor badge:
  - **Future Horizon (Above `TODAY`, Dashed Spine, Soonest-First):**
    - Ordered **soonest-first** from top to bottom (e.g., *Tomorrow* → *In 3 days* → *In 14 days* → **`TODAY`**) so urgent upcoming items are immediately visible at the top of the screen.
    - Combines three future node types:
      1. **Scheduled FollowUps / Next Notes:** Displays the contact avatar, name, due date, and task note, with inline **`Done`** and **`Snooze`** action chips.
      2. **Upcoming Birthdays:** Contacts with birthdays within the next 60 days.
      3. **Projected Cadence Reconnections:** Next expected appearance/catch-up date derived from the Contact's effective cadence (`resolveEffectiveCadenceDays`) and their most recent encounter, with an inline **`Log Met`** action chip.
  - **Past Log (Below `TODAY`, Solid Spine, Newest-First):**
    - Ordered **newest-first** from `TODAY` downward (*Met today* → *Met yesterday* → *4 days ago* → older dates).
    - Every recorded `Encounter` across all Contacts is rendered with the Contact's avatar, name, date, optional `Place` badge, and encounter notes.
    - Distinguishes **First Met / Added** (the earliest chronological encounter recorded for that Contact) from **Met Again** (subsequent encounters for that Contact) via a semantic badge on each node.

### Decision 3: Ultra-Minimal Temporal Filter Strip (`All` | `Future` | `Past`)
- The Timeline header remains ultra-minimal with a 3-pill temporal filter strip:
  - **`All`** (default): Shows the Future Horizon above `TODAY` and the Past Log below `TODAY`.
  - **`Future`**: Filters the spine to only upcoming Future Horizon nodes.
  - **`Past`**: Filters the spine to only recorded Past Log encounter nodes.
- Circle/tag filter chips are intentionally omitted from the Timeline header to keep the chronological reading experience clean and uncluttered.

### Decision 4: Interactive Nodes with Profile Navigation & Inline Actions
- Tapping any node card on the Timeline navigates directly to that Contact's `Contact Profile Screen` (`PersonDetailRoute`).
- Future `FollowUp` nodes expose inline **`Done`** and **`Snooze`** actions, and Future `Cadence` nodes expose an inline **`Log Met`** button that opens the Flow B Encounter Logger pre-selected for that Contact.

---

## 3. Consequences

### Positive
- **Complete Temporal Picture:** Users can scan both their relationship history ("Who did I meet this week?") and their upcoming horizon ("Whose birthday or follow-up is coming up next?") on a single visual spine.
- **Consistent Visual Language:** Reuses the tactile vertical spine and node aesthetic from the per-contact `LivingTimeline` while scaling it to the entire network.
- **Zero Clutter:** Keeping the filter bar strictly to `All | Future | Past` prevents filter fatigue.

### Negative / Tradeoffs
- `DockMotionConfig` must support 3 tab slots instead of assuming a 2-tab left/right anchor, requiring updated slot center and pill bound calculations.
