# File Manager

A Kotlin / Jetpack Compose file manager for Android with three interchangeable
access engines: **Shizuku**, **root**, and the **Storage Access Framework (SAF)**.
You pick an engine on first launch and can switch at any time from the browser's
overflow menu.

## Screenshots

| File browser | Access engine settings |
|---|---|
| ![File browser](screenshots/file-browser.jpg) | ![Access engine settings](screenshots/engine-settings.jpg) |

## Access engines

| Engine  | Requires                                   | Scope                                              |
|---------|--------------------------------------------|----------------------------------------------------|
| Shizuku | Shizuku running (ADB or root pairing)      | Full filesystem, as the `shell` user (UID 2000)    |
| Root    | A rooted device (Magisk or similar)        | Full filesystem, via `su`                          |
| SAF     | Nothing                                    | Only the single folder you grant access to         |

**Shizuku** runs commands through the Shizuku service (`ShizukuFileEngine`, using
`ShizukuManager`). Recent Shizuku versions no longer expose `newProcess` as public
API, so `ShizukuManager` calls it via reflection.

**SAF** needs no root or Shizuku. You choose one folder with `OpenDocumentTree`,
and all operations go through `DocumentFile`. It cannot browse outside the granted
folder, but it is always available.

## Architecture

| File | Role |
|------|------|
| `fs/engine/FileEngine.kt` | Shared interface (`list`, `mkdir`, `createFile`, `delete`, `rename`, `copy`, `move`, `parentPath`) implemented by all engines. Paths are opaque per engine: raw filesystem paths for Shizuku/Root, document Uri strings for SAF. |
| `fs/engine/ShizukuFileEngine.kt`, `RootFileEngine.kt` | Thin wrappers around shell commands (`ls -la --full-time`, `mkdir`, `rm -rf`, `mv`, `cp -r`), parsed by the shared `LsParser`. |
| `fs/engine/SafFileEngine.kt` | Caches `DocumentFile` objects by Uri string so navigation and rename/delete/move can look nodes up again, since SAF does not support path string math. Move tries `DocumentsContract.moveDocument` first and falls back to copy + delete. |
| `fs/engine/EnginePrefs.kt` | SharedPreferences storing the chosen engine and, for SAF, the granted tree Uri, so you don't re-pick on every launch. |
| `ui/EngineSelectionScreen.kt` | First-run choice between the three engines. |
| `ui/ShizukuGateScreen.kt`, `RootGateScreen.kt`, `SafPickerScreen.kt` | Per-engine "not ready yet" screens: waiting for Shizuku permission, waiting for root grant, or prompting for a SAF folder. |
| `ui/FileBrowserViewModel.kt` | Engine-agnostic. Takes a `FileEngine` through `FileBrowserViewModel.Factory` and drives all state from the interface. |
| `MainActivity.kt` (`AppRoot`) | Reads the saved engine choice, shows the right gate screen until that engine is ready, then passes a live `FileEngine` to `FileBrowserScreen`. |

## Requirements

- Android Studio Koala or newer
- An Android device running one of: Shizuku, root, or nothing extra (SAF)

## Setup

1. Open the project in Android Studio (Koala+) and let Gradle sync.
2. Run the app. On first launch you'll be asked to choose Shizuku, Root, or SAF.
   - **Shizuku:** start Shizuku (ADB or root pairing) before choosing this option,
     then grant the permission prompt.
   - **Root:** the device must already be rooted. The app triggers the grant prompt
     itself.
   - **SAF:** no prerequisites. Pick a folder when prompted.
3. To switch engines later, open the ⋮ menu in the browser's top bar and tap
   **Switch access method…**.

## Known limitations

1. No file preview or "open with". Tapping a file only selects it
   (`state.selected` in the ViewModel, not yet used by the UI).
2. No search, no multi-select batch actions, and no chmod/permissions editor.
3. SAF copy and move of large directory trees run synchronously per file with no
   progress UI. This is fine for casual use but needs a progress indicator for big
   folders.
4. The root engine spawns one `su -c` process per command instead of keeping a
   persistent root shell. This is simpler and more robust across root managers but
   slower for many rapid operations.
