package dev.fieldreporter.feature.incidents.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.Incident
import dev.fieldreporter.core.domain.IncidentEdit
import dev.fieldreporter.core.domain.IncidentRepository
import dev.fieldreporter.core.domain.Location
import dev.fieldreporter.core.domain.MAX_PHOTOS
import dev.fieldreporter.core.domain.NewIncident
import dev.fieldreporter.core.domain.NotEditableException
import dev.fieldreporter.core.domain.PhotoLimitException
import dev.fieldreporter.core.domain.Severity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The parts of the form that can be wrong, in the order they appear on screen. */
enum class FormField { TITLE, DESCRIPTION, PHOTOS }

/** A photo shown in the form: a server URL, or a local file that has not been uploaded yet. */
data class FormPhoto(val id: String, val model: String)

data class FormState(
    val loaded: Boolean = false,
    val title: String = "",
    val description: String = "",
    val category: Category = Category.EQUIPMENT,
    val severity: Severity = Severity.MEDIUM,
    val location: Location = Location(),
    val photos: List<FormPhoto> = emptyList(),
    val titleError: String? = null,
    val descriptionError: String? = null,
    val photoError: String? = null,
    val locationError: String? = null,
    /** True from asking the device for its location until it answers, found or not. */
    val locating: Boolean = false,
    val saving: Boolean = false,
    /** Set when the Incident cannot be edited any more (an Operator picked it up). */
    val blockedMessage: String? = null,
    /** Set once the form has been saved and the screen should close. */
    val done: Boolean = false,
    /** Whether closing now would lose something the Technician entered. The screen asks first. */
    val hasUnsavedChanges: Boolean = false,
    /** Whether a report needs at least one photo. Said on the form, not only after a failed submit. */
    val photoRequired: Boolean = false,
    /** Goes up each time a submit is refused, so the screen can bring [firstProblem] into view. */
    val rejectedSubmits: Int = 0,
) {
    /** The topmost part of the form with an error, or null when there is none. */
    val firstProblem: FormField? get() = when {
        titleError != null -> FormField.TITLE
        descriptionError != null -> FormField.DESCRIPTION
        photoError != null -> FormField.PHOTOS
        else -> null
    }

    val canAddPhoto: Boolean get() = photos.size < MAX_PHOTOS
}

/**
 * Report a new Incident ([editingId] is null) or edit an existing one. When editing, photo changes
 * apply immediately, because every change is queued locally anyway. When reporting, photos are
 * held in the form until it is submitted.
 */
