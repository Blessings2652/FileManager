package com.shizuku.filemanager.db

import android.content.Context
import androidx.room.*

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey val path: String,
    val label: String,
    val engineType: String,
    val sortOrder: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: String,
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val srcParentPath: String,
    val engineType: String,
    val mode: String // "CUT" or "COPY"
)

@Entity(tableName = "file_versions")
data class FileVersion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val snapshotPath: String,
    val engineType: String,
    val reason: String, // "OVERWRITE" or "DELETE"
    val sizeBytes: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY sortOrder ASC")
    suspend fun getAll(): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE path = :path AND engineType = :engineType")
    suspend fun remove(path: String, engineType: String)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE path = :path AND engineType = :engineType")
    suspend fun isBookmarked(path: String, engineType: String): Int
}

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY id ASC")
    suspend fun getAll(): List<ClipboardItem>

    @Insert
    suspend fun addAll(items: List<ClipboardItem>)

    @Query("DELETE FROM clipboard_items")
    suspend fun clear()
}

@Dao
interface FileVersionDao {
    @Query("SELECT * FROM file_versions WHERE originalPath = :originalPath ORDER BY timestamp DESC")
    suspend fun getVersions(originalPath: String): List<FileVersion>

    @Insert
    suspend fun add(version: FileVersion): Long

    @Delete
    suspend fun delete(version: FileVersion)

    @Query("SELECT * FROM file_versions ORDER BY timestamp DESC")
    suspend fun getAllVersions(): List<FileVersion>
}

@Database(entities = [Bookmark::class, ClipboardItem::class, FileVersion::class], version = 1)
abstract class ExtrasDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun fileVersionDao(): FileVersionDao

    companion object {
        @Volatile private var INSTANCE: ExtrasDatabase? = null

        fun getDatabase(context: Context): ExtrasDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExtrasDatabase::class.java,
                    "extras_database"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
