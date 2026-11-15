package dev.fieldreporter.feature.incidents

import androidx.compose.runtime.Composable
import dev.fieldreporter.core.domain.Location

/** Launches the camera or the gallery. Each call eventually reports one compressed local image file. */
class PhotoActions(val takePhoto: () -> Unit, val pickFromGallery: () -> Unit)

/**
 * What only the platform can do. Implementations ask for permissions and run the system
 * camera, picker and location services. Returning null means "not available here", and the UI
 * hides that feature.
 */
interface PlatformCapabilities {
    /** Whether this device can take or pick photos. Reports then require at least one. */
    val canCapturePhotos: Boolean

    /** @param onPhoto called with the path of a compressed JPEG. */
    @Composable
    fun rememberPhotoActions(onPhoto: (String) -> Unit): PhotoActions?

    /** @return a function that looks up the current location, reporting null if it could not be found. */
    @Composable
    fun rememberLocationLookup(onResult: (Location?) -> Unit): (() -> Unit)?
}

/** Used until a platform provides its own: no camera, no location. */
object NoPlatformCapabilities : PlatformCapabilities {
    override val canCapturePhotos: Boolean = false

    @Composable
    override fun rememberPhotoActions(onPhoto: (String) -> Unit): PhotoActions? = null

    @Composable
    override fun rememberLocationLookup(onResult: (Location?) -> Unit): (() -> Unit)? = null
}
