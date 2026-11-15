package dev.fieldreporter.core.data

import dev.fieldreporter.core.data.local.LocalStore
import dev.fieldreporter.core.data.local.PendingOp
import dev.fieldreporter.core.data.local.QueuedOp
import dev.fieldreporter.core.data.remote.IncidentRemote
import dev.fieldreporter.core.data.remote.RemoteException
import dev.fieldreporter.core.data.repository.IncidentRepositoryImpl
import dev.fieldreporter.core.data.sync.SyncEngine
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Photo
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.core.domain.SyncState
import dev.fieldreporter.core.domain.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

internal val technician = User("tech-1", "Aung Kyaw", Role.TECHNICIAN)

internal class FakeLocalStore : LocalStore {
    private val rows = MutableStateFlow<Map<String, Incident>>(emptyMap())
    private val hidden = mutableSetOf<String>()
    private val outbox = mutableListOf<QueuedOp>()
    private var nextSeq = 1L
    private val user = MutableStateFlow<User?>(null)

    override fun observeIncidents(userId: String): Flow<List<Incident>> = rows.map { m ->
        m.values.filter { it.reportedBy == userId && it.id !in hidden }.sortedByDescending { it.createdAt }
    }

    override fun observeIncident(id: String): Flow<Incident?> = rows.map { if (id in hidden) null else it[id] }
    override suspend fun incident(id: String) = rows.value[id]
    override suspend fun incidentsFor(userId: String) = rows.value.values.filter { it.reportedBy == userId }
    override suspend fun save(incident: Incident) { rows.value = rows.value + (incident.id to incident) }
    override suspend fun restore(incident: Incident) { hidden.remove(incident.id); save(incident) }
    override suspend fun deleteIncident(id: String) { hidden.remove(id); rows.value = rows.value - id }
    override suspend fun markPendingDelete(id: String) { hidden.add(id) }
    override suspend fun enqueue(op: PendingOp): Long = nextSeq++.also { outbox.add(QueuedOp(it, op)) }
    override suspend fun pendingOps() = outbox.toList()
    override suspend fun replaceOp(seq: Long, op: PendingOp) {
        val i = outbox.indexOfFirst { it.seq == seq }
        outbox[i] = QueuedOp(seq, op)
    }
    override suspend fun removeOp(seq: Long) { outbox.removeAll { it.seq == seq } }
    override suspend fun removeOpsFor(incidentId: String) { outbox.removeAll { it.op.incidentId == incidentId } }
    override fun observeCurrentUser(): Flow<User?> = user
    override suspend fun currentUser() = user.value
    override suspend fun setCurrentUser(user: User) { this.user.value = user }
    override suspend fun clearCurrentUser() { user.value = null }
    override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
}

/** A scriptable server. It behaves like the real one: Create is idempotent and only Open Incidents change. */
internal class FakeRemote : IncidentRemote {
    var online = true
    val server = mutableMapOf<String, Incident>()
    val calls = mutableListOf<String>()
    var rejectCreateWith: String? = null
    private var counter = 0

    private fun gate(call: String) {
        calls += call
        if (!online) throw RemoteException.Network()
    }

    private fun requireOpen(id: String): Incident {
        val row = server[id] ?: throw RemoteException.NotFound("missing")
        if (row.status != Status.OPEN) throw RemoteException.Conflict("Incident is already being handled")
        return row
    }

    /** What an Operator does on the dashboard. */
    fun operatorSetsStatus(id: String, status: Status) {
        server[id] = server.getValue(id).copy(status = status, updatedAt = Instant.fromEpochSeconds(9_000))
    }

    override suspend fun users() = listOf(technician)
    override suspend fun listMine(userId: String): List<Incident> {
        gate("list")
        return server.values.filter { it.reportedBy == userId }
    }

    override suspend fun get(id: String): Incident {
        gate("get $id")
        return server[id] ?: throw RemoteException.NotFound("missing")
    }

    override suspend fun create(incident: Incident): Incident {
        gate("create ${incident.id}")
        rejectCreateWith?.let { throw RemoteException.Rejected(422, it) }
        return server.getOrPut(incident.id) {
            incident.copy(photos = emptyList(), syncState = SyncState.SYNCED, updatedAt = Instant.fromEpochSeconds(100))
        }
    }

    override suspend fun update(id: String, edit: IncidentEdit): Incident {
        gate("update $id")
        val updated = edit.applyTo(requireOpen(id)).copy(updatedAt = Instant.fromEpochSeconds(200))
        server[id] = updated
        return updated
    }

    override suspend fun delete(id: String) {
        gate("delete $id")
        if (server[id] != null) requireOpen(id)
        server.remove(id)
    }

    override suspend fun uploadPhoto(incidentId: String, photoId: String, localPath: String): Photo {
        gate("upload $photoId")
        val row = requireOpen(incidentId)
        val photo = Photo(photoId, url = "http://server/uploads/$photoId.jpg")
        if (row.photos.none { it.id == photoId }) server[incidentId] = row.copy(photos = row.photos + photo)
        return photo
    }

    override suspend fun deletePhoto(incidentId: String, photoId: String) {
        gate("deletePhoto $photoId")
        val row = requireOpen(incidentId)
        server[incidentId] = row.copy(photos = row.photos.filterNot { it.id == photoId })
    }
}

internal class Harness {
    val store = FakeLocalStore()
    val remote = FakeRemote()
    val deletedFiles = mutableListOf<String>()
    var syncRequests = 0
    private var ids = 0
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochSeconds(50)
    }
    val engine = SyncEngine(store, remote) { deletedFiles += it }
    val repo = IncidentRepositoryImpl(
        store = store,
        engine = engine,
        scheduler = SyncScheduler { syncRequests++ },
        deleteFile = { deletedFiles += it },
        clock = clock,
        newId = { "id-${++ids}" },
    )

    suspend fun signIn() = store.setCurrentUser(technician)
}
