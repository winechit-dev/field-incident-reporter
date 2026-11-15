package dev.fieldreporter.feature.incidents.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

/** A row of photos, thumbnails by default. [onRemove] adds a remove button to each; [onOpen] makes each open on a tap; [leading] puts items, such as add buttons, in front. */
@Composable
fun PhotoStrip(
    models: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    height: Dp = 120.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onRemove: ((String) -> Unit)? = null,
    onOpen: ((Int) -> Unit)? = null,
    leading: (LazyListScope.() -> Unit)? = null,
) {
    LazyRow(modifier, contentPadding = contentPadding, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        leading?.invoke(this)
        itemsIndexed(models, key = { _, it -> it.first }) { index, (id, model) ->
            val position = "Photo ${index + 1} of ${models.size}"
            Box(
                Modifier.size(width, height).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(if (onOpen != null) Modifier.clickable(onClickLabel = "View full screen") { onOpen(index) } else Modifier),
            ) {
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

/** One photo at the full width of its container, for an Incident that has exactly one. */
@Composable
fun SinglePhoto(model: String, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    AsyncImage(
        model = model,
        contentDescription = "Photo 1 of 1",
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClickLabel = "View full screen", onClick = onOpen),
    )
}

/**
 * The photos full screen, uncropped, on black; swipe between them. A Technician checks here that
 * the photo shows what the report says.
 */
@Composable
fun PhotoViewer(models: List<String>, start: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            val pager = rememberPagerState(initialPage = start.coerceIn(0, models.lastIndex)) { models.size }
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = models[page],
                    contentDescription = "Photo ${page + 1} of ${models.size}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f), contentColor = Color.White),
            ) { Icon(Icons.Filled.Close, contentDescription = "Close photo") }
            if (models.size > 1) {
                Text(
                    "${pager.currentPage + 1} / ${models.size}",
                    Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
