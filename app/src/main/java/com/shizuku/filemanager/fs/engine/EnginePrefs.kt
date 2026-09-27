package com.shizuku.filemanager.fs.engine

import android.content.Context
import com.shizuku.filemanager.fs.RootManager
import com.shizuku.filemanager.shizuku.ShizukuManager
import com.shizuku.filemanager.sys.VaultManager
import com.shizuku.filemanager.ui.isStandardPermissionGranted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Plain SharedPreferences wrapper — no need for DataStore's async ceremony for two small values. */
object EnginePrefs {
    private const val PREFS = "engine_prefs"
    
    private val _settingsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val settingsChanged: SharedFlow<Unit> = _settingsChanged

    private fun notifyChanged() {
        _settingsChanged.tryEmit(Unit)
    }

    private const val KEY_ENGINE = "engine_type"
    private const val KEY_SAF_TREE_URI = "saf_tree_uri"
    private const val KEY_HIGH_PRIORITY = "high_thread_priority"
    private const val KEY_CALC_FOLDER_SIZE = "calculate_folder_size"
    private const val KEY_ARCHIVE_FORMAT = "default_archive_format"
    private const val KEY_COMPRESSION_LEVEL = "compression_level"
    private const val KEY_SECURE_DELETE = "secure_delete"
    private const val KEY_AUTO_EMPTY_TRASH = "auto_empty_trash"
    private const val KEY_TRASH_DAYS = "trash_retention_days"
    private const val KEY_TRASH_LIMIT = "trash_size_limit"
    private const val KEY_SHOW_HIDDEN = "show_hidden_files"
    private const val KEY_SHOW_EXTENSIONS = "show_file_extensions"
    private const val KEY_CONFIRM_DELETE = "confirm_delete"
    private const val KEY_CONFIRM_MOVE = "confirm_move"
    private const val KEY_CONFIRM_OVERWRITE = "confirm_overwrite"
    private const val KEY_CLICK_TO_OPEN = "click_to_open" // true for single, false for double
    private const val KEY_USE_INTERNAL_VIEWER = "use_internal_viewer"
    private const val KEY_VIEW_MODE = "view_mode"
    private const val KEY_SORT_BY = "sort_by"
    private const val KEY_SORT_ORDER = "sort_order"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT_COLOR = "accent_color_argb" // 0 = use dynamic/Material You default
    private const val KEY_ENABLE_THUMBNAILS = "enable_thumbnails"
    private const val KEY_SHOW_TYPE_ICON = "show_type_icon_on_thumbnails"
    private const val KEY_SHOW_RECENT = "show_recent_files"
    private const val KEY_APP_LOCK = "app_lock_enabled"
    private const val KEY_VAULT_ENABLED = "vault_enabled"
    private const val KEY_VAULT_PIN = "vault_pin"
    private const val KEY_VAULT_TYPE = "vault_type" // "PIN" or "PASSWORD"
    private const val KEY_REFRESH_RATE = "refresh_rate_ms"
    private const val KEY_ITEM_SIZE_MULTIPLIER = "item_size_multiplier"
    private const val KEY_FONT_SIZE_MULTIPLIER = "font_size_multiplier"
    private const val KEY_FIRST_LAUNCH = "first_launch"

    fun isFirstLaunch(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FIRST_LAUNCH, true)

    fun setFirstLaunchCompleted(context: Context) {
        prefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        notifyChanged()
    }

    fun getSavedEngine(context: Context): EngineType? {
        val name = prefs(context).getString(KEY_ENGINE, null) ?: return null
        return try { EngineType.valueOf(name) } catch (e: Throwable) { null }
    }

    fun saveEngine(context: Context, type: EngineType) {
        prefs(context).edit().putString(KEY_ENGINE, type.name).apply()
        notifyChanged()
    }

    /**
     * Automatically detects the best engine based on availability and permissions.
     * Priority: Shizuku (if running) > Root (if su present) > Standard (if granted) > SAF (if URI exists) > STANDARD (fallback)
     */
    fun detectBestEngine(context: Context): EngineType {
        if (ShizukuManager.isAvailable.value) {
            return EngineType.SHIZUKU
        }
        
        if (RootManager.isSuBinaryPresent()) {
            return EngineType.ROOT
        }
        
        if (isStandardPermissionGranted(context)) {
            return EngineType.STANDARD
        }
        
        if (getSafTreeUri(context) != null) {
            return EngineType.SAF
        }
        
        return EngineType.STANDARD
    }

