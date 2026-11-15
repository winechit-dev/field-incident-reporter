package dev.fieldreporter.feature.incidents

import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.User
import dev.fieldreporter.feature.incidents.detail.DetailState
import dev.fieldreporter.feature.incidents.detail.IncidentDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentDetailViewModelTest {
    private val mya = User("u-op-1", "Mya Sandar", Role.OPERATOR)
    private val htet = User("u-op-2", "Htet Naing", Role.OPERATOR)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private class Setup(val vm: IncidentDetailViewModel, val repo: FakeIncidentRepository, val users: FakeUserRepository, val seen: MutableList<DetailState>)

    private fun TestScope.setUp(assignee: String?, known: List<User> = emptyList()): Setup {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeIncidentRepository(listOf(anIncident("i1", Status.IN_PROGRESS).copy(assignee = assignee)))
        val users = FakeUserRepository().apply { this.known.value = known }
        val vm = IncidentDetailViewModel(repo, users, "i1")
        val seen = mutableListOf<DetailState>()
        vm.state.onEach { seen += it }.launchIn(backgroundScope)
        testScheduler.runCurrent() // let the collector subscribe
        return Setup(vm, repo, users, seen)
    }

    /** advanceUntilIdle skips work resumed for a backgroundScope collector, so step the clock instead. */
    private fun TestScope.settle() {
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
    }

    private fun Setup.loaded() = assertIs<DetailState.Loaded>(seen.last())

    @Test
    fun theStateNamesTheAssignedOperator() = runTest {
        val s = setUp("u-op-1", listOf(mya))
        settle()

        assertEquals("Mya Sandar", s.loaded().assigneeName)
    }

    @Test
    fun noStoredListMeansNoNameNeverTheRawId() = runTest {
        val s = setUp("u-op-1")
        settle()

        assertNull(s.loaded().assigneeName)
        assertEquals("u-op-1", s.loaded().incident.assignee)
    }

    @Test
    fun anIdMissingFromTheStoredListMeansNoName() = runTest {
        val s = setUp("u-op-9", listOf(mya))
        settle()

        assertNull(s.loaded().assigneeName)
    }

    @Test
    fun anIdThatBelongsToANonOperatorMeansNoName() = runTest {
        val s = setUp("tech-1", listOf(technician, mya))
        settle()

        assertNull(s.loaded().assigneeName)
    }

    @Test
    fun aReassignmentIsNamedWithoutReopeningTheScreen() = runTest {
        val s = setUp("u-op-1", listOf(mya, htet))
        settle()
        assertEquals("Mya Sandar", s.loaded().assigneeName)

        s.repo.rows.value = s.repo.rows.value.map { it.copy(assignee = "u-op-2") }
        settle()

        assertEquals("Htet Naing", s.loaded().assigneeName)
    }

    @Test
    fun aListThatArrivesAfterTheScreenOpenedFillsTheNameIn() = runTest {
        val s = setUp("u-op-1")
        settle()
        assertNull(s.loaded().assigneeName)

        s.users.known.value = listOf(mya)
        settle()

        assertEquals("Mya Sandar", s.loaded().assigneeName)
    }

    @Test
    fun anUnassignedIncidentHasNoAssigneeAndNoName() = runTest {
        val s = setUp(null, listOf(mya))
        settle()

        assertNull(s.loaded().incident.assignee)
        assertNull(s.loaded().assigneeName)
    }

    @Test
    fun unassigningOnTheServerRemovesTheAssigneeAfterTheNextSync() = runTest {
        val s = setUp("u-op-1", listOf(mya))
        settle()

        s.repo.rows.value = s.repo.rows.value.map { it.copy(assignee = null) }
        settle()

        assertNull(s.loaded().incident.assignee)
        assertNull(s.loaded().assigneeName)
    }
}

class HandlingTextTest {
    @Test
    fun namesTheOperatorWhenKnown() =
        assertEquals("Mya Sandar is handling this incident.", dev.fieldreporter.feature.incidents.detail.handlingText("Mya Sandar"))

    @Test
    fun keepsTodaysWordingWhenTheNameIsNotKnown() =
        assertEquals("An operator is handling this incident.", dev.fieldreporter.feature.incidents.detail.handlingText(null))

    @Test
    fun aBlankNameKeepsTodaysWording() {
        for (blank in listOf("", "   ", "\t\n")) {
            assertEquals(
                "An operator is handling this incident.",
                dev.fieldreporter.feature.incidents.detail.handlingText(blank),
                "for name \"$blank\"",
            )
        }
    }
}
