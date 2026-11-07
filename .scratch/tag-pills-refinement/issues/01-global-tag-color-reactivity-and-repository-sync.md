# 01: Global Tag Color Reactivity and Repository Pipeline Sync

**What to build:** When a user customizes a tag's color (or edits its name) in Settings, the update must immediately propagate globally and reactively across all screens—including the People screen's filter bar, all Contact Cards, recent encounters, and Profile views—without requiring an app restart or manual cache refresh.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] `ContactRepository.updateTag(updatedTag)` updates `_tags`, persists to Room, and immediately updates `_contacts` and `_recentContacts` so all contact summaries carry the updated tag object and color.
- [x] `ContactsViewModel` observes `repository.tags` or resolves smart filter pills against active repository tags, ensuring customized colors from Settings appear on the People filter bar instead of falling back to default name hashes.
- [x] Merging or deleting tags similarly updates `_contacts` and `_recentContacts` reactively.
- [x] Unit tests verify that editing a tag color in repository emits updated `tags` on `contacts` and `recentContacts` StateFlows.

