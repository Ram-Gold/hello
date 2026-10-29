# 0010. Contact Profile Card Harmonization and Minimal Communication Design

Date: 2026-10-29

## Status
Accepted

## Context
Hello was originally architected with traditional phone, SMS, and email action shortcuts in the contact profile header (`DetailCollapsingHeader`), alongside outlined cards (`1.dp` borders, elevated surfaces) for Cadence health, circles, tags, bio/notes, and timeline encounters. 

As a dedicated personal relationship CRM—not a phone dialer or email client—these prominent communication buttons encouraged tactical utility over relationship reflection. Furthermore, the cards across the profile screen still carried legacy 1.dp borders and inconsistent surface backgrounds, conflicting with the Material 3 Expressive connected card standard established in ADR-0008 and ADR-0009.

## Decision
1. **Remove Communication Quick Actions from Profile**:
   - Completely remove the "Call", "Message", and "Email" action row from `DetailCollapsingHeader`.
   - Remove dialer/SMS/email intents from profile viewing, focusing the surface entirely on relationship context, Cadence health, circles, tags, notes, and past encounters.
   - Retain phone and email data fields strictly in edit mode (`PersonEditScreen`) for identity record-keeping without cluttering the viewing experience.

2. **Full Card System Harmonization on Profile (`surfaceContainer`, 0dp Borders)**:
   - Modernize all profile cards in `PropertiesPanel` (`CadenceHealthCard`, Circles & Tags card, Bio/Notes card) to `Surface` with `MaterialTheme.colorScheme.surfaceContainer`, `0.dp` border, and `20.dp` squircle corners.
   - Modernize `LivingTimeline` encounter cards and `EmptyTimelineCard` to `surfaceContainer` (0dp border, `20.dp` squircle corners).
   - Use `surfaceContainerHigh` for internal chip backgrounds (circles, tags, location pills).

3. **FullScreenCardDialog Minimalist Upgrade**:
   - Update `FullScreenCardDialog` to `surfaceContainer` with 0dp border and 28dp squircle corners.
   - Remove the contact details card (phone and email rows) from the dialog, dedicating the view to high-contrast QR offline exchange.

## Consequences
- **Positive**:
  - The Contact Profile screen feels focused, tactile, and peaceful, free from utility app clutter.
  - Complete aesthetic unity across all app surfaces (Settings, Home Feed, Contact Cards, and Contact Profile).
  - Clean separation between relationship tracking and device system dialers.
- **Negative / Mitigations**:
  - Direct calling or messaging cannot be launched from the profile viewing surface. Users can use their phone's native dialer or access phone/email when opening Edit mode.
