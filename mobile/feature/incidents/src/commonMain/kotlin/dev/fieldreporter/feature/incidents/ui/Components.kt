package dev.fieldreporter.feature.incidents.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncState

/** Content never grows wider than this, so lines stay readable on a tablet or in landscape. */
val MaxContentWidth = 640.dp

/** Centres [content] and caps its width at [MaxContentWidth]. */
@Composable
fun ContentWidth(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = MaxContentWidth).fillMaxWidth()) { content() }
    }
}

/** A small tinted label. Meaning is always in the text; the colour only supports it. */
@Composable
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Box(modifier.background(tone.container, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = tone.content, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
fun StatusPill(status: Status, modifier: Modifier = Modifier) = Pill(status.label(), status.tone(LocalDarkTheme.current), modifier)

@Composable
fun SeverityPill(severity: Severity, modifier: Modifier = Modifier) = Pill(severity.label(), severity.tone(LocalDarkTheme.current), modifier)

/** A label with no meaning of its own to colour, such as a Category. */
@Composable
fun NeutralPill(text: String, modifier: Modifier = Modifier) =
    Pill(text, Tone(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant), modifier)

@Composable
fun SyncBadge(incident: Incident, modifier: Modifier = Modifier) {
    val label = incident.syncLabel() ?: return
    val colors = MaterialTheme.colorScheme
    val tone = if (incident.syncState == SyncState.FAILED) Tone(colors.errorContainer, colors.onErrorContainer)
    else Tone(colors.surfaceVariant, colors.onSurfaceVariant)
    Pill(label, tone, modifier)
}

/** The Status, Severity and Sync State of an Incident. Wraps instead of clipping on a narrow screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncidentPills(incident: Incident, modifier: Modifier = Modifier, showCategory: Boolean = false) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(incident.status)
        SeverityPill(incident.severity)
        if (showCategory) NeutralPill(incident.category.label())
        SyncBadge(incident)
    }
}

/** The app's card: a white (or raised, in the dark theme) surface with a hairline border. */
@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) Card(onClick = onClick, modifier = modifier, colors = colors, border = border, content = content)
    else Card(modifier = modifier, colors = colors, border = border, content = content)
}

/** Explains a failed sync and lets the Technician accept the server's version. */
@Composable
fun FailureBanner(incident: Incident, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val failure = incident.syncFailure ?: return
    Notice(
        title = "Your change wasn't saved",
        body = failure.message.ifBlank { "The server refused it." } + "\nThe latest version from the server is shown.",
        modifier = modifier,
    ) { TextButton(onClick = onDismiss) { Text("OK") } }
}

/** Something went wrong, or cannot be done. An icon and words, never colour alone. */
@Composable
fun Notice(title: String?, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val colors = MaterialTheme.colorScheme
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer)) {
        Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (action == null) 16.dp else 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (action != null) Box(Modifier.align(Alignment.End).padding(horizontal = 8.dp)) { action() }
    }
}

/** A full-area message for "nothing here" or "couldn't load": what happened, then the way out. */
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

/** The small heading above a group of fields or a block of detail. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A single-choice set of chips. They wrap, so every option is visible without scrolling sideways. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                modifier = Modifier.semantics { role = Role.RadioButton },
            )
        }
    }
}
