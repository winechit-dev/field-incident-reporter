package dev.fieldreporter.feature.incidents

import androidx.compose.runtime.mutableStateListOf

internal sealed interface Screen {
    data object List : Screen
    data class Detail(val id: String) : Screen

    /**
     * One visit to the report or edit form. A form's view model holds what was typed and whether
     * it was submitted, so each visit needs its own: reusing one made a closed form reopen with its
     * old text, and after a submit reopen already "done", which closed it again at once.
     */
    data class Form(val editingId: String?, val visit: Int) : Screen {
        val viewModelKey: String get() = "form-$visit"
    }
}

/** The app's back stack. Used from the UI thread only. */
internal class Navigator {
    val stack = mutableStateListOf<Screen>(Screen.List)
    private var visits = 0

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun openDetail(id: String) {
        stack.add(Screen.Detail(id))
    }

    /** @param editingId the Incident to edit, or null to report a new one. */
    fun openForm(editingId: String? = null) {
        stack.add(Screen.Form(editingId, visits++))
    }

    fun back() {
        if (canGoBack) stack.removeAt(stack.lastIndex)
    }

    fun backToList() {
        stack.clear()
        stack.add(Screen.List)
    }
}
