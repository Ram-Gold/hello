# 0009. Uniform Material 3 Expressive Card System & Dimension Invariants

Date: 2026-10-29

## Status
Accepted

## Context
Following the transition to Material 3 Expressive connected contact cards (ADR-0008), the home screen and empty-state surfaces retained legacy card implementations (`Card` with `1.dp` borders, `elevation = 1.dp/2.dp`, and gradient fills). In particular, the horizontal `UpcomingFeedSection` suffered from dimension instability: `UpcomingAlertCard` variants (Birthday, FollowUp reminder, Cadence alert) had varying description line counts and button structures. When descriptions were long, cards expanded vertically, creating jagged, inconsistent card heights within the horizontal `LazyRow`. Furthermore, alert cards lacked prominent visual iconography harmonized with the 56dp avatar hierarchy of `ContactCard`.

## Decision
1. **System-Wide Card Harmonization (`surfaceContainer`, Zero Borders, Zero Elevation)**:
   - Modernize all remaining cards in Hello—including `MyCardWidget`, `UpcomingAlertCard`, and empty states (`UpcomingEmptyState`, `EncountersEmptyState`, `ContactsEmptyState`)—to use `Surface` with `MaterialTheme.colorScheme.surfaceContainer`, `0.dp` borders, `0.dp` elevation, and squircle corner radii (`20.dp` to `24.dp`).
   - Deprecate legacy `1.dp outlineVariant` borders and gradient overlays.

2. **Upcoming Alert Card Dimension Invariants**:
   - Standardize `UpcomingAlertCard` to a fixed dimension of `width = 260.dp` and `height = 156.dp`.
   - Constrain context/task descriptions to a strict 1-line truncation with `TextOverflow.Ellipsis`.
   - Anchor action buttons at the card bottom via a flexible vertical spacer (`Spacer(Modifier.weight(1f))`), guaranteeing uniform visual baseline across Birthday, FollowUp, and Cadence alerts in the carousel.

3. **Prominent Tonal Icon Hierarchy**:
   - Equip every `UpcomingAlertCard` with a prominent 40dp circular tonal icon container at the top:
     - **BirthdayAlert**: `Icons.Outlined.Cake` in `secondaryContainer`
     - **FollowUpReminder**: `Icons.Outlined.Notifications` in `primaryContainer`
     - **CadenceAlert**: `Icons.Outlined.Refresh` in `tertiaryContainer`
   - Pair with an inline date or category chip in `surfaceContainerHigh` for rapid scanning.

## Consequences
- **Positive**:
  - Elimination of layout shifts and uneven heights in the horizontal feed.
  - Consistent visual rhythm across Home Screen (`MyCardWidget`, `UpcomingAlertCard`, `ContactCard`).
  - Seamless dark/light theme fidelity with native M3 surface container roles.
- **Negative / Mitigations**:
  - Long reminder descriptions are truncated after one line. Users can inspect the full details by opening the contact sheet.
