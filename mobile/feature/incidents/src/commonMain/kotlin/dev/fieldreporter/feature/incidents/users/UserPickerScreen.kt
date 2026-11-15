package dev.fieldreporter.feature.incidents.users

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.fieldreporter.core.domain.User
import dev.fieldreporter.feature.incidents.ui.AppCard
import dev.fieldreporter.feature.incidents.ui.Avatar
import dev.fieldreporter.feature.incidents.ui.ContentWidth
import dev.fieldreporter.feature.incidents.ui.EmptyState

@Composable
fun UserPickerScreen(viewModel: UserPickerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold { padding ->
        ContentWidth(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // The product's mark, the same glyph on the same blue as the dashboard's header.
                        Box(
                            Modifier.padding(bottom = 16.dp).size(56.dp).background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Text("Who are you?", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Pick your name to see and report your incidents.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                when {
                    state.loading -> item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                    state.error != null -> item(key = "error") {
                        EmptyState(Icons.Filled.Warning, "Couldn't load the names", state.error.orEmpty()) {
                            Button(onClick = viewModel::load) { Text("Try again") }
                        }
                    }
                    state.technicians.isEmpty() -> item(key = "empty") {
                        EmptyState(Icons.Filled.Person, "No technicians yet", "Ask an operator to add you, then try again.") {
                            Button(onClick = viewModel::load) { Text("Try again") }
                        }
                    }
                    else -> items(state.technicians, key = { it.id }) { user ->
                        TechnicianRow(user, onClick = { viewModel.select(user) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TechnicianRow(user: User, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(user.name, size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium)
                Text("Technician", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
