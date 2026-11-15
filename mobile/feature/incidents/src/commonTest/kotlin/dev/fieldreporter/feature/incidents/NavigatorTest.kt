package dev.fieldreporter.feature.incidents

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NavigatorTest {
    private fun Navigator.formKey() = assertIs<Screen.Form>(current).viewModelKey

    @Test
    fun everyVisitToTheReportFormGetsItsOwnViewModelKey() {
        // A form's view model holds what was typed and whether it was submitted. Reusing one
        // across visits made a closed form reopen with its old text, and after a submit reopen
        // already "done", which closed it again at once.
        val nav = Navigator()

        nav.openForm()
        val first = nav.formKey()
        nav.back()
        nav.openForm()
        val second = nav.formKey()

        assertNotEquals(first, second)
    }

    @Test
    fun reopeningTheSameIncidentForEditingAlsoGetsAFreshKey() {
        val nav = Navigator()

        nav.openForm("incident-1")
        val first = nav.formKey()
        nav.back()
        nav.openForm("incident-1")

        assertNotEquals(first, nav.formKey())
        assertEquals("incident-1", assertIs<Screen.Form>(nav.current).editingId)
    }

    @Test
    fun theReportAndEditFormsNeverShareAKey() {
        val nav = Navigator()
        nav.openForm()
        val report = nav.formKey()
        nav.back()
        nav.openForm("incident-1")

        assertNotEquals(report, nav.formKey())
    }

    @Test
    fun backReturnsToThePreviousScreenAndStopsAtTheList() {
        val nav = Navigator()
        assertFalse(nav.canGoBack)

        nav.openDetail("incident-1")
        nav.openForm("incident-1")
        assertTrue(nav.canGoBack)

        nav.back()
        assertEquals(Screen.Detail("incident-1"), nav.current)
        nav.back()
        assertEquals(Screen.List, nav.current)
        nav.back() // nothing left to go back to
        assertEquals(Screen.List, nav.current)
    }

    @Test
    fun backToListClearsEverythingAbove() {
        val nav = Navigator()
        nav.openDetail("incident-1")
        nav.openForm("incident-1")

        nav.backToList()

        assertEquals(Screen.List, nav.current)
        assertFalse(nav.canGoBack)
    }

    @Test
    fun aRestoredNavigatorShowsTheSameScreenAndBackStack() {
        // The activity is recreated on rotation and after process death. Before this, the back
        // stack was lost and a half-written report dropped the Technician back on the list.
        val nav = Navigator()
        nav.openDetail("i1")
        nav.openForm(editingId = "i1")
        val formKey = nav.formKey()

        val restored = Navigator.restore(nav.snapshot())

        assertEquals(nav.stack.toList(), restored.stack.toList())
        assertEquals(formKey, restored.formKey(), "the form keeps its view model, so what was typed is still there")
        restored.back()
        assertEquals(Screen.Detail("i1"), restored.current)
    }

    @Test
    fun aRestoredNavigatorStillGivesTheNextFormAFreshKey() {
        val nav = Navigator()
        nav.openForm()
        val first = nav.formKey()
        nav.back()

        val restored = Navigator.restore(nav.snapshot())
        restored.openForm()

        assertNotEquals(first, restored.formKey())
    }

    @Test
    fun aSnapshotThatCannotBeReadStartsOnTheList() {
        val restored = Navigator.restore(listOf("3", "detail:i1", "something-else"))
        assertEquals(listOf<Screen>(Screen.List), restored.stack.toList())
    }

    @Test
    fun aFormIsASheetOverTheScreenItWasOpenedFrom() {
        // The form slides up over the list (a new report) or the detail (an edit); that screen
        // stays where it is underneath, and is what shows again when the sheet goes.
        val nav = Navigator()
        assertEquals(Screen.List, nav.base)
        assertEquals(null, nav.sheet)

        nav.openForm()
        assertEquals(Screen.List, nav.base)
        assertIs<Screen.Form>(nav.sheet)
        nav.back()

        nav.openDetail("i1")
        assertEquals(Screen.Detail("i1"), nav.base)
        nav.openForm(editingId = "i1")
        assertEquals(Screen.Detail("i1"), nav.base, "the detail stays under the edit sheet")
        assertEquals("i1", nav.sheet?.editingId)
        nav.back()
        assertEquals(null, nav.sheet)
    }
}
