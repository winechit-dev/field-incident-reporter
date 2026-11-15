package dev.fieldreporter.feature.incidents

import dev.fieldreporter.core.domain.Category
import dev.fieldreporter.core.domain.Status
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
}
