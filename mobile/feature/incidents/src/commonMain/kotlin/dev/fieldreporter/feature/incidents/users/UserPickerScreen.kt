package dev.fieldreporter.feature.incidents.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UserPickerScreen(viewModel: UserPickerViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().safeContentPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Who are you?", style = MaterialTheme.typography.headlineMedium)
        Text("Pick your name to see and report your incidents.", style = MaterialTheme.typography.bodyMedium)
        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::load) { Text("Try again") }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.technicians, key = { it.id }) { user ->
                    Card(Modifier.fillMaxWidth().clickable { viewModel.select(user) }) {
                        Text(user.name, Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
