package dev.fieldreporter

import dev.fieldreporter.core.data.dataModule
import dev.fieldreporter.core.data.iosDatabaseModule
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.platform.BackgroundSync
import dev.fieldreporter.platform.RetryingSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** App-wide setup for iOS. Everything is created on first use, so launching stays quick. */
internal object IosApp {
    /** The iOS simulator shares the Mac's network, so the mock API is on localhost. */
    private const val BASE_URL = "http://localhost:3000"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val scheduler: SyncScheduler by lazy {
        RetryingSyncScheduler(
            scope = scope,
            sync = { koin.get<IncidentRepository>().sync() },
            onRequested = { BackgroundSync.scheduleProcessing() },
        )
    }

    val koin: Koin by lazy {
        startKoin {
            modules(
                iosDatabaseModule(),
                dataModule(BASE_URL),
                module { single<SyncScheduler> { scheduler } },
            )
        }.koin
    }
}

/**
 * Called from Swift when the app starts: iOS requires background tasks to be registered before
 * launch finishes.
 */
fun registerBackgroundTasks() {
    BackgroundSync.register { IosApp.koin.get<IncidentRepository>().sync() }
    BackgroundSync.scheduleRefresh()
}
