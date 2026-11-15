package dev.fieldreporter.feature.incidents.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.MAX_PHOTOS
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.feature.incidents.PhotoActions
import dev.fieldreporter.feature.incidents.PlatformCapabilities
import dev.fieldreporter.feature.incidents.ui.AppIcons
import dev.fieldreporter.feature.incidents.ui.LocalDarkTheme
import dev.fieldreporter.feature.incidents.ui.Notice
import dev.fieldreporter.feature.incidents.ui.PhotoStrip
import dev.fieldreporter.feature.incidents.ui.SectionLabel
import dev.fieldreporter.feature.incidents.ui.SeverityBars
import dev.fieldreporter.feature.incidents.ui.icon
import dev.fieldreporter.feature.incidents.ui.label
import dev.fieldreporter.feature.incidents.ui.tone
import kotlinx.coroutines.launch

/**
 * The report (or edit) form as a full-height sheet that slides up over the screen it was opened
 * from. Three parts: a header with the close button and title, the scrolling fields, and a footer
 * that keeps the submit button in reach.
 *
 * The sheet can be closed by the close button, the back gesture, a tap outside or a drag down.
 * With something entered, every one of those asks before discarding it.
 *
 * @param onDismissed the sheet has gone without saving. May be called more than once.
 * @param onSaved the form was saved and the sheet has gone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentFormSheet(
    viewModel: IncidentFormViewModel,
    capabilities: PlatformCapabilities,
    editing: Boolean,
    onDismissed: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var discarding by remember { mutableStateOf(false) }
    val unsaved by rememberUpdatedState(state.hasUnsavedChanges)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true, // opens at full height; there is no half-open state to land in
        confirmValueChange = { target ->
            // Refuse to hide while there is something to lose, and ask instead. This one check
            // covers the back gesture, a tap outside and a drag down.
            if (target == SheetValue.Hidden && unsaved && !discarding) {
                confirmDiscard = true
                false
            } else {
                true
            }
        },
    )
    // Slide down first, then leave the back stack, so closing is animated like opening.
    val slideAway: (() -> Unit) -> Unit = { then -> scope.launch { sheetState.hide() }.invokeOnCompletion { then() } }
    LaunchedEffect(state.done) {
        if (state.done) {
            sheetState.hide()
            onSaved()
        }
    }

    val photoActions = capabilities.rememberPhotoActions(viewModel::onPhotoAdded)
    val lookupLocation = capabilities.rememberLocationLookup(viewModel::onLocationFound)

    // A refused submit brings the first problem into view. Without this, an error below the fold
    // (a missing photo) made the button look as if it had done nothing.
    val scroll = rememberScrollState()
    LaunchedEffect(state.rejectedSubmits) {
        when (state.firstProblem) {
            FormField.TITLE, FormField.DESCRIPTION -> scroll.animateScrollTo(0)
            FormField.PHOTOS -> scroll.animateScrollTo(scroll.maxValue)
            null -> Unit
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissed,
        sheetState = sheetState,
        // Stops below the status bar, so the clock and battery stay readable above the sheet.
        modifier = Modifier.statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { DragHandle() },
        // The footer and the keyboard padding below handle the bottom edge themselves.
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        // The keyboard shrinks the sheet's content rather than covering it: the header stays and
        // the footer rides above the keys.
        BoxWithConstraints(Modifier.fillMaxHeight().imePadding()) {
            // On a short screen (a phone on its side, or with the keyboard up) a fixed footer
            // would leave only a sliver for the fields, so there it scrolls with them instead.
            val fixedFooter = maxHeight >= SHORT_SCREEN
            val footer: @Composable () -> Unit = {
                Footer(
                    label = if (editing) "Save changes" else "Submit report",
                    saving = state.saving,
                    enabled = !state.saving && state.blockedMessage == null,
                    divided = fixedFooter,
                    onSubmit = viewModel::submit,
                )
            }
            Column(Modifier.fillMaxSize()) {
                Header(
                    title = if (editing) "Edit incident" else "Report incident",
                    scrolled = scroll.value > 0,
                    onClose = { if (state.hasUnsavedChanges) confirmDiscard = true else slideAway(onDismissed) },
                )
                if (!state.loaded) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Column(Modifier.weight(1f).verticalScroll(scroll)) {
                        Column(
                            Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
                        ) {
                            state.blockedMessage?.let { Notice(title = null, body = it) }
                            DetailsSection(state, viewModel)
                            Section("Category") { CategoryPicker(state.category, viewModel::onCategory) }
                            Section("Severity") { SeverityPicker(state.severity, viewModel::onSeverity) }
                            LocationSection(state, viewModel, lookupLocation)
                            PhotosSection(state, viewModel, photoActions)
                        }
                        if (!fixedFooter) footer()
                    }
                    if (fixedFooter) footer()
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(if (editing) "Discard your changes?" else "Discard this report?") },
            text = { Text("What you entered here will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    discarding = true
                    slideAway(onDismissed)
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

/** Between sections. Inside a section, its parts sit 6 to 8 dp apart, so the groups read as groups. */
private val SECTION_GAP = 16.dp

