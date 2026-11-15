package dev.fieldreporter.feature.incidents.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** A row of photo thumbnails. [onRemove] adds a remove button to each. */
@Composable
fun PhotoStrip(models: List<Pair<String, String>>, modifier: Modifier = Modifier, onRemove: ((String) -> Unit)? = null) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(models, key = { it.first }) { (id, model) ->
            Box(Modifier.size(112.dp).clip(RoundedCornerShape(12.dp))) {
                AsyncImage(
                    model = model,
                    contentDescription = "Incident photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(112.dp),
                )
                if (onRemove != null) {
                    TextButton(onClick = { onRemove(id) }, modifier = Modifier.align(Alignment.TopEnd)) {
                        Text("✕", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