class IncidentFormViewModel(
    private val incidents: IncidentRepository,
    private val editingId: String?,
    /** Reporting needs at least one photo, but only on devices that can capture one. */
    private val requirePhoto: Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(FormState(loaded = editingId == null, photoRequired = editingId == null && requirePhoto))
    val state: StateFlow<FormState> = _state.asStateFlow()
    private var original: Incident? = null
    private var counter = 0

    /** Every state change goes through here, so [FormState.hasUnsavedChanges] is never stale. */
    private fun change(transform: (FormState) -> FormState) = _state.update { current ->
        transform(current).let { it.copy(hasUnsavedChanges = hasUnsavedChanges(it)) }
    }

    private fun hasUnsavedChanges(s: FormState): Boolean = when {
        s.done || s.blockedMessage != null -> false
        // Photo changes on an edit are applied as they happen, so only the fields can be unsaved.
        editingId != null -> original != null && diff(s).let {
            it.title != null || it.description != null || it.category != null || it.severity != null || it.location != null
        }
        else -> s.title.isNotBlank() || s.description.isNotBlank() || s.photos.isNotEmpty() ||
            s.location != Location() || s.category != FormState().category || s.severity != FormState().severity
    }

    init {
        if (editingId != null) load(editingId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val incident = incidents.incident(id).first()
            if (incident == null || !incident.isEditable) {
                change { it.copy(loaded = true, blockedMessage = BLOCKED) }
                return@launch
            }
            original = incident
            change {
                FormState(
                    loaded = true, title = incident.title, description = incident.description,
                    category = incident.category, severity = incident.severity, location = incident.location,
                    photos = incident.photos.toFormPhotos(),
                )
            }
            // Keep photos live: uploads finish and the server swaps local files for URLs.
            incidents.incident(id).collect { latest ->
                if (latest == null || !latest.isEditable) {
                    change { it.copy(blockedMessage = BLOCKED) }
                } else {
                    change { it.copy(photos = latest.photos.toFormPhotos()) }
                }
            }
        }
    }

    fun onTitle(value: String) = change { it.copy(title = value, titleError = null) }
    fun onDescription(value: String) = change { it.copy(description = value, descriptionError = null) }
    fun onCategory(value: Category) = change { it.copy(category = value) }
    fun onSeverity(value: Severity) = change { it.copy(severity = value) }
    fun onAddress(value: String) =
        change { it.copy(location = it.location.copy(address = value.ifBlank { null })) }

    /** The Technician asked for the device's location; [onLocationFound] ends the wait. */
    fun onLocationRequested() = change { it.copy(locating = true, locationError = null) }

    /** @param found null when the lookup failed, which leaves the location as it was. */
    fun onLocationFound(found: Location?) {
        if (found == null) {
            change { it.copy(locating = false, locationError = "Couldn't find your location. Type an address instead.") }
            return
        }
        change {
            it.copy(location = found.copy(address = found.address ?: it.location.address), locationError = null, locating = false)
        }
    }

    fun onPhotoAdded(path: String) {
        if (!_state.value.canAddPhoto) return
        if (editingId == null) {
            change { it.copy(photos = it.photos + FormPhoto("new-${counter++}", path), photoError = null) }
        } else {
            viewModelScope.launch {
                try {
                    incidents.addPhoto(editingId, path)
                } catch (e: PhotoLimitException) {
                    // The button is hidden at five photos, so this is a race at worst.
                } catch (e: NotEditableException) {
                    change { it.copy(blockedMessage = BLOCKED) }
                }
            }
        }
    }

    fun onPhotoRemoved(photo: FormPhoto) {
        if (editingId == null) {
            change { it.copy(photos = it.photos.filterNot { p -> p.id == photo.id }) }
        } else {
            viewModelScope.launch {
                try {
                    incidents.removePhoto(editingId, photo.id)
                } catch (e: NotEditableException) {
                    change { it.copy(blockedMessage = BLOCKED) }
                }
            }
        }
    }

    fun submit() {
        val s = _state.value
        if (s.saving || s.blockedMessage != null) return
        val titleError = if (s.title.isBlank()) "Give the incident a title" else if (s.title.trim().length > 120) "At most 120 characters" else null
        val descriptionError = if (s.description.length > 2000) "At most 2000 characters" else null
        val photoError = if (editingId == null && requirePhoto && s.photos.isEmpty()) "Add at least one photo" else null
        if (titleError != null || descriptionError != null || photoError != null) {
            change { it.copy(titleError = titleError, descriptionError = descriptionError, photoError = photoError, rejectedSubmits = it.rejectedSubmits + 1) }
            return
        }
        change { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                if (editingId == null) {
                    incidents.report(
                        NewIncident(
                            title = s.title, description = s.description, category = s.category,
                            severity = s.severity, location = s.location, photoPaths = s.photos.map { it.model },
                        ),
                    )
                } else {
                    incidents.edit(editingId, diff(s))
                }
                change { it.copy(saving = false, done = true) }
            } catch (e: NotEditableException) {
                change { it.copy(saving = false, blockedMessage = BLOCKED) }
            }
        }
    }

    /** Only what changed, so an untouched field is never overwritten. */
    private fun diff(s: FormState): IncidentEdit {
        val o = original ?: return IncidentEdit()
        return IncidentEdit(
            title = s.title.trim().takeIf { it != o.title },
            description = s.description.trim().takeIf { it != o.description },
            category = s.category.takeIf { it != o.category },
            severity = s.severity.takeIf { it != o.severity },
            location = s.location.takeIf { it != o.location },
        )
    }

    private fun List<dev.fieldreporter.core.domain.Photo>.toFormPhotos() =
        mapNotNull { p -> (p.url ?: p.localPath?.let { "file://$it" })?.let { FormPhoto(p.id, it) } }

    private companion object {
        const val BLOCKED = "An operator is already handling this incident, so it can't be edited any more."
    }
}
