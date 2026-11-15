package dev.fieldreporter.core.data.remote

import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Photo
import dev.fieldreporter.core.domain.User

/** What can go wrong talking to the server, reduced to what the sync engine must decide on. */
sealed class RemoteException(message: String) : Exception(message) {
    /** No connection, a timeout or a server error. Try again later. */
    class Network(message: String = "Network unavailable") : RemoteException(message)

    /** `409`: the Incident is no longer Open. */
    class Conflict(message: String) : RemoteException(message)

    /** `404` on a read. */
    class NotFound(message: String) : RemoteException(message)

    /** `401`: no usable Current User yet. */
    class Unauthorized(message: String) : RemoteException(message)

    /** Any other `4xx`: retrying the same request will not help. */
    class Rejected(val status: Int, message: String) : RemoteException(message)
}

internal interface IncidentRemote {
    suspend fun users(): List<User>
    suspend fun listMine(userId: String): List<Incident>
    suspend fun get(id: String): Incident

    /** Idempotent by [Incident.id]. */
    suspend fun create(incident: Incident): Incident
    suspend fun update(id: String, edit: IncidentEdit): Incident
    suspend fun delete(id: String)

    /** Idempotent by [photoId]. */
    suspend fun uploadPhoto(incidentId: String, photoId: String, localPath: String): Photo
    suspend fun deletePhoto(incidentId: String, photoId: String)
}
