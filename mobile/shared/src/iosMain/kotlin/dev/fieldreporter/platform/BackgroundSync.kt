package dev.fieldreporter.platform

import dev.fieldreporter.core.domain.SyncOutcome
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.dateWithTimeIntervalSinceNow
import kotlinx.cinterop.ObjCObjectVar

/**
 * Lets iOS run a sync when the app is not in front: a processing task that waits for a network
 * connection after a change is queued, and a periodic refresh so an Operator's changes show up
 * without the Technician opening the app. iOS decides when they actually run, and never in the
 * simulator, so the foreground retry loop in `RetryingSyncScheduler` is what covers the app being open.
 *
 * The identifiers must also be listed under `BGTaskSchedulerPermittedIdentifiers` in Info.plist.
 */
@OptIn(ExperimentalForeignApi::class)
object BackgroundSync {
    const val PROCESSING_ID = "dev.fieldreporter.sync"
    const val REFRESH_ID = "dev.fieldreporter.refresh"
    private const val REFRESH_INTERVAL_SECONDS = 15 * 60.0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Must be called before the app finishes launching. */
    fun register(sync: suspend () -> SyncOutcome) {
        for (id in listOf(PROCESSING_ID, REFRESH_ID)) {
            BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(id, usingQueue = null) { task ->
                if (task == null) return@registerForTaskWithIdentifier
                if (id == REFRESH_ID) scheduleRefresh() // keep the periodic refresh going
                val job = scope.launch {
                    var succeeded = false
                    try {
                        succeeded = sync() == SyncOutcome.COMPLETED
                    } finally {
                        // Runs on completion and on cancellation, so the task is reported exactly once.
                        task.setTaskCompletedWithSuccess(succeeded)
                    }
                }
                task.expirationHandler = { job.cancel() }
            }
        }
    }

    fun scheduleProcessing() {
        submit(BGProcessingTaskRequest(PROCESSING_ID).apply { requiresNetworkConnectivity = true })
    }

    fun scheduleRefresh() {
        submit(
            BGAppRefreshTaskRequest(REFRESH_ID).apply {
                earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(REFRESH_INTERVAL_SECONDS)
            },
        )
    }

    private fun submit(request: BGTaskRequest) {
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            // Failing to schedule (for example in the simulator) is not an error worth surfacing:
            // the foreground retry loop still runs.
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error.ptr)
        }
    }
}
