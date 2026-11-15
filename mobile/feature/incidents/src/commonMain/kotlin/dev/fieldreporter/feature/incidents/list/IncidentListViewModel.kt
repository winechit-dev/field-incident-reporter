package dev.fieldreporter.feature.incidents.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.NotEditableException
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncOutcome
import dev.fieldreporter.core.domain.User
import dev.fieldreporter.core.domain.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IncidentListState(
    val user: User? = null,
    val incidents: List<Incident> = emptyList(),
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    /** The Status the list is narrowed to, or null for all. [incidents] is already narrowed. */
    val filter: Status? = null,
    /** How many Incidents there are of each Status, whatever the filter. Every Status has an entry. */
    val counts: Map<Status, Int> = emptyMap(),
    /** How many Incidents there are before the filter. */
    val total: Int = 0,
)

sealed interface ListEvent {
    /** Offer an Undo for a delete that is waiting out its grace period. */
    data class DeleteUndoOffered(val title: String) : ListEvent
    data class Message(val text: String) : ListEvent
}

/**
 * Also owns the delete flow, because a delete starts on the detail screen but its Undo lives on
 * the list. The server delete is sent only after [undoWindowMs] has passed.
 */
class IncidentListViewModel(
    private val incidents: IncidentRepository,
    private val users: UserRepository,
    private val undoWindowMs: Long = UNDO_WINDOW_MS,
) : ViewModel() {
    private val hidden = MutableStateFlow<Set<String>>(emptySet())
    private val refreshing = MutableStateFlow(false)
    private val filter = MutableStateFlow<Status?>(null)
    private val pendingDeletes = mutableMapOf<String, Job>()
    private var lastDeleted: String? = null
    private val _events = Channel<ListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state: StateFlow<IncidentListState> = combine(
        users.currentUser, incidents.incidents, hidden, refreshing, filter,
    ) { user, list, hiddenIds, isRefreshing, status ->
        val shown = list.filter { it.id !in hiddenIds }
        IncidentListState(
            user = user,
            incidents = if (status == null) shown else shown.filter { it.status == status },
            refreshing = isRefreshing,
            loaded = true,
            filter = status,
            counts = Status.entries.associateWith { s -> shown.count { it.status == s } },
            total = shown.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncidentListState())

    /** Narrow the list to one Status, or pass null to show every Incident. */
    fun onFilter(status: Status?) {
        filter.value = status
    }

    init {
        refresh(quiet = true)
    }

    fun refresh(quiet: Boolean = false) {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            try {
                val outcome = incidents.sync()
                if (outcome == SyncOutcome.RETRY && !quiet) {
                    _events.send(ListEvent.Message("Offline. Changes will sync when you're back online."))
                }
            } finally {
                refreshing.value = false
            }
        }
    }

    fun requestDelete(incident: Incident) {
        val id = incident.id
        hidden.value += id
        lastDeleted = id
        pendingDeletes[id] = viewModelScope.launch {
            delay(undoWindowMs)
            try {
                incidents.delete(id)
            } catch (e: NotEditableException) {
                _events.send(ListEvent.Message("Couldn't delete: this incident is already being handled."))
            } finally {
                hidden.value -= id
                pendingDeletes.remove(id)
            }
        }
        _events.trySend(ListEvent.DeleteUndoOffered(incident.title))
    }

    fun undoDelete() {
        val id = lastDeleted ?: return
        pendingDeletes.remove(id)?.cancel()
        hidden.value -= id
        lastDeleted = null
    }

    fun switchUser() {
        viewModelScope.launch { users.signOut() }
    }

    fun dismissFailure(id: String) {
        viewModelScope.launch { incidents.dismissSyncFailure(id) }
    }

    companion object {
        const val UNDO_WINDOW_MS = 5_000L
    }
}
