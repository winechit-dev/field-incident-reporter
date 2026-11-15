package dev.fieldreporter.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.feature.incidents.PhotoActions
import dev.fieldreporter.feature.incidents.PlatformCapabilities
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * The camera and gallery run through the system apps, so no camera permission is needed. Every
 * photo is compressed before it reaches the form. Location asks for permission on first use.
 */
class AndroidPlatformCapabilities(private val compressor: PhotoCompressor) : PlatformCapabilities {
    override val canCapturePhotos: Boolean = true

    private companion object {
        const val TAG = "PhotoCapture"
    }

    @Composable
    override fun rememberPhotoActions(onPhoto: (String) -> Unit): PhotoActions? {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val latest by rememberUpdatedState(onPhoto)
        // Survives the Activity being recreated while the camera app is in front.
        var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }

        fun handle(source: Uri, cleanup: File? = null) {
            scope.launch {
                try {
                    latest(compressor.compress(source))
                } catch (e: IOException) {
                    // An unreadable photo is skipped; the form simply doesn't gain a photo.
                    Log.w(TAG, "Couldn't use the selected photo", e)
                } finally {
                    cleanup?.delete()
                }
            }
        }

        val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val file = cameraFile?.let(::File)
            cameraFile = null
            if (file == null) return@rememberLauncherForActivityResult
            if (saved) handle(Uri.fromFile(file), cleanup = file) else file.delete()
        }
        val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            Log.d(TAG, "Gallery returned $uri")
            if (uri != null) handle(uri)
        }

        return remember(takePicture, pickImage) {
            PhotoActions(
                takePhoto = {
                    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                    val file = File.createTempFile("raw-", ".jpg", dir)
                    cameraFile = file.absolutePath
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    takePicture.launch(uri)
                },
                pickFromGallery = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        }
    }

    @Composable
    override fun rememberLocationLookup(onResult: (Location?) -> Unit): (() -> Unit)? {
        val context = LocalContext.current
        val latest by rememberUpdatedState(onResult)
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            if (granted.values.any { it }) currentLocation(context) { latest(it) } else latest(null)
        }
        return remember(launcher) {
            {
                if (permissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                    currentLocation(context) { latest(it) }
                } else {
                    launcher.launch(permissions)
                }
            }
        }
    }

    @SuppressLint("MissingPermission") // Callers check the permission first.
    private fun currentLocation(context: Context, onResult: (Location?) -> Unit) {
        LocationServices.getFusedLocationProviderClient(context)
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { found -> onResult(found?.let { Location(latitude = it.latitude, longitude = it.longitude) }) }
            .addOnFailureListener { onResult(null) }
    }
}
