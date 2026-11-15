package dev.fieldreporter.core.data.local

import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.data.remote.LocationDto
import dev.fieldreporter.core.data.remote.toDomain
import dev.fieldreporter.core.data.remote.toDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A change queued while offline. Stored as JSON in the outbox. */
@Serializable
internal sealed interface PendingOp {
    val incidentId: String

    /** Sends the Incident as it is in the local row when the op runs, so earlier edits are folded in. */
    @Serializable @SerialName("create")
    data class Create(override val incidentId: String) : PendingOp

    @Serializable @SerialName("update")
    data class Update(override val incidentId: String, val edit: EditPayload) : PendingOp

    @Serializable @SerialName("delete")
    data class Delete(override val incidentId: String) : PendingOp

    @Serializable @SerialName("addPhoto")
    data class AddPhoto(override val incidentId: String, val photoId: String, val localPath: String) : PendingOp

    @Serializable @SerialName("removePhoto")
    data class RemovePhoto(override val incidentId: String, val photoId: String) : PendingOp
}

@Serializable
internal data class EditPayload(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val severity: String? = null,
    val location: LocationDto? = null,
)

internal fun IncidentEdit.toPayload() = EditPayload(title, description, category?.name, severity?.name, location?.toDto())
internal fun EditPayload.toEdit() = IncidentEdit(
    title, description, category?.let(Category::valueOf), severity?.let(Severity::valueOf), location?.toDomain(),
)

internal val OpJson = Json { ignoreUnknownKeys = true }
internal fun PendingOp.encode(): String = OpJson.encodeToString(PendingOp.serializer(), this)
internal fun decodePendingOp(json: String): PendingOp = OpJson.decodeFromString(PendingOp.serializer(), json)