    fun getSafTreeUri(context: Context): String? =
        prefs(context).getString(KEY_SAF_TREE_URI, null)

    fun saveSafTreeUri(context: Context, uri: String) {
        prefs(context).edit().putString(KEY_SAF_TREE_URI, uri).apply()
        notifyChanged()
    }

    fun clearSafTreeUri(context: Context) {
        prefs(context).edit().remove(KEY_SAF_TREE_URI).apply()
        notifyChanged()
    }

    fun isHighPriority(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HIGH_PRIORITY, true)

    fun setHighPriority(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIGH_PRIORITY, enabled).apply()
        notifyChanged()
    }

    fun isCalculateFolderSize(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CALC_FOLDER_SIZE, true)

    fun setCalculateFolderSize(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CALC_FOLDER_SIZE, enabled).apply()
        notifyChanged()
    }

    fun getDefaultArchiveFormat(context: Context): String =
        prefs(context).getString(KEY_ARCHIVE_FORMAT, "ZIP") ?: "ZIP"

    fun setDefaultArchiveFormat(context: Context, format: String) {
        prefs(context).edit().putString(KEY_ARCHIVE_FORMAT, format).apply()
        notifyChanged()
    }

    fun getCompressionLevel(context: Context): String =
        prefs(context).getString(KEY_COMPRESSION_LEVEL, "Fast") ?: "Fast"

    fun setCompressionLevel(context: Context, level: String) {
        prefs(context).edit().putString(KEY_COMPRESSION_LEVEL, level).apply()
        notifyChanged()
    }

    fun isSecureDelete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SECURE_DELETE, false)

    fun setSecureDelete(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SECURE_DELETE, enabled).apply()
        notifyChanged()
    }

    fun isAutoEmptyTrash(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_EMPTY_TRASH, true)

    fun setAutoEmptyTrash(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_EMPTY_TRASH, enabled).apply()
        notifyChanged()
    }

    fun getTrashRetentionDays(context: Context): Int =
        prefs(context).getInt(KEY_TRASH_DAYS, 30)

    fun setTrashRetentionDays(context: Context, days: Int) {
        prefs(context).edit().putInt(KEY_TRASH_DAYS, days).apply()
        notifyChanged()
    }

    fun getTrashSizeLimit(context: Context): Int =
        prefs(context).getInt(KEY_TRASH_LIMIT, 5)

    fun setTrashSizeLimit(context: Context, limitPercent: Int) {
        prefs(context).edit().putInt(KEY_TRASH_LIMIT, limitPercent).apply()
        notifyChanged()
    }

    fun isShowHidden(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_HIDDEN, false)

    fun setShowHidden(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_HIDDEN, enabled).apply()
        notifyChanged()
    }

    fun isShowExtensions(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_EXTENSIONS, true)

    fun setShowExtensions(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_EXTENSIONS, enabled).apply()
        notifyChanged()
    }

    fun isConfirmDelete(context: Context): Boolean = true

    fun setConfirmDelete(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONFIRM_DELETE, enabled).apply()
        notifyChanged()
    }

    fun isConfirmMove(context: Context): Boolean = true

    fun setConfirmMove(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONFIRM_MOVE, enabled).apply()
        notifyChanged()
    }

    fun isConfirmOverwrite(context: Context): Boolean = true

    fun setConfirmOverwrite(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONFIRM_OVERWRITE, enabled).apply()
        notifyChanged()
    }

    fun isSingleClickToOpen(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CLICK_TO_OPEN, true)

    fun setSingleClickToOpen(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CLICK_TO_OPEN, enabled).apply()
        notifyChanged()
    }

    fun isUseInternalViewer(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USE_INTERNAL_VIEWER, true)

    fun setUseInternalViewer(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_USE_INTERNAL_VIEWER, enabled).apply()
        notifyChanged()
    }

    fun getViewMode(context: Context): String =
        prefs(context).getString(KEY_VIEW_MODE, "List") ?: "List"

    fun setViewMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_VIEW_MODE, mode).apply()
        notifyChanged()
    }

    fun getSortBy(context: Context): String =
        prefs(context).getString(KEY_SORT_BY, "Name") ?: "Name"

    fun setSortBy(context: Context, sortBy: String) {
        prefs(context).edit().putString(KEY_SORT_BY, sortBy).apply()
        notifyChanged()
    }

    fun getSortOrder(context: Context): String =
        prefs(context).getString(KEY_SORT_ORDER, "Ascending") ?: "Ascending"

    fun setSortOrder(context: Context, order: String) {
        prefs(context).edit().putString(KEY_SORT_ORDER, order).apply()
        notifyChanged()
    }

    fun getThemeMode(context: Context): String =
        prefs(context).getString(KEY_THEME_MODE, "System") ?: "System"

    /** 0 means "no override" — follow dynamic/Material You or the built-in scheme as before. */
    fun getAccentColor(context: Context): Int =
        prefs(context).getInt(KEY_ACCENT_COLOR, 0)

    fun setAccentColor(context: Context, argb: Int) {
        prefs(context).edit().putInt(KEY_ACCENT_COLOR, argb).apply()
        notifyChanged()
    }

    fun clearAccentColor(context: Context) {
        prefs(context).edit().remove(KEY_ACCENT_COLOR).apply()
        notifyChanged()
    }

    fun setThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
        notifyChanged()
    }

    fun isThumbnailsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLE_THUMBNAILS, true)

    fun setThumbnailsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLE_THUMBNAILS, enabled).apply()
        notifyChanged()
    }

    fun isShowTypeIconOnThumbnails(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_TYPE_ICON, true)

    fun setShowTypeIconOnThumbnails(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_TYPE_ICON, enabled).apply()
        notifyChanged()
    }

    fun isShowRecentFiles(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_RECENT, true)

    fun setShowRecentFiles(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_RECENT, enabled).apply()
        notifyChanged()
    }

    fun isAppLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_APP_LOCK, false)

    fun setAppLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        notifyChanged()
    }

    fun isVaultEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VAULT_ENABLED, false)

    fun setVaultEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_VAULT_ENABLED, enabled).apply()
        notifyChanged()
    }

    fun getVaultPin(context: Context): String? =
        prefs(context).getString(KEY_VAULT_PIN, null)

    fun setVaultPin(context: Context, pin: String) {
        val hashed = VaultManager.hashSecret(pin)
        prefs(context).edit().putString(KEY_VAULT_PIN, hashed).apply()
        notifyChanged()
    }

    fun verifyVaultPin(context: Context, inputSecret: String): Boolean {
        val stored = prefs(context).getString(KEY_VAULT_PIN, null) ?: return false
        val hashedInput = VaultManager.hashSecret(inputSecret)
        if (stored == hashedInput) return true
        if (stored == inputSecret) {
            setVaultPin(context, inputSecret)
            return true
        }
        return false
    }

    fun getVaultType(context: Context): String =
        prefs(context).getString(KEY_VAULT_TYPE, "PIN") ?: "PIN"

    fun setVaultType(context: Context, type: String) {
        prefs(context).edit().putString(KEY_VAULT_TYPE, type).apply()
        notifyChanged()
    }

    fun getMediaPosition(context: Context, path: String): Long {
        val key = "media_pos_" + VaultManager.hashSecret(path)
        return prefs(context).getLong(key, 0L)
    }

    fun saveMediaPosition(context: Context, path: String, positionMs: Long) {
        val key = "media_pos_" + VaultManager.hashSecret(path)
        prefs(context).edit().putLong(key, positionMs).apply()
    }

    fun clearMediaPosition(context: Context, path: String) {
        val key = "media_pos_" + VaultManager.hashSecret(path)
        prefs(context).edit().remove(key).apply()
    }

    fun getRefreshRate(context: Context): Long =
        prefs(context).getLong(KEY_REFRESH_RATE, 100L)

    fun setRefreshRate(context: Context, rateMs: Long) {
        prefs(context).edit().putLong(KEY_REFRESH_RATE, rateMs).apply()
        notifyChanged()
    }

    fun getItemSizeMultiplier(context: Context): Float =
        prefs(context).getFloat(KEY_ITEM_SIZE_MULTIPLIER, 1.0f)

    fun setItemSizeMultiplier(context: Context, multiplier: Float) {
        prefs(context).edit().putFloat(KEY_ITEM_SIZE_MULTIPLIER, multiplier).apply()
        notifyChanged()
    }

    fun getFontSizeMultiplier(context: Context): Float =
        prefs(context).getFloat(KEY_FONT_SIZE_MULTIPLIER, 1.0f)

    fun setFontSizeMultiplier(context: Context, multiplier: Float) {
        prefs(context).edit().putFloat(KEY_FONT_SIZE_MULTIPLIER, multiplier).apply()
        notifyChanged()
    }

    /** Clears the chosen engine (and SAF tree, if any) so the picker shows again. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ENGINE).remove(KEY_SAF_TREE_URI).apply()
        notifyChanged()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
