package dev.fieldreporter.platform

import dev.fieldreporter.core.domain.SyncOutcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RetryingSyncSchedulerTest {
    @Test
    fun retriesWithDoublingBackoffUntilASyncCompletes() = runTest {
        val outcomes = ArrayDeque(listOf(SyncOutcome.RETRY, SyncOutcome.RETRY, SyncOutcome.COMPLETED))
        val callTimes = mutableListOf<Long>()
        val scheduler = RetryingSyncScheduler(backgroundScope, sync = {
            callTimes += currentTime
            outcomes.removeFirst()
        })

        scheduler.requestSync()
        advanceTimeBy(1_000_000)

        assertEquals(listOf(0L, 10_000L, 30_000L), callTimes, "waits 10s, then 20s, then stops")
    }

    @Test
    fun backoffIsCapped() = runTest {
        val callTimes = mutableListOf<Long>()
        val scheduler = RetryingSyncScheduler(
            backgroundScope, initialDelayMs = 1_000, maxDelayMs = 4_000,
            sync = { callTimes += currentTime; SyncOutcome.RETRY },
        )

        scheduler.requestSync()
        advanceTimeBy(13_000)

        // 0, then waits of 1s, 2s, 4s, 4s, ...
        assertEquals(listOf(0L, 1_000L, 3_000L, 7_000L, 11_000L), callTimes)
    }

    @Test
    fun aNewRequestRestartsTheBackoffAtOnce() = runTest {
        val callTimes = mutableListOf<Long>()
        val scheduler = RetryingSyncScheduler(backgroundScope, sync = { callTimes += currentTime; SyncOutcome.RETRY })

        scheduler.requestSync()
        runCurrent()
        advanceTimeBy(25_000) // calls at 0, 10s and 30s are next
        scheduler.requestSync()
        runCurrent()

        assertEquals(listOf(0L, 10_000L, 25_000L), callTimes.take(3), "the new request syncs immediately")
        advanceTimeBy(10_000)
        runCurrent() // advanceTimeBy stops just before the target, so run what is due exactly then
        assertEquals(35_000L, callTimes[3], "and its own wait starts again at 10s")
    }

    @Test
    fun stopsCallingOnceSynced() = runTest {
        var calls = 0
        val scheduler = RetryingSyncScheduler(backgroundScope, sync = { calls++; SyncOutcome.COMPLETED })

        scheduler.requestSync()
        advanceTimeBy(600_000)

        assertEquals(1, calls)
    }

    @Test
    fun runsTheHookOnEveryRequest() = runTest {
        var requested = 0
        val scheduler = RetryingSyncScheduler(backgroundScope, sync = { SyncOutcome.COMPLETED }, onRequested = { requested++ })

        scheduler.requestSync()
        scheduler.requestSync()

        assertEquals(2, requested)
    }
}
