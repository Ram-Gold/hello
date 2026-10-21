# 02: Side Drawer Navigation for Group Switching

**What to build:** Clicking the drawer button in the top left smoothly opens a side drawer. Inside the drawer, the user sees a list of available Groups (Circles such as Work, Friends, Family, Uni). Tapping any Group selects it as the active context, updates the application state, and dismisses the drawer with feedback.

**Blocked by:** 01 Restructure HomeTopBar and Relocate Greeting Block

**Status:** resolved

- [x] Tapping the top-left drawer icon opens the side navigation drawer
- [x] Tapping outside or on a scrim closes the drawer
- [x] The drawer presents all available Groups (Circles) with distinct icons/labels and indicates the currently active selection
- [x] Selecting a Group updates the active context in the state flow and closes the drawer
- [x] State updates and drawer open/dismiss transitions are covered by unit and UI tests
