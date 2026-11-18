package com.katoaapps.openminilaunch.features.mindyspace.todo

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoExportTest {
    @Test fun includesCompletionStateAndText() {
        val export = formatTodoExport(
            "OpenMindy To-do List",
            listOf(
                TodoItem("1", "Book train tickets"),
                TodoItem("2", "Send revised deck", completed = true),
            ),
        )

        assertEquals(
            "OpenMindy To-do List\n\n[ ] Book train tickets\n[x] Send revised deck",
            export,
        )
    }
}
