package com.shizuku.filemanager.db

import android.content.Context
import androidx.room.*

@Entity(tableName = "file_tags")
data class FileTag(
    @PrimaryKey val path: String,
    val tags: String // comma-separated
)

@Entity(tableName = "document_annotations")
data class DocumentAnnotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val path: String,
    val pageIndex: Int,
    val content: String,
    val type: String, // "NOTE", "HIGHLIGHT"
    val color: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TagDao {
    @Query("SELECT * FROM file_tags WHERE path = :path")
    suspend fun getTags(path: String): FileTag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTags(tag: FileTag)

    @Query("SELECT * FROM file_tags")
    suspend fun getAll(): List<FileTag>
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM document_annotations WHERE path = :path")
    suspend fun getAnnotations(path: String): List<DocumentAnnotation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAnnotation(annotation: DocumentAnnotation)

    @Delete
    suspend fun deleteAnnotation(annotation: DocumentAnnotation)
}

@Database(entities = [FileTag::class, DocumentAnnotation::class], version = 2)
abstract class TagDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun annotationDao(): AnnotationDao

    companion object {
        @Volatile private var instance: TagDatabase? = null
        fun getDatabase(context: Context): TagDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(context, TagDatabase::class.java, "tag_db")
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { instance = it }
            }
    }
}
