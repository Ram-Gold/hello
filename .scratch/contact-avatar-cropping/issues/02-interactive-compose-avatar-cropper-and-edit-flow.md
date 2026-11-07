# 02: Interactive 1:1 Compose Avatar Cropper and Staged Edit Contact Flow

**What to build:** On the Edit Contact screen, tapping the 96dp Contact Avatar and selecting an image (PNG, JPEG, or WebP) opens an in-app interactive 1:1 circular Compose cropper with pinch-to-zoom, pan clamping, and a "Fit whole image" toggle. Confirming the crop immediately previews the staged 512×512 avatar on the Edit Contact screen and enables the Save button, while canceling or discarding leaves the original avatar untouched.

**Blocked by:** 01: Local-First Avatar Storage Engine, Memory Cache, and Contact Avatar Rendering

**Status:** done

- [x] Selecting a valid image in `PersonEditScreen` opens the interactive 1:1 circular crop overlay with darkened exterior scrim, pinch-to-zoom (up to 4x), and drag-to-pan.
- [x] Default crop mode fills the 1:1 circle (clamping pan so edges never pull inside the circle), while toggling "Fit whole image" scales the entire image inside the circle for transparent PNG stickers or full portraits.
- [x] Confirming the crop writes a staged 512×512 PNG, updates the 96dp Contact Avatar preview on the Edit Contact screen, and enables the top-right Save button.
- [x] Tapping Save commits the staged avatar file to the contact and deletes any previous avatar file; tapping Back -> Discard deletes the staged file and preserves the original avatar.
- [x] Tapping "Reset to Doodle Avatar" clears the preview and deletes the old avatar file once Save is confirmed.
- [x] Selecting an unreadable or corrupted file displays a friendly `"Could not load selected image"` snackbar and stays on the Edit Contact screen without crashing.
