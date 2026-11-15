package dev.fieldreporter.feature.incidents.ui

import androidx.compose.ui.graphics.Color
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.Photo
import dev.fieldreporter.core.domain.Severity
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.core.domain.SyncState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
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

/** A tinted background and the text colour that reads on it. */
data class Tone(val container: Color, val content: Color)

/*
 * The same hues the dashboard's badges use (admin/src/components/status-badges.tsx): a pale
 * tint with dark text in the light theme, a deep tint with pale text in the dark one. Each pair
 * keeps at least 4.5:1 contrast, which the single mid-tone colours used before did not.
 */
fun Status.tone(dark: Boolean): Tone = when (this) {
    Status.OPEN -> if (dark) Tone(Color(0xFF172554), Color(0xFF93C5FD)) else Tone(Color(0xFFDBEAFE), Color(0xFF1E40AF))
    Status.IN_PROGRESS -> if (dark) Tone(Color(0xFF451A03), Color(0xFFFCD34D)) else Tone(Color(0xFFFEF3C7), Color(0xFF78350F))
    Status.RESOLVED -> if (dark) Tone(Color(0xFF052E16), Color(0xFF86EFAC)) else Tone(Color(0xFFDCFCE7), Color(0xFF166534))
}

fun Severity.tone(dark: Boolean): Tone = when (this) {
    Severity.LOW -> if (dark) Tone(Color(0xFF1E293B), Color(0xFFCBD5E1)) else Tone(Color(0xFFE2E8F0), Color(0xFF334155))
    Severity.MEDIUM -> if (dark) Tone(Color(0xFF422006), Color(0xFFFDE047)) else Tone(Color(0xFFFEF9C3), Color(0xFF713F12))
    Severity.HIGH -> if (dark) Tone(Color(0xFF431407), Color(0xFFFDBA74)) else Tone(Color(0xFFFFEDD5), Color(0xFF7C2D12))
    Severity.CRITICAL -> if (dark) Tone(Color(0xFF450A0A), Color(0xFFFCA5A5)) else Tone(Color(0xFFFEE2E2), Color(0xFF991B1B))
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

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * Times come from the server in UTC and are shown in the viewer's zone, in the same form as the
 * dashboard: `10 Nov 2026, 14:45`.
 */
fun Instant.display(zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val t = toLocalDateTime(zone)
    val two = { n: Int -> n.toString().padStart(2, '0') }
    return "${t.day} ${MONTHS[t.month.number - 1]} ${t.year}, ${two(t.hour)}:${two(t.minute)}"
}
