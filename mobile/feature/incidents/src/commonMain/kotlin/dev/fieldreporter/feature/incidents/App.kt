package dev.fieldreporter.feature.incidents

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.UserRepository
import dev.fieldreporter.feature.incidents.detail.IncidentDetailScreen
import dev.fieldreporter.feature.incidents.detail.IncidentDetailViewModel
import dev.fieldreporter.feature.incidents.form.IncidentFormScreen
import dev.fieldreporter.feature.incidents.form.IncidentFormViewModel
import dev.fieldreporter.feature.incidents.list.IncidentListScreen
import dev.fieldreporter.feature.incidents.list.IncidentListViewModel
import dev.fieldreporter.feature.incidents.users.UserPickerScreen
import dev.fieldreporter.feature.incidents.users.UserPickerViewModel

private enum class Session { Loading, SignedOut, SignedIn }

private sealed interface Screen {
    data object List : Screen
    data class Detail(val id: String) : Screen
    data class Form(val editingId: String?) : Screen
}

/**
 * The whole app. The host (Android activity or iOS view controller) supplies the repositories
 * and the platform capabilities.
 */
@Composable
fun App(
    incidents: IncidentRepository,
    users: UserRepository,
    capabilities: PlatformCapabilities = NoPlatformCapabilities,
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).components { add(KtorNetworkFetcherFactory()) }.build()
    }
    MaterialTheme {
        val session by remember(users) { users.currentUser.map { if (it == null) Session.SignedOut else Session.SignedIn } }
            .collectAsStateWithLifecycle(Session.Loading)
        when (session) {
            Session.Loading -> Unit // avoids flashing the picker while the saved user loads
            Session.SignedOut -> UserPickerScreen(viewModel { UserPickerViewModel(users) })
            Session.SignedIn -> Signed(incidents, users, capabilities)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Signed(incidents: IncidentRepository, users: UserRepository, capabilities: PlatformCapabilities) {
    val stack = remember { mutableStateListOf<Screen>(Screen.List) }
    val listViewModel = viewModel { IncidentListViewModel(incidents, users) }
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    when (val screen = stack.last()) {
        Screen.List -> IncidentListScreen(
            viewModel = listViewModel,
            onOpen = { stack.add(Screen.Detail(it.id)) },
            onReport = { stack.add(Screen.Form(editingId = null)) },
            onSwitchUser = listViewModel::switchUser,
        )
        is Screen.Detail -> IncidentDetailScreen(
            viewModel = viewModel(key = "detail-${screen.id}") { IncidentDetailViewModel(incidents, screen.id) },
            onBack = { stack.removeAt(stack.lastIndex) },
            onEdit = { stack.add(Screen.Form(editingId = it.id)) },
            onDelete = {
                listViewModel.requestDelete(it)
                stack.clear()
                stack.add(Screen.List)
            },
        )
        is Screen.Form -> IncidentFormScreen(
            viewModel = viewModel(key = "form-${screen.editingId ?: "new"}") {
                IncidentFormViewModel(incidents, screen.editingId, requirePhoto = capabilities.canCapturePhotos)
            },
            capabilities = capabilities,
            editing = screen.editingId != null,
            onClose = { stack.removeAt(stack.lastIndex) },
        )
    }
}
