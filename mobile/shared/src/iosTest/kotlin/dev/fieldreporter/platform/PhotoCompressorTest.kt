package dev.fieldreporter.platform

import dev.fieldreporter.feature.incidents.media.PhotoMath
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSFileManager
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Runs inside the iOS simulator, against real UIKit images. */
@OptIn(ExperimentalForeignApi::class)
class PhotoCompressorTest {
    private val written = mutableListOf<String>()

    @AfterTest
    fun cleanUp() = written.forEach { NSFileManager.defaultManager.removeItemAtPath(it, error = null) }

    private fun solid(width: Int, height: Int): UIImage {
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply { scale = 1.0; opaque = true }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(width.toDouble(), height.toDouble()), format = format)
        return renderer.imageWithActions { context ->
            UIColor.orangeColor.setFill()
            context?.fillRect(CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
        }
    }

    private fun pixelSize(path: String): Pair<Int, Int> {
        val image = assertNotNull(UIImage.imageWithContentsOfFile(path), "the compressed file should be a readable image")
        return image.size.useContents { (width * image.scale).toInt() to (height * image.scale).toInt() }
    }

    private fun compress(image: UIImage): String {
        val path = assertNotNull(PhotoCompressor().compress(image), "compression should produce a file")
        written += path
        return path
    }

    @Test
    fun aLargePhotoIsShrunkToTheMaxEdge() {
        val path = compress(solid(3000, 2000))
        assertEquals(PhotoMath.scaledSize(3000, 2000), pixelSize(path))
        assertEquals(1600, pixelSize(path).first)
    }

    @Test
    fun aSmallPhotoIsNotEnlarged() {
        assertEquals(800 to 600, pixelSize(compress(solid(800, 600))))
    }

    @Test
    fun outputIsAJpegInThePendingPhotosFolder() {
        val path = compress(solid(1000, 1000))
        assertTrue(path.endsWith(".jpg"))
        assertTrue(path.startsWith(PhotoCompressor.pendingPhotosDirectory()))
        assertTrue(NSFileManager.defaultManager.fileExistsAtPath(path))
    }

    @Test
    fun orientationIsBakedIn() {
        // The pixels are landscape (3000x2000) but the camera flagged them to be shown rotated, so
        // the photo is portrait. The compressed file must be portrait with no flag left to honour.
        val landscape = solid(3000, 2000)
        val rotated = UIImage.imageWithCGImage(
            landscape.CGImage, scale = 1.0, orientation = UIImageOrientation.UIImageOrientationRight,
        )
        val (w, h) = pixelSize(compress(rotated))
        assertTrue(h > w, "expected portrait, got ${w}x$h")
        assertEquals(PhotoMath.scaledSize(2000, 3000), w to h)
    }
}
