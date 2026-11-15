package dev.fieldreporter.feature.incidents.ui

import androidx.compose.ui.graphics.Color
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.Photo
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncState
import kotlin.time.Instant

fun Category.label() = when (this) {
    Category.EQUIPMENT -> "Equipment"
    Category.ROAD_HAZARD -> "Road hazard"
    Category.SAFETY -> "Safety"
    Category.OTHER -> "Other"
}

fun Severity.label() = when (this) {
    Severity.LOW -> "Low"
    Severity.MEDIUM -> "Medium"
    Severity.HIGH -> "High"
    Severity.CRITICAL -> "Critical"
}

fun Status.label() = when (this) {
    Status.OPEN -> "Open"
    Status.IN_PROGRESS -> "In progress"
    Status.RESOLVED -> "Resolved"
}

fun Status.color() = when (this) {
    Status.OPEN -> Color(0xFF1565C0)
    Status.IN_PROGRESS -> Color(0xFFEF6C00)
    Status.RESOLVED -> Color(0xFF2E7D32)
}

fun Severity.color() = when (this) {
    Severity.LOW -> Color(0xFF607D8B)
    Severity.MEDIUM -> Color(0xFFF9A825)
    Severity.HIGH -> Color(0xFFE65100)
    Severity.CRITICAL -> Color(0xFFC62828)
}

/** The text for the sync badge, or null when there is nothing to say. */
fun Incident.syncLabel(): String? = when (syncState) {
    SyncState.SYNCED -> null
    SyncState.PENDING -> "Waiting to sync"
    SyncState.SYNCING -> "Syncing…"
    SyncState.FAILED -> "Sync failed"
}

/** What an image loader can show: the server URL, or the local file that has not been uploaded yet. */
fun Photo.imageModel(): String? = url ?: localPath?.let { "file://$it" }

/** Times come from the server in UTC. */
fun Instant.display(): String = toString().replace('T', ' ').take(16) + " UTC"
