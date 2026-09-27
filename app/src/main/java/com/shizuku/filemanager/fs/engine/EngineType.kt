package com.shizuku.filemanager.fs.engine

enum class EngineType(val label: String, val description: String) {
    SHIZUKU(
        label = "Shizuku",
        description = "Runs as the shell user via the Shizuku service. Broad read access, no pairing dialog needed each boot if Shizuku is already running."
    ),
    ROOT(
        label = "Root",
        description = "Full filesystem access, requires a rooted device with Magisk and will trigger a root grant prompt."
    ),
    SAF(
        label = "Storage Access Framework",
        description = "You pick a folder once and Android grants access to just that subtree — most restricted, but always available."
    ),
    STANDARD(
        label = "Standard",
        description = "Access all files in internal storage using the standard Android permission. Requires a one-time grant in system settings."
    )
}
