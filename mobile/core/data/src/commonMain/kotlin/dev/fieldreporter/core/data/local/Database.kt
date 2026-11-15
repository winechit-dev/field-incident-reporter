package dev.fieldreporter.core.data.local

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "incidents")
internal data class IncidentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val severity: String,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val photosJson: String,
    val reportedBy: String,
    val assignee: String?,
    val resolutionNote: String?,
    val resolvedAt: String?,
    val activityLogJson: String,
    val createdAt: String,
    val updatedAt: String,
    val syncState: String,
    val failureReason: String?,
    val failureMessage: String?,
    val pendingDelete: Boolean,
)

@Entity(tableName = "outbox")
internal data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val incidentId: String,
    val payload: String,
)

@Entity(tableName = "kv")
internal data class KvEntity(@PrimaryKey val key: String, val value: String)

@Dao
internal interface IncidentDao {
    @Query("SELECT * FROM incidents WHERE reportedBy = :userId AND pendingDelete = 0 ORDER BY createdAt DESC")
    fun observeForUser(userId: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id AND pendingDelete = 0")
    fun observe(id: String): Flow<IncidentEntity?>

    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun get(id: String): IncidentEntity?

    @Query("SELECT * FROM incidents WHERE reportedBy = :userId")
    suspend fun allFor(userId: String): List<IncidentEntity>

    @Upsert
    suspend fun upsert(entity: IncidentEntity)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE incidents SET pendingDelete = 1 WHERE id = :id")
    suspend fun markPendingDelete(id: String)
}

@Dao
internal interface OutboxDao {
    @Insert
    suspend fun insert(entity: OutboxEntity): Long

    @Query("SELECT * FROM outbox ORDER BY seq")
    suspend fun all(): List<OutboxEntity>

    @Update
    suspend fun update(entity: OutboxEntity)

    @Query("DELETE FROM outbox WHERE seq = :seq")
    suspend fun delete(seq: Long)

    @Query("DELETE FROM outbox WHERE incidentId = :incidentId")
    suspend fun deleteFor(incidentId: String)
}

@Dao
internal interface KvDao {
    @Upsert
    suspend fun set(entity: KvEntity)

    @Query("DELETE FROM kv WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM kv WHERE `key` = :key")
    suspend fun get(key: String): KvEntity?

    @Query("SELECT * FROM kv WHERE `key` = :key")
    fun observe(key: String): Flow<KvEntity?>
}

@Database(entities = [IncidentEntity::class, OutboxEntity::class, KvEntity::class], version = 1, exportSchema = true)
@ConstructedBy(FieldReporterDatabaseConstructor::class)
internal abstract class FieldReporterDatabase : RoomDatabase() {
    abstract fun incidents(): IncidentDao
    abstract fun outbox(): OutboxDao
    abstract fun kv(): KvDao
}

// Room generates the actual implementations for each platform.
@Suppress("KotlinNoActualForExpect")
internal expect object FieldReporterDatabaseConstructor : RoomDatabaseConstructor<FieldReporterDatabase> {
    override fun initialize(): FieldReporterDatabase
}
