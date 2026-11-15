package dev.fieldreporter.core.data

import dev.fieldreporter.core.data.remote.KtorIncidentRemote
import dev.fieldreporter.core.data.repository.IncidentRepositoryImpl
import dev.fieldreporter.core.data.sync.SyncEngine
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.core.domain.NewIncident
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncFailureReason
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.core.domain.SyncState
import dev.fieldreporter.core.domain.User
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The app's real sync engine and Ktor client against a real API, with an Operator acting on the
 * same incident in between. Runs only when `FIR_API_URL` and `FIR_START_WORK_PATCH` are set:
 * `scripts/integration/run-conflicts.sh` does that and starts the API.
 *
 * "Offline" here means not calling `sync()`: the change is saved locally and queued, exactly as it
 * is when the phone has no network. The Operator's "Start work" is the patch the dashboard sends
 * (a fixture shared with the dashboard's own integration test).
 */
class ConflictIntegrationTest {
    private val apiUrl: String? = System.getenv("FIR_API_URL")
    private val startWorkPatch: String? = System.getenv("FIR_START_WORK_PATCH")

    private val technician = User("u-tech-1", "Aung Kyaw", Role.TECHNICIAN)

    private lateinit var store: FakeLocalStore
    private lateinit var remote: KtorIncidentRemote
    private lateinit var repo: IncidentRepositoryImpl
    private lateinit var operatorClient: HttpClient

    @Before
    fun setUp() {
        assumeNotNull(apiUrl, startWorkPatch) // skipped, not passed, when there is no API to talk to
        store = FakeLocalStore()
        remote = KtorIncidentRemote(createHttpClient(), apiUrl!!, userId = { technician.id })
        val engine = SyncEngine(store, remote) { }
        repo = IncidentRepositoryImpl(store, engine, scheduler = SyncScheduler { /* offline: nothing runs */ }, deleteFile = { })
        operatorClient = createHttpClient()
        runBlocking { store.setCurrentUser(technician) }
    }

    /** What the dashboard's "Start work" does, sent as the Operator. */
    private fun operatorStartsWork(incidentId: String) = runBlocking {
        val response = operatorClient.patch("$apiUrl/incidents/$incidentId") {
            header("X-User-Id", "u-op-1")
            contentType(ContentType.Application.Json)
            setBody(startWorkPatch!!)
        }
        assertTrue(response.status.isSuccess(), "the Operator's Start work should succeed: ${response.bodyAsText()}")
    }

    private suspend fun reportAndSync(title: String): String {
        val incident = repo.report(
            NewIncident(title, "Found during a round", Category.EQUIPMENT, Severity.HIGH, Location(address = "Dock 2")),
        )
        assertEquals(SyncOutcome.COMPLETED, repo.sync())
        return incident.id
    }

    @Test
    fun control_aReportSyncsToTheRealApi() = runBlocking {
        val id = reportAndSync("Control report")

        val onServer = remote.get(id)
        assertEquals("Control report", onServer.title)
        assertEquals(Status.OPEN, onServer.status)
        assertEquals(technician.id, onServer.reportedBy)
        assertEquals(SyncState.SYNCED, store.incident(id)?.syncState)
    }

    @Test
    fun conflictOnEdit_serverVersionWinsAndTheLocalEditIsDiscarded() = runBlocking {
        val id = reportAndSync("Hydraulic lift leaking oil")

        // Offline: the Technician edits the title. It is saved locally and queued, not sent.
        repo.edit(id, IncidentEdit(title = "Hydraulic lift leaking oil - field note"))
        assertEquals(SyncState.PENDING, store.incident(id)?.syncState)

        // Meanwhile the Operator takes the incident.
        operatorStartsWork(id)

        // The phone reconnects and syncs.
        assertEquals(SyncOutcome.COMPLETED, repo.sync())

        val local = assertNotNull(store.incident(id))
        assertEquals("Hydraulic lift leaking oil", local.title, "the local edit is discarded")
        assertEquals(Status.IN_PROGRESS, local.status, "the server's Status is shown")
        assertEquals(SyncState.FAILED, local.syncState)
        assertEquals(SyncFailureReason.CONFLICT, local.syncFailure?.reason)
        assertEquals("Incident is already being handled", local.syncFailure?.message)
        assertTrue(store.pendingOps().isEmpty(), "nothing is left to retry")

        val onServer = remote.get(id)
        assertEquals("Hydraulic lift leaking oil", onServer.title, "the refused edit never reached the server")
        assertEquals(listOf(Status.IN_PROGRESS), onServer.activityLog.map { it.status })
    }

    @Test
    fun conflictOnDelete_theIncidentComesBackAndStillExistsOnTheServer() = runBlocking {
        val id = reportAndSync("Cracked floor near loading dock")

        // Offline: the Technician deletes it. It disappears from the list and the delete is queued.
        repo.delete(id)
        assertTrue(repo.incidents.first().none { it.id == id }, "hidden while the delete waits")

        // Meanwhile the Operator takes the incident.
        operatorStartsWork(id)

        assertEquals(SyncOutcome.COMPLETED, repo.sync())

        val visible = assertNotNull(repo.incidents.first().firstOrNull { it.id == id }, "it comes back into the list")
        assertEquals(Status.IN_PROGRESS, visible.status)
        assertEquals(SyncState.FAILED, visible.syncState)
        assertEquals(SyncFailureReason.CONFLICT, visible.syncFailure?.reason)
        assertTrue(store.pendingOps().isEmpty(), "nothing is left to retry")

        assertEquals("Cracked floor near loading dock", remote.get(id).title, "the refused delete left it on the server")
    }
}
