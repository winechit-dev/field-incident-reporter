package dev.fieldreporter.feature.incidents.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.feature.incidents.ui.AppCard
import dev.fieldreporter.feature.incidents.ui.ContentWidth
import dev.fieldreporter.feature.incidents.ui.EmptyState
import dev.fieldreporter.feature.incidents.ui.FailureBanner
import dev.fieldreporter.feature.incidents.ui.IncidentPills
import dev.fieldreporter.feature.incidents.ui.InfoRow
import dev.fieldreporter.feature.incidents.ui.LocalDarkTheme
import dev.fieldreporter.feature.incidents.ui.PhotoStrip
import dev.fieldreporter.feature.incidents.ui.PhotoViewer
import dev.fieldreporter.feature.incidents.ui.SinglePhoto
import dev.fieldreporter.feature.incidents.ui.SectionLabel
import dev.fieldreporter.feature.incidents.ui.display
import dev.fieldreporter.feature.incidents.ui.imageModel
import dev.fieldreporter.feature.incidents.ui.label
import dev.fieldreporter.feature.incidents.ui.tone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    viewModel: IncidentDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Incident) -> Unit,
    onDelete: (Incident) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val editable = (state as? DetailState.Loaded)?.incident?.takeIf { it.isEditable }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        // The actions stay in reach however long the Incident is, instead of sitting below the scroll.
        bottomBar = { if (editable != null) Actions(onEdit = { onEdit(editable) }, onDelete = { confirmDelete = true }) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                DetailState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                DetailState.Missing -> EmptyState(
                    icon = Icons.Filled.Search,
                    title = "This incident no longer exists",
                    body = "It may have been deleted on another device.",
                    modifier = Modifier.align(Alignment.Center),
                ) { Button(onClick = onBack) { Text("Back to incidents") } }
                is DetailState.Loaded -> {
                    Content(s.incident, s.assigneeName, viewModel::dismissFailure)
                    if (confirmDelete) {
                        AlertDialog(
                            onDismissRequest = { confirmDelete = false },
                            title = { Text("Delete this incident?") },
                            text = { Text("You can undo for a few seconds after deleting.") },
                            confirmButton = {
                                TextButton(onClick = { confirmDelete = false; onDelete(s.incident) }) {
                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Actions(onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ContentWidth(Modifier.navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Text("Delete", Modifier.padding(start = ButtonDefaults.IconSpacing))
                    }
                    Button(onClick = onEdit, modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Text("Edit", Modifier.padding(start = ButtonDefaults.IconSpacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun Content(incident: Incident, assigneeName: String?, onDismissFailure: () -> Unit) {
    ContentWidth {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val side = Modifier.padding(horizontal = 16.dp)
            if (incident.syncFailure != null) FailureBanner(incident, onDismissFailure, side)

            // Photos first and large: they are what a Technician recognises an Incident by. The
            // strip scrolls edge to edge, so its padding is inside it rather than around it.
            val photos = incident.photos.mapNotNull { p -> p.imageModel()?.let { p.id to it } }
            var viewing by rememberSaveable { mutableStateOf(NOT_VIEWING) }
            when (photos.size) {
                0 -> Unit
                1 -> SinglePhoto(photos[0].second, onOpen = { viewing = 0 }, modifier = side)
                else -> PhotoStrip(photos, width = 280.dp, height = 200.dp, contentPadding = PaddingValues(horizontal = 16.dp), onOpen = { viewing = it })
            }
            if (viewing != NOT_VIEWING && photos.isNotEmpty()) {
                PhotoViewer(photos.map { it.second }, start = viewing, onClose = { viewing = NOT_VIEWING })
            }

            Column(side, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(incident.title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
                IncidentPills(incident)
            }
            if (!incident.isEditable) {
                // Said up front, where the Edit and Delete buttons would otherwise be looked for.
                Row(
                    side.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "An operator is already handling this incident, so it can't be changed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (incident.description.isNotBlank()) {
                Section("What happened", side) { Text(incident.description, style = MaterialTheme.typography.bodyLarge) }
            }

            AppCard(side.fillMaxWidth()) {
                val loc = incident.location
                val where = buildList {
                    loc.address?.let { add(it) }
                    if (loc.hasCoordinates) add("${loc.latitude}, ${loc.longitude}")
                }.joinToString("\n").ifEmpty { "Location unavailable" }
                InfoRow(Icons.Filled.LocationOn, "Location", where)
                HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow(Icons.Filled.DateRange, "Reported", incident.createdAt.display())
                if (incident.assignee != null) {
                    HorizontalDivider(Modifier.padding(start = 68.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    InfoRow(Icons.Filled.Person, "Handling", handlingText(assigneeName))
                }
            }

            incident.resolutionNote?.takeIf { it.isNotBlank() }?.let { note ->
                Section("Resolution", side) { Text(note, style = MaterialTheme.typography.bodyLarge) }
            }
            if (incident.activityLog.isNotEmpty()) {
                Section("Activity", side) {
                    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        incident.activityLog.forEach { entry ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(entry.status.tone(LocalDarkTheme.current).content, CircleShape))
                                Text(entry.status.label(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(entry.at.display(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val NOT_VIEWING = -1

/** The Handling line: the Operator's name when known, today's wording otherwise (also for a blank name). */
internal fun handlingText(assigneeName: String?): String =
    if (assigneeName.isNullOrBlank()) "An operator is handling this incident." else "$assigneeName is handling this incident."

@Composable
private fun Section(title: String, modifier: Modifier = Modifier, body: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionLabel(title, Modifier.semantics { heading() })
        body()
    }
}
