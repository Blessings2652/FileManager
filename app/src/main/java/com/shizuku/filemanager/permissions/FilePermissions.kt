package com.shizuku.filemanager.permissions

data class PermissionTriple(
    val read: Boolean,
    val write: Boolean,
    val execute: Boolean
) {
    fun toInt(): Int = (if (read) 4 else 0) + (if (write) 2 else 0) + (if (execute) 1 else 0)

    companion object {
        fun fromInt(value: Int) = PermissionTriple(
            read = (value and 4) != 0,
            write = (value and 2) != 0,
            execute = (value and 1) != 0
        )
    }
}

data class FilePermissions(
    val owner: PermissionTriple,
    val group: PermissionTriple,
    val other: PermissionTriple,
    val ownerUser: String,
    val ownerGroup: String
) {
    val octalString: String
        get() = "${owner.toInt()}${group.toInt()}${other.toInt()}"
}

sealed class PermissionResult {
    object Success : PermissionResult()
    data class Failure(val message: String) : PermissionResult()
}
