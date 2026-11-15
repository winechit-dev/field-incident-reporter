package dev.fieldreporter.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.feature.incidents.PhotoActions
import dev.fieldreporter.feature.incidents.PlatformCapabilities

/** The iOS camera, photo picker and location, behind the shared UI's [PlatformCapabilities]. */
class IosPlatformCapabilities : PlatformCapabilities {
    override val canCapturePhotos: Boolean = true

    private val picker = PhotoPicker(PhotoCompressor())
    private val location = LocationLookup()

    @Composable
    override fun rememberPhotoActions(onPhoto: (String) -> Unit): PhotoActions? {
        val latest by rememberUpdatedState(onPhoto)
        return remember {
            PhotoActions(
                takePhoto = { picker.takePhoto { latest(it) } },
                pickFromGallery = { picker.pickFromGallery { latest(it) } },
            )
        }
    }

    @Composable
    override fun rememberLocationLookup(onResult: (Location?) -> Unit): (() -> Unit)? {
        val latest by rememberUpdatedState(onResult)
        return remember { { location.lookup { latest(it) } } }
    }
}
