package com.shizuku.filemanager.fs.engine

import java.io.IOException

/**
 * Thrown when a directory (like /Android/data on Android 11+) is visible
 * in the UI but cannot be accessed via standard java.io.File APIs.
 */
class RestrictedAccessException(val path: String) : IOException("Access restricted by system: $path")
