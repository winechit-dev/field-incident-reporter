package dev.fieldreporter.feature.incidents.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.feature.incidents.ui.FailureBanner
import dev.fieldreporter.feature.incidents.ui.PhotoStrip
import dev.fieldreporter.feature.incidents.ui.Pill
import dev.fieldreporter.feature.incidents.ui.SyncBadge
import dev.fieldreporter.feature.incidents.ui.color
import dev.fieldreporter.feature.incidents.ui.display
import dev.fieldreporter.feature.incidents.ui.imageModel
import dev.fieldreporter.feature.incidents.ui.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    viewModel: IncidentDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Incident) -> Unit,
    onDelete: (Incident) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Incident") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = state) {
                DetailState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                DetailState.Missing -> Text(
                    "This incident no longer exists.",
                    Modifier.align(Alignment.Center).padding(32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
                is DetailState.Loaded -> {
                    Content(s.incident, s.assigneeName, viewModel::dismissFailure, onEdit = { onEdit(s.incident) }, onDelete = { confirmDelete = true })
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
private fun Content(incident: Incident, assigneeName: String?, onDismissFailure: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FailureBanner(incident, onDismissFailure)
        val photos = incident.photos.mapNotNull { p -> p.imageModel()?.let { p.id to it } }
        if (photos.isNotEmpty()) PhotoStrip(photos)

        Text(incident.title, style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(incident.status.label(), incident.status.color())
            Pill(incident.severity.label(), incident.severity.color())
            Pill(incident.category.label(), MaterialTheme.colorScheme.secondary)
            SyncBadge(incident)
        }
        if (incident.description.isNotBlank()) Text(incident.description, style = MaterialTheme.typography.bodyLarge)

        Section("Location") {
            val loc = incident.location
            val parts = buildList {
                loc.address?.let { add(it) }
                if (loc.hasCoordinates) add("${loc.latitude}, ${loc.longitude}")
            }
            Text(parts.joinToString("\n").ifEmpty { "Location unavailable" })
        }
        Section("Reported") { Text(incident.createdAt.display()) }

        if (incident.assignee != null) Section("Handling") { Text(handlingText(assigneeName)) }
        incident.resolutionNote?.takeIf { it.isNotBlank() }?.let { note -> Section("Resolution") { Text(note) } }
        if (incident.activityLog.isNotEmpty()) {
            Section("Activity") {
                incident.activityLog.forEach { entry ->
                    Text("${entry.at.display()} · ${entry.status.label()}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        HorizontalDivider()
        if (incident.isEditable) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            }
        } else {
            Text(
                "An operator is already handling this incident, so it can't be changed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/** The Handling line: the Operator's name when known, today's wording otherwise (also for a blank name). */
internal fun handlingText(assigneeName: String?): String =
    if (assigneeName.isNullOrBlank()) "An operator is handling this incident." else "$assigneeName is handling this incident."

@Composable
private fun Section(title: String, body: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
        body()
    }
}
