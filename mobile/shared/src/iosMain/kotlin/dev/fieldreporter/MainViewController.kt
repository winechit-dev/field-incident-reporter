package dev.fieldreporter

import androidx.compose.ui.window.ComposeUIViewController
import dev.fieldreporter.core.data.dataModule
import dev.fieldreporter.core.data.iosDatabaseModule
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.core.domain.UserRepository
import dev.fieldreporter.feature.incidents.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.dsl.module

private const val BASE_URL = "http://localhost:3000"

private val koin by lazy {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    startKoin {
        modules(
            iosDatabaseModule(),
            dataModule(BASE_URL),
            // Placeholder until BGTaskScheduler: syncs in the background once, without retries.
            module {
                single<SyncScheduler> { SyncScheduler { scope.launch { get<IncidentRepository>().sync() } } }
            },
        )
    }.koin
}

fun MainViewController() = ComposeUIViewController {
    val incidents = koin.get<IncidentRepository>()
    val users = koin.get<UserRepository>()
    App(incidents, users)
}
