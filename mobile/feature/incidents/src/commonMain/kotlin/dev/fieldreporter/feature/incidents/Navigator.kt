package dev.fieldreporter.feature.incidents

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

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

    /** The back stack as plain strings, so it survives rotation and process death. */
    fun snapshot(): List<String> = listOf(visits.toString()) + stack.map { screen ->
        when (screen) {
            Screen.List -> "list"
            is Screen.Detail -> "detail:${screen.id}"
            is Screen.Form -> "form:${screen.visit}:${screen.editingId.orEmpty()}"
        }
    }

    companion object {
        /** Rebuilds a navigator from [snapshot]. Anything unreadable starts again on the list. */
        fun restore(saved: List<String>): Navigator {
            val visits = saved.firstOrNull()?.toIntOrNull() ?: return Navigator()
            val screens = saved.drop(1).map { entry ->
                val parts = entry.split(':', limit = 3)
                when {
                    entry == "list" -> Screen.List
                    parts[0] == "detail" && parts.size >= 2 -> Screen.Detail(entry.substringAfter(':'))
                    parts[0] == "form" && parts.size == 3 ->
                        Screen.Form(parts[2].ifEmpty { null }, parts[1].toIntOrNull() ?: return Navigator())
                    else -> return Navigator()
                }
            }
            if (screens.firstOrNull() != Screen.List) return Navigator()
            return Navigator().apply {
                this.visits = visits
                stack.clear()
                stack.addAll(screens)
            }
        }

        val Saver: Saver<Navigator, Any> = listSaver(save = { it.snapshot() }, restore = { restore(it) })
    }
}
