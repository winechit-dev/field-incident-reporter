package dev.fieldreporter.feature.incidents.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.User
import dev.fieldreporter.feature.incidents.ui.AppCard
import dev.fieldreporter.feature.incidents.ui.Avatar
import dev.fieldreporter.feature.incidents.ui.ContentWidth
import dev.fieldreporter.feature.incidents.ui.EmptyState
import dev.fieldreporter.feature.incidents.ui.FailureBanner
import dev.fieldreporter.feature.incidents.ui.SeverityLevel
import dev.fieldreporter.feature.incidents.ui.StatusPill
import dev.fieldreporter.feature.incidents.ui.SyncBadge
import dev.fieldreporter.feature.incidents.ui.display
import dev.fieldreporter.feature.incidents.ui.imageModel
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
    var showAccount by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

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
                ListEvent.ReportSaved -> {
                    // The new report is newest, so it is first: bring it into view, then say so.
                    launch { listState.animateScrollToItem(0) }
                    snackbar.showSnackbar("Report saved", duration = SnackbarDuration.Short)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incidents", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    state.user?.let { user ->
                        IconButton(onClick = { showAccount = true }, modifier = Modifier.padding(end = 8.dp)) {
                            Avatar(user.name, Modifier.semantics { contentDescription = "Account: ${user.name}" }, size = 36.dp)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onReport,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Report incident") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.loaded && state.total > 0) StatusFilter(state, viewModel::onFilter)
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    !state.loaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    // Scrollable even when empty, so pulling down to refresh still works.
                    state.incidents.isEmpty() -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val filter = state.filter
                        if (filter == null) {
                            EmptyState(
                                icon = Icons.AutoMirrored.Filled.List,
                                title = "No incidents yet",
                                body = "Tap Report incident to add the first one. Pull down to check for updates.",
                            )
                        } else {
                            EmptyState(
                                icon = Icons.Filled.Search,
                                title = "Nothing ${filter.label().lowercase()}",
                                body = "None of your incidents has this status right now.",
                            ) { OutlinedButton(onClick = { viewModel.onFilter(null) }) { Text("Show all incidents") } }
                        }
                    }
                    else -> ContentWidth {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.incidents, key = { it.id }) { incident ->
                                IncidentCard(
                                    incident,
                                    onClick = { onOpen(incident) },
                                    onDismissFailure = { viewModel.dismissFailure(incident.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val user = state.user
    if (showAccount && user != null) {
        AccountSheet(user, onDismiss = { showAccount = false }, onSwitch = { showAccount = false; onSwitchUser() })
    }
}

/** One chip per Status, each with its count, and All in front. A summary and a filter in one row. */
@Composable
private fun StatusFilter(state: IncidentListState, onFilter: (Status?) -> Unit) {
    ContentWidth {
        LazyRow(
            Modifier.selectableGroup(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "all") { CountChip("All", state.total, state.filter == null) { onFilter(null) } }
            items(Status.entries, key = { it.name }) { status ->
                CountChip(status.label(), state.counts[status] ?: 0, state.filter == status) { onFilter(status) }
            }
        }
    }
}

@Composable
private fun CountChip(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        shape = CircleShape,
        modifier = Modifier.semantics { role = Role.RadioButton },
    )
}

@Composable
private fun IncidentCard(incident: Incident, onClick: () -> Unit, onDismissFailure: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(incident.status)
                Text(
                    incident.createdAt.display(),
                    Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(incident.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (incident.description.isNotBlank()) {
                        Text(
                            incident.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                incident.photos.firstNotNullOfOrNull { it.imageModel() }?.let { model ->
                    AsyncImage(
                        model = model,
                        contentDescription = null, // the title beside it says what this Incident is
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SeverityLevel(incident.severity)
                Text(
                    incident.category.label(),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                SyncBadge(incident)
            }
            FailureBanner(incident, onDismiss = onDismissFailure)
        }
    }
}

/** Who is signed in, and the way to become someone else. Opening it is the pause a confirm dialog would give. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSheet(user: User, onDismiss: () -> Unit, onSwitch: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 16.dp), // the sheet already clears the navigation bar
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(user.name, size = 56.dp)
                Column {
                    Text(user.name, style = MaterialTheme.typography.titleLarge)
                    Text("Technician", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "Switching takes you back to the list of names. Changes still waiting to sync stay on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onSwitch, modifier = Modifier.fillMaxWidth()) { Text("Switch technician") }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}
