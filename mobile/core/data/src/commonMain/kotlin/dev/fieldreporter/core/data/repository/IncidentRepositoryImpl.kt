package dev.fieldreporter.core.data.repository

import dev.fieldreporter.core.data.local.LocalStore
import dev.fieldreporter.core.data.local.PendingOp
import dev.fieldreporter.core.data.local.QueuedOp
import dev.fieldreporter.core.data.local.toEdit
import dev.fieldreporter.core.data.local.toPayload
import dev.fieldreporter.core.data.sync.SyncEngine
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.MAX_PHOTOS
import dev.fieldreporter.core.domain.NewIncident
import dev.fieldreporter.core.domain.NotEditableException
import dev.fieldreporter.core.domain.NotFoundException
import dev.fieldreporter.core.domain.Photo
import dev.fieldreporter.core.domain.PhotoLimitException
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.core.domain.SyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
internal class IncidentRepositoryImpl(
    private val store: LocalStore,
    private val engine: SyncEngine,
    private val scheduler: SyncScheduler,
    private val deleteFile: (String) -> Unit,
    private val clock: Clock = Clock.System,
    private val newId: () -> String = { Uuid.random().toString() },
) : IncidentRepository {

    override val incidents: Flow<List<Incident>> =
        store.observeCurrentUser().flatMapLatest { user ->
            if (user == null) emptyFlow() else store.observeIncidents(user.id)
        }

    override fun incident(id: String): Flow<Incident?> = store.observeIncident(id)

    override suspend fun report(new: NewIncident): Incident {
        if (new.photoPaths.size > MAX_PHOTOS) throw PhotoLimitException()
        val user = store.currentUser() ?: error("Choose who you are before reporting an incident")
        val now = clock.now()
        val id = newId()
        val photos = new.photoPaths.map { Photo(id = newId(), localPath = it, syncState = SyncState.PENDING) }
        val incident = Incident(
            id = id, title = new.title.trim(), description = new.description.trim(),
            category = new.category, severity = new.severity, status = Status.OPEN, location = new.location,
            photos = photos, reportedBy = user.id, assignee = null, resolutionNote = null, resolvedAt = null,
            activityLog = emptyList(), createdAt = now, updatedAt = now, syncState = SyncState.PENDING,
        )
        store.inTransaction {
            store.enqueue(PendingOp.Create(id))
            photos.forEach { store.enqueue(PendingOp.AddPhoto(id, it.id, it.localPath!!)) }
            store.save(incident)
        }
        scheduler.requestSync()
        return incident
    }

    override suspend fun edit(id: String, edit: IncidentEdit) {
        if (edit.isEmpty) return
        store.inTransaction {
            val row = editable(id)
            val ops = store.pendingOps()
            val createWaiting = ops.any { it.op is PendingOp.Create && it.op.incidentId == id }
            when {
                // The Create op sends the row as it is when it runs, so the edit rides along.
                createWaiting && !inFlight(row, ops, id) -> Unit
                else -> {
                    val existing = ops.firstOrNull { it.op is PendingOp.Update && it.op.incidentId == id && !isFirstInFlight(row, ops, it) }
                    if (existing != null) {
                        val merged = (existing.op as PendingOp.Update).edit.toEdit().mergedWith(edit)
                        store.replaceOp(existing.seq, PendingOp.Update(id, merged.toPayload()))
                    } else {
                        store.enqueue(PendingOp.Update(id, edit.toPayload()))
                    }
                }
            }
            store.save(
                edit.applyTo(row).copy(
                    updatedAt = clock.now(),
                    syncState = if (row.syncState == SyncState.SYNCING) SyncState.SYNCING else SyncState.PENDING,
                    syncFailure = null,
                ),
            )
        }
        scheduler.requestSync()
    }

    override suspend fun delete(id: String) {
        store.inTransaction {
            val row = editable(id)
            val ops = store.pendingOps()
            val neverReachedServer = ops.any { it.op is PendingOp.Create && it.op.incidentId == id } &&
                !inFlight(row, ops, id)
            if (neverReachedServer) {
                // Nothing on the server to delete: forget it locally.
                store.removeOpsFor(id)
                row.photos.forEach { p -> p.localPath?.let(deleteFile) }
                store.deleteIncident(id)
            } else {
                if (!inFlight(row, ops, id)) store.removeOpsFor(id)
                store.enqueue(PendingOp.Delete(id))
                store.markPendingDelete(id)
                store.save(row.copy(syncState = SyncState.PENDING, syncFailure = null))
            }
        }
        scheduler.requestSync()
    }

    override suspend fun addPhoto(incidentId: String, localPath: String) {
        store.inTransaction {
            val row = editable(incidentId)
            if (row.photos.size >= MAX_PHOTOS) throw PhotoLimitException()
            val photo = Photo(id = newId(), localPath = localPath, syncState = SyncState.PENDING)
            store.enqueue(PendingOp.AddPhoto(incidentId, photo.id, localPath))
            store.save(
                row.copy(
                    photos = row.photos + photo,
                    syncState = if (row.syncState == SyncState.SYNCING) SyncState.SYNCING else SyncState.PENDING,
                    syncFailure = null,
                ),
            )
        }
        scheduler.requestSync()
    }

    override suspend fun removePhoto(incidentId: String, photoId: String) {
        store.inTransaction {
            val row = editable(incidentId)
            val photo = row.photos.firstOrNull { it.id == photoId } ?: return@inTransaction
            val ops = store.pendingOps()
            val upload = ops.firstOrNull { it.op is PendingOp.AddPhoto && it.op.photoId == photoId }
            if (upload != null && !isFirstInFlight(row, ops, upload)) {
                // Never uploaded: cancel the upload and drop the local file.
                store.removeOp(upload.seq)
                photo.localPath?.let(deleteFile)
            } else {
                store.enqueue(PendingOp.RemovePhoto(incidentId, photoId))
            }
            store.save(
                row.copy(
                    photos = row.photos.filterNot { it.id == photoId },
                    syncState = if (row.syncState == SyncState.SYNCING) SyncState.SYNCING else SyncState.PENDING,
                    syncFailure = null,
                ),
            )
        }
        scheduler.requestSync()
    }

    override suspend fun dismissSyncFailure(id: String) {
        store.inTransaction {
            val row = store.incident(id) ?: return@inTransaction
            if (row.syncState == SyncState.FAILED) store.save(row.copy(syncState = SyncState.SYNCED, syncFailure = null))
        }
    }

    override suspend fun sync(): SyncOutcome = engine.sync()

    private suspend fun editable(id: String): Incident {
        val row = store.incident(id) ?: throw NotFoundException(id)
        if (!row.isEditable) throw NotEditableException()
        return row
    }

    /** True while the sync engine is sending this Incident's first queued op. */
    private fun inFlight(row: Incident, ops: List<QueuedOp>, id: String) =
        row.syncState == SyncState.SYNCING && ops.firstOrNull()?.op?.incidentId == id

    private fun isFirstInFlight(row: Incident, ops: List<QueuedOp>, queued: QueuedOp) =
        row.syncState == SyncState.SYNCING && ops.firstOrNull()?.seq == queued.seq
}
