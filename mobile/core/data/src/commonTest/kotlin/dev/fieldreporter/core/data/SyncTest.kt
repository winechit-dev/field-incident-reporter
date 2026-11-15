package dev.fieldreporter.core.data

import dev.fieldreporter.core.data.local.PendingOp
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.core.domain.NewIncident
import dev.fieldreporter.core.domain.NotEditableException
import dev.fieldreporter.core.domain.PhotoLimitException
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncFailureReason
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncTest {
    private val report = NewIncident("Leak", "Oil on floor", Category.EQUIPMENT, Severity.HIGH, Location(address = "Bay 3"))

    private suspend fun Harness.reportOne(photos: List<String> = emptyList()) =
        repo.report(report.copy(photoPaths = photos))

    @Test
    fun reportingOfflineSavesLocallyAsPendingAndAsksForSync() = runTest {
        val h = Harness().apply { signIn(); remote.online = false }
        val incident = h.reportOne()

        assertEquals(SyncState.PENDING, h.store.incident(incident.id)?.syncState)
        assertEquals(listOf(PendingOp.Create(incident.id)), h.store.pendingOps().map { it.op })
        assertEquals(1, h.syncRequests)
        assertEquals(listOf(incident.id), h.repo.incidents.first().map { it.id })
    }

    @Test
    fun networkFailureKeepsQueueIntactAndRequestsRetry() = runTest {
        val h = Harness().apply { signIn(); remote.online = false }
        val incident = h.reportOne()

        assertEquals(SyncOutcome.RETRY, h.repo.sync())
        assertEquals(SyncState.PENDING, h.store.incident(incident.id)?.syncState)
        assertEquals(1, h.store.pendingOps().size)
    }

    @Test
    fun comingBackOnlineSyncsEverythingInOrder() = runTest {
        val h = Harness().apply { signIn(); remote.online = false }
        val incident = h.reportOne(photos = listOf("a.jpg", "b.jpg"))
        h.repo.sync()
        h.remote.online = true

        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())

        val row = h.store.incident(incident.id)!!
        assertEquals(SyncState.SYNCED, row.syncState)
        assertTrue(h.store.pendingOps().isEmpty())
        assertEquals(2, row.photos.size)
        assertTrue(row.photos.all { it.url != null && it.localPath == null && it.syncState == SyncState.SYNCED })
        // Create must come before the photo uploads.
        val order = h.remote.calls.filter { it.startsWith("create") || it.startsWith("upload") }
        assertTrue(order.first().startsWith("create"))
        assertEquals(listOf("a.jpg", "b.jpg"), h.deletedFiles)
    }

    @Test
    fun retriedCreateAfterALostResponseDoesNotDuplicate() = runTest {
        val h = Harness().apply { signIn() }
        val incident = h.reportOne()
        // The server already has it (the earlier response was lost), the client retries.
        h.remote.create(h.store.incident(incident.id)!!)

        h.repo.sync()

        assertEquals(1, h.remote.server.size)
        assertEquals(SyncState.SYNCED, h.store.incident(incident.id)?.syncState)
    }

    @Test
    fun editingBeforeFirstSyncRidesAlongWithTheCreate() = runTest {
        val h = Harness().apply { signIn(); remote.online = false }
        val incident = h.reportOne()

        h.repo.edit(incident.id, IncidentEdit(title = "Hydraulic leak"))

        assertEquals(listOf(PendingOp.Create(incident.id)), h.store.pendingOps().map { it.op })
        h.remote.online = true
        h.repo.sync()
        assertEquals("Hydraulic leak", h.remote.server.getValue(incident.id).title)
        assertEquals(0, h.remote.calls.count { it.startsWith("update") })
    }

    @Test
    fun editsAfterSyncAreQueuedAndMergedIntoOneUpdate() = runTest {
        val h = Harness().apply { signIn() }
        val incident = h.reportOne()
        h.repo.sync()
        h.remote.online = false

        h.repo.edit(incident.id, IncidentEdit(title = "A"))
        h.repo.edit(incident.id, IncidentEdit(description = "B"))
        h.repo.edit(incident.id, IncidentEdit(title = "C"))

        assertEquals(1, h.store.pendingOps().size)
        h.remote.online = true
        h.repo.sync()
        val server = h.remote.server.getValue(incident.id)
        assertEquals("C", server.title)
        assertEquals("B", server.description)
        assertEquals(1, h.remote.calls.count { it.startsWith("update") })
    }

    @Test
    fun cannotEditOrDeleteOnceNoLongerOpen() = runTest {
        val h = Harness().apply { signIn() }
        val incident = h.reportOne()
        h.repo.sync()
        h.remote.operatorSetsStatus(incident.id, Status.IN_PROGRESS)
        h.repo.sync() // pull picks up the new status

        assertFailsWith<NotEditableException> { h.repo.edit(incident.id, IncidentEdit(title = "x")) }
        assertFailsWith<NotEditableException> { h.repo.delete(incident.id) }
        assertFailsWith<NotEditableException> { h.repo.addPhoto(incident.id, "p.jpg") }
    }

    @Test
    fun conflictOnEditMakesServerVersionWinAndMarksFailed() = runTest {
        val h = Harness().apply { signIn() }
        val incident = h.reportOne()
        h.repo.sync()
        h.remote.online = false
        h.repo.edit(incident.id, IncidentEdit(title = "My late edit"))
        // Meanwhile an Operator picks the Incident up.
        h.remote.operatorSetsStatus(incident.id, Status.IN_PROGRESS)
        h.remote.online = true

        h.repo.sync()

        val row = h.store.incident(incident.id)!!
        assertEquals("Leak", row.title, "local edit is discarded")
        assertEquals(Status.IN_PROGRESS, row.status)
        assertEquals(SyncState.FAILED, row.syncState)
        assertEquals(SyncFailureReasonOf(row), SyncFailureReason.CONFLICT)
        assertTrue(h.store.pendingOps().isEmpty())
        assertEquals("Leak", h.remote.server.getValue(incident.id).title)
    }

    @Test
    fun conflictOnDeleteBringsTheIncidentBack() = runTest {
        val h = Harness().apply { signIn() }
        val incident = h.reportOne()
        h.repo.sync()
        h.remote.online = false
        h.repo.delete(incident.id)
        assertNull(h.repo.incident(incident.id).first(), "hidden while the delete waits")
        h.remote.operatorSetsStatus(incident.id, Status.RESOLVED)
        h.remote.online = true

        h.repo.sync()

        val visible = assertNotNull(h.repo.incident(incident.id).first())
        assertEquals(Status.RESOLVED, visible.status)
        assertEquals(SyncState.FAILED, visible.syncState)
        assertNotNull(h.remote.server[incident.id])
    }

    @Test
    fun deletingSomethingNeverSyncedNeverCallsTheServer() = runTest {
        val h = Harness().apply { signIn(); remote.online = false }
        val incident = h.reportOne(photos = listOf("a.jpg"))

        h.repo.delete(incident.id)

        assertNull(h.store.incident(incident.id))
        assertTrue(h.store.pendingOps().isEmpty())
        assertEquals(listOf("a.jpg"), h.deletedFiles)
        assertTrue(h.remote.calls.isEmpty())
    }

    @Test
    fun deletingASyncedIncidentIsQueuedThenRemovedEverywhere() = runTest {
        val h = Harness().apply { signIn() }
        val incident = h.reportOne()
        h.repo.sync()
        h.remote.online = false

        h.repo.delete(incident.id)
        assertTrue(h.repo.incidents.first().isEmpty())
        h.remote.online = true
        h.repo.sync()

        assertNull(h.store.incident(incident.id))
        assertTrue(h.remote.server.isEmpty())
    }

    @Test
    fun photoUploadFailureKeepsOrderAndResumesLater() = runTest {
        val h = Harness().apply { signIn() }
        val incident = h.reportOne(photos = listOf("a.jpg", "b.jpg"))
        h.remote.online = true
        // Network drops after the Incident is created.
        h.remote.create(h.store.incident(incident.id)!!)
        h.remote.online = false
        assertEquals(SyncOutcome.RETRY, h.repo.sync())
        assertEquals(3, h.store.pendingOps().size)
        h.remote.online = true

        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())

        assertEquals(2, h.remote.server.getValue(incident.id).photos.size)
        assertEquals(SyncState.SYNCED, h.store.incident(incident.id)?.syncState)
    }

    @Test
    fun photoCapIsEnforcedLocally() = runTest {
        val h = Harness().apply { signIn(); remote.online = false }
        val incident = h.reportOne(photos = List(5) { "p$it.jpg" })

        assertFailsWith<PhotoLimitException> { h.repo.addPhoto(incident.id, "extra.jpg") }
        assertFailsWith<PhotoLimitException> { h.reportOne(photos = List(6) { "q$it.jpg" }) }
    }

    @Test
    fun removingAnUnuploadedPhotoCancelsItsUpload() = runTest {
        val h = Harness().apply { signIn(); remote.online = false }
        val incident = h.reportOne(photos = listOf("a.jpg"))
        val photoId = h.store.incident(incident.id)!!.photos.single().id

        h.repo.removePhoto(incident.id, photoId)

        assertEquals(listOf(PendingOp.Create(incident.id)), h.store.pendingOps().map { it.op })
        assertEquals(listOf("a.jpg"), h.deletedFiles)
    }

    @Test
    fun pullDoesNotOverwritePendingEditsAndDropsStaleSyncedRows() = runTest {
        val h = Harness().apply { signIn() }
        val kept = h.reportOne()
        val stale = h.reportOne()
        h.repo.sync()
        h.remote.online = false
        h.repo.edit(kept.id, IncidentEdit(title = "Local title"))
        h.remote.online = true
        h.remote.server.remove(stale.id) // gone on the server

        h.engine.sync()

        // The pending edit was pushed first, so it wins; the stale row is gone.
        assertEquals("Local title", h.store.incident(kept.id)?.title)
        assertNull(h.store.incident(stale.id))
    }

    @Test
    fun rejectedCreateIsMarkedFailedAndNotRetriedForever() = runTest {
        val h = Harness().apply { signIn() }
        h.remote.rejectCreateWith = "title must be 1-120 characters"
        val incident = h.reportOne()

        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())

        val row = h.store.incident(incident.id)!!
        assertEquals(SyncState.FAILED, row.syncState)
        assertEquals(SyncFailureReason.REJECTED, row.syncFailure?.reason)
        assertTrue(h.store.pendingOps().isEmpty())
        // Pull must not wipe the failure notice.
        h.repo.sync()
        assertEquals(SyncState.FAILED, h.store.incident(incident.id)?.syncState)
    }

    @Test
    fun dismissingAFailureAcceptsTheServerVersion() = runTest {
        val h = Harness().apply { signIn() }
        val incident = h.reportOne()
        h.repo.sync()
        h.remote.operatorSetsStatus(incident.id, Status.IN_PROGRESS)
        h.remote.online = false
        // Force a conflict: pretend the local copy still thinks it is Open.
        h.store.save(h.store.incident(incident.id)!!.copy(status = Status.OPEN))
        h.repo.edit(incident.id, IncidentEdit(title = "x"))
        h.remote.online = true
        h.repo.sync()
        assertEquals(SyncState.FAILED, h.store.incident(incident.id)?.syncState)

        h.repo.dismissSyncFailure(incident.id)

        assertEquals(SyncState.SYNCED, h.store.incident(incident.id)?.syncState)
        assertNull(h.store.incident(incident.id)?.syncFailure)
    }

    private fun SyncFailureReasonOf(row: dev.fieldreporter.core.domain.Incident) = row.syncFailure?.reason
}
