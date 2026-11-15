package dev.fieldreporter.feature.incidents.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.MAX_PHOTOS
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.feature.incidents.PlatformCapabilities
import dev.fieldreporter.feature.incidents.ui.ChoiceChips
import dev.fieldreporter.feature.incidents.ui.PhotoStrip
import dev.fieldreporter.feature.incidents.ui.label

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (editing) "Edit incident" else "Report incident") },
                navigationIcon = { TextButton(onClick = onClose) { Text("Cancel") } },
            )
        },
    ) { padding ->
        if (!state.loaded) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.blockedMessage?.let { message ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(message, Modifier.padding(16.dp))
                }
            }

            OutlinedTextField(
                value = state.title, onValueChange = viewModel::onTitle, label = { Text("Title") },
                isError = state.titleError != null, supportingText = state.titleError?.let { { Text(it) } },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.description, onValueChange = viewModel::onDescription, label = { Text("What happened?") },
                isError = state.descriptionError != null, supportingText = state.descriptionError?.let { { Text(it) } },
                minLines = 3, modifier = Modifier.fillMaxWidth(),
            )

            Text("Category", style = MaterialTheme.typography.labelLarge)
            ChoiceChips(Category.entries, state.category, { it.label() }, viewModel::onCategory)
            Text("Severity", style = MaterialTheme.typography.labelLarge)
            ChoiceChips(Severity.entries, state.severity, { it.label() }, viewModel::onSeverity)

            Text("Location", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = state.location.address.orEmpty(), onValueChange = viewModel::onAddress,
                label = { Text("Address or landmark") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            if (state.location.hasCoordinates) {
                Text(
                    "${state.location.latitude}, ${state.location.longitude}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
                )
            }
            lookupLocation?.let { OutlinedButton(onClick = it) { Text("Use my location") } }

            Text("Photos (${state.photos.size}/$MAX_PHOTOS)", style = MaterialTheme.typography.labelLarge)
            if (state.photos.isNotEmpty()) {
                PhotoStrip(state.photos.map { it.id to it.model }, onRemove = { id ->
                    state.photos.firstOrNull { it.id == id }?.let(viewModel::onPhotoRemoved)
                })
            }
            if (photoActions != null && state.canAddPhoto) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = photoActions.takePhoto) { Text("Take photo") }
                    OutlinedButton(onClick = photoActions.pickFromGallery) { Text("Choose from gallery") }
                }
            }
            state.photoError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = viewModel::submit,
                enabled = !state.saving && state.blockedMessage == null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (editing) "Save changes" else "Submit report") }
            Text(
                "Saved on this device first. It syncs when you're online.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
