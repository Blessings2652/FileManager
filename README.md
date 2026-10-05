# File Manager (Shizuku/standard/SAF)

Kotlin/Compose Android file manager with a choice of three access engines,
picked on first launch and switchable anytime from the browser's overflow menu.

## Engines

- **Shizuku** — commands run as the `shell` (UID 2000) user via the Shizuku
  service (`ShizukuFileEngine`, using `ShizukuManager`, which calls
  `Shizuku`'s hidden `newProcess` via reflection since it's no longer public
  API on recent versions).
-SAF— no root or Shizuku needed. User picks one folder via
  `OpenDocumentTree`, and everything happens through `DocumentFile`
  (`SafFileEngine`). Most restricted — can't browse outside the granted
  folder — but always available.

## Architecture

- `fs/engine/FileEngine.kt` — shared interface (`list`, `mkdir`, `createFile`,
  `delete`, `rename`, `copy`, `move`, `parentPath`) all three engines
  implement. Paths are opaque per engine: raw filesystem paths for
  Shizuku/Root, document Uri strings for SAF.
- `fs/engine/ShizukuFileEngine.kt` / `RootFileEngine.kt` — thin wrappers
  around shell commands (`ls -la --full-time`, `mkdir`, `rm -rf`, `mv`,
  `cp -r`), parsed by the shared `LsParser`.
- `fs/engine/SafFileEngine.kt` — caches `DocumentFile` objects by Uri string
  so navigation and rename/delete/move can look nodes back up (SAF doesn't
  support path string math the way real filesystem paths do). Move tries
  `DocumentsContract.moveDocument` first, falls back to copy+delete.
- `fs/engine/EnginePrefs.kt` — SharedPreferences: remembers the chosen
  engine and (for SAF) the granted tree Uri, so you're not re-picking on
  every launch.
- `ui/EngineSelectionScreen.kt` — first-run choice between the three.
- `ui/ShizukuGateScreen.kt` / `RootGateScreen.kt` / `SafPickerScreen.kt` —
  per-engine "not ready yet" screens (waiting on Shizuku permission,
  waiting on root grant, or prompting to pick a SAF folder).
- `ui/FileBrowserViewModel.kt` — engine-agnostic; takes a `FileEngine` via
  `FileBrowserViewModel.Factory` and drives all state off the interface.
- `MainActivity.kt` (`AppRoot`) — reads the saved engine choice, shows the
  right gate screen until that engine is ready, then hands a live
  `FileEngine` instance to `FileBrowserScreen`.

## Setup

1. Open in Android Studio (Koala+), let Gradle sync.
2. Run the app. On first launch you'll be asked to choose Shizuku, Root, or
   SAF.
   - **Shizuku**: have Shizuku running (ADB or root pairing) before
     choosing this, then grant the permission prompt.
   - **Root**: device needs to already be rooted (Magisk etc.) — the app
     will trigger the grant prompt itself.
   - SAF: no prerequisite — just pick a folder when prompted.
3. Switch engines anytime via the ⋮ menu in the browser's top bar →
   "Switch access method…".

## Known limits

- No file preview/open-with — tapping a file just selects it
  (`state.selected` in the ViewModel, unused by the UI so far).
- No search, no multi-select batch actions, no chmod/permissions editor.
- SAF copy/move of large directory trees is synchronous per-file (no
  progress UI) — fine for casual use, would want a progress indicator for
  big folders.
- Root engine spawns one `su -c` process per command rather than keeping a
  persistent root shell open — simpler and more robust across root
  managers, but slightly slower for many rapid operations.
