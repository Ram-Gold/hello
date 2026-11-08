# Spec: Keyboard Auto-Scroll & IME Viewport Clearance

## Objective
When editing a contact in `PersonEditScreen` or entering data in quick action sheets, the soft keyboard pops up over the lower portions of the screen, occluding the active input field. The user must manually scroll to see what they are typing. This feature enables automatic viewport shrinking and smooth auto-scrolling to bring any focused input field into clear view above the keyboard.

## Key Requirements
1. **Viewport Resizing (IME Padding):** The scrollable container must shrink above the soft keyboard using `Modifier.imePadding()` instead of extending behind it.
2. **Auto-Scroll on Focus (Bring Into View):** When an input field (Full Name, Nickname, Role, Company, Phone, Email, Notes, Custom Cadence, Custom Properties) gains focus or is tapped, the scroll container automatically animates its scroll offset so the input field and its label remain fully visible with generous clearance (>= 24.dp).
3. **Modal Sheets & Quick Capture:** Quick Action cards (`FlowAAddContactCard`, `FlowBLogEncounterCard`, custom tag/property inputs) must respect keyboard bounds and avoid clipping action buttons.
4. **Zero Regression:** Normal scrolling when the keyboard is hidden must remain smooth, fast, and tactile.

## Success Criteria
- Tapping on lower fields like Notes or Phone in `PersonEditScreen` instantly animates the field above the keyboard.
- The user can see their cursor, typed text, and suggestions without manual scrolling.
- Unit and UI tests verify that IME insets trigger appropriate viewport shrinkage and focus-based bring-into-view invocations.

