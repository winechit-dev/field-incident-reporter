package dev.fieldreporter.feature.incidents

import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.feature.incidents.list.IncidentListViewModel
import dev.fieldreporter.feature.incidents.list.ListEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentListViewModelTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun setUp(repo: FakeIncidentRepository, scope: kotlinx.coroutines.test.TestScope): IncidentListViewModel {
        Dispatchers.setMain(StandardTestDispatcher(scope.testScheduler))
        return IncidentListViewModel(repo, FakeUserRepository(), undoWindowMs = 5_000)
    }

    @Test
    fun deleteIsHiddenAtOnceButOnlySentAfterTheUndoWindow() = runTest {
        val repo = FakeIncidentRepository(listOf(anIncident("a"), anIncident("b")))
        val vm = setUp(repo, this)
        val seen = mutableListOf<List<String>>()
        vm.state.onEach { seen += it.incidents.map { i -> i.id } }.launchIn(backgroundScope)
        advanceUntilIdle()

        vm.requestDelete(anIncident("a"))
        advanceTimeBy(4_999)
        assertEquals(listOf("b"), seen.last(), "hidden straight away")
        assertTrue(repo.deleted.isEmpty(), "not sent yet")

        advanceTimeBy(2)
        advanceUntilIdle()
        assertEquals(listOf("a"), repo.deleted)
    }

    @Test
    fun undoWithinTheWindowBringsItBackAndNeverSendsTheDelete() = runTest {
        val repo = FakeIncidentRepository(listOf(anIncident("a")))
        val vm = setUp(repo, this)
        val seen = mutableListOf<List<String>>()
        vm.state.onEach { seen += it.incidents.map { i -> i.id } }.launchIn(backgroundScope)
        advanceUntilIdle()

        vm.requestDelete(anIncident("a"))
        advanceTimeBy(2_000)
        vm.undoDelete()
        advanceTimeBy(10_000)
        advanceUntilIdle()

        assertEquals(listOf("a"), seen.last())
        assertTrue(repo.deleted.isEmpty())
    }

    @Test
    fun offersUndoWhenDeleting() = runTest {
        val repo = FakeIncidentRepository(listOf(anIncident("a")))
        val vm = setUp(repo, this)
        vm.requestDelete(anIncident("a"))
        val event = vm.events.first()
        assertIs<ListEvent.DeleteUndoOffered>(event)
        assertEquals("Leak", event.title)
    }

    @Test
    fun deleteRefusedByTheRepositoryTellsTheUserAndShowsTheIncidentAgain() = runTest {
        val repo = FakeIncidentRepository(listOf(anIncident("a"))).apply { deleteFails = true }
        val vm = setUp(repo, this)
        val seen = mutableListOf<List<String>>()
        vm.state.onEach { seen += it.incidents.map { i -> i.id } }.launchIn(backgroundScope)
        val events = mutableListOf<ListEvent>()
        vm.events.onEach { events += it }.launchIn(backgroundScope)
        advanceUntilIdle()

        vm.requestDelete(anIncident("a"))
        advanceTimeBy(6_000)
        advanceUntilIdle()

        assertEquals(listOf("a"), seen.last())
        assertTrue(events.any { it is ListEvent.Message })
    }

    private val mixed = listOf(
        anIncident("a", Status.OPEN), anIncident("b", Status.OPEN),
        anIncident("c", Status.IN_PROGRESS), anIncident("d", Status.RESOLVED),
    )

    @Test
    fun aStatusFilterShowsOnlyThatStatusAndClearingItShowsEverything() = runTest {
        val vm = setUp(FakeIncidentRepository(mixed), this)
        vm.state.launchIn(backgroundScope)
        advanceUntilIdle()
        assertEquals(null, vm.state.value.filter)
        assertEquals(4, vm.state.value.incidents.size)

        vm.onFilter(Status.OPEN)
        advanceUntilIdle()
        assertEquals(listOf("a", "b"), vm.state.value.incidents.map { it.id })

        vm.onFilter(null)
        advanceUntilIdle()
        assertEquals(4, vm.state.value.incidents.size)
    }

    @Test
    fun countsCoverEveryStatusAndDoNotChangeWithTheFilter() = runTest {
        val vm = setUp(FakeIncidentRepository(mixed), this)
        vm.state.launchIn(backgroundScope)
        advanceUntilIdle()
        val expected = mapOf(Status.OPEN to 2, Status.IN_PROGRESS to 1, Status.RESOLVED to 1)
        assertEquals(expected, vm.state.value.counts)
        assertEquals(4, vm.state.value.total)

        vm.onFilter(Status.RESOLVED)
        advanceUntilIdle()
        assertEquals(expected, vm.state.value.counts, "the chips keep showing how many there are of each")
        assertEquals(4, vm.state.value.total)
    }

    @Test
    fun anIncidentWaitingOutItsUndoWindowIsNotCounted() = runTest {
        val vm = setUp(FakeIncidentRepository(mixed), this)
        vm.state.launchIn(backgroundScope)
        advanceUntilIdle()

        vm.requestDelete(mixed[0])
        advanceTimeBy(1_000)
        assertEquals(1, vm.state.value.counts[Status.OPEN])
        assertEquals(3, vm.state.value.total)
    }
}
