package com.hermes.downloader.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "download_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "format") val format: String,
    @ColumnInfo(name = "quality") val quality: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "format") val format: String = "mp4",
    @ColumnInfo(name = "quality") val quality: String = "best",
    @ColumnInfo(name = "state") val state: String = "QUEUED",
    @ColumnInfo(name = "progress") val progress: Int = 0,
    @ColumnInfo(name = "priority") val priority: Int = 0,
    @ColumnInfo(name = "file_path") val filePath: String = "",
    @ColumnInfo(name = "error") val error: String = "",
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "value") val value: String
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM download_history ORDER BY created_at DESC LIMIT 200")
    fun getAll(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM download_history WHERE file_path = :filePath")
    suspend fun deleteByFilePath(filePath: String)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY priority DESC, created_at ASC")
    fun getAll(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Query("UPDATE tasks SET state = :state WHERE id = :taskId")
    suspend fun updateState(taskId: String, state: String)

    @Query("UPDATE tasks SET progress = :progress, state = 'DOWNLOADING' WHERE id = :taskId")
    suspend fun updateProgress(taskId: String, progress: Int)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun delete(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE key = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entity: SettingsEntity)

    @Query("DELETE FROM settings WHERE key = :key")
    suspend fun delete(key: String)
}

@Database(
    entities = [HistoryEntity::class, TaskEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class YTDowDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun taskDao(): TaskDao
    abstract fun settingsDao(): SettingsDao
}
