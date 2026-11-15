package dev.fieldreporter.feature.incidents.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.feature.incidents.ui.FailureBanner
import dev.fieldreporter.feature.incidents.ui.Pill
import dev.fieldreporter.feature.incidents.ui.SyncBadge
import dev.fieldreporter.feature.incidents.ui.color
import dev.fieldreporter.feature.incidents.ui.display
import dev.fieldreporter.feature.incidents.ui.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(
    viewModel: IncidentListViewModel,
    onOpen: (Incident) -> Unit,
    onReport: () -> Unit,
    onSwitchUser: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ListEvent.DeleteUndoOffered -> {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(
                        message = "Deleted \"${event.title}\"",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                }
                is ListEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.user?.name ?: "Incidents") },
                actions = { TextButton(onClick = onSwitchUser) { Text("Switch") } },
            )
        },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onReport) { Text("Report incident") } },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            if (state.loaded && state.incidents.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No incidents yet. Tap Report incident to add the first one.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.incidents, key = { it.id }) { incident ->
                        IncidentCard(incident, onClick = { onOpen(incident) }, onDismissFailure = { viewModel.dismissFailure(incident.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentCard(incident: Incident, onClick: () -> Unit, onDismissFailure: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(incident.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(incident.status.label(), incident.status.color())
                Pill(incident.severity.label(), incident.severity.color())
                SyncBadge(incident)
            }
            Text(
                "${incident.category.label()} · ${incident.createdAt.display()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            FailureBanner(incident, onDismiss = onDismissFailure)
        }
    }
}
