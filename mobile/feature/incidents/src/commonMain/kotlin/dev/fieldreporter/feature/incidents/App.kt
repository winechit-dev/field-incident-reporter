package dev.fieldreporter.feature.incidents

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import dev.fieldreporter.feature.incidents.ui.FieldReporterTheme
import dev.fieldreporter.feature.incidents.users.UserPickerScreen
import dev.fieldreporter.feature.incidents.users.UserPickerViewModel

private enum class Session { Loading, SignedOut, SignedIn }

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
    FieldReporterTheme {
        // The surface paints the theme's background behind every screen, including the blank
        // moment while the saved user loads, so a dark theme never flashes white.
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val session by remember(users) { users.currentUser.map { if (it == null) Session.SignedOut else Session.SignedIn } }
                .collectAsStateWithLifecycle(Session.Loading)
            when (session) {
                Session.Loading -> Unit // avoids flashing the picker while the saved user loads
                Session.SignedOut -> UserPickerScreen(viewModel { UserPickerViewModel(users) })
                Session.SignedIn -> Signed(incidents, users, capabilities)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Signed(incidents: IncidentRepository, users: UserRepository, capabilities: PlatformCapabilities) {
    val nav = rememberSaveable(saver = Navigator.Saver) { Navigator() }
    val listViewModel = viewModel { IncidentListViewModel(incidents, users) }
    BackHandler(enabled = nav.canGoBack) { nav.back() }

    // Going deeper slides in from the right, going back from the left; a short distance and a
    // fade, enough to say which way you moved without making you wait.
    val depth = nav.stack.size
    var lastDepth by remember { mutableIntStateOf(depth) }
    val forward = depth >= lastDepth
    SideEffect { lastDepth = depth }
    AnimatedContent(
        targetState = nav.current,
        transitionSpec = {
            val from: (Int) -> Int = { width -> (if (forward) width else -width) / 8 }
            (fadeIn(tween(SCREEN_FADE_MS)) + slideInHorizontally(tween(SCREEN_FADE_MS), from)) togetherWith
                (fadeOut(tween(SCREEN_FADE_MS / 2)) + slideOutHorizontally(tween(SCREEN_FADE_MS)) { width -> -from(width) })
        },
        label = "screen",
    ) { screen -> Destination(screen, nav, listViewModel, incidents, users, capabilities) }
}

private const val SCREEN_FADE_MS = 220

@Composable
private fun Destination(
    screen: Screen,
    nav: Navigator,
    listViewModel: IncidentListViewModel,
    incidents: IncidentRepository,
    users: UserRepository,
    capabilities: PlatformCapabilities,
) {
    when (screen) {
        Screen.List -> IncidentListScreen(
            viewModel = listViewModel,
            onOpen = { nav.openDetail(it.id) },
            onReport = { nav.openForm() },
            onSwitchUser = listViewModel::switchUser,
        )
        is Screen.Detail -> IncidentDetailScreen(
            viewModel = viewModel(key = "detail-${screen.id}") { IncidentDetailViewModel(incidents, users, screen.id) },
            onBack = { nav.back() },
            onEdit = { nav.openForm(editingId = it.id) },
            onDelete = {
                listViewModel.requestDelete(it)
                nav.backToList()
            },
        )
        is Screen.Form -> IncidentFormScreen(
            viewModel = viewModel(key = screen.viewModelKey) {
                IncidentFormViewModel(incidents, screen.editingId, requirePhoto = capabilities.canCapturePhotos)
            },
            capabilities = capabilities,
            editing = screen.editingId != null,
            onClose = { nav.back() },
        )
    }
}