/** Below this much height for the sheet's content, the footer stops being fixed. */
private val SHORT_SCREEN = 480.dp

private const val MAX_TITLE = 120
private const val MAX_DESCRIPTION = 2000

/** Material's handle with less air around it, so the header sits close under the sheet's top edge. */
@Composable
private fun DragHandle() {
    Box(
        Modifier.padding(top = 10.dp, bottom = 2.dp).size(width = 32.dp, height = 4.dp)
            .background(MaterialTheme.colorScheme.outline, CircleShape)
            .semantics { contentDescription = "Drag down to close" },
    )
}

@Composable
private fun Header(title: String, scrolled: Boolean, onClose: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            Text(title, Modifier.padding(start = 4.dp).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        }
        // A hairline appears once the fields have scrolled under the header.
        HorizontalDivider(color = if (scrolled) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.surface)
    }
}

@Composable
private fun Section(title: String, trailing: String? = null, hint: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(title, Modifier.weight(1f).semantics { heading() })
            if (trailing != null) {
                Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
    }
}

@Composable
private fun DetailsSection(state: FormState, viewModel: IncidentFormViewModel) {
    val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    Column(verticalArrangement = Arrangement.spacedBy(SECTION_GAP)) {
        Field(
            name = "Title", value = state.title, onChange = viewModel::onTitle,
            placeholder = "A short name for what happened", error = state.titleError,
            counter = "${state.title.trim().length}/$MAX_TITLE", singleLine = true,
            keyboard = sentences.copy(imeAction = ImeAction.Next),
        )
        Field(
            name = "What happened?", value = state.description, onChange = viewModel::onDescription,
            placeholder = "What you saw, and anything that makes it urgent", error = state.descriptionError,
            counter = "${state.description.length}/$MAX_DESCRIPTION", minLines = 3, keyboard = sentences,
        )
    }
}

/**
 * A text field with its label above it, always visible, and a placeholder that suggests what to
 * write. The character count sits beside the label, so the line under the field appears only
 * when there is an error to show and otherwise costs no height. Without a [label] the section
 * heading above is the label, and [name] is what a screen reader calls the field.
 */
@Composable
private fun Field(
    name: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardOptions,
    label: String? = name,
    error: String? = null,
    counter: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    leadingIcon: ImageVector? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(label, Modifier.weight(1f))
                if (counter != null) Text(counter, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = colors.onSurfaceVariant) },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            isError = error != null,
            supportingText = error?.let { { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } },
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboard,
            shape = MaterialTheme.shapes.small,
            // White (or raised, in the dark theme) against the sheet, so a field reads as a place to type.
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = colors.surfaceContainerLow,
                focusedContainerColor = colors.surfaceContainerLow,
                errorContainerColor = colors.surfaceContainerLow,
                unfocusedBorderColor = colors.outlineVariant,
            ),
            // The visible label sits outside the field, so name the field for a screen reader.
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = name },
        )
    }
}

/** The four Categories as a two-by-two grid of tiles, each with its icon. */
@Composable
private fun CategoryPicker(selected: Category, onSelect: (Category) -> Unit) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Category.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { category ->
                    val isSelected = category == selected
                    val colors = MaterialTheme.colorScheme
                    Surface(
                        selected = isSelected,
                        onClick = { onSelect(category) },
                        modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) colors.secondaryContainer else colors.surfaceContainerLow,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.primary else colors.outlineVariant),
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(category.icon(), contentDescription = null, Modifier.size(20.dp), tint = if (isSelected) colors.primary else colors.onSurfaceVariant)
                            Text(category.label(), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp), tint = colors.primary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Severity as one four-part scale, Low to Critical, rather than four separate chips: it is an
 * order, and the bars and the position both say so. The chosen part takes its Severity's colour.
 */
