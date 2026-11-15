package dev.fieldreporter.feature.incidents

import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.Status
import dev.fieldreporter.feature.incidents.form.FormField
import dev.fieldreporter.feature.incidents.form.IncidentFormViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentFormViewModelTest {
    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm(repo: FakeIncidentRepository, editingId: String? = null, requirePhoto: Boolean = false): IncidentFormViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return IncidentFormViewModel(repo, editingId, requirePhoto)
    }

    @Test
    fun blankTitleIsRejectedAndNothingIsSaved() = runTest {
        val repo = FakeIncidentRepository()
        val vm = vm(repo)
        vm.submit()
        advanceUntilIdle()
        assertNotNull(vm.state.value.titleError)
        assertTrue(repo.reported.isEmpty())
    }

    @Test
    fun reportNeedsAPhotoWhenTheDeviceCanCaptureOne() = runTest {
        val repo = FakeIncidentRepository()
        val vm = vm(repo, requirePhoto = true)
        vm.onTitle("Leak")
        vm.submit()
        assertNotNull(vm.state.value.photoError)

        vm.onPhotoAdded("/cache/a.jpg")
        vm.submit()
        advanceUntilIdle()
        assertEquals(listOf("/cache/a.jpg"), repo.reported.single().photoPaths)
        assertTrue(vm.state.value.done)
    }

    @Test
    fun photoCapIsFive() = runTest {
        val vm = vm(FakeIncidentRepository())
        repeat(8) { vm.onPhotoAdded("/cache/$it.jpg") }
        assertEquals(5, vm.state.value.photos.size)
    }

    @Test
    fun editSendsOnlyTheFieldsThatChanged() = runTest {
        val repo = FakeIncidentRepository(listOf(anIncident("i1")))
        val vm = vm(repo, editingId = "i1")
        advanceUntilIdle()
        vm.onTitle("Hydraulic leak")
        vm.onCategory(Category.SAFETY)
        vm.submit()
        advanceUntilIdle()

        val (id, edit) = repo.edits.single()
        assertEquals("i1", id)
        assertEquals("Hydraulic leak", edit.title)
        assertEquals(Category.SAFETY, edit.category)
        assertNull(edit.description)
        assertNull(edit.severity)
        assertNull(edit.location)
    }

    @Test
    fun editingAnIncidentAnOperatorPickedUpIsBlocked() = runTest {
        val repo = FakeIncidentRepository(listOf(anIncident("i1", Status.IN_PROGRESS)))
        val vm = vm(repo, editingId = "i1")
        advanceUntilIdle()
        assertNotNull(vm.state.value.blockedMessage)
        vm.submit()
        advanceUntilIdle()
        assertTrue(repo.edits.isEmpty())
    }

    @Test
    fun locationLookupKeepsAddressAndReportsAFailure() = runTest {
        val vm = vm(FakeIncidentRepository())
        vm.onAddress("North gate")
        vm.onLocationFound(dev.fieldreporter.core.domain.Location(latitude = 16.8, longitude = 96.1))
        assertEquals("North gate", vm.state.value.location.address)
        assertEquals(16.8, vm.state.value.location.latitude)

        vm.onLocationFound(null)
        assertNotNull(vm.state.value.locationError)
        assertEquals(16.8, vm.state.value.location.latitude, "a failed lookup keeps the earlier fix")
    }

    @Test
    fun aNewReportHasUnsavedChangesOnceSomethingIsEntered() = runTest {
        val vm = vm(FakeIncidentRepository())
        assertFalse(vm.state.value.hasUnsavedChanges, "an untouched form can be closed without asking")

        vm.onTitle("Leak")
        assertTrue(vm.state.value.hasUnsavedChanges)
        vm.onTitle("  ")
        assertFalse(vm.state.value.hasUnsavedChanges, "only whitespace is nothing to lose")

        vm.onPhotoAdded("/cache/a.jpg")
        assertTrue(vm.state.value.hasUnsavedChanges, "a photo held in the form would be lost")
    }

    @Test
    fun anEditHasUnsavedChangesOnlyWhileAFieldDiffersFromTheIncident() = runTest {
        val repo = FakeIncidentRepository(listOf(anIncident("i1")))
        val vm = vm(repo, editingId = "i1")
        advanceUntilIdle()
        assertFalse(vm.state.value.hasUnsavedChanges)

        vm.onSeverity(dev.fieldreporter.core.domain.Severity.LOW)
        assertTrue(vm.state.value.hasUnsavedChanges)
        vm.onSeverity(dev.fieldreporter.core.domain.Severity.HIGH)
        assertFalse(vm.state.value.hasUnsavedChanges, "back to the stored value")
    }

    @Test
    fun aSavedFormHasNothingUnsaved() = runTest {
        val vm = vm(FakeIncidentRepository())
        vm.onTitle("Leak")
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value.done)
        assertFalse(vm.state.value.hasUnsavedChanges)
    }

    @Test
    fun aRejectedSubmitSaysWhichProblemComesFirstSoTheScreenCanShowIt() = runTest {
        val vm = vm(FakeIncidentRepository(), requirePhoto = true)
        assertNull(vm.state.value.firstProblem)

        vm.submit()
        assertEquals(FormField.TITLE, vm.state.value.firstProblem, "the title comes before the photos")
        val first = vm.state.value.rejectedSubmits

        vm.onTitle("Leak")
        vm.submit()
        assertEquals(FormField.PHOTOS, vm.state.value.firstProblem)
        assertEquals(first + 1, vm.state.value.rejectedSubmits, "each rejected submit is a new signal, even for the same problem")

        vm.onPhotoAdded("/cache/a.jpg")
        assertNull(vm.state.value.firstProblem, "fixing the problem clears it")
    }

    @Test
    fun theFormSaysUpFrontWhetherAPhotoIsRequired() = runTest {
        assertTrue(vm(FakeIncidentRepository(), requirePhoto = true).state.value.photoRequired)
        assertFalse(vm(FakeIncidentRepository(), requirePhoto = false).state.value.photoRequired)
        val repo = FakeIncidentRepository(listOf(anIncident("i1")))
        val editing = vm(repo, editingId = "i1", requirePhoto = true)
        advanceUntilIdle()
        assertFalse(editing.state.value.photoRequired, "an edit never asks for a photo")
    }

    @Test
    fun askingForTheLocationShowsItIsBeingFoundUntilAnAnswerComes() = runTest {
        val vm = vm(FakeIncidentRepository())
        assertFalse(vm.state.value.locating)

        vm.onLocationRequested()
        assertTrue(vm.state.value.locating)
        assertFalse(vm.state.value.hasUnsavedChanges, "asking is not yet something to lose")

        vm.onLocationFound(dev.fieldreporter.core.domain.Location(latitude = 16.8, longitude = 96.1))
        assertFalse(vm.state.value.locating)

        vm.onLocationRequested()
        vm.onLocationFound(null)
        assertFalse(vm.state.value.locating, "a failed lookup also ends the wait")
        assertNotNull(vm.state.value.locationError)
    }

    @Test
    fun askingAgainClearsTheEarlierLocationError() = runTest {
        val vm = vm(FakeIncidentRepository())
        vm.onLocationFound(null)
        assertNotNull(vm.state.value.locationError)
        vm.onLocationRequested()
        assertNull(vm.state.value.locationError)
    }
}
