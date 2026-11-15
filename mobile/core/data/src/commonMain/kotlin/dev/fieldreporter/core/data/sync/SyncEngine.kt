package dev.fieldreporter.core.data.sync

import dev.fieldreporter.core.data.local.LocalStore
import dev.fieldreporter.core.data.local.PendingOp
import dev.fieldreporter.core.data.local.QueuedOp
import dev.fieldreporter.core.data.local.toEdit
import dev.fieldreporter.core.data.remote.IncidentRemote
import dev.fieldreporter.core.data.remote.RemoteException
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.SyncFailure
import dev.fieldreporter.core.domain.SyncFailureReason
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.SyncState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pushes the outbox to the server in order, then pulls server changes.
 *
 * - A network failure stops the run and leaves the queue intact ([SyncOutcome.RETRY]).
 * - A conflict (the Incident is no longer Open) makes the server's version win: the local change
 *   is discarded and the Incident is marked Failed with the reason.
 * - Any other rejection marks the Incident Failed and drops its queued changes.
 */
internal class SyncEngine(
    private val store: LocalStore,
    private val remote: IncidentRemote,
    private val deleteFile: (String) -> Unit,
) {
    private val running = Mutex()

    suspend fun sync(): SyncOutcome = running.withLock {
        if (!push()) return SyncOutcome.RETRY
        if (!pull()) return SyncOutcome.RETRY
        SyncOutcome.COMPLETED
    }

    /** @return false if the run must stop and be retried later. */
    private suspend fun push(): Boolean {
        while (true) {
            val queued = store.pendingOps().firstOrNull() ?: return true
            val id = queued.op.incidentId
            if (store.incident(id) == null) {
                store.removeOp(queued.seq) // the Incident vanished locally; nothing to send
                continue
            }
            setSyncing(id)
            try {
                execute(queued)
                store.removeOp(queued.seq)
                settle(id)
            } catch (e: RemoteException.Conflict) {
                if (!resolveConflict(id, e.message.orEmpty())) return false
            } catch (e: RemoteException.Rejected) {
                reject(id, e.message.orEmpty())
            } catch (e: RemoteException.Network) {
                backToPending(id)
                return false
            } catch (e: RemoteException.Unauthorized) {
                backToPending(id)
                return false
            } catch (e: RemoteException.NotFound) {
                reject(id, e.message.orEmpty())
            }
        }
    }

    private suspend fun execute(queued: QueuedOp) {
        when (val op = queued.op) {
            is PendingOp.Create -> {
                val local = store.incident(op.incidentId) ?: return
                mergeServer(remote.create(local))
            }
            is PendingOp.Update -> mergeServer(remote.update(op.incidentId, op.edit.toEdit()))
            is PendingOp.Delete -> {
                remote.delete(op.incidentId)
                store.incident(op.incidentId)?.photos?.forEach { p -> p.localPath?.let(deleteFile) }
                store.removeOpsFor(op.incidentId)
                store.deleteIncident(op.incidentId)
            }
            is PendingOp.AddPhoto -> {
                val photo = remote.uploadPhoto(op.incidentId, op.photoId, op.localPath)
                store.inTransaction {
                    store.incident(op.incidentId)?.let { row ->
                        val photos = row.photos.map { if (it.id == op.photoId) photo else it }
                        store.save(row.copy(photos = photos))
                    }
                }
                deleteFile(op.localPath)
            }
            is PendingOp.RemovePhoto -> remote.deletePhoto(op.incidentId, op.photoId)
        }
    }

    /** Take the server-owned fields from [server] and keep the local, possibly newer, content. */
    private suspend fun mergeServer(server: Incident) = store.inTransaction {
        val row = store.incident(server.id) ?: return@inTransaction
        store.save(
            row.copy(
                status = server.status,
                assignee = server.assignee,
                resolutionNote = server.resolutionNote,
                resolvedAt = server.resolvedAt,
                activityLog = server.activityLog,
                createdAt = server.createdAt,
                updatedAt = server.updatedAt,
            ),
        )
    }

    private suspend fun setSyncing(id: String) = store.inTransaction {
        store.incident(id)?.let { store.save(it.copy(syncState = SyncState.SYNCING)) }
    }

    private suspend fun backToPending(id: String) = store.inTransaction {
        store.incident(id)?.let { store.save(it.copy(syncState = SyncState.PENDING)) }
    }

    /** After an op succeeds: Pending if more changes wait for this Incident, otherwise Synced. */
    private suspend fun settle(id: String) = store.inTransaction {
        val row = store.incident(id) ?: return@inTransaction
        val more = store.pendingOps().any { it.op.incidentId == id }
        store.save(
            row.copy(
                syncState = if (more) SyncState.PENDING else SyncState.SYNCED,
                syncFailure = if (more) row.syncFailure else null,
            ),
        )
    }

    /** @return false if the server version could not be fetched (retry later). */
    private suspend fun resolveConflict(id: String, message: String): Boolean {
        val server = try {
            remote.get(id)
        } catch (e: RemoteException.NotFound) {
            discardLocal(id)
            return true
        } catch (e: RemoteException.Network) {
            backToPending(id)
            return false
        } catch (e: RemoteException.Unauthorized) {
            backToPending(id)
            return false
        } catch (e: RemoteException.Rejected) {
            reject(id, e.message.orEmpty())
            return true
        } catch (e: RemoteException.Conflict) {
            reject(id, e.message.orEmpty())
            return true
        }
        store.inTransaction {
            store.incident(id)?.photos?.forEach { p -> p.localPath?.let(deleteFile) }
            store.removeOpsFor(id)
            store.restore(
                server.copy(
                    syncState = SyncState.FAILED,
                    syncFailure = SyncFailure(SyncFailureReason.CONFLICT, message),
                ),
            )
        }
        return true
    }

    private suspend fun discardLocal(id: String) = store.inTransaction {
        store.incident(id)?.photos?.forEach { p -> p.localPath?.let(deleteFile) }
        store.removeOpsFor(id)
        store.deleteIncident(id)
    }

    private suspend fun reject(id: String, message: String) = store.inTransaction {
        store.removeOpsFor(id)
        store.incident(id)?.let {
            store.restore(
                it.copy(syncState = SyncState.FAILED, syncFailure = SyncFailure(SyncFailureReason.REJECTED, message)),
            )
        }
    }

    /**
     * Merge the server's list into Room. Rows with unsynced changes win until they sync; rows
     * that no longer exist on the server are dropped once they have nothing pending.
     */
    private suspend fun pull(): Boolean {
        val user = store.currentUser() ?: return true
        val remoteRows = try {
            remote.listMine(user.id)
        } catch (e: RemoteException) {
            return false
        }
        store.inTransaction {
            val remoteIds = remoteRows.map { it.id }.toSet()
            val locals = store.incidentsFor(user.id).associateBy { it.id }
            for (server in remoteRows) {
                val local = locals[server.id]
                when {
                    local == null -> store.save(server)
                    local.syncState == SyncState.PENDING || local.syncState == SyncState.SYNCING -> Unit
                    local.syncState == SyncState.FAILED && local.syncFailure?.reason == SyncFailureReason.REJECTED -> Unit
                    local.syncState == SyncState.FAILED ->
                        store.save(server.copy(syncState = SyncState.FAILED, syncFailure = local.syncFailure))
                    else -> store.save(server)
                }
            }
            for (local in locals.values) {
                if (local.id !in remoteIds && local.syncState == SyncState.SYNCED) {
                    local.photos.forEach { p -> p.localPath?.let(deleteFile) }
                    store.deleteIncident(local.id)
                }
            }
        }
        return true
    }
}
