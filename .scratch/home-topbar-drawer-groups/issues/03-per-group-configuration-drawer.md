# 03: Per-Group Configuration Drawer Interface

**What to build:** Within the side drawer, each Group reveals its distinct configuration (e.g. cadence intervals, associated default card profile, notification preferences, or active contact filters). The user can inspect and modify group-specific settings directly from the drawer, persisting changes locally.

**Blocked by:** 02 Side Drawer Navigation for Group Switching

**Status:** resolved

- [x] Each Group item in the drawer displays its distinct configuration details (such as target cadence frequency and associated card profile)
- [x] Users can edit or toggle configuration options for a selected Group directly within the drawer surface
- [x] Changes made to a Group's configuration persist in state and take effect across the Home feed and alerts
- [x] Group configuration models and mutation reducers are thoroughly verified with unit tests
