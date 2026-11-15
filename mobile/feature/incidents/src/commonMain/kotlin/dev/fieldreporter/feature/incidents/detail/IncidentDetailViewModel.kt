package dev.fieldreporter.feature.incidents.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data object Missing : DetailState
    /** [assigneeName] is the assigned Operator's name, or null when it is not known (or unassigned). */
    data class Loaded(val incident: Incident, val assigneeName: String? = null) : DetailState
}

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentDetailViewModel(
    private val incidents: IncidentRepository,
    private val users: UserRepository,
    private val id: String,
) : ViewModel() {
    val state: StateFlow<DetailState> = incidents.incident(id)
        .flatMapLatest { incident ->
            when (val assignee = incident?.assignee) {
                null -> flowOf(incident?.let { DetailState.Loaded(it) } ?: DetailState.Missing)
                else -> users.knownOperator(assignee).map { DetailState.Loaded(incident, it?.name) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    fun dismissFailure() {
        viewModelScope.launch { incidents.dismissSyncFailure(id) }
    }
}
