package dev.fieldreporter.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoMathTest {
    @Test
    fun largePhotoIsScaledToTheMaxEdgeKeepingAspectRatio() {
        assertEquals(1600 to 1200, PhotoMath.scaledSize(4000, 3000, 1600))
        assertEquals(1200 to 1600, PhotoMath.scaledSize(3000, 4000, 1600))
    }

    @Test
    fun smallPhotoIsNeverEnlarged() {
        assertEquals(800 to 600, PhotoMath.scaledSize(800, 600, 1600))
    }

    @Test
    fun extremeAspectRatioKeepsAtLeastOnePixel() {
        assertEquals(1600 to 1, PhotoMath.scaledSize(100_000, 10, 1600))
    }

    @Test
    fun decodeShrinkNeverGoesBelowTheTargetSize() {
        assertEquals(2, PhotoMath.sampleSize(4000, 3000, 1600)) // 2000 >= 1600, 1000 < 1600
        assertEquals(1, PhotoMath.sampleSize(1600, 1200, 1600))
        assertEquals(4, PhotoMath.sampleSize(8000, 6000, 1600))
    }
}
