# Spec: Avatar Selection Checkmark with Ease-Out Motion

## Problem Statement

Currently, when a contact is selected in multi-selection mode (or marked for batch actions), a checkmark icon is displayed inline directly beside the contact's name. This inline checkmark causes the contact name and metadata to shift horizontally upon selection, resulting in jarring layout shifts across the directory and timeline. Furthermore, placing the selection indicator in the middle of textual information breaks standard mobile contact patterns (such as Google Contacts and Material Design 3), where the profile avatar itself serves as the selection anchor.

## Solution

Refactor contact selection indicators across Hello:
1. **Remove Inline Checkmark from Name Row:**
   - Eliminate the inline checkmark beside contact names in `ContactCard` and `TimelineScreen`, ensuring text baseline and horizontal alignment remain perfectly stationary regardless of selection state.
2. **Avatar Checkmark Overlay:**
   - Position the selection checkmark directly on the circular profile avatar (`DoodleAvatar`).
   - When selected, a circular selection badge overlays the avatar with a high-contrast checkmark (`Icons.Rounded.Check`) and primary accent surface.
3. **Responsive Ease-Out Animation:**
   - Drive the checkmark overlay with a smooth ease-out animation curve (`FastOutSlowInEasing` / `CubicBezierEasing(0f, 0f, 0.2f, 1f)`):
     - **Enter:** Scales up from $0.45\times \rightarrow 1.0\times$ while fading in over $180\text{ms}$ (`ease-out`), delivering immediate responsive visual feedback.
     - **Exit:** Scales down ($1.0\times \rightarrow 0.45\times$) while fading out over $140\text{ms}$.
4. **Consistency across Directory & Timeline:**
   - Apply the avatar selection pattern consistently across `ContactCard` in the relationship directory and encounter nodes in `TimelineScreen`.

---

## User Stories

1. As a relationship keeper selecting contacts for batch operations, I want the checkmark to appear directly on the contact's profile picture instead of beside their name, so that the card layout matches standard Android contact patterns.
2. As a user selecting a contact, I want the contact's name to remain fixed in place without jumping or shifting horizontally, so that my reading flow is never disrupted.
3. As a user tapping a contact to select them, I want the avatar checkmark to animate in with a snappy ease-out scale and fade, so that the interaction feels tactile and alive.
4. As a user unselecting a contact, I want the checkmark to smoothly scale down and fade away, so that the avatar is revealed without an abrupt flicker.
5. As a user long-pressing a contact card to enter selection mode, I want the initial card's avatar to immediately transition into the checked state with the ease-out animation, confirming selection mode activation.
6. As a user selecting "Select All" from the action tray, I want all visible contact avatars to transition into their selected checkmark state with synchronized ease-out animation, providing cohesive visual confirmation.
7. As a user clearing selection, I want all avatar checkmarks to smoothly exit, returning cards to their standard avatar presentation.
8. As a user browsing the Living Timeline and selecting encounters for batch actions, I want the encounter contact avatar to display the same ease-out checkmark overlay, maintaining consistency across the entire app.
9. As a user with custom contact photos or initials avatars, I want the checkmark overlay to sit cleanly on top of both photos and doodle avatars with a subtle border, ensuring readability against any background.
10. As a user with "Reduce Animations" enabled, I want selection checkmarks to appear and disappear instantaneously without scale or fade transitions, respecting my motion preferences.

---

## Implementation Decisions

### 1. Avatar Selection Overlay Component (`SelectableAvatarContainer`)
- Create a dedicated container composable wrapping `DoodleAvatar`:
  - Contains the underlying avatar at its original size ($56\text{dp}$ in `ContactCard`, $36\text{dp}$ in `TimelineScreen`).
  - Contains an `AnimatedVisibility` overlay layer matching avatar dimensions with `CircleShape`.
  - When `isSelected == true`:
    - Renders a circular container (`MaterialTheme.colorScheme.primary` or `primaryContainer`).
    - Centered `Icons.Rounded.Check` with corresponding `onPrimary` / `onPrimaryContainer` tint.
  - Animation specs:
    - `scaleIn(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing), initialScale = 0.45f)`
    - `fadeIn(animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing))`
    - `scaleOut(animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing), targetScale = 0.45f)`
    - `fadeOut(animationSpec = tween(durationMillis = 100))`

### 2. Name Row Refactoring
- In `ContactCard.kt`:
  - Remove `if (isSelected) Icon(Icons.Rounded.Check, ...)` from line 114.
  - The name `Text` composable retains full horizontal width and stable start alignment.
- In `TimelineScreen.kt`:
  - Remove `if (isSelected) Icon(Icons.Rounded.Check, ...)` from lines 719 and 1009.
  - Wrap timeline avatar in the selectable avatar overlay.

### 3. Motion Philosophy Compliance (Emil Kowalski)
- Uses `ease-out` exclusively for entry: starts fast to provide instantaneous feedback to user touch.
- Scales from $0.45\times$ rather than $0\times$, adhering to the principle that elements in the physical world do not expand from a dimensionless point.
- Duration capped under $200\text{ms}$ to ensure zero perceived latency during rapid multi-tap selections.

---

## Testing Decisions

### What Makes a Good Test
- Tests should verify external layout and state contracts:
  - Verifies `ContactCard` no longer renders an inline checkmark inside the text hierarchy when `isSelected == true`.
  - Verifies the avatar container presents the selection overlay when selected.
  - Verifies animation spec contracts use `FastOutSlowInEasing` with duration $\le 200\text{ms}$.

### Modules Tested
- **ContactCard Component Contract:** Validates layout stability and checkmark placement on avatar.
- **SelectableAvatarContainer Motion Specs:** Validates ease-out easing and scale boundaries.

### Prior Art
- `PullToActionStateTest` and `NavigationTransitionsTest` in `app/src/test/`.

---

## Out of Scope

- Floating checkbox badges positioned outside the avatar boundary (the full circular overlay is preferred for clean touch targets and visual clarity).
- Re-coloring the entire contact card background to harsh red/error tones (the avatar checkmark handles state clearly).

---

## Further Notes

- This refactoring aligns Hello CRM directly with Google Material Design 3 and Android Contacts design guidelines for multi-selection lists.
