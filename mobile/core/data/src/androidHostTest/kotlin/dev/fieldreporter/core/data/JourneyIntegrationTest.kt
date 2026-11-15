package dev.fieldreporter.core.data

import dev.fieldreporter.core.data.local.PendingOp
import dev.fieldreporter.core.data.remote.RemoteException
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.core.domain.NewIncident
import dev.fieldreporter.core.domain.NotEditableException
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.SyncState
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The journeys that cross from the phone to the dashboard and back, with the phone's real sync
 * code against a real API. Each test also hands what it made to the dashboard phase
 * (`FIR_STATE_DIR`), which checks the dashboard sees the same through its own code.
 */
class JourneyIntegrationTest {
    private val apiUrl: String? = System.getenv("FIR_API_URL")
    private val resolve: String? = System.getenv("FIR_RESOLVE_PATCH")

    private lateinit var h: IntegrationHarness

    @Before
    fun setUp() {
        assumeNotNull(apiUrl, resolve)
        h = IntegrationHarness(apiUrl!!)
    }

    @Test
    fun reportWithPhotoAndLocation_reachesTheServerIntact() = runBlocking {
        val title = "Cracked floor near loading dock ${System.nanoTime()}"
        val photo = IntegrationHarness.tempPhoto()

        val incident = h.repo.report(
            NewIncident(
                title, "A long crack across the concrete at the dock edge", Category.SAFETY, Severity.HIGH,
                Location(latitude = 16.8409, longitude = 96.1735, address = "Dock 2"), photoPaths = listOf(photo),
            ),
        )
        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())

        val onServer = h.remote.get(incident.id)
        assertEquals(title, onServer.title)
        assertEquals(Category.SAFETY, onServer.category)
        assertEquals(Severity.HIGH, onServer.severity)
        assertEquals(Status.OPEN, onServer.status)
        assertEquals(Location(16.8409, 96.1735, "Dock 2"), onServer.location)
        assertEquals(h.technician.id, onServer.reportedBy)

        val serverPhoto = onServer.photos.single()
        val url = assertNotNull(serverPhoto.url)
        assertContentEquals(IntegrationHarness.PHOTO_BYTES, h.download(url), "the photo's bytes survive the trip")

        val local = assertNotNull(h.store.incident(incident.id))
        assertEquals(SyncState.SYNCED, local.syncState)
        assertNull(local.photos.single().localPath, "the local file is replaced by the server's photo")

        h.writeHandoff(
            "scenario1",
            mapOf(
                "id" to incident.id, "title" to title, "category" to "SAFETY", "severity" to "HIGH",
                "address" to "Dock 2", "latitude" to 16.8409, "longitude" to 96.1735,
                "photoSha256" to IntegrationHarness.sha256(IntegrationHarness.PHOTO_BYTES),
            ),
        )
    }

    @Test
    fun anOperatorsResolution_reachesThePhoneOnTheNextSync() = runBlocking {
        val title = "Pothole on service road ${System.nanoTime()}"
        val incident = h.repo.report(NewIncident(title, "", Category.ROAD_HAZARD, Severity.MEDIUM, Location()))
        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())
        assertEquals(Status.OPEN, h.store.incident(incident.id)?.status)

        // The Operator resolves it on the dashboard.
        h.operatorPatches(incident.id, resolve!!)
        assertEquals(Status.OPEN, h.store.incident(incident.id)?.status, "the phone does not know yet")

        // The Technician refreshes (or the periodic sync runs).
        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())

        val local = assertNotNull(h.store.incident(incident.id))
        assertEquals(Status.RESOLVED, local.status)
        assertTrue(local.resolutionNote?.startsWith("Replaced the hydraulic seal") == true, "the note arrives: ${local.resolutionNote}")
        assertNotNull(local.resolvedAt)
        assertEquals(SyncState.SYNCED, local.syncState)
        assertTrue(!local.isEditable, "the phone now treats it as read-only")
        assertFailsWith<NotEditableException> { h.repo.edit(incident.id, IncidentEdit(title = "Too late")) }

        h.writeHandoff("scenario2", mapOf("id" to incident.id, "title" to title, "note" to local.resolutionNote))
    }

    @Test
    fun anOfflineReport_waitsLocallyThenSyncsWithItsPhoto() = runBlocking {
        val title = "Offline report from the field ${System.nanoTime()}"

        // Offline: report with a photo. Nothing is sent.
        val incident = h.repo.report(
            NewIncident(title, "", Category.EQUIPMENT, Severity.MEDIUM, Location(), photoPaths = listOf(IntegrationHarness.tempPhoto())),
        )

        val local = assertNotNull(h.store.incident(incident.id))
        assertEquals(SyncState.PENDING, local.syncState)
        assertEquals(
            listOf("Create", "AddPhoto"),
            h.store.pendingOps().map { it.op::class.simpleName },
            "the report is queued first, then its photo",
        )
        assertTrue(h.store.pendingOps().first().op is PendingOp.Create)
        assertFailsWith<RemoteException.NotFound> { h.remote.get(incident.id) }
        assertEquals(0, h.operatorListCount(title), "the Operator cannot see it yet")

        // The network returns and the phone syncs.
        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())

        assertTrue(h.store.pendingOps().isEmpty())
        assertEquals(SyncState.SYNCED, h.store.incident(incident.id)?.syncState)
        val onServer = h.remote.get(incident.id)
        assertEquals(title, onServer.title)
        assertContentEquals(IntegrationHarness.PHOTO_BYTES, h.download(assertNotNull(onServer.photos.single().url)))
        assertEquals(1, h.operatorListCount(title), "now the Operator sees it")

        h.writeHandoff(
            "scenario5",
            mapOf("id" to incident.id, "title" to title, "photoSha256" to IntegrationHarness.sha256(IntegrationHarness.PHOTO_BYTES)),
        )
    }
}
