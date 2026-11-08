# 01: SelectableAvatarBadge Component with Ease-Out Motion

**What to build:**
Create a reusable `SelectableAvatarBadge` component that wraps `DoodleAvatar` with an animated selection overlay. When `isSelected == true`, an accent-colored circular badge with a high-contrast checkmark (`Icons.Rounded.Check`) smoothly scales and fades in using an ease-out curve (`FastOutSlowInEasing`, initial scale $0.45\times$, duration $180\text{ms}$). When unselected, it scales down and fades out cleanly.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Create `SelectableAvatarBadge` composable container wrapping `DoodleAvatar`.
- [ ] Implement ease-out enter animation (`scaleIn` from $0.45\times$ + `fadeIn` over $180\text{ms}$ with `FastOutSlowInEasing`).
- [ ] Implement exit animation (`scaleOut` to $0.45\times$ + `fadeOut` over $140\text{ms}$).
- [ ] Add unit tests verifying motion spec parameters and ease-out curves.

