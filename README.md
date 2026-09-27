# File Manager

A Kotlin/Compose Android file manager with a **runtime-selectable file access engine**:

- **Shizuku** — privileged access via the Shizuku service, no root required
- **Root** — full filesystem access via `su` (libsu)
- **SAF** — sandboxed access via Storage Access Framework, no special permissions

Switch engines from within the app depending on what's available on the device.

## Project structure

```
app/src/main/java/com/theblacksheep/filemanager/
├── engine/
│   ├── FileAccessEngine.kt   # common interface + FileEntry/EngineType
│   ├── RootEngine.kt          # libsu-backed implementation
│   ├── ShizukuEngine.kt       # Shizuku-backed implementation (scaffold)
│   ├── SafEngine.kt           # DocumentFile/SAF-backed implementation
│   └── EngineProvider.kt      # runtime engine selection
├── ui/theme/                  # Compose theme
└── MainActivity.kt            # entry point + basic browser UI
```

## Status

Early scaffold. `RootEngine` and `SafEngine` have working list/delete/rename;
`ShizukuEngine` has permission handling wired up but file operations are
stubbed pending an `IRemoteProcess`/AIDL service (see Shizuku-API demo).

## Building

```bash
./gradlew assembleDebug
```

Requires JDK 17 and the Android SDK (compileSdk 34, minSdk 26).

## Roadmap

- [ ] Implement Shizuku command execution via IRemoteProcess
- [ ] Copy/move for SAF engine
- [ ] Directory navigation + breadcrumb UI
- [ ] Multi-select + bulk operations
- [ ] Search
- [ ] Settings screen for engine selection + persistence

## License

MIT — see [LICENSE](LICENSE).
