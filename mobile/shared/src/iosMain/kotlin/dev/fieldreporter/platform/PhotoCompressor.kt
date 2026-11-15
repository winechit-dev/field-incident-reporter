package dev.fieldreporter.platform

import dev.fieldreporter.feature.incidents.media.PhotoMath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

/**
 * Shrinks a photo to a JPEG that is cheap to upload on a poor connection. Drawing the image into
 * a new bitmap also bakes in its orientation, so it looks right everywhere. Results live in
 * Application Support, not the caches, because an offline Technician may need them days later.
 */
@OptIn(ExperimentalForeignApi::class)
class PhotoCompressor(
    private val maxEdge: Int = PhotoMath.MAX_EDGE,
    private val quality: Double = 0.8,
) {
    /** @return the absolute path of the compressed JPEG, or null if it could not be encoded or saved. */
    fun compress(image: UIImage): String? {
        val (pixelWidth, pixelHeight) = image.size.useContents {
            (width * image.scale).toInt() to (height * image.scale).toInt()
        }
        if (pixelWidth <= 0 || pixelHeight <= 0) return null
        val (w, h) = PhotoMath.scaledSize(pixelWidth, pixelHeight, maxEdge)

        // Scale 1.0 makes the renderer's size mean pixels, not points.
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply {
            scale = 1.0
            opaque = true
        }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(w.toDouble(), h.toDouble()), format = format)
        val resized = renderer.imageWithActions { _ ->
            image.drawInRect(CGRectMake(0.0, 0.0, w.toDouble(), h.toDouble()))
        }

        val data = UIImageJPEGRepresentation(resized, quality) ?: return null
        val path = "${pendingPhotosDirectory()}/${NSUUID().UUIDString}.jpg"
        return if (data.writeToFile(path, atomically = true)) path else null
    }

    /** For photos that arrive as bytes, such as from the gallery. */
    fun compress(data: NSData): String? = UIImage.imageWithData(data)?.let { compress(it) }

    companion object {
        /** Where compressed photos wait for upload. */
        fun pendingPhotosDirectory(): String {
            val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
                .first() as String
            val directory = "$base/pending_photos"
            NSFileManager.defaultManager.createDirectoryAtPath(
                directory, withIntermediateDirectories = true, attributes = null, error = null,
            )
            return directory
        }
    }
}
