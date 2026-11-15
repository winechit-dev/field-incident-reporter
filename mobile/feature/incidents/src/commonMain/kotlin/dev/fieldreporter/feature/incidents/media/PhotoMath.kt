package dev.fieldreporter.feature.incidents.media

import kotlin.math.max
import kotlin.math.roundToInt

/** The photo sizing rules, shared so Android and iOS shrink photos the same way. */
object PhotoMath {
    /** The longest edge after compression, in pixels. */
    const val MAX_EDGE = 1600

    /** Scale so the longer edge is at most [maxEdge], keeping the aspect ratio. Never enlarges. */
    fun scaledSize(width: Int, height: Int, maxEdge: Int = MAX_EDGE): Pair<Int, Int> {
        val longest = max(width, height)
        if (longest <= maxEdge) return width to height
        val factor = maxEdge.toFloat() / longest
        return max(1, (width * factor).roundToInt()) to max(1, (height * factor).roundToInt())
    }

    /** The largest power-of-two decode shrink that still leaves the image at least [maxEdge] on its long edge. */
    fun sampleSize(width: Int, height: Int, maxEdge: Int = MAX_EDGE): Int {
        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }
}
