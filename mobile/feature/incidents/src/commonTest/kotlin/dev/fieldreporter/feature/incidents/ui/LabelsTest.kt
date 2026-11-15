package dev.fieldreporter.feature.incidents.ui

import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Instant

class LabelsTest {
    private val reported = Instant.parse("2026-11-10T08:15:00Z")

    @Test
    fun timesAreShownInTheViewersZone() {
        assertEquals("10 Nov 2026, 14:45", reported.display(TimeZone.of("Asia/Yangon")))
        assertEquals("10 Nov 2026, 08:15", reported.display(TimeZone.UTC))
    }

    @Test
    fun aTimeNearMidnightLandsOnTheLocalDay() {
        assertEquals("1 Jan 2027, 05:30", Instant.parse("2026-12-31T23:00:00Z").display(TimeZone.of("Asia/Yangon")))
    }

    @Test
    fun everyStatusAndSeverityHasItsOwnToneInBothThemes() {
        for (dark in listOf(false, true)) {
            assertEquals(Status.entries.size, Status.entries.map { it.tone(dark) }.toSet().size)
            assertEquals(Severity.entries.size, Severity.entries.map { it.tone(dark) }.toSet().size)
            Status.entries.forEach { assertNotEquals(it.tone(dark).container, it.tone(dark).content) }
        }
    }
}
