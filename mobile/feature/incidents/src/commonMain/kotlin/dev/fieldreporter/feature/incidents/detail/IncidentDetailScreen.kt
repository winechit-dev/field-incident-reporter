package dev.fieldreporter.feature.incidents.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import dev.fieldreporter.feature.incidents.ui.ContentWidth
import dev.fieldreporter.feature.incidents.ui.EmptyState
import dev.fieldreporter.feature.incidents.ui.FailureBanner
import dev.fieldreporter.feature.incidents.ui.IncidentPills
import dev.fieldreporter.feature.incidents.ui.PhotoStrip
import dev.fieldreporter.feature.incidents.ui.SectionLabel
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
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val editable = (state as? DetailState.Loaded)?.incident?.takeIf { it.isEditable }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident") },
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
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Text("Delete", Modifier.padding(start = ButtonDefaults.IconSpacing))
                    }
                    Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
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
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FailureBanner(incident, onDismissFailure)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(incident.title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
                IncidentPills(incident, showCategory = true)
            }
            if (!incident.isEditable) {
                // Said up front, where the Edit and Delete buttons would otherwise be looked for.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "An operator is already handling this incident, so it can't be changed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val photos = incident.photos.mapNotNull { p -> p.imageModel()?.let { p.id to it } }
            if (photos.isNotEmpty()) PhotoStrip(photos)

            if (incident.description.isNotBlank()) {
                Section("What happened") { Text(incident.description, style = MaterialTheme.typography.bodyLarge) }
            }
            Section("Location") {
                val loc = incident.location
                val parts = buildList {
                    loc.address?.let { add(it) }
                    if (loc.hasCoordinates) add("${loc.latitude}, ${loc.longitude}")
                }
                Text(parts.joinToString("\n").ifEmpty { "Location unavailable" }, style = MaterialTheme.typography.bodyLarge)
            }
            Section("Reported") { Text(incident.createdAt.display(), style = MaterialTheme.typography.bodyLarge) }

            if (incident.assignee != null) Section("Handling") { Text(handlingText(assigneeName), style = MaterialTheme.typography.bodyLarge) }
            incident.resolutionNote?.takeIf { it.isNotBlank() }?.let { note ->
                Section("Resolution") { Text(note, style = MaterialTheme.typography.bodyLarge) }
            }
            if (incident.activityLog.isNotEmpty()) {
                Section("Activity") {
                    incident.activityLog.forEach { entry ->
                        Text("${entry.status.label()} · ${entry.at.display()}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** The Handling line: the Operator's name when known, today's wording otherwise (also for a blank name). */
internal fun handlingText(assigneeName: String?): String =
    if (assigneeName.isNullOrBlank()) "An operator is handling this incident." else "$assigneeName is handling this incident."

@Composable
private fun Section(title: String, body: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel(title, Modifier.semantics { heading() })
        body()
    }
}
