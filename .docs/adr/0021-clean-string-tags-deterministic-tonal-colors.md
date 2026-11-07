# ADR-0021: Clean-String Tags with Deterministic Material 3 Expressive Tonal Colors

## Status
Accepted

## Context
Following the consolidation of all categorization primitives onto Tags (ADR-0020), tags were displayed across the application using social-media-style hashtag syntax (e.g. `#work`, `#coffee`, `#design`). In addition, tags lacked consistent color representation across contact cards and selection chips, often falling back to neutral container colors or a single hardcoded terracotta hue without automatic pre-coloring when created.

This presented several UI and domain frictions:
1. **Visual Clutter**: The `#` prefix added visual noise across contact cards, filter bars, profile panels, and export notes without adding semantic value.
2. **Inconsistent Color Application**: While `Tag` included a `colorHex` attribute in Room and domain models, several surfaces (e.g. `ContactCard`, `PersonEditScreen` filter chips) disregarded it or required manual palette selection in dialogs.
3. **Friction in Ad-Hoc Tagging**: Quick Capture and inline tag creation did not assign intuitive, distinct colors without requiring manual palette intervention.

## Decision
1. **Strict Clean-String Tag Identity**:
   - Strip the `#` symbol everywhere: in UI labels, chips, search inputs, dialogs, and export bundles (Markdown frontmatter, JSON, CSV).
   - Sanitize all input fields on submit and provide transparent startup sanitization in `ContactRepository` to strip any legacy `#` prefixes from existing `TagEntity.name` and contact context strings.
2. **Deterministic Tonal Palette Auto-Coloring ("Pre-Colored")**:
   - Pre-seed canonical tags (Work, Family, Friends, Tech, Design, Coffee, AI, etc.) with distinct warm palette hues from `TagColorPalette`.
   - Automatically assign new ad-hoc tags a deterministic color using a hash of the clean tag name mapped over `TagColorPalette.PALETTE_HEX`, guaranteeing instant, stable color assignment without user friction.
   - Users can still manually override or fine-tune tag colors in Settings (`SettingsTagsScreen` and `TagDialogs`).
3. **Material 3 Expressive Tonal Tag Pill**:
   - Standardize all tag presentations across Contact Cards, Contact Profile, Edit Screen, Quick Action, and Settings onto the `TonalTagPill` component.
   - **Unselected / Display State**: Soft tinted background (~14–18% alpha of tag color), matching saturated colored text, and subtle rounded squircle shape (`RoundedCornerShape(8.dp)` to `12.dp`).
   - **Selected / Active State** (in filter strips & edit pickers): Full solid fill using the tag's color with contrasting white/dark text and a checkmark indicator.

## Consequences
- Tags feel intentional, modern, and harmonious with Hello's warm earthy M3 Expressive design language.
- Quick capture and ad-hoc tagging remain frictionless with zero required color decisions.
- Database records and export bundles remain completely clean, readable, and portable.

