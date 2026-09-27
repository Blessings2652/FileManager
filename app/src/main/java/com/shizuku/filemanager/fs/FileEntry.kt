package com.shizuku.filemanager.fs

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val isSymlink: Boolean,
    val sizeBytes: Long,
    val permissions: String,
    val owner: String,
    val group: String,
    val modified: String
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")

    val isImage: Boolean
        get() = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "tiff").contains(extension.lowercase())

    val isVideo: Boolean
        get() = listOf("mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "wmv", "ts", "mpg", "mpeg", "m4v", "rmvb", "asf").contains(extension.lowercase())

    val isAudio: Boolean
        get() = listOf(
            "mp3", "wav", "ogg", "flac", "m4a", "aac", "opus", "wma", "amr", "awb",
            "mid", "midi", "aiff", "aif", "ape", "alac", "mka", "ac3", "dts", "caf",
            "3ga", "mp2", "mp1", "m4b", "m4p", "au", "pcm", "snd", "qcp", "gsm"
        ).contains(extension.lowercase())

    val isText: Boolean
        get() = listOf(
            "txt", "kt", "kts", "java", "xml", "json", "md", "sh", "py", "gradle",
            "properties", "conf", "yaml", "yml", "log", "sql", "html", "htm", "css",
            "js", "ts", "c", "cpp", "h", "hpp", "cs", "dart", "go", "rs", "swift",
            "rb", "pl", "php", "lua", "ini", "cfg", "bat", "cmd", "vbs", "ps1"
        ).contains(extension.lowercase())

    val isKotlin: Boolean
        get() = listOf("kt", "kts").contains(extension.lowercase())

    val isArchive: Boolean
        get() = listOf("zip", "rar", "7z", "tar", "gz", "bz2").contains(extension.lowercase())

    val isPdf: Boolean
        get() = extension.lowercase() == "pdf"

    val isJson: Boolean
        get() = extension.lowercase() == "json"

    val isDocument: Boolean
        get() = listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp").contains(extension.lowercase())

    val isApk: Boolean
        get() = extension.lowercase() == "apk"
}
