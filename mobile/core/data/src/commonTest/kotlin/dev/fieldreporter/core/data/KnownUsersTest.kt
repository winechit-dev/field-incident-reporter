package dev.fieldreporter.core.data

import dev.fieldreporter.core.data.local.PendingOp
import dev.fieldreporter.core.data.remote.RemoteException
import dev.fieldreporter.core.data.repository.UserRepositoryImpl
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.core.domain.NewIncident
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.ServerUnavailableException
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.core.domain.SyncState
import dev.fieldreporter.core.domain.User
import io.ktor.serialization.JsonConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class KnownUsersTest {
    private val mya = User("u-op-1", "Mya Sandar", Role.OPERATOR)
    private val htet = User("u-op-2", "Htet Naing", Role.OPERATOR)

    private fun Harness.users(store: dev.fieldreporter.core.data.local.LocalStore = this.store) =
        UserRepositoryImpl(store, remote, SyncScheduler { })

    private suspend fun Harness.reportOne() =
        repo.report(NewIncident("Leak", "Oil", Category.EQUIPMENT, Severity.HIGH, Location(address = "Bay 3")))

    @Test
    fun syncStoresTheUserListAndTheLookupFindsTheOperator() = runTest {
        val h = Harness().apply { signIn(); remote.userList = listOf(technician, mya) }

        h.repo.sync()

        assertEquals(listOf(technician, mya), h.store.observeKnownUsers().first())
        assertEquals(mya, h.users().knownOperator("u-op-1").first())
    }

    @Test
    fun theStoredListIsStillReadOfflineByAFreshRepository() = runTest {
        val h = Harness().apply { signIn(); remote.userList = listOf(mya) }
        h.repo.sync()

        h.remote.online = false
        val afterRestart = h.users()

        assertEquals(mya, afterRestart.knownOperator("u-op-1").first())
    }

    @Test
    fun aFailedUserFetchKeepsTheListAndDoesNotChangeTheOutcomeOrTheOutbox() = runTest {
        for (failure in listOf(RemoteException.Network(), IllegalArgumentException("No enum constant Role.AUDITOR"))) {
            val h = Harness().apply { signIn(); remote.userList = listOf(mya) }
            val incident = h.reportOne()
            h.repo.sync()
            // A real queued change: an edit made after the first sync, waiting in the outbox.
            h.repo.edit(incident.id, IncidentEdit(title = "Hydraulic leak"))
            assertEquals(incident.id, (h.store.pendingOps().single().op as PendingOp.Update).incidentId)
            h.remote.calls.clear()

            h.remote.usersFailure = failure

            assertEquals(SyncOutcome.COMPLETED, h.repo.sync(), "$failure")
            assertEquals(listOf("update ${incident.id}", "list", "users"), h.remote.calls, "refresh runs after push and pull")
            assertEquals(emptyList(), h.store.pendingOps(), "the queued edit was pushed and removed, nothing re-queued")
            val row = h.store.incident(incident.id)
            assertEquals("Hydraulic leak", row?.title)
            assertEquals(SyncState.SYNCED, row?.syncState)
            assertEquals("Hydraulic leak", h.remote.server.getValue(incident.id).title)
            assertEquals(listOf(mya), h.store.observeKnownUsers().first())
        }
    }

    @Test
    fun aUsersBodyThatDoesNotDecodeStillCompletesTheSyncAndKeepsTheList() = runTest {
        val h = Harness().apply { signIn(); remote.userList = listOf(mya) }
        h.reportOne()
        h.repo.sync()

        // What Ktor throws when `GET /users` answers 200 with a body that does not decode.
        h.remote.usersFailure = JsonConvertException("Illegal input: Field 'name' is required")

        assertEquals(SyncOutcome.COMPLETED, h.repo.sync())
        assertEquals(listOf(mya), h.store.observeKnownUsers().first())
        assertEquals(listOf(SyncState.SYNCED), h.repo.incidents.first().map { it.syncState })
    }

    @Test
    fun cancellingTheUsersFetchStillCancelsTheSync() = runTest {
        val h = Harness().apply { signIn(); remote.userList = listOf(mya) }
        h.repo.sync()

        h.remote.usersFailure = CancellationException("scope cancelled")

        assertFailsWith<CancellationException> { h.repo.sync() }
        assertEquals(listOf(mya), h.store.observeKnownUsers().first())
    }

    @Test
    fun anOfflineSyncKeepsTheListAndStillAsksForRetry() = runTest {
        val h = Harness().apply { signIn(); remote.userList = listOf(mya) }
        h.repo.sync()
        h.reportOne()

        h.remote.online = false

        assertEquals(SyncOutcome.RETRY, h.repo.sync())
        assertEquals(listOf(mya), h.store.observeKnownUsers().first())
        assertEquals(listOf(SyncState.PENDING), h.repo.incidents.first().map { it.syncState })
    }

    @Test
    fun aLaterSyncReplacesTheWholeList() = runTest {
        val h = Harness().apply { signIn(); remote.userList = listOf(mya) }
        h.repo.sync()
        val users = h.users()

        h.remote.userList = listOf(mya, htet)
        h.repo.sync()
        assertEquals(htet, users.knownOperator("u-op-2").first())

        h.remote.userList = listOf(htet)
        h.repo.sync()
        assertNull(users.knownOperator("u-op-1").first(), "a user the server dropped is no longer known")
    }

    @Test
    fun onlyAnOperatorIsReturnedAsTheAssignee() = runTest {
        val h = Harness().apply { signIn(); remote.userList = listOf(technician, mya) }
        h.repo.sync()

        assertNull(h.users().knownOperator(technician.id).first())
        assertNull(h.users().knownOperator("nobody").first())
    }

    @Test
    fun fetchingTheListForThePickerAlsoStoresIt() = runTest {
        val h = Harness().apply { remote.userList = listOf(technician, mya) }

        h.users().availableUsers()

        assertEquals(mya, h.users().knownOperator("u-op-1").first())
    }

    @Test
    fun thePickerStillFailsOfflineAndDoesNotUseTheStoredList() = runTest {
        val h = Harness().apply { remote.userList = listOf(technician, mya) }
        h.users().availableUsers()

        h.remote.online = false

        assertFailsWith<ServerUnavailableException> { h.users().availableUsers() }
    }

    @Test
    fun aReassignedOrClearedAssigneeLandsInTheLocalRowAfterSync() = runTest {
        val h = Harness().apply { signIn() }
        val incident = h.reportOne()
        h.repo.sync()
        assertNull(h.store.incident(incident.id)?.assignee)

        h.remote.server[incident.id] = h.remote.server.getValue(incident.id).copy(assignee = "u-op-1")
        h.repo.sync()
        assertEquals("u-op-1", h.store.incident(incident.id)?.assignee)

        h.remote.server[incident.id] = h.remote.server.getValue(incident.id).copy(assignee = "u-op-2")
        h.repo.sync()
        assertEquals("u-op-2", h.store.incident(incident.id)?.assignee)

        h.remote.server[incident.id] = h.remote.server.getValue(incident.id).copy(assignee = null)
        h.repo.sync()
        assertNull(h.store.incident(incident.id)?.assignee)
    }
}
