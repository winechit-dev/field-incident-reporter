package dev.fieldreporter.feature.incidents.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** A row of photos, thumbnails by default. [onRemove] adds a remove button to each. */
@Composable
fun PhotoStrip(
    models: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    height: Dp = 120.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onRemove: ((String) -> Unit)? = null,
) {
    LazyRow(modifier, contentPadding = contentPadding, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(models, key = { _, it -> it.first }) { index, (id, model) ->
            val position = "Photo ${index + 1} of ${models.size}"
            Box(Modifier.size(width, height).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant)) {
                AsyncImage(
                    model = model,
                    contentDescription = position,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width, height),
                )
                if (onRemove != null) {
                    // A dark disc keeps the cross visible on any photo; the button itself is 48 dp.
                    IconButton(
                        onClick = { onRemove(id) },
                        modifier = Modifier.align(Alignment.TopEnd),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) {
                        Box(Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.extraLarge), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove ${position.lowercase()}", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
