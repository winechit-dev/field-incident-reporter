package dev.fieldreporter.platform

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.SyncScheduler
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

/** Runs one sync. A network failure asks WorkManager to retry with exponential backoff. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        when (GlobalContext.get().get<IncidentRepository>().sync()) {
            SyncOutcome.COMPLETED -> Result.success()
            SyncOutcome.RETRY -> Result.retry()
        }
}

/**
 * Syncs as soon as the device is online, retrying with backoff, plus a periodic refresh so
 * Operator changes show up without the Technician pulling. A new request replaces a running one:
 * every sync step is idempotent, so being cut off mid-way is safe.
 */
class WorkManagerSyncScheduler(context: Context) : SyncScheduler {
    private val workManager = WorkManager.getInstance(context)
    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    override fun requestSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(online)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(ONE_SHOT, ExistingWorkPolicy.REPLACE, request)
    }

    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(online)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val ONE_SHOT = "incident-sync"
        const val PERIODIC = "incident-sync-periodic"
        const val BACKOFF_SECONDS = 10L
    }
}
