package dev.fieldreporter.feature.incidents

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
}
