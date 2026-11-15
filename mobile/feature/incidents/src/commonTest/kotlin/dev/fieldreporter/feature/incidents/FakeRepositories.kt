package dev.fieldreporter.feature.incidents

import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.core.domain.NewIncident
import dev.fieldreporter.core.domain.NotEditableException
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.User
import dev.fieldreporter.core.domain.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

val technician = User("tech-1", "Aung Kyaw", Role.TECHNICIAN)

fun anIncident(id: String = "i1", status: Status = Status.OPEN) = Incident(
    id = id, title = "Leak", description = "Oil", category = Category.EQUIPMENT, severity = Severity.HIGH,
    status = status, location = Location(address = "Bay 3"), photos = emptyList(), reportedBy = technician.id,
    assignee = null, resolutionNote = null, resolvedAt = null, activityLog = emptyList(),
    createdAt = Instant.fromEpochSeconds(0), updatedAt = Instant.fromEpochSeconds(0),
)

class FakeIncidentRepository(initial: List<Incident> = emptyList()) : IncidentRepository {
    val rows = MutableStateFlow(initial)
    val reported = mutableListOf<NewIncident>()
    val edits = mutableListOf<Pair<String, IncidentEdit>>()
    val deleted = mutableListOf<String>()
    var syncOutcome = SyncOutcome.COMPLETED
    var deleteFails = false

    override val incidents: Flow<List<Incident>> = rows
    override fun incident(id: String): Flow<Incident?> = rows.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun report(new: NewIncident): Incident {
        reported += new
        return anIncident(id = "new")
    }

    override suspend fun edit(id: String, edit: IncidentEdit) {
        if (rows.value.firstOrNull { it.id == id }?.isEditable == false) throw NotEditableException()
        edits += id to edit
    }

    override suspend fun delete(id: String) {
        if (deleteFails) throw NotEditableException()
        deleted += id
    }

    override suspend fun addPhoto(incidentId: String, localPath: String) = Unit
    override suspend fun removePhoto(incidentId: String, photoId: String) = Unit
    override suspend fun dismissSyncFailure(id: String) = Unit
    override suspend fun sync(): SyncOutcome = syncOutcome
}

class FakeUserRepository : UserRepository {
    val user = MutableStateFlow<User?>(technician)
    override val currentUser: Flow<User?> = user
    override suspend fun availableUsers() = listOf(technician)
    override suspend fun select(user: User) { this.user.value = user }
    override suspend fun signOut() { user.value = null }
}
