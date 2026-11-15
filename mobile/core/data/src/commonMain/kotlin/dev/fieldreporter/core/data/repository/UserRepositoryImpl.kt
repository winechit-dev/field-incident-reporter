package dev.fieldreporter.core.data.repository

import dev.fieldreporter.core.data.local.LocalStore
import dev.fieldreporter.core.data.remote.IncidentRemote
import dev.fieldreporter.core.data.remote.RemoteException
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.ServerUnavailableException
import dev.fieldreporter.core.domain.SyncScheduler
import dev.fieldreporter.core.domain.User
import dev.fieldreporter.core.domain.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class UserRepositoryImpl(
    private val store: LocalStore,
    private val remote: IncidentRemote,
    private val scheduler: SyncScheduler,
) : UserRepository {
    override val currentUser: Flow<User?> = store.observeCurrentUser()

    override suspend fun availableUsers(): List<User> = try {
        remote.users().also { store.setKnownUsers(it) }
    } catch (e: RemoteException) {
        throw ServerUnavailableException(e.message ?: "Can't reach the server")
    }

    override fun knownOperator(id: String): Flow<User?> =
        store.observeKnownUsers().map { users -> users.firstOrNull { it.id == id && it.role == Role.OPERATOR } }

    override suspend fun signOut() = store.clearCurrentUser()

    override suspend fun select(user: User) {
        store.setCurrentUser(user)
        scheduler.requestSync() // pull this user's Incidents
    }
}
