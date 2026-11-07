# 01: Remove Home Screen Card Feature End-to-End

**What to build:** Remove the Home Screen Card / QR Card feature end-to-end so the Home Screen flows directly from the Top Bar and Greeting into the **Upcoming** feed and **Recent Encounters** list without any card widget, full-screen QR modal, or card editor dialog.

**Blocked by:** None (can start immediately)

**Status:** done

- [x] Delete the Home Card UI components (`MyCardWidget.kt` and `FullScreenCardDialog.kt`, including `QrCardEditorDialog` and `QrCardMatrixView`) and remove their rendering and imports from `HomeScreen`.
- [x] Remove `QrCard`, `QrCardSource`, and `generateDeterministicQrMatrix` from the domain models (`Models.kt`).
- [x] Remove all card-related state fields (`qrCards`, `activeQrCardId`, `activeQrCard`, `isAddOrEditCardSheetOpen`, `editingQrCard`, `isQrExpanded`) and events (`OnSelectQrCard`, `OnOpenAddQrCard`, `OnOpenEditQrCard`, `OnDismissQrCardSheet`, `OnSaveQrCard`, `OnDeleteQrCard`, `OnExpandQr`) from `HomeUiState`, `HomeUiEvent`, and `HomeViewModel`.
- [x] Clean up obsolete `.scratch/minimal-qr-card/` ticket files and update `HomeViewModelTest` so all unit tests and the debug APK build pass cleanly.
