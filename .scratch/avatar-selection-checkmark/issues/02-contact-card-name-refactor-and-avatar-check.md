# 02: ContactCard Name Row Refactor & Avatar Checkmark Integration

**What to build:**
Refactor `ContactCard` in the relationship directory to remove the inline checkmark icon next to `contact.name`, eliminating text layout jumps when selecting cards. Replace the standard avatar with `SelectableAvatarBadge` so selecting a card displays the animated ease-out checkmark directly on the contact's profile picture.

**Blocked by:** 01 (SelectableAvatarBadge Component with Ease-Out Motion)

**Status:** ready-for-agent

- [ ] Remove inline `Icon(Icons.Rounded.Check, ...)` from next to `contact.name` in `ContactCard.kt`.
- [ ] Ensure name text maintains stable start alignment and never shifts horizontally on selection toggle.
- [ ] Integrate `SelectableAvatarBadge` into `ContactCard.kt` driven by `isSelected`.
- [ ] Verify card visual aesthetics and selection state responsiveness.

