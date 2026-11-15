package dev.fieldreporter.feature.incidents.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fieldreporter.core.domain.Role
import dev.fieldreporter.core.domain.ServerUnavailableException
import dev.fieldreporter.core.domain.User
import dev.fieldreporter.core.domain.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserPickerState(
    val loading: Boolean = true,
    val technicians: List<User> = emptyList(),
    val error: String? = null,
)

/** The "choose who you are" screen. There is no login. */
class UserPickerViewModel(private val users: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(UserPickerState())
    val state: StateFlow<UserPickerState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UserPickerState(loading = true)
        viewModelScope.launch {
            _state.value = try {
                UserPickerState(loading = false, technicians = users.availableUsers().filter { it.role == Role.TECHNICIAN })
            } catch (e: ServerUnavailableException) {
                UserPickerState(loading = false, error = "Can't reach the server. Check that the API is running.")
            }
        }
    }

    fun select(user: User) {
        viewModelScope.launch { users.select(user) }
    }
}
