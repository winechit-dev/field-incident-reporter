package dev.fieldreporter

import android.app.Application
import dev.fieldreporter.core.data.androidDatabaseModule
import dev.fieldreporter.core.data.dataModule
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.dsl.module

class FieldReporterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        startKoin {
            modules(
                androidDatabaseModule(this@FieldReporterApp),
                dataModule(BASE_URL),
                // Placeholder until WorkManager: syncs in the background once, without retries.
                module {
                    single<SyncScheduler> {
                        SyncScheduler { scope.launch { getKoin().get<IncidentRepository>().sync() } }
                    }
                },
            )
        }
    }

    private companion object {
        /** The Android emulator's address for the host machine. */
        const val BASE_URL = "http://10.0.2.2:3000"
    }
}
