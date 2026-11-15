package dev.fieldreporter.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dev.fieldreporter.feature.incidents.media.PhotoMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Shrinks a photo to a JPEG that is cheap to upload on a poor connection, and bakes in the EXIF
 * rotation so it looks right everywhere. Results live in app storage, not the cache, because an
 * offline Technician may need them days later.
 */
class PhotoCompressor(
    private val context: Context,
    private val maxEdge: Int = PhotoMath.MAX_EDGE,
    private val quality: Int = 80,
) {
    /** @return the absolute path of the compressed JPEG. @throws IOException if the image can't be read. */
    suspend fun compress(source: Uri): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // A bounds-only decode returns null by design, so check the stream itself.
        (resolver.openInputStream(source) ?: throw IOException("Can't open the photo")).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Not an image")

        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoMath.sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Can't decode the photo")
        val orientation = resolver.openInputStream(source)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val (w, h) = PhotoMath.scaledSize(decoded.width, decoded.height, maxEdge)
        val matrix = Matrix().apply {
            postScale(w.toFloat() / decoded.width, h.toFloat() / decoded.height)
            applyOrientation(orientation)
        }
        val finished = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val dir = File(context.filesDir, "pending_photos").apply { mkdirs() }
        val out = File(dir, "${UUID.randomUUID()}.jpg")
        try {
            out.outputStream().use { finished.compress(Bitmap.CompressFormat.JPEG, quality, it) }
        } finally {
            if (finished !== decoded) finished.recycle()
            decoded.recycle()
        }
        out.absolutePath
    }

    private fun Matrix.applyOrientation(orientation: Int) {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }
}
