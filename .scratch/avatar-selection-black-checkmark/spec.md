# Spec: Opaque Black Avatar Selection Checkmark with Center Scale-Up

**Status:** `ready-for-agent`

## Problem Statement

When a user enters Contact Selection Mode on the Relationship Tab or Timeline, the transparent selection checkmark overlay on a Contact's avatar can blend into busy contact photos or colored doodle avatars, reducing contrast and making selection state harder to scan at a glance. In addition, the subtle scale-in from `0.85x` feels more like a fade than a crisp scale-up from the center.

## Solution

Update the avatar selection badge overlay so that selecting a Contact covers the avatar with a 100% opaque black circular/shaped background (`Color.Black`), displays a high-contrast Material `Icons.Rounded.Check` icon in the center, and animates in by scaling up from the center (`TransformOrigin.Center`, starting from `0f` or a pronounced small scale) with a smooth ease-out curve.

## User Stories

1. As a user in Contact Selection Mode, I want selected Contact avatars to display a 100% opaque black background, so that the selection state is unmistakably distinct from unselected avatars.
2. As a user in Contact Selection Mode, I want the black selection background to completely cover the underlying avatar photo or initials, so that busy avatar imagery never reduces checkmark legibility.
3. As a user in Contact Selection Mode, I want a crisp Material `Icons.Rounded.Check` icon centered inside the black selection background, so that the selection indicator matches the app's Material icon language.
4. As a user in Contact Selection Mode, I want the checkmark icon to have high contrast against the 100% black background in both light and dark themes, so that it remains legible regardless of system theme or dynamic color.
5. As a user tapping or long-pressing a Contact to select them, I want the black badge and Material checkmark to scale up smoothly from the center of the avatar, so that selection feels responsive, tactile, and anchored to the avatar's center.
6. As a user deselecting a Contact, I want the black badge and Material checkmark to scale down smoothly toward the center and fade out, so that deselection mirrors the entry motion cleanly.
7. As a user with reduced motion enabled, I want the selection overlay to appear and disappear immediately without scaling motion, so that accessibility preferences are respected.
8. As a user viewing selected items on both the Relationship Tab and the Timeline screen, I want both surfaces to share the exact same black selection overlay and center scale-up animation, so that selection behavior is consistent across the app.

## Implementation Decisions

- **Avatar selection badge component**:
  - Default the selection overlay container color to 100% opaque black (`Color.Black`) rather than transparent.
  - Render `Icons.Rounded.Check` centered inside the overlay with a high-contrast foreground tint (such as white) while keeping the `badgeContainerColor` and `badgeContentColor` parameters overridable for callers.
  - Keep the subtle surface border around the opaque container so the black badge separates cleanly from adjacent card surfaces.
- **Center scale-up motion tokens and defaults**:
  - Configure the avatar selection enter transition to scale up from `TransformOrigin.Center` starting from `0f` (or a small central origin) to `1f` using an ease-out / `FastOutSlowInEasing` curve, paired with a quick fade-in.
  - Configure the exit transition to scale back down toward `TransformOrigin.Center` and fade out.
  - Preserve the `isReducedMotion` bypass returning no transition (`EnterTransition.None` / `ExitTransition.None`).

## Testing Decisions

- **Testing seam**: Test at the `SelectableAvatarBadge` + `SelectableAvatarBadgeDefaults` seam (Compose UI tests for rendered visual state + unit tests for transition defaults).
- **What makes a good test**:
  - Assert external behavior: when `isSelected = true`, the `"Selected"` checkmark content description is present and visible, the overlay uses an opaque black container, and `Icons.Rounded.Check` is rendered.
  - Assert that when `isSelected = false`, the selection checkmark is not shown.
  - Assert transition defaults: enter/exit transitions are non-`None` by default, use center-origin scaling from a smaller initial scale, and return `EnterTransition.None` / `ExitTransition.None` when `isReducedMotion = true`.
- **Prior art**: Existing unit tests for `SelectableAvatarBadgeDefaults` and Compose UI tests in the `ui.contacts` test suite.

## Out of Scope

- Changing the Contact card container highlight color during Contact Selection Mode.
- Changing batch action tray icons or confirmation dialogs.
- Adding per-screen custom selection badge shapes outside `SelectableAvatarBadge`.
