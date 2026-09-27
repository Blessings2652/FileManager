package com.shizuku.filemanager.sys

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)
