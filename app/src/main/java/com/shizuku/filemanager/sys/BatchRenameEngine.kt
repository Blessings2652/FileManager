package com.shizuku.filemanager.sys

import com.shizuku.filemanager.fs.FileEntry

/**
 * Pattern-based rename plan for a multi-select. Pure functions over
 * [FileEntry] -> new name; the caller (ViewModel) is responsible for
 * actually invoking FileEngine.rename per entry and rolling back on
 * partial failure if it wants that behavior.
 */
object BatchRenameEngine {

    enum class CaseMode { UNCHANGED, LOWER, UPPER, TITLE }

    data class Options(
        val findText: String = "",
        val replaceText: String = "",
        val useRegex: Boolean = false,
        val prefix: String = "",
        val suffix: String = "",
        val numbering: Boolean = false,
        val numberStart: Int = 1,
        val numberPadding: Int = 2,
        val numberSeparator: String = "_",
        val caseMode: CaseMode = CaseMode.UNCHANGED,
        val keepExtension: Boolean = true,
    )

    data class Plan(val original: FileEntry, val newName: String) {
        val changed: Boolean get() = original.name != newName
    }

    fun preview(entries: List<FileEntry>, options: Options): List<Plan> {
        return entries.mapIndexed { index, entry ->
            Plan(entry, buildName(entry, index, options))
        }
    }

    /** True if the plan would produce duplicate names or an empty name. */
    fun validate(plans: List<Plan>): String? {
        val names = plans.map { it.newName }
        if (names.any { it.isBlank() }) return "One or more resulting names would be empty."
        if (names.toSet().size != names.size) return "Two or more files would end up with the same name."
        return null
    }

    private fun buildName(entry: FileEntry, index: Int, o: Options): String {
        val ext = if (entry.isDirectory) "" else entry.name.substringAfterLast('.', "")
        val hasExt = !entry.isDirectory && ext.isNotEmpty() && entry.name.contains('.')
        val baseName = if (hasExt) entry.name.removeSuffix(".$ext") else entry.name

        var name = if (o.findText.isNotEmpty()) {
            if (o.useRegex) {
                try {
                    baseName.replace(Regex(o.findText), o.replaceText)
                } catch (_: Exception) {
                    baseName // invalid regex: leave untouched rather than crash the preview
                }
            } else {
                baseName.replace(o.findText, o.replaceText)
            }
        } else baseName

        name = when (o.caseMode) {
            CaseMode.UNCHANGED -> name
            CaseMode.LOWER -> name.lowercase()
            CaseMode.UPPER -> name.uppercase()
            CaseMode.TITLE -> name.split(" ", "_", "-").joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercase() }
            }
        }

        if (o.prefix.isNotEmpty()) name = o.prefix + name
        if (o.suffix.isNotEmpty()) name = name + o.suffix

        if (o.numbering) {
            val num = (o.numberStart + index).toString().padStart(o.numberPadding, '0')
            name = "$name${o.numberSeparator}$num"
        }

        return if (hasExt && o.keepExtension) "$name.$ext" else name
    }
}
