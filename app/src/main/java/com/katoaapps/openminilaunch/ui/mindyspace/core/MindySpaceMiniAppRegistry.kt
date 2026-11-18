package com.katoaapps.openminilaunch.ui.mindyspace.core

import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.mindyspace.MindySpaceMiniApp
import com.katoaapps.openminilaunch.platform.DeviceActions
import com.katoaapps.openminilaunch.ui.launcher.home.HomeTodoTextMetrics

/** Host-owned dependencies used only while constructing feature-specific mini-apps. */
internal data class MindySpaceHostDependencies(
    val store: LauncherStore,
    val actions: DeviceActions,
    val openTodos: () -> Unit,
    val todoJumpToken: Int,
    val todoItemsPerPage: Int,
    val todoTextMetrics: HomeTodoTextMetrics,
)

/**
 * Dependencies available to a mini-app rendered above the complete launcher surface.
 * OpenMindy currently ships no total-screen mini-app; the platform shell is kept for future ones.
 */
internal class MindySpaceTotalScreenDependencies

/** One catalog entry: static metadata plus an optional runtime implementation factory. */
internal data class MindySpaceMiniAppRegistration(
    val manifest: MindySpaceMiniAppManifest,
    val createTotalScreen: ((MindySpaceTotalScreenDependencies) -> MindySpaceTotalScreenApp)? = null,
    val create: ((MindySpaceHostDependencies) -> MindySpaceApp)?,
)

internal class MindySpaceMiniAppRegistry(
    apps: List<MindySpaceApp>,
) {
    private val byId = apps.associateBy { it.manifest.miniApp }

    init {
        check(byId.size == apps.size) { "MindySpace registry contains duplicate mini-app IDs" }
        apps.forEach { app ->
            check(app.manifest.supportsTotalScreen == (app is MindySpaceTotalScreenApp)) {
                "MindySpace total-screen capability does not match runtime type for " +
                    app.manifest.miniApp.stableId
            }
        }
    }

    fun get(miniApp: MindySpaceMiniApp): MindySpaceApp =
        checkNotNull(byId[miniApp]) { "No runtime MindySpace app registered for ${miniApp.stableId}" }
}
