# 0011. Contacts Screen Pixel-Style Expressive Header Architecture

Date: 2026-10-29

## Status
Accepted

## Context
The Relationship Tab (`ContactsScreen.kt`) originally used a custom header containing a small `"← Home"` badge on the top right, paired with asymmetrical 20.dp screen margins and an ad-hoc layout distinct from the app's Settings design language. 

With the adoption of the Material 3 Expressive top bar pattern across all multi-screen settings views (ADR-0007), the contacts/relationship screen felt visually disconnected when navigating between tabs and settings.

## Decision
1. **Settings-Style Top Bar Pattern on Contacts Screen**:
   - Modernize the header of `ContactsScreen.kt` to mirror `PixelStyleTopBar`.
   - Anchor a 44dp circular `FilledIconButton` with an auto-mirrored back arrow on the far left, navigating back to Home.
   - Present the active relationship terminology (`terminology.pluralTitle`: People, Friends, Connections, or Contacts) in bold `headlineMedium`.
   - Embed an M3 tonal counter badge pill in `surfaceContainerHigh` on the far right displaying the total connections count.

2. **Unified Margin Hierarchy**:
   - Standardize horizontal screen margins from 20.dp to 16.dp across the header, pill search bar, and filter chips, achieving 1:1 visual alignment with the Settings suite.

## Consequences
- **Positive**:
  - Immediate visual familiarity and consistent navigation rhythm across all non-Home destinations.
  - Clear, prominent hierarchy for the active relationship vocabulary.
  - Tonal counter pill provides quick network size feedback without taking up vertical line space.
