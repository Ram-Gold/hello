# 01: Restructure HomeTopBar and Relocate Greeting Block

**What to build:** The user sees a modernized top bar where the drawer trigger icon is positioned on the top left and the Settings icon is positioned on the top right. The personalized greeting ("Hello, $userName 👋" and "Cultivate your circle today") is rendered directly beneath the top bar in the home scrollable feed, preserving clean spacing and visual hierarchy.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] Drawer trigger button is positioned at the top left of the top bar and fires a drawer open intent
- [x] Settings icon button is positioned at the top right of the top bar and invokes the settings action when clicked
- [x] The user greeting and subtitle column is moved out of the top bar row and rendered beneath the top bar in the home feed
- [x] Visual styling complies with Material 3 and Hello theme guidelines (warm beige/charcoal palette, tactile button bounds)
- [x] Compose Previews and UI/ViewModel unit tests pass with updated contracts
