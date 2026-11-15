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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier, dot: Boolean = false) {
    Row(
        modifier.background(tone.container, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) Box(Modifier.size(6.dp).background(tone.content, CircleShape))
        Text(text, color = tone.content, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** Where an Incident stands: a tinted pill with a dot. The one filled badge on a card. */
@Composable
fun StatusPill(status: Status, modifier: Modifier = Modifier) =
    Pill(status.label(), status.tone(LocalDarkTheme.current), modifier, dot = true)

/**
 * How serious an Incident is: one to four bars and the word. Drawn differently from Status on
 * purpose, so the two are never read as the same kind of thing.
 */
@Composable
fun SeverityLevel(severity: Severity, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        SeverityBars(severity)
        Text(severity.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

/** One to four filled bars out of four. They repeat a word shown beside them, so a screen reader skips them. */
@Composable
fun SeverityBars(severity: Severity, modifier: Modifier = Modifier) {
    val filled = severity.tone(LocalDarkTheme.current).content
    val idle = MaterialTheme.colorScheme.outlineVariant
    Row(modifier.height(12.dp).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        for (bar in 1..4) {
            Box(Modifier.width(3.dp).height((3 * bar).dp).background(if (bar <= severity.level()) filled else idle, RoundedCornerShape(1.dp)))
        }
    }
}

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

/** The Status, Severity, Category and Sync State of an Incident. Wraps instead of clipping on a narrow screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncidentPills(incident: Incident, modifier: Modifier = Modifier) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        StatusPill(incident.status)
        SeverityLevel(incident.severity)
        NeutralPill(incident.category.label())
        SyncBadge(incident)
    }
}

/** A person, as their initials on a tinted disc. Decorative: the name is always shown or spoken beside it. */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier.size(size).background(MaterialTheme.colorScheme.primaryContainer, CircleShape).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials(name),
            style = if (size >= 56.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** The app's card: a white (or raised, in the dark theme) surface with a hairline border and no shadow. */
@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val shape = MaterialTheme.shapes.large
    if (onClick != null) Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, border = border, content = content)
    else Card(modifier = modifier, shape = shape, colors = colors, border = border, content = content)
}

/** One fact about an Incident: an icon, what it is, and its value. Rows stack inside an [AppCard]. */
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
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
    Card(
        modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
    ) {
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
        Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(title, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action?.let { Box(Modifier.padding(top = 4.dp)) { it() } }
    }
}

/** The small heading above a group of fields or a block of detail. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
}

/**
 * A single-choice set of chips. They wrap, so every option is visible without scrolling sideways.
 * [tone] colours the selected chip, for choices that carry a colour of their own.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tone: ((T) -> Tone)? = null,
) {
    FlowRow(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            val chosen = tone?.invoke(option)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else null,
                shape = CircleShape,
                colors = if (chosen == null) FilterChipDefaults.filterChipColors() else FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chosen.container,
                    selectedLabelColor = chosen.content,
                    selectedLeadingIconColor = chosen.content,
                ),
                modifier = Modifier.semantics { role = Role.RadioButton },
            )
        }
    }
}
