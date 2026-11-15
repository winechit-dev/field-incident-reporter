package dev.fieldreporter.feature.incidents.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.MAX_PHOTOS
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.feature.incidents.PlatformCapabilities
import dev.fieldreporter.feature.incidents.ui.AppCard
import dev.fieldreporter.feature.incidents.ui.AppIcons
import dev.fieldreporter.feature.incidents.ui.ChoiceChips
import dev.fieldreporter.feature.incidents.ui.ContentWidth
import dev.fieldreporter.feature.incidents.ui.LocalDarkTheme
import dev.fieldreporter.feature.incidents.ui.Notice
import dev.fieldreporter.feature.incidents.ui.PhotoStrip
import dev.fieldreporter.feature.incidents.ui.SectionLabel
import dev.fieldreporter.feature.incidents.ui.label
import dev.fieldreporter.feature.incidents.ui.tone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun IncidentFormScreen(
    viewModel: IncidentFormViewModel,
    capabilities: PlatformCapabilities,
    editing: Boolean,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onClose() }

    val photoActions = capabilities.rememberPhotoActions(viewModel::onPhotoAdded)
    val lookupLocation = capabilities.rememberLocationLookup(viewModel::onLocationFound)

    // Closing with something entered asks first; an untouched form just closes.
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val close = { if (state.hasUnsavedChanges) confirmDiscard = true else onClose() }
    BackHandler(enabled = state.hasUnsavedChanges) { confirmDiscard = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit incident" else "Report incident", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = close) { Icon(Icons.Filled.Close, contentDescription = "Close") } },
            )
        },
        // Always visible, so the form can be sent without scrolling to its end.
        bottomBar = {
            if (state.loaded) {
                SubmitBar(
                    label = if (editing) "Save changes" else "Submit report",
                    saving = state.saving,
                    enabled = !state.saving && state.blockedMessage == null,
                    onSubmit = viewModel::submit,
                )
            }
        },
    ) { padding ->
        if (!state.loaded) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        ContentWidth(Modifier.padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                state.blockedMessage?.let { Notice(title = null, body = it) }

                val dark = LocalDarkTheme.current
                val sentences = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                OutlinedTextField(
                    value = state.title, onValueChange = viewModel::onTitle, label = { Text("Title") },
                    isError = state.titleError != null, supportingText = state.titleError?.let { { Text(it) } },
                    singleLine = true, keyboardOptions = sentences.copy(imeAction = ImeAction.Next), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.description, onValueChange = viewModel::onDescription, label = { Text("What happened?") },
                    isError = state.descriptionError != null,
                    // The limit is shown while typing, not only as an error after submitting.
                    supportingText = { Text(state.descriptionError ?: "${state.description.length}/$MAX_DESCRIPTION") },
                    minLines = 3, keyboardOptions = sentences, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth(),
                )

                Group("Category") { ChoiceChips(Category.entries, state.category, { it.label() }, viewModel::onCategory) }
                Group("Severity") { ChoiceChips(Severity.entries, state.severity, { it.label() }, viewModel::onSeverity, tone = { it.tone(dark) }) }

                Group("Location") {
                    OutlinedTextField(
                        value = state.location.address.orEmpty(), onValueChange = viewModel::onAddress,
                        label = { Text("Address or landmark") }, singleLine = true,
                        keyboardOptions = sentences.copy(imeAction = ImeAction.Done), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.location.hasCoordinates) {
                        Text(
                            "Coordinates: ${state.location.latitude}, ${state.location.longitude}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    lookupLocation?.let {
                        OutlinedButton(onClick = it) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                            Text("Use my location", Modifier.padding(start = ButtonDefaults.IconSpacing))
                        }
                    }
                    state.locationError?.let { ErrorText(it) }
                }

                Group("Photos (${state.photos.size}/$MAX_PHOTOS)") {
                    if (state.photos.isNotEmpty()) {
                        PhotoStrip(state.photos.map { it.id to it.model }, onRemove = { id ->
                            state.photos.firstOrNull { it.id == id }?.let(viewModel::onPhotoRemoved)
                        })
                    }
                    if (photoActions != null && state.canAddPhoto) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AddPhotoTile(AppIcons.Camera, "Take photo", photoActions.takePhoto, Modifier.weight(1f))
                            AddPhotoTile(AppIcons.Image, "From gallery", photoActions.pickFromGallery, Modifier.weight(1f))
                        }
                    }
                    state.photoError?.let { ErrorText(it) }
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
                TextButton(onClick = { confirmDiscard = false; onClose() }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

private const val MAX_DESCRIPTION = 2000

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(title)
        content()
    }
}

/** One way to add a photo: a large, labelled target instead of a small text button. */
@Composable
private fun AddPhotoTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** A field problem. Announced when it appears, because it shows up away from the button that caused it. */
@Composable
private fun ErrorText(text: String) {
    Text(
        text,
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun SubmitBar(label: String, saving: Boolean, enabled: Boolean, onSubmit: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ContentWidth(Modifier.navigationBarsPadding()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}
