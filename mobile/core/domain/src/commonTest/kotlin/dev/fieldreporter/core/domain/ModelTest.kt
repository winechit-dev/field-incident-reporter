package dev.fieldreporter.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ModelTest {
    private val incident = Incident(
        id = "i1", title = "t", description = "d", category = Category.OTHER, severity = Severity.LOW,
        status = Status.OPEN, location = Location(), photos = emptyList(), reportedBy = "u", assignee = null,
        resolutionNote = null, resolvedAt = null, activityLog = emptyList(),
        createdAt = Instant.fromEpochSeconds(0), updatedAt = Instant.fromEpochSeconds(0),
    )

    @Test
    fun onlyOpenIncidentsAreEditable() {
        assertTrue(incident.isEditable)
        assertFalse(incident.copy(status = Status.IN_PROGRESS).isEditable)
        assertFalse(incident.copy(status = Status.RESOLVED).isEditable)
    }

    @Test
    fun photoCapIsFive() {
        val photos = List(MAX_PHOTOS) { Photo(id = "p$it") }
        assertTrue(incident.canAddPhoto)
        assertFalse(incident.copy(photos = photos).canAddPhoto)
        assertFalse(incident.copy(status = Status.IN_PROGRESS).canAddPhoto)
    }

    @Test
    fun laterEditsWinFieldByField() {
        val first = IncidentEdit(title = "a", description = "x")
        val merged = first.mergedWith(IncidentEdit(title = "b"))
        assertEquals("b", merged.title)
        assertEquals("x", merged.description)
    }
}
