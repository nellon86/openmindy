@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.katoaapps.openminilaunch.ui.mindyspace.core

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics

/** Hosts visible mini-apps, the settings shortcut, and MindySpace's vertical switching axis. */
@Composable
internal fun MindySpaceHost(
    store: LauncherStore,
    actions: DeviceActions,
    openTodos: () -> Unit,
    todoJumpToken: Int,
    todoItemsPerPage: Int,
    todoTextMetrics: HomeTodoTextMetrics,
    compact: Boolean,
    contentColor: Color,
    mutedContentColor: Color,
    insetColor: Color,
    openMindySpaceSettings: () -> Unit,
    presentationState: MindySpacePresentationState,
    expandedPresentation: Boolean,
    modifier: Modifier = Modifier,
) {
    val dependencies = remember(
        store,
        actions,
        openTodos,
        todoJumpToken,
        todoItemsPerPage,
        todoTextMetrics,
    ) {
        MindySpaceHostDependencies(
            store = store,
            actions = actions,
            openTodos = openTodos,
            todoJumpToken = todoJumpToken,
            todoItemsPerPage = todoItemsPerPage,
            todoTextMetrics = todoTextMetrics,
        )
    }
    val registry = remember(dependencies) {
        MindySpaceMiniAppCatalog.createRegistry(dependencies)
    }
    // A capture scene must not inherit a live widget, media grant, vCard, or media session.
    // Keep the real MindySpace configuration untouched and substitute only safe built-in pages.
    val activeMiniApps = if (store.demoModeEnabled) {
        listOf(MindySpaceMiniApp.TODO, MindySpaceMiniApp.CALCULATOR)
    } else {
        store.mindySpaceMiniAppOrder.filter { miniApp ->
            miniApp.readyForHome && miniApp in store.enabledMindySpaceMiniApps
        }
    }
    val settingsPage = activeMiniApps.size
    val pagerState = rememberPagerState(pageCount = { activeMiniApps.size + 1 })

    BackHandler(enabled = presentationState.isExpanded) {
        presentationState.collapse()
    }

    LaunchedEffect(activeMiniApps) {
        if (pagerState.currentPage > settingsPage) {
            pagerState.scrollToPage(settingsPage)
        }
    }
    LaunchedEffect(todoJumpToken, activeMiniApps) {
        if (todoJumpToken > 0) {
            val todoPage = activeMiniApps.indexOf(MindySpaceMiniApp.TODO)
            if (todoPage >= 0 && pagerState.currentPage != todoPage) {
                pagerState.animateScrollToPage(todoPage)
            }
        }
    }
    LaunchedEffect(pagerState.currentPage, activeMiniApps, presentationState.expandedMiniApp) {
        val currentMiniApp = activeMiniApps.getOrNull(pagerState.currentPage)
        presentationState.retainOnly(currentMiniApp)
    }

    VerticalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = !presentationState.isExpanded,
        beyondViewportPageCount = 1,
    ) { page ->
        val miniApp = activeMiniApps.getOrNull(page)
        if (miniApp == null) {
            MindySpaceSettingsShortcutPage(
                compact = compact,
                contentColor = contentColor,
                mutedContentColor = mutedContentColor,
                containerColor = insetColor,
                onOpenSettings = openMindySpaceSettings,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            key(miniApp.stableId) {
                val app = registry.get(miniApp)
                val expansion = remember(presentationState, app.manifest) {
                    presentationState.expansionController(app.manifest)
                }
                MindySpaceAppFrame(
                    app = app,
                    scope = MindySpaceScope(
                        compact = compact,
                        contentColor = contentColor,
                        mutedContentColor = mutedContentColor,
                        containerColor = insetColor,
                        expandedPresentation = expandedPresentation,
                        expansion = expansion,
                    ),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
