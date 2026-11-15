package dev.fieldreporter.core.data

import dev.fieldreporter.core.data.local.FieldReporterDatabase
import dev.fieldreporter.core.data.local.LocalStore
import dev.fieldreporter.core.data.local.RoomLocalStore
import dev.fieldreporter.core.data.remote.IncidentRemote
import dev.fieldreporter.core.data.remote.KtorIncidentRemote
import dev.fieldreporter.core.data.repository.IncidentRepositoryImpl
import dev.fieldreporter.core.data.repository.UserRepositoryImpl
import dev.fieldreporter.core.data.sync.SyncEngine
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.UserRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The data layer. Each platform supplies the Room database ([androidDatabaseModule] or
 * [iosDatabaseModule]) and the app supplies a `SyncScheduler`.
 *
 * @param baseUrl the mock API, e.g. `http://10.0.2.2:3000` from the Android emulator.
 */
fun dataModule(baseUrl: String): Module = module {
    single { createHttpClient() }
    single<LocalStore> { RoomLocalStore(get<FieldReporterDatabase>()) }
    single<IncidentRemote> {
        val store = get<LocalStore>()
        KtorIncidentRemote(get(), baseUrl, userId = { store.currentUser()?.id })
    }
    single { SyncEngine(get(), get(), ::deleteLocalFile) }
    single<IncidentRepository> { IncidentRepositoryImpl(get(), get(), get(), ::deleteLocalFile) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get()) }
}

private fun createHttpClient() = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; explicitNulls = false })
    }
    // Fail fast when offline so the sync engine can back off instead of hanging.
    install(HttpTimeout) {
        connectTimeoutMillis = 5_000
        requestTimeoutMillis = 30_000
    }
}

internal fun deleteLocalFile(path: String) {
    try {
        SystemFileSystem.delete(Path(path), mustExist = false)
    } catch (_: IOException) {
        // Best effort: a leftover cache file is harmless.
    }
}
