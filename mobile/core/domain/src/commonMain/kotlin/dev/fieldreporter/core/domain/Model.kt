package dev.fieldreporter.core.domain

import kotlin.time.Instant

enum class Role { TECHNICIAN, OPERATOR }
enum class Category { EQUIPMENT, ROAD_HAZARD, SAFETY, OTHER }
enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }
enum class Status { OPEN, IN_PROGRESS, RESOLVED }

/** Whether a local change has reached the server. Separate from [Status]. */
enum class SyncState { PENDING, SYNCING, FAILED, SYNCED }

/** Why a change ended in [SyncState.FAILED]. */
enum class SyncFailureReason {
    /** An Operator already moved the Incident out of Open. The server version won. */
    CONFLICT,

    /** The server refused the change (validation or permissions). */
    REJECTED,
}

data class SyncFailure(val reason: SyncFailureReason, val message: String)

data class User(val id: String, val name: String, val role: Role)

data class Location(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
) {
    val hasCoordinates: Boolean get() = latitude != null && longitude != null
}

/** A Photo is either already on the server ([url]) or still a local file ([localPath]). */
data class Photo(
    val id: String,
    val url: String? = null,
    val localPath: String? = null,
    val syncState: SyncState = SyncState.SYNCED,
)

data class ActivityEntry(val at: Instant, val actorId: String, val status: Status)

data class Incident(
    val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val severity: Severity,
    val status: Status,
    val location: Location,
    val photos: List<Photo>,
    val reportedBy: String,
    val assignee: String?,
    val resolutionNote: String?,
    val resolvedAt: Instant?,
    val activityLog: List<ActivityEntry>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val syncState: SyncState = SyncState.SYNCED,
    val syncFailure: SyncFailure? = null,
) {
    /** A Technician may edit or delete only while the Incident is Open. */
    val isEditable: Boolean get() = status == Status.OPEN
    val canAddPhoto: Boolean get() = isEditable && photos.size < MAX_PHOTOS
}

const val MAX_PHOTOS = 5

/** What a Technician fills in to report an Incident. */
data class NewIncident(
    val title: String,
    val description: String,
    val category: Category,
    val severity: Severity,
    val location: Location,
    /** Already compressed local image files. */
    val photoPaths: List<String> = emptyList(),
)

/** A partial edit by a Technician. Null means "leave unchanged". */
data class IncidentEdit(
    val title: String? = null,
    val description: String? = null,
    val category: Category? = null,
    val severity: Severity? = null,
    val location: Location? = null,
) {
    val isEmpty: Boolean get() = title == null && description == null && category == null && severity == null && location == null

    /** Later edits win field by field. */
    fun mergedWith(later: IncidentEdit) = IncidentEdit(
        title = later.title ?: title,
        description = later.description ?: description,
        category = later.category ?: category,
        severity = later.severity ?: severity,
        location = later.location ?: location,
    )

    fun applyTo(incident: Incident) = incident.copy(
        title = title ?: incident.title,
        description = description ?: incident.description,
        category = category ?: incident.category,
        severity = severity ?: incident.severity,
        location = location ?: incident.location,
    )
}
