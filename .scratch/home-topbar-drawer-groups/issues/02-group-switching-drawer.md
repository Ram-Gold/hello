# 02: Side Drawer Navigation for Group Switching

**What to build:** Clicking the drawer button in the top right smoothly opens a side drawer. Inside the drawer, the user sees a list of available Groups (Circles such as Work, Friends, Family, Uni). Tapping any Group selects it as the active context, updates the application state, and dismisses the drawer with feedback.

**Blocked by:** 01 Restructure HomeTopBar and Relocate Greeting Block

**Status:** ready-for-agent

- [ ] Tapping the top-right drawer icon opens the side navigation drawer
- [ ] Tapping outside or on a scrim closes the drawer
- [ ] The drawer presents all available Groups (Circles) with distinct icons/labels and indicates the currently active selection
- [ ] Selecting a Group updates the active context in the state flow and closes the drawer
- [ ] State updates and drawer open/dismiss transitions are covered by unit and UI tests
