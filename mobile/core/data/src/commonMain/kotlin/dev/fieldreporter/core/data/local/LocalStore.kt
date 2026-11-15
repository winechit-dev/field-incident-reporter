package dev.fieldreporter.core.data.local

import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.User
import kotlinx.coroutines.flow.Flow

/** One queued change waiting to reach the server, in the order it was made. */
internal data class QueuedOp(val seq: Long, val op: PendingOp)

/**
 * The on-device source of truth: Incidents, the outbox of pending changes, and the Current User.
 *
 * [inTransaction] makes a read-modify-write step atomic with respect to other callers. It is not
 * reentrant, so never nest it. Callers enqueue the outbox op *before* saving the row, so a crash
 * in between leaves an op with no row (which the sync engine drops) rather than a row that never syncs.
 */
internal interface LocalStore {
    fun observeIncidents(userId: String): Flow<List<Incident>>
    fun observeIncident(id: String): Flow<Incident?>

    suspend fun incident(id: String): Incident?
    suspend fun incidentsFor(userId: String): List<Incident>
    suspend fun save(incident: Incident)
    suspend fun deleteIncident(id: String)

    /** Save and also show the Incident again if its delete was waiting to sync. */
    suspend fun restore(incident: Incident)

    /** Hide an Incident from the lists while its delete waits to sync. */
    suspend fun markPendingDelete(id: String)

    suspend fun enqueue(op: PendingOp): Long
    suspend fun pendingOps(): List<QueuedOp>
    suspend fun replaceOp(seq: Long, op: PendingOp)
    suspend fun removeOp(seq: Long)
    suspend fun removeOpsFor(incidentId: String)

    fun observeCurrentUser(): Flow<User?>
    suspend fun currentUser(): User?
    suspend fun setCurrentUser(user: User)
    suspend fun clearCurrentUser()

    suspend fun <T> inTransaction(block: suspend () -> T): T
}
