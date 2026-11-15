package dev.fieldreporter.feature.incidents.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.SyncState

/** A small coloured label, used for Status and Severity. */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SyncBadge(incident: Incident, modifier: Modifier = Modifier) {
    val label = incident.syncLabel() ?: return
    val color = if (incident.syncState == SyncState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    Pill(label, color, modifier)
}

/** Explains a failed sync and lets the Technician accept the server's version. */
@Composable
fun FailureBanner(incident: Incident, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val failure = incident.syncFailure ?: return
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(PaddingValues(16.dp)), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your change wasn't saved", style = MaterialTheme.typography.titleSmall)
            Text(failure.message.ifBlank { "The server refused it." }, style = MaterialTheme.typography.bodyMedium)
            Text("The latest version from the server is shown.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

/** A single-choice row of chips. */
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}
