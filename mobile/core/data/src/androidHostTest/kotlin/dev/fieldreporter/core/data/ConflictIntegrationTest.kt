package dev.fieldreporter.core.data

import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.core.domain.NewIncident
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncFailureReason
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A Technician's offline edit or delete meets an Operator who took the incident in the meantime.
 * The Operator's "Start work" is the patch the dashboard sends (a fixture shared with the
 * dashboard's own integration test). See [IntegrationHarness] for how the real parts are wired.
 */
class ConflictIntegrationTest {
    private val apiUrl: String? = System.getenv("FIR_API_URL")
    private val startWork: String? = System.getenv("FIR_START_WORK_PATCH")

    private lateinit var h: IntegrationHarness

    @Before
    fun setUp() {
        assumeNotNull(apiUrl, startWork) // skipped, not passed, when there is no API to talk to
        h = IntegrationHarness(apiUrl!!)
    }

    private suspend fun reportAndSync(title: String): String {
        val incident = h.repo.report(
            NewIncident(title, "Found during a round", Category.EQUIPMENT, Severity.HIGH, Location(address = "Dock 2")),
        )
        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())
        return incident.id
    }

    @Test
    fun control_aReportSyncsToTheRealApi() = runBlocking {
        val id = reportAndSync("Control report")

        val onServer = h.remote.get(id)
        assertEquals("Control report", onServer.title)
        assertEquals(Status.OPEN, onServer.status)
        assertEquals(h.technician.id, onServer.reportedBy)
        assertEquals(SyncState.SYNCED, h.store.incident(id)?.syncState)
    }

    @Test
    fun conflictOnEdit_serverVersionWinsAndTheLocalEditIsDiscarded() = runBlocking {
        val id = reportAndSync("Hydraulic lift leaking oil")

        // Offline: the Technician edits the title. It is saved locally and queued, not sent.
        h.repo.edit(id, IncidentEdit(title = "Hydraulic lift leaking oil - field note"))
        assertEquals(SyncState.PENDING, h.store.incident(id)?.syncState)

        // Meanwhile the Operator takes the incident.
        h.operatorPatches(id, startWork!!)

        // The phone reconnects and syncs.
        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())

        val local = assertNotNull(h.store.incident(id))
        assertEquals("Hydraulic lift leaking oil", local.title, "the local edit is discarded")
        assertEquals(Status.IN_PROGRESS, local.status, "the server's Status is shown")
        assertEquals(SyncState.FAILED, local.syncState)
        assertEquals(SyncFailureReason.CONFLICT, local.syncFailure?.reason)
        assertEquals("Incident is already being handled", local.syncFailure?.message)
        assertTrue(h.store.pendingOps().isEmpty(), "nothing is left to retry")

        val onServer = h.remote.get(id)
        assertEquals("Hydraulic lift leaking oil", onServer.title, "the refused edit never reached the server")
        assertEquals(listOf(Status.IN_PROGRESS), onServer.activityLog.map { it.status })
    }

    @Test
    fun conflictOnDelete_theIncidentComesBackAndStillExistsOnTheServer() = runBlocking {
        val id = reportAndSync("Cracked floor near loading dock")

        // Offline: the Technician deletes it. It disappears from the list and the delete is queued.
        h.repo.delete(id)
        assertTrue(h.repo.incidents.first().none { it.id == id }, "hidden while the delete waits")

        // Meanwhile the Operator takes the incident.
        h.operatorPatches(id, startWork!!)

        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())

        val visible = assertNotNull(h.repo.incidents.first().firstOrNull { it.id == id }, "it comes back into the list")
        assertEquals(Status.IN_PROGRESS, visible.status)
        assertEquals(SyncState.FAILED, visible.syncState)
        assertEquals(SyncFailureReason.CONFLICT, visible.syncFailure?.reason)
        assertTrue(h.store.pendingOps().isEmpty(), "nothing is left to retry")

        assertEquals("Cracked floor near loading dock", h.remote.get(id).title, "the refused delete left it on the server")
    }
}
