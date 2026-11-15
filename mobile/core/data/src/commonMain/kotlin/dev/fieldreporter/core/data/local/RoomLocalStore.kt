package dev.fieldreporter.core.data.local

import dev.fieldreporter.core.data.remote.ActivityDto
import dev.fieldreporter.core.data.remote.toDomain
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.core.domain.Photo
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncFailure
import dev.fieldreporter.core.domain.SyncFailureReason
import dev.fieldreporter.core.domain.SyncState
import dev.fieldreporter.core.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.Instant

@Serializable
private data class StoredPhoto(val id: String, val url: String? = null, val localPath: String? = null, val syncState: String)

@Serializable
private data class StoredUser(val id: String, val name: String, val role: String)

internal class RoomLocalStore(private val db: FieldReporterDatabase) : LocalStore {
    private val lock = Mutex()
    private val photoList = ListSerializer(StoredPhoto.serializer())
    private val activityList = ListSerializer(ActivityDto.serializer())

    override fun observeIncidents(userId: String): Flow<List<Incident>> =
        db.incidents().observeForUser(userId).map { rows -> rows.map { it.toDomain() } }

    override fun observeIncident(id: String): Flow<Incident?> = db.incidents().observe(id).map { it?.toDomain() }
    override suspend fun incident(id: String) = db.incidents().get(id)?.toDomain()
    override suspend fun incidentsFor(userId: String) = db.incidents().allFor(userId).map { it.toDomain() }

    override suspend fun save(incident: Incident) {
        val pendingDelete = db.incidents().get(incident.id)?.pendingDelete ?: false
        db.incidents().upsert(incident.toEntity(pendingDelete))
    }

    override suspend fun restore(incident: Incident) = db.incidents().upsert(incident.toEntity(pendingDelete = false))

    override suspend fun deleteIncident(id: String) = db.incidents().delete(id)
    override suspend fun markPendingDelete(id: String) = db.incidents().markPendingDelete(id)

    override suspend fun enqueue(op: PendingOp): Long =
        db.outbox().insert(OutboxEntity(incidentId = op.incidentId, payload = op.encode()))

    override suspend fun pendingOps() = db.outbox().all().map { QueuedOp(it.seq, decodePendingOp(it.payload)) }
    override suspend fun replaceOp(seq: Long, op: PendingOp) =
        db.outbox().update(OutboxEntity(seq, op.incidentId, op.encode()))

    override suspend fun removeOp(seq: Long) = db.outbox().delete(seq)
    override suspend fun removeOpsFor(incidentId: String) = db.outbox().deleteFor(incidentId)

    override fun observeCurrentUser(): Flow<User?> = db.kv().observe(CURRENT_USER).map { it?.toUser() }
    override suspend fun currentUser(): User? = db.kv().get(CURRENT_USER)?.toUser()
    override suspend fun setCurrentUser(user: User) {
        val stored = StoredUser(user.id, user.name, user.role.name)
        db.kv().set(KvEntity(CURRENT_USER, OpJson.encodeToString(StoredUser.serializer(), stored)))
    }

    override suspend fun clearCurrentUser() = db.kv().delete(CURRENT_USER)

    override fun observeKnownUsers(): Flow<List<User>> = db.kv().observe(KNOWN_USERS).map { decodeKnownUsers(it?.value) }

    override suspend fun setKnownUsers(users: List<User>) {
        val stored = users.map { StoredUser(it.id, it.name, it.role.name) }
        db.kv().set(KvEntity(KNOWN_USERS, OpJson.encodeToString(storedUserList, stored)))
    }

    override suspend fun <T> inTransaction(block: suspend () -> T): T = lock.withLock { block() }

    private fun KvEntity.toUser(): User {
        val s = OpJson.decodeFromString(StoredUser.serializer(), value)
        return User(s.id, s.name, Role.valueOf(s.role))
    }

    private fun IncidentEntity.toDomain() = Incident(
        id = id, title = title, description = description,
        category = Category.valueOf(category), severity = Severity.valueOf(severity), status = Status.valueOf(status),
        location = Location(latitude, longitude, address),
        photos = OpJson.decodeFromString(photoList, photosJson)
            .map { Photo(it.id, it.url, it.localPath, SyncState.valueOf(it.syncState)) },
        reportedBy = reportedBy, assignee = assignee, resolutionNote = resolutionNote,
        resolvedAt = resolvedAt?.let(Instant::parse),
        activityLog = OpJson.decodeFromString(activityList, activityLogJson).map { it.toDomain() },
        createdAt = Instant.parse(createdAt), updatedAt = Instant.parse(updatedAt),
        syncState = SyncState.valueOf(syncState),
        syncFailure = failureReason?.let { SyncFailure(SyncFailureReason.valueOf(it), failureMessage.orEmpty()) },
    )

    private fun Incident.toEntity(pendingDelete: Boolean) = IncidentEntity(
        id = id, title = title, description = description,
        category = category.name, severity = severity.name, status = status.name,
        latitude = location.latitude, longitude = location.longitude, address = location.address,
        photosJson = OpJson.encodeToString(
            photoList,
            photos.map { StoredPhoto(it.id, it.url, it.localPath, it.syncState.name) },
        ),
        reportedBy = reportedBy, assignee = assignee, resolutionNote = resolutionNote,
        resolvedAt = resolvedAt?.toString(),
        activityLogJson = OpJson.encodeToString(
            activityList,
            activityLog.map { ActivityDto(it.at.toString(), it.actorId, it.status.name) },
        ),
        createdAt = createdAt.toString(), updatedAt = updatedAt.toString(),
        syncState = syncState.name,
        failureReason = syncFailure?.reason?.name, failureMessage = syncFailure?.message,
        pendingDelete = pendingDelete,
    )

    private companion object {
        const val CURRENT_USER = "current_user"
        const val KNOWN_USERS = "known_users"
    }
}

private val storedUserList = ListSerializer(StoredUser.serializer())

/**
 * The stored `known_users` value. A missing or undecodable value reads as "no list", so the screen
 * falls back instead of crashing; a user whose role this build does not know is skipped.
 */
internal fun decodeKnownUsers(value: String?): List<User> {
    if (value == null) return emptyList()
    return try {
        // User by user, so one role this build does not know drops that user only.
        OpJson.decodeFromString(storedUserList, value).mapNotNull { s ->
            Role.entries.firstOrNull { it.name == s.role }?.let { User(s.id, s.name, it) }
        }
    } catch (_: SerializationException) {
        emptyList()
    }
}
