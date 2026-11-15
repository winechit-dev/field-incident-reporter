package dev.fieldreporter.feature.incidents.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DetailState {
    data object Loading : DetailState
    data object Missing : DetailState
    data class Loaded(val incident: Incident) : DetailState
}

class IncidentDetailViewModel(private val incidents: IncidentRepository, private val id: String) : ViewModel() {
    val state: StateFlow<DetailState> = incidents.incident(id)
        .map { if (it == null) DetailState.Missing else DetailState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    fun dismissFailure() {
        viewModelScope.launch { incidents.dismissSyncFailure(id) }
    }
}
