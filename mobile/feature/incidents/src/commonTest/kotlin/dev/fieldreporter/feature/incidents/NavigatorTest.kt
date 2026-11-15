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
}
