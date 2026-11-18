package com.katoaapps.openminilaunch.ui.mindyspace.todo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceApp
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceMiniAppManifest
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceMiniAppSettingsTarget
import com.katoaapps.openminilaunch.ui.mindyspace.core.MindySpaceScope

/** Todo's complete MindySpace adapter. The feature only receives platform primitives through [MindySpaceScope]. */
internal class TodoMindySpaceApp(
    private val store: LauncherStore,
    private val openTodos: () -> Unit,
    private val jumpToken: Int,
    private val itemsPerPage: Int,
    private val textMetrics: HomeTodoTextMetrics,
) : MindySpaceApp {
    override val manifest: MindySpaceMiniAppManifest = Manifest

    @Composable
    override fun Content(scope: MindySpaceScope, modifier: Modifier) {
        scope.Surface(modifier = modifier) {
            TodoPager(
                store = store,
                openTodos = openTodos,
                jumpToken = jumpToken,
                itemsPerPage = itemsPerPage,
                textMetrics = textMetrics,
                compact = scope.compact,
                contentColor = scope.contentColor,
                mutedContentColor = scope.mutedContentColor,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    companion object {
        val Manifest = MindySpaceMiniAppManifest(
            miniApp = MindySpaceMiniApp.TODO,
            labelRes = R.string.todo_page_title,
            icon = Icons.Default.Checklist,
            settingsTarget = MindySpaceMiniAppSettingsTarget.TODOS,
        )
    }
}
