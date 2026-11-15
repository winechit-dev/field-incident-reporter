package dev.fieldreporter.platform

import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

/**
 * Syncs now, and keeps retrying with exponential backoff until a sync completes.
 *
 * A new request cancels a retry that is still waiting and starts again straight away. Every sync
 * step is idempotent, so being cut off mid-way is safe. Requests are funnelled through one
 * conflated channel, so this holds no shared mutable state and may be called from any thread.
 *
 * @param onRequested runs on every request, for platform work such as asking the OS for a
 *   background task, so a sync can happen even if the app is suspended before it finishes.
 */
class RetryingSyncScheduler(
    scope: CoroutineScope,
    private val sync: suspend () -> SyncOutcome,
    private val onRequested: () -> Unit = {},
    private val initialDelayMs: Long = 10_000,
    private val maxDelayMs: Long = 300_000,
) : SyncScheduler {
    private val requests = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch { requests.consumeAsFlow().collectLatest { syncUntilDone() } }
    }

    override fun requestSync() {
        onRequested()
        requests.trySend(Unit)
    }

    private suspend fun syncUntilDone() {
        var wait = initialDelayMs
        while (sync() != SyncOutcome.COMPLETED) {
            delay(wait)
            wait = minOf(wait * 2, maxDelayMs)
        }
    }
}
