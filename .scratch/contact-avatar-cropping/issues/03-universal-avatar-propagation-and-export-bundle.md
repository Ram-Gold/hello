# 03: Universal Contact Avatar Propagation Across Profile, Cards, Timeline, and Export Bundle

**What to build:** Display each contact's custom cropped Contact Avatar consistently across the Contact Profile Header (88dp), Contact Cards on the Home and Relationship tabs (56dp), and Living Timeline encounter and birthday nodes (36dp), and bundle local avatar PNGs with relative metadata paths into the Export Bundle ZIP archive.

**Blocked by:** 01: Local-First Avatar Storage Engine, Memory Cache, and Contact Avatar Rendering

**Status:** done

- [x] `ContactSummary`, `TimelineEntry`, and `UpcomingBirthdayItem` carry `photoUri` populated reactively from `ContactRepository`.
- [x] Contact Profile Header (88dp), Contact Cards on Home and Relationship tabs (56dp), and Living Timeline encounter/birthday nodes (36dp) render the contact's custom photo via Contact Avatar (`DoodleAvatar`).
- [x] Deleting a contact permanently removes its associated avatar file from `filesDir/avatars/`.
- [x] `ExportBundleGenerator` writes `"photoUri"` in `contacts.json`, `avatar:` in Markdown frontmatter, and packages local avatar files under `avatars/<contactId>.png` inside the exported ZIP archive.
- [x] Unit tests verify `photoUri` propagation across repository streams and Export Bundle generation.
