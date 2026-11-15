package dev.fieldreporter.core.data.repository

import dev.fieldreporter.core.data.local.LocalStore
import dev.fieldreporter.core.data.remote.IncidentRemote
import dev.fieldreporter.core.data.remote.RemoteException
import dev.fieldreporter.core.domain.ServerUnavailableException
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.core.domain.User
import dev.fieldreporter.core.domain.UserRepository
import kotlinx.coroutines.flow.Flow

internal class UserRepositoryImpl(
    private val store: LocalStore,
    private val remote: IncidentRemote,
    private val scheduler: SyncScheduler,
) : UserRepository {
    override val currentUser: Flow<User?> = store.observeCurrentUser()

    override suspend fun availableUsers(): List<User> = try {
        remote.users()
    } catch (e: RemoteException) {
        throw ServerUnavailableException(e.message ?: "Can't reach the server")
    }

    override suspend fun signOut() = store.clearCurrentUser()

    override suspend fun select(user: User) {
        store.setCurrentUser(user)
        scheduler.requestSync() // pull this user's Incidents
    }
}
