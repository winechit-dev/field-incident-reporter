package dev.fieldreporter

import android.app.Application
import dev.fieldreporter.core.data.androidDatabaseModule
import dev.fieldreporter.core.data.dataModule
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.platform.WorkManagerSyncScheduler
import org.koin.core.context.startKoin
import org.koin.dsl.module

class FieldReporterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val scheduler = WorkManagerSyncScheduler(this)
        startKoin {
            modules(
                androidDatabaseModule(this@FieldReporterApp),
                dataModule(BASE_URL),
                module { single<SyncScheduler> { scheduler } },
            )
        }
        scheduler.schedulePeriodic()
    }

    private companion object {
        /** The Android emulator's address for the host machine. */
        const val BASE_URL = "http://10.0.2.2:3000"
    }
}