@Composable
private fun SeverityPicker(selected: Severity, onSelect: (Severity) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(shape).border(1.dp, colors.outlineVariant, shape).selectableGroup(),
    ) {
        Severity.entries.forEachIndexed { index, severity ->
            val isSelected = severity == selected
            val tone = severity.tone(LocalDarkTheme.current)
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(colors.outlineVariant))
            Column(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (isSelected) tone.container else colors.surfaceContainerLow)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(severity) })
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SeverityBars(severity)
                Text(
                    severity.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) tone.content else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun LocationSection(state: FormState, viewModel: IncidentFormViewModel, lookup: (() -> Unit)?) {
    val colors = MaterialTheme.colorScheme
    Section("Location") {
        Field(
            name = "Address or landmark", label = null, value = state.location.address.orEmpty(), onChange = viewModel::onAddress,
            placeholder = "Address or landmark", singleLine = true, leadingIcon = Icons.Filled.LocationOn,
            keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        )
        if (state.location.hasCoordinates) {
            // Says the lookup worked, in words and with the numbers, not only by a change of button.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, Modifier.size(18.dp), tint = colors.primary)
                Text(
                    "This phone's location is attached · ${state.location.latitude}, ${state.location.longitude}",
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (lookup != null) {
            FilledTonalButton(
                onClick = { viewModel.onLocationRequested(); lookup() },
                enabled = !state.locating,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state.locating) {
                    CircularProgressIndicator(Modifier.size(ButtonDefaults.IconSize), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Text("Finding your location…", Modifier.padding(start = ButtonDefaults.IconSpacing))
                } else {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Text(if (state.location.hasCoordinates) "Update my location" else "Use my location", Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
            }
        }
        state.locationError?.let { ErrorText(it) }
    }
}

/** One strip: the ways to add a photo first, then the photos added, each with its remove button. */
@Composable
private fun PhotosSection(state: FormState, viewModel: IncidentFormViewModel, actions: PhotoActions?) {
    Section(
        title = "Photos",
        trailing = "${state.photos.size} of $MAX_PHOTOS",
        hint = when {
            state.photoRequired -> "Add at least one photo of the incident."
            else -> null
        },
    ) {
        val canAdd = actions != null && state.canAddPhoto
        if (canAdd || state.photos.isNotEmpty()) {
            PhotoStrip(
                state.photos.map { it.id to it.model },
                width = PHOTO_TILE, height = PHOTO_TILE,
                onRemove = { id -> state.photos.firstOrNull { it.id == id }?.let(viewModel::onPhotoRemoved) },
                leading = if (canAdd && actions != null) {
                    {
                        item(key = "take") { AddPhotoTile(AppIcons.Camera, "Take photo", actions.takePhoto) }
                        item(key = "pick") { AddPhotoTile(AppIcons.Image, "Gallery", actions.pickFromGallery) }
                    }
                } else null,
            )
        }
        state.photoError?.let { ErrorText(it) }
    }
}

private val PHOTO_TILE = 104.dp

/** One way to add a photo: a tile the size of a photo, so the strip reads as one row of things. */
@Composable
private fun AddPhotoTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.size(PHOTO_TILE),
        shape = MaterialTheme.shapes.medium,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** A problem with part of the form. An icon and words; announced when it appears. */
@Composable
private fun ErrorText(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Warning, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
        Text(
            text,
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** The submit button and the note about syncing. Fixed under the fields when there is room, so the form can be sent without scrolling to its end. */
@Composable
private fun Footer(label: String, saving: Boolean, enabled: Boolean, divided: Boolean, onSubmit: () -> Unit) {
    Column {
        if (divided) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onSubmit, enabled = enabled, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(ButtonDefaults.IconSize), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Text("Saving…", Modifier.padding(start = ButtonDefaults.IconSpacing))
                } else {
                    Text(label)
                }
            }
            Text(
                "Saved on this device first. It syncs when you're online.",
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
