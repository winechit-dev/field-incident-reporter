package dev.fieldreporter.core.domain

import kotlinx.coroutines.flow.Flow

/** Thrown when a Technician tries to change an Incident that is no longer Open. */
class NotEditableException(message: String = "This incident is already being handled") : Exception(message)

/** Thrown when an Incident already has [MAX_PHOTOS] photos. */
class PhotoLimitException : Exception("An incident can have at most $MAX_PHOTOS photos")

class NotFoundException(id: String) : Exception("Incident $id not found")

/** The server could not be reached, so a read that needs it (such as the user list) failed. */
class ServerUnavailableException(message: String = "Can't reach the server") : Exception(message)

enum class SyncOutcome {
    /** Everything pushed and pulled. */
    COMPLETED,

    /** The network failed. The queue is intact, so the scheduler should retry with backoff. */
    RETRY,
}

/**
 * The Technician's view of their Incidents. Room is the source of truth: every write lands
 * locally first and is synced later, so all of it works offline.
 */
interface IncidentRepository {
    /** The Current User's Incidents, newest first. */
    val incidents: Flow<List<Incident>>

    fun incident(id: String): Flow<Incident?>

    suspend fun report(new: NewIncident): Incident

    /** @throws NotEditableException if the Incident is not Open. */
    suspend fun edit(id: String, edit: IncidentEdit)

    /** @throws NotEditableException if the Incident is not Open. */
    suspend fun delete(id: String)

    /** @throws NotEditableException if not Open, [PhotoLimitException] if there are already five. */
    suspend fun addPhoto(incidentId: String, localPath: String)

    suspend fun removePhoto(incidentId: String, photoId: String)

    /** Acknowledge a failed sync: the notice goes away and the server's version stands. */
    suspend fun dismissSyncFailure(id: String)

    /** Push the queue, then pull server changes. */
    suspend fun sync(): SyncOutcome
}

/** The seeded users and the "choose who you are" picker. There is no login. */
interface UserRepository {
    val currentUser: Flow<User?>

    /** Fetch the seeded users from the server. @throws ServerUnavailableException if it can't be reached. */
    suspend fun availableUsers(): List<User>

    /**
     * The Operator with [id] from the user list the last sync (or picker fetch) stored on this
     * device, or null when the list was never stored, does not hold [id], or [id] is not an
     * Operator. Works offline and emits again when the stored list changes.
     */
    fun knownOperator(id: String): Flow<User?>

    suspend fun select(user: User)

    /** Forget the Current User so the picker shows again. Local Incidents stay on the device. */
    suspend fun signOut()
}

/** Asks the platform to run [IncidentRepository.sync] soon (WorkManager on Android, BGTask on iOS). */
fun interface SyncScheduler {
    fun requestSync()
}
