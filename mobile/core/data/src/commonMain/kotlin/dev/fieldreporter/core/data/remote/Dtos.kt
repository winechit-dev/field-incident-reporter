package dev.fieldreporter.core.data.remote

import dev.fieldreporter.core.domain.ActivityEntry
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.core.domain.Photo
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncState
import dev.fieldreporter.core.domain.User
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class LocationDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
)

@Serializable
internal data class PhotoDto(val id: String, val url: String)

@Serializable
internal data class ActivityDto(val at: String, val actorId: String, val status: String)

@Serializable
internal data class IncidentDto(
    val id: String,
    val title: String,
    val description: String = "",
    val category: String,
    val severity: String,
    val status: String,
    val location: LocationDto = LocationDto(),
    val photos: List<PhotoDto> = emptyList(),
    val reportedBy: String,
    val assignee: String? = null,
    val resolutionNote: String? = null,
    val resolvedAt: String? = null,
    val activityLog: List<ActivityDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
internal data class CreateIncidentRequest(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val severity: String,
    val location: LocationDto,
)

/** Null fields are omitted when encoding, so only changed fields are sent. */
@Serializable
internal data class UpdateIncidentRequest(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val severity: String? = null,
    val location: LocationDto? = null,
)

@Serializable
internal data class UserDto(val id: String, val name: String, val role: String)

@Serializable
internal data class ErrorEnvelope(val error: ErrorBody)

@Serializable
internal data class ErrorBody(val code: String = "", val message: String = "")

internal fun LocationDto.toDomain() = Location(latitude, longitude, address)
internal fun Location.toDto() = LocationDto(latitude, longitude, address)

internal fun PhotoDto.toDomain() = Photo(id = id, url = url, localPath = null, syncState = SyncState.SYNCED)
internal fun ActivityDto.toDomain() = ActivityEntry(Instant.parse(at), actorId, Status.valueOf(status))

internal fun IncidentDto.toDomain() = Incident(
    id = id,
    title = title,
    description = description,
    category = Category.valueOf(category),
    severity = Severity.valueOf(severity),
    status = Status.valueOf(status),
    location = location.toDomain(),
    photos = photos.map { it.toDomain() },
    reportedBy = reportedBy,
    assignee = assignee,
    resolutionNote = resolutionNote,
    resolvedAt = resolvedAt?.let(Instant::parse),
    activityLog = activityLog.map { it.toDomain() },
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    syncState = SyncState.SYNCED,
)

internal fun Incident.toCreateRequest() = CreateIncidentRequest(
    id = id,
    title = title,
    description = description,
    category = category.name,
    severity = severity.name,
    location = location.toDto(),
)

internal fun IncidentEdit.toRequest() = UpdateIncidentRequest(
    title = title,
    description = description,
    category = category?.name,
    severity = severity?.name,
    location = location?.toDto(),
)

/** Null when [UserDto.role] is a role this build does not know. */
internal fun UserDto.toDomainOrNull(): User? = Role.entries.firstOrNull { it.name == role }?.let { User(id, name, it) }

/** Maps user by user: a user with an unknown role is skipped, the rest are kept. */
internal fun List<UserDto>.toDomain(): List<User> = mapNotNull { it.toDomainOrNull() }
